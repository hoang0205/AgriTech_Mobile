package com.example.agritech_mobile.ui.order

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.agritech_mobile.data.remote.dto.GhnArea
import com.example.agritech_mobile.data.remote.dto.GhnShop
import com.example.agritech_mobile.data.remote.dto.GhnSenderRequest
import com.example.agritech_mobile.data.repository.GhnRepository
import kotlinx.coroutines.CancellationException

@Composable
internal fun GhnSenderAddressForm(
    shop: GhnShop, repo: GhnRepository, busy: Boolean, forceOpen: Boolean,
    onSave: (GhnSenderRequest) -> Unit, onDraftChange: (GhnSenderRequest?) -> Unit
) {
    var expanded by remember(shop.shopId) { mutableStateOf(shop.senderAddress == null) }
    var address by remember(shop.shopId, shop.senderAddress) {
        mutableStateOf(shop.senderAddress?.address ?: suggestedSenderAddress(shop))
    }
    var provinces by remember { mutableStateOf(emptyList<GhnArea>()) }
    var wards by remember { mutableStateOf(emptyList<GhnArea>()) }
    var province by remember(shop.shopId, shop.senderAddress) { mutableStateOf(shop.senderAddress?.let { GhnArea(it.provinceId.toString(), it.provinceName) }) }
    var ward by remember(shop.shopId, shop.senderAddress) { mutableStateOf(shop.senderAddress?.let { GhnArea(it.wardCode, it.wardName) }) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(address, province?.id, ward?.id, loading, error) {
        onDraftChange(if (!loading && error == null && address.trim().length >= 5 && province != null && ward != null)
            GhnSenderRequest(address, province!!.id.toInt(), ward!!.id) else null)
    }
    LaunchedEffect(forceOpen) { if (forceOpen) expanded = true }
    LaunchedEffect(shop.senderAddress) { if (shop.senderAddress != null) expanded = false }
    LaunchedEffect(expanded, province?.id, retry) {
        if (!expanded) return@LaunchedEffect
        loading = true; error = null
        try {
            if (provinces.isEmpty()) provinces = repo.areas("provinces")
            val selected = province
            if (selected == null) province = matchRecipientProvince(address, provinces)
            else {
                wards = repo.areas("wards", selected.id.toInt())
                if (ward?.id !in wards.map { it.id }) ward = matchRecipientWard(address, selected, wards)
            }
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { error = e.message }
        finally { loading = false }
    }
    TextButton(enabled = !busy, onClick = { expanded = !expanded }) {
        Text(if (expanded) "Thu gọn địa chỉ lấy hàng" else "Chuẩn hóa địa chỉ lấy hàng")
    }
    if (expanded) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (province != null && ward != null) "Đã điền sẵn. Địa chỉ sẽ được lưu khi lấy báo giá."
                else "Đã lấy địa chỉ có sẵn. Chọn thêm khu vực chưa xác định được.", style = MaterialTheme.typography.bodySmall)
            OutlinedTextField(address, { address = it.take(500) }, label = { Text("Địa chỉ lấy hàng") }, enabled = !busy,
                modifier = Modifier.fillMaxWidth())
            GhnAreaPicker("Tỉnh/thành lấy hàng", province, provinces, !busy && !loading) {
                province = it; ward = null; wards = emptyList()
            }
            GhnAreaPicker("Phường/xã lấy hàng", ward, wards, !busy && !loading) { ward = it }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                TextButton(enabled = !busy && !loading, onClick = { retry++ }) { Text("Tải lại khu vực") }
            }
            Button(enabled = !busy && !loading && address.trim().length >= 5 && province != null && ward != null,
                onClick = { onSave(GhnSenderRequest(address, province!!.id.toInt(), ward!!.id)) }) { Text("Lưu địa chỉ lấy hàng") }
        }
    }
}

internal fun suggestedSenderAddress(shop: GhnShop): String =
    shop.suggestedPickupAddress?.takeIf { it.isNotBlank() } ?: shop.address.orEmpty()
