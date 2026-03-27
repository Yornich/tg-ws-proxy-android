package com.tgwsproxy.proxy

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket
import java.security.SecureRandom
import java.util.Base64
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.cert.X509Certificate

class WsHandshakeException(val statusCode: Int, val statusLine: String, val location: String? = null) :
    Exception("HTTP $statusCode: $statusLine") {
    val isRedirect get() = statusCode in listOf(301, 302, 303, 307, 308)
}

class RawWebSocket private constructor(
    private val socket: Socket,
    private val input: InputStream,
    private val output: OutputStream
) {
    @Volatile private var closed = false

    companion object {
        private val SSL_CTX: SSLContext = SSLContext.getInstance("TLS").also { ctx ->
            ctx.init(null, arrayOf<TrustManager>(object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
            }), SecureRandom())
        }

        suspend fun connect(ip: String, domain: String, path: String = "/apiws", timeoutMs: Int = 10_000): RawWebSocket =
            withContext(Dispatchers.IO) {
                val socket = SSL_CTX.socketFactory.createSocket() as javax.net.ssl.SSLSocket
                socket.connect(InetSocketAddress(ip, 443), timeoutMs)
                socket.soTimeout = timeoutMs
                (socket as javax.net.ssl.SSLSocket).apply {
                    enabledProtocols = arrayOf("TLSv1.2", "TLSv1.3")
                    startHandshake()
                }
                val input = socket.getInputStream()
                val output = socket.getOutputStream()

                val wsKey = Base64.getEncoder().encodeToString(ByteArray(16).also { SecureRandom().nextBytes(it) })
                val req = buildString {
                    append("GET $path HTTP/1.1\r\n")
                    append("Host: $domain\r\n")
                    append("Upgrade: websocket\r\n")
                    append("Connection: Upgrade\r\n")
                    append("Sec-WebSocket-Key: $wsKey\r\n")
                    append("Sec-WebSocket-Version: 13\r\n")
                    append("Sec-WebSocket-Protocol: binary\r\n")
                    append("Origin: https://web.telegram.org\r\n")
                    append("User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36\r\n")
                    append("\r\n")
                }
                output.write(req.toByteArray())
                output.flush()

                val lines = mutableListOf<String>()
                val buf = StringBuilder()
                var prev = -1
                while (true) {
                    val c = input.read()
                    if (c == -1) break
                    if (prev == '\r'.code && c == '\n'.code) {
                        val line = buf.toString().trimEnd('\r')
                        if (line.isEmpty()) break
                        lines.add(line)
                        buf.clear()
                        prev = -1
                        continue
                    }
                    buf.append(c.toChar())
                    prev = c
                }

                val firstLine = lines.firstOrNull() ?: throw WsHandshakeException(0, "empty response")
                val parts = firstLine.split(" ", limit = 3)
                val statusCode = parts.getOrNull(1)?.toIntOrNull() ?: 0

                if (statusCode == 101) return@withContext RawWebSocket(socket, input, output)

                val headers = lines.drop(1).associate { line ->
                    val idx = line.indexOf(':')
                    if (idx > 0) line.substring(0, idx).trim().lowercase() to line.substring(idx + 1).trim()
                    else line to ""
                }
                socket.close()
                throw WsHandshakeException(statusCode, firstLine, headers["location"])
            }
    }

    suspend fun send(data: ByteArray) = withContext(Dispatchers.IO) {
        if (closed) throw IllegalStateException("WebSocket closed")
        output.write(buildFrame(0x2, data, mask = true))
        output.flush()
    }

    suspend fun sendBatch(parts: List<ByteArray>) = withContext(Dispatchers.IO) {
        if (closed) throw IllegalStateException("WebSocket closed")
        for (part in parts) output.write(buildFrame(0x2, part, mask = true))
        output.flush()
    }

    suspend fun recv(): ByteArray? = withContext(Dispatchers.IO) {
        while (!closed) {
            val (opcode, payload) = readFrame() ?: return@withContext null
            when (opcode) {
                0x8 -> {
                    closed = true
                    try { output.write(buildFrame(0x8, payload.take(2).toByteArray(), mask = true)); output.flush() } catch (_: Exception) {}
                    return@withContext null
                }
                0x9 -> {
                    try { output.write(buildFrame(0xA, payload, mask = true)); output.flush() } catch (_: Exception) {}
                }
                0xA -> { }
                0x1, 0x2 -> return@withContext payload
            }
        }
        null
    }

    suspend fun close() = withContext(Dispatchers.IO) {
        if (closed) return@withContext
        closed = true
        try { output.write(buildFrame(0x8, ByteArray(0), mask = true)); output.flush() } catch (_: Exception) {}
        try { socket.close() } catch (_: Exception) {}
    }

    val isClosed get() = closed

    private fun readFrame(): Pair<Int, ByteArray>? {
        return try {
            val b0 = input.read().takeIf { it != -1 } ?: return null
            val b1 = input.read().takeIf { it != -1 } ?: return null
            val opcode = b0 and 0x0F
            var length = (b1 and 0x7F).toLong()
            if (length == 126L) {
                length = ((input.read() shl 8) or input.read()).toLong()
            } else if (length == 127L) {
                length = 0L
                repeat(8) { length = (length shl 8) or input.read().toLong() }
            }
            val masked = (b1 and 0x80) != 0
            val maskKey = if (masked) ByteArray(4) { input.read().toByte() } else null
            val payload = ByteArray(length.toInt()) { input.read().toByte() }
            if (maskKey != null) {
                for (i in payload.indices) payload[i] = (payload[i].toInt() xor maskKey[i % 4].toInt()).toByte()
            }
            opcode to payload
        } catch (_: Exception) { null }
    }

    private fun buildFrame(opcode: Int, data: ByteArray, mask: Boolean): ByteArray {
        val len = data.size
        val header = mutableListOf<Byte>()
        header.add((0x80 or opcode).toByte())
        val maskBit = if (mask) 0x80 else 0
        when {
            len < 126 -> header.add((maskBit or len).toByte())
            len < 65536 -> { header.add((maskBit or 126).toByte()); header.add((len shr 8).toByte()); header.add((len and 0xFF).toByte()) }
            else -> { header.add((maskBit or 127).toByte()); for (i in 7 downTo 0) header.add(((len.toLong() shr (i * 8)) and 0xFF).toByte()) }
        }
        if (!mask) return header.toByteArray() + data
        val maskKey = ByteArray(4).also { SecureRandom().nextBytes(it) }
        val masked = ByteArray(len) { i -> (data[i].toInt() xor maskKey[i % 4].toInt()).toByte() }
        return header.toByteArray() + maskKey + masked
    }
}
