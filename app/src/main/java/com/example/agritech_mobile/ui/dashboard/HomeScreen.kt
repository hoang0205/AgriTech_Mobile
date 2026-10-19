package com.example.agritech_mobile.ui.dashboard

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    val imageUrl: String = "",
    val unit: String = "",
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val availableQuantity: Double = 0.0
)

data class CategoryItem(
    val name: String,
    val titleRes: Int,
    val icon: Int? = null,
    val materialIcon: ImageVector? = null
)

data class HomeUiState(
    val userName: String = "",
    val userAvatar: String = "",
    val categories: List<CategoryItem> = emptyList(),
    val newProducts: List<Product> = emptyList(),
    val suggestedProducts: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val selectedCategory: String? = null
)

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit,
    onNavigateToChat: () -> Unit
) {
    val context = LocalContext.current
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val uiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userAvatar by viewModel.userAvatar.collectAsStateWithLifecycle()



    val defaultCategories = listOf(
        CategoryItem(
            "Rau củ",
            R.string.cat_vegetables,
            R.drawable.vegetable_food_salad_lettuce_cabbage_svgrepo_com
        ),
        CategoryItem(
            "Trái cây",
            R.string.cat_fruits,
            R.drawable.fruit_fruits_grape_svgrepo_com
        ),
        CategoryItem(
            "Thịt",
            R.string.cat_meat,
            R.drawable.meat_svgrepo_com
        ),
        CategoryItem(
            "Thủy hải sản",
            R.string.cat_seafood,
            R.drawable.seafood_prawn_shrimp_lobster_svgrepo_com
        ),
        CategoryItem(
            name = "Sản phẩm khác",
            titleRes = R.string.cat_others,
            materialIcon = Icons.Default.MoreHoriz
        )
    )

    LaunchedEffect(Unit) {
        viewModel.updateHomeUiState {
            it.copy(categories = defaultCategories)
        }
    }

    LaunchedEffect(userName, userAvatar) {
        viewModel.updateHomeUiState {
            it.copy(userName = userName, userAvatar = userAvatar)
        }
    }

    LaunchedEffect(dashboardState) {
        when (val state = dashboardState) {
            is DashboardState.Loading -> {
                viewModel.updateHomeUiState {
                    it.copy(isLoading = true)
                }
            }

            is DashboardState.HomeDataSuccess -> {
                val mappedNew = state.newProducts.map { res ->
                    Product(
                        id = res.id,
                        name = res.name,
                        isNew = true,
                        priceStr = formatProductPrice(res.price),
                        seller = res.farmerName,
                        imageUrl = res.imageUrls.firstOrNull().orEmpty(),
                        unit = res.unit,
                        rating = res.rating,
                        reviewCount = res.reviewCount,
                        availableQuantity = res.quantity
                    )
                }

                val mappedSuggested = state.suggestedProducts.map { res ->
                    Product(
                        id = res.id,
                        name = res.name,
                        priceStr = formatProductPrice(res.price),
                        seller = res.farmerName,
                        imageUrl = res.imageUrls.firstOrNull().orEmpty(),
                        unit = res.unit,
                        rating = res.rating,
                        reviewCount = res.reviewCount,
                        availableQuantity = res.quantity
                    )
                }

                viewModel.updateHomeUiState {
                    it.copy(
                        isLoading = false,
                        newProducts = mappedNew,
                        suggestedProducts = mappedSuggested
                    )
                }
            }

            is DashboardState.ProductListSuccess -> {
                val mappedProducts = state.products.map { res ->
                    Product(
                        id = res.id,
                        name = res.name,
                        priceStr = formatProductPrice(res.price),
                        seller = res.farmerName,
                        imageUrl = res.imageUrls.firstOrNull().orEmpty(),
                        unit = res.unit,
                        rating = res.rating,
                        reviewCount = res.reviewCount,
                        availableQuantity = res.quantity
                    )
                }

                viewModel.updateHomeUiState {
                    it.copy(
                        isLoading = false,
                        suggestedProducts = mappedProducts
                    )
                }
                viewModel.resetState()
            }

            is DashboardState.Error -> {
                viewModel.updateHomeUiState {
                    it.copy(isLoading = false)
                }
                Toast.makeText(
                    context,
                    state.error,
                    Toast.LENGTH_LONG
                ).show()
                viewModel.resetState()
            }

            else -> {
                viewModel.updateHomeUiState {
                    it.copy(isLoading = false)
                }
            }
        }
    }

    HomeContent(
        uiState = uiState,
        searchState = searchState,
        onSearchChange = { query ->
            viewModel.onSearchQueryChanged(query)
        },
        onExecuteSearch = { query ->
            viewModel.executeSearch(query)
        },
        onLoadMoreSearch = {
            viewModel.loadMoreSearchResults()
        },
        onSearchProductClick = onNavigateToDetail,
        onCategoryClick = { category ->
            viewModel.updateHomeUiState {
                it.copy(selectedCategory = category.name)
            }
            viewModel.getProductsByCategory(category.name)
        },
        onProductClick = { product ->
            onNavigateToDetail(product.id)
        },
        onSeeAllClick = {
            viewModel.updateHomeUiState {
                it.copy(selectedCategory = null)
            }
            viewModel.getProducts()
        },
        onResetCategoryClick = {},
        onNavigateToChat = onNavigateToChat
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    searchState: ProductSearchUiState,
    onSearchChange: (String) -> Unit,
    onExecuteSearch: (String) -> Unit,
    onLoadMoreSearch: () -> Unit,
    onSearchProductClick: (String) -> Unit,
    onCategoryClick: (CategoryItem) -> Unit,
    onProductClick: (Product) -> Unit,
    onSeeAllClick: () -> Unit,
    onResetCategory: () -> Unit = {},
    onResetCategoryClick: () -> Unit = {},
    onNavigateToChat: () -> Unit
) {
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchMode) {
        isSearchMode = false
        focusManager.clearFocus()
        onSearchChange("")
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        AnimatedVisibility(
            visible = !isSearchMode,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            HomeHeader(userName = uiState.userName, userAvatar = uiState.userAvatar, onNavigateToChat = onNavigateToChat)
        }

        HomeSearchBar(
            query = searchState.query,
            onQueryChange = onSearchChange,
            isSearchMode = isSearchMode,
            onFocusChange = { isFocused -> if (isFocused) isSearchMode = true },
            onBackClick = {
                isSearchMode = false
                focusManager.clearFocus()
                onSearchChange("")
            },
            onSearchAction = {
                focusManager.clearFocus()
                onExecuteSearch(searchState.query)
            }
        )

        if (isSearchMode) {
            ProductSearchContent(
                state = searchState,
                onExecuteSearch = { query ->
                    focusManager.clearFocus()
                    onExecuteSearch(query)
                },
                onProductClick = onSearchProductClick,
                onLoadMore = onLoadMoreSearch,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                item {
                    SectionTitle(title = stringResource(R.string.category_title))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.padding(bottom = 24.dp)
                    ) {
                        items(uiState.categories) { category ->
                            CategoryChip(
                                category = category,
                                isSelected = uiState.selectedCategory == category.name,
                                onClick = { onCategoryClick(category) }
                            )
                        }
                    }
                }

                if (uiState.selectedCategory == null) {
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
                                    onClick = { onProductClick(product) })
                            }
                        }
                    }
                    item { SectionTitle(title = stringResource(R.string.suggested_title)) }
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
                            if (rowProducts.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                } else {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${uiState.selectedCategory} (${uiState.suggestedProducts.size})",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            TextButton(onClick = onSeeAllClick) {
                                Text(
                                    text = "Quay lại",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable {
                                        onSeeAllClick()
                                    }
                                )
                            }
                        }
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
                            if (rowProducts.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HomeSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    isSearchMode: Boolean,
    onFocusChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onSearchAction: () -> Unit
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = if (isSearchMode) 12.dp else 24.dp)
            .onFocusChanged { onFocusChange(it.isFocused) },
        placeholder = {
            Text(
                stringResource(R.string.search_placeholder),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        leadingIcon = {
            if (isSearchMode) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        trailingIcon = {
            if (isSearchMode && query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchAction() }),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(12.dp),
        singleLine = true
    )
}

@Composable
fun TextSuggestionItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
    HorizontalDivider(
        color = MaterialTheme.colorScheme.surfaceVariant,
        thickness = 1.dp,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
fun HomeHeader(userName: String, userAvatar: String, onNavigateToChat: () -> Unit) {
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (userAvatar.isNotBlank()) {
                AsyncImage(
                    model = userAvatar,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.greeting_user, userName),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onNavigateToChat) {
            Icon(
                Icons.Default.Chat,
                contentDescription = "Chat",
                tint = MaterialTheme.colorScheme.primary
            )
        }
//        IconButton(onClick = { /* TODO: Notification */ }) {
//            Icon(
//                Icons.Default.Notifications,
//                contentDescription = "Notification",
//                tint = MaterialTheme.colorScheme.primary
//            )
//        }
    }
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
            color = MaterialTheme.colorScheme.onBackground
        )
//        if (actionText != null && onActionClick != null) {
//            Text(
//                text = actionText,
//                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
//                color = MaterialTheme.colorScheme.primary,
//                modifier = Modifier.clickable { onActionClick() })
//        }
    }
}

@Composable
fun CategoryChip(
    category: CategoryItem,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val primaryGreen = Color(0xFF1E7032)
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) primaryGreen else MaterialTheme.colorScheme.surface  // ← THAY ĐỔI
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconTint = if (isSelected) {
            Color.White
        } else {
            MaterialTheme.colorScheme.primary
        }

        val materialIcon = category.materialIcon
        val drawableIcon = category.icon

        if (materialIcon != null) {
            Icon(
                imageVector = materialIcon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        } else if (drawableIcon != null) {
            Icon(
                painter = painterResource(drawableIcon),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(category.titleRes),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface  // ← THAY ĐỔI
        )
    }
}

@Composable
fun NewProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ProductCard(
        product = product,
        modifier = modifier.width(160.dp),
        onClick = onClick
    )
}

@Composable
fun SuggestedProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    ProductCard(
        product = product,
        modifier = modifier.fillMaxWidth(),
        onClick = onClick
    )
}

@Preview(showBackground = true, showSystemUi = true, locale = "vi")
@Composable
fun HomeScreenPreview() {
    AgritechTheme {
        HomeContent(
            uiState = HomeUiState(userName = "Hoàng"),            searchState = ProductSearchUiState(),
            onLoadMoreSearch = {},
            onSearchProductClick = {},
            onSearchChange = {},
            onExecuteSearch = {},
            onCategoryClick = {},
            onProductClick = {},
            onSeeAllClick = {},
            onResetCategory = {},
            onResetCategoryClick = {},
            onNavigateToChat = {}
        )
    }
}
