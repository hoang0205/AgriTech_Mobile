package com.example.agritech_mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.ui.theme.AgritechTheme

data class SellerProductItem(
    val id: String,
    val name: String,
    val priceStr: String,
    val unit: String = "kg",
    val imageUrl: String = "",
    val seller: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val availableQuantity: Double = 0.0
)

data class SellerStoreUiState(
    val sellerId: String = "",
    val sellerName: String = "",
    val sellerAvatar: String? = null,
    val sellerPhone: String? = null,
    val products: List<SellerProductItem> = emptyList(),
    val isLoading: Boolean = false,
    val totalProducts: Int? = null,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null
)

@Composable
fun SellerStoreScreen(
    sellerId: String,
    sellerName: String,
    sellerAvatar: String? = null,
    sellerPhone: String? = null,
    onBackClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onChatClick: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val rawProducts by viewModel.sellerProducts.collectAsStateWithLifecycle()
    val isLoading by viewModel.isSellerLoading.collectAsStateWithLifecycle()

    LaunchedEffect(sellerId) {
        viewModel.getProductsBySeller(sellerId)
    }

    val mappedProducts = remember(rawProducts) {
        rawProducts.map { res ->
            SellerProductItem(
                id = res.id,
                name = res.name,
                priceStr = formatProductPrice(res.price),
                unit = res.unit,
                imageUrl = res.imageUrls.firstOrNull().orEmpty(),
                seller = res.farmerName,
                rating = res.rating,
                reviewCount = res.reviewCount,
                availableQuantity = res.quantity
            )
        }
    }

    val uiState = SellerStoreUiState(
        sellerId = sellerId,
        sellerName = sellerName,
        sellerAvatar = sellerAvatar,
        sellerPhone = sellerPhone,
        products = mappedProducts,
        isLoading = isLoading
    )

    SellerStoreContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onProductClick = onProductClick,
        onChatClick = onChatClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerStoreContent(
    uiState: SellerStoreUiState,
    onBackClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onChatClick: () -> Unit,
    isOwnStore: Boolean = false,
    onLoadMore: () -> Unit = {},
    onRetry: () -> Unit = {},
    onEditProduct: ((SellerProductItem) -> Unit)? = null
) {
    val primaryGreen = Color(0xFF1E7032)
    val lightGreen = Color(0xFFE8F3EA)
    val textDark = Color(0xFF1D1D1D)
    val textGray = Color(0xFF757575)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (isOwnStore) "Sản phẩm của tôi" else "Gian hàng",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = primaryGreen
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = textDark
                )
            )
        },
        containerColor = Color(0xFFFAFBFA)
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(CircleShape)
                                        .background(lightGreen)
                                        .border(2.dp, primaryGreen, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!uiState.sellerAvatar.isNullOrBlank()) {
                                        AsyncImage(
                                            model = uiState.sellerAvatar,
                                            contentDescription = "Avatar",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(
                                            Icons.Default.Storefront,
                                            contentDescription = null,
                                            tint = primaryGreen,
                                            modifier = Modifier.size(32.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(16.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = uiState.sellerName.ifBlank { "Nhà vườn AgriTech" },
                                            style = MaterialTheme.typography.titleMedium.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            color = textDark,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Default.Verified,
                                            contentDescription = "Verified",
                                            tint = primaryGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${uiState.totalProducts ?: uiState.products.size} sản phẩm đang bán",                                        fontSize = 12.sp,
                                        color = primaryGreen,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            if (!isOwnStore) {
                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = onChatClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(42.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryGreen)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Chat",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    )
                                }
                            }
                        }
                    }
                }

                item {
                    Text(
                        text = "Tất cả sản phẩm (${uiState.totalProducts ?: uiState.products.size})",                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = textDark,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }

                uiState.error?.let { error ->
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = error,
                                color = MaterialTheme.colorScheme.error
                            )

                            TextButton(
                                onClick = onRetry,
                                enabled = !uiState.isLoadingMore
                            ) {
                                Text("Thử lại")
                            }
                        }
                    }
                }

                if (
                    uiState.products.isEmpty() &&
                    uiState.error == null
                ) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isOwnStore) {
                                    "Bạn chưa có sản phẩm đang bán."
                                } else {
                                    "Gian hàng chưa có sản phẩm."
                                },                                color = textGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    items(uiState.products.chunked(2)) { rowProducts ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (product in rowProducts) {
                                Column(modifier = Modifier.weight(1f)) {
                                    SellerProductCard(
                                        product = product,
                                        onClick = { onProductClick(product.id) }
                                    )
                                    if (isOwnStore && onEditProduct != null) {
                                        OutlinedButton(
                                            onClick = { onEditProduct(product) },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            shape = RoundedCornerShape(12.dp)
                                        ) {
                                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Chỉnh sửa")
                                        }
                                    }
                                }
                            }
                            if (rowProducts.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                if (uiState.hasMore || uiState.isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (uiState.isLoadingMore) {
                                CircularProgressIndicator(
                                    color = primaryGreen
                                )
                            } else {
                                OutlinedButton(onClick = onLoadMore) {
                                    Text("Xem thêm sản phẩm")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SellerProductCard(
    product: SellerProductItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ProductCard(
        product = Product(
            id = product.id,
            name = product.name,
            priceStr = product.priceStr,
            seller = product.seller,
            imageUrl = product.imageUrl,
            unit = product.unit,
            rating = product.rating,
            reviewCount = product.reviewCount,
            availableQuantity = product.availableQuantity
        ),
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SellerStoreContentPreview() {
    AgritechTheme {
        SellerStoreContent(
            uiState = SellerStoreUiState(
                sellerId = "farmer_123",
                sellerName = "Đoàn Minh Hoàng",
                sellerAvatar = "",
                sellerPhone = "0835817188",
                isLoading = false,
                products = listOf(
                    SellerProductItem(
                        id = "p1",
                        name = "Dâu tây New Zealand",
                        priceStr = "180.000 đ",
                        unit = "hộp 500g"
                    ),
                    SellerProductItem(
                        id = "p2",
                        name = "Cà chua bi cherry",
                        priceStr = "45.000 đ",
                        unit = "kg"
                    ),
                    SellerProductItem(
                        id = "p3",
                        name = "Bắp cải tím hữu cơ",
                        priceStr = "35.000 đ",
                        unit = "kg"
                    ),
                    SellerProductItem(
                        id = "p4",
                        name = "Xà lách Lolo xanh thủy canh",
                        priceStr = "28.000 đ",
                        unit = "bó"
                    )
                )
            ),
            onBackClick = {},
            onProductClick = {},
            onChatClick = {}
        )
    }
}
