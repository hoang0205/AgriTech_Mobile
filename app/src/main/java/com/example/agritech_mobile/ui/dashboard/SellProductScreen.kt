package com.example.agritech_mobile.ui.dashboard

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
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
    val isLoading: Boolean = false,
    val isPredictingPrice: Boolean = false
)

@Composable
fun SellProductScreen(
    onBackClick: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    var uiState by remember { mutableStateOf(SellProductUiState()) }

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
            val newImages = (uiState.images + uris).take(5)
            uiState = uiState.copy(images = newImages)

            uris.forEach { uri ->
                val part = uriToMultipartBodyPart(context, uri, "files")
                if (part != null) {
                    viewModel.verifyImageWithAI(part) { isOk ->
                        if (!isOk) {
                            uiState = uiState.copy(invalidImages = uiState.invalidImages + uri)
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
                val newImages = (uiState.images + uri).take(5)
                uiState = uiState.copy(images = newImages)

                val part = uriToMultipartBodyPart(context, uri, "files")
                if (part != null) {
                    viewModel.verifyImageWithAI(part) { isOk ->
                        if (!isOk) {
                            uiState = uiState.copy(invalidImages = uiState.invalidImages + uri)
                        }
                    }
                }
            }
        }
    }

    fun handlePublish() {
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
        if (uiState.price.isBlank()) {
            Toast.makeText(context, "Vui lòng nhập giá bán!", Toast.LENGTH_SHORT).show()
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
        val safePrice = uiState.price.toDoubleOrNull() ?: 0.0
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
                imageParts = imageParts
            )
        } else {
            Toast.makeText(context, "Lỗi xử lý ảnh, vui lòng thử lại!", Toast.LENGTH_SHORT).show()
        }
    }

    SellProductContent(
        uiState = uiState,
        onNameChange = { uiState = uiState.copy(productName = it) },
        onPriceChange = { uiState = uiState.copy(price = it) },
        onUnitChange = { uiState = uiState.copy(unit = it) },
        onQuantityChange = { uiState = uiState.copy(quantity = it) },
        onDescriptionChange = { uiState = uiState.copy(description = it) },
        onCategoryChange = { selectedCat -> uiState = uiState.copy(category = selectedCat) },
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
            if (uiState.productName.isNotBlank()) {
                uiState = uiState.copy(isPredictingPrice = true)

                viewModel.getPricePrediction(uiState.productName) { predictedPrice, message ->
                    uiState = uiState.copy(isPredictingPrice = false)

                    if (predictedPrice != null) {
                        uiState = uiState.copy(price = predictedPrice)
                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                    }
                }

            } else {
                Toast.makeText(context, "Vui lòng nhập tên sản phẩm trước!", Toast.LENGTH_SHORT).show()
            }
        }
    )
}

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
) {
    val backgroundColor = Color(0xFFFAFBFA)
    val primaryGreen = Color(0xFF1E7032)
    val lightGreen = Color(0xFFE8F3EA)
    val textDark = Color(0xFF1D1D1D)
    val textGray = Color(0xFF757575)
    val borderGray = Color(0xFFE0E0E0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = primaryGreen,
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable { onBackClick() }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = stringResource(R.string.sell_product_title),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = primaryGreen
            )
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.product_images),
                    fontWeight = FontWeight.SemiBold,
                    color = textDark
                )
                Text(
                    text = stringResource(R.string.max_5_images),
                    style = MaterialTheme.typography.bodySmall,
                    color = textGray
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (uiState.images.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .drawBehind {
                            val stroke = Stroke(
                                width = 4.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(25f, 20f), 0f)
                            )
                            drawRoundRect(
                                color = borderGray,
                                style = stroke,
                                cornerRadius = CornerRadius(12.dp.toPx())
                            )
                        }
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onImageUploadClick() }
                                .padding(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Photo,
                                contentDescription = null,
                                tint = textDark,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.library),
                                color = textDark,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(60.dp)
                                .background(borderGray.copy(alpha = 0.5f))
                        )
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onCameraCaptureClick() }
                                .padding(12.dp)
                        ) {
                            Icon(
                                Icons.Default.PhotoCamera,
                                contentDescription = null,
                                tint = primaryGreen,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.take_picture),
                                color = primaryGreen,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            } else {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(uiState.images) { uri ->
                        val isInvalid = uiState.invalidImages.contains(uri)

                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, borderGray, RoundedCornerShape(8.dp))
                        ) {
                            AsyncImage(
                                model = uri,
                                contentDescription = "Selected Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )

                            if (isInvalid) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Red.copy(alpha = 0.4f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Lỗi",
                                        tint = Color.White,
                                        modifier = Modifier.size(32.dp)
                                    )
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.8f))
                                    .clickable { onRemoveImage(uri) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Delete",
                                    tint = Color.Red,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    if (uiState.images.size < 5) {
                        item {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, borderGray, RoundedCornerShape(8.dp))
                                    .clickable { onImageUploadClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Photo,
                                        contentDescription = "Thư viện",
                                        tint = textGray,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Thư viện",
                                        fontSize = 12.sp,
                                        color = textGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                        item {
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, borderGray, RoundedCornerShape(8.dp))
                                    .clickable { onCameraCaptureClick() },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.PhotoCamera,
                                        contentDescription = "Chụp ảnh",
                                        tint = textGray,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Chụp ảnh",
                                        fontSize = 12.sp,
                                        color = textGray,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }
                if (uiState.invalidImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "⚠️ AI phát hiện ảnh không hợp lệ. Vui lòng xóa ảnh bị đánh dấu đỏ!",
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            FormInputField(
                label = stringResource(R.string.product_name),
                value = uiState.productName,
                onValueChange = onNameChange,
                placeholder = stringResource(R.string.product_name_hint)
            )

            Text(
                text = stringResource(R.string.category),
                style = MaterialTheme.typography.labelMedium,
                color = textDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            CategoryDropdownMenu(
                selectedCategory = uiState.category,
                onCategorySelected = onCategoryChange
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    FormInputField(
                        label = stringResource(R.string.price),
                        value = uiState.price,
                        onValueChange = onPriceChange,
                        placeholder = stringResource(R.string.price_hint),
                        keyboardType = KeyboardType.Number,
                        trailingText = stringResource(R.string.currency)
                    )
                }

                Button(
                    onClick = {
                        onPredictPriceClick()
                    },
                    modifier = Modifier
                        .padding(bottom = 20.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F3EA)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isPredictingPrice) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = primaryGreen, strokeWidth = 2.dp)
                    } else {
                        Text("✨ Gợi ý AI", color = primaryGreen, fontWeight = FontWeight.Bold)
                    }
                }
            }

            FormInputField(
                label = stringResource(R.string.unit),
                value = uiState.unit,
                onValueChange = onUnitChange,
                placeholder = stringResource(R.string.unit_hint)
            )

            Text(
                text = stringResource(R.string.quantity),
                style = MaterialTheme.typography.labelMedium,
                color = textDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextField(
                    value = if (uiState.quantity == 0) "" else uiState.quantity.toString(),
                    onValueChange = { onQuantityChange(it.toIntOrNull() ?: 0) },
                    placeholder = { Text("0", color = textGray) },
                    modifier = Modifier.weight(1f),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFFF5F5F5),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { if (uiState.quantity > 0) onQuantityChange(uiState.quantity - 1) }) {
                        Icon(imageVector = Icons.Default.Remove, contentDescription = "Decrease")
                    }
                    IconButton(onClick = { onQuantityChange(uiState.quantity + 1) }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Increase")
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.description),
                style = MaterialTheme.typography.labelMedium,
                color = textDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextField(
                value = uiState.description,
                onValueChange = onDescriptionChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .border(1.dp, borderGray, RoundedCornerShape(8.dp)),
                placeholder = { Text(stringResource(R.string.description_hint), color = textGray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(lightGreen)
                    .padding(16.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Verified, contentDescription = null, tint = primaryGreen)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.cert_title),
                            fontWeight = FontWeight.Bold,
                            color = primaryGreen
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.cert_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = primaryGreen,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.White)
                            .clickable { onAddCertClick() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.add_cert),
                            color = primaryGreen,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
        val isFormValid = uiState.images.isNotEmpty() &&
                uiState.invalidImages.isEmpty() &&
                uiState.productName.isNotBlank() &&
                uiState.category.isNotBlank() &&
                uiState.price.isNotBlank() &&
                uiState.unit.isNotBlank() &&
                uiState.quantity > 0 &&
                uiState.description.isNotBlank()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = onPublishClick,
                enabled = isFormValid && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryGreen,
                    disabledContainerColor = Color.LightGray
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = stringResource(R.string.loading),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = stringResource(R.string.btn_sell),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FormInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType = KeyboardType.Text,
    trailingText: String? = null
) {
    Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color(0xFF1D1D1D))
    Spacer(modifier = Modifier.height(8.dp))
    TextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = Color(0xFF757575)) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color(0xFFF5F5F5),
            unfocusedContainerColor = Color(0xFFF5F5F5),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        trailingIcon = if (trailingText != null) {
            {
                Text(
                    text = trailingText,
                    color = Color(0xFF1D1D1D),
                    modifier = Modifier.padding(end = 16.dp),
                    fontWeight = FontWeight.Medium
                )
            }
        } else null
    )
    Spacer(modifier = Modifier.height(20.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDropdownMenu(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    val categories = listOf(
        stringResource(R.string.cat_vegetables),
        stringResource(R.string.cat_fruits),
        stringResource(R.string.cat_meat),
        stringResource(R.string.cat_seafood),
        stringResource(R.string.cat_others)
    )
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        TextField(
            value = selectedCategory.ifEmpty { stringResource(R.string.category_dropdown_hint) },
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(type = MenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF5F5F5),
                unfocusedContainerColor = Color(0xFFF5F5F5),
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            shape = RoundedCornerShape(8.dp)
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            categories.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption, color = Color(0xFF1D1D1D)) },
                    onClick = {
                        onCategorySelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SellProductScreenPreview() {
    AgritechTheme {
        SellProductContent(
            uiState = SellProductUiState(
                images = emptyList(),
                invalidImages = emptyList(),
                productName = "",
                category = "",
                price = "",
                unit = "",
                quantity = 0,
                description = "",
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
            onPredictPriceClick = {}
        )
    }
}