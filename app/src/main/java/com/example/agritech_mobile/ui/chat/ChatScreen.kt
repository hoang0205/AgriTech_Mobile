package com.example.agritech_mobile.ui.chat

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.agritech_mobile.data.remote.dto.ChatMessage
import com.google.firebase.auth.FirebaseAuth
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatScreen(
    roomId: String,
    partnerId: String,
    partnerName: String,
    partnerAvatar: String?,
    partnerPhone: String? = null,
    initialProductId: String? = null,
    initialProductName: String? = null,
    initialProductPrice: Double? = null,
    initialProductImage: String? = null,
    onBack: () -> Unit,
    onNavigateToProductDetail: (String) -> Unit = {},
    viewModel: ChatViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val context = LocalContext.current

    LaunchedEffect(roomId) {
        viewModel.initChatRoom(roomId, listOf(currentUserId, partnerId))

        if (!initialProductId.isNullOrBlank() && initialProductName != null && initialProductPrice != null) {
            viewModel.sendProductCard(
                senderName = "Tôi",
                productId = initialProductId,
                productName = initialProductName,
                productPrice = initialProductPrice,
                productImage = initialProductImage ?: ""
            )
        }
    }

    ChatContent(
        uiState = uiState,
        currentUserId = currentUserId,
        partnerName = partnerName,
        partnerAvatar = partnerAvatar,
        hasPhone = !partnerPhone.isNullOrBlank(),
        onBack = onBack,
        onCallClick = {
            partnerPhone?.let { phone ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                context.startActivity(intent)
            }
        },
        onInputTextChanged = viewModel::onInputTextChanged,
        onSendClicked = { viewModel.sendTextMessage("Tôi") },
        onProductClick = onNavigateToProductDetail
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatContent(
    uiState: ChatUiState,
    currentUserId: String,
    partnerName: String,
    partnerAvatar: String?,
    hasPhone: Boolean,
    onBack: () -> Unit,
    onCallClick: () -> Unit,
    onInputTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size - 1)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box {
                            AsyncImage(
                                model = partnerAvatar ?: "https://via.placeholder.com/150",
                                contentDescription = null,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(Color(0xFF2E7D32), CircleShape)
                                    .align(Alignment.BottomEnd)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = partnerName,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = "Online",
                                fontSize = 12.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    if (hasPhone) {
                        IconButton(onClick = onCallClick) {
                            Icon(Icons.Default.Call, contentDescription = "Gọi điện", tint = Color.Gray)
                        }
                    }
                    IconButton(onClick = { }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Thêm", tint = Color.Gray)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            ChatBottomInput(
                inputText = uiState.inputText,
                isSending = uiState.isSending,
                onTextChanged = onInputTextChanged,
                onSendClicked = onSendClicked
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF9F9F9))
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE0E0E0)
                        ) {
                            Text(
                                text = "TODAY",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.DarkGray
                            )
                        }
                    }
                }

                items(uiState.messages, key = { it.id.ifBlank { it.timestamp.toString() } }) { message ->
                    val isMe = message.senderId == currentUserId

                    if (!message.productId.isNullOrBlank()) {
                        ProductMessageCard(
                            message = message,
                            onProductClick = { onProductClick(message.productId) }
                        )
                    } else {
                        ChatBubbleItem(
                            message = message,
                            isMe = isMe,
                            partnerAvatar = partnerAvatar
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubbleItem(
    message: ChatMessage,
    isMe: Boolean,
    partnerAvatar: String?
) {
    val timeFormat = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
    val timeString = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMe) {
            AsyncImage(
                model = partnerAvatar ?: "https://via.placeholder.com/150",
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isMe) 16.dp else 4.dp,
                        bottomEnd = if (isMe) 4.dp else 16.dp
                    )
                )
                .background(if (isMe) Color(0xFF1B5E20) else Color(0xFFF1F3F4))
                .padding(12.dp)
        ) {
            Text(
                text = message.text,
                color = if (isMe) Color.White else Color(0xFF212121),
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = timeString,
                    color = if (isMe) Color(0xFFE0E0E0) else Color.Gray,
                    fontSize = 10.sp
                )
                if (isMe) {
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(
                        imageVector = Icons.Default.DoneAll,
                        contentDescription = "Đã gửi",
                        tint = Color(0xFF81C784),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductMessageCard(
    message: ChatMessage,
    onProductClick: () -> Unit
) {
    val formatPrice = remember(message.productPrice) {
        NumberFormat.getNumberInstance(Locale("vi", "VN")).format(message.productPrice ?: 0.0)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onProductClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = message.productImage,
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.productName ?: "Sản phẩm",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "$formatPrice đ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF2E7D32)
                )
            }

            IconButton(onClick = onProductClick) {
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = "Xem hàng",
                    tint = Color(0xFF2E7D32)
                )
            }
        }
    }
}

@Composable
fun ChatBottomInput(
    inputText: String,
    isSending: Boolean,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { }) {
                Icon(Icons.Default.Add, contentDescription = "Thêm", tint = Color.Gray)
            }

            TextField(
                value = inputText,
                onValueChange = onTextChanged,
                placeholder = { Text("Nhập tin nhắn...", fontSize = 14.sp, color = Color.Gray) },
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 44.dp),
                shape = RoundedCornerShape(24.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFFF1F3F4),
                    unfocusedContainerColor = Color(0xFFF1F3F4),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                maxLines = 4
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = onSendClicked,
                enabled = inputText.isNotBlank() && !isSending,
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        if (inputText.isNotBlank() && !isSending) Color(0xFF1B5E20) else Color(0xFFA5D6A7),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Gửi",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun ChatScreenPreview() {
    val sampleMessages = listOf(
        ChatMessage(
            id = "1",
            productId = "prod_001",
            productName = "Durian Ri6 (Premium Grade)",
            productPrice = 120000.0,
            productImage = "https://via.placeholder.com/150",
            senderId = "user_2",
            timestamp = 1714500000000L
        ),
        ChatMessage(
            id = "2",
            senderId = "user_2",
            text = "Chào bạn, sầu riêng Ri6 nhà mình vừa hái sáng nay, bao ăn nhé!",
            timestamp = 1714500100000L
        ),
        ChatMessage(
            id = "3",
            senderId = "user_1",
            text = "Chào chú, con muốn đặt 2 quả tầm 5-6kg ạ. Có giao trong chiều nay được không chú?",
            timestamp = 1714500200000L
        ),
        ChatMessage(
            id = "4",
            senderId = "user_2",
            text = "Được nhé con. Chú chọn cho 2 quả ngon nhất, chiều 3h chú giao qua.",
            timestamp = 1714500300000L
        )
    )

    ChatContent(
        uiState = ChatUiState(
            messages = sampleMessages,
            inputText = "Dạ vâng con cảm ơn chú!",
            isLoading = false
        ),
        currentUserId = "user_1",
        partnerName = "Farmer Hoang",
        partnerAvatar = null,
        hasPhone = true,
        onBack = {},
        onCallClick = {},
        onInputTextChanged = {},
        onSendClicked = {},
        onProductClick = {}
    )
}