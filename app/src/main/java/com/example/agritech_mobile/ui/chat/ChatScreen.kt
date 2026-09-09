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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.agritech_mobile.data.remote.dto.ChatMessage
import com.example.agritech_mobile.ui.dashboard.DashboardViewModel
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
    viewModel: ChatViewModel = hiltViewModel(),
    dashboardViewModel: DashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val context = LocalContext.current

    val myAvatar = dashboardViewModel.userAvatar.collectAsStateWithLifecycle().value
    val myName = dashboardViewModel.userName.collectAsStateWithLifecycle().value

    LaunchedEffect(roomId, myName, myAvatar) {
        viewModel.initChatRoom(
            roomId = roomId,
            partnerId = partnerId,
            partnerName = partnerName,
            partnerAvatar = partnerAvatar,
            myName = myName,
            myAvatar = myAvatar
        )
    }

    var pendingProduct by remember(roomId) {
        mutableStateOf(
            if (!initialProductId.isNullOrBlank() && initialProductName != null && initialProductPrice != null) {
                ChatMessage(
                    id = "draft_product",
                    senderId = currentUserId,
                    senderName = myName,
                    senderAvatar = myAvatar,
                    productId = initialProductId,
                    productName = initialProductName,
                    productPrice = initialProductPrice,
                    productImage = initialProductImage,
                    timestamp = System.currentTimeMillis()
                )
            } else null
        )
    }

    ChatContent(
        uiState = uiState,
        currentUserId = currentUserId,
        partnerName = partnerName,
        partnerAvatar = partnerAvatar,
        hasPhone = !partnerPhone.isNullOrBlank(),
        pendingProduct = pendingProduct,
        onBack = onBack,
        onCallClick = {
            partnerPhone?.let { phone ->
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                context.startActivity(intent)
            }
        },
        onInputTextChanged = viewModel::onInputTextChanged,
        onSendClicked = {
            viewModel.sendMessageWithOptionalProduct(
                product = pendingProduct,
                senderName = myName,
                senderAvatar = myAvatar
            )
            pendingProduct = null
        },
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
    pendingProduct: ChatMessage?,
    onBack: () -> Unit,
    onCallClick: () -> Unit,
    onInputTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onProductClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // Cuộn xuống dòng cuối khi có tin nhắn mới hoặc có thẻ tạm
    val totalCount = uiState.messages.size + (if (pendingProduct != null) 1 else 0)
    LaunchedEffect(totalCount) {
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount)
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
                                text = "Người bán",
                                fontSize = 12.sp,
                                color = Color.Gray
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
                verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.Bottom)
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

                if (pendingProduct != null) {
                    item {
                        ProductMessageCard(
                            message = pendingProduct,
                            onProductClick = { onProductClick(pendingProduct.productId ?: "") }
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
            val displayAvatar = message.senderAvatar ?: partnerAvatar ?: "https://via.placeholder.com/150"
            AsyncImage(
                model = displayAvatar,
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
                    color = Color.Black,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
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
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        color = Color.White,
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isImeVisible) {
                        Modifier.padding(bottom = max(navBarHeight, 24.dp))
                    } else {
                        Modifier.navigationBarsPadding()
                    }
                )
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
            senderId = "user_2",
            text = "Chào bạn! Shop có thể hỗ trợ gì cho bạn?",
            timestamp = 1714500100000L
        )
    )

    val sampleDraftProduct = ChatMessage(
        id = "draft_prod",
        productId = "prod_001",
        productName = "Sầu riêng Ri6 Chín Cây (Loại 1)",
        productPrice = 120000.0,
        productImage = "https://via.placeholder.com/150",
        senderId = "user_1",
        timestamp = System.currentTimeMillis()
    )

    ChatContent(
        uiState = ChatUiState(
            messages = sampleMessages,
            inputText = "Quả này còn hàng giao liền không shop?",
            isLoading = false
        ),
        currentUserId = "user_1",
        partnerName = "Nông Trại Hữu Cơ Ba Vì",
        partnerAvatar = null,
        hasPhone = true,
        pendingProduct = sampleDraftProduct,
        onBack = {},
        onCallClick = {},
        onInputTextChanged = {},
        onSendClicked = {},
        onProductClick = {}
    )
}