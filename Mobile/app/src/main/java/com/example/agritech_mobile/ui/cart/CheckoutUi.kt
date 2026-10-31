package com.example.agritech_mobile.ui.cart

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.data.remote.dto.DeliveryStatus
import com.example.agritech_mobile.ui.order.CheckoutActionBar
import com.example.agritech_mobile.ui.order.CheckoutAddressCard
import com.example.agritech_mobile.ui.order.CheckoutPanel
import com.example.agritech_mobile.ui.order.CheckoutPriceRow
import com.example.agritech_mobile.ui.order.CheckoutStyle
import java.text.NumberFormat
import java.util.Locale

private fun checkoutMoney(value: Double) = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value) + "đ"

@Composable
fun CheckoutContent(uiState: CheckoutUiState, onBackClick: () -> Unit, onRetryDelivery: () -> Unit = {},
    onChangeAddressClick: () -> Unit, onPaymentMethodSelect: (PaymentMethodType) -> Unit, onPlaceOrderClick: () -> Unit) {
    val buttonText = when (uiState.selectedPaymentMethod) {
        PaymentMethodType.VNPAY -> "Thanh toán qua VNPay"
        else -> stringResource(R.string.checkout_place_order)
    }
    Scaffold(containerColor = CheckoutStyle.Background, topBar = { CheckoutTopBar(onBackClick) }, bottomBar = {
        Column {
            if (!uiState.stockAvailable && uiState.items.isNotEmpty()) {
                Box(Modifier.background(Color.White).padding(horizontal = 16.dp, vertical = 8.dp)) {
                    CheckoutMessage("Có sản phẩm hết hàng hoặc không đủ tồn kho. Vui lòng quay lại giỏ hàng.", error = true)
                }
            }
            CheckoutActionBar(buttonText,
                total = if (uiState.deliveryAvailable) checkoutMoney(uiState.totalAmount) else "Chưa xác định",
                onClick = onPlaceOrderClick,
                enabled = uiState.items.isNotEmpty() && uiState.stockAvailable && uiState.deliveryAvailable && !uiState.deliveryLoading)
        }
    }) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("Địa chỉ nhận hàng", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    TextButton(onClick = onChangeAddressClick, colors = ButtonDefaults.textButtonColors(contentColor = CheckoutStyle.Green)) {
                        Text(if (uiState.address.isBlank()) "Thêm địa chỉ" else "Thay đổi")
                    }
                }
                CheckoutAddressCard(uiState.userName, uiState.phone, uiState.address, onChangeAddressClick, selected = uiState.address.isNotBlank())
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Sản phẩm đã chọn", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Surface(color = CheckoutStyle.PaleGreen, shape = RoundedCornerShape(20.dp)) {
                        Text("${uiState.items.size} sản phẩm", Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            color = CheckoutStyle.Green, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            if (uiState.items.isEmpty()) item { CheckoutMessage("Chưa có sản phẩm để thanh toán. Vui lòng quay lại giỏ hàng.") }
            items(uiState.items, key = { it.id }) { CheckoutProductCard(it) }
            item { DeliveryInfoCard(uiState, onRetryDelivery) }
            item {
                Text("Phương thức thanh toán", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                PaymentMethodCard(Icons.Default.Payments, "COD", stringResource(R.string.checkout_cod),
                    uiState.selectedPaymentMethod == PaymentMethodType.COD, { onPaymentMethodSelect(PaymentMethodType.COD) })
                Spacer(Modifier.height(10.dp))
                PaymentMethodCard(Icons.Default.AccountBalance, stringResource(R.string.checkout_bank_transfer),
                    stringResource(R.string.checkout_bank_transfer_desc), uiState.selectedPaymentMethod == PaymentMethodType.BANK_TRANSFER,
                    { onPaymentMethodSelect(PaymentMethodType.BANK_TRANSFER) })
            }
            item { CheckoutPanel {
                Text("Chi tiết thanh toán", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                CheckoutPriceRow("Tiền hàng", checkoutMoney(uiState.subtotal))
                CheckoutPriceRow("Phí giao hàng", if (uiState.deliveryAvailable) checkoutMoney(uiState.shippingFee)
                    else if (uiState.deliveryLoading) "Đang tính…" else "Chưa xác định")
                if (uiState.discount > 0) CheckoutPriceRow("Giảm giá", "−${checkoutMoney(uiState.discount)}")
                HorizontalDivider(color = CheckoutStyle.Outline)
                CheckoutPriceRow("Tổng thanh toán", if (uiState.deliveryAvailable) checkoutMoney(uiState.totalAmount) else "—", true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payments, null, Modifier.size(20.dp), tint = CheckoutStyle.Green)
                    Text(when (uiState.selectedPaymentMethod) {
                        PaymentMethodType.COD -> "Thanh toán khi nhận hàng (COD)"
                        PaymentMethodType.VNPAY -> "Thanh toán qua VNPay"
                        PaymentMethodType.BANK_TRANSFER -> stringResource(R.string.checkout_bank_transfer)
                    }, style = MaterialTheme.typography.bodySmall, color = CheckoutStyle.Green)
                }
            } }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutTopBar(onBackClick: () -> Unit) {
    TopAppBar(title = { Text(stringResource(R.string.checkout_title), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge) },
        navigationIcon = { IconButton(onClick = onBackClick) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = CheckoutStyle.Green) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White))
}

@Composable
private fun CheckoutProductCard(item: CheckoutItem) {
    CheckoutPanel {
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(72.dp).clip(CheckoutStyle.ButtonShape).background(CheckoutStyle.PaleGreen), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.ShoppingBag, null, tint = CheckoutStyle.Green)
                if (item.imageUrl.isNotBlank()) AsyncImage(item.imageUrl, item.name, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(item.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                if (item.description.isNotBlank()) Text(item.description, style = MaterialTheme.typography.bodySmall, color = CheckoutStyle.Muted,
                    maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(item.quantityDesc, style = MaterialTheme.typography.bodySmall, color = CheckoutStyle.Muted)
                Text(checkoutMoney(item.price), color = CheckoutStyle.Green, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(icon: ImageVector, title: String, description: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = CheckoutStyle.CardShape,
        color = if (isSelected) CheckoutStyle.PaleGreen else Color.White,
        border = BorderStroke(1.dp, if (isSelected) CheckoutStyle.Green else CheckoutStyle.Outline)) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(24.dp), tint = CheckoutStyle.Green)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Text(description, style = MaterialTheme.typography.bodySmall, color = CheckoutStyle.Muted)
            }
            Icon(if (isSelected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked, null,
                Modifier.size(22.dp), tint = if (isSelected) CheckoutStyle.Green else CheckoutStyle.Muted)
        }
    }
}

@Composable
private fun CheckoutMessage(text: String, error: Boolean = false) {
    val tint = if (error) MaterialTheme.colorScheme.error else CheckoutStyle.Green
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else CheckoutStyle.PaleGreen,
        shape = CheckoutStyle.ButtonShape, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(Icons.Default.Info, null, Modifier.size(18.dp), tint = tint)
            Text(text, style = MaterialTheme.typography.bodySmall, color = tint)
        }
    }
}

@Composable
private fun DeliveryInfoCard(state: CheckoutUiState, onRetry: () -> Unit) {
    var showFeeInfo by remember { mutableStateOf(false) }
    CheckoutPanel {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.LocalShipping, null, Modifier.size(20.dp), tint = CheckoutStyle.Green)
            Spacer(Modifier.width(8.dp))
            Text("Giao hàng", Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showFeeInfo = true }) { Icon(Icons.Default.Info, "Cách tính phí giao hàng", tint = CheckoutStyle.Muted) }
        }
        if (state.deliveryLoading) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = CheckoutStyle.Green)
                Text("Đang tính phí giao hàng…", color = CheckoutStyle.Muted, style = MaterialTheme.typography.bodySmall)
            }
        } else {
            state.deliveryError?.let { error ->
                CheckoutMessage(error, true)
                TextButton(onClick = onRetry, colors = ButtonDefaults.textButtonColors(contentColor = CheckoutStyle.Green)) { Text("Tính lại phí giao hàng") }
            }
            state.deliveryQuotes.forEachIndexed { index, seller ->
                if (index > 0) HorizontalDivider(color = CheckoutStyle.Outline)
                val available = seller.status == DeliveryStatus.AVAILABLE && seller.shippingFee != null
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(seller.sellerName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val detail = listOfNotNull(seller.pickupArea?.takeIf { it.isNotBlank() },
                            seller.distanceKm?.let { "≈ ${String.format(Locale.forLanguageTag("vi-VN"), "%.1f", it)} km" }).joinToString(" · ")
                        if (detail.isNotBlank()) Text(detail, style = MaterialTheme.typography.bodySmall, color = CheckoutStyle.Muted)
                        if (!available) Text(seller.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    if (available) Text(checkoutMoney(requireNotNull(seller.shippingFee).toDouble()), color = CheckoutStyle.Green,
                        fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (state.deliveryQuotes.isNotEmpty() && !state.deliveryAvailable)
                Text("Đổi địa chỉ hoặc bỏ chọn shop chưa hỗ trợ giao.", style = MaterialTheme.typography.bodySmall, color = CheckoutStyle.Muted)
            if (state.deliveryQuotes.isEmpty() && state.deliveryError == null)
                Text("Chọn địa chỉ nhận hàng để xem phí giao hàng.", style = MaterialTheme.typography.bodySmall, color = CheckoutStyle.Muted)
        }
    }
    if (showFeeInfo) AlertDialog(onDismissRequest = { showFeeInfo = false }, title = { Text("Cách tính phí giao hàng") },
        text = { Text("Mỗi người bán có phí giao riêng, dựa trên khoảng cách đường thẳng ước tính đến địa chỉ nhận.\n\nĐến 5 km: 15.000đ\nTrên 5–15 km: 25.000đ\nTrên 15–30 km: 40.000đ\n\nPhạm vi hỗ trợ: tối đa 30 km.") },
        confirmButton = { TextButton(onClick = { showFeeInfo = false }) { Text("Đã hiểu") } })
}
