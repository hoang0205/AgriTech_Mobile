package com.example.agritech_mobile.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import coil.compose.SubcomposeAsyncImage
import com.example.agritech_mobile.data.remote.dto.StatisticsOrderDto
import com.example.agritech_mobile.data.remote.dto.StatisticsOrderItemDto
import java.math.BigDecimal
import com.example.agritech_mobile.ui.theme.AgritechTheme

@Composable
internal fun StatisticsOrderCard(
    order: StatisticsOrderDto,
    isProfit: Boolean,
    onEditProduct: ((String) -> Unit)? = null,
    isSpending: Boolean = false
) {
    var expanded by remember(order.orderId) { mutableStateOf(false) }
    StatisticsCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Đơn #${order.orderId}", Modifier.weight(1f), style = MaterialTheme.typography.titleSmall)
            Text(order.orderDate.substringBefore('T'), style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (isSpending) {
            StatisticsBuyerRow(order.sellerName, order.sellerAvatar, label = "Người bán")
        } else {
            StatisticsBuyerRow(order.buyerName, order.buyerAvatar)
        }
        val visibleItems = if (expanded) order.items else order.items.take(1)
        visibleItems.forEach { product ->
            StatisticsProductRow(product, isProfit,
                onEdit = onEditProduct?.takeIf { expanded }?.let { edit -> { edit(product.productId) } })
        }
        if (!expanded && order.items.size > 1) {
            Text("+${order.items.size - 1} sản phẩm khác", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        val contactPhone = if (isSpending) order.sellerPhone else order.buyerPhone
        if (expanded && !contactPhone.isNullOrBlank()) {
            Text("Liên hệ ${if (isSpending) "người bán" else "đơn"}: $contactPhone", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        StatisticsValueRow(if (isProfit) "Lãi gộp" else "Tổng tiền", order.amount)
        if (order.missingCostItems > 0) {
            Text("${order.missingCostItems} sản phẩm chưa nhập vốn, tạm tính 0 đ", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
            Text(if (expanded) "Thu gọn" else "Chi tiết đơn", style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
internal fun StatisticsBuyerRow(name: String?, avatar: String?, label: String = "Người mua") {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SubcomposeAsyncImage(model = avatar, contentDescription = null,
            modifier = Modifier.size(28.dp).clip(CircleShape), contentScale = ContentScale.Crop,
            loading = { BuyerPlaceholder() }, error = { BuyerPlaceholder() })
        Text("$label: ${name?.takeIf { it.isNotBlank() } ?: "Chưa có thông tin"}",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BuyerPlaceholder() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun StatisticsProductRow(product: StatisticsOrderItemDto, isProfit: Boolean = false,
    showAmount: Boolean = true, onEdit: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SubcomposeAsyncImage(model = product.imageUrl, contentDescription = "Ảnh ${product.productName}",
            modifier = Modifier.size(60.dp).clip(RoundedCornerShape(10.dp)), contentScale = ContentScale.Crop,
            loading = { ProductPlaceholder() }, error = { ProductPlaceholder() })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(product.productName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            val quantity = BigDecimal.valueOf(product.quantity).stripTrailingZeros().toPlainString().replace('.', ',')
            Text("$quantity ${product.unit} × ${statisticsMoney(product.unitPrice)}",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (showAmount) Text(product.amount?.let { (if (isProfit) "Lãi gộp: " else "") + statisticsMoney(it) } ?: "Chưa có giá vốn",
                style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                color = if ((product.amount ?: 0.0) < 0) MaterialTheme.colorScheme.error else StatisticsGreen)
            if (onEdit != null) TextButton(onClick = onEdit, contentPadding = PaddingValues(0.dp)) {
                Text("Sửa sản phẩm · giá vốn", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ProductPlaceholder() {
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center) {
        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(name = "Đơn thống kê", showBackground = true, widthDp = 360)
@Preview(name = "Đơn thống kê - chữ lớn", showBackground = true, widthDp = 360, fontScale = 1.5f)
@Composable
private fun StatisticsOrderPreview() {
    AgritechTheme {
        StatisticsOrderCard(
            StatisticsOrderDto(orderId = 123, orderDate = "2026-10-22T10:00:00", amount = 175000.0,
                missingCostItems = 0, buyerName = "Nguyễn Minh Anh",
                items = listOf(
                    StatisticsOrderItemDto("cam", "Cam sành", 2.0, "kg", 35000.0, 70000.0),
                    StatisticsOrderItemDto("ca", "Cá rô phi tươi", 1.0, "kg", 105000.0, 105000.0)
                )), isProfit = false
        )
    }
}
