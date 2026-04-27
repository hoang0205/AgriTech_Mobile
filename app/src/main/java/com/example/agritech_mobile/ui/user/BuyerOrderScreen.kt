package com.example.agritech_mobile.ui.order

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.ui.theme.AgritechTheme
import kotlinx.coroutines.launch
import java.text.DecimalFormat

data class BuyerOrderItem(
    val id: String,
    val name: String,
    val quantityDesc: String,
    val itemTotal: Double,
    val imageUrl: String
)

data class BuyerOrder(
    val orderId: String,
    val orderCode: String,
    val shopName: String,
    val shopAvatar: String,
    val dateTime: String,
    val status: OrderStatus,
    val items: List<BuyerOrderItem>,
    val totalAmount: Double
)

@Composable
fun BuyerOrdersScreen(
    initialStatus: String = "ALL",
    onBackClick: () -> Unit,
    viewModel: OrderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val orderState by viewModel.orderState.collectAsState()

    var orders by remember { mutableStateOf<List<BuyerOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    val defaultShopName = stringResource(R.string.buyer_order_shop_default)

    LaunchedEffect(Unit) {
        viewModel.getBuyerOrders()
    }

    LaunchedEffect(orderState) {
        when (val state = orderState) {
            is OrderState.Loading -> isLoading = true

            is OrderState.OrderBuyerItemSuccess -> {
                isLoading = false
                orders = state.items.map { res ->
                    val firstItem = res.items?.firstOrNull()
                    val shopName = firstItem?.farmerName ?: defaultShopName

                    val orderIdStr = res.orderId?.toString() ?: ""

                    BuyerOrder(
                        orderId = orderIdStr,
                        orderCode = "#ORD-${orderIdStr.takeLast(5).uppercase()}",
                        shopName = shopName,
                        shopAvatar = "https://ui-avatars.com/api/?name=${
                            shopName.replace(
                                " ",
                                "+"
                            )
                        }&background=random",
                        dateTime = res.orderDate ?: "",
                        status = try {
                            OrderStatus.valueOf(res.status ?: "PENDING")
                        } catch (e: Exception) {
                            OrderStatus.PENDING
                        },
                        items = res.items?.map { item ->
                            BuyerOrderItem(
                                id = item.productId ?: "",
                                name = item.productName ?: "",
                                quantityDesc = "${item.quantity ?: 0.0} ${item.unit ?: ""} x ${item.price ?: 0.0}đ",
                                itemTotal = (item.quantity ?: 0.0) * (item.price ?: 0.0),
                                imageUrl = item.thumbnail ?: ""
                            )
                        } ?: emptyList(),
                        totalAmount = res.totalAmount ?: 0.0
                    )
                }
            }

            is OrderState.Success -> {
                isLoading = false
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.getBuyerOrders()
            }

            is OrderState.Error -> {
                isLoading = false
                Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
            }

            else -> isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BuyerOrdersContent(
            orders = orders,
            initialStatus = initialStatus,
            onBackClick = onBackClick,
            onCancelOrder = { orderId ->
                viewModel.updateOrderStatus(orderId, OrderStatus.CANCELLED.name)
            }
        )

        if (isLoading && orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun BuyerOrdersContent(
    orders: List<BuyerOrder>,
    initialStatus: String,
    onBackClick: () -> Unit,
    onCancelOrder: (String) -> Unit
) {
    val formatter = DecimalFormat("#,###")
    val tabs = OrderStatus.values()

    val initialPageIndex = tabs.indexOfFirst { it.name == initialStatus }.takeIf { it >= 0 } ?: 0
    val pagerState = rememberPagerState(initialPage = initialPageIndex, pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFFF8FAF9),
        topBar = {
            BuyerOrdersTopBar(onBackClick = onBackClick)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(tabs) { index, tab ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF1B5E20) else Color(0xFFF5F5F5))
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(id = tab.titleRes),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else Color(0xFF757575)
                        )
                    }
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val currentStatus = tabs[page]
                val filteredOrders = orders.filter { it.status == currentStatus }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (filteredOrders.isEmpty()) {
                        item {
                            Spacer(modifier = Modifier.height(100.dp))
                            EndOfListIndicator(text = stringResource(R.string.buyer_order_empty))
                        }
                    } else {
                        items(filteredOrders) { order ->
                            BuyerOrderCard(
                                order = order,
                                formatter = formatter,
                                onCancelClick = { onCancelOrder(order.orderId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuyerOrdersTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.buyer_order_title),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20),
                fontSize = 18.sp
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1B5E20)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
    )
}

@Composable
fun BuyerOrderCard(
    order: BuyerOrder,
    formatter: DecimalFormat,
    onCancelClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFF1B5E20))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = order.shopName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF212121),
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(id = order.status.titleRes),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1B5E20)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            order.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.name,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF212121)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = item.quantityDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF757575)
                        )
                    }
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.buyer_order_total),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF616161)
                )
                Text(
                    text = "${formatter.format(order.totalAmount).replace(',', '.')}đ",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF1B5E20)
                )
            }

            if (order.status == OrderStatus.PENDING) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {},
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFF0F0)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFE57373))
                ) {
                    Text(
                        stringResource(R.string.buyer_order_cancel_btn),
                        color = Color(0xFFD32F2F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "vi", device = "spec:width=411dp,height=1000dp,dpi=420")
@Composable
fun BuyerOrdersScreenPreview() {
    AgritechTheme {
        val mockOrders = listOf(
            BuyerOrder(
                orderId = "1",
                orderCode = "#ORD-99887",
                shopName = "Nông trại Bác Ba Phi",
                shopAvatar = "https://ui-avatars.com/api/?name=Bac+Ba+Phi",
                dateTime = "14:20, 20/04/2026",
                status = OrderStatus.PENDING,
                items = listOf(
                    BuyerOrderItem(
                        id = "i1",
                        name = "Cà chua thân gỗ",
                        quantityDesc = "2.0 kg x 45.000đ",
                        itemTotal = 90000.0,
                        imageUrl = "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?q=80&w=200"
                    )
                ),
                totalAmount = 90000.0
            ),
            BuyerOrder(
                orderId = "2",
                orderCode = "#ORD-11223",
                shopName = "Vựa trái cây Miền Tây",
                shopAvatar = "https://ui-avatars.com/api/?name=Mien+Tay",
                dateTime = "09:00, 18/04/2026",
                status = OrderStatus.COMPLETED,
                items = listOf(
                    BuyerOrderItem(
                        id = "i2",
                        name = "Sầu riêng Ri6",
                        quantityDesc = "1.0 trái x 350.000đ",
                        itemTotal = 350000.0,
                        imageUrl = "https://images.unsplash.com/photo-1633324673678-ebf13b65f7c0?q=80&w=200"
                    ),
                    BuyerOrderItem(
                        id = "i3",
                        name = "Dừa sáp Trà Vinh",
                        quantityDesc = "2.0 trái x 120.000đ",
                        itemTotal = 240000.0,
                        imageUrl = "https://images.unsplash.com/photo-1595085189033-0c3098f98d7f?q=80&w=200"
                    )
                ),
                totalAmount = 590000.0
            )
        )

        BuyerOrdersContent(
            orders = mockOrders,
            initialStatus = "PENDING",
            onBackClick = {},
            onCancelOrder = {}
        )
    }
}