package com.tgwsproxy.proxy

import kotlinx.coroutines.*
import timber.log.Timber
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.net.Socket

class Socks5Handler(
    private val clientSocket: Socket,
    private val config: ProxyConfig,
    private val stats: ProxyStats,
    private val wsPool: WsPool,
    private val wsBlacklist: MutableSet<Pair<Int, Boolean>>,
    private val dcFailUntil: MutableMap<Pair<Int, Boolean>, Long>,
    private val logger: (String) -> Unit
) {
    private val input: InputStream = clientSocket.getInputStream()
    private val output: OutputStream = clientSocket.getOutputStream()
    private val label = clientSocket.remoteSocketAddress?.toString() ?: "?"

    companion object {
        private val SOCKS5_SUCCESS = byteArrayOf(0x05, 0x00, 0x00, 0x01, 0, 0, 0, 0, 0, 0)
        private val SOCKS5_FAIL    = byteArrayOf(0x05, 0x05, 0x00, 0x01, 0, 0, 0, 0, 0, 0)
        private val SOCKS5_CMD_ERR = byteArrayOf(0x05, 0x07, 0x00, 0x01, 0, 0, 0, 0, 0, 0)
        private val SOCKS5_ATYP_ERR= byteArrayOf(0x05, 0x08, 0x00, 0x01, 0, 0, 0, 0, 0, 0)
        private const val WS_FAIL_TIMEOUT_MS   = 2_000
        private const val WS_CONNECT_TIMEOUT_MS = 10_000
        private const val DC_FAIL_COOLDOWN_MS  = 30_000L
    }

    suspend fun handle() = withContext(Dispatchers.IO) {
        stats.connectionsTotal.incrementAndGet()
        try {

            val ver = input.read()
            if (ver != 5) { clientSocket.close(); return@withContext }
            val nmethods = input.read()
            repeat(nmethods) { input.read() }
            output.write(byteArrayOf(0x05, 0x00))
            output.flush()


            val req = ByteArray(4).also { readFully(input, it) }
            val cmd  = req[1].toInt() and 0xFF
            val atyp = req[3].toInt() and 0xFF
            if (cmd != 1) {
                output.write(SOCKS5_CMD_ERR); output.flush(); clientSocket.close(); return@withContext
            }

            val dst: String = when (atyp) {
                1 -> {
                    val raw = ByteArray(4).also { readFully(input, it) }
                    raw.joinToString(".") { (it.toInt() and 0xFF).toString() }
                }
                3 -> {
                    val len = input.read()
                    String(ByteArray(len).also { readFully(input, it) })
                }
                4 -> {
                    ByteArray(16).also { readFully(input, it) }
                    output.write(SOCKS5_ATYP_ERR); output.flush(); clientSocket.close(); return@withContext
                }
                else -> {
                    output.write(SOCKS5_ATYP_ERR); output.flush(); clientSocket.close(); return@withContext
                }
            }
            val portBuf = ByteArray(2).also { readFully(input, it) }
            val port = ((portBuf[0].toInt() and 0xFF) shl 8) or (portBuf[1].toInt() and 0xFF)

            if (dst.contains(':')) {
                logger("[$label] IPv6 not supported: $dst:$port")
                output.write(SOCKS5_FAIL); output.flush(); clientSocket.close(); return@withContext
            }


            if (!DcResolver.isTelegramIp(dst)) {
                stats.connectionsPassthrough.incrementAndGet()
                handlePassthrough(dst, port)
                return@withContext
            }


            output.write(SOCKS5_SUCCESS); output.flush()

            val init = ByteArray(64)
            try { readFully(input, init) } catch (_: Exception) { clientSocket.close(); return@withContext }

            if (isHttpTransport(init)) {
                stats.connectionsHttpRejected.incrementAndGet()
                clientSocket.close(); return@withContext
            }


            var (dc, isMedia) = CryptoUtils.dcFromInit(init)
            var patchedInit = init
            var initPatched = false
            val dcIpMap = config.dcIpList.associate { it.dc to it.ip }


            if (dc == null) {
                DcResolver.IP_TO_DC[dst]?.let { (mappedDc, mappedMedia) ->
                    dc = mappedDc; isMedia = mappedMedia
                }
            }

            if (dc == null) {
                DcResolver.guessDcByIp(dst)?.let { (guessedDc, guessedMedia) ->
                    dc = guessedDc; isMedia = guessedMedia
                    logger("[$label] guessed DC$dc for unknown IP $dst")
                }
            }


            val dcFromCrypto = CryptoUtils.dcFromInit(init).first != null
            if (!dcFromCrypto && dc != null && dcIpMap.containsKey(dc)) {
                patchedInit = CryptoUtils.patchInitDc(init, if (isMedia == true) dc!! else -(dc!!))
                initPatched = true
            }

            if (dc == null || !dcIpMap.containsKey(dc)) {
                logger("[$label] unknown DC for $dst:$port -> TCP fallback")
                tcpFallback(dst, port, init)
                return@withContext
            }

            val dcKey    = dc!! to (isMedia ?: true)
            val targetIp = dcIpMap[dc!!]!!
            val domains  = DcResolver.wsDomains(dc!!, isMedia)


            if (dcKey in wsBlacklist) {
                logger("[$label] DC${dc} blacklisted -> TCP $dst:$port")
                tcpFallback(dst, port, patchedInit)
                return@withContext
            }


            val now = System.currentTimeMillis()
            val wsTimeout = if (now < (dcFailUntil[dcKey] ?: 0L)) WS_FAIL_TIMEOUT_MS else WS_CONNECT_TIMEOUT_MS

            var ws = wsPool.get(dc!!, isMedia ?: false, targetIp, domains)
            if (ws != null) {
                stats.poolHits.incrementAndGet()
                logger("[$label] DC${dc} pool hit -> $dst:$port")
            } else {
                stats.poolMisses.incrementAndGet()
                var wsFailedRedirect = false
                var allRedirects = true

                for (domain in domains) {
                    try {
                        ws = RawWebSocket.connect(targetIp, domain, timeoutMs = wsTimeout)
                        allRedirects = false
                        logger("[$label] DC${dc} WS connected via $domain")
                        break
                    } catch (e: WsHandshakeException) {
                        stats.wsErrors.incrementAndGet()
                        if (e.isRedirect) {
                            wsFailedRedirect = true
                            logger("[$label] DC${dc} redirect ${e.statusCode} from $domain -> ${e.location}")
                            continue
                        }
                        allRedirects = false
                        logger("[$label] DC${dc} WS handshake failed: ${e.statusLine}")
                    } catch (e: Exception) {
                        stats.wsErrors.incrementAndGet()
                        allRedirects = false
                        logger("[$label] DC${dc} WS connect error: $e")
                    }
                }

                if (ws == null) {
                    if (wsFailedRedirect && allRedirects) {
                        wsBlacklist.add(dcKey)
                        logger("[$label] DC${dc} blacklisted (all 302)")
                    } else {
                        dcFailUntil[dcKey] = now + DC_FAIL_COOLDOWN_MS
                    }
                    logger("[$label] DC${dc} -> TCP fallback $dst:$port")
                    tcpFallback(dst, port, patchedInit)
                    return@withContext
                }
            }

            dcFailUntil.remove(dcKey)
            stats.connectionsWs.incrementAndGet()

            val splitter = if (initPatched) runCatching { MsgSplitter(init) }.getOrNull() else null


            try {
                ws!!.send(patchedInit)
            } catch (e: Exception) {
                logger("[$label] DC${dc} failed to send init: $e")
                runCatching { ws!!.close() }
                tcpFallback(dst, port, patchedInit)
                return@withContext
            }

            bridgeWs(ws!!, splitter, dc!!, isMedia ?: false)

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Timber.w(e, "[$label] handler error")
        } finally {
            runCatching { clientSocket.close() }
        }
    }


    private suspend fun handlePassthrough(dst: String, port: Int) {
        val remote = try {
            Socket().also { it.connect(InetSocketAddress(dst, port), 10_000) }
        } catch (e: Exception) {
            logger("[$label] passthrough failed $dst:$port: $e")
            output.write(SOCKS5_FAIL); output.flush(); clientSocket.close(); return
        }
        output.write(SOCKS5_SUCCESS); output.flush()
        bridgeTcp(remote)
    }


    private suspend fun tcpFallback(dst: String, port: Int, init: ByteArray) {
        val remote = try {
            Socket().also { it.connect(InetSocketAddress(dst, port), 10_000) }
        } catch (e: Exception) {
            logger("[$label] TCP fallback connect failed $dst:$port: $e")
            runCatching { clientSocket.close() }
            return
        }
        stats.connectionsTcpFallback.incrementAndGet()
        try {
            remote.getOutputStream().write(init)
            remote.getOutputStream().flush()
        } catch (_: Exception) {}
        bridgeTcp(remote)
    }


    private suspend fun bridgeWs(
        ws: RawWebSocket,
        splitter: MsgSplitter?,
        dc: Int,
        isMedia: Boolean
    ) = coroutineScope {
        val toWs = launch(Dispatchers.IO) {
            try {
                val buf = ByteArray(65536)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    val chunk = buf.copyOf(n)
                    stats.bytesUp.addAndGet(n.toLong())
                    if (splitter != null) {
                        val parts = splitter.split(chunk)
                        if (parts.size > 1) ws.sendBatch(parts) else ws.send(parts[0])
                    } else {
                        ws.send(chunk)
                    }
                }
            } catch (_: CancellationException) {}
            catch (e: Exception) { logger("[$label] DC$dc tcp->ws: $e") }
        }

        val fromWs = launch(Dispatchers.IO) {
            try {
                while (true) {
                    val data = ws.recv() ?: break
                    stats.bytesDown.addAndGet(data.size.toLong())
                    output.write(data)
                    output.flush()
                }
            } catch (_: CancellationException) {}
            catch (e: Exception) { logger("[$label] DC$dc ws->tcp: $e") }
        }


        toWs.invokeOnCompletion { fromWs.cancel() }
        fromWs.invokeOnCompletion { toWs.cancel() }

        toWs.join()
        fromWs.join()

        runCatching { ws.close() }
        logger("[$label] DC$dc${if (isMedia) "m" else ""} session closed")
    }


    private suspend fun bridgeTcp(remote: Socket) = coroutineScope {
        val remoteIn  = remote.getInputStream()
        val remoteOut = remote.getOutputStream()

        val up = launch(Dispatchers.IO) {
            try {
                val buf = ByteArray(65536)
                while (true) {
                    val n = input.read(buf)
                    if (n <= 0) break
                    stats.bytesUp.addAndGet(n.toLong())
                    remoteOut.write(buf, 0, n)
                    remoteOut.flush()
                }
            } catch (_: CancellationException) {}
            catch (_: Exception) {}
        }

        val down = launch(Dispatchers.IO) {
            try {
                val buf = ByteArray(65536)
                while (true) {
                    val n = remoteIn.read(buf)
                    if (n <= 0) break
                    stats.bytesDown.addAndGet(n.toLong())
                    output.write(buf, 0, n)
                    output.flush()
                }
            } catch (_: CancellationException) {}
            catch (_: Exception) {}
        }

        up.invokeOnCompletion { down.cancel() }
        down.invokeOnCompletion { up.cancel() }

        up.join()
        down.join()

        runCatching { remote.close() }
    }


    private fun isHttpTransport(data: ByteArray): Boolean {
        if (data.size < 5) return false
        val s = String(data, 0, minOf(8, data.size), Charsets.ISO_8859_1)
        return s.startsWith("POST ") || s.startsWith("GET ") ||
                s.startsWith("HEAD ") || s.startsWith("OPTIONS ")
    }


    private fun readFully(stream: InputStream, buf: ByteArray) {
        var offset = 0
        while (offset < buf.size) {
            val n = stream.read(buf, offset, buf.size - offset)
            if (n < 0) throw java.io.EOFException("Stream ended after $offset/${buf.size} bytes")
            offset += n
        }
    }
}
