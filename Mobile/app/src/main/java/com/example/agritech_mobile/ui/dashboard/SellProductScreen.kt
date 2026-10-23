package com.example.agritech_mobile.ui.dashboard

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.Job
import com.example.agritech_mobile.di.bitmapToUri
import com.example.agritech_mobile.di.uriToMultipartBodyPart
import com.example.agritech_mobile.ui.theme.AgritechTheme

data class SellProductUiState(
    val images: List<Uri> = emptyList(),
    val invalidImages: List<Uri> = emptyList(),
    val productName: String = "",
    val category: String = "",
    val price: String = "",
    val unit: String = "",
    val quantity: Int = 0,
    val description: String = "",
    val characteristics: String = "",
    val isLoading: Boolean = false,
    val isPredictingPrice: Boolean = false,
    val isGeneratingDescription: Boolean = false,
    val priceSuggestion: String? = null,
    val costPrice: String = "",
)

private fun parseProductPrice(value: String): Double? {
    return value.trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it > 0.0 }
}

private fun parseProductCost(value: String): Double? {
    return value.trim()
        .replace(',', '.')
        .toDoubleOrNull()
        ?.takeIf { it.isFinite() && it >= 0.0 }
}

@Composable
fun SellProductScreen(
    onBackClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    var uiState by remember { mutableStateOf(SellProductUiState()) }
    var descriptionJob by remember { mutableStateOf<Job?>(null) }
    var descriptionInputVersion by remember { mutableIntStateOf(0) }
    DisposableEffect(viewModel) {
        onDispose { descriptionJob?.cancel() }
    }

    val dashboardState by viewModel.dashboardState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(dashboardState) {
        when (dashboardState) {
            is DashboardState.Loading -> {
                uiState = uiState.copy(isLoading = true)
            }

            is DashboardState.ActionSuccess -> {
                uiState = uiState.copy(isLoading = false)
                val msg = (dashboardState as DashboardState.ActionSuccess).message
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
                onBackClick()
            }

            is DashboardState.Error -> {
                uiState = uiState.copy(isLoading = false)
                val err = (dashboardState as DashboardState.Error).error
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
                viewModel.resetState()
            }

            else -> {
                uiState = uiState.copy(isLoading = false)
            }
        }
    }

    val multiplePhotoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 5)
    ) { uris ->
        if (uris.isNotEmpty()) {
            uris.forEach { uri ->
                val part = uriToMultipartBodyPart(context, uri, "file")
                if (part != null) {
                    viewModel.verifyImageWithAI(part) { isValid, detectedCategory ->
                        if (isValid) {
                            val newImages = (uiState.images + uri).take(5)
                            uiState = uiState.copy(images = newImages)

                            if (detectedCategory != null && uiState.category.isBlank()) {
                                uiState = uiState.copy(category = detectedCategory)
                                Toast.makeText(context, "AI phát hiện: $detectedCategory", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Ảnh không hợp lệ, vui lòng chọn ảnh khác", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val uri = bitmapToUri(context, bitmap)
            if (uri != null) {
                val part = uriToMultipartBodyPart(context, uri, "file")
                if (part != null) {
                    viewModel.verifyImageWithAI(part) { isValid, detectedCategory ->
                        if (isValid) {
                            val newImages = (uiState.images + uri).take(5)
                            uiState = uiState.copy(images = newImages)

                            if (detectedCategory != null && uiState.category.isBlank()) {
                                uiState = uiState.copy(category = detectedCategory)
                                Toast.makeText(context, "AI phát hiện: $detectedCategory", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            Toast.makeText(context, "Ảnh không hợp lệ, vui lòng chụp lại", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    fun handlePublish() {
        if (uiState.isGeneratingDescription || uiState.isLoading) return
        if (uiState.images.isEmpty()) {
            Toast.makeText(context, "Vui lòng chọn ít nhất 1 ảnh sản phẩm!", Toast.LENGTH_SHORT)
                .show()
            return
        }
        if (uiState.productName.isBlank()) {
            Toast.makeText(context, "Vui lòng nhập tên sản phẩm!", Toast.LENGTH_SHORT).show()
            return
        }
        if (uiState.category.isBlank()) {
            Toast.makeText(context, "Vui lòng chọn danh mục!", Toast.LENGTH_SHORT).show()
            return
        }
        val safePrice = parseProductPrice(uiState.price)

        if (safePrice == null) {
            Toast.makeText(
                context,
                "Giá bán phải là số lớn hơn 0. Ví dụ: 20000",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val safeCost = parseProductCost(uiState.costPrice)

        if (uiState.costPrice.isNotBlank() && safeCost == null) {
            Toast.makeText(
                context,
                "Giá vốn phải là số không âm. Ví dụ: 15000",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if (uiState.unit.isBlank()) {
            Toast.makeText(
                context,
                "Vui lòng nhập đơn vị tính (VD: kg, hộp...)!",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (uiState.quantity <= 0) {
            Toast.makeText(context, "Số lượng sản phẩm phải lớn hơn 0!", Toast.LENGTH_SHORT).show()
            return
        }
        if (uiState.description.isBlank()) {
            Toast.makeText(context, "Vui lòng nhập mô tả chi tiết sản phẩm!", Toast.LENGTH_SHORT)
                .show()
            return
        }
        val safeQuantity = uiState.quantity.toDouble()
        val imageParts = uiState.images.mapNotNull { uri ->
            uriToMultipartBodyPart(context, uri, paramName = "files")
        }
        if (imageParts.isNotEmpty()) {
            viewModel.publishProduct(
                name = uiState.productName,
                category = uiState.category,
                price = safePrice,
                quantity = safeQuantity,
                unit = uiState.unit,
                description = uiState.description,
                imageParts = imageParts,
                costPrice = safeCost
            )
        } else {
            Toast.makeText(context, "Lỗi xử lý ảnh, vui lòng thử lại!", Toast.LENGTH_SHORT).show()
        }
    }

    SellProductContent(
        uiState = uiState,
        onNameChange = {
            descriptionInputVersion++
            uiState = uiState.copy(
                productName = it,
                priceSuggestion = null
            )
        },
        onPriceChange = { uiState = uiState.copy(price = it) },
        onCostPriceChange = {
            uiState = uiState.copy(costPrice = it)
        },
        onUnitChange = { uiState = uiState.copy(unit = it) },
        onQuantityChange = { uiState = uiState.copy(quantity = it) },
        onDescriptionChange = { if (!uiState.isGeneratingDescription) uiState = uiState.copy(description = it) },
        onCharacteristicsChange = {
            descriptionInputVersion++
            uiState = uiState.copy(characteristics = it)
        },
        onCategoryChange = { selectedCat ->
            descriptionInputVersion++
            uiState = uiState.copy(category = selectedCat)
        },
        onImageUploadClick = {
            multiplePhotoPickerLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        },
        onCameraCaptureClick = {
            cameraLauncher.launch(null)
        },
        onRemoveImage = { uriToRemove ->
            uiState = uiState.copy(
                images = uiState.images - uriToRemove,
                invalidImages = uiState.invalidImages - uriToRemove
            )
        },
        onAddCertClick = { },
        onBackClick = onBackClick,
        onPublishClick = {
            if (!uiState.isLoading) {
                handlePublish()
            }
        },
        onPredictPriceClick = {
            if (uiState.productName.isBlank()) {
                Toast.makeText(
                    context,
                    "Vui lòng nhập tên sản phẩm trước!",
                    Toast.LENGTH_SHORT
                ).show()
            } else if (!uiState.isPredictingPrice) {
                val requestedName = uiState.productName

                uiState = uiState.copy(
                    isPredictingPrice = true,
                    priceSuggestion = null
                )

                viewModel.getPricePrediction(requestedName) { predictedPrice, message ->
                    uiState = uiState.copy(isPredictingPrice = false)

                    if (uiState.productName == requestedName) {
                        val isKg = uiState.unit.trim()
                            .equals("kg", ignoreCase = true)

                        val unitNote = if (isKg) "" else
                            "\nGiá tham khảo tính theo kg. Bạn cần tự quy đổi theo đơn vị bán."

                        uiState = uiState.copy(
                            priceSuggestion = message + unitNote,
                            price = if (predictedPrice != null && isKg)
                                predictedPrice
                            else
                                uiState.price
                        )
                    }
                }
            }
        },
        onGenerateDescriptionClick = {
            if (uiState.productName.isBlank()) {
                Toast.makeText(context, "Vui lòng nhập tên sản phẩm trước!", Toast.LENGTH_SHORT).show()
            } else if (uiState.category.isBlank()) {
                Toast.makeText(context, "Vui lòng chọn danh mục trước!", Toast.LENGTH_SHORT).show()
            } else if (uiState.characteristics.isBlank()) {
                Toast.makeText(context, "Vui lòng nhập đặc điểm sản phẩm trước!", Toast.LENGTH_SHORT).show()
            } else if (!uiState.isGeneratingDescription) {
                val requestedVersion = descriptionInputVersion
                val requestedName = uiState.productName
                val requestedCategory = uiState.category
                val generated = StringBuilder()
                uiState = uiState.copy(isGeneratingDescription = true)
                descriptionJob = viewModel.generateDescriptionStream(
                    productName = uiState.productName,
                    category = uiState.category,
                    characteristics = uiState.characteristics,
                    onToken = { token ->
                        generated.append(token.replace("*", ""))
                    },
                    onComplete = {
                        val matchesInput = requestedVersion == descriptionInputVersion &&
                            requestedName == uiState.productName && requestedCategory == uiState.category
                        val description = generated.toString().trim()
                        if (matchesInput && description.isNotBlank()) {
                            uiState = uiState.copy(description = description)
                        }
                        uiState = uiState.copy(isGeneratingDescription = false)
                        val message = if (!matchesInput) "Thông tin đã thay đổi. Vui lòng tạo lại mô tả."
                            else if (description.isBlank()) "AI chưa trả về mô tả. Vui lòng thử lại."
                            else "AI đã soạn xong mô tả!"
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    },
                    onError = { err ->
                        uiState = uiState.copy(isGeneratingDescription = false)
                        Toast.makeText(context, "Lỗi: $err", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellProductContent(
    uiState: SellProductUiState,
    onNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onQuantityChange: (Int) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onImageUploadClick: () -> Unit,
    onCameraCaptureClick: () -> Unit,
    onRemoveImage: (Uri) -> Unit,
    onAddCertClick: () -> Unit,
    onBackClick: () -> Unit,
    onPublishClick: () -> Unit,
    onPredictPriceClick: () -> Unit,
    onGenerateDescriptionClick: () -> Unit,
    onCostPriceChange: (String) -> Unit = {},
    onCharacteristicsChange: (String) -> Unit = {}
) {
    val busy = uiState.isLoading || uiState.isGeneratingDescription || uiState.isPredictingPrice
    val valid = uiState.images.isNotEmpty() && uiState.invalidImages.isEmpty() &&
        uiState.productName.isNotBlank() && uiState.category.isNotBlank() &&
        parseProductPrice(uiState.price) != null &&
        (uiState.costPrice.isBlank() || parseProductCost(uiState.costPrice) != null) &&
        uiState.unit.isNotBlank() && uiState.quantity > 0 && uiState.description.isNotBlank()
    Scaffold(
        containerColor = ProductFormBackground,
        topBar = {
            TopAppBar(
                title = { Text("Thêm sản phẩm", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick, enabled = !busy) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = ProductFormBackground)
            )
        },
        bottomBar = {
            Surface(color = Color.White, shadowElevation = 12.dp) {
                Column(Modifier.fillMaxWidth().navigationBarsPadding().imePadding().padding(16.dp)) {
                    Button(
                        onClick = onPublishClick, enabled = valid && !busy,
                        modifier = Modifier.fillMaxWidth().height(54.dp), shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ProductFormGreen)
                    ) {
                        if (uiState.isLoading) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.isLoading) "Đang đăng…" else "Đăng sản phẩm", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        when {
                            uiState.isGeneratingDescription -> "AI đang soạn mô tả…"
                            uiState.isPredictingPrice -> "AI đang tìm giá tham khảo…"
                            else -> "Kiểm tra thông tin trước khi đăng bán."
                        },
                        style = MaterialTheme.typography.labelSmall, color = Color(0xFF747E76),
                        modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProductFormHeader("ĐĂNG BÁN SẢN PHẨM", uiState.productName.ifBlank { "Thêm sản phẩm mới" },
                "Chăm chút thông tin để người mua hiểu rõ sản phẩm của bạn.")

            ProductFormSection(Icons.Default.PhotoLibrary, "Ảnh sản phẩm", "Ảnh đầu tiên được dùng làm ảnh bìa") {
                if (uiState.images.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(uiState.images) { index, uri ->
                            Box(Modifier.size(116.dp).clip(RoundedCornerShape(14.dp))) {
                                AsyncImage(model = uri, contentDescription = "Ảnh sản phẩm ${index + 1}",
                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                if (uri in uiState.invalidImages) {
                                    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.error.copy(alpha = 0.4f)))
                                }
                                FilledIconButton(
                                    onClick = { onRemoveImage(uri) }, enabled = !busy,
                                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp).size(28.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = Color.White)
                                ) { Icon(Icons.Default.Close, "Xóa ảnh", Modifier.size(16.dp)) }
                                if (index == 0) {
                                    Surface(color = ProductFormGreen, shape = RoundedCornerShape(topEnd = 8.dp),
                                        modifier = Modifier.align(Alignment.BottomStart)) {
                                        Text("Ảnh bìa", color = Color.White, style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                if (uiState.images.size < 5) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(onClick = onImageUploadClick, enabled = !busy,
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Chọn ảnh")
                        }
                        OutlinedButton(onClick = onCameraCaptureClick, enabled = !busy,
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                            Icon(Icons.Default.PhotoCamera, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Chụp ảnh")
                        }
                    }
                }
                Text("${uiState.images.size}/5 ảnh · Chọn ảnh rõ nét, đúng sản phẩm.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF747E76))
            }

            ProductFormSection(Icons.Default.Spa, "Thông tin sản phẩm", "Tên gọi, danh mục và cách tính số lượng") {
                ProductFormField("Tên sản phẩm", uiState.productName, onNameChange, enabled = !busy)
                ProductFormCategory(uiState.category, onCategoryChange, enabled = !busy)
                ProductFormField("Đơn vị tính", uiState.unit, onUnitChange, enabled = !busy, hint = "Ví dụ: kg, bó, túi, hộp")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("kg", "g", "bó", "túi", "hộp", "quả").forEach { unit ->
                        item {
                            FilterChip(selected = uiState.unit == unit, enabled = !busy,
                                onClick = { onUnitChange(unit) }, label = { Text(unit) })
                        }
                    }
                }
            }

            ProductFormSection(Icons.Default.Payments, "Giá bán & giá vốn", "Giá tính trên mỗi ${uiState.unit.ifBlank { "đơn vị" }}") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProductFormField("Giá bán (đ)", uiState.price, onPriceChange, modifier = Modifier.weight(1f),
                        enabled = !busy, hint = "Ví dụ: 25000", keyboardType = KeyboardType.Decimal)
                    ProductFormField("Giá vốn (đ)", uiState.costPrice, onCostPriceChange, modifier = Modifier.weight(1f),
                        enabled = !busy, hint = "Có thể để trống", keyboardType = KeyboardType.Decimal,
                        error = if (uiState.costPrice.isNotBlank() && parseProductCost(uiState.costPrice) == null) "Giá vốn phải là số không âm" else null)
                }
                Text("Giá vốn chỉ bạn nhìn thấy. Để trống được tính vốn 0 đ khi tính lãi.",
                    style = MaterialTheme.typography.bodySmall, color = Color(0xFF747E76))
                val sellingPrice = parseProductPrice(uiState.price)
                val cost = parseProductCost(uiState.costPrice)
                if (sellingPrice != null && cost != null) {
                    val difference = sellingPrice - cost
                    Surface(color = Color(0xFFF0F6EE), shape = RoundedCornerShape(12.dp)) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Lãi gộp / ${uiState.unit.ifBlank { "đơn vị" }}", style = MaterialTheme.typography.bodySmall)
                            Text(formatProductPrice(difference), fontWeight = FontWeight.Bold,
                                color = if (difference >= 0) ProductFormGreen else MaterialTheme.colorScheme.error)
                        }
                    }
                }
                OutlinedButton(onClick = onPredictPriceClick, enabled = !busy, shape = RoundedCornerShape(12.dp)) {
                    if (uiState.isPredictingPrice) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isPredictingPrice) "Đang tìm giá…" else "Gợi ý giá bằng AI")
                }
                uiState.priceSuggestion?.let { suggestion ->
                    Surface(color = Color(0xFFF0F6EE), shape = RoundedCornerShape(12.dp)) {
                        Text(suggestion, modifier = Modifier.fillMaxWidth().padding(14.dp),
                            style = MaterialTheme.typography.bodySmall, color = ProductFormGreen)
                    }
                }
            }

            ProductFormSection(Icons.Default.Inventory2, "Tồn kho", "Số lượng hiện có để bán") {
                ProductFormField("Số lượng (${uiState.unit.ifBlank { "đơn vị" }})",
                    if (uiState.quantity == 0) "" else uiState.quantity.toString(),
                    onValueChange = { input ->
                        if (input.isEmpty()) onQuantityChange(0)
                        else input.toIntOrNull()?.let(onQuantityChange)
                    }, enabled = !busy, keyboardType = KeyboardType.Number,
                    hint = "Nhập số lượng lớn hơn 0 để bắt đầu đăng bán.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalIconButton(onClick = { onQuantityChange(uiState.quantity - 1) }, enabled = !busy && uiState.quantity > 0) {
                        Icon(Icons.Default.Remove, "Giảm số lượng")
                    }
                    FilledTonalIconButton(onClick = { onQuantityChange(uiState.quantity + 1) }, enabled = !busy && uiState.quantity < Int.MAX_VALUE) {
                        Icon(Icons.Default.Add, "Tăng số lượng")
                    }
                }
            }

            ProductFormSection(Icons.Default.Notes, "Mô tả sản phẩm", "Nguồn gốc, chất lượng và hướng dẫn bảo quản") {
                ProductFormField("Đặc điểm để AI viết mô tả", uiState.characteristics,
                    onValueChange = { if (it.length <= 1000) onCharacteristicsChange(it) },
                    enabled = !busy, multiline = true,
                    hint = "VD: Cam Cao Phong, ngọt dịu, ít hạt, túi 2 kg. ${uiState.characteristics.length}/1000 ký tự")
                Button(onClick = onGenerateDescriptionClick, enabled = !busy,
                    colors = ButtonDefaults.buttonColors(containerColor = ProductFormGreen)) {
                    if (uiState.isGeneratingDescription) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                    else Icon(Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (uiState.isGeneratingDescription) "Đang tạo mô tả…" else "Tạo mô tả bằng AI")
                }
                Text("AI sẽ thay mô tả khi soạn xong. Bạn có thể chỉnh sửa trước khi đăng.", style = MaterialTheme.typography.bodySmall)
                ProductFormField("Mô tả", uiState.description, onDescriptionChange, enabled = !busy,
                    multiline = true, hint = "${uiState.description.length}/10.000 ký tự")
            }
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 1000, locale = "vi")
@Composable
fun SellProductScreenPreview() {
    AgritechTheme {
        SellProductContent(
            uiState = SellProductUiState(
                images = emptyList(),
                invalidImages = emptyList(),
                productName = "Cà chua sạch tại vườn",
                category = "Rau củ",
                price = "25000",
                costPrice = "15000",
                unit = "kg",
                quantity = 20,
                description = "Cà chua thu hoạch trong ngày, bảo quản nơi thoáng mát.",
                isLoading = false,
                isPredictingPrice = false
            ),
            onNameChange = {},
            onPriceChange = {},
            onUnitChange = {},
            onQuantityChange = {},
            onDescriptionChange = {},
            onCategoryChange = {},
            onImageUploadClick = {},
            onCameraCaptureClick = {},
            onRemoveImage = {},
            onAddCertClick = {},
            onBackClick = {},
            onPublishClick = {},
            onPredictPriceClick = {},
            onGenerateDescriptionClick = {}
        )
    }
}
