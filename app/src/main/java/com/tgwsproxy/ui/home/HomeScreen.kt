package com.tgwsproxy.ui.home

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.SyncAlt
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tgwsproxy.R
import com.tgwsproxy.data.ProxyStatus
import com.tgwsproxy.ui.theme.StatusRunning
import com.tgwsproxy.ui.theme.StatusStarting
import com.tgwsproxy.ui.theme.StatusStopped

@Composable
fun HomeScreen(
    onOpenLog: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val status by viewModel.status.collectAsState()
    val stats  by viewModel.stats.collectAsState()
    val config by viewModel.config.collectAsState()
    val context = LocalContext.current
    val proxyAddress = "${config.host}:${config.port}"
    val telegramProxyUrl = "https://t.me/socks?server=127.0.0.1&port=${config.port}"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        StatusHeroCard(
            status           = status,
            proxyAddress     = proxyAddress,
            telegramProxyUrl = telegramProxyUrl,
            context          = context,
            onStart          = viewModel::startProxy,
            onStop           = viewModel::stopProxy
        )


        ConnectTelegramButton(telegramProxyUrl = telegramProxyUrl, context = context)


        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(
                icon  = Icons.Rounded.Dns,
                label = stringResource(R.string.stat_connections),
                value = stats.connectionsTotal.toString(),
                modifier = Modifier.weight(1f)
            )
            StatTile(
                icon  = Icons.Rounded.Wifi,
                label = "WS",
                value = stats.connectionsWs.toString(),
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            StatTile(
                icon  = Icons.Rounded.SyncAlt,
                label = stringResource(R.string.stat_tcp_fallback),
                value = stats.connectionsTcpFallback.toString(),
                modifier = Modifier.weight(1f)
            )
            TrafficTile(
                up   = stats.trafficUp,
                down = stats.trafficDown,
                modifier = Modifier.weight(1f)
            )
        }


        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenLog() },
            shape  = RoundedCornerShape(14.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Text(
                    text  = stringResource(R.string.recent_logs),
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatusHeroCard(
    status: ProxyStatus,
    proxyAddress: String,
    telegramProxyUrl: String,
    context: Context,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val statusColor by animateColorAsState(
        targetValue = when (status) {
            ProxyStatus.RUNNING  -> StatusRunning
            ProxyStatus.STARTING -> StatusStarting
            ProxyStatus.STOPPED  -> StatusStopped
        },
        animationSpec = tween(600),
        label = "statusColor"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (status == ProxyStatus.STOPPED) 1f else 1.7f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue  = if (status == ProxyStatus.STOPPED) 0.5f else 0f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(24.dp),
        colors   = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {

            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {

                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = pulseAlpha))
                )

                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(statusColor.copy(alpha = 0.25f))
                )

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(statusColor)
                )
            }

            Text(
                text = when (status) {
                    ProxyStatus.RUNNING  -> stringResource(R.string.status_running)
                    ProxyStatus.STOPPED  -> stringResource(R.string.status_stopped)
                    ProxyStatus.STARTING -> stringResource(R.string.status_starting)
                },
                fontSize   = 24.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                letterSpacing = 2.sp
            )


            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable {
                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("proxy", telegramProxyUrl))
                        Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text       = proxyAddress,
                    fontSize   = 15.sp,
                    fontFamily = FontFamily.Monospace,
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = Icons.Rounded.ContentCopy,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }

            StartStopButton(status = status, onStart = onStart, onStop = onStop)
        }
    }
}

@Composable
private fun ConnectTelegramButton(telegramProxyUrl: String, context: Context) {
    FilledTonalButton(
        onClick = {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(telegramProxyUrl)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            } catch (_: Exception) {

            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(14.dp)
    ) {
        Icon(Icons.Rounded.Link, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.connect_telegram), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun TrafficTile(
    up: String,
    down: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.height(90.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Speed,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text     = stringResource(R.string.stat_traffic),
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text       = "TX $up",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text       = "RX $down",
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun StatTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier.height(90.dp),
        shape    = RoundedCornerShape(14.dp),
        colors   = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement   = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint     = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text     = label,
                    fontSize = 11.sp,
                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
            Text(
                text       = value,
                fontSize   = 18.sp,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.onSurface,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun StartStopButton(
    status: ProxyStatus,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val isRunning  = status == ProxyStatus.RUNNING
    val isStarting = status == ProxyStatus.STARTING

    val infiniteTransition = rememberInfiniteTransition(label = "btnPulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue  = if (isStarting) 0.96f else 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "btnScale"
    )

    Button(
        onClick = { if (isRunning) onStop() else onStart() },
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale),
        shape  = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.error
            else MaterialTheme.colorScheme.primary
        )
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
            contentDescription = null,
            modifier = Modifier.size(22.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text       = if (isRunning) stringResource(R.string.action_stop)
            else stringResource(R.string.action_start),
            fontWeight = FontWeight.Bold,
            fontSize   = 16.sp,
            letterSpacing = 1.sp
        )
    }
}
