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
    val imageUrl: String = ""
)

data class CategoryItem(val name: String, val titleRes: Int, val icon: Int)
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

    val textSuggestions by viewModel.textSuggestions.collectAsStateWithLifecycle()
    val searchResultsRes by viewModel.searchResults.collectAsStateWithLifecycle()

    var uiState by remember { mutableStateOf(HomeUiState()) }

    val userName by viewModel.userName.collectAsStateWithLifecycle()

    val finalSearchResults = searchResultsRes.map { res ->
        Product(
            id = res.id,
            name = res.name,
            priceStr = "${res.price.toLong()} đ",
            seller = res.farmerName,
            imageUrl = res.imageUrls.firstOrNull() ?: "",
            isNew = false
        )
    }

    val defaultCategories = listOf(
        CategoryItem("Rau củ", R.string.cat_vegetables, R.drawable.vegetable_food_salad_lettuce_cabbage_svgrepo_com),
        CategoryItem("Trái cây", R.string.cat_fruits, R.drawable.fruit_fruits_grape_svgrepo_com),
        CategoryItem("Thịt", R.string.cat_meat, R.drawable.meat_svgrepo_com),
        CategoryItem("Thủy hải sản", R.string.cat_seafood, R.drawable.seafood_prawn_shrimp_lobster_svgrepo_com),
        CategoryItem("Khác", R.string.cat_others, R.drawable.food_delivery_bot_svgrepo_com),
    )

    LaunchedEffect(Unit) {
        uiState = uiState.copy(categories = defaultCategories, userName = userName)
        viewModel.loadHomeData()
    }

    LaunchedEffect(dashboardState) {
        when (dashboardState) {
            is DashboardState.Loading -> uiState = uiState.copy(isLoading = true)
            is DashboardState.HomeDataSuccess -> {
                val state = dashboardState as DashboardState.HomeDataSuccess
                val mappedNew = state.newProducts.map { res ->
                    Product(
                        res.id,
                        res.name,
                        "${res.price.toLong()} đ",
                        res.farmerName,
                        false,
                        true,
                        res.imageUrls.firstOrNull() ?: ""
                    )
                }
                val mappedSuggestedProducts = state.suggestedProducts.map { res ->
                    Product(
                        res.id,
                        res.name,
                        "${res.price.toLong()} đ",
                        res.farmerName,
                        false,
                        false,
                        res.imageUrls.firstOrNull() ?: ""
                    )
                }
                uiState = uiState.copy(
                    isLoading = false,
                    newProducts = mappedNew,
                    suggestedProducts = mappedSuggestedProducts
                )
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

            else -> uiState = uiState.copy(isLoading = false)
        }
    }

    HomeContent(
        uiState = uiState,
        textSuggestions = textSuggestions,
        searchResults = finalSearchResults,
        onSearchChange = { newQuery ->
            uiState = uiState.copy(searchQuery = newQuery)
            viewModel.onSearchQueryChanged(newQuery)
        },
        onExecuteSearch = { queryToSearch ->
            uiState = uiState.copy(searchQuery = queryToSearch)
            viewModel.executeSearch(queryToSearch)
        },
        onCategoryClick = { category -> viewModel.getProductsByCategory(category.name) },
        onProductClick = { product -> onNavigateToDetail(product.id) },
        onSeeAllClick = { viewModel.getProducts() }
    )
}

@Composable
fun HomeContent(
    uiState: HomeUiState,
    textSuggestions: List<String>,
    searchResults: List<Product>,
    onSearchChange: (String) -> Unit,
    onExecuteSearch: (String) -> Unit,
    onCategoryClick: (CategoryItem) -> Unit,
    onProductClick: (Product) -> Unit,
    onSeeAllClick: () -> Unit
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
            HomeHeader(userName = uiState.userName)
        }

        HomeSearchBar(
            query = uiState.searchQuery,
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
                onExecuteSearch(uiState.searchQuery)
            }
        )

        if (isSearchMode) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else if (searchResults.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(searchResults.chunked(2)) { rowProducts ->
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
                                    onClick = { onProductClick(product) })
                            }
                            if (rowProducts.size == 1) Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            } else if (textSuggestions.isNotEmpty()) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    items(textSuggestions) { text ->
                        TextSuggestionItem(
                            text = text,
                            onClick = {
                                focusManager.clearFocus()
                                onExecuteSearch(text)
                            }
                        )
                    }
                }
            } else if (uiState.searchQuery.isNotBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.search_result_not_found),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        stringResource(R.string.search_noti),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
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
                                onClick = { onCategoryClick(category) })
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
                            NewProductCard(product = product, onClick = { onProductClick(product) })
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
                                onClick = { onProductClick(product) })
                        }
                        if (rowProducts.size == 1) Spacer(modifier = Modifier.weight(1f))
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
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.greeting_user, userName),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = { /* TODO: Notification */ }) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notification",
                tint = MaterialTheme.colorScheme.primary
            )
        }
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
        if (actionText != null && onActionClick != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onActionClick() })
        }
    }
}

@Composable
fun CategoryChip(category: CategoryItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = category.icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = stringResource(category.titleRes),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NewProductCard(product: Product, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clickable { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
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
                    .background(MaterialTheme.colorScheme.surface)
                    .align(Alignment.TopEnd)
                    .clickable { /* TODO: Toggle Favorite */ }, contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (product.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = product.priceStr,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Storefront,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = stringResource(R.string.sold_by, product.seller),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SuggestedProductCard(product: Product, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier.clickable { onClick() }) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
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
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = product.priceStr,
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = product.seller,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true, locale = "vi")
@Composable
fun HomeScreenPreview() {
    AgritechTheme {
        HomeContent(
            uiState = HomeUiState(userName = "Hoàng", searchQuery = ""),
            textSuggestions = listOf("Dưa hấu Long An", "Dưa lưới"),
            searchResults = emptyList(),
            onSearchChange = {},
            onExecuteSearch = {},
            onCategoryClick = {},
            onProductClick = {},
            onSeeAllClick = {}
        )
    }
}