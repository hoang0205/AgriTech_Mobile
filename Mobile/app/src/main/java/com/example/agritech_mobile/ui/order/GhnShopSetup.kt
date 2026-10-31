package com.example.agritech_mobile.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agritech_mobile.data.remote.dto.GhnArea
import com.example.agritech_mobile.data.remote.dto.GhnShop
import com.example.agritech_mobile.data.remote.dto.GhnShopRequest
import com.example.agritech_mobile.data.remote.dto.GhnSenderRequest
import com.example.agritech_mobile.data.repository.GhnRepository
import kotlinx.coroutines.CancellationException

@Composable
internal fun GhnShopSetup(
    shop: GhnShop?, busy: Boolean, repo: GhnRepository,
    onConnect: () -> Unit, onRegister: (GhnShopRequest) -> Unit, onReload: () -> Unit,
    senderNeedsUpdate: Boolean, onSaveSender: (GhnSenderRequest) -> Unit,
    onSenderDraftChange: (GhnSenderRequest?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var province by remember { mutableStateOf<GhnArea?>(null) }
    var district by remember { mutableStateOf<GhnArea?>(null) }
    var ward by remember { mutableStateOf<GhnArea?>(null) }
    var provinces by remember { mutableStateOf(emptyList<GhnArea>()) }
    var districts by remember { mutableStateOf(emptyList<GhnArea>()) }
    var wards by remember { mutableStateOf(emptyList<GhnArea>()) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(expanded, province?.id, district?.id) {
        if (!expanded) return@LaunchedEffect
        loading = true; error = null
        try {
            if (provinces.isEmpty()) provinces = repo.shopAreas("provinces")
            districts = province?.let { repo.shopAreas("districts", it.id.toInt()) }.orEmpty()
            wards = district?.let { repo.shopAreas("wards", it.id.toInt()) }.orEmpty()
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message }
        finally { loading = false }
    }
    if (shop?.connected == true) {
        Text("Điểm lấy hàng GHN", style = MaterialTheme.typography.titleSmall)
        shop.name?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        Text("Shop ID: ${shop.shopId}", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(shop.senderAddress?.let { "${it.address}\n${it.wardName}, ${it.provinceName}" }
            ?: suggestedSenderAddress(shop).ifBlank { "Shop chưa có địa chỉ lấy hàng." }, style = MaterialTheme.typography.bodyMedium)
        GhnSenderAddressForm(shop, repo, busy, senderNeedsUpdate, onSaveSender, onSenderDraftChange)
    } else if (shop?.pending == true) {
        Text("Đang đối chiếu cửa hàng GHN", style = MaterialTheme.typography.titleSmall)
        Text("Kiểm tra kết nối để lấy kết quả lần tạo trước.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(enabled = !busy, onClick = onConnect) { Text("Kiểm tra kết nối") }
    } else if (shop == null) {
        OutlinedButton(enabled = !busy, onClick = onReload) { Text("Tải kết nối GHN") }
    } else {
        Text("Kết nối cửa hàng một lần để dùng GHN.", style = MaterialTheme.typography.bodySmall)
        OutlinedButton(enabled = !busy, onClick = onConnect) { Text("Kết nối shop đã có") }
        TextButton(enabled = !busy, onClick = { expanded = !expanded }) {
            Text(if (expanded) "Thu gọn" else "Chưa có shop? Tạo ngay")
        }
        if (expanded) {
            OutlinedTextField(name, { name = it.take(100) }, label = { Text("Tên cửa hàng") }, enabled = !busy, singleLine = true)
            OutlinedTextField(address, { address = it.take(500) }, label = { Text("Địa chỉ lấy hàng đầy đủ") }, enabled = !busy)
            Text("Chọn khu vực theo danh mục đăng ký shop GHN; tên địa phương có thể theo địa chỉ cũ.", style = MaterialTheme.typography.bodySmall)
            GhnAreaPicker("Tỉnh/thành lấy hàng", province, provinces, !busy && !loading) {
                province = it; district = null; ward = null; districts = emptyList(); wards = emptyList()
            }
            GhnAreaPicker("Quận/huyện lấy hàng", district, districts, !busy && !loading) {
                district = it; ward = null; wards = emptyList()
            }
            GhnAreaPicker("Phường/xã lấy hàng", ward, wards, !busy && !loading) { ward = it }
            Text("Dùng số điện thoại đăng nhập của bạn. COD được quản lý qua tài khoản GHN của AgriTech.", style = MaterialTheme.typography.bodySmall)
            Button(enabled = !busy && !loading && name.isNotBlank() && address.trim().length >= 5 && ward != null,
                onClick = { onRegister(GhnShopRequest(name, address, province!!.id.toInt(), district!!.id.toInt(), ward!!.id)) }) {
                Text("Tạo cửa hàng GHN")
            }
        }
    }
    if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
    error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
    Spacer(Modifier.height(2.dp))
}

internal fun ghnShopResultMessage(shop: GhnShop): String = when (shop.outcome) {
    "CREATED" -> "Đã tạo shop GHN mới, mã ${shop.shopId}."
    "CONNECTED_EXISTING" -> "Đã kết nối shop GHN có sẵn, mã ${shop.shopId}; không tạo thêm shop."
    "ALREADY_CONNECTED" -> "Bạn đã kết nối shop GHN mã ${shop.shopId}; không tạo thêm shop."
    "RECOVERED" -> "Đã tìm lại shop GHN từ lần tạo trước, mã ${shop.shopId}."
    else -> "Đã kết nối shop GHN mã ${shop.shopId}."
}
