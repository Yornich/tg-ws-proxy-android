package com.tgwsproxy.ui.home

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgwsproxy.data.ProxyRepository
import com.tgwsproxy.data.ProxyStatus
import com.tgwsproxy.proxy.ProxyService
import com.tgwsproxy.proxy.StatsSnapshot
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: ProxyRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val status: StateFlow<ProxyStatus> = repository.status
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ProxyStatus.STOPPED)

    val stats: StateFlow<StatsSnapshot> = repository.stats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsSnapshot())

    val config = repository.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.tgwsproxy.proxy.ProxyConfig())

    fun startProxy() {
        repository.setStarting()
        val intent = Intent(context, ProxyService::class.java).apply {
            action = ProxyService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun stopProxy() {
        val intent = Intent(context, ProxyService::class.java).apply {
            action = ProxyService.ACTION_STOP
        }
        context.startService(intent)
    }
}
