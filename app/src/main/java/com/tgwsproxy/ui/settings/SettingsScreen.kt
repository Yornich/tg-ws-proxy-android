package com.tgwsproxy.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tgwsproxy.R
import com.tgwsproxy.proxy.AppLanguage
import com.tgwsproxy.proxy.AppTheme
import com.tgwsproxy.proxy.DcPresets
import com.tgwsproxy.proxy.ProxyConfig
import com.tgwsproxy.ui.components.DcIpEditor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onThemeChanged: (AppTheme) -> Unit = {},
    onLanguageChanged: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val loadedConfig by viewModel.config.collectAsState()
    var draft by remember { mutableStateOf<ProxyConfig?>(null) }


    LaunchedEffect(loadedConfig) {
        if (loadedConfig != null && draft == null) {
            draft = loadedConfig
        }
    }

    val current = draft ?: run {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    var advancedExpanded by rememberSaveable { mutableStateOf(false) }

    fun update(new: ProxyConfig) {
        draft = new
        viewModel.save(new)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        SectionHeader(stringResource(R.string.settings_appearance))
        SegmentedRow(
            label    = stringResource(R.string.settings_theme),
            icon     = Icons.Rounded.DarkMode,
            options  = listOf(
                stringResource(R.string.theme_system),
                stringResource(R.string.theme_dark),
                stringResource(R.string.theme_light)
            ),
            selected = current.appTheme.ordinal,
            onSelect = {
                val new = current.copy(appTheme = AppTheme.values()[it])
                update(new)
                onThemeChanged(new.appTheme)
            }
        )

        SegmentedRow(
            label    = stringResource(R.string.settings_language),
            icon     = Icons.Rounded.Language,
            options  = listOf(
                stringResource(R.string.lang_system),
                "English",
                "Русский"
            ),
            selected = current.appLanguage.ordinal,
            onSelect = {
                val new = current.copy(appLanguage = AppLanguage.values()[it])
                update(new)
                onLanguageChanged()
            }
        )

        SectionHeader(stringResource(R.string.settings_notifications))
        SettingsToggle(
            label     = stringResource(R.string.settings_show_notification),
            icon      = Icons.Rounded.Notifications,
            checked   = current.showNotification,
            onChanged = { update(current.copy(showNotification = it)) }
        )

        Spacer(Modifier.height(8.dp))
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            colors   = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.settings_advanced), fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface)
                IconButton(onClick = { advancedExpanded = !advancedExpanded }) {
                    Icon(
                        imageVector = if (advancedExpanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null
                    )
                }
            }
        }

        if (advancedExpanded) {
            SectionHeader(stringResource(R.string.settings_proxy))
            PortField(current.port) { update(current.copy(port = it)) }
            HostDropdown(current.host) { update(current.copy(host = it)) }
            SettingsToggle(
                label     = stringResource(R.string.settings_autostart_boot),
                checked   = current.autostartBoot,
                onChanged = { update(current.copy(autostartBoot = it)) }
            )
            SettingsToggle(
                label     = stringResource(R.string.settings_autostart_launch),
                checked   = current.autostartLaunch,
                onChanged = { update(current.copy(autostartLaunch = it)) }
            )

            SectionHeader(stringResource(R.string.settings_dc_ip_override))
            Text(stringResource(R.string.settings_presets), fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    stringResource(R.string.preset_default)   to DcPresets.DEFAULT,
                    stringResource(R.string.preset_all_main)  to DcPresets.ALL_MAIN,
                    stringResource(R.string.preset_all_media) to DcPresets.ALL_MEDIA,
                ).forEach { (label, preset) ->
                    FilterChip(
                        selected = current.dcIpList == preset,
                        onClick  = { update(current.copy(dcIpList = preset)) },
                        label    = { Text(label, fontSize = 12.sp) }
                    )
                }
            }
            DcIpEditor(
                entries  = current.dcIpList,
                onAdd    = { update(current.copy(dcIpList = current.dcIpList + it)) },
                onRemove = { idx -> update(current.copy(dcIpList = current.dcIpList.toMutableList().also { it.removeAt(idx) })) },
                onEdit   = { idx, entry -> update(current.copy(dcIpList = current.dcIpList.toMutableList().also { it[idx] = entry })) }
            )

            SectionHeader(stringResource(R.string.settings_performance))
            Text("${stringResource(R.string.settings_buf_size)}: ${current.bufKb} KB",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = current.bufKb.toFloat(),
                onValueChange = { update(current.copy(bufKb = it.toInt())) },
                valueRange = 4f..1024f)
            Text("${stringResource(R.string.settings_pool_size)}: ${current.poolSize}",
                fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Slider(value = current.poolSize.toFloat(),
                onValueChange = { update(current.copy(poolSize = it.toInt())) },
                valueRange = 0f..16f, steps = 15)
            SettingsToggle(
                label     = stringResource(R.string.settings_tcp_nodelay),
                checked   = current.tcpNodelay,
                onChanged = { update(current.copy(tcpNodelay = it)) }
            )

            SectionHeader(stringResource(R.string.settings_logging))
            SettingsToggle(
                label     = stringResource(R.string.settings_verbose_log),
                checked   = current.verboseLog,
                onChanged = { update(current.copy(verboseLog = it)) }
            )
            SettingsToggle(
                label     = stringResource(R.string.settings_log_to_file),
                checked   = current.logToFile,
                onChanged = { update(current.copy(logToFile = it)) }
            )
            if (current.logToFile) {
                OutlinedTextField(
                    value         = current.logFilePath,
                    onValueChange = { update(current.copy(logFilePath = it)) },
                    label         = { Text(stringResource(R.string.settings_log_file_path)) },
                    modifier      = Modifier.fillMaxWidth(),
                    singleLine    = true
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionHeader(title: String) {
    Spacer(Modifier.height(8.dp))
    Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary, letterSpacing = 0.8.sp)
    HorizontalDivider(modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
        color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun SettingsToggle(
    label: String,
    checked: Boolean,
    onChanged: (Boolean) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (icon != null) Icon(icon, contentDescription = null,
                modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Switch(checked = checked, onCheckedChange = onChanged)
    }
}

@Composable
private fun SegmentedRow(
    label: String,
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 6.dp)) {
            if (icon != null) Icon(icon, contentDescription = null,
                modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            options.forEachIndexed { idx, opt ->
                FilterChip(selected = selected == idx, onClick = { onSelect(idx) },
                    label = { Text(opt, fontSize = 13.sp) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PortField(port: Int, onChanged: (Int) -> Unit) {
    var text by remember(port) { mutableStateOf(port.toString()) }
    val isError = text.toIntOrNull()?.let { it !in 1..65535 } ?: true
    OutlinedTextField(
        value = text,
        onValueChange = { text = it; it.toIntOrNull()?.let { v -> if (v in 1..65535) onChanged(v) } },
        label = { Text(stringResource(R.string.settings_listen_port)) },
        isError = isError,
        supportingText = if (isError) ({ Text(stringResource(R.string.port_invalid)) }) else null,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HostDropdown(host: String, onChanged: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("127.0.0.1", "0.0.0.0")
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = host, onValueChange = {}, readOnly = true,
            label = { Text(stringResource(R.string.settings_listen_host)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { opt ->
                DropdownMenuItem(text = { Text(opt) }, onClick = { onChanged(opt); expanded = false })
            }
        }
    }
}
