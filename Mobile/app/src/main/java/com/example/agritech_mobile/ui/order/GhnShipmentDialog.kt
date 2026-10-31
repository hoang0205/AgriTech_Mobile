package com.example.agritech_mobile.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.example.agritech_mobile.data.remote.dto.GhnArea
import com.example.agritech_mobile.data.remote.dto.GhnShop
import com.example.agritech_mobile.data.remote.dto.GhnSenderRequest
import com.example.agritech_mobile.data.remote.dto.GhnParcelRequest
import com.example.agritech_mobile.data.remote.dto.GhnQuote
import com.example.agritech_mobile.data.remote.dto.GhnShipment
import com.example.agritech_mobile.data.repository.GhnRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.example.agritech_mobile.ui.cart.SummaryRow
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@HiltViewModel
class GhnViewModel @Inject constructor(val repository: GhnRepository) : ViewModel()

@Composable
fun GhnShipmentDialog(
    orderId: String, seller: Boolean, onDismiss: () -> Unit, onChanged: () -> Unit,
    viewModel: GhnViewModel = hiltViewModel()
) {
    val repo = viewModel.repository
    val scope = rememberCoroutineScope()
    var shipment by remember(orderId) { mutableStateOf<GhnShipment?>(null) }
    var shop by remember { mutableStateOf<GhnShop?>(null) }
    var senderNeedsUpdate by remember { mutableStateOf(false) }
    var senderDraft by remember { mutableStateOf<GhnSenderRequest?>(null) }
    var provinces by remember { mutableStateOf(emptyList<GhnArea>()) }
    var wards by remember { mutableStateOf(emptyList<GhnArea>()) }
    var province by remember { mutableStateOf<GhnArea?>(null) }
    var ward by remember { mutableStateOf<GhnArea?>(null) }
    var autoSelectRecipientArea by remember(orderId) { mutableStateOf(true) }
    var areasLoading by remember { mutableStateOf(false) }
    var recipientAreaLoading by remember { mutableStateOf(false) }
    var weight by remember { mutableStateOf("") }
    var length by remember { mutableStateOf("") }
    var width by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var quote by remember { mutableStateOf<GhnQuote?>(null) }
    var quotedRequest by remember { mutableStateOf<GhnParcelRequest?>(null) }
    var confirm by remember { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    var confirmSimulation by remember { mutableStateOf(false) }

    fun runAction(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null
        scope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                error = e.message
                if (e.message?.contains("FROM_ADDRESS_CONVERT_FAIL") == true) senderNeedsUpdate = true
            }
            finally { busy = false }
        }
    }

    fun shipmentAction(action: String) {
        runAction {
            try {
                shipment = repo.action(orderId, action)
                if (action == "release") {
                    quote = null; quotedRequest = null
                    notice = if (shipment?.orderCode != null)
                        "GHN đã có vận đơn. Bạn có thể theo dõi hoặc hủy vận đơn nếu chưa bàn giao."
                    else "GHN xác nhận chưa có vận đơn. Đóng cửa sổ để tự giao hoặc hủy đơn, hoặc sửa thông tin và báo giá lại."
                }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                try { shipment = repo.view(orderId) }
                catch (cancel: CancellationException) { throw cancel }
                catch (_: Exception) { /* Keep the last known state. */ }
                throw e
            } finally { onChanged() }
        }
    }

    LaunchedEffect(orderId) {
        busy = true
        try {
            shipment = repo.view(orderId)
            if (seller) shop = repo.shop()
        }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message }
        finally { busy = false }
    }
    LaunchedEffect(seller, shop?.connected, shipment?.recipientAddress, shipment?.orderCode, shipment?.pendingCreation) {
        val current = shipment ?: return@LaunchedEffect
        if (!seller || shop?.connected != true || current.orderCode != null || current.pendingCreation || current.orderStatus !in listOf(null, "CONFIRMED")) return@LaunchedEffect
        areasLoading = true
        try {
            if (provinces.isEmpty()) provinces = repo.areas("provinces")
            if (autoSelectRecipientArea && province == null) province = matchRecipientProvince(current.recipientAddress, provinces)
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message }
        finally { areasLoading = false }
    }
    LaunchedEffect(province?.id, shipment?.recipientAddress) {
        wards = emptyList()
        ward = null
        val selectedProvince = province ?: return@LaunchedEffect
        recipientAreaLoading = true
        try {
            wards = repo.areas("wards", selectedProvince.id.toInt())
            if (autoSelectRecipientArea) ward = matchRecipientWard(shipment?.recipientAddress.orEmpty(), selectedProvince, wards)
        }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message }
        finally { recipientAreaLoading = false }
    }
    LaunchedEffect(province, ward, weight, length, width, height, senderDraft) {
        quote = null; quotedRequest = null
    }

    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.large) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            Column(Modifier.fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (shipment?.orderCode != null && shipment?.status != "simulation") "Vận đơn GHN cũ" else "Cước tham khảo GHN", style = MaterialTheme.typography.titleLarge)
                shipment?.let { current ->
                    Text("Người nhận: ${current.recipientName ?: "Chưa có"}\n${current.recipientAddress}")
                    if (current.environment != null || shop != null) Text(
                        if ((current.environment ?: shop?.environment) == "staging") "Môi trường thử nghiệm" else "Môi trường thật",
                        style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    if (current.status == "simulation" && current.environment == "staging") {
                        Text("Đơn thử nghiệm do người bán tự cập nhật giao hàng.", style = MaterialTheme.typography.bodyMedium)
                        current.shippingFee?.let { GhnFeeSummary(current.customerShippingFee, it) }
                    } else if (current.orderCode != null || current.pendingCreation) {
                        Text("Vận đơn: ${current.orderCode ?: "Đang đối chiếu yêu cầu tạo"}")
                        Text(ghnStatusLabel(current.status))
                        if (seller) current.shippingFee?.let { GhnFeeSummary(current.customerShippingFee, it) }
                        else SummaryRow("Phí giao đã chốt", ghnMoney(current.customerShippingFee))
                        current.expectedDeliveryTime?.let { Text("Dự kiến giao: $it") }
                        current.updatedAt?.let { Text("Cập nhật lần cuối: $it") }
                        if (seller && current.environment == "staging" && current.orderStatus in listOf("CONFIRMED", "SHIPPING")) OutlinedButton(enabled = !busy,
                            onClick = { confirmSimulation = true }) { Text("Chuyển sang tự giao mô phỏng") }
                        if (current.pendingCreation && seller) {
                            Text("Thử lại dùng cùng thông tin đã gửi. Nếu muốn sửa thông tin, tự giao hoặc hủy đơn, hãy kiểm tra yêu cầu với GHN trước.")
                            Button(enabled = !busy, onClick = { shipmentAction("retry") }) { Text("Thử tạo lại") }
                            OutlinedButton(enabled = !busy, onClick = { shipmentAction("release") }) {
                                Text("Kiểm tra để tự giao / hủy đơn")
                            }
                        } else if (current.orderCode != null) {
                            Button(enabled = !busy, onClick = { runAction {
                                shipment = repo.action(orderId, "refresh"); onChanged()
                            } }) { Text("Cập nhật từ GHN") }
                            if (seller && current.status in listOf("ready_to_pick", "picking", "money_collect_picking")) {
                                OutlinedButton(enabled = !busy, onClick = { confirmCancel = true }) { Text("Hủy vận đơn GHN") }
                            }
                        }
                    } else if (seller && current.orderStatus in listOf(null, "CONFIRMED")) {
                        Text("GHN chỉ báo cước. Bạn tự tổ chức và cập nhật giao hàng.", style = MaterialTheme.typography.bodySmall)
                        if (current.status == "quote_only") current.shippingFee?.let {
                            SummaryRow("Cước đã lưu", ghnMoney(it))
                        }
                        GhnShopSetup(shop, busy, repo, senderNeedsUpdate = senderNeedsUpdate,
                            onSenderDraftChange = { senderDraft = it },
                            onSaveSender = { request -> runAction {
                                shop = repo.senderAddress(request)
                                senderNeedsUpdate = false; quote = null; quotedRequest = null
                                notice = "Đã lưu địa chỉ lấy hàng. Bấm lấy báo giá GHN lại."
                            } },
                            onConnect = { runAction {
                                shop = repo.connectShop()
                                notice = shop?.let(::ghnShopResultMessage)
                            } },
                            onReload = { runAction { shop = repo.shop() } },
                            onRegister = { request -> runAction {
                                try {
                                    shop = repo.registerShop(request)
                                    notice = shop?.let(::ghnShopResultMessage)
                                }
                                catch (e: CancellationException) { throw e }
                                catch (e: Exception) {
                                    try { shop = repo.shop() }
                                    catch (cancel: CancellationException) { throw cancel }
                                    catch (_: Exception) { }
                                    throw e
                                }
                            } })
                        if (shop?.connected == true) {
                        HorizontalDivider()
                        Text("Khu vực người nhận", style = MaterialTheme.typography.titleSmall)
                        Text(if (autoSelectRecipientArea && province != null && ward != null) "Đã điền theo địa chỉ người mua."
                            else "Chọn khu vực khớp địa chỉ người mua ở trên.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (areasLoading || recipientAreaLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
                        if (provinces.isEmpty()) OutlinedButton(enabled = !busy && !areasLoading, onClick = { runAction {
                            provinces = repo.areas("provinces")
                            if (autoSelectRecipientArea) province = matchRecipientProvince(current.recipientAddress, provinces)
                        } }) { Text("Tải địa chỉ GHN") }
                        GhnAreaPicker("Tỉnh/thành", province, provinces, !busy && !areasLoading && !recipientAreaLoading) {
                            autoSelectRecipientArea = false; province = it; ward = null
                        }
                        GhnAreaPicker("Phường/xã", ward, wards, !busy && !recipientAreaLoading) {
                            autoSelectRecipientArea = false; ward = it
                        }
                        OutlinedTextField(weight, { value ->
                            if (value.length <= 6 && value.matches(Regex("[0-9]*([.,][0-9]{0,3})?"))) weight = value
                        }, label = { Text("Khối lượng cả bao bì (kg)") },
                            supportingText = { Text("Ví dụ: 2 = 2 kg · dưới 20 kg") },
                            enabled = !busy, singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(Modifier.weight(1f)) { GhnNumberField("Dài (cm)", length, !busy) { length = it } }
                            Box(Modifier.weight(1f)) { GhnNumberField("Rộng (cm)", width, !busy) { width = it } }
                            Box(Modifier.weight(1f)) { GhnNumberField("Cao (cm)", height, !busy) { height = it } }
                        }
                        Button(enabled = !busy && !areasLoading && !recipientAreaLoading && province != null && ward != null && senderDraft != null, onClick = { runAction {
                            val pickup = requireNotNull(senderDraft) { "Chọn đủ khu vực lấy hàng trước khi báo giá." }
                            val savedPickup = shop?.senderAddress?.let { GhnSenderRequest(it.address, it.provinceId, it.wardCode) }
                            if (pickup != savedPickup) {
                                shop = repo.senderAddress(pickup)
                                senderNeedsUpdate = false
                            }
                            val grams = parcelWeightGrams(weight)
                            val request = GhnParcelRequest(
                                province?.id?.toIntOrNull() ?: 0, 0, ward?.id ?: "",
                                grams, length.toIntOrNull() ?: 0, width.toIntOrNull() ?: 0, height.toIntOrNull() ?: 0)
                            quote = repo.quote(orderId, request)
                            quotedRequest = request
                        } }) { Text("Lấy báo giá GHN") }
                        quote?.let { price ->
                            GhnFeeSummary(price.customerShippingFee, price.fee)
                            Button(enabled = !busy && quotedRequest != null, onClick = { confirm = true }) { Text("Lưu cước tham khảo") }
                        }
                        }
                    } else if (current.status == "quote_only") {
                        current.shippingFee?.let { SummaryRow("Cước tham khảo đã lưu", ghnMoney(it)) }
                        Text("Người bán tự tổ chức và cập nhật giao hàng.", style = MaterialTheme.typography.bodySmall)
                    } else Text("Chưa có cước tham khảo GHN.")
                }
                notice?.let { Text(it) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (error != null) TextButton(enabled = !busy, onClick = { runAction { shipment = repo.view(orderId) } }) { Text("Tải lại thông tin vận đơn") }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                TextButton(enabled = !busy, onClick = onDismiss) { Text("Đóng") }
            }
            }
        }
    }
    if (confirm) AlertDialog(onDismissRequest = { confirm = false }, title = { Text("Lưu cước tham khảo?") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            quote?.let { GhnFeeSummary(it.customerShippingFee, it.fee) }
            Text("Chỉ lưu báo giá, không yêu cầu GHN lấy hàng. Bạn tự cập nhật đang giao và đã giao; chi phí thực tế ghi riêng.")
        } },
        confirmButton = { TextButton(onClick = {
            confirm = false
            val request = quotedRequest?.copy(expectedFee = quote?.fee) ?: return@TextButton
            runAction {
                try { shipment = repo.reference(orderId, request); quote = null; quotedRequest = null; onChanged()
                    notice = "Đã lưu cước. Đóng cửa sổ để bắt đầu giao và xác nhận đã giao." }
                catch (e: CancellationException) { throw e }
                catch (e: Exception) {
                    // Recover a saved quote if the response was lost after the backend committed it.
                    shipment = runCatching { repo.view(orderId) }.getOrNull() ?: shipment
                    onChanged(); throw e
                }
            }
        }) { Text("Lưu cước") } }, dismissButton = { TextButton(onClick = { confirm = false }) { Text("Quay lại") } })
    if (confirmSimulation) AlertDialog(onDismissRequest = { confirmSimulation = false },
        title = { Text("Tự giao mô phỏng?") },
        text = { Text("Đơn thử nghiệm sẽ do bạn tự cập nhật đang giao và đã giao. Trạng thái GHN sẽ không được đồng bộ vào đơn nữa.") },
        confirmButton = { TextButton(onClick = { confirmSimulation = false; runAction {
            shipment = repo.action(orderId, "simulation"); onChanged()
        } }) { Text("Chuyển sang mô phỏng") } },
        dismissButton = { TextButton(onClick = { confirmSimulation = false }) { Text("Quay lại") } })
    if (confirmCancel) AlertDialog(onDismissRequest = { confirmCancel = false }, title = { Text("Hủy vận đơn GHN?") },
        text = { Text("Chỉ hủy yêu cầu giao qua GHN. Đơn mua hàng vẫn được giữ để bạn tự giao hoặc xử lý tiếp.") },
        confirmButton = { TextButton(onClick = { confirmCancel = false; runAction {
            shipment = repo.action(orderId, "cancel"); onChanged()
        } }) { Text("Hủy vận đơn") } }, dismissButton = { TextButton(onClick = { confirmCancel = false }) { Text("Quay lại") } })
}

@Composable
private fun GhnNumberField(label: String, value: String, enabled: Boolean, onChange: (String) -> Unit) {
    OutlinedTextField(value, { if (it.length <= 6 && it.all(Char::isDigit)) onChange(it) }, label = { Text(label) },
        enabled = enabled, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
}

@Composable
internal fun GhnAreaPicker(label: String, selected: GhnArea?, values: List<GhnArea>, enabled: Boolean, onSelect: (GhnArea) -> Unit) {
    var open by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }
    OutlinedButton(enabled = enabled && values.isNotEmpty(), onClick = { search = ""; open = true }, modifier = Modifier.fillMaxWidth()) {
        Text(selected?.name ?: label)
    }
    if (open) AlertDialog(onDismissRequest = { open = false }, title = { Text(label) }, text = {
        Column {
            OutlinedTextField(search, { search = it }, label = { Text("Tìm kiếm") })
            androidx.compose.foundation.lazy.LazyColumn(Modifier.heightIn(max = 300.dp)) {
                val filtered = values.filter { it.name.contains(search, ignoreCase = true) }
                items(filtered.size) { index ->
                    TextButton(onClick = { onSelect(filtered[index]); open = false }) { Text(filtered[index].name) }
                }
            }
        }
    }, confirmButton = { TextButton(onClick = { open = false }) { Text("Đóng") } })
}

internal fun parcelWeightGrams(value: String): Int {
    val grams = try { BigDecimal(value.replace(',', '.')).multiply(BigDecimal(1000)).intValueExact() }
    catch (_: NumberFormatException) { throw IllegalArgumentException("Nhập khối lượng bằng kg, ví dụ 2 hoặc 0,5.") }
    catch (_: ArithmeticException) { throw IllegalArgumentException("Khối lượng không hợp lệ.") }
    require(grams in 1..19999) { "Khối lượng phải từ 0,001 kg và dưới 20 kg." }
    return grams
}

internal fun ghnMoney(value: Long): String = NumberFormat.getIntegerInstance(Locale.forLanguageTag("vi-VN")).format(value) + " đ"

@Composable
internal fun GhnFeeSummary(customerFee: Long, carrierFee: Long) {
    val extra = carrierFee - customerFee
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SummaryRow("Khách trả ship", ghnMoney(customerFee))
            SummaryRow("Cước GHN dự kiến", ghnMoney(carrierFee))
            SummaryRow(if (extra > 0) "Bạn dự kiến bù" else "Dư phí giao dự kiến", ghnMoney(kotlin.math.abs(extra)))
            Text("Phí khách đã chốt được giữ nguyên. Chi phí thực tế được ghi riêng khi quyết toán.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

fun ghnStatusLabel(status: String?): String = when (status) {
    "quote_only" -> "Đã lưu cước tham khảo GHN"
    "simulation" -> "Tự giao mô phỏng; người bán cập nhật trạng thái"
    "creating" -> "Đang đối chiếu yêu cầu tạo vận đơn"
    "create_rejected" -> "GHN từ chối tạo vận đơn. Có thể kiểm tra để sửa thông tin hoặc chuyển sang tự giao."
    "not_created" -> "GHN xác nhận chưa có vận đơn"
    "ready_to_pick" -> "GHN đang chờ lấy hàng"
    "picking", "money_collect_picking" -> "GHN đang lấy hàng"
    "picked", "storing", "transporting", "sorting" -> "GHN đã nhận và đang vận chuyển"
    "delivering", "money_collect_delivering" -> "GHN đang giao cho người nhận"
    "delivered" -> "GHN đã giao thành công"
    "delivery_fail" -> "Giao chưa thành công, chờ GHN xử lý tiếp"
    "cancel" -> "Vận đơn GHN đã hủy"
    "returned" -> "Hàng đã hoàn về, cần người bán kiểm tra"
    "waiting_to_return", "return", "return_transporting", "return_sorting", "returning", "return_fail" -> "GHN đang xử lý hoàn hàng"
    "lost", "damage" -> "Vận đơn có sự cố, liên hệ GHN"
    null -> "Chưa gửi GHN"
    else -> "Trạng thái GHN: $status"
}
