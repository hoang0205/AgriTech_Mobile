package com.example.agritech_mobile.ui.order

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal object CheckoutStyle {
    val Green = Color(0xFF1B5E20)
    val PaleGreen = Color(0xFFE8F5E9)
    val Background = Color(0xFFF8FAF9)
    val Outline = Color(0xFFE8EDE9)
    val Muted = Color(0xFF757575)
    val CardShape = RoundedCornerShape(16.dp)
    val ButtonShape = RoundedCornerShape(12.dp)
}

@Composable
internal fun CheckoutPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), color = Color.White, shape = CheckoutStyle.CardShape,
        border = BorderStroke(1.dp, CheckoutStyle.Outline)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun CheckoutPriceRow(label: String, value: String, prominent: Boolean = false) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium,
            color = if (prominent) Color(0xFF212121) else CheckoutStyle.Muted)
        Text(value, style = if (prominent) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            fontWeight = if (prominent) FontWeight.Bold else FontWeight.Medium,
            color = if (prominent) CheckoutStyle.Green else Color(0xFF333333))
    }
}

@Composable
internal fun CheckoutAddressCard(name: String, phone: String, address: String, onClick: () -> Unit,
    selected: Boolean = true, selectable: Boolean = false, enabled: Boolean = true) {
    Surface(onClick = onClick, enabled = enabled, modifier = Modifier.fillMaxWidth(),
        shape = CheckoutStyle.CardShape, color = if (selected) CheckoutStyle.PaleGreen else Color.White,
        border = BorderStroke(1.dp, if (selected) CheckoutStyle.Green else CheckoutStyle.Outline)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(if (selectable) {
                if (selected) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked
            } else Icons.Default.LocationOn, null, tint = if (selected) CheckoutStyle.Green else CheckoutStyle.Muted)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(name.ifBlank { "Chọn địa chỉ nhận hàng" }, fontWeight = FontWeight.SemiBold)
                if (phone.isNotBlank()) Text(phone, style = MaterialTheme.typography.bodySmall, color = CheckoutStyle.Muted)
                Text(address.ifBlank { "Thêm địa chỉ để tính phí giao hàng" }, style = MaterialTheme.typography.bodySmall, color = CheckoutStyle.Muted)
            }
            if (!selectable) Icon(Icons.Default.ChevronRight, "Đổi địa chỉ", tint = CheckoutStyle.Green)
        }
    }
}

@Composable
internal fun CheckoutActionBar(text: String, total: String, onClick: () -> Unit, enabled: Boolean,
    busy: Boolean = false, navigationPadding: Boolean = true) {
    Surface(color = Color.White, shadowElevation = 8.dp, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.then(if (navigationPadding) Modifier.navigationBarsPadding() else Modifier).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CheckoutPriceRow("Tổng thanh toán", total, true)
            Button(onClick = onClick, enabled = enabled && !busy, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                shape = CheckoutStyle.ButtonShape, colors = ButtonDefaults.buttonColors(containerColor = CheckoutStyle.Green),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = LocalContentColor.current)
                else Icon(Icons.Default.ShoppingBag, null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(text, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
