package com.tgwsproxy.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tgwsproxy.proxy.DcIpEntry

@Composable
fun DcIpEditor(
    entries: List<DcIpEntry>,
    onAdd: (DcIpEntry) -> Unit,
    onRemove: (Int) -> Unit,
    onEdit: (Int, DcIpEntry) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        entries.forEachIndexed { index, entry ->
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                DcIpRow(
                    entry = entry,
                    onRemove = { onRemove(index) },
                    onEdit = { onEdit(index, it) }
                )
            }
            Spacer(Modifier.height(8.dp))
        }
        OutlinedButton(
            onClick = { onAdd(DcIpEntry(1, "")) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Add DC entry")
        }
    }
}

@Composable
private fun DcIpRow(
    entry: DcIpEntry,
    onRemove: () -> Unit,
    onEdit: (DcIpEntry) -> Unit
) {
    var dcText by remember(entry.dc) { mutableStateOf(entry.dc.toString()) }
    var ipText by remember(entry.ip) { mutableStateOf(entry.ip) }
    val ipValid = isValidIp(ipText)

    Row(verticalAlignment = Alignment.CenterVertically) {
        OutlinedTextField(
            value = dcText,
            onValueChange = {
                dcText = it
                val dc = it.toIntOrNull()
                if (dc != null) onEdit(entry.copy(dc = dc))
            },
            label = { Text("DC") },
            modifier = Modifier.width(72.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true
        )
        Spacer(Modifier.width(8.dp))
        OutlinedTextField(
            value = ipText,
            onValueChange = {
                ipText = it
                if (isValidIp(it)) onEdit(entry.copy(ip = it))
            },
            label = { Text("IP") },
            modifier = Modifier.weight(1f),
            isError = ipText.isNotEmpty() && !ipValid,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
            singleLine = true
        )
        Spacer(Modifier.width(8.dp))
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
        }
    }
}

private fun isValidIp(ip: String): Boolean {
    val parts = ip.split(".")
    if (parts.size != 4) return false
    return parts.all { part -> part.toIntOrNull()?.let { it in 0..255 } == true }
}
