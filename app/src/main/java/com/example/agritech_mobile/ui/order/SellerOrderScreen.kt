package com.example.agritech_mobile.ui.order

import android.widget.Toast
import androidx.annotation.StringRes
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
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

enum class OrderStatus(@StringRes val titleRes: Int) {
    PENDING(R.string.pending),
    CONFIRMED(R.string.confirmed),
    SHIPPING(R.string.shipping),
    COMPLETED(R.string.completed),
    CANCELLED(R.string.cancelled)
}

data class SellerOrderItem(
    val id: String,
    val name: String,
    val quantityDesc: String,
    val itemTotal: Double,
    val imageUrl: String
)

data class SellerOrder(
    val orderId: String,
    val orderCode: String,
    val buyerName: String,
    val buyerAvatar: String,
    val dateTime: String,
    val status: OrderStatus,
    val items: List<SellerOrderItem>,
    val totalAmount: Double
)

@Composable
fun SellerOrdersScreen(
    viewModel: OrderViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val orderState by viewModel.orderState.collectAsState()

    var orders by remember { mutableStateOf<List<SellerOrder>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.getSellerOrders()
    }

    LaunchedEffect(orderState) {
        when (val state = orderState) {
            is OrderState.Loading -> {
                isLoading = true
            }

            is OrderState.OrderItemSuccess -> {
                isLoading = false
                orders = state.items?.map { res ->
                    SellerOrder(
                        orderId = res.orderId ?: "",
                        orderCode = "#ORD-${res.orderId?.takeLast(5)?.uppercase() ?: "UNKNOWN"}",
                        buyerName = res.buyerName ?: "Khách hàng",
                        buyerAvatar = "https://ui-avatars.com/api/?name=${
                            res.buyerName?.replace(
                                " ",
                                "+"
                            ) ?: "User"
                        }&background=random",
                        dateTime = res.orderDate ?: "",
                        status = try {
                            OrderStatus.valueOf(res.status ?: "PENDING")
                        } catch (e: Exception) {
                            OrderStatus.PENDING
                        },
                        items = res.orderItems?.map { item ->
                            SellerOrderItem(
                                id = item.productName ?: "",
                                name = item.productName ?: "",
                                quantityDesc = "${item.quantity ?: 0.0} ${item.unit ?: ""} x ${item.price ?: 0.0}đ",
                                itemTotal = (item.quantity ?: 0.0) * (item.price ?: 0.0),
                                imageUrl = item.thumbnail ?: ""
                            )
                        } ?: emptyList(),
                        totalAmount = res.totalRevenueFromThisOrder ?: 0.0
                    )
                } ?: emptyList()
            }

            is OrderState.OrderBuyerItemSuccess -> {
                isLoading = false
            }

            is OrderState.Success -> {
                isLoading = false
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.getSellerOrders()
            }

            is OrderState.Error -> {
                isLoading = false
                Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
            }

            is OrderState.Idle -> {
                isLoading = false
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SellerOrdersContent(
            orders = orders,
            onUpdateOrderStatus = { orderId, newStatus ->
                viewModel.updateOrderStatus(orderId, newStatus.name)
            },
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
fun SellerOrdersContent(
    orders: List<SellerOrder>,
    onUpdateOrderStatus: (String, OrderStatus) -> Unit,
) {
    val formatter = DecimalFormat("#,###")

    val tabs = OrderStatus.values()
    val pagerState = rememberPagerState(pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFFF8FAF9),
        topBar = {
            SellerOrdersTopBar(itemCount = orders.size)
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
                    .background(Color(0xFFF1F8F4))
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                itemsIndexed(tabs) { index, tab ->
                    val isSelected = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF1B5E20) else Color(0xFFE0E0E0))
                            .clickable {
                                coroutineScope.launch { pagerState.animateScrollToPage(index) }
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(id = tab.titleRes),
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) Color.White else Color(0xFF616161)
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
                            EndOfListIndicator(text = stringResource(R.string.no_order))
                        }
                    } else {
                        items(filteredOrders) { order ->
                            SellerOrderCard(
                                order = order,
                                formatter = formatter,
                                onUpdateStatus = { newStatus ->
                                    onUpdateOrderStatus(order.orderId, newStatus)
                                }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(24.dp))
                            EndOfListIndicator(text = stringResource(R.string.end_order))
                            Spacer(modifier = Modifier.height(80.dp))
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerOrdersTopBar(itemCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.order_title, itemCount),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = {}) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notification",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SellerOrderCard(
    order: SellerOrder,
    formatter: DecimalFormat,
    onUpdateStatus: (OrderStatus) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AsyncImage(
                    model = order.buyerAvatar,
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = order.buyerName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF212121)
                    )
                    Text(
                        text = "${order.orderCode} • ${order.dateTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF757575)
                    )
                }

                if (order.status == OrderStatus.PENDING) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1B5E20))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.new_order),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                            .size(50.dp)
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
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.quantityDesc,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF757575)
                        )
                    }
                    Text(
                        text = "${formatter.format(item.itemTotal).replace(',', '.')}đ",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF212121)
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.total_amount),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF616161)
                )
                Text(
                    text = "${formatter.format(order.totalAmount).replace(',', '.')}đ",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF1B5E20)
                )
            }

            if (order.status != OrderStatus.COMPLETED && order.status != OrderStatus.CANCELLED) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {

                    if (order.status == OrderStatus.PENDING || order.status == OrderStatus.CONFIRMED) {
                        Button(
                            onClick = { onUpdateStatus(OrderStatus.CANCELLED) },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.cancel),
                                color = Color(0xFF212121),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val actionText = when (order.status) {
                        OrderStatus.PENDING -> stringResource(R.string.but_pending)
                        OrderStatus.CONFIRMED -> stringResource(R.string.but_confirm)
                        OrderStatus.SHIPPING -> stringResource(R.string.but_shipping)
                        else -> ""
                    }
                    val nextStatus = when (order.status) {
                        OrderStatus.PENDING -> OrderStatus.CONFIRMED
                        OrderStatus.CONFIRMED -> OrderStatus.SHIPPING
                        OrderStatus.SHIPPING -> OrderStatus.COMPLETED
                        else -> order.status
                    }

                    Button(
                        onClick = { onUpdateStatus(nextStatus) },
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = actionText,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EndOfListIndicator(text: String) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            painter = painterResource(R.drawable.farm_svgrepo_com),
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
            color = Color(0xFF9E9E9E)
        )
    }
}

@Preview(showBackground = true, locale = "vi", device = "spec:width=411dp,height=1000dp,dpi=420")
@Composable
fun SellerOrdersScreenPreview() {
    AgritechTheme {
        SellerOrdersContent(
            orders = listOf(
                SellerOrder(
                    orderId = "1",
                    orderCode = "#ORD-12345",
                    buyerName = "Trần Văn An",
                    buyerAvatar = "https://ui-avatars.com/api/?name=Tran+Van+An",
                    dateTime = "10:30, 24/05",
                    status = OrderStatus.PENDING,
                    items = listOf(
                        SellerOrderItem(
                            "i1",
                            "Cải thìa hữu cơ",
                            "2.0 kg x 35.000đ",
                            70000.0,
                            "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?q=80&w=200"
                        )
                    ),
                    totalAmount = 70000.0
                )
            ),
            onUpdateOrderStatus = { _, _ -> },
        )
    }
}