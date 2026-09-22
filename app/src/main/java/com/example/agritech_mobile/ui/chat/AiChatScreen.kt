package com.example.agritech_mobile.ui.chat

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.agritech_mobile.ui.dashboard.AiChatUiState
import com.example.agritech_mobile.ui.dashboard.AiViewModel
import java.text.NumberFormat
import java.util.Locale

data class AiInspectionData(
    val title: String,
    val summary: String,
    val brixScore: String,
    val brixDesc: String,
    val seedRatio: String,
    val seedDesc: String,
    val gardenName: String,
    val harvestCode: String,
    val comboName: String,
    val comboPrice: String,
    val originalPrice: String
)

data class AiMessage(
    val id: String,
    val text: String,
    val isBot: Boolean,
    val timestamp: String,
    val subNote: String? = null,
    val inspectionData: AiInspectionData? = null
)

@Composable
fun AiChatScreen(
    productId: String,
    productName: String,
    productPrice: Double = 0.0,
    productImage: String? = null,
    onBackClick: () -> Unit = {},
    onProductClick: (String) -> Unit = {},
    viewModel: AiViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(productId, productName) {
        viewModel.initSession(productId, productName)
    }

    AiChatContent(
        uiState = uiState,
        productId = productId,
        productName = productName,
        productPrice = productPrice,
        productImage = productImage,
        onBackClick = onBackClick,
        onProductClick = onProductClick,
        onRefreshClick = { viewModel.initSession(productId, productName) },
        onInputTextChanged = viewModel::onInputTextChanged,
        onSendClicked = { viewModel.sendStreamMessage(productName) },
        onChipClicked = { chipText -> viewModel.sendStreamMessage(productName, chipText) }
    )
}

@Composable
fun AiChatContent(
    uiState: AiChatUiState,
    productId: String,
    productName: String,
    productPrice: Double,
    productImage: String?,
    onBackClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onRefreshClick: () -> Unit,
    onInputTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit,
    onChipClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    var userScrolledUp by remember { mutableStateOf(false) }

    val isAtBottom by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            totalItems == 0 || lastVisibleItem >= totalItems - 2
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            userScrolledUp = !isAtBottom
        }
    }

    LaunchedEffect(isAtBottom) {
        if (isAtBottom) {
            userScrolledUp = false
        }
    }

    LaunchedEffect(uiState.messages.size) {
        userScrolledUp = false
        if (uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.size)
        }
    }

    LaunchedEffect(uiState.messages.lastOrNull()?.text) {
        if (!userScrolledUp && !listState.isScrollInProgress && uiState.messages.isNotEmpty()) {
            listState.scrollToItem(uiState.messages.size)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            AiChatTopBar(
                onBackClick = onBackClick,
                onRefreshClick = onRefreshClick
            )
        },
        bottomBar = {
            AiChatBottomBar(
                inputText = uiState.inputText,
                isSending = uiState.isStreaming,
                onTextChanged = onInputTextChanged,
                onSendClicked = onSendClicked
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF6F8F7))
        ) {
            PinnedProductBanner(
                productName = productName,
                productPrice = productPrice,
                productImage = productImage,
                onProductClick = { onProductClick(productId) },
                onChipClick = onChipClicked
            )

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(top = 10.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                items(uiState.messages, key = { it.id }) { msg ->
                    if (msg.isBot) {
                        if (msg.text.isEmpty() && uiState.isStreaming) {
                            BotTypingIndicator()
                        } else {
                            BotMessageItem(message = msg)
                        }
                    } else {
                        UserMessageItem(message = msg)
                    }
                }

                item(key = "bottom_anchor") {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
        }
    }
}

@Composable
fun BotTypingIndicator() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFF2E7D32)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SupportAgent,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF1B5E20)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AgriBot đang suy nghĩ...",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatTopBar(
    onBackClick: () -> Unit,
    onRefreshClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF2E7D32)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.SupportAgent,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "AgriBot",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                    }
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
            }
        },
        actions = {
            IconButton(onClick = onRefreshClick) {
                Icon(Icons.Default.Refresh, contentDescription = "Làm mới", tint = Color.Gray)
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun PinnedProductBanner(
    productName: String,
    productPrice: Double,
    productImage: String?,
    onProductClick: () -> Unit,
    onChipClick: (String) -> Unit
) {
    val formattedPrice = remember(productPrice) {
        if (productPrice > 0) {
            NumberFormat.getNumberInstance(Locale("vi", "VN")).format(productPrice) + " đ"
        } else {
            "Liên hệ"
        }
    }

    Surface(
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onProductClick() },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ĐANG TƯ VẤN",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Text(
                        text = productName.ifBlank { "Nông sản sạch" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = onProductClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Xem chi tiết", fontSize = 11.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SuggestionChipItem(text = "✔ Mẹo bảo quản tươi lâu?") {
                    onChipClick("Mẹo bảo quản $productName được tươi ngon lâu nhất?")
                }
                SuggestionChipItem(text = "💡 Món ngon chế biến từ món này?") {
                    onChipClick("Gợi ý các món ngon dễ nấu từ $productName?")
                }
                SuggestionChipItem(text = "🛡 Giá trị dinh dưỡng ra sao?") {
                    onChipClick("Giá trị dinh dưỡng và lợi ích sức khoẻ của $productName?")
                }
            }
        }
    }
}

@Composable
fun SuggestionChipItem(text: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFFF1F8E9),
        border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = Color(0xFF1B5E20),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

@Composable
fun BotMessageItem(
    message: AiMessage,
    onOrderComboClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFF2E7D32)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.SupportAgent,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Column(modifier = Modifier.widthIn(max = 310.dp)) {
            Surface(
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = Color.White,
                shadowElevation = 1.dp
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    if (message.inspectionData != null) {
                        val report = message.inspectionData
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(report.title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF1B5E20))
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(message.text, fontSize = 13.sp, color = Color(0xFF333333), lineHeight = 19.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricBox(
                                title = "ĐỘ NGỌT & CƠM",
                                value = report.brixScore,
                                note = report.brixDesc,
                                modifier = Modifier.weight(1f)
                            )
                            MetricBox(
                                title = "TỶ LỆ CHUẨN",
                                value = report.seedRatio,
                                note = report.seedDesc,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF9F9F9),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color.Gray)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(report.gardenName, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(report.harvestCode, fontSize = 10.sp, color = Color.Gray)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(report.summary, fontSize = 12.sp, color = Color(0xFF333333), lineHeight = 17.sp)

                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingBag,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Đề xuất mẻ nông sản ngon", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color(0xFF1B5E20))
                                    }
                                    Text("Ưu tiên", fontSize = 9.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(6.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text(report.comboName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        Text("Tặng kèm ưu đãi vận chuyển", fontSize = 10.sp, color = Color.Gray)
                                    }
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(report.comboPrice, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFF1B5E20))
                                        Text(report.originalPrice, fontSize = 10.sp, color = Color.Gray)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.weight(1f).height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(12.dp), tint = Color.DarkGray)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Liên hệ", fontSize = 11.sp, color = Color.DarkGray)
                                    }

                                    Button(
                                        onClick = onOrderComboClick,
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(0.dp),
                                        modifier = Modifier.weight(1.2f).height(32.dp)
                                    ) {
                                        Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Đặt giữ ngay", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    } else {
                        Text(
                            text = message.text,
                            fontSize = 13.sp,
                            color = Color(0xFF212121),
                            lineHeight = 19.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${message.timestamp} ${if (message.subNote != null) "• ${message.subNote}" else ""}",
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
fun MetricBox(title: String, value: String, note: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF9F9F9),
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
            Spacer(modifier = Modifier.height(2.dp))
            Text(note, fontSize = 9.sp, color = Color.DarkGray)
        }
    }
}

@Composable
fun UserMessageItem(message: AiMessage) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier.widthIn(max = 280.dp),
            horizontalAlignment = Alignment.End
        ) {
            Surface(
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
                color = Color(0xFF1B5E20)
            ) {
                Text(
                    text = message.text,
                    color = Color.White,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(12.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = message.timestamp,
                fontSize = 10.sp,
                color = Color.Gray,
                modifier = Modifier.padding(end = 4.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B5E20)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AiChatBottomBar(
    inputText: String,
    isSending: Boolean,
    onTextChanged: (String) -> Unit,
    onSendClicked: () -> Unit
) {
    val isImeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val navBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isImeVisible) {
                        Modifier.padding(bottom = max(navBarHeight, 24.dp))
                    } else {
                        Modifier.navigationBarsPadding()
                    }
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {

                TextField(
                    value = inputText,
                    onValueChange = onTextChanged,
                    placeholder = { Text("Hỏi AgriBot về sản phẩm...", fontSize = 13.sp, color = Color.Gray) },
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
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onSendClicked,
                    enabled = inputText.isNotBlank() && !isSending,
                    modifier = Modifier
                        .size(42.dp)
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

            Text(
                text = "Bảo chứng bởi AgriTech • Kết nối dữ liệu nông nghiệp thời gian thực",
                fontSize = 9.sp,
                color = Color.LightGray,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = 6.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun AiChatScreenPreview() {
    val sampleMessages = listOf(
        AiMessage(
            id = "1",
            text = "Dạ chào bạn! Tôi là AgriBot ✨\n\nTôi là trợ lý AI của ứng dụng này. Bạn đang quan tâm đến Sầu riêng Ri6 Cái Mơn đúng không ạ? Bạn cần tư vấn gì về sản phẩm này ạ?",
            isBot = true,
            timestamp = "09:42",
            subNote = "AgriTech 3.5 AI"
        ),
        AiMessage(
            id = "2",
            text = "Sầu riêng này bảo quản như thế nào thì tươi ngon lâu nhất vậy bạn?",
            isBot = false,
            timestamp = "09:43"
        ),
        AiMessage(
            id = "3",
            text = "Để sầu riêng Ri6 tươi ngon lâu, bạn nên để nơi khô ráo, thoáng mát nếu quả chưa chín hoàn toàn. Khi quả đã chín nứt gai nhẹ, bạn nên khui múi, bọc màng thực phẩm kín hoặc cho vào hộp đậy nắp kín rồi để ngăn mát (2-3 ngày) hoặc cấp đông dùng dần nhé!",
            isBot = true,
            timestamp = "09:44",
            subNote = "Cẩm nang Nông sản"
        )
    )

    MaterialTheme {
        AiChatContent(
            uiState = AiChatUiState(
                messages = sampleMessages,
                inputText = "",
                isStreaming = false
            ),
            productId = "p1",
            productName = "Sầu riêng Ri6 Cái Mơn",
            productPrice = 120000.0,
            productImage = null,
            onBackClick = {},
            onProductClick = {},
            onRefreshClick = {},
            onInputTextChanged = {},
            onSendClicked = {},
            onChipClicked = {}
        )
    }
}