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
import com.example.agritech_mobile.ui.cart.SummaryRow
import com.example.agritech_mobile.data.remote.dto.DeliveryManagement
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
fun DeliveryManagementDialog(
    orderId: String, onDismiss: () -> Unit, onChanged: () -> Unit, onGhn: () -> Unit,
    viewModel: GhnViewModel = hiltViewModel()
) {
    val repo = viewModel.repository
    val scope = rememberCoroutineScope()
    var data by remember(orderId) { mutableStateOf<DeliveryManagement?>(null) }
    var cost by remember(orderId) { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    var showOtherDelivery by remember(orderId) { mutableStateOf(false) }
    fun action(block: suspend () -> Unit) {
        if (busy) return
        busy = true; error = null; notice = null
        scope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message }
            finally { busy = false }
        }
    }
    LaunchedEffect(orderId) {
        busy = true
        try { data = repo.delivery(orderId); cost = data?.actualCost?.toString().orEmpty() }
        catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message }
        finally { busy = false }
    }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        Surface(shape = MaterialTheme.shapes.large) {
            ProvideTextStyle(MaterialTheme.typography.bodyMedium) {
            Column(Modifier.fillMaxWidth().heightIn(max = 680.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Giao hàng & chi phí", style = MaterialTheme.typography.titleLarge)
                data?.let { current ->
                    if (current.method != null) SummaryRow("Cách giao", deliveryMethodLabel(current.method))
                    SummaryRow("Phí thu khách", "${current.customerShippingFee} đ")
                    if (current.status == "CONFIRMED" && !current.ghnActive) {
                        Text("Tự giao hoặc thuê người giao.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(enabled = !busy, onClick = { action {
                            data = repo.chooseDelivery(orderId, "SELF"); onChanged()
                            notice = "Đã chọn tự giao."
                        } }, modifier = Modifier.fillMaxWidth()) { Text("Tự tổ chức giao") }
                    }
                    HorizontalDivider()
                    Text("Chi phí giao", style = MaterialTheme.typography.titleSmall)
                    Text("Gồm phí giao và hoàn hàng đã phát sinh.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(cost, { value ->
                        if (value.length <= 9 && value.all(Char::isDigit)) cost = value
                    }, enabled = !busy, singleLine = true, label = { Text("Chi phí thực tế (đ)") },
                        supportingText = { Text("Trống: chưa ghi · 0 đ: không phát sinh") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    Button(enabled = !busy, onClick = { action {
                        val amount = if (cost.isBlank()) null else cost.toLongOrNull()
                            ?: throw IllegalArgumentException("Chi phí không hợp lệ.")
                        data = repo.shippingCost(orderId, amount)
                        onChanged()
                        notice = if (amount == null) "Chưa ghi chi phí; lợi nhuận chưa xác định."
                            else "Đã lưu chi phí."
                    } }) { Text("Lưu chi phí") }
                    if (current.status == "CANCELLED") {
                        Text("Đơn hủy chỉ ghi chi phí phát sinh.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        SummaryRow("Chênh lệch giao hàng", current.actualCost?.let { "${current.customerShippingFee - it} đ" } ?: "Chưa rõ")
                    }
                    if (current.ghnActive || current.method == "GHN" || current.ghnEstimatedFee != null) {
                        TextButton(enabled = !busy, onClick = onGhn) { Text(if (current.ghnActive) "Vận đơn GHN cũ" else "Cước tham khảo GHN") }
                        current.ghnEstimatedFee?.let {
                            if (current.status != "CANCELLED") GhnFeeSummary(current.customerShippingFee, it)
                            Text("Cước tham khảo chưa phải chi phí thực tế. Chỉ lưu khoản đã phát sinh để tính lợi nhuận.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(enabled = !busy, onClick = { cost = it.toString() }) { Text("Điền cước GHN vào chi phí") }
                        }
                    }
                    if (current.status == "CONFIRMED" && !current.ghnActive && current.method != "GHN") {
                        TextButton(enabled = !busy, onClick = { showOtherDelivery = !showOtherDelivery }) {
                            Text(if (showOtherDelivery) "Thu gọn tùy chọn giao hàng" else "Tùy chọn giao hàng khác")
                        }
                        if (showOtherDelivery) {
                            Text("Tham khảo phí vận chuyển", style = MaterialTheme.typography.bodySmall)
                            OutlinedButton(enabled = !busy, onClick = { action {
                                data = repo.chooseDelivery(orderId, "SELF"); onChanged(); onGhn()
                            } }, modifier = Modifier.fillMaxWidth()) { Text("Tính cước GHN") }
                        }
                    }
                }
                notice?.let { Text(it) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (data == null && !busy) TextButton(onClick = { action {
                    data = repo.delivery(orderId); cost = data?.actualCost?.toString().orEmpty()
                } }) { Text("Tải lại") }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                TextButton(enabled = !busy, onClick = onDismiss) { Text("Đóng") }
            }
            }
        }
    }
}

fun deliveryMethodLabel(method: String?) = when (method) {
    "SELF" -> "Tự tổ chức giao"
    "GHN" -> "Giao Hàng Nhanh"
    else -> "Chưa chọn"
}
