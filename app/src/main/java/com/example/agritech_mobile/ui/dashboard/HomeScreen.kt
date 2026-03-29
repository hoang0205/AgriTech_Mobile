package com.example.agritech_mobile.ui.dashboard

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.ui.theme.AgritechTheme

data class Product(
    val id: String,
    val name: String,
    val priceStr: String,
    val seller: String,
    val isFavorite: Boolean = false,
    val isNew: Boolean = false,
    val imageUrl: String = ""
)

data class CategoryItem(val name: String, val titleRes: Int, val icon: ImageVector)
data class HomeUiState(
    val userName: String = "",
    val searchQuery: String = "",
    val categories: List<CategoryItem> = emptyList(),
    val newProducts: List<Product> = emptyList(),
    val suggestedProducts: List<Product> = emptyList(),
    val isLoading: Boolean = false
)

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreateProduct: () -> Unit
) {
    val context = LocalContext.current
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    var uiState by remember { mutableStateOf(HomeUiState()) }

    val defaultCategories = listOf(
        CategoryItem("RAU_CU", R.string.cat_vegetables, Icons.Default.Eco),
        CategoryItem("TRAI_CAY", R.string.cat_fruits, Icons.Default.RiceBowl),
        CategoryItem("THIT", R.string.cat_meat, Icons.Default.SetMeal)
    )

    LaunchedEffect(Unit) {
        uiState = uiState.copy(categories = defaultCategories, userName = "Hoàng")
        viewModel.loadHomeData()
    }

    LaunchedEffect(dashboardState) {
        when (dashboardState) {
            is DashboardState.Loading -> {
                uiState = uiState.copy(isLoading = true)
            }

            is DashboardState.HomeDataSuccess -> {
                val state = dashboardState as DashboardState.HomeDataSuccess

                val mappedNew = state.newProducts.map { res ->
                    Product(
                        id = res.id,
                        name = res.name,
                        priceStr = "${res.price.toLong()} đ",
                        seller = res.farmerName,
                        isNew = true,
                        imageUrl = res.imageUrls.firstOrNull() ?: ""
                    )
                }

                val mappedSuggestedProducts = state.suggestedProducts.map { res ->
                    Product(
                        id = res.id,
                        name = res.name,
                        priceStr = "${res.price.toLong()} đ",
                        seller = res.farmerName,
                        isNew = false,
                        imageUrl = res.imageUrls.firstOrNull() ?: ""
                    )
                }

                uiState = uiState.copy(
                    isLoading = false,
                    newProducts = mappedNew,
                    suggestedProducts = mappedSuggestedProducts
                )
            }

            is DashboardState.ProductListSuccess -> {
                val state = dashboardState as DashboardState.ProductListSuccess
                val mappedProducts = state.products.map { res ->
                    Product(
                        id = res.id,
                        name = res.name,
                        priceStr = "${res.price.toLong()} đ",
                        seller = res.farmerName,
                        imageUrl = res.imageUrls.firstOrNull() ?: "",
                        isNew = uiState.searchQuery.isBlank()
                    )
                }
                if (uiState.searchQuery.isNotBlank()) {
                    uiState = uiState.copy(
                        isLoading = false,
                        suggestedProducts = mappedProducts,
                        newProducts = emptyList()
                    )
                } else {
                    uiState = uiState.copy(
                        isLoading = false,
                        newProducts = mappedProducts
                    )
                }
            }

            is DashboardState.Error -> {
                uiState = uiState.copy(isLoading = false)
                Toast.makeText(
                    context,
                    (dashboardState as DashboardState.Error).error,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetState()
            }

            else -> {}
        }
    }

    HomeContent(
        uiState = uiState,
        onSearchChange = { newQuery ->
            uiState = uiState.copy(searchQuery = newQuery)
            if (newQuery.isNotBlank()) {
                viewModel.searchProducts(newQuery)
            } else {
                viewModel.loadHomeData()
            }
        },
        onCategoryClick = { category -> viewModel.getProductsByCategory(category.name) },
        onProductClick = { product ->
            onNavigateToDetail(product.id)
        },
        onSeeAllClick = { viewModel.getProducts() },
        onCreateProductClick = { onNavigateToCreateProduct() }
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    onSearchChange: (String) -> Unit,
    onCategoryClick: (CategoryItem) -> Unit,
    onProductClick: (Product) -> Unit,
    onSeeAllClick: () -> Unit,
    onCreateProductClick: () -> Unit
) {
    val backgroundColor = Color(0xFFF8F9FA)

    Scaffold(
        bottomBar = { AgritechBottomNavigation(onCreateProductClick) },
        containerColor = backgroundColor
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                HomeHeader(userName = uiState.userName)
            }

            item {
                HomeSearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = onSearchChange
                )
            }

            item {
                SectionTitle(title = stringResource(R.string.category_title))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(uiState.categories) { category ->
                        CategoryChip(category = category, onClick = { onCategoryClick(category) })
                    }
                }
            }

            item {
                SectionTitle(
                    title = stringResource(R.string.new_products_title),
                    actionText = stringResource(R.string.see_all),
                    onActionClick = onSeeAllClick
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 32.dp)
                ) {
                    items(uiState.newProducts) { product ->
                        NewProductCard(
                            product = product,
                            onClick = { onProductClick(product) }
                        )
                    }
                }
            }

            item {
                SectionTitle(title = stringResource(R.string.suggested_title))
            }

            items(uiState.suggestedProducts.chunked(2)) { rowProducts ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (product in rowProducts) {
                        SuggestedProductCard(
                            product = product,
                            modifier = Modifier.weight(1f),
                            onClick = { onProductClick(product) }
                        )
                    }
                    if (rowProducts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun HomeHeader(userName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0E0E0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.greeting_user, userName),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1B5E20),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { /* TODO: Notification */ }) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notification",
                tint = Color(0xFF1B5E20)
            )
        }
    }
}

@Composable
fun HomeSearchBar(query: String, onQueryChange: (String) -> Unit) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp),
        placeholder = { Text(stringResource(R.string.search_placeholder), color = Color.Gray) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
fun SectionTitle(title: String, actionText: String? = null, onActionClick: (() -> Unit)? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF212121)
        )
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1B5E20),
                modifier = Modifier.clickable { onActionClick() }
            )
        }
    }
}

@Composable
fun CategoryChip(category: CategoryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = category.icon,
            contentDescription = null,
            tint = Color(0xFF1B5E20),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(category.titleRes),
            style = MaterialTheme.typography.labelLarge,
            color = Color(0xFF212121)
        )
    }
}

@Composable
fun NewProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF80CBC4))
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_launcher_background),
                error = painterResource(R.drawable.ic_launcher_background)
            )

            Box(
                modifier = Modifier
                    .padding(8.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .align(Alignment.TopEnd)
                    .clickable { /* TODO: Toggle Favorite */ },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (product.isFavorite) Color(0xFF1B5E20) else Color.LightGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = product.priceStr,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1B5E20)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Storefront,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.sold_by, product.seller),
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SuggestedProductCard(product: Product, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = modifier.clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF212121))
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(R.drawable.ic_launcher_background),
                error = painterResource(R.drawable.ic_launcher_background)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = product.priceStr,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = Color(0xFF1B5E20)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = Color.Gray
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = product.seller,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AgritechBottomNavigation(
    onCreateProductClick: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = Color.White,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItem(
                    icon = Icons.Default.Home,
                    label = stringResource(R.string.nav_home),
                    isSelected = true,
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    icon = Icons.Outlined.ReceiptLong,
                    label = stringResource(R.string.nav_orders),
                    isSelected = false,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(64.dp))

                BottomNavItem(
                    icon = Icons.Outlined.ShoppingCart,
                    label = stringResource(R.string.nav_cart),
                    isSelected = false,
                    modifier = Modifier.weight(1f)
                )
                BottomNavItem(
                    icon = Icons.Outlined.AccountCircle,
                    label = stringResource(R.string.nav_account),
                    isSelected = false,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.offset(y = (-15).dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1B5E20))
                    .clickable { onCreateProductClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Create",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Composable
fun BottomNavItem(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isSelected) Color(0xFF1B5E20) else Color.Gray
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { /* TODO:*/ },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxSize()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = color,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "vi")
@Composable
fun HomeScreenPreview() {
    AgritechTheme {
        val dummyCategories = listOf(
            CategoryItem("RAU_CU", R.string.cat_vegetables, Icons.Default.Eco),
            CategoryItem("TRAI_CAY", R.string.cat_fruits, Icons.Default.RiceBowl),
            CategoryItem("THIT", R.string.cat_meat, Icons.Default.SetMeal)
        )

        val dummyNewProducts = listOf(
            Product(
                id = "1",
                name = "Dưa hấu Long An",
                priceStr = "15.000 đ",
                seller = "Đoàn Minh Hoàng",
                imageUrl = "",
                isFavorite = true,
                isNew = true
            ),
            Product(
                id = "2",
                name = "Rau muống thủy canh",
                priceStr = "10.000 đ",
                seller = "HTX Nông Nghiệp",
                imageUrl = "",
                isFavorite = false,
                isNew = true
            )
        )

        val dummySuggestedProducts = listOf(
            Product(
                id = "3",
                name = "Thịt heo sạch 3F",
                priceStr = "120.000 đ",
                seller = "Trại heo sạch",
                imageUrl = "",
                isFavorite = false,
                isNew = false
            ),
            Product(
                id = "4",
                name = "Cà chua sấy khô",
                priceStr = "45.000 đ",
                seller = "Đoàn Minh Hoàng",
                imageUrl = "",
                isFavorite = true,
                isNew = false
            )
        )

        val fakeUiState = HomeUiState(
            userName = "Hoàng",
            searchQuery = "",
            categories = dummyCategories,
            newProducts = dummyNewProducts,
            suggestedProducts = dummySuggestedProducts,
            isLoading = false
        )

        HomeContent(
            uiState = fakeUiState,
            onSearchChange = {},
            onCategoryClick = {},
            onProductClick = {},
            onSeeAllClick = {},
            onCreateProductClick = {}
        )
    }
}