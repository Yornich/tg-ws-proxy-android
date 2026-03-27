package com.tgwsproxy.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.tgwsproxy.data.ProxyStatus
import com.tgwsproxy.ui.theme.StatusRunning
import com.tgwsproxy.ui.theme.StatusStarting
import com.tgwsproxy.ui.theme.StatusStopped

@Composable
fun StatusIndicator(status: ProxyStatus, modifier: Modifier = Modifier) {
    val color = when (status) {
        ProxyStatus.RUNNING -> StatusRunning
        ProxyStatus.STARTING -> StatusStarting
        ProxyStatus.STOPPED -> StatusStopped
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == ProxyStatus.STARTING) 600 else 1200,
                easing = EaseOut
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = if (status == ProxyStatus.STARTING) 600 else 1200,
                easing = EaseOut
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulseScale"
    )

    Canvas(modifier = modifier) {
        val cx = size.width / 2
        val cy = size.height / 2
        val innerR = size.minDimension / 2 * 0.43f

        if (status != ProxyStatus.STOPPED) {

            drawCircle(color = color.copy(alpha = pulseAlpha * 0.5f), radius = innerR * pulseScale * 1.4f, center = center)

            drawCircle(color = color.copy(alpha = pulseAlpha), radius = innerR * pulseScale, center = center)
        }

        drawCircle(color = color, radius = innerR, center = center)
    }
}
