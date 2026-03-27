package com.tgwsproxy.proxy

import javax.crypto.Cipher

class MsgSplitter(initData: ByteArray) {
    private val cipher: Cipher = CryptoUtils.createMsgSplitterCipher(initData)

    fun split(chunk: ByteArray): List<ByteArray> {
        val plain = cipher.update(chunk) ?: return listOf(chunk)
        val boundaries = mutableListOf<Int>()
        var pos = 0
        val plainLen = plain.size

        while (pos < plainLen) {
            val first = plain[pos].toInt() and 0xFF
            val msgLen: Int
            if (first == 0x7F) {
                if (pos + 4 > plainLen) break
                val b1 = plain[pos + 1].toInt() and 0xFF
                val b2 = plain[pos + 2].toInt() and 0xFF
                val b3 = plain[pos + 3].toInt() and 0xFF
                msgLen = (b1 or (b2 shl 8) or (b3 shl 16)) * 4
                pos += 4
            } else {
                msgLen = first * 4
                pos += 1
            }
            if (msgLen == 0 || pos + msgLen > plainLen) break
            pos += msgLen
            boundaries.add(pos)
        }

        if (boundaries.size <= 1) return listOf(chunk)

        val parts = mutableListOf<ByteArray>()
        var prev = 0
        for (b in boundaries) {
            parts.add(chunk.copyOfRange(prev, b))
            prev = b
        }
        if (prev < chunk.size) parts.add(chunk.copyOfRange(prev, chunk.size))
        return parts
    }
}
