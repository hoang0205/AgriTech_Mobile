package com.example.agritech_mobile.ui.dashboard

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.agritech_mobile.data.remote.dto.EditableProductResponse
import com.example.agritech_mobile.ui.theme.AgritechTheme

@Composable
fun EditProductScreen(
    onBackClick: () -> Unit,
    viewModel: EditProductViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmDiscard by remember { mutableStateOf(false) }
    val leave: () -> Unit = {
        if (!state.isSaving && !state.isUploading && !state.isGeneratingDescription) {
            if (state.hasChanges) confirmDiscard = true else onBackClick()
        }
    }
    BackHandler { leave() }
    LaunchedEffect(state.saved) {
        if (state.saved) {
            Toast.makeText(context, "Đã cập nhật sản phẩm", Toast.LENGTH_SHORT).show()
            onBackClick()
        }
    }
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris -> viewModel.addImages(context.applicationContext, uris) }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Bỏ thay đổi?") },
            text = { Text("Các thông tin bạn vừa chỉnh sửa chưa được lưu.") },
            confirmButton = { TextButton(onClick = onBackClick) { Text("Bỏ thay đổi") } },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Tiếp tục sửa") } }
        )
    }
    EditProductContent(
        state = state,
        onBackClick = leave,
        onChange = viewModel::change,
        onAddImages = { imagePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
        onRemoveImage = viewModel::removeImage,
        onSave = viewModel::save,
        onRetry = viewModel::load,
        onRefreshStock = viewModel::refreshStock,
        onGenerateDescription = viewModel::generateDescription
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductContent(
    state: ProductEditUiState,
    onBackClick: () -> Unit,
    onChange: (String, String) -> Unit,
    onAddImages: () -> Unit,
    onRemoveImage: (Int) -> Unit,
    onSave: () -> Unit,
    onRetry: () -> Unit,
    onRefreshStock: () -> Unit,
    onGenerateDescription: () -> Unit = {}
) {
    val green = Color(0xFF236B3B)
    val scrollState = rememberScrollState()
    LaunchedEffect(state.error) {
        if (state.error != null) scrollState.animateScrollTo(0)
    }
    Scaffold(
        containerColor = Color(0xFFF5F7F4),
        topBar = {
            TopAppBar(
                title = { Text("Chỉnh sửa sản phẩm", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, enabled = !state.isSaving && !state.isUploading && !state.isGeneratingDescription) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFF5F7F4))
            )
        },
        bottomBar = {
            if (state.original != null) {
                Surface(color = Color.White, shadowElevation = 12.dp) {
                    Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp)) {
                        Button(
                            onClick = onSave,
                            enabled = state.hasChanges && !state.isBusy && !state.stockConflict && !state.saved,
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = green)
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(if (state.isSaving) "Đang lưu…" else "Lưu thay đổi", fontWeight = FontWeight.Bold)
                        }
                        Text(
                            if (state.isGeneratingDescription) "AI đang soạn mô tả…"
                            else if (state.isUploading) "Đang kiểm tra và tải ảnh lên…"
                            else if (state.hasChanges) "Thay đổi sẽ hiển thị sau khi bạn lưu."
                            else "Bạn đang xem thông tin hiện tại của sản phẩm.",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF747E76),
                            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    ) { padding ->
        if (state.isLoading && state.original == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = green)
            }
        } else if (state.original == null) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(48.dp), tint = green)
                Spacer(Modifier.height(16.dp))
                Text(state.error ?: "Không tải được sản phẩm")
                TextButton(onClick = onRetry) { Text("Tải lại") }
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF195D34), Color(0xFF367C4F))))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(color = Color.White.copy(alpha = 0.15f), shape = RoundedCornerShape(8.dp)) {
                            Text("SẢN PHẨM CỦA TÔI", color = Color.White,
                                style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(8.dp))
                        }
                        Text(state.name.ifBlank { "Thông tin sản phẩm" }, color = Color.White,
                            style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold,
                            maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("Chăm chút thông tin để người mua hiểu rõ sản phẩm của bạn.",
                            color = Color(0xFFDCEBDD), style = MaterialTheme.typography.bodyMedium)
                    }
                }

                state.error?.let { error ->
                    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.fillMaxWidth().padding(16.dp)) {
                            Text(error, color = MaterialTheme.colorScheme.onErrorContainer)
                            if (state.stockConflict) {
                                TextButton(onClick = onRefreshStock, enabled = !state.isBusy) { Text("Đọc lại tồn kho") }
                                Text("Các thông tin khác bạn đã sửa vẫn được giữ lại.",
                                    style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                ProductEditSection(Icons.Default.PhotoLibrary, "Ảnh sản phẩm", "Ảnh đầu tiên được dùng làm ảnh bìa") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(state.imageUrls) { index, image ->
                            Box(Modifier.size(116.dp).clip(RoundedCornerShape(14.dp))) {
                                ProductImage(image, "Ảnh sản phẩm ${index + 1}", Modifier.fillMaxSize())
                                FilledIconButton(
                                    onClick = { onRemoveImage(index) }, enabled = !state.isBusy,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)
                                ) { Icon(Icons.Default.Close, contentDescription = "Xóa ảnh", modifier = Modifier.size(16.dp)) }
                                if (index == 0) {
                                    Surface(color = green, shape = RoundedCornerShape(topEnd = 8.dp),
                                        modifier = Modifier.align(Alignment.BottomStart)) {
                                        Text("Ảnh bìa", color = Color.White, style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                        if (state.imageUrls.size < 5) {
                            item {
                                OutlinedButton(
                                    onClick = onAddImages, enabled = !state.isBusy,
                                    modifier = Modifier.size(116.dp), contentPadding = PaddingValues(8.dp),
                                    shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color(0xFFB8CEBD))
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (state.isUploading) CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                        else Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                                        Spacer(Modifier.height(6.dp))
                                        Text("Thêm ảnh", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                    Text(state.fieldErrors["images"] ?: "${state.imageUrls.size}/5 ảnh · Chọn ảnh rõ nét, đúng sản phẩm.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (state.fieldErrors.containsKey("images")) MaterialTheme.colorScheme.error else Color(0xFF747E76))
                }

                ProductEditSection(Icons.Default.Spa, "Thông tin sản phẩm", "Tên gọi, danh mục và cách tính số lượng") {
                    ProductEditField("name", "Tên sản phẩm", state.name, state, onChange)
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded && !state.isBusy,
                        onExpandedChange = { if (!state.isBusy) expanded = !expanded }) {
                        OutlinedTextField(
                            value = state.category, onValueChange = {}, readOnly = true, enabled = !state.isBusy,
                            label = { Text("Danh mục") }, shape = RoundedCornerShape(12.dp),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor(),
                            isError = state.fieldErrors.containsKey("category"),
                            supportingText = state.fieldErrors["category"]?.let { message -> { Text(message) } }
                        )
                        ExposedDropdownMenu(expanded = expanded && !state.isBusy, onDismissRequest = { expanded = false }) {
                            editableProductCategories.forEach { category ->
                                DropdownMenuItem(text = { Text(category) }, onClick = { onChange("category", category); expanded = false })
                            }
                        }
                    }
                    ProductEditField("unit", "Đơn vị tính", state.unit, state, onChange, hint = "Ví dụ: kg, bó, túi, hộp")
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("kg", "g", "bó", "túi", "hộp", "quả").forEach { unit ->
                            item {
                                FilterChip(selected = state.unit == unit, enabled = !state.isBusy,
                                    onClick = { onChange("unit", unit) }, label = { Text(unit) })
                            }
                        }
                    }
                }

                ProductEditSection(Icons.Default.Payments, "Giá bán & giá vốn", "Giá tính trên mỗi ${state.unit.ifBlank { "đơn vị" }}") {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        ProductEditField("price", "Giá bán (đ)", state.price, state, onChange,
                            modifier = Modifier.weight(1f), numeric = true, hint = "Ví dụ: 25000")
                        ProductEditField("costPrice", "Giá vốn (đ)", state.costPrice, state, onChange,
                            modifier = Modifier.weight(1f), numeric = true, hint = "Có thể để trống")
                    }
                    Text("Giá vốn chỉ bạn nhìn thấy. Để trống nếu chưa xác định.",
                        style = MaterialTheme.typography.bodySmall, color = Color(0xFF747E76))
                    val price = parseEditNumber(state.price)
                    val cost = parseEditNumber(state.costPrice)
                    if (price != null && price > 0 && cost != null && cost >= 0) {
                        val difference = price - cost
                        Surface(color = Color(0xFFF0F6EE), shape = RoundedCornerShape(12.dp)) {
                            Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Lãi gộp / ${state.unit.ifBlank { "đơn vị" }}", style = MaterialTheme.typography.bodySmall)
                                Text(formatProductPrice(difference), fontWeight = FontWeight.Bold,
                                    color = if (difference >= 0) green else MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }

                ProductEditSection(Icons.Default.Inventory2, "Tồn kho", "Số lượng hiện có để bán") {
                    ProductEditField("quantity", "Số lượng (${state.unit.ifBlank { "đơn vị" }})", state.quantity,
                        state, onChange, numeric = true, hint = "Nhập 0 để hiển thị hết hàng. Có thể dùng số thập phân.")
                }

                ProductEditSection(Icons.Default.Notes, "Mô tả sản phẩm", "Nguồn gốc, chất lượng và hướng dẫn bảo quản") {
                    ProductEditField("characteristics", "Đặc điểm để AI viết mô tả", state.characteristics, state, onChange,
                        multiline = true, hint = "VD: Cam Cao Phong, ngọt dịu, ít hạt, túi 2 kg. ${state.characteristics.length}/1000 ký tự")
                    Button(onClick = onGenerateDescription, enabled = !state.isBusy,
                        colors = ButtonDefaults.buttonColors(containerColor = green)) {
                        if (state.isGeneratingDescription) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.isGeneratingDescription) "Đang tạo mô tả…" else "Tạo mô tả bằng AI")
                    }
                    state.descriptionError?.let { Text(it, color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall) }
                    Text("AI sẽ thay mô tả khi soạn xong. Bạn có thể chỉnh sửa rồi lưu thay đổi.",
                        style = MaterialTheme.typography.bodySmall)
                    ProductEditField("description", "Mô tả", state.description, state, onChange,
                        multiline = true, hint = "${state.description.length}/10.000 ký tự")
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun ProductEditSection(icon: ImageVector, title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = Color.White, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(Modifier.size(38.dp).clip(CircleShape).background(Color(0xFFEDF4EA)), contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = null, tint = Color(0xFF236B3B), modifier = Modifier.size(20.dp))
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
private fun ProductEditField(
    field: String, label: String, value: String, state: ProductEditUiState,
    onChange: (String, String) -> Unit, modifier: Modifier = Modifier,
    numeric: Boolean = false, multiline: Boolean = false, hint: String? = null
) {
    val message = state.fieldErrors[field] ?: hint
    OutlinedTextField(
        value = value, onValueChange = { onChange(field, it) }, enabled = !state.isBusy,
        label = { Text(label) }, modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
        singleLine = !multiline, minLines = if (multiline) 5 else 1,
        isError = state.fieldErrors.containsKey(field),
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Decimal else KeyboardType.Text),
        supportingText = message?.let { text -> { Text(text) } }
    )
}

@Preview(showBackground = true, widthDp = 411, heightDp = 1000, locale = "vi")
@Composable
private fun EditProductPreview() {
    val product = EditableProductResponse("1", "Cà chua sạch tại vườn", "Rau củ", 25000.0, 20.0,
        "kg", "Cà chua thu hoạch trong ngày, bảo quản nơi thoáng mát.", emptyList(), 15000.0)
    AgritechTheme {
        EditProductContent(
            state = ProductEditUiState(original = product, name = product.name, category = product.category,
                price = "28000", costPrice = "15000", quantity = "20", unit = "kg",
                description = product.description, isLoading = false),
            onBackClick = {}, onChange = { _, _ -> }, onAddImages = {}, onRemoveImage = {},
            onSave = {}, onRetry = {}, onRefreshStock = {}
        )
    }
}
