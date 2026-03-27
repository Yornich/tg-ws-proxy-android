package com.tgwsproxy.ui.log

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tgwsproxy.R
import com.tgwsproxy.ui.components.LogLine
import kotlinx.coroutines.launch

@Composable
fun LogScreen(viewModel: LogViewModel = hiltViewModel()) {
    val entries by viewModel.entries.collectAsState()
    val totalCount by viewModel.totalCount.collectAsState()
    val query by viewModel.query.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val shareLabel = stringResource(R.string.log_share)


    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) listState.animateScrollToItem(entries.size - 1)
    }

    Column(modifier = Modifier.fillMaxSize()) {

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text(stringResource(R.string.log_search_hint), fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = {
                val text = viewModel.export()
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, text)
                }
                context.startActivity(Intent.createChooser(intent, shareLabel))
            }) {
                Icon(Icons.Rounded.Share, contentDescription = shareLabel)
            }
            IconButton(onClick = viewModel::clear) {
                Icon(Icons.Rounded.ClearAll, contentDescription = stringResource(R.string.log_clear))
            }
        }

        Text(
            text = stringResource(R.string.lines_count, entries.size, totalCount),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(entries, key = { "${it.timestamp}_${it.message.hashCode()}" }) { entry ->
                LogLine(entry = entry)
            }
        }
    }
}
