package com.example.agritech_mobile.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.agritech_mobile.data.remote.dto.SellerFinanceSummary
import com.example.agritech_mobile.data.remote.dto.SellerOrderFinance
import com.example.agritech_mobile.ui.order.deliveryMethodLabel

@Composable
internal fun SellerFinanceCard(finance: SellerFinanceSummary, isProfit: Boolean = true, period: String? = null) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = StatisticsGreen)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                period?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = Color.White) }
                Text(if (isProfit) "Lợi nhuận hàng hóa" else "Doanh thu hàng hóa",
                    style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.85f))
                val primary = if (isProfit) finance.grossProfit else finance.merchandiseRevenue
                Text(primary?.let(::statisticsMoney) ?: "Chưa đủ dữ liệu",
                    style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                if (isProfit) Text("Tiền hàng − giá vốn · Chưa gồm ship",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
                val completed = finance.orders.count { it.status == "COMPLETED" }
                Text("$completed đơn hoàn thành",
                    style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            }
        }
        if (isProfit) {
            StatisticsCard {
                Text(if (finance.missingShippingCostOrders > 0) "Lãi / lỗ ship · Tạm tính" else "Lãi / lỗ ship", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                val margin = finance.shippingMargin
                Text(margin?.let { (if (it < 0) "Lỗ ship: " else "Lãi ship: ") + statisticsMoney(it.toDouble()) }
                    ?: "Chưa xác định",
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                    color = when {
                        margin == null -> MaterialTheme.colorScheme.onSurfaceVariant
                        margin < 0 -> MaterialTheme.colorScheme.error
                        else -> StatisticsGreen
                    })
                FinanceValue("Phí ship thu khách", finance.shippingCollected.toDouble())
                FinanceValue("Chi phí giao đã ghi", finance.recordedShippingCost.toDouble())
                if (finance.cancelledShippingCost > 0) {
                    FinanceValue("Trong đó: chi phí đơn hủy", finance.cancelledShippingCost.toDouble())
                }
                Text("Lãi / lỗ ship = phí ship thu khách − chi phí giao thực tế.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (finance.missingShippingCostOrders > 0) Text(
                    "${finance.missingShippingCostOrders} đơn chưa ghi chi phí giao: tạm tính hòa vốn ship (0 đ). Nhập chi phí để cập nhật lãi / lỗ.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StatisticsCard {
                FinanceValue(if (finance.missingShippingCostOrders > 0) "Tổng lợi nhuận tạm tính" else "Tổng lợi nhuận trước thuế", finance.profitAfterShipping, emphasize = true)
                Text("Lợi nhuận hàng hóa + lãi / lỗ ship. Chưa trừ thuế, phí thanh toán, phí sàn và chi phí khác.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (finance.missingCostItems > 0) Text(
                    "${finance.missingCostItems} mục chưa nhập giá vốn, tạm tính vốn 0 đ.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                    Text(if (expanded) "Thu gọn" else "Xem chi tiết")
                }
                if (expanded) {
                    FinanceValue("Tiền hàng", finance.merchandiseRevenue)
                    FinanceValue("Giá vốn", finance.costOfGoods)
                    FinanceValue("Lợi nhuận hàng hóa", finance.grossProfit)
                    FinanceValue("Lãi / lỗ ship", finance.shippingMargin?.toDouble())
                    Text("Đơn chưa ghi chi phí giao tạm tính lãi / lỗ ship 0 đ. Chi phí thực tế vẫn để trống. Đơn hủy đã ghi chi phí được tính vào lỗ ship.",
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
internal fun SellerFinanceOrderCard(order: SellerOrderFinance, onEditCost: () -> Unit, onEditProduct: (String) -> Unit = {}) {
    var expanded by remember(order.orderId) { mutableStateOf(false) }
    val products = order.items.orEmpty()
    StatisticsCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Text("Đơn #${order.orderId}", style = MaterialTheme.typography.titleSmall)
            Text(if (order.status == "CANCELLED") "Đã hủy" else "Hoàn thành",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${order.orderDate.substringBefore('T')} · ${deliveryMethodLabel(order.deliveryMethod)}",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StatisticsBuyerRow(order.buyerName, order.buyerAvatar)
        if (!expanded) {
            products.firstOrNull()?.let { StatisticsProductRow(it, showAmount = false) }
            if (products.size > 1) Text("+${products.size - 1} sản phẩm khác",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (order.missingCostItems > 0) {
            Text("Giá vốn chưa nhập: tạm tính 0 đ",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FinanceValue("Lợi nhuận hàng hóa", order.grossProfit, emphasize = true)
        FinanceValue(if (order.actualShippingCost == null) "Lãi / lỗ ship · Tạm tính" else "Lãi / lỗ ship", order.shippingMargin?.toDouble(), emphasize = true)
        FinanceValue(if (order.actualShippingCost == null) "Tổng lợi nhuận tạm tính" else "Tổng lợi nhuận trước thuế", order.profitAfterShipping, emphasize = true)
        if (order.actualShippingCost == null) Text("Chưa ghi chi phí giao: tạm tính hòa vốn ship (0 đ).",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (expanded) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            products.forEach { product ->
                StatisticsProductRow(product, showAmount = false, onEdit = { onEditProduct(product.productId) })
            }
            if (!order.buyerPhone.isNullOrBlank()) Text("Liên hệ đơn: ${order.buyerPhone}",
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FinanceValue("Tiền hàng", order.merchandiseRevenue)
            FinanceValue("Giá vốn", order.costOfGoods)
            FinanceValue("Lợi nhuận hàng hóa", order.grossProfit)
            FinanceValue("Phí giao thu khách", order.shippingCollected.toDouble())
            FinanceValue("Chi phí giao", order.actualShippingCost?.toDouble())
            FinanceValue("Lãi / lỗ ship", order.shippingMargin?.toDouble())
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(0.dp)) {
                Text(if (expanded) "Thu gọn" else "Chi tiết")
            }
            TextButton(onClick = onEditCost, contentPadding = PaddingValues(0.dp)) { Text("Chi phí giao") }
        }
    }
}

@Composable
private fun FinanceValue(label: String, value: Double?, emphasize: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value?.let(::statisticsMoney) ?: "Chưa xác định",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            color = if ((value ?: 0.0) < 0) MaterialTheme.colorScheme.error else if (emphasize) StatisticsGreen else MaterialTheme.colorScheme.onSurface)
    }
}
