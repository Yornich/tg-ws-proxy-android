package com.tgwsproxy.data

import com.tgwsproxy.proxy.ProxyConfig
import com.tgwsproxy.proxy.StatsSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class ProxyStatus { STOPPED, STARTING, RUNNING }

@Singleton
class ProxyRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) {
    private val _status = MutableStateFlow(ProxyStatus.STOPPED)
    val status: StateFlow<ProxyStatus> = _status.asStateFlow()

    private val _stats = MutableStateFlow(StatsSnapshot())
    val stats: StateFlow<StatsSnapshot> = _stats.asStateFlow()

    val configFlow = settingsDataStore.config

    @Volatile var currentConfig: ProxyConfig = ProxyConfig()

    fun setRunning(running: Boolean) {
        _status.value = if (running) ProxyStatus.RUNNING else ProxyStatus.STOPPED
    }

    fun setStarting() { _status.value = ProxyStatus.STARTING }

    fun updateStats(snapshot: StatsSnapshot) { _stats.value = snapshot }

    suspend fun saveConfig(config: ProxyConfig) {
        currentConfig = config
        settingsDataStore.save(config)
    }
}
