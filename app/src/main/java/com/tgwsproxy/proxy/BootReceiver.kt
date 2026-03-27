package com.tgwsproxy.proxy

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tgwsproxy.data.SettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsDataStore: SettingsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val autostartBoot = runBlocking { settingsDataStore.config.first().autostartBoot }
        if (autostartBoot) {
            val serviceIntent = Intent(context, ProxyService::class.java).apply {
                action = ProxyService.ACTION_START
            }
            context.startForegroundService(serviceIntent)
        }
    }
}
