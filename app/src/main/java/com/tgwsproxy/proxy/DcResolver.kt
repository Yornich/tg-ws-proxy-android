package com.tgwsproxy.proxy

import java.net.InetAddress

object DcResolver {


    private val TG_RANGES: List<Pair<Long, Long>> = listOf(
        ipRange("185.76.151.0",  "185.76.151.255"),
        ipRange("149.154.160.0", "149.154.175.255"),
        ipRange("91.105.192.0",  "91.105.193.255"),
        ipRange("91.108.0.0",    "91.108.255.255"),
    )


    val IP_TO_DC: Map<String, Pair<Int, Boolean>> = mapOf(

        "149.154.175.50"  to (1 to false),
        "149.154.175.51"  to (1 to false),
        "149.154.175.53"  to (1 to false),
        "149.154.175.54"  to (1 to false),
        "149.154.175.52"  to (1 to true),

        "149.154.167.41"  to (2 to false),
        "149.154.167.50"  to (2 to false),
        "149.154.167.51"  to (2 to false),
        "149.154.167.220" to (2 to false),
        "95.161.76.100"   to (2 to false),
        "149.154.167.151" to (2 to true),
        "149.154.167.222" to (2 to true),
        "149.154.167.223" to (2 to true),
        "149.154.162.123" to (2 to true),

        "149.154.175.100" to (3 to false),
        "149.154.175.101" to (3 to false),
        "149.154.175.102" to (3 to true),

        "149.154.167.91"  to (4 to false),
        "149.154.167.92"  to (4 to false),
        "149.154.164.250" to (4 to true),
        "149.154.166.120" to (4 to true),
        "149.154.166.121" to (4 to true),
        "149.154.167.118" to (4 to true),
        "149.154.165.111" to (4 to true),

        "91.108.56.100"   to (5 to false),
        "91.108.56.101"   to (5 to false),
        "91.108.56.116"   to (5 to false),
        "91.108.56.126"   to (5 to false),
        "149.154.171.5"   to (5 to false),
        "91.108.56.102"   to (5 to true),
        "91.108.56.128"   to (5 to true),
        "91.108.56.151"   to (5 to true),

        "91.105.192.100"  to (203 to false),
    )

    val DC_OVERRIDES: Map<Int, Int> = mapOf(203 to 2)

    private fun ipRange(from: String, to: String): Pair<Long, Long> {
        fun toLong(ip: String) = InetAddress.getByName(ip).address
            .fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xFF) }
        return toLong(from) to toLong(to)
    }

    fun isTelegramIp(ip: String): Boolean {
        return try {
            val n = InetAddress.getByName(ip).address
                .fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xFF) }
            TG_RANGES.any { (lo, hi) -> n in lo..hi }
        } catch (_: Exception) { false }
    }

    fun wsDomains(dc: Int, isMedia: Boolean?): List<String> {
        val d = DC_OVERRIDES[dc] ?: dc
        return if (isMedia == null || isMedia)
            listOf("kws${d}-1.web.telegram.org", "kws${d}.web.telegram.org")
        else
            listOf("kws${d}.web.telegram.org", "kws${d}-1.web.telegram.org")
    }


    fun guessDcByIp(ip: String): Pair<Int, Boolean>? {
        return try {
            val n = InetAddress.getByName(ip).address
                .fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xFF) }
            when {

                n in ipToLong("149.154.175.0")..ipToLong("149.154.175.255") -> 1 to false

                n in ipToLong("149.154.167.0")..ipToLong("149.154.167.255") -> 2 to false
                n in ipToLong("149.154.162.0")..ipToLong("149.154.162.255") -> 2 to true

                n in ipToLong("149.154.164.0")..ipToLong("149.154.166.255") -> 4 to true

                n in ipToLong("91.108.56.0")..ipToLong("91.108.56.255") -> 5 to false

                n in ipToLong("91.105.192.0")..ipToLong("91.105.193.255") -> 203 to false
                else -> null
            }
        } catch (_: Exception) { null }
    }

    private fun ipToLong(ip: String) = InetAddress.getByName(ip).address
        .fold(0L) { acc, b -> (acc shl 8) or (b.toLong() and 0xFF) }
}
