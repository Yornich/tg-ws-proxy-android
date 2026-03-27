package com.tgwsproxy.proxy

import kotlinx.coroutines.*
import timber.log.Timber

class WsPool(private val poolSize: Int = 4) {
    private val idle = mutableMapOf<Pair<Int, Boolean>, MutableList<Pair<RawWebSocket, Long>>>()
    private val refilling = mutableSetOf<Pair<Int, Boolean>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    companion object {
        const val MAX_AGE_MS = 120_000L
    }

    suspend fun get(dc: Int, isMedia: Boolean, targetIp: String, domains: List<String>): RawWebSocket? {
        val key = dc to isMedia
        val now = System.currentTimeMillis()
        val bucket = idle.getOrPut(key) { mutableListOf() }

        while (bucket.isNotEmpty()) {
            val (ws, created) = bucket.removeAt(0)
            if (System.currentTimeMillis() - created > MAX_AGE_MS || ws.isClosed) {
                scope.launch { runCatching { ws.close() } }
                continue
            }
            scheduleRefill(key, targetIp, domains)
            return ws
        }
        scheduleRefill(key, targetIp, domains)
        return null
    }

    private fun scheduleRefill(key: Pair<Int, Boolean>, targetIp: String, domains: List<String>) {
        if (key in refilling) return
        refilling.add(key)
        scope.launch {
            try {
                val bucket = idle.getOrPut(key) { mutableListOf() }
                val needed = poolSize - bucket.size
                if (needed <= 0) return@launch
                val jobs = (0 until needed).map {
                    async { connectOne(targetIp, domains) }
                }
                for (job in jobs) {
                    val ws = runCatching { job.await() }.getOrNull()
                    if (ws != null) bucket.add(ws to System.currentTimeMillis())
                }
                Timber.d("WS pool refilled DC%d%s: %d ready", key.first, if (key.second) "m" else "", bucket.size)
            } finally {
                refilling.remove(key)
            }
        }
    }

    private suspend fun connectOne(targetIp: String, domains: List<String>): RawWebSocket? {
        for (domain in domains) {
            return try {
                RawWebSocket.connect(targetIp, domain, timeoutMs = 8_000)
            } catch (e: WsHandshakeException) {
                if (e.isRedirect) continue else null
            } catch (_: Exception) { null }
        }
        return null
    }

    fun warmup(dcIpMap: Map<Int, String>) {
        for ((dc, ip) in dcIpMap) {
            for (isMedia in listOf(false, true)) {
                val domains = DcResolver.wsDomains(dc, isMedia)
                scheduleRefill(dc to isMedia, ip, domains)
            }
        }
    }

    fun shutdown() {
        scope.cancel()
        idle.values.flatten().forEach { (ws, _) -> scope.launch { runCatching { ws.close() } } }
        idle.clear()
    }
}
