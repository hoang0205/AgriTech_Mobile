package com.example.agritech_mobile.ui.order

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage

private val complaintReasons = linkedMapOf("NOT_RECEIVED" to "Chưa nhận được hàng", "DAMAGED" to "Hàng hỏng / chất lượng không đúng",
    "MISSING_OR_WRONG" to "Thiếu hàng / giao sai hàng", "OTHER" to "Vấn đề khác")
private fun complaintStatus(status: String) = when (status) {
    "OPEN" -> "Chờ xử lý"; "REVIEWING" -> "Đang xem xét"; "RESOLVED" -> "Đã giải quyết"; "REJECTED" -> "Đã từ chối"; else -> status
}

@Composable
fun OrderComplaintDialog(orderId: Long, onDismiss: () -> Unit, onChanged: () -> Unit,
    viewModel: OrderComplaintViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var reason by rememberSaveable(orderId) { mutableStateOf("NOT_RECEIVED") }
    var description by rememberSaveable(orderId) { mutableStateOf("") }
    var imagePaths by rememberSaveable(orderId) { mutableStateOf(arrayListOf<String>()) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickMultipleVisualMedia(3)) { uris ->
        if (!state.busy) {
            uris.forEach { uri -> runCatching { context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } }
            imagePaths = ArrayList((imagePaths + uris.map { it.toString() }).distinct().take(3))
        }
    }
    LaunchedEffect(orderId) { viewModel.load(orderId) }
    LaunchedEffect(state.submitted) { if (state.submitted) onChanged() }
    Dialog(onDismissRequest = { if (!state.busy) onDismiss() }) {
        Surface(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().heightIn(max = 720.dp).verticalScroll(rememberScrollState()).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Khiếu nại đơn #$orderId", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                if (state.loading || state.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error)
                    if (!state.busy) TextButton(onClick = { viewModel.load(orderId) }) { Text("Tải lại") } }
                val complaint = state.context?.complaint
                if (complaint != null) {
                    Text(complaintStatus(complaint.status), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    if (complaint.status in listOf("OPEN", "REVIEWING")) Text(if (complaint.orderStatus == "COMPLETED")
                        "Khiếu nại đang chờ admin xử lý." else "Đơn đang tạm dừng hoàn thành trong khi admin xử lý.")
                    complaint.outcome?.let { Text("Kết quả: " + when (it) {
                        "COMPLETE_ORDER" -> "Admin xác nhận hoàn thành đơn"
                        "CANCEL_ORDER" -> "Admin đã đóng / hủy đơn"
                        else -> "Giữ nguyên trạng thái đơn"
                    }, fontWeight = FontWeight.Bold) }
                    Text(complaintReasons[complaint.reason] ?: complaint.reason, fontWeight = FontWeight.SemiBold)
                    Text(complaint.description)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        complaint.imageUrls.forEach { url -> AsyncImage(url, "Ảnh minh chứng", Modifier.size(90.dp)) }
                    }
                    Text("Lịch sử xử lý", fontWeight = FontWeight.Bold)
                    complaint.events.forEach { event ->
                        Text("${complaintStatus(event.status)} • ${event.occurredAt.replace('T', ' ').take(16)}", style = MaterialTheme.typography.labelMedium)
                        Text(event.message, style = MaterialTheme.typography.bodyMedium)
                    }
                } else if (state.context?.canCreate == true) {
                    Text("Khiếu nại sẽ được gửi tới admin. Đơn tạm dừng tự hoàn thành trong thời gian xử lý.", style = MaterialTheme.typography.bodySmall)
                    complaintReasons.forEach { (key, label) ->
                        Row { RadioButton(reason == key, onClick = { reason = key }, enabled = !state.busy)
                            TextButton(onClick = { reason = key }, enabled = !state.busy) { Text(label) } }
                    }
                    OutlinedTextField(description, { if (it.length <= 2000) description = it }, label = { Text("Mô tả sự việc") },
                        supportingText = { Text("${description.trim().length}/2000 ký tự • tối thiểu 10") },
                        minLines = 3, enabled = !state.busy, modifier = Modifier.fillMaxWidth())
                    OutlinedButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !state.busy && imagePaths.size < 3, modifier = Modifier.fillMaxWidth()) { Text("Thêm ảnh minh chứng (${imagePaths.size}/3)") }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        imagePaths.forEach { path -> Column {
                            AsyncImage(Uri.parse(path), "Ảnh đã chọn", Modifier.size(72.dp))
                            TextButton(onClick = { imagePaths = ArrayList(imagePaths - path) }, enabled = !state.busy) { Text("Xóa") }
                        } }
                    }
                    Button(onClick = { viewModel.submit(orderId, reason, description, imagePaths.map(Uri::parse)) },
                        enabled = !state.busy && description.trim().length in 10..2000,
                        modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text(if (state.busy) "Đang gửi…" else "Gửi khiếu nại") }
                } else if (!state.loading && state.error == null) Text("Đơn chưa đủ điều kiện gửi khiếu nại hoặc đã hết thời hạn.")
                TextButton(onClick = onDismiss, enabled = !state.busy, modifier = Modifier.fillMaxWidth()) { Text("Đóng") }
            }
        }
    }
}
