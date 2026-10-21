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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.data.remote.dto.DeliveryStatus
import com.example.agritech_mobile.data.remote.dto.ProductResponse
import com.example.agritech_mobile.ui.theme.AgritechTheme
import com.example.agritech_mobile.ui.user.AddressState
import com.example.agritech_mobile.ui.user.UserViewModel
import com.example.agritech_mobile.ui.notification.HomeBadgeViewModel

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
    val availableQuantity: Double = 0.0,
    val distanceKm: Double? = null,
    val deliveryStatus: DeliveryStatus? = null
)

private fun ProductResponse.toNearbyProduct() = Product(
    id = id, name = name, priceStr = formatProductPrice(price), seller = farmerName,
    imageUrl = imageUrls.firstOrNull().orEmpty(), unit = unit, rating = rating,
    reviewCount = reviewCount, availableQuantity = quantity,
    distanceKm = distanceKm, deliveryStatus = deliveryStatus
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
    val selectedCategory: String? = null,
    val recommendedProducts: List<Product> = emptyList()
)

@Composable
fun HomeScreen(
    viewModel: DashboardViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToDeliveryAddress: () -> Unit = {},
    addressViewModel: UserViewModel = hiltViewModel(),
    onNavigateToNotifications: () -> Unit = {},
    badgeViewModel: HomeBadgeViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val unreadMessages by badgeViewModel.unreadMessages.collectAsStateWithLifecycle()
    val unreadNotifications by badgeViewModel.unreadNotifications.collectAsStateWithLifecycle()
    LifecycleResumeEffect(badgeViewModel) {
        badgeViewModel.startUpdates()
        onPauseOrDispose { badgeViewModel.stopUpdates() }
    }
    val dashboardState by viewModel.dashboardState.collectAsStateWithLifecycle()
    val uiState by viewModel.homeUiState.collectAsStateWithLifecycle()
    val searchState by viewModel.searchUiState.collectAsStateWithLifecycle()
    val userName by viewModel.userName.collectAsStateWithLifecycle()
    val userAvatar by viewModel.userAvatar.collectAsStateWithLifecycle()
    val nearbyState by viewModel.nearbyState.collectAsStateWithLifecycle()
    val addressState by addressViewModel.addressState.collectAsStateWithLifecycle()
    val homeAddressId by addressViewModel.homeDeliveryAddressId.collectAsStateWithLifecycle()
    val addresses = (addressState as? AddressState.Success)?.addresses.orEmpty()
    val deliveryAddress = addresses.find { it.id == homeAddressId }
        ?: addresses.firstOrNull { it.isDefault }
        ?: addresses.firstOrNull()
        ?: nearbyState.address?.takeIf { addressState !is AddressState.Success && it.id == homeAddressId }
    val hasDeliveryLocation = deliveryAddress?.let { address ->
        address.latitude?.let { it.isFinite() && it in -90.0..90.0 } == true &&
            address.longitude?.let { it.isFinite() && it in -180.0..180.0 } == true
    } == true
    val addressMessage = when {
        addressState is AddressState.Loading -> "Đang tải địa chỉ…"
        addressState is AddressState.Error -> "Không tải được địa chỉ. Nhấn để thử lại."
        deliveryAddress == null -> "Chọn địa chỉ nhận hàng"
        !hasDeliveryLocation -> "Cần bổ sung vị trí trên bản đồ"
        else -> null
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        addressViewModel.getUserAddresses()
    }

    LaunchedEffect(addressState, deliveryAddress) {
        if (addressState is AddressState.Success) {
            viewModel.setNearbyDeliveryAddress(deliveryAddress.takeIf { hasDeliveryLocation })
        }
    }



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
                        suggestedProducts = mappedSuggested,
                        recommendedProducts = mappedSuggested
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
        nearbyState = if (hasDeliveryLocation) {
            if (nearbyState.address == deliveryAddress) nearbyState
            else NearbyProductsState(address = deliveryAddress, isLoading = true)
        } else NearbyProductsState(),
        onLoadMoreNearby = { viewModel.loadNearbyProducts() },
        deliveryAddress = deliveryAddress?.addressDetail,
        addressMessage = addressMessage,
        onNavigateToDeliveryAddress = {
            if (addressState is AddressState.Error) addressViewModel.getUserAddresses()
            else onNavigateToDeliveryAddress()
        },
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
            if (hasDeliveryLocation) viewModel.setNearbyCategory(category.name)
            else viewModel.getProductsByCategory(category.name)
        },
        onProductClick = { product ->
            onNavigateToDetail(product.id)
        },
        onSeeAllClick = {
            viewModel.updateHomeUiState {
                it.copy(selectedCategory = null)
            }
            if (hasDeliveryLocation) viewModel.setNearbyCategory(null)
            else viewModel.getProducts()
        },
        onResetCategoryClick = {
            viewModel.updateHomeUiState { it.copy(selectedCategory = null) }
            if (hasDeliveryLocation) viewModel.setNearbyCategory(null)
            else viewModel.loadHomeData()
        },
        onNavigateToChat = onNavigateToChat,
        onNavigateToNotifications = onNavigateToNotifications,
        unreadMessages = unreadMessages,
        unreadNotifications = unreadNotifications
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
    onNavigateToChat: () -> Unit,
    deliveryAddress: String? = null,
    addressMessage: String? = null,
    onNavigateToDeliveryAddress: () -> Unit = {},
    nearbyState: NearbyProductsState = NearbyProductsState(),
    onLoadMoreNearby: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
    unreadMessages: Long = 0,
    unreadNotifications: Long = 0
) {
    var isSearchMode by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    BackHandler(enabled = isSearchMode) {
        isSearchMode = false
        focusManager.clearFocus()
        onSearchChange("")
    }

    BackHandler(enabled = !isSearchMode && uiState.selectedCategory != null) {
        onResetCategoryClick()
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
            HomeHeader(userName = uiState.userName, userAvatar = uiState.userAvatar, onNavigateToChat = onNavigateToChat,
                onNavigateToNotifications = onNavigateToNotifications, unreadMessages = unreadMessages, unreadNotifications = unreadNotifications)
        }

        Surface(
            onClick = onNavigateToDeliveryAddress,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 12.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Giao đến", style = MaterialTheme.typography.labelMedium)
                    Text(
                        deliveryAddress ?: addressMessage ?: "Chọn địa chỉ nhận hàng",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (deliveryAddress != null && addressMessage != null) {
                        Text(addressMessage, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                }
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Chọn địa chỉ giao hàng")
            }
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

                if (nearbyState.address != null) {
                    if (uiState.selectedCategory == null && nearbyState.category == null) {
                        item {
                            SectionTitle(title = "Gợi ý cho bạn")
                            if (uiState.recommendedProducts.isNotEmpty()) {
                                LazyRow(
                                    contentPadding = PaddingValues(horizontal = 20.dp),
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    modifier = Modifier.padding(bottom = 24.dp)
                                ) {
                                    items(uiState.recommendedProducts, key = { it.id }) { product ->
                                        NewProductCard(product = product, onClick = { onProductClick(product) })
                                    }
                                }
                            } else if (uiState.isLoading) {
                                Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                                }
                            } else {
                                Text(
                                    "Chưa có gợi ý phù hợp.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                                )
                            }
                        }
                    }
                    item {
                        SectionTitle(
                            title = nearbyState.category ?: "Sản phẩm gần bạn",
                            actionText = if (nearbyState.category != null) "Quay lại" else "Làm mới",
                            onActionClick = if (nearbyState.category != null) onResetCategoryClick else onSeeAllClick
                        )
                        Text(
                            "Khoảng cách đường thẳng ước tính. Phạm vi giao hàng 30 km.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                        )
                    }
                    items(nearbyState.products.chunked(2)) { rowProducts ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowProducts.forEach { response ->
                                val product = response.toNearbyProduct()
                                SuggestedProductCard(
                                    product = product, modifier = Modifier.weight(1f),
                                    onClick = { onProductClick(product) }
                                )
                            }
                            if (rowProducts.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (nearbyState.isLoading) CircularProgressIndicator()
                            else if (nearbyState.error != null) {
                                Text(nearbyState.error, color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = onLoadMoreNearby) { Text("Thử lại") }
                            } else if (nearbyState.products.isEmpty()) {
                                Text("Chưa có sản phẩm phù hợp.")
                            } else if (nearbyState.hasMore) {
                                OutlinedButton(onClick = onLoadMoreNearby) { Text("Xem thêm") }
                            }
                        }
                    }
                } else if (uiState.selectedCategory == null) {
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
                            TextButton(onClick = onResetCategoryClick) {
                                Text(
                                    text = "Quay lại",
                                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.primary
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
fun HomeHeader(
    userName: String, userAvatar: String, onNavigateToChat: () -> Unit,
    onNavigateToNotifications: () -> Unit = {}, unreadMessages: Long = 0, unreadNotifications: Long = 0
) {
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
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        HomeUnreadIcon(Icons.Default.Chat, "Tin nhắn", unreadMessages, onNavigateToChat)
        HomeUnreadIcon(Icons.Default.Notifications, "Thông báo", unreadNotifications, onNavigateToNotifications)
    }
}

@Composable
private fun HomeUnreadIcon(icon: ImageVector, label: String, unread: Long, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        BadgedBox(badge = {
            if (unread > 0) Badge { Text(if (unread > 99) "99+" else unread.toString()) }
        }) {
            Icon(icon, contentDescription = if (unread > 0) "$label, $unread chưa đọc" else label,
                tint = MaterialTheme.colorScheme.primary)
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
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (actionText != null && onActionClick != null) {
            TextButton(onClick = onActionClick) {
                Text(actionText, fontWeight = FontWeight.Bold)
            }
        }
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
