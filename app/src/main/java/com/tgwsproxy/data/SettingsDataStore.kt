package com.tgwsproxy.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.tgwsproxy.proxy.AppLanguage
import com.tgwsproxy.proxy.AppTheme
import com.tgwsproxy.proxy.DcIpEntry
import com.tgwsproxy.proxy.ProxyConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "proxy_settings")

@Singleton
class SettingsDataStore @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val PORT              = intPreferencesKey("proxy_port")
        val HOST              = stringPreferencesKey("proxy_host")
        val AUTOSTART_BOOT    = booleanPreferencesKey("autostart_boot")
        val AUTOSTART_LAUNCH  = booleanPreferencesKey("autostart_launch")
        val DC_IP_LIST        = stringPreferencesKey("dc_ip_list")
        val BUF_KB            = intPreferencesKey("buf_kb")
        val POOL_SIZE         = intPreferencesKey("pool_size")
        val TCP_NODELAY       = booleanPreferencesKey("tcp_nodelay")
        val VERBOSE_LOG       = booleanPreferencesKey("verbose_log")
        val LOG_TO_FILE       = booleanPreferencesKey("log_to_file")
        val LOG_FILE_PATH     = stringPreferencesKey("log_file_path")
        val SHOW_NOTIFICATION = booleanPreferencesKey("show_notification")
        val APP_THEME         = stringPreferencesKey("app_theme")
        val APP_LANGUAGE      = stringPreferencesKey("app_language")
    }

    private val defaultDcJson = Json.encodeToString(listOf(
        mapOf("dc" to "2", "ip" to "149.154.167.220"),
        mapOf("dc" to "4", "ip" to "149.154.167.220")
    ))

    val config: Flow<ProxyConfig> = context.dataStore.data.map { prefs ->
        ProxyConfig(
            port             = prefs[Keys.PORT] ?: 1080,
            host             = prefs[Keys.HOST] ?: "127.0.0.1",
            autostartBoot    = prefs[Keys.AUTOSTART_BOOT] ?: false,
            autostartLaunch  = prefs[Keys.AUTOSTART_LAUNCH] ?: false,
            dcIpList         = parseDcList(prefs[Keys.DC_IP_LIST] ?: defaultDcJson),
            bufKb            = prefs[Keys.BUF_KB] ?: 256,
            poolSize         = prefs[Keys.POOL_SIZE] ?: 4,
            tcpNodelay       = prefs[Keys.TCP_NODELAY] ?: true,
            verboseLog       = prefs[Keys.VERBOSE_LOG] ?: false,
            logToFile        = prefs[Keys.LOG_TO_FILE] ?: false,
            logFilePath      = prefs[Keys.LOG_FILE_PATH] ?: "",
            showNotification = prefs[Keys.SHOW_NOTIFICATION] ?: true,
            appTheme         = AppTheme.values().find { it.name == prefs[Keys.APP_THEME] } ?: AppTheme.SYSTEM,
            appLanguage      = AppLanguage.values().find { it.name == prefs[Keys.APP_LANGUAGE] } ?: AppLanguage.SYSTEM
        )
    }

    suspend fun save(config: ProxyConfig) {
        context.getSharedPreferences("lang_prefs", Context.MODE_PRIVATE)
            .edit().putString("app_language", config.appLanguage.name).apply()
        context.dataStore.edit { prefs ->
            prefs[Keys.PORT]              = config.port
            prefs[Keys.HOST]              = config.host
            prefs[Keys.AUTOSTART_BOOT]    = config.autostartBoot
            prefs[Keys.AUTOSTART_LAUNCH]  = config.autostartLaunch
            prefs[Keys.DC_IP_LIST]        = encodeDcList(config.dcIpList)
            prefs[Keys.BUF_KB]            = config.bufKb
            prefs[Keys.POOL_SIZE]         = config.poolSize
            prefs[Keys.TCP_NODELAY]       = config.tcpNodelay
            prefs[Keys.VERBOSE_LOG]       = config.verboseLog
            prefs[Keys.LOG_TO_FILE]       = config.logToFile
            prefs[Keys.LOG_FILE_PATH]     = config.logFilePath
            prefs[Keys.SHOW_NOTIFICATION] = config.showNotification
            prefs[Keys.APP_THEME]         = config.appTheme.name
            prefs[Keys.APP_LANGUAGE]      = config.appLanguage.name
        }
    }

    private fun parseDcList(json: String): List<DcIpEntry> = try {
        Json.decodeFromString<List<Map<String, String>>>(json).mapNotNull { m ->
            DcIpEntry(m["dc"]?.toIntOrNull() ?: return@mapNotNull null, m["ip"] ?: return@mapNotNull null)
        }
    } catch (_: Exception) {
        listOf(DcIpEntry(2, "149.154.167.220"), DcIpEntry(4, "149.154.167.220"))
    }

    private fun encodeDcList(list: List<DcIpEntry>): String =
        Json.encodeToString(list.map { mapOf("dc" to it.dc.toString(), "ip" to it.ip) })
}
