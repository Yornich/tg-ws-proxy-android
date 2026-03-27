package com.tgwsproxy.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tgwsproxy.data.LogEntry
import com.tgwsproxy.data.LogLevel
import com.tgwsproxy.ui.theme.*

@Composable
fun LogLine(entry: LogEntry, modifier: Modifier = Modifier) {
    val color = when (entry.level) {
        LogLevel.DEBUG -> LogDebug
        LogLevel.INFO -> LogInfo
        LogLevel.WARNING -> LogWarning
        LogLevel.ERROR -> LogError
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 1.dp)
    ) {
        Text(
            text = entry.timestamp,
            fontSize = 10.sp,
            color = LogDebug,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(80.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "[${entry.level.name}]",
            fontSize = 10.sp,
            color = color,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(72.dp)
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = entry.message,
            fontSize = 11.sp,
            color = color,
            fontFamily = FontFamily.Monospace
        )
    }
}
