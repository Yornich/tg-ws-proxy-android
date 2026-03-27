package com.tgwsproxy

import android.Manifest
import android.content.Context
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.tgwsproxy.data.SettingsDataStore
import com.tgwsproxy.proxy.AppLanguage
import com.tgwsproxy.proxy.AppTheme
import com.tgwsproxy.ui.AppNavigation
import com.tgwsproxy.ui.theme.TGWSProxyTheme
import dagger.hilt.android.AndroidEntryPoint
import android.os.Build
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settingsDataStore: SettingsDataStore

    private val requestPostNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->

    }

    override fun attachBaseContext(newBase: Context) {
        val prefs = newBase.getSharedPreferences("lang_prefs", Context.MODE_PRIVATE)
        val langName = prefs.getString("app_language", AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
        val lang = AppLanguage.values().find { it.name == langName } ?: AppLanguage.SYSTEM
        super.attachBaseContext(applyLanguage(newBase, lang))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= 33) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                requestPostNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val config by settingsDataStore.config.collectAsState(
                initial = com.tgwsproxy.proxy.ProxyConfig()
            )
            TGWSProxyTheme(appTheme = config.appTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    companion object {
        fun applyLanguage(context: Context, lang: AppLanguage): Context {
            val locale = when (lang) {
                AppLanguage.EN -> Locale("en")
                AppLanguage.RU -> Locale("ru")
                AppLanguage.SYSTEM -> return context
            }
            Locale.setDefault(locale)
            val config = Configuration(context.resources.configuration)
            config.setLocale(locale)
            return context.createConfigurationContext(config)
        }
    }
}
