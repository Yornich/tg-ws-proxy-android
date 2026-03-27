package com.tgwsproxy.proxy

import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

class ProxyStats {
    val connectionsTotal = AtomicLong(0)
    val connectionsWs = AtomicLong(0)
    val connectionsTcpFallback = AtomicLong(0)
    val connectionsHttpRejected = AtomicLong(0)
    val connectionsPassthrough = AtomicLong(0)
    val wsErrors = AtomicLong(0)
    val bytesUp = AtomicLong(0)
    val bytesDown = AtomicLong(0)
    val poolHits = AtomicLong(0)
    val poolMisses = AtomicLong(0)

    fun reset() {
        connectionsTotal.set(0); connectionsWs.set(0)
        connectionsTcpFallback.set(0); connectionsHttpRejected.set(0)
        connectionsPassthrough.set(0); wsErrors.set(0)
        bytesUp.set(0); bytesDown.set(0)
        poolHits.set(0); poolMisses.set(0)
    }

    fun snapshot() = StatsSnapshot(
        connectionsTotal = connectionsTotal.get(),
        connectionsWs = connectionsWs.get(),
        connectionsTcpFallback = connectionsTcpFallback.get(),
        connectionsHttpRejected = connectionsHttpRejected.get(),
        connectionsPassthrough = connectionsPassthrough.get(),
        wsErrors = wsErrors.get(),
        bytesUp = bytesUp.get(),
        bytesDown = bytesDown.get(),
        poolHits = poolHits.get(),
        poolMisses = poolMisses.get()
    )
}

data class StatsSnapshot(
    val connectionsTotal: Long = 0,
    val connectionsWs: Long = 0,
    val connectionsTcpFallback: Long = 0,
    val connectionsHttpRejected: Long = 0,
    val connectionsPassthrough: Long = 0,
    val wsErrors: Long = 0,
    val bytesUp: Long = 0,
    val bytesDown: Long = 0,
    val poolHits: Long = 0,
    val poolMisses: Long = 0
) {
    fun humanBytes(n: Long): String {
        var v = n.toDouble()
        for (unit in listOf("B", "KB", "MB", "GB")) {
            if (v < 1024) return String.format(Locale.US, "%.1f %s", v, unit)
            v /= 1024
        }
        return String.format(Locale.US, "%.1f TB", v)
    }

    val trafficUp get() = humanBytes(bytesUp)
    val trafficDown get() = humanBytes(bytesDown)
}
