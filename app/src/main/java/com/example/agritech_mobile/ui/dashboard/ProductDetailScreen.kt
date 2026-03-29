package com.example.agritech_mobile.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.ui.theme.AgritechTheme

data class ProductDetailUiState(
    val id: String = "1",
    val name: String = "Đang tải...",
    val price: String = "0",
    val unit: String = "kg",
    val category: String = "DANH MỤC",
    val rating: String = "5.0",
    val reviewCount: String = "0",
    val stock: String = "0",
    val sellerName: String = "Đang tải...",
    val description: String = "Đang tải...",
    val features: List<String> = listOf(
        "Sản phẩm sạch",
        "Nguồn gốc rõ ràng"
    ),
    val shippingFee: String = "15.000",
    val isFavorite: Boolean = false,
    val isLoading: Boolean = false,
    val imageUrls: List<String> = emptyList() // Chứa danh sách link ảnh
)

@Composable
fun ProductDetailScreen(
    onBackClick: () -> Unit = {},
    productId: String = "",
    viewModel: DashboardViewModel = hiltViewModel()
) {
    var uiState by remember { mutableStateOf(ProductDetailUiState()) }
    val dashboardState by viewModel.dashboardState.collectAsState()

    LaunchedEffect(productId) {
        viewModel.getProductById(productId)
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
                    description = productRes.description,
                    sellerName = productRes.farmerName,
                    imageUrls = productRes.imageUrls // Nhận mảng ảnh từ API
                )
            }

            else -> {}
        }
    }

    ProductDetailContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onFavoriteClick = { uiState = uiState.copy(isFavorite = !uiState.isFavorite) },
        onViewShopClick = { /* TODO: Mở trang Shop */ },
        onChatClick = { /* TODO: Mở màn hình Chat */ },
        onAddToCartClick = { /* TODO: Thêm vào giỏ hàng (Gọi API) */ }
    )
}

@Composable
fun ProductDetailContent(
    uiState: ProductDetailUiState,
    onBackClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onViewShopClick: () -> Unit,
    onChatClick: () -> Unit,
    onAddToCartClick: () -> Unit
) {
    val primaryGreen = Color(0xFF1B5E20)
    val lightGreen = Color(0xFFE8F5E9)
    val textDark = Color(0xFF212121)
    val textGray = Color(0xFF757575)
    val bgGray = Color(0xFFF5F5F5)

    Scaffold(
        bottomBar = {
            ProductDetailBottomBar(
                onChatClick = onChatClick,
                onAddToCartClick = onAddToCartClick
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
                                    val color = if (pagerState.currentPage == iteration) primaryGreen else Color.LightGray
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
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            tint = textDark,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(
                                R.string.product_rating_format,
                                uiState.rating,
                                uiState.reviewCount
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = textDark
                        )
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
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE0E0E0))
                                .clickable { onViewShopClick() }
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.view_shop_btn),
                                style = MaterialTheme.typography.labelMedium,
                                color = textDark,
                                fontWeight = FontWeight.Bold
                            )
                        }
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

                    Column {
                        val chunks = uiState.features.chunked(2)
                        chunks.forEach { rowItems ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                            ) {
                                rowItems.forEach { feature ->
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = primaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = feature,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = textDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                if (rowItems.size == 1) Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Column {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFC8E6C9))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.shipping_fee_tag,
                                    uiState.shippingFee
                                ),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = primaryGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(lightGreen)
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.delivery_time_tag),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = primaryGreen
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))
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

@Composable
fun ProductDetailBottomBar(
    onChatClick: () -> Unit,
    onAddToCartClick: () -> Unit
) {
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
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF5F5F5))
                    .clickable { onChatClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.ChatBubbleOutline,
                    contentDescription = "Chat",
                    tint = Color(0xFF1B5E20)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Button(
                onClick = onAddToCartClick,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.add_to_cart_btn),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProductDetailScreenPreview() {
    AgritechTheme {
        ProductDetailScreen()
    }
}