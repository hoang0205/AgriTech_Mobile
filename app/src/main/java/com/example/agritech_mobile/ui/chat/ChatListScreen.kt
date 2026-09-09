package com.example.agritech_mobile.ui.chat

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatListScreen(
    onBackClick: () -> Unit,
    onRoomClick: (roomId: String, partnerId: String, partnerName: String, partnerAvatar: String?) -> Unit,
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.chatListUiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadChatRooms()
    }

    ChatListContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRoomClick = { room ->
            onRoomClick(room.roomId, room.partnerId, room.partnerName, room.partnerAvatar)
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListContent(
    uiState: ChatListUiState,
    onBackClick: () -> Unit,
    onRoomClick: (ChatRoomItem) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Tin nhắn",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF9F9F9))
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = Color(0xFF1B5E20)
                    )
                }

                uiState.chatRooms.isEmpty() -> {
                    EmptyChatView(modifier = Modifier.align(Alignment.Center))
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = uiState.chatRooms,
                            key = { it.roomId }
                        ) { room ->
                            ChatRoomRowItem(
                                room = room,
                                onClick = { onRoomClick(room) }
                            )
                            HorizontalDivider(
                                color = Color(0xFFEEEEEE),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(start = 78.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 3. ITEM & EMPTY STATE COMPOSABLES
// ==========================================
@Composable
fun ChatRoomRowItem(
    room: ChatRoomItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box {
            AsyncImage(
                model = room.partnerAvatar ?: "https://via.placeholder.com/150",
                contentDescription = room.partnerName,
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            // Chấm xanh trạng thái
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(Color(0xFF2E7D32), CircleShape)
                    .align(Alignment.BottomEnd)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = room.partnerName,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF212121),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = formatTime(room.lastUpdated),
                    fontSize = 11.sp,
                    color = Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = room.lastMessage.ifBlank { "Chưa có tin nhắn nào" },
                    fontSize = 13.sp,
                    color = if (room.unreadCount > 0) Color.Black else Color.Gray,
                    fontWeight = if (room.unreadCount > 0) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (room.unreadCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFF2E7D32), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = room.unreadCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyChatView(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.ChatBubbleOutline,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Chưa có cuộc trò chuyện nào",
            fontSize = 15.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Hãy kết nối và trò chuyện với người bán ngay nhé!",
            fontSize = 13.sp,
            color = Color.LightGray
        )
    }
}

fun formatTime(timestamp: Long): String {
    if (timestamp <= 0) return ""
    return if (DateUtils.isToday(timestamp)) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    } else {
        SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(timestamp))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatListContentPreview() {
    val sampleRooms = listOf(
        ChatRoomItem(
            roomId = "room_1",
            partnerId = "p_1",
            partnerName = "Farmer Hoang",
            lastMessage = "Ok em yêu",
            lastUpdated = System.currentTimeMillis() - 1000 * 60 * 15,
            unreadCount = 2
        ),
        ChatRoomItem(
            roomId = "room_2",
            partnerId = "p_2",
            partnerName = "Nông Trại Hữu Cơ Ba Vì",
            lastMessage = "[Đã gửi một sản phẩm]",
            lastUpdated = System.currentTimeMillis() - 1000 * 60 * 60 * 3,
            unreadCount = 0
        ),
        ChatRoomItem(
            roomId = "room_3",
            partnerId = "p_3",
            partnerName = "Hợp Tác Xã Sầu Riêng",
            lastMessage = "Cảm ơn bạn đã ủng hộ cửa hàng!",
            lastUpdated = System.currentTimeMillis() - 1000 * 60 * 60 * 26,
            unreadCount = 0
        )
    )

    ChatListContent(
        uiState = ChatListUiState(
            chatRooms = sampleRooms,
            isLoading = false
        ),
        onBackClick = {},
        onRoomClick = {}
    )
}