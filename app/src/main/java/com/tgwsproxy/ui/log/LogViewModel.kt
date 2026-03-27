package com.tgwsproxy.ui.log

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tgwsproxy.data.LogBuffer
import com.tgwsproxy.data.LogEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class LogViewModel @Inject constructor(private val logBuffer: LogBuffer) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    val entries: StateFlow<List<LogEntry>> = combine(logBuffer.entries, _query) { entries, q ->
        if (q.isBlank()) entries else entries.filter { it.message.contains(q, ignoreCase = true) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = logBuffer.entries
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setQuery(q: String) { _query.value = q }
    fun clear() { logBuffer.clear() }
    fun export() = logBuffer.export()
}
