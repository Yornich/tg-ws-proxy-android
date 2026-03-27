package com.tgwsproxy.proxy

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoUtils {

    private val ZERO_64 = ByteArray(64)
    private val VALID_PROTOS = setOf(0xEFEFEFEFL, 0xEEEEEEEEL, 0xDDDDDDDDL)


    private fun keystream(data: ByteArray): ByteArray {
        val key = SecretKeySpec(data.copyOfRange(8, 40), "AES")
        val iv = IvParameterSpec(data.copyOfRange(40, 56))
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        return cipher.update(ZERO_64)
    }


    fun dcFromInit(data: ByteArray): Pair<Int?, Boolean> {
        if (data.size < 64) return null to false
        return try {
            val ks = keystream(data)

            val plain = ByteArray(8) { i -> (data[56 + i].toInt() xor ks[56 + i].toInt()).toByte() }
            val proto = (plain[0].toLong() and 0xFF) or
                    ((plain[1].toLong() and 0xFF) shl 8) or
                    ((plain[2].toLong() and 0xFF) shl 16) or
                    ((plain[3].toLong() and 0xFF) shl 24)
            val dcRaw = (plain[4].toInt() and 0xFF) or ((plain[5].toInt() and 0xFF) shl 8)
            val dcSigned = if (dcRaw > 32767) dcRaw - 65536 else dcRaw
            if (proto in VALID_PROTOS) {
                val dc = Math.abs(dcSigned)
                if (dc in 1..5 || dc == 203) return dc to (dcSigned < 0)
            }
            null to false
        } catch (_: Exception) { null to false }
    }


    fun patchInitDc(data: ByteArray, dc: Int): ByteArray {
        if (data.size < 64) return data
        return try {
            val ks = keystream(data)
            val newDc = shortToLeBytes(dc.toShort())
            val patched = data.copyOf()
            patched[60] = (ks[60].toInt() xor newDc[0].toInt()).toByte()
            patched[61] = (ks[61].toInt() xor newDc[1].toInt()).toByte()
            patched
        } catch (_: Exception) { data }
    }

    private fun shortToLeBytes(v: Short): ByteArray =
        byteArrayOf((v.toInt() and 0xFF).toByte(), ((v.toInt() shr 8) and 0xFF).toByte())


    fun createMsgSplitterCipher(initData: ByteArray): Cipher {
        val key = SecretKeySpec(initData.copyOfRange(8, 40), "AES")
        val iv = IvParameterSpec(initData.copyOfRange(40, 56))
        val cipher = Cipher.getInstance("AES/CTR/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, iv)
        cipher.update(ZERO_64)
        return cipher
    }
}
