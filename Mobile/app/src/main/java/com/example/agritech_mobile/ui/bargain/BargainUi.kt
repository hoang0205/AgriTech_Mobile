package com.example.agritech_mobile.ui.bargain

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.agritech_mobile.data.remote.dto.DeliveryStatus
import com.example.agritech_mobile.data.remote.dto.ProductResponse
import com.example.agritech_mobile.ui.theme.AgritechTheme
import com.example.agritech_mobile.ui.order.CheckoutActionBar
import com.example.agritech_mobile.ui.order.CheckoutAddressCard
import com.example.agritech_mobile.ui.order.CheckoutPriceRow
import com.example.agritech_mobile.ui.order.CheckoutStyle
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Green = CheckoutStyle.Green
private val PaleGreen = CheckoutStyle.PaleGreen
private val PageBackground = CheckoutStyle.Background
private val Muted = CheckoutStyle.Muted
private val Outline = CheckoutStyle.Outline
private val CardShape = CheckoutStyle.CardShape
private val ButtonShape = CheckoutStyle.ButtonShape

private fun money(value: Double) = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value) + "đ"
private fun amount(value: Double) = NumberFormat.getNumberInstance(Locale.forLanguageTag("vi-VN")).format(value)
private fun deadline(value: String) = runCatching {
    DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy").withZone(ZoneId.systemDefault()).format(Instant.parse(value))
}.getOrDefault(value)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BargainTopBar(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    TopAppBar(title = { Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Quay lại", tint = Green) } },
        actions = actions, colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White))
}

@Composable
private fun Notice(text: String, icon: ImageVector = Icons.Default.Info, error: Boolean = false) {
    val tint = if (error) MaterialTheme.colorScheme.error else Green
    Surface(color = if (error) MaterialTheme.colorScheme.errorContainer else PaleGreen,
        shape = ButtonShape, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, null, Modifier.size(18.dp), tint = tint)
            Text(text, style = MaterialTheme.typography.bodySmall, color = tint)
        }
    }
}

@Composable
private fun SectionTitle(text: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = Muted) }
    }
}

@Composable
private fun PriceRow(label: String, value: String, prominent: Boolean = false) {
    CheckoutPriceRow(label, value, prominent)
}

@Composable
private fun StatusBadge(status: String) {
    val (text, tint, background) = when (status) {
        "PENDING" -> Triple("Chờ phản hồi", Color(0xFF9A6700), Color(0xFFFFF4D9))
        "ACCEPTED" -> Triple("Đã chấp nhận", Green, PaleGreen)
        "ORDERED" -> Triple("Đã tạo đơn", Color(0xFF1565C0), Color(0xFFEAF2FD))
        "REJECTED" -> Triple("Đã từ chối", Color(0xFFC62828), Color(0xFFFFEBEE))
        "WITHDRAWN" -> Triple("Đã rút", Muted, Color(0xFFF1F3F2))
        else -> Triple("Hết hạn", Muted, Color(0xFFF1F3F2))
    }
    Surface(color = background, shape = RoundedCornerShape(20.dp)) {
        Text(text, Modifier.padding(horizontal = 10.dp, vertical = 5.dp), color = tint,
            style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PrimaryAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, busy: Boolean = false, icon: ImageVector? = null) {
    Button(onClick = onClick, enabled = enabled && !busy, modifier = modifier.heightIn(min = 50.dp),
        shape = ButtonShape, colors = ButtonDefaults.buttonColors(containerColor = Green),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)) {
        if (busy) {
            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
            Spacer(Modifier.width(8.dp))
        } else if (icon != null) {
            Icon(icon, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
        }
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OfferCard(o: Offer, busy: Boolean, onAction: (String) -> Unit, onCheckout: () -> Unit, onOrders: () -> Unit) {
    Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Outline), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("ĐỀ NGHỊ #${o.id}", Modifier.weight(1f), style = MaterialTheme.typography.labelSmall,
                    color = Muted, fontWeight = FontWeight.SemiBold)
                StatusBadge(o.status)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = PaleGreen, shape = ButtonShape) {
                    Icon(Icons.Default.ShoppingBag, null, Modifier.padding(12.dp).size(24.dp), tint = Green)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(o.productName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(if (o.isBuyer) "Người bán: ${o.sellerName}" else "Người mua: ${o.buyerName}",
                        style = MaterialTheme.typography.bodySmall, color = Muted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Surface(color = PageBackground, shape = ButtonShape) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    PriceRow("Giá niêm yết lúc gửi", "${money(o.listedPrice)}/${o.unit}")
                    PriceRow("Giá đề nghị", "${money(o.unitPrice)}/${o.unit}")
                    PriceRow("Số lượng", "${amount(o.quantity)} ${o.unit}")
                    HorizontalDivider(color = Outline)
                    PriceRow("Tiền hàng", money(o.quantity * o.unitPrice), prominent = true)
                    Text("Chưa gồm phí giao hàng", style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
            if (o.message.isNotBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(16.dp), tint = Muted)
                    Text(o.message, style = MaterialTheme.typography.bodySmall, color = Muted)
                }
            }
            if (o.status == "PENDING" || o.status == "ACCEPTED") {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Schedule, null, Modifier.size(15.dp), tint = Muted)
                    Text("${if (o.status == "ACCEPTED") "Đặt hàng trước" else "Phản hồi trước"} ${deadline(o.expiresAt)}",
                        style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
            when {
                o.status == "PENDING" && !o.isBuyer -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = { onAction("reject") }, enabled = !busy,
                        modifier = Modifier.weight(1f).heightIn(min = 50.dp), shape = ButtonShape,
                        border = BorderStroke(1.dp, Outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted)) { Text("Từ chối") }
                    PrimaryAction("Đồng ý", { onAction("accept") }, Modifier.weight(1f), enabled = !busy, icon = Icons.Default.Check)
                }
                o.status == "PENDING" && o.isBuyer -> OutlinedButton(onClick = { onAction("withdraw") }, enabled = !busy,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp), shape = ButtonShape,
                    border = BorderStroke(1.dp, Outline), colors = ButtonDefaults.outlinedButtonColors(contentColor = Muted)) { Text("Rút đề nghị") }
                o.status == "ACCEPTED" && o.isBuyer -> PrimaryAction("Đặt hàng theo giá đã chốt", onCheckout,
                    Modifier.fillMaxWidth(), enabled = !busy, icon = Icons.Default.ShoppingBag)
                o.orderId != null && o.isBuyer -> OutlinedButton(onClick = onOrders, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = ButtonShape, border = BorderStroke(1.dp, Green), colors = ButtonDefaults.outlinedButtonColors(contentColor = Green)) {
                    Text("Xem đơn #${o.orderId}"); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun BargainScreen(onBack: () -> Unit, onAddresses: () -> Unit, onOrders: () -> Unit,
    viewModel: BargainViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    var buying by rememberSaveable { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.load() }
    Scaffold(containerColor = PageBackground, topBar = {
        BargainTopBar("Trả giá", onBack) {
            IconButton(onClick = viewModel::load, enabled = !s.busy) { Icon(Icons.Default.Refresh, "Làm mới", tint = Green) }
        }
    }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 16.dp, vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                listOf(true to "Đã gửi", false to "Nhận được").forEach { (buyer, label) ->
                    val count = s.offers.count { it.isBuyer == buyer }
                    val selected = buying == buyer
                    Surface(onClick = { buying = buyer }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
                        color = if (selected) Green else Color(0xFFF3F5F3)) {
                        Text("$label ($count)", Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            color = if (selected) Color.White else Muted, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelLarge, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green, trackColor = PaleGreen)
            LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Notice("Giá tốt cho đơn hàng của bạn. Chấp nhận đề nghị chưa giữ hàng; tồn kho được kiểm tra khi đặt đơn.") }
                s.notice?.let { item { Notice(it, Icons.Default.CheckCircle) } }
                s.error?.let { item { Notice(it, error = true) } }
                if (s.offers.none { it.isBuyer == buying } && !s.busy && s.error == null) item {
                    Column(Modifier.fillMaxWidth().padding(vertical = 48.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = RoundedCornerShape(28.dp), color = PaleGreen) {
                            Icon(Icons.Default.Handshake, null, Modifier.padding(24.dp).size(40.dp), tint = Green)
                        }
                        Text(if (buying) "Chưa có đề nghị nào" else "Chưa có đề nghị nhận được", fontWeight = FontWeight.Bold)
                        Text(if (buying) "Mở một sản phẩm và chọn Trả giá\nđể gửi mức giá bạn mong muốn." else "Đề nghị từ người mua sẽ xuất hiện ở đây.",
                            color = Muted, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
                items(s.offers.filter { it.isBuyer == buying }, key = { it.id }) { o ->
                    OfferCard(o, s.busy, { viewModel.act(o, it) }, { viewModel.open(o) }, onOrders)
                }
            }
        }
    }
    s.selected?.let { CheckoutSheet(s, viewModel::close, viewModel::select, {
        viewModel.close(); onAddresses()
    }, viewModel::checkout) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CheckoutSheet(s: BargainState, onClose: () -> Unit,
    onSelect: (com.example.agritech_mobile.data.remote.dto.ShippingDetails) -> Unit,
    onAddresses: () -> Unit, onCheckout: () -> Unit) {
    val o = s.selected ?: return
    val fee = s.quote?.takeIf { it.status == DeliveryStatus.AVAILABLE }?.shippingFee
    ModalBottomSheet(onDismissRequest = { if (!s.busy) onClose() }, containerColor = PageBackground,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(0.9f)) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Xác nhận đơn hàng", Modifier.weight(1f), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = onClose, enabled = !s.busy) { Icon(Icons.Default.Close, "Đóng") }
            }
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                item { Surface(color = Color.White, shape = CardShape, border = BorderStroke(1.dp, Outline)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(o.productName, fontWeight = FontWeight.Bold)
                        Text("${amount(o.quantity)} ${o.unit} × ${money(o.unitPrice)}", color = Muted, style = MaterialTheme.typography.bodyMedium)
                        Text("Giá đã được người bán chấp nhận", color = Green, style = MaterialTheme.typography.labelSmall)
                    }
                } }
                item { SectionTitle("Địa chỉ nhận hàng", "Chọn địa chỉ để tính phí giao hàng") }
                if (s.addresses.isEmpty()) item { Notice("Bạn chưa có địa chỉ nhận hàng. Hãy thêm địa chỉ để tiếp tục.", Icons.Default.LocationOn) }
                items(s.addresses, key = { it.id }) { a ->
                    val selected = s.address?.id == a.id
                    CheckoutAddressCard(a.receiverName, a.phoneNumber, a.addressDetail, { onSelect(a) },
                        selected = selected, selectable = true, enabled = !s.busy)
                }
                item { OutlinedButton(onClick = onAddresses, enabled = !s.busy, modifier = Modifier.fillMaxWidth(), shape = ButtonShape,
                    border = BorderStroke(1.dp, Green), colors = ButtonDefaults.outlinedButtonColors(contentColor = Green)) {
                    Icon(Icons.Default.AddLocationAlt, null, Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)); Text("Thêm / sửa địa chỉ")
                } }
                item { Surface(color = Color.White, shape = CardShape, border = BorderStroke(1.dp, Outline)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SectionTitle("Chi tiết thanh toán")
                        PriceRow("Tiền hàng", money(o.quantity * o.unitPrice))
                        PriceRow("Phí giao hàng", fee?.let { money(it.toDouble()) } ?: "Chưa xác định")
                        HorizontalDivider(color = Outline)
                        PriceRow("Tổng thanh toán", fee?.let { money(o.quantity * o.unitPrice + it) } ?: "—", true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, null, Modifier.size(20.dp), tint = Green)
                            Text("Thanh toán khi nhận hàng (COD)", style = MaterialTheme.typography.bodySmall, color = Green)
                        }
                    }
                } }
                s.quote?.takeIf { it.status != DeliveryStatus.AVAILABLE }?.let { item { Notice(it.message, error = true) } }
                s.error?.let { item { Notice(it, error = true) } }
            }
            CheckoutActionBar("Xác nhận đặt hàng",
                total = fee?.let { money(o.quantity * o.unitPrice + it) } ?: "Chưa xác định",
                onClick = onCheckout, enabled = fee != null && s.address != null,
                busy = s.busy, navigationPadding = false)
        }
    }
}

@Composable
private fun ProductSummary(product: ProductResponse) {
    Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Outline)) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(76.dp).clip(ButtonShape).background(PaleGreen), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Eco, null, tint = Green, modifier = Modifier.size(32.dp))
                if (product.imageUrls.isNotEmpty()) AsyncImage(product.imageUrls.first(), product.name,
                    Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text("${money(product.price)}/${product.unit}", fontWeight = FontWeight.SemiBold, color = Green)
                Text("Còn ${amount(product.quantity)} ${product.unit}", style = MaterialTheme.typography.bodySmall, color = Muted)
            }
        }
    }
}

@Composable
fun CreateBargainScreen(productId: String, onBack: () -> Unit, onCreated: () -> Unit,
    viewModel: BargainViewModel = hiltViewModel()) {
    val s by viewModel.state.collectAsStateWithLifecycle()
    var quantity by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }
    val q = quantity.replace(',', '.').toDoubleOrNull()
    val p = price.toLongOrNull()
    val product = s.product
    val quantityError = quantity.isNotBlank() && (q == null || !q.isFinite() || q <= 0 || (product != null && q > product.quantity))
    val priceError = price.isNotBlank() && (p == null || p <= 0 || (product != null && p >= product.price))
    val valid = product != null && q != null && q.isFinite() && q > 0 && p != null && p > 0 && !quantityError && !priceError
    LaunchedEffect(productId) { viewModel.product(productId) }
    Scaffold(containerColor = PageBackground, modifier = Modifier.imePadding(), topBar = { BargainTopBar("Gửi đề nghị trả giá", onBack) },
        bottomBar = { Surface(color = Color.White, shadowElevation = 8.dp) {
            Column(Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (valid) PriceRow("Tiền hàng đề nghị", money(q!! * p!!), true)
                PrimaryAction("Gửi đề nghị", { viewModel.create(NewOffer(productId, q!!, p!!, message), onCreated) },
                    Modifier.fillMaxWidth(), enabled = valid, busy = s.busy, icon = Icons.Default.Handshake)
            }
        } }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            if (product != null) ProductSummary(product)
            else if (s.busy) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Green, trackColor = PaleGreen)
            SectionTitle("Mức giá bạn mong muốn", "Gửi số lượng và đơn giá để thương lượng với người bán.")
            Card(shape = CardShape, colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Outline)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Số lượng muốn mua") },
                        suffix = { Text(product?.unit.orEmpty()) }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = ButtonShape,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        isError = quantityError, supportingText = if (quantityError) ({ Text("Nhập số lượng lớn hơn 0 và không vượt tồn kho.") }) else null,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green, cursorColor = Green))
                    OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Đơn giá đề nghị") },
                        suffix = { Text("đ/${product?.unit.orEmpty()}") }, singleLine = true, modifier = Modifier.fillMaxWidth(), shape = ButtonShape,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next), isError = priceError,
                        supportingText = { Text(if (priceError) "Nhập số đồng nguyên dương, thấp hơn giá niêm yết." else "Giá cho mỗi ${product?.unit ?: "đơn vị"}, chưa gồm phí giao hàng.") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green, cursorColor = Green))
                    OutlinedTextField(value = message, onValueChange = { if (it.length <= 500) message = it },
                        label = { Text("Lời nhắn (không bắt buộc)") }, placeholder = { Text("Ví dụ: Mình lấy số lượng lớn, giá này được không?") },
                        modifier = Modifier.fillMaxWidth(), shape = ButtonShape, minLines = 3, maxLines = 5,
                        supportingText = { Text("${message.length}/500", Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Green, focusedLabelColor = Green, cursorColor = Green))
                }
            }
            Notice("Người bán có 24 giờ để phản hồi. Sau khi được đồng ý, bạn có 24 giờ để đặt đơn COD. Hàng chưa được giữ khi chấp nhận giá.", Icons.Default.Schedule)
            s.error?.let { Notice(it, error = true) }
        }
    }
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun OfferCardPreview() {
    AgritechTheme {
        Box(Modifier.background(PageBackground).padding(16.dp)) {
            OfferCard(Offer(12, "p", "Cà chua hữu cơ Đà Lạt", "kg", 20.0, 25000.0, 30000.0,
                "Mình lấy 20 kg, giá này được không?", "Minh Hoàng", "Nông trại xanh", true, "ACCEPTED", "2026-10-25T10:00:00Z", null),
                false, {}, {}, {})
        }
    }
}

@Composable
fun BargainChatCard(id: Long, text: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = Color.White), border = BorderStroke(1.dp, Color(0xFFC8E6C9))) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Surface(color = PaleGreen, shape = RoundedCornerShape(10.dp)) {
                    Icon(Icons.Default.Handshake, null, Modifier.padding(8.dp).size(20.dp), tint = Green)
                }
                Column {
                    Text("Đề nghị trả giá", fontWeight = FontWeight.Bold, color = Green)
                    Text("#$id", style = MaterialTheme.typography.labelSmall, color = Muted)
                }
            }
            Text(text.substringBefore("\nMở đề nghị để"), style = MaterialTheme.typography.bodyMedium, maxLines = 6, overflow = TextOverflow.Ellipsis)
            HorizontalDivider(color = Outline)
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Xem đề nghị", Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = Green)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp), tint = Green)
            }
        }
    }
}
