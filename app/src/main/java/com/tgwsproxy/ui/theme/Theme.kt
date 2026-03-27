package com.tgwsproxy.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.tgwsproxy.proxy.AppTheme

private val DarkColorScheme = darkColorScheme(
    primary        = Primary,
    onPrimary      = OnPrimary,
    secondary      = Secondary,
    error          = Error,
    background     = Background,
    surface        = Surface,
    surfaceVariant = SurfaceVariant,
    onSurface      = OnSurface,
    onSurfaceVariant = OnSurfaceVariant,
)

private val LightColorScheme = lightColorScheme(
    primary        = Primary,
    onPrimary      = OnPrimary,
    secondary      = Secondary,
    error          = Error,
)

@Composable
fun TGWSProxyTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (appTheme) {
        AppTheme.DARK   -> true
        AppTheme.LIGHT  -> false
        AppTheme.SYSTEM -> systemDark
    }

    val colorScheme = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val ctx = LocalContext.current
        if (isDark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
    } else {
        if (isDark) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
