package com.example.agritech_mobile.ui.user

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.StarBorder
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.agritech_mobile.di.uriToMultipartBodyPart
import okhttp3.MultipartBody
import com.example.agritech_mobile.ui.user.ReviewState
import com.example.agritech_mobile.ui.user.ReviewViewModel

@Composable
fun ReviewProductScreen(
    productID: String,
    productName: String = "",
    productImageUrl: String = "",
    shopName: String = "",
    onBackClick: () -> Unit = {},
    onReviewSuccess: () -> Unit = {},
    viewModel: ReviewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val reviewState by viewModel.reviewState.collectAsState()

    var rating by remember { mutableIntStateOf(0) }
    var comment by remember { mutableStateOf("") }
    var isAnonymous by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) selectedImageUri = uri }
    )

    LaunchedEffect(reviewState) {
        when (val state = reviewState) {
            is ReviewState.Success -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.clearReviewState()
                onReviewSuccess()
            }
            is ReviewState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                viewModel.clearReviewState()
            }
            else -> {}
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ReviewProductContent(
            productName = productName,
            productImageUrl = productImageUrl,
            shopName = shopName,
            rating = rating,
            comment = comment,
            isAnonymous = isAnonymous,
            selectedImageUri = selectedImageUri,
            onRatingChange = { rating = it },
            onCommentChange = { if (it.length <= 500) comment = it },
            onAnonymousChange = { isAnonymous = it },
            onAddImageClick = {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onRemoveImage = { selectedImageUri = null },
            onBackClick = onBackClick,
            onSubmitClick = {
                val imagePart = selectedImageUri?.let { uri ->
                    uriToMultipartBodyPart(context, uri, "files")
                }
                viewModel.submitReview(productID, rating, comment, imagePart)
            }
        )

        if (reviewState is ReviewState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1B5E20))
            }
        }
    }
}

@Composable
fun ReviewProductContent(
    productName: String,
    productImageUrl: String,
    shopName: String,
    rating: Int,
    comment: String,
    isAnonymous: Boolean,
    selectedImageUri: Uri?,
    onRatingChange: (Int) -> Unit,
    onCommentChange: (String) -> Unit,
    onAnonymousChange: (Boolean) -> Unit,
    onAddImageClick: () -> Unit,
    onRemoveImage: () -> Unit,
    onBackClick: () -> Unit,
    onSubmitClick: () -> Unit
) {
    val primaryGreen = Color(0xFF1B5E20)
    val bgColor = Color(0xFFF8FAF9)

    Scaffold(
        containerColor = bgColor,
        topBar = {
            ReviewTopBar(onBackClick = onBackClick)
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(bgColor)
                    .padding(16.dp)
            ) {
                Button(
                    onClick = onSubmitClick,
                    enabled = rating > 0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "GỬI ĐÁNH GIÁ",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ProductInfoCard(productName, productImageUrl, shopName)
            Spacer(modifier = Modifier.height(20.dp))

            RatingCard(
                rating = rating,
                onRatingChange = onRatingChange
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Nhận xét chi tiết",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF212121)
            )
            Spacer(modifier = Modifier.height(12.dp))
            ReviewTextField(
                value = comment,
                onValueChange = onCommentChange
            )
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Thêm hình ảnh/video",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Color(0xFF212121)
            )
            Spacer(modifier = Modifier.height(12.dp))
            ImagePickerBox(
                selectedImageUri = selectedImageUri,
                onClick = onAddImageClick,
                onRemove = onRemoveImage
            )
        }
    }
}

@Composable
fun ReviewTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .statusBarsPadding(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBackClick) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color(0xFF1B5E20)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Đánh giá sản phẩm",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1B5E20)
        )
    }
}

@Composable
fun ProductInfoCard(productName: String, productImageUrl: String, shopName: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = productImageUrl.ifEmpty { "https://images.unsplash.com/photo-1576045057995-568f588f82fb?q=80&w=200" }, // Xử lý nếu chưa có ảnh
                contentDescription = "Product Image",
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = productName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF212121)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = shopName.ifEmpty { "Cửa hàng mặc định" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF757575)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1B5E20))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "ĐÃ NHẬN HÀNG",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun RatingCard(rating: Int, onRatingChange: (Int) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Bạn cảm thấy sản phẩm này thế nào?",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF212121),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                for (i in 1..5) {
                    Icon(
                        imageVector = if (i <= rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "Star $i",
                        tint = if (i <= rating) Color(0xFFFFC107) else Color(0xFF9E9E9E),
                        modifier = Modifier
                            .size(40.dp)
                            .clickable { onRatingChange(i) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            val ratingText = when (rating) {
                1 -> "Rất tệ"
                2 -> "Tệ"
                3 -> "Bình thường"
                4 -> "Tốt"
                5 -> "Tuyệt vời"
                else -> "Vui lòng chọn đánh giá"
            }
            Text(
                text = ratingText,
                style = MaterialTheme.typography.bodyMedium,
                color = Color(0xFF757575)
            )
        }
    }
}

@Composable
fun ReviewTextField(value: String, onValueChange: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF5F5F5))
            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
    ) {
        TextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            placeholder = {
                Text(
                    text = "Chia sẻ thêm về chất lượng sản phẩm,\nđóng gói, vận chuyển...",
                    color = Color(0xFF9E9E9E)
                )
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Text(
            text = "${value.length}/500",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF757575),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        )
    }
}

@Composable
fun ImagePickerBox(
    selectedImageUri: Uri?,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    if (selectedImageUri != null) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(RoundedCornerShape(8.dp))
        ) {
            AsyncImage(
                model = selectedImageUri,
                contentDescription = "Selected Image",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(4.dp)
                    .size(24.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Clear,
                    contentDescription = "Remove Image",
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    } else {
        val borderColor = Color(0xFFBDBDBD)
        Box(
            modifier = Modifier
                .size(90.dp)
                .drawBehind {
                    val stroke = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )
                    drawRoundRect(
                        color = borderColor,
                        style = stroke,
                        cornerRadius = CornerRadius(8.dp.toPx())
                    )
                }
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFEEEEEE))
                .clickable { onClick() },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Outlined.AddPhotoAlternate,
                    contentDescription = "Add Photo",
                    tint = Color(0xFF616161),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Thêm ảnh",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF616161)
                )
            }
        }
    }
}

@Preview(showBackground = true, locale = "vi", device = "spec:width=411dp,height=1000dp")
@Composable
fun ReviewProductScreenPreview() {
    ReviewProductContent(
        productName = "Sầu riêng Ri6 Mọng Nước",
        productImageUrl = "https://images.unsplash.com/photo-1633324673678-ebf13b65f7c0?q=80&w=200",
        shopName = "Vựa trái cây Miền Tây",
        rating = 4,
        comment = "Giao hàng nhanh, sầu riêng cơm vàng hạt lép ăn cực kỳ ngon. Tuy nhiên vỏ hơi dày một chút.",
        isAnonymous = false,
        selectedImageUri = null,
        onRatingChange = {},
        onCommentChange = {},
        onAnonymousChange = {},
        onAddImageClick = {},
        onRemoveImage = {},
        onBackClick = {},
        onSubmitClick = {}
    )
}