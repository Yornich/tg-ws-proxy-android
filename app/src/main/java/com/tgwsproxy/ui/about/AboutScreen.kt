package com.tgwsproxy.ui.about

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tgwsproxy.BuildConfig
import com.tgwsproxy.R

@Composable
fun AboutScreen(onBack: () -> Unit = {}) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "v${BuildConfig.VERSION_NAME}",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        HorizontalDivider()


        AboutSectionTitle(stringResource(R.string.about_how_it_works))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                AboutMonoRow("Telegram", stringResource(R.string.about_flow_1))
                AboutMonoRow("SOCKS5", "127.0.0.1:1080")
                AboutMonoRow("TG WS Proxy", stringResource(R.string.about_flow_2))
                AboutMonoRow("WSS", stringResource(R.string.about_flow_3))
                AboutMonoRow("Telegram DC", stringResource(R.string.about_flow_4))
            }
        }


        AboutSectionTitle(stringResource(R.string.about_what_it_does))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                AboutBullet(stringResource(R.string.about_does_1))
                AboutBullet(stringResource(R.string.about_does_2))
                AboutBullet(stringResource(R.string.about_does_3))
                AboutBullet(stringResource(R.string.about_does_4))
                AboutBullet(stringResource(R.string.about_does_5))
            }
        }


        AboutSectionTitle(stringResource(R.string.about_setup_mobile))
        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.about_setup_mobile_path), fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                AboutProxyParams()
            }
        }


        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://github.com/Flowseal/tg-ws-proxy")))
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Rounded.Code, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.about_github))
        }

        Text(
            text = "${stringResource(R.string.about_based_on)}\n${stringResource(R.string.about_license)}",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        FilledTonalButton(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Rounded.ArrowBack, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.about_back))
        }
    }
}

@Composable
private fun AboutSectionTitle(text: String) {
    Text(text, fontSize = 13.sp, fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary, letterSpacing = 0.5.sp)
}

@Composable
private fun AboutMonoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.secondary)
        Text(value, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AboutBullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("•", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun AboutProxyParams() {
    val params = listOf(
        "Type" to "SOCKS5",
        "Server" to "127.0.0.1",
        "Port" to "1080",
        "Login / Password" to "—"
    )
    params.forEach { (k, v) ->
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(k, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(v, fontSize = 12.sp, fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
