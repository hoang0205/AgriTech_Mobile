package com.example.agritech_mobile.ui.order

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.agritech_mobile.data.remote.dto.OrderDetailResponse
import com.example.agritech_mobile.ui.components.UserAvatar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

private val DetailGreen = Color(0xFF1B5E20)
private val DetailMuted = Color(0xFF757575)
private fun orderMoney(value: Number): String = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(value) + " đ"
private fun orderDate(value: String): String = runCatching {
    val parsed = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(value)
        ?: return@runCatching value
    SimpleDateFormat("HH:mm • dd/MM/yyyy", Locale.getDefault()).format(parsed)
}.getOrDefault(value)
private fun statusTitle(status: String): String = when (status) {
    "UNPAID" -> "Chờ thanh toán"
    "PENDING" -> "Chờ xác nhận"
    "CONFIRMED" -> "Đã xác nhận"
    "SHIPPING" -> "Đang giao hàng"
    "COMPLETED" -> "Đã hoàn thành"
    "CANCELLED" -> "Đã hủy"
    else -> "Đang cập nhật"
}

@Composable
fun OrderDetailScreen(
    orderId: Long,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onPayment: (Long, String) -> Unit,
    viewModel: OrderDetailViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDelivery by remember(orderId) { mutableStateOf(false) }
    var showGhn by remember(orderId) { mutableStateOf(false) }
    var showComplaint by remember(orderId) { mutableStateOf(false) }
    LifecycleResumeEffect(orderId) {
        viewModel.load(orderId)
        onPauseOrDispose { }
    }
    val order = state.order
    if (showComplaint) OrderComplaintDialog(orderId, onDismiss = { showComplaint = false }, onChanged = { viewModel.load(orderId) })
    if (showDelivery && order != null) DeliveryManagementDialog(
        orderId.toString(), onDismiss = { showDelivery = false },
        onChanged = { viewModel.load(orderId) },
        onGhn = { showDelivery = false; showGhn = true }
    )
    if (showGhn && order != null) GhnShipmentDialog(
        orderId.toString(), seller = order.viewerRole == "SELLER",
        onDismiss = { showGhn = false }, onChanged = { viewModel.load(orderId) }
    )
    OrderDetailContent(state, onBack, { viewModel.load(orderId) }, onProductClick,
        { viewModel.updateStatus(it) }, { viewModel.pay(onPayment) },
        { showDelivery = true }, { showGhn = true }, onComplaint = { showComplaint = true })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailContent(
    state: OrderDetailUiState,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onProductClick: (String) -> Unit,
    onStatus: (String) -> Unit,
    onPay: () -> Unit,
    onDelivery: () -> Unit,
    onGhn: () -> Unit,
    onComplaint: () -> Unit = {}
) {
    val order = state.order
    var confirmStatus by remember(order?.orderId) { mutableStateOf<String?>(null) }
    val enabled = !state.busy && !state.loading
    val context = LocalContext.current
    confirmStatus?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmStatus = null },
            title = { Text(if (target == "CANCELLED") "Hủy đơn hàng?"
                else if (order?.viewerRole == "BUYER") "Bạn đã nhận được hàng?" else "Xác nhận đã giao hàng?") },
            text = { Text(if (target == "CANCELLED") "Đơn hàng sẽ được hủy. Bạn không thể tiếp tục xử lý đơn này."
                else if (order?.viewerRole == "BUYER") "Chỉ xác nhận khi bạn đã nhận được hàng. Đơn sẽ được hoàn thành."
                else "Chỉ xác nhận khi đã giao hàng. Người mua có 48 giờ để xác nhận hoặc khiếu nại trước khi đơn tự hoàn thành.") },
            confirmButton = {
                TextButton(onClick = { confirmStatus = null; onStatus(target) }) {
                    Text("Xác nhận", color = if (target == "CANCELLED") Color(0xFFC62828) else DetailGreen)
                }
            },
            dismissButton = { TextButton(onClick = { confirmStatus = null }) { Text("Quay lại") } }
        )
    }
    Scaffold(
        containerColor = Color(0xFFF8FAF9),
        topBar = {
            TopAppBar(
                title = { Text("Chi tiết đơn hàng", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DetailGreen) },
                navigationIcon = { IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = DetailGreen)
                } },
                actions = { IconButton(onClick = onRefresh, enabled = enabled) {
                    Icon(Icons.Default.Refresh, "Tải lại đơn hàng", tint = DetailGreen)
                } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (order != null && (order.canCancel || order.canPay || order.nextStatus != null)) {
                Surface(shadowElevation = 6.dp, color = Color.White) {
                    Row(Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (order.canCancel) OutlinedButton(
                            onClick = { confirmStatus = "CANCELLED" }, enabled = enabled,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                        ) { Text("Hủy đơn", color = Color(0xFFC62828)) }
                        if (order.canPay || order.nextStatus != null) Button(
                            onClick = {
                                if (order.canPay) onPay()
                                else order.nextStatus?.let { if (it == "COMPLETED") confirmStatus = it else onStatus(it) }
                            },
                            enabled = enabled, modifier = Modifier.weight(1.4f).heightIn(min = 48.dp),
                            shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = DetailGreen)
                        ) { Text(when {
                            state.busy -> "Đang xử lý…"
                            order.canPay -> "Thanh toán ngay"
                            order.nextStatus == "CONFIRMED" -> "Xác nhận đơn"
                            order.nextStatus == "SHIPPING" -> "Bắt đầu giao"
                            order.viewerRole == "BUYER" -> "Đã nhận hàng"
                            else -> "Xác nhận đã giao"
                        }, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    ) { padding ->
        if (order == null && state.loading) Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = DetailGreen)
        } else if (order == null) Column(
            Modifier.fillMaxSize().padding(padding).padding(32.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.ReceiptLong, null, Modifier.size(48.dp), tint = DetailMuted)
            Spacer(Modifier.height(16.dp))
            Text(state.error ?: "Chưa tải được đơn hàng.")
            Spacer(Modifier.height(16.dp))
            Button(onClick = onRefresh, colors = ButtonDefaults.buttonColors(containerColor = DetailGreen)) { Text("Thử lại") }
        } else Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = DetailGreen)
            state.error?.let { DetailMessage(it, true) }
            state.notice?.let { DetailMessage(it, false) }
            StatusSection(order)
            if (order.complaintOpen) DetailMessage(if (order.status == "COMPLETED") "Khiếu nại đang chờ admin xử lý."
                else "Đơn đang có khiếu nại. Tạm dừng hoàn thành để admin xử lý.", true)
            if (order.canComplain || order.complaintId != null) OutlinedButton(onClick = onComplaint,
                enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Icon(Icons.Default.Flag, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (order.complaintId != null) "Xem khiếu nại & phản hồi" else "Khiếu nại đơn hàng")
            }
            DetailSection("Thông tin đơn hàng", Icons.Default.ReceiptLong) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Mã đơn hàng", style = MaterialTheme.typography.bodySmall, color = DetailMuted)
                        Text("#ORD-${order.orderId}", fontWeight = FontWeight.Bold, color = DetailGreen)
                    }
                    IconButton(onClick = {
                        (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                            .setPrimaryClip(ClipData.newPlainText("Mã đơn hàng", order.orderId.toString()))
                        Toast.makeText(context, "Đã sao chép mã đơn hàng", Toast.LENGTH_SHORT).show()
                    }) { Icon(Icons.Default.ContentCopy, "Sao chép mã đơn", tint = DetailMuted, modifier = Modifier.size(20.dp)) }
                }
                DetailValue("Thời gian đặt", orderDate(order.orderDate))
            }
            DetailSection("Địa chỉ nhận hàng", Icons.Default.LocationOn) {
                Text(order.receiverName, fontWeight = FontWeight.Bold)
                Text(order.receiverPhone, style = MaterialTheme.typography.bodyMedium, color = DetailMuted)
                Text(order.shippingAddress, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
            }
            DetailSection(if (order.viewerRole == "SELLER") "Người mua" else "Người bán", Icons.Default.Storefront) {
                val parties = if (order.viewerRole == "SELLER") listOf(order.buyer) else order.sellers
                parties.forEach { party ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        UserAvatar(party.avatar, party.name, Modifier.size(42.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(party.name, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        if (party.phone.isNotBlank()) IconButton(onClick = {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", party.phone, null))
                            runCatching { context.startActivity(intent) }
                                .onFailure { Toast.makeText(context, "Không mở được ứng dụng gọi điện.", Toast.LENGTH_SHORT).show() }
                        }) { Icon(Icons.Default.Call, "Gọi ${party.name}", tint = DetailGreen) }
                    }
                }
            }
            DetailSection("Sản phẩm (${order.items.size})", Icons.Default.ShoppingBag) {
                order.items.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = Color(0xFFF0F0F0))
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).clickable { onProductClick(item.productId) }.padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        AsyncImage(item.image, item.name, Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFF1F3F1)), contentScale = ContentScale.Crop)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(item.name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("${NumberFormat.getNumberInstance(Locale("vi", "VN")).format(item.quantity)} ${item.unit} × ${orderMoney(item.unitPrice)}",
                                style = MaterialTheme.typography.bodySmall, color = DetailMuted)
                            Text(orderMoney(item.subtotal), fontWeight = FontWeight.Bold, color = DetailGreen)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = DetailMuted, modifier = Modifier.size(18.dp))
                    }
                }
            }
            DetailSection("Thanh toán", Icons.Default.Payments) {
                DetailValue("Tiền hàng", orderMoney(order.subtotal))
                DetailValue("Phí giao hàng", orderMoney(order.shippingFee))
                HorizontalDivider(color = Color(0xFFEEEEEE))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Tổng thanh toán", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                    Text(orderMoney(order.totalAmount), Modifier.weight(1.2f), fontWeight = FontWeight.ExtraBold,
                        color = DetailGreen, fontSize = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.End)
                }
                HorizontalDivider(color = Color(0xFFEEEEEE))
                DetailValue("Phương thức", when (order.paymentMethod) {
                    "COD" -> "Thanh toán khi nhận hàng"
                    "VNPAY" -> "VNPay"
                    else -> "Chưa có thông tin"
                })
                DetailValue("Thanh toán", when (order.paymentStatus) {
                    "PAID" -> "Đã thanh toán"
                    "COD" -> "Thu tiền khi nhận hàng"
                    "UNPAID" -> if (order.status == "CANCELLED") "Chưa thanh toán • Đơn đã hủy" else "Chưa thanh toán"
                    else -> "Chưa có thông tin"
                })
                order.transactionNo?.let { DetailValue("Mã giao dịch", it) }
            }
            DetailSection("Thông tin giao hàng", Icons.Default.LocalShipping) {
                DetailValue("Hình thức giao", order.deliveryMethod?.let { deliveryMethodLabel(it) } ?: "Chưa chọn")
                order.pickupAddress?.takeIf { it.isNotBlank() }?.let {
                    Text("Địa chỉ gửi", style = MaterialTheme.typography.bodySmall, color = DetailMuted)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }
                order.ghnOrderCode?.let {
                    DetailValue(if (order.ghnStatus == "simulation") "Mã mô phỏng" else "Mã vận đơn", it)
                    Text(ghnStatusLabel(order.ghnStatus), style = MaterialTheme.typography.bodyMedium, color = DetailGreen)
                    OutlinedButton(onClick = onGhn, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                        Text(if (order.ghnStatus == "simulation") "Xem mô phỏng giao hàng" else "Xem thông tin vận đơn", color = DetailGreen)
                    }
                }
                order.expectedDeliveryTime?.takeIf { it.isNotBlank() }?.let { DetailValue("Dự kiến giao", orderDate(it)) }
                if (order.viewerRole == "SELLER") DetailValue("Chi phí giao thực tế", order.actualShippingCost?.let { orderMoney(it) } ?: "Chưa ghi nhận")
                if (order.canManageDelivery) OutlinedButton(onClick = onDelivery, enabled = enabled,
                    modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(10.dp)) {
                    Text("Giao hàng & chi phí", color = DetailGreen)
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatusSection(order: OrderDetailResponse) {
    val cancelled = order.status == "CANCELLED"
    val accent = if (cancelled) Color(0xFFC62828) else DetailGreen
    Surface(color = if (cancelled) Color(0xFFFFF0F0) else Color(0xFFEAF5EC), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(when (order.status) {
                    "CANCELLED" -> Icons.Default.Cancel
                    "COMPLETED" -> Icons.Default.CheckCircle
                    "SHIPPING" -> Icons.Default.LocalShipping
                    "UNPAID" -> Icons.Default.Payments
                    else -> Icons.Default.Inventory2
                }, null, tint = accent, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(statusTitle(order.status), fontWeight = FontWeight.Bold, color = accent, fontSize = 19.sp)
                    Text(when (order.status) {
                        "UNPAID" -> "Thanh toán để người bán tiếp nhận đơn hàng."
                        "PENDING" -> "Đơn hàng đang chờ người bán xác nhận."
                        "CONFIRMED" -> "Người bán đang chuẩn bị đơn hàng."
                        "SHIPPING" -> "Đơn hàng đang được giao đến người nhận."
                        "COMPLETED" -> "Cảm ơn bạn đã đồng hành cùng AgriTech."
                        "CANCELLED" -> "Đơn hàng đã dừng xử lý."
                        else -> "Tải lại để xem trạng thái mới nhất."
                    }, style = MaterialTheme.typography.bodySmall, color = accent, lineHeight = 18.sp)
                }
            }
            if (!cancelled && order.status != "UNPAID") {
                val statuses = listOf("PENDING", "CONFIRMED", "SHIPPING", "COMPLETED")
                val current = statuses.indexOf(order.status)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Đặt hàng", "Xác nhận", "Đang giao", "Hoàn tất").forEachIndexed { index, label ->
                        Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(if (index <= current) DetailGreen else Color(0xFFCEE0D0)))
                            Spacer(Modifier.height(8.dp))
                            Text(label, fontSize = 10.sp, color = if (index <= current) DetailGreen else DetailMuted)
                        }
                    }
                }
            }
            if (order.status == "SHIPPING" || order.sellerDeliveredAt != null || order.buyerReceivedAt != null) {
                HorizontalDivider(color = Color(0xFFCEE0D0))
                Text("Xác nhận giao nhận", fontWeight = FontWeight.SemiBold, color = DetailGreen)
                Text(order.sellerDeliveredAt?.let { "✓ Người bán đã giao • ${orderDate(it)}" }
                    ?: "○ Chờ người bán xác nhận đã giao", style = MaterialTheme.typography.bodySmall, color = DetailGreen)
                Text(order.buyerReceivedAt?.let { "✓ Người mua đã nhận • ${orderDate(it)}" }
                    ?: if (order.status == "SHIPPING") "○ Chờ người mua xác nhận đã nhận" else "Người mua chưa xác nhận trên ứng dụng",
                    style = MaterialTheme.typography.bodySmall, color = DetailGreen)
                Text(when {
                    order.status == "CANCELLED" -> "Đơn hàng đã được đóng / hủy."
                    order.completionMethod == "AUTO" -> "Đơn đã tự hoàn thành sau thời gian chờ xác nhận."
                    order.completionMethod == "ADMIN" -> "Admin đã hoàn thành đơn sau khi xử lý khiếu nại."
                    order.status == "COMPLETED" -> "Đơn hàng đã hoàn thành."
                    order.complaintOpen -> "Tạm dừng hoàn thành trong khi admin xử lý khiếu nại."
                    order.autoCompleteAt != null -> "Tự hoàn thành lúc ${orderDate(order.autoCompleteAt)} nếu không có khiếu nại."
                    else -> "Chờ người bán xác nhận đã giao hàng."
                }, style = MaterialTheme.typography.bodySmall, color = DetailMuted)
            }
        }
    }
}

@Composable
private fun DetailSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = DetailGreen, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall, color = Color(0xFF212121))
            }
            content()
        }
    }
}

@Composable
private fun DetailValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, color = DetailMuted)
        Text(value, Modifier.weight(1.4f), style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.End, color = Color(0xFF333333))
    }
}

@Composable
private fun DetailMessage(text: String, error: Boolean) {
    Surface(color = if (error) Color(0xFFFFF0F0) else Color(0xFFEAF5EC), shape = RoundedCornerShape(12.dp)) {
        Text(text, Modifier.fillMaxWidth().padding(12.dp), style = MaterialTheme.typography.bodyMedium,
            color = if (error) Color(0xFFC62828) else DetailGreen)
    }
}
