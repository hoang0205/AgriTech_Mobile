package com.example.agritech_mobile.ui.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.agritech_mobile.data.remote.dto.InboxNotification
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationScreen(
    onBackClick: () -> Unit,
    onOpenOrder: (InboxNotification) -> Unit,
    viewModel: NotificationViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val unread by viewModel.unreadCount.collectAsStateWithLifecycle()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load(refresh = true) }
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Thông báo", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại") } },
                actions = {
                    IconButton(onClick = { viewModel.load(refresh = true) }, enabled = !state.isLoading && !state.busy) {
                        Icon(Icons.Default.Refresh, "Làm mới")
                    }
                })
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(if (unread > 0) "$unread thông báo chưa đọc" else "Bạn đã đọc hết thông báo",
                        style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = viewModel::readAll, enabled = unread > 0 && state.items.isNotEmpty() && !state.busy && !state.isLoading) {
                        Icon(Icons.Default.DoneAll, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Đọc tất cả")
                    }
                }
            }
            state.error?.let { message ->
                item {
                    Text(message, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = { viewModel.load(refresh = true) }) { Text("Thử lại") }
                }
            }
            if (state.items.isEmpty() && !state.isLoading && state.error == null) {
                item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 64.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Notifications, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(16.dp))
                        Text("Chưa có thông báo", style = MaterialTheme.typography.titleMedium)
                        Text("Các cập nhật đơn hàng sẽ xuất hiện ở đây.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(state.items, key = { it.id }) { item ->
                Card(onClick = { viewModel.open(item) { opened ->
                    if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) onOpenOrder(opened)
                } }, enabled = !state.busy && !state.isLoading,
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (item.read) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))) {
                    Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Notifications, null, tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(item.title, fontWeight = if (item.read) FontWeight.Medium else FontWeight.Bold)
                            Text(item.body, style = MaterialTheme.typography.bodyMedium)
                            val date = remember(item.createdAt) {
                                runCatching {
                                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
                                        timeZone = TimeZone.getTimeZone("UTC")
                                    }
                                    parser.parse(item.createdAt.take(19))?.let {
                                        SimpleDateFormat("HH:mm · dd/MM/yyyy", Locale.forLanguageTag("vi-VN")).format(it)
                                    }.orEmpty()
                                }.getOrDefault("")
                            }
                            Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (!item.read) Box(Modifier.padding(top = 6.dp).size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    }
                }
            }
            if (state.isLoading || state.busy) item {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            }
            else if (state.hasMore) item {
                OutlinedButton(onClick = { viewModel.load() }, modifier = Modifier.fillMaxWidth()) { Text("Xem thêm") }
            }
        }
    }
}
