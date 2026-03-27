package com.tgwsproxy.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgwsproxy.data.ProxyRepository
import com.tgwsproxy.proxy.ProxyConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: ProxyRepository
) : ViewModel() {


    val config = repository.configFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun save(config: ProxyConfig) {
        viewModelScope.launch { repository.saveConfig(config) }
    }
}
