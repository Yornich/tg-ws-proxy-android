package com.tgwsproxy.proxy

data class DcIpEntry(val dc: Int, val ip: String)

enum class AppTheme { SYSTEM, DARK, LIGHT }
enum class AppLanguage { SYSTEM, EN, RU }

data class ProxyConfig(
    val port: Int = 1080,
    val host: String = "127.0.0.1",
    val autostartBoot: Boolean = false,
    val autostartLaunch: Boolean = false,
    val dcIpList: List<DcIpEntry> = listOf(
        DcIpEntry(2, "149.154.167.220"),
        DcIpEntry(4, "149.154.167.220")
    ),
    val bufKb: Int = 256,
    val poolSize: Int = 4,
    val tcpNodelay: Boolean = true,
    val verboseLog: Boolean = false,
    val logToFile: Boolean = false,
    val logFilePath: String = "",
    val showNotification: Boolean = true,
    val appTheme: AppTheme = AppTheme.SYSTEM,
    val appLanguage: AppLanguage = AppLanguage.SYSTEM
)

object DcPresets {
    val DEFAULT = listOf(
        DcIpEntry(2, "149.154.167.220"),
        DcIpEntry(4, "149.154.167.220")
    )
    val ALL_MAIN = listOf(
        DcIpEntry(1, "149.154.175.50"),
        DcIpEntry(2, "149.154.167.41"),
        DcIpEntry(3, "149.154.175.100"),
        DcIpEntry(4, "149.154.167.91"),
        DcIpEntry(5, "91.108.56.100")
    )
    val ALL_MEDIA = listOf(
        DcIpEntry(1, "149.154.175.52"),
        DcIpEntry(2, "149.154.167.151"),
        DcIpEntry(3, "149.154.175.102"),
        DcIpEntry(4, "149.154.164.250"),
        DcIpEntry(5, "91.108.56.102")
    )
    val DC203 = listOf(
        DcIpEntry(203, "91.105.192.100")
    )
}
