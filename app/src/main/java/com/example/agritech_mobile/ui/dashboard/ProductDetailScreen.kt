package com.example.agritech_mobile.ui.dashboard

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardBackspace
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarHalf
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.data.remote.dto.ReviewModelsResponse
import com.example.agritech_mobile.data.remote.dto.ReviewSummaryResponse
import com.example.agritech_mobile.ui.cart.CartState
import com.example.agritech_mobile.ui.cart.CartViewModel
import com.example.agritech_mobile.ui.theme.AgritechTheme
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class ProductDetailUiState(
    val id: String = "1",
    val name: String = "Đang tải...",
    val price: String = "0",
    val unit: String = "kg",
    val category: String = "DANH MỤC",
    val rating: Double = 5.0,
    val reviewCount: Int = 0,
    val stock: String = "0",
    val sellerId: String = "",
    val sellerName: String = "Đang tải...",
    val sellerAvatar: String? = null,
    val sellerPhone: String? = null,
    val description: String = "Đang tải...",
    val features: List<String> = listOf("Sản phẩm sạch", "Nguồn gốc rõ ràng"),
    val shippingFee: String = "15.000",
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val imageUrls: List<String> = emptyList(),
    val quantity: Double = 1.0,
)

@Composable
fun ProductDetailScreen(
    onBackClick: () -> Unit = {},
    productId: String = "",
    onChatClick: (
        sellerId: String,
        sellerName: String,
        sellerAvatar: String?,
        sellerPhone: String?,
        productId: String,
        productName: String,
        productPrice: Double,
        productImage: String?
    ) -> Unit = { _, _, _, _, _, _, _, _ -> },
    viewModel: DashboardViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel()
) {
    var uiState by remember { mutableStateOf(ProductDetailUiState()) }
    val dashboardState by viewModel.dashboardState.collectAsState()
    val cartState by cartViewModel.cartState.collectAsState()
    var showAddToCartError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val reviewsList by viewModel.reviews.collectAsState()
    val reviewSummary by viewModel.reviewSummary.collectAsState()

    LaunchedEffect(productId) {
        viewModel.getProductById(productId)
        viewModel.getProductReviews(productId)
        viewModel.getReviewSummary(productId)
    }

    LaunchedEffect(dashboardState) {
        when (dashboardState) {
            is DashboardState.Loading -> {
                uiState = uiState.copy(isLoading = true)
            }

            is DashboardState.ProductDetailSuccess -> {
                val productRes = (dashboardState as DashboardState.ProductDetailSuccess).product

                uiState = uiState.copy(
                    isLoading = false,
                    id = productRes.id,
                    name = productRes.name,
                    price = "${productRes.price.toLong()}",
                    unit = productRes.unit,
                    category = productRes.category,
                    rating = productRes.rating,
                    reviewCount = productRes.reviewCount,
                    description = productRes.description,
                    sellerId = productRes.farmerId,
                    sellerName = productRes.farmerName,
                    sellerAvatar = productRes.farmerAvatar,
                    sellerPhone = productRes.farmerPhone,
                    stock = productRes.quantity.toString(),
                    imageUrls = productRes.imageUrls,
                )
            }

            else -> {}
        }
    }
    var hasHandledCartSuccess by remember { mutableStateOf(false) }
    val context = LocalContext.current

    LaunchedEffect(cartState) {
        when (val state = cartState) {
            is CartState.ActionSuccess -> {
                if (!hasHandledCartSuccess) {
                    hasHandledCartSuccess = true
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    onBackClick()
                }
            }

            is CartState.Error -> {
                errorMessage = state.error
                showAddToCartError = true
            }

            is CartState.Loading -> {
                hasHandledCartSuccess = false
            }

            else -> {}
        }
    }

    if (showAddToCartError) {
        AlertDialog(
            onDismissRequest = { showAddToCartError = false },
            title = { Text("Lỗi thêm vào giỏ hàng") },
            text = { Text(errorMessage) },
            confirmButton = {
                Button(onClick = { showAddToCartError = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    ProductDetailContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onFavoriteClick = { uiState = uiState.copy(isFavorite = !uiState.isFavorite) },
        onViewShopClick = { /* TODO*/ },
        onChatClick = {
            onChatClick(
                uiState.sellerId,
                uiState.sellerName,
                uiState.sellerAvatar,
                uiState.sellerPhone,
                uiState.id,
                uiState.name,
                uiState.price.toDoubleOrNull() ?: 0.0,
                uiState.imageUrls.firstOrNull()
            )
        },
        onAddToCartClick = {
            cartViewModel.addToCart(uiState.id, uiState.quantity)
        },
        onIncreaseQuantity = {
            uiState = uiState.copy(quantity = uiState.quantity + 1.0)
        },
        onDecreaseQuantity = {
            if (uiState.quantity > 1.0) {
                uiState = uiState.copy(quantity = uiState.quantity - 1.0)
            }
        },
        onQuantityChange = { input ->
            val filtered = input.filter { it.isDigit() || it == '.' }
            val newQty = filtered.toDoubleOrNull() ?: 0.0
            uiState = uiState.copy(quantity = newQty)
        },
        reviews = reviewsList,
        reviewSummary = reviewSummary
    )
}

@Composable
fun ProductDetailContent(
    uiState: ProductDetailUiState,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onViewShopClick: () -> Unit,
    onChatClick: () -> Unit,
    onAddToCartClick: () -> Unit,
    onIncreaseQuantity: () -> Unit,
    onDecreaseQuantity: () -> Unit,
    onQuantityChange: (String) -> Unit,
    reviews: List<ReviewModelsResponse>,
    reviewSummary: ReviewSummaryResponse?
) {
    val primaryGreen = Color(0xFF1B5E20)
    val lightGreen = Color(0xFFE8F5E9)
    val textDark = Color(0xFF212121)
    val textGray = Color(0xFF757575)
    val bgGray = Color(0xFFF5F5F5)

    Scaffold(
        bottomBar = {
            ProductDetailBottomBar(
                quantity = uiState.quantity,
                unit = uiState.unit,
                onChatClick = onChatClick,
                onAddToCartClick = onAddToCartClick,
                onIncreaseQuantity = onIncreaseQuantity,
                onDecreaseQuantity = onDecreaseQuantity,
                onQuantityChange = onQuantityChange
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .background(Color(0xFF263238))
                ) {
                    if (uiState.imageUrls.isNotEmpty()) {
                        val pagerState = rememberPagerState(
                            initialPage = 0,
                            pageCount = { uiState.imageUrls.size }
                        )

                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            AsyncImage(
                                model = uiState.imageUrls.getOrNull(page),
                                contentDescription = "Ảnh sản phẩm ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        if (uiState.imageUrls.size > 1) {
                            Row(
                                Modifier
                                    .height(20.dp)
                                    .align(Alignment.BottomEnd)
                                    .padding(bottom = 36.dp, end = 16.dp)
                                    .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                                    .padding(horizontal = 8.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                repeat(uiState.imageUrls.size) { iteration ->
                                    val color =
                                        if (pagerState.currentPage == iteration) primaryGreen else Color.LightGray
                                    Box(
                                        modifier = Modifier
                                            .padding(2.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .size(6.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Sản phẩm không có hình ảnh", color = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .offset(y = (-24).dp)
                        .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                        .background(Color.White)
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(lightGreen)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = uiState.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = primaryGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
//                        Icon(
//                            Icons.Default.Star,
//                            contentDescription = null,
//                            tint = textDark,
//                            modifier = Modifier.size(16.dp)
//                        )
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text(
//                            text = stringResource(
//                                R.string.product_rating_format,
//                                uiState.rating,
//                                uiState.reviewCount
//                            ),
//                            style = MaterialTheme.typography.bodyMedium,
//                            color = textDark
//                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = uiState.name,
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        color = textDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${uiState.price} đ",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = primaryGreen
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "/ ${uiState.unit}",
                            style = MaterialTheme.typography.titleMedium,
                            color = textDark,
                            modifier = Modifier.padding(bottom = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.stock_format, uiState.stock, uiState.unit),
                        style = MaterialTheme.typography.bodyMedium,
                        color = textDark
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(bgGray)
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.Gray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Person,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.sold_by_label),
                                style = MaterialTheme.typography.bodySmall,
                                color = textGray
                            )
                            Text(
                                text = uiState.sellerName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = textDark
                            )
                        }
//                        Box(
//                            modifier = Modifier
//                                .clip(RoundedCornerShape(8.dp))
//                                .background(Color(0xFFE0E0E0))
//                                .clickable { onViewShopClick() }
//                                .padding(horizontal = 16.dp, vertical = 8.dp)
//                        ) {
//                            Text(
//                                text = stringResource(R.string.view_shop_btn),
//                                style = MaterialTheme.typography.labelMedium,
//                                color = textDark,
//                                fontWeight = FontWeight.Bold
//                            )
//                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(20.dp)
                                .background(primaryGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.product_desc_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = textDark
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = uiState.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF424242),
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    HorizontalDivider(color = Color(0xFFEEEEEE), thickness = 1.dp)
                    Spacer(modifier = Modifier.height(24.dp))

                    ProductReviewsSection(
                        rating = uiState.rating,
                        reviewCount = uiState.reviewCount,
                        primaryGreen = primaryGreen,
                        textDark = textDark,
                        textGray = textGray,
                        reviews = reviews,
                        reviewSummary = reviewSummary
                    )

                    Spacer(modifier = Modifier.height(24.dp))

//                    Column {
//                        val chunks = uiState.features.chunked(2)
//                        chunks.forEach { rowItems ->
//                            Row(
//                                modifier = Modifier
//                                    .fillMaxWidth()
//                                    .padding(bottom = 12.dp)
//                            ) {
//                                rowItems.forEach { feature ->
//                                    Row(
//                                        modifier = Modifier.weight(1f),
//                                        verticalAlignment = Alignment.CenterVertically
//                                    ) {
//                                        Icon(
//                                            Icons.Default.CheckCircle,
//                                            contentDescription = null,
//                                            tint = primaryGreen,
//                                            modifier = Modifier.size(16.dp)
//                                        )
//                                        Spacer(modifier = Modifier.width(8.dp))
//                                        Text(
//                                            text = feature,
//                                            style = MaterialTheme.typography.bodySmall,
//                                            color = textDark,
//                                            maxLines = 1,
//                                            overflow = TextOverflow.Ellipsis
//                                        )
//                                    }
//                                }
//                                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
//                            }
//                        }
//                    }

                    Spacer(modifier = Modifier.height(24.dp))
//
//                    Column {
//                        Box(
//                            modifier = Modifier
//                                .clip(RoundedCornerShape(6.dp))
//                                .background(Color(0xFFC8E6C9))
//                                .padding(horizontal = 12.dp, vertical = 8.dp)
//                        ) {
//                            Text(
//                                text = stringResource(
//                                    R.string.shipping_fee_tag,
//                                    uiState.shippingFee
//                                ),
//                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
//                                color = primaryGreen
//                            )
//                        }
//                        Spacer(modifier = Modifier.height(8.dp))
//                        Box(
//                            modifier = Modifier
//                                .clip(RoundedCornerShape(6.dp))
//                                .background(lightGreen)
//                                .padding(horizontal = 12.dp, vertical = 8.dp)
//                        ) {
//                            Text(
//                                text = stringResource(R.string.delivery_time_tag),
//                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
//                                color = primaryGreen
//                            )
//                        }
//                    }
//
//                    Spacer(modifier = Modifier.height(32.dp))
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .statusBarsPadding()
                    .padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                        .clickable { onBackClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = primaryGreen
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.8f))
                        .clickable { onFavoriteClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = if (uiState.isFavorite) primaryGreen else Color.Gray
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun ProductReviewsSection(
    rating: Double,
    reviewCount: Int,
    reviews: List<ReviewModelsResponse>,
    reviewSummary: ReviewSummaryResponse?,
    primaryGreen: Color,
    textDark: Color,
    textGray: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = "Đánh giá từ cộng đồng",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = textDark
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Dựa trên $reviewCount lượt mua thực tế",
                    style = MaterialTheme.typography.bodySmall,
                    color = textGray
                )
            }
//            Row(
//                verticalAlignment = Alignment.CenterVertically,
//                modifier = Modifier.clickable { /* TODO: Go to all reviews */ }
//            ) {
//                Text(
//                    text = "Xem tất cả",
//                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
//                    color = primaryGreen
//                )
//                Icon(
//                    imageVector = Icons.AutoMirrored.Filled.KeyboardBackspace,
//                    contentDescription = null,
//                    tint = primaryGreen,
//                    modifier = Modifier.size(16.dp)
//                )
//            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        ReviewSummaryCard(rating, reviewSummary, primaryGreen, textDark)

        Spacer(modifier = Modifier.height(24.dp))

        if (reviews.isEmpty()) {
            Text(
                text = "Chưa có đánh giá nào cho sản phẩm này.",
                style = MaterialTheme.typography.bodyMedium,
                color = textGray,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        } else {
            reviews.forEach { review ->
                ReviewItem(
                    name = review.userName ?: "Khách hàng",
                    time = formatReviewTime(review.createdAt ?: ""),
                    rating = review.rating ?: 5,
                    comment = review.comment ?: "",
                    imageUrls = review.imageUrls,
                    avatarColor = primaryGreen,
                    primaryGreen = primaryGreen,
                    textDark = textDark,
                    textGray = textGray
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun ReviewSummaryCard(
    rating: Double, reviewSummary: ReviewSummaryResponse?, primaryGreen: Color, textDark: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF5F5F5))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = String.format("%.1f", rating),
                fontSize = 50.sp,
                fontWeight = FontWeight.ExtraBold,
                color = textDark
            )
            Row(modifier = Modifier.padding(vertical = 4.dp)) {
                for (i in 1..5) {
                    val icon = when {
                        rating >= i -> Icons.Default.Star
                        rating >= (i - 0.5) -> Icons.Default.StarHalf
                        else -> Icons.Outlined.StarBorder
                    }

                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (rating >= (i - 0.5)) primaryGreen else Color(0xFFBDBDBD),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .width(1.dp)
                .height(70.dp)
                .background(Color(0xFFE0E0E0))
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val total = reviewSummary?.totalReviews?.toFloat() ?: 1f
            val safeTotal = if (total == 0f) 1f else total

            val percent5 = (reviewSummary?.star5 ?: 0) / safeTotal
            val percent4 = (reviewSummary?.star4 ?: 0) / safeTotal
            val percent3 = (reviewSummary?.star3 ?: 0) / safeTotal
            val percent2 = (reviewSummary?.star2 ?: 0) / safeTotal
            val percent1 = (reviewSummary?.star1 ?: 0) / safeTotal

            RatingProgressBar(stars = 5, percentage = percent5, primaryGreen = primaryGreen)
            RatingProgressBar(stars = 4, percentage = percent4, primaryGreen = primaryGreen)
            RatingProgressBar(stars = 3, percentage = percent3, primaryGreen = primaryGreen)
            RatingProgressBar(stars = 2, percentage = percent2, primaryGreen = primaryGreen)
            RatingProgressBar(stars = 1, percentage = percent1, primaryGreen = primaryGreen)
        }
    }
}

@Composable
fun RatingProgressBar(stars: Int, percentage: Float, primaryGreen: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = "$stars",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF424242),
            modifier = Modifier.width(12.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .weight(1f)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFFE0E0E0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(primaryGreen)
            )
        }
    }
}

@Composable
fun ReviewItem(
    name: String,
    time: String,
    rating: Int,
    comment: String,
    imageUrls: List<String>?,
    avatarColor: Color,
    primaryGreen: Color,
    textDark: Color,
    textGray: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val initials =
                name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("")
                    .uppercase()
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    color = if (avatarColor == primaryGreen) Color.White else textDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textDark
                )
                Row(modifier = Modifier.padding(top = 2.dp)) {
                    repeat(5) { i ->
                        Icon(
                            imageVector = if (i < rating) Icons.Default.Star else Icons.Outlined.StarBorder,
                            contentDescription = null,
                            tint = if (i < rating) primaryGreen else Color(0xFFBDBDBD),
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
            Text(
                text = time,
                fontSize = 10.sp,
                color = textGray,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = comment,
            fontSize = 14.sp,
            color = Color(0xFF424242),
            lineHeight = 20.sp
        )

        if (!imageUrls.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                imageUrls.forEach { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = "Review Image",
                        modifier = Modifier
                            .size(80.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@RequiresApi(Build.VERSION_CODES.O)
fun formatReviewTime(rawTime: String): String {
    return try {
        val cleanTime = if (rawTime.contains(".")) rawTime.substringBefore(".") else rawTime
        val parsedTime = LocalDateTime.parse(cleanTime)
        val formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy")
        parsedTime.format(formatter)
    } catch (e: Exception) {
        rawTime
    }
}

@Composable
fun ProductDetailBottomBar(
    quantity: Double,
    unit: String,
    onChatClick: () -> Unit,
    onAddToCartClick: () -> Unit,
    onIncreaseQuantity: () -> Unit,
    onDecreaseQuantity: () -> Unit,
    onQuantityChange: (String) -> Unit
) {
    val displayQuantity = if (quantity % 1.0 == 0.0) {
        quantity.toLong().toString()
    } else {
        quantity.toString()
    }

    Surface(
        shadowElevation = 16.dp,
        color = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable { onChatClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ChatBubbleOutline,
                    contentDescription = "Chat",
                    tint = Color(0xFF1B5E20),
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .height(48.dp)
                    .padding(horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDecreaseQuantity,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Remove,
                        contentDescription = "Decrease",
                        tint = Color(0xFF212121),
                        modifier = Modifier.size(16.dp)
                    )
                }

                BasicTextField(
                    value = displayQuantity,
                    onValueChange = onQuantityChange,
                    modifier = Modifier.widthIn(min = 28.dp, max = 45.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color(0xFF212121),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(end = 2.dp)
                )

                IconButton(
                    onClick = onIncreaseQuantity,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Increase",
                        tint = Color(0xFF212121),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onAddToCartClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Thêm vào giỏ",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=411dp,height=1600dp,dpi=420"
)
@Composable
fun ProductDetailScreenPreview() {
    AgritechTheme {
        ProductDetailContent(
            uiState = ProductDetailUiState(
                name = "Heirloom Rainbow Carrots",
                price = "45000",
                unit = "kg",
                category = "RAU CỦ",
                sellerName = "Meadowbrook Farms",
                description = "Cà rốt cầu vồng hữu cơ, trồng theo chuẩn organic không sử dụng thuốc trừ sâu hóa học.",
                imageUrls = listOf("https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?q=80&w=600&auto=format&fit=crop"),
                quantity = 1.5,
                rating = 4.5,
                reviewCount = 1
            ),
            onBackClick = {},
            onFavoriteClick = {},
            onViewShopClick = {},
            onChatClick = {},
            onAddToCartClick = {},
            onIncreaseQuantity = {},
            onDecreaseQuantity = {},
            onQuantityChange = {},
            reviews = listOf(
                ReviewModelsResponse(
                    id = "mock_id_1",
                    productId = "mock_product_id_1",
                    userName = "Nguyễn Khánh Ly",
                    userId = "Nguyễn Khánh Ly",
                    rating = 5,
                    comment = "Sản phẩm tươi ngon, đóng gói rất cẩn thận. Giao hàng siêu nhanh luôn, 10 điểm không có nhưng! Lần sau sẽ tiếp tục ủng hộ shop.",
                    imageUrls = listOf(
                        "https://images.unsplash.com/photo-1592924357228-91a4daadcfea?q=80&w=200",
                        "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?q=80&w=200"
                    ),
                    createdAt = "2 NGÀY TRƯỚC"
                )
            ),
            reviewSummary = ReviewSummaryResponse(1, 5.0, 1, 0, 0, 0, 0)
        )
    }
}