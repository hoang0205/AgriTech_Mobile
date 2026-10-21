package com.example.agritech_mobile.ui.dashboard

import androidx.compose.runtime.*
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.repository.ProductRepository
import com.example.agritech_mobile.ui.theme.AgritechTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

@HiltViewModel
class MyProductsViewModel @Inject constructor(
    private val repository: ProductRepository,
    tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        SellerStoreUiState(
            sellerName = tokenManager.getUserName(),
            sellerAvatar = tokenManager.getAvatarUrl()
        )
    )

    val uiState = _uiState.asStateFlow()

    private var nextPage = 0

    init {
        loadPage(refresh = true)
    }

    fun refresh() { loadPage(refresh = true) }

    fun loadMore() {
        if (!_uiState.value.hasMore) return
        loadPage(refresh = false)
    }

    fun retry() {
        loadPage(
            refresh = _uiState.value.products.isEmpty()
        )
    }

    private fun loadPage(refresh: Boolean) {
        val current = _uiState.value

        if (current.isLoading || current.isLoadingMore) return

        val requestedPage = if (refresh) 0 else nextPage

        _uiState.update {
            it.copy(
                isLoading = refresh,
                isLoadingMore = !refresh,
                error = null
            )
        }

        viewModelScope.launch {
            try {
                val response = repository.getMyProducts(
                    page = requestedPage,
                    size = 12
                ).getOrThrow()

                val products = response.content.map { product ->
                    SellerProductItem(
                        id = product.id,
                        name = product.name,
                        priceStr = formatProductPrice(product.price),
                        unit = product.unit,
                        imageUrl = product.imageUrls.firstOrNull().orEmpty(),
                        seller = product.farmerName,
                        rating = product.rating,
                        reviewCount = product.reviewCount,
                        availableQuantity = product.quantity
                    )
                }

                nextPage = requestedPage + 1

                _uiState.update { state ->
                    val previous = if (refresh) {
                        emptyList()
                    } else {
                        state.products
                    }

                    state.copy(
                        products = (previous + products).distinctBy { it.id },
                        totalProducts = response.totalElements,
                        isLoading = false,
                        isLoadingMore = false,
                        hasMore = !response.last &&
                                response.content.isNotEmpty(),
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = when (e) {
                            is HttpException -> when (e.code()) {
                                401 -> "Vui lòng đăng nhập lại."
                                404 -> "Không tìm thấy API sản phẩm của tôi."
                                else -> "Không tải được sản phẩm (${e.code()})."
                            }
                            else -> "Không tải được sản phẩm. Vui lòng thử lại."
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun MyProductsScreen(
    onBackClick: () -> Unit,
    onProductClick: (String) -> Unit,
    onEditProduct: (String) -> Unit,
    viewModel: MyProductsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refresh()
    }

    SellerStoreContent(
        uiState = state,
        onBackClick = onBackClick,
        onProductClick = onProductClick,
        onChatClick = {},
        isOwnStore = true,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
        onEditProduct = { product -> onEditProduct(product.id) }
    )
}

@Preview(
    name = "Sản phẩm của tôi",
    showBackground = true,
    locale = "vi",
    widthDp = 411,
    heightDp = 900
)
@Composable
private fun MyProductsPreview() {
    AgritechTheme {
        SellerStoreContent(
            uiState = SellerStoreUiState(
                sellerName = "Hoàng",
                totalProducts = 2,
                products = listOf(
                    SellerProductItem(
                        id = "01",
                        name = "Cà chua sạch",
                        priceStr = "25.000 đ",
                        unit = "kg",
                        seller = "Hoàng"
                    ),
                    SellerProductItem(
                        id = "02",
                        name = "Cam ngọt",
                        priceStr = "40.000 đ",
                        unit = "kg",
                        seller = "Hoàng"
                    )
                )
            ),
            onBackClick = {},
            onProductClick = {},
            onChatClick = {},
            isOwnStore = true
        )
    }
}
