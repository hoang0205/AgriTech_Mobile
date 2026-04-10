package com.example.agritech_mobile.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.agritech_mobile.ui.theme.AgritechTheme
import java.text.DecimalFormat

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
    val statusTag: String?, // VD: "MỚI"
    val items: List<SellerOrderItem>,
    val totalAmount: Double
)

@Composable
fun SellerOrdersScreen(
    // viewModel: OrderViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableStateOf("Chờ xác nhận") }

    val dummyOrders = listOf(
        SellerOrder(
            orderId = "1",
            orderCode = "#ORD-12345",
            buyerName = "Trần Văn An",
            buyerAvatar = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?q=80&w=200",
            dateTime = "10:30, 24/05",
            statusTag = "MỚI",
            items = listOf(
                SellerOrderItem(
                    "i1",
                    "Cải thìa hữu cơ",
                    "2.0 kg x 35.000đ",
                    70000.0,
                    "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?q=80&w=200"
                ),
                SellerOrderItem(
                    "i2",
                    "Cà rốt Đà Lạt",
                    "1.5 kg x 28.000đ",
                    42000.0,
                    "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?q=80&w=200"
                ) // Đổi link ảnh thực tế sau
            ),
            totalAmount = 112000.0
        ),
        SellerOrder(
            orderId = "2",
            orderCode = "#ORD-12348",
            buyerName = "Lê Thị Mai",
            buyerAvatar = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?q=80&w=200",
            dateTime = "09:15, 24/05",
            statusTag = null,
            items = listOf(
                SellerOrderItem(
                    "i3",
                    "Chuối Laba chín",
                    "1.0 nải x 45.000đ",
                    45000.0,
                    "https://images.unsplash.com/photo-1481349518771-20055b2a7b24?q=80&w=200"
                )
            ),
            totalAmount = 45000.0
        )
    )

    SellerOrdersContent(
        orders = dummyOrders,
        selectedTab = selectedTab,
        onTabSelected = { selectedTab = it },
        onCancelOrder = { /* TODO: Gọi API Hủy đơn */ },
        onConfirmOrder = { /* TODO: Gọi API Xác nhận đơn */ },
        onMenuClick = { /* TODO: Mở Drawer hoặc Action */ },
        onSearchClick = { /* TODO: Mở màn tìm kiếm */ }
    )
}

@Composable
fun SellerOrdersContent(
    orders: List<SellerOrder>,
    selectedTab: String,
    onTabSelected: (String) -> Unit,
    onCancelOrder: (String) -> Unit,
    onConfirmOrder: (String) -> Unit,
    onMenuClick: () -> Unit,
    onSearchClick: () -> Unit
) {
    val formatter = DecimalFormat("#,###")

    Scaffold(
        containerColor = Color(0xFFF8FAF9),
        topBar = {
            SellerOrdersTopBar(onMenuClick, onSearchClick)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            OrderFilterTabs(selectedTab = selectedTab, onTabSelected = onTabSelected)

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(orders) { order ->
                    SellerOrderCard(
                        order = order,
                        formatter = formatter,
                        onCancelClick = { onCancelOrder(order.orderId) },
                        onConfirmClick = { onConfirmOrder(order.orderId) }
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(24.dp))
                    EndOfListIndicator()
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerOrdersTopBar(onMenuClick: () -> Unit, onSearchClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Quản lý đơn hàng",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1B5E20)
            )
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu", tint = Color(0xFF1B5E20))
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF1B5E20))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF1F8F4)) // Màu nền header
    )
}

@Composable
fun OrderFilterTabs(selectedTab: String, onTabSelected: (String) -> Unit) {
    val tabs = listOf("Chờ xác nhận", "Đang xử lý", "Đã giao")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF1F8F4))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tabs.forEach { tab ->
            val isSelected = selectedTab == tab
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isSelected) Color(0xFF1B5E20) else Color(0xFFE0E0E0))
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = tab,
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = if (isSelected) Color.White else Color(0xFF616161)
                )
            }
        }
    }
}

@Composable
fun SellerOrderCard(
    order: SellerOrder,
    formatter: DecimalFormat,
    onCancelClick: () -> Unit,
    onConfirmClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // 1. Header (Info người mua)
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
                if (order.statusTag != null) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0xFF1B5E20))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = order.statusTag,
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
                    text = "Tổng thanh toán",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF616161)
                )
                Text(
                    text = "${formatter.format(order.totalAmount).replace(',', '.')}đ",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                    color = Color(0xFF1B5E20)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = onCancelClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEEEEEE)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Hủy đơn", color = Color(0xFF212121), fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onConfirmClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Xác nhận", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun EndOfListIndicator() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Eco,
            contentDescription = null,
            tint = Color(0xFFBDBDBD),
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "HẾT ĐƠN HÀNG CHỜ DUYỆT",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.sp),
            color = Color(0xFF9E9E9E)
        )
    }
}

@Preview(showBackground = true, locale = "vi", device = "spec:width=411dp,height=1000dp,dpi=420")
@Composable
fun SellerOrdersScreenPreview() {
    AgritechTheme {
        SellerOrdersScreen()
    }
}