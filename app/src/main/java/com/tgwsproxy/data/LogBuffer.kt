package com.tgwsproxy.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class LogLevel { DEBUG, INFO, WARNING, ERROR }

data class LogEntry(
    val timestamp: String,
    val level: LogLevel,
    val message: String
)

@Singleton
class LogBuffer @Inject constructor() {
    private val maxSize = 1000
    private val _entries = MutableStateFlow<List<LogEntry>>(emptyList())
    val entries: StateFlow<List<LogEntry>> = _entries.asStateFlow()

    private val fmt = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun log(level: String, message: String) {
        val entry = LogEntry(
            timestamp = fmt.format(Date()),
            level = when (level.uppercase()) {
                "DEBUG" -> LogLevel.DEBUG
                "WARNING", "WARN" -> LogLevel.WARNING
                "ERROR" -> LogLevel.ERROR
                else -> LogLevel.INFO
            },
            message = message
        )
        val current = _entries.value.toMutableList()
        current.add(entry)
        if (current.size > maxSize) current.removeAt(0)
        _entries.value = current
    }

    fun clear() { _entries.value = emptyList() }

    fun export(): String = _entries.value.joinToString("\n") {
        "[${it.timestamp}] ${it.level} ${it.message}"
    }
}
