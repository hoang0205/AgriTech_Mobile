package com.example.agritech_mobile.ui.dashboard

import androidx.compose.runtime.*
import androidx.compose.material3.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
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

    var isSavingStock by mutableStateOf(false)
        private set
    var stockError by mutableStateOf<String?>(null)
        private set

    fun updateStock(product: SellerProductItem, quantity: Double, onSuccess: () -> Unit) {
        if (isSavingStock || _uiState.value.isLoadingMore) return
        isSavingStock = true
        stockError = null
        viewModelScope.launch {
            try {
                repository.updateStock(product.id, quantity).getOrThrow()
                _uiState.update { state ->
                    state.copy(products = state.products.map {
                        if (it.id == product.id) it.copy(availableQuantity = quantity) else it
                    }.sortedBy { it.availableQuantity <= 0.0 })
                }
                loadPage(refresh = true)
                onSuccess()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                stockError = e.message
            } finally {
                isSavingStock = false
            }
        }
    }

    fun clearStockError() { stockError = null }

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
    viewModel: MyProductsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var editingProduct by remember { mutableStateOf<SellerProductItem?>(null) }
    var stockInput by remember { mutableStateOf("") }

    editingProduct?.let { product ->
        val quantity = stockInput.replace(',', '.').toDoubleOrNull()
        val valid = quantity != null && quantity.isFinite() && quantity >= 0.0
        AlertDialog(
            onDismissRequest = { if (!viewModel.isSavingStock) editingProduct = null },
            title = { Text("Sửa tồn kho") },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text(product.name)
                    OutlinedTextField(
                        value = stockInput,
                        onValueChange = { stockInput = it; viewModel.clearStockError() },
                        label = { Text("Tồn kho hiện tại (${product.unit})") },
                        supportingText = { Text(viewModel.stockError ?: "Nhập 0 để đánh dấu hết hàng.") },
                        isError = !valid || viewModel.stockError != null,
                        enabled = !viewModel.isSavingStock,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            },
            confirmButton = {
                TextButton(enabled = valid && !viewModel.isSavingStock, onClick = {
                    viewModel.updateStock(product, requireNotNull(quantity)) {
                        editingProduct = null
                        Toast.makeText(context, "Đã cập nhật tồn kho", Toast.LENGTH_SHORT).show()
                    }
                }) { Text(if (viewModel.isSavingStock) "Đang lưu..." else "Lưu") }
            },
            dismissButton = {
                TextButton(enabled = !viewModel.isSavingStock, onClick = { editingProduct = null }) { Text("Hủy") }
            }
        )
    }

    SellerStoreContent(
        uiState = state,
        onBackClick = onBackClick,
        onProductClick = onProductClick,
        onChatClick = {},
        isOwnStore = true,
        onLoadMore = viewModel::loadMore,
        onRetry = viewModel::retry,
        onEditStock = { product ->
            viewModel.clearStockError()
            stockInput = product.availableQuantity.toString()
            editingProduct = product
        }
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
