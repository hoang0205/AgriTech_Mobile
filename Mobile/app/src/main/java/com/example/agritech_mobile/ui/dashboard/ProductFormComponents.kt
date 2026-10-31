package com.example.agritech_mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

internal val ProductFormGreen = Color(0xFF236B3B)
internal val ProductFormBackground = Color(0xFFF5F7F4)

@Composable
internal fun ProductFormHeader(label: String, name: String, subtitle: String) {
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
        .background(Brush.linearGradient(listOf(Color(0xFF195D34), Color(0xFF367C4F)))).padding(20.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                Text(label, color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
            }
            Text(name, color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(subtitle, color = Color(0xFFDCEBDD), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
internal fun ProductFormSection(icon: ImageVector, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFEDF4EA)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = ProductFormGreen, modifier = Modifier.size(20.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF747E76))
                }
            }
            content()
        }
    }
}

@Composable
internal fun ProductFormField(
    label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, error: String? = null, hint: String? = null,
    multiline: Boolean = false, keyboardType: KeyboardType = KeyboardType.Text
) {
    val message = error ?: hint
    OutlinedTextField(value = value, onValueChange = onValueChange, enabled = enabled,
        label = { Text(label) }, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        singleLine = !multiline, minLines = if (multiline) 5 else 1, isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        supportingText = message?.let { text -> { Text(text) } })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ProductFormCategory(value: String, onValueChange: (String) -> Unit, enabled: Boolean = true, error: String? = null) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = !expanded }) {
        OutlinedTextField(value = value, onValueChange = {}, readOnly = true, enabled = enabled,
            label = { Text("Danh mục") }, shape = RoundedCornerShape(12.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(), isError = error != null,
            supportingText = error?.let { message -> { Text(message) } })
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            editableProductCategories.forEach { category ->
                DropdownMenuItem(text = { Text(category) }, onClick = { onValueChange(category); expanded = false })
            }
        }
    }
}
