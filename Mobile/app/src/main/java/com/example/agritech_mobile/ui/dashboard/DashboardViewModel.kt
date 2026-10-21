package com.example.agritech_mobile.ui.dashboard

import android.util.Log
import android.util.Log.e
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.remote.dto.GenerateDescriptionRequest
import com.example.agritech_mobile.data.remote.dto.ProductResponse
import com.example.agritech_mobile.data.remote.dto.PageResponse
import com.example.agritech_mobile.data.remote.dto.ShippingDetails
import com.example.agritech_mobile.data.remote.dto.ReviewModelsResponse
import com.example.agritech_mobile.data.remote.dto.ReviewSummaryResponse
import com.example.agritech_mobile.data.repository.AiRepository
import com.example.agritech_mobile.data.repository.ProductRepository
import com.example.agritech_mobile.data.repository.ReviewRepository
import com.example.agritech_mobile.data.repository.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

sealed class DashboardState {
    object Idle : DashboardState()
    object Loading : DashboardState()

    data class HomeDataSuccess(
        val newProducts: List<ProductResponse>,
        val suggestedProducts: List<ProductResponse>
    ) : DashboardState()

    data class ProductListSuccess(val products: List<ProductResponse>) : DashboardState()
    data class ProductDetailSuccess(val product: ProductResponse) : DashboardState()
    data class ActionSuccess(val message: String) : DashboardState()
    data class Error(val error: String) : DashboardState()
}

data class ProductSearchUiState(
    val query: String = "",
    val farmerId: String? = null,
    val suggestions: List<String> = emptyList(),
    val products: List<ProductResponse> = emptyList(),
    val isSuggesting: Boolean = false,
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val totalElements: Int = 0,
    val error: String? = null
)

data class NearbyProductsState(
    val address: ShippingDetails? = null,
    val category: String? = null,
    val products: List<ProductResponse> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = false,
    val nextPage: Int = 0,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val uploadRepository: UploadRepository,
    private val tokenManager: TokenManager,
    private val aiRepository: AiRepository,
    private val reviewRepository: ReviewRepository
) : ViewModel() {
    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Idle)
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    private val _productState = MutableStateFlow<ProductResponse?>(null)
    val productState: StateFlow<ProductResponse?> = _productState.asStateFlow()

    private val _homeUiState = MutableStateFlow(HomeUiState())
    val homeUiState: StateFlow<HomeUiState> = _homeUiState.asStateFlow()

    private val _nearbyState = MutableStateFlow(NearbyProductsState())
    val nearbyState = _nearbyState.asStateFlow()
    private var nearbyJob: Job? = null

    fun setNearbyDeliveryAddress(address: ShippingDetails?) {
        if (_nearbyState.value.address == address) return
        nearbyJob?.cancel()
        _nearbyState.value = NearbyProductsState(address = address, category = _homeUiState.value.selectedCategory)
        if (address != null) loadNearbyProducts()
        else {
            val category = _homeUiState.value.selectedCategory
            if (category == null) loadHomeData() else getProductsByCategory(category)
        }
        val search = _searchUiState.value
        if (search.hasSearched) executeSearch(search.query, search.farmerId)
        else if (search.query.isNotBlank()) onSearchQueryChanged(search.query, search.farmerId)
        else clearSearch()
    }

    fun setNearbyCategory(category: String?) {
        nearbyJob?.cancel()
        _nearbyState.value = NearbyProductsState(address = _nearbyState.value.address, category = category)
        loadNearbyProducts()
    }

    fun loadNearbyProducts() {
        val current = _nearbyState.value
        val address = current.address ?: return
        if (current.isLoading || (current.nextPage > 0 && !current.hasMore)) return
        _nearbyState.value = current.copy(isLoading = true, error = null)
        nearbyJob = viewModelScope.launch {
            val result = repository.getNearbyProducts(address.id, current.nextPage, category = current.category)
            currentCoroutineContext().ensureActive()
            result.onSuccess { response ->
                _nearbyState.value = current.copy(
                    products = (current.products + response.content).distinctBy { it.id },
                    nextPage = current.nextPage + 1,
                    hasMore = !response.last,
                    isLoading = false
                )
            }.onFailure {
                _nearbyState.value = current.copy(
                    isLoading = false,
                    error = "Không tải được sản phẩm theo địa chỉ nhận hàng. Vui lòng thử lại."
                )
            }
        }
    }

    private suspend fun searchForDeliveryAddress(
        query: String,
        page: Int,
        size: Int,
        farmerId: String?
    ): Result<PageResponse<ProductResponse>> {
        val address = _nearbyState.value.address
        return if (address != null && farmerId == null) {
            repository.getNearbyProducts(address.id, page, size, keyword = query)
        } else repository.searchProducts(query, page, size, farmerId)
    }

    fun updateHomeUiState(transform: (HomeUiState) -> HomeUiState) {
        _homeUiState.update(transform)
    }

    private val _searchUiState = MutableStateFlow(ProductSearchUiState())
    val searchUiState: StateFlow<ProductSearchUiState> =
        _searchUiState.asStateFlow()

    private var searchJob: Job? = null
    private var nextSearchPage = 0
    private val searchPageSize = 10

    private fun publishSearchState(state: ProductSearchUiState) {
        _searchUiState.value = state
    }

    val userName: StateFlow<String> = tokenManager.userNameFlow
    val userAvatar: StateFlow<String> = tokenManager.avatarUrlFlow

    private val _reviews = MutableStateFlow<List<ReviewModelsResponse>>(emptyList())
    val reviews: StateFlow<List<ReviewModelsResponse>> = _reviews.asStateFlow()

    private val _reviewSummary = MutableStateFlow<ReviewSummaryResponse?>(null)
    val reviewSummary: StateFlow<ReviewSummaryResponse?> = _reviewSummary.asStateFlow()

    private val _aiPros = MutableStateFlow<List<String>>(emptyList())
    val aiPros: StateFlow<List<String>> = _aiPros.asStateFlow()

    private val _aiCons = MutableStateFlow<List<String>>(emptyList())
    val aiCons: StateFlow<List<String>> = _aiCons.asStateFlow()

    private val _isAiSummarizing = MutableStateFlow(false)
    val isAiSummarizing: StateFlow<Boolean> = _isAiSummarizing.asStateFlow()

    private val _sellerProducts = MutableStateFlow<List<ProductResponse>>(emptyList())
    val sellerProducts: StateFlow<List<ProductResponse>> = _sellerProducts.asStateFlow()

    private val _isSellerLoading = MutableStateFlow(false)
    val isSellerLoading: StateFlow<Boolean> = _isSellerLoading.asStateFlow()

    init {
        loadHomeData()
    }

    fun onSearchQueryChanged(
        query: String,
        farmerId: String? = null
    ) {
        searchJob?.cancel()
        nextSearchPage = 0

        val keyword = query.trim()
        val initialState = ProductSearchUiState(
            query = query,
            farmerId = farmerId,
            isSuggesting = keyword.isNotEmpty()
        )
        publishSearchState(initialState)

        if (!validateSearchKeyword(keyword, initialState)) return
        if (keyword.isEmpty()) return

        searchJob = viewModelScope.launch {
            delay(300)

            val result = repository.getSuggestedProducts(
                query = keyword,
                farmerId = farmerId
            )

            currentCoroutineContext().ensureActive()

            result.fold(
                onSuccess = { suggestions ->
                    publishSearchState(
                        initialState.copy(
                            suggestions = suggestions,
                            isSuggesting = false
                        )
                    )
                },
                onFailure = {
                    publishSearchState(
                        initialState.copy(
                            isSuggesting = false,
                            error = "Không tải được gợi ý. Bạn vẫn có thể bấm tìm kiếm."
                        )
                    )
                }
            )
        }
    }

    fun executeSearch(
        query: String,
        farmerId: String? = null
    ) {
        searchJob?.cancel()
        nextSearchPage = 0

        val keyword = query.trim()
        val initialState = ProductSearchUiState(
            query = keyword,
            farmerId = farmerId,
            isSearching = keyword.isNotEmpty()
        )
        publishSearchState(initialState)

        if (!validateSearchKeyword(keyword, initialState)) return
        if (keyword.isEmpty()) return

        searchJob = viewModelScope.launch {
            val result = searchForDeliveryAddress(
                query = keyword,
                page = 0,
                size = searchPageSize,
                farmerId = farmerId
            )

            currentCoroutineContext().ensureActive()

            result.fold(
                onSuccess = { response ->
                    nextSearchPage = 1

                    publishSearchState(
                        initialState.copy(
                            products = response.content,
                            isSearching = false,
                            hasSearched = true,
                            hasMore = !response.last,
                            totalElements = response.totalElements
                        )
                    )
                },
                onFailure = {
                    publishSearchState(
                        initialState.copy(
                            isSearching = false,
                            error = "Không tìm kiếm được sản phẩm. Vui lòng thử lại."
                        )
                    )
                }
            )
        }
    }

    fun loadMoreSearchResults() {
        val current = _searchUiState.value

        if (
            !current.hasSearched ||
            !current.hasMore ||
            current.isSearching ||
            current.isLoadingMore ||
            current.isSuggesting
        ) {
            return
        }

        publishSearchState(
            current.copy(
                isLoadingMore = true,
                error = null
            )
        )

        searchJob = viewModelScope.launch {
            val result = searchForDeliveryAddress(
                query = current.query,
                page = nextSearchPage,
                size = searchPageSize,
                farmerId = current.farmerId
            )

            currentCoroutineContext().ensureActive()

            result.fold(
                onSuccess = { response ->
                    nextSearchPage += 1

                    publishSearchState(
                        current.copy(
                            products = (current.products + response.content)
                                .distinctBy { it.id },
                            isLoadingMore = false,
                            hasMore = !response.last,
                            totalElements = response.totalElements,
                            error = null
                        )
                    )
                },
                onFailure = {
                    publishSearchState(
                        current.copy(
                            isLoadingMore = false,
                            error = "Không tải được trang tiếp theo. Vui lòng thử lại."
                        )
                    )
                }
            )
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        searchJob = null
        nextSearchPage = 0
        publishSearchState(ProductSearchUiState())
    }

    fun loadHomeData() {
        _dashboardState.value = DashboardState.Loading
        viewModelScope.launch {
            try {
                Log.d("LOAD_DATA_DEBUG", "Bắt đầu gọi API lấy sản phẩm trang chủ...")
                val newProductsDeferred = async { repository.getProducts() }
                val randomProductsDeferred = async { repository.getRecommendations() }

                val newProductsResult = newProductsDeferred.await()
                val randomProductsResult = randomProductsDeferred.await()

                if (newProductsResult.isFailure && randomProductsResult.isFailure) {
                    val errNew = newProductsResult.exceptionOrNull()?.message
                    val errRandom = randomProductsResult.exceptionOrNull()?.message
                    Log.e("LOAD_DATA_DEBUG", "LỖI KẾT NỐI SERVER: new=$errNew | random=$errRandom")

                    _dashboardState.value = DashboardState.Error(
                        errNew ?: errRandom ?: "Không thể kết nối đến máy chủ Spring Boot"
                    )
                    return@launch
                }

                val newProducts = newProductsResult.getOrNull()?.content ?: emptyList()
                val suggestedProducts = randomProductsResult.getOrNull() ?: emptyList()

                Log.d("LOAD_DATA_DEBUG", "Tải thành công: ${newProducts.size} sản phẩm mới, ${suggestedProducts.size} gợi ý.")

                _dashboardState.value = DashboardState.HomeDataSuccess(
                    newProducts = newProducts,
                    suggestedProducts = suggestedProducts
                )
            } catch (e: Exception) {
                Log.e("LOAD_DATA_DEBUG", "CRASH khi loadHomeData: ${e.localizedMessage}", e)
                _dashboardState.value = DashboardState.Error(e.message ?: "Lỗi tải dữ liệu")
            }
        }
    }

    fun resetState() {
        _dashboardState.value = DashboardState.Idle
    }

    fun getProducts() {
        _dashboardState.value = DashboardState.Loading
        viewModelScope.launch {
            val result = repository.getProducts()
            result.onSuccess { response ->
                _dashboardState.value = DashboardState.ProductListSuccess(response.content)
            }.onFailure { exception ->
                _dashboardState.value = DashboardState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun createProduct(
        name: String,
        category: String,
        price: Double,
        quantity: Double,
        unit: String,
        description: String,
        imageUrls: List<String>,
        costPrice: Double? = null
    ) {
        _dashboardState.value = DashboardState.Loading
        viewModelScope.launch {
            val result = repository.createProduct(
                name = name,
                category = category,
                price = price,
                quantity = quantity,
                unit = unit,
                description = description,
                imageUrls = imageUrls,
                costPrice = costPrice
            )
            result.onSuccess { response ->
                _dashboardState.value =
                    DashboardState.ActionSuccess("Đăng bán sản phẩm thành công!")
            }.onFailure { exception ->
                _dashboardState.value = DashboardState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun getProductsBySeller(sellerId: String) {
        _isSellerLoading.value = true
        _sellerProducts.value = emptyList()
        viewModelScope.launch {
            val result = repository.getProductsByFarmerId(sellerId)
            result.onSuccess { response ->
                _sellerProducts.value = response.content
                _isSellerLoading.value = false
            }.onFailure { exception ->
                Log.e("DashboardViewModel", "Lỗi lấy sản phẩm của người bán: ${exception.message}")
                _sellerProducts.value = emptyList()
                _isSellerLoading.value = false
            }
        }
    }

    fun publishProduct(
        name: String,
        category: String,
        price: Double,
        quantity: Double,
        unit: String,
        description: String,
        imageParts: List<MultipartBody.Part>,
        costPrice: Double? = null
    ) {
        _dashboardState.value = DashboardState.Loading
        viewModelScope.launch {
            val uploadedUrls = uploadMultipleImages(imageParts)

            if (uploadedUrls.isNullOrEmpty()) {
                _dashboardState.value = DashboardState.Error("Up ảnh thất bại")
                return@launch
            }
            Log.d("PublishProduct", "Uploaded URLs: $uploadedUrls")

            createProduct(
                name = name,
                category = category,
                price = price,
                quantity = quantity,
                unit = unit,
                description = description,
                imageUrls = uploadedUrls,
                costPrice = costPrice
            )
        }
    }

    fun getProductById(productId: String) {
        _dashboardState.value = DashboardState.Loading
        viewModelScope.launch {
            val result = repository.getProductById(productId)
            result.onSuccess { response ->
                _dashboardState.value = DashboardState.ProductDetailSuccess(response)
            }.onFailure { exception ->
                _dashboardState.value = DashboardState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun getProductsByCategory(category: String) {
        _dashboardState.value = DashboardState.Loading
        viewModelScope.launch {
            val result = repository.getProductsByCategory(category)
            result.onSuccess { response ->
                _dashboardState.value = DashboardState.ProductListSuccess(response.content)
            }.onFailure { exception ->
                _dashboardState.value = DashboardState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    private fun validateSearchKeyword(
        keyword: String,
        state: ProductSearchUiState
    ): Boolean {
        if (keyword.length <= 255) return true

        publishSearchState(
            state.copy(
                isSuggesting = false,
                isSearching = false,
                error = "Từ khóa tìm kiếm không được vượt quá 255 ký tự."
            )
        )
        return false
    }

    suspend fun uploadMultipleImages(imageParts: List<MultipartBody.Part>): List<String>? {
        return try {
            val result = uploadRepository.uploadImages(imageParts)
            if (result.isSuccess) {
                result.getOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getPricePrediction(
        productName: String,
        onResult: (String?, String) -> Unit
    ) {
        viewModelScope.launch {
            aiRepository.predictPrice(productName)
                .onSuccess { response ->
                    val low = response.priceLow
                    val high = response.priceHigh

                    if (!response.success ||
                        low == null || high == null ||
                        low <= 0 || high < low
                    ) {
                        onResult(
                            null,
                            response.error ?: "Chưa có giá tham khảo phù hợp."
                        )
                        return@onSuccess
                    }

                    val dateNote = response.dataDate
                        ?.let { "\nNgày dữ liệu: $it." }
                        .orEmpty()

                    if (low == high) {
                        onResult(
                            low.toString(),
                            "Giá tham khảo: ${response.priceRange}$dateNote"
                        )
                    } else {
                        onResult(
                            null,
                            "Khoảng giá tham khảo: ${response.priceRange}." +
                                    "\nBạn hãy chọn giá bán phù hợp.$dateNote"
                        )
                    }
                }
                .onFailure {
                    onResult(null, "Lỗi kết nối đến máy chủ AI.")
                }
        }
    }

    fun verifyImageWithAI(
        imagePart: okhttp3.MultipartBody.Part,
        onResult: (isValid: Boolean, category: String?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                Log.d("AI_IMAGE_DEBUG", "1. Gửi ảnh trực tiếp cho AI nhận diện...")

                val aiResult = aiRepository.predictImage(imagePart)

                aiResult.onSuccess { response ->
                    Log.d(
                        "AI_IMAGE_DEBUG",
                        "2. AI TRẢ VỀ: success=${response.success}, label='${response.label}', confidence=${response.confidence}"
                    )

                    if (response.success && response.label != null) {
                        val validKeywords = listOf(
                            "Fruit", "Vegetable", "Meat", "Seafood", "Other",
                            "rice", "grain", "coffee", "nut", "seed"  // ← Thêm keywords mới
                        )
                        val isAgricultural = validKeywords.any { keyword ->
                            response.label.contains(keyword, ignoreCase = true)
                        }
                        val isConfident = (response.confidence ?: 0.0) > 0.50

                        Log.d(
                            "AI_IMAGE_DEBUG",
                            "3. Phân tích: Có từ khóa = $isAgricultural | Đủ độ tin cậy = $isConfident"
                        )

                        if (isAgricultural && isConfident) {
                            Log.d("AI_IMAGE_DEBUG", "=> KẾT LUẬN: Ảnh hợp lệ")
                            val detectedCategory = extractCategory(response.label)
                            onResult(true, detectedCategory)
                        } else {
                            Log.d("AI_IMAGE_DEBUG", "=> KẾT LUẬN: Ảnh rác")
                            onResult(false, null)
                        }
                    } else {
                        Log.e("AI_IMAGE_DEBUG", "=> LỖI: AI trả về label null hoặc success = false")
                        onResult(false, null)
                    }
                }.onFailure { error ->
                    Log.e("AI_IMAGE_DEBUG", "=> LỖI MẠNG: ${error.localizedMessage}")
                    onResult(false, null)
                }
            } catch (e: Exception) {
                Log.e("AI_IMAGE_DEBUG", "=> CRASH: ${e.localizedMessage}")
                onResult(false, null)
            }
        }
    }

    private fun extractCategory(label: String): String {
        return when {
            label.contains("Fruit", ignoreCase = true) -> "Trái cây"
            label.contains("Vegetable", ignoreCase = true) -> "Rau củ"
            label.contains("Meat", ignoreCase = true) -> "Thịt"
            label.contains("Seafood", ignoreCase = true) -> "Thủy hải sản"
            label.contains("rice", ignoreCase = true) -> "Sản phẩm khác"
            label.contains("grain", ignoreCase = true) -> "Sản phẩm khác"
            label.contains("coffee", ignoreCase = true) -> "Sản phẩm khác"
            label.contains("nut", ignoreCase = true) -> "Sản phẩm khác"
            label.contains("seed", ignoreCase = true) -> "Sản phẩm khác"
            else -> "Sản phẩm khác"
        }
    }

    fun getProductReviews(productId: String) {
        viewModelScope.launch {
            val result = reviewRepository.getReviewsByProductId(productId)
            result.onSuccess { reviewList ->
                Log.d("DEBUG_ANH_REVIEW", "Tổng số đánh giá: ${reviewList.size}")
                reviewList.forEachIndexed { index, review ->
                    Log.d("DEBUG_ANH_REVIEW", "Review [$index] - Tên: ${review.userId}")
                    Log.d("DEBUG_ANH_REVIEW", "Review [$index] - List ảnh: ${review.imageUrls}")
                    Log.d("DEBUG_ANH_REVIEW", "Review [$index] - Size mảng ảnh: ${review.imageUrls?.size}")
                }
                _reviews.value = reviewList
            }.onFailure { e ->
                _reviews.value = emptyList()
            }
        }
    }

    fun getReviewSummary(productId: String) {
        viewModelScope.launch {
            val result = reviewRepository.getReviewSummaryByProductId(productId)
            result.onSuccess { summary ->
                _reviewSummary.value = summary
                Log.d("DEBUG_REVIEW_SUMMARY", "Lấy thống kê thành công: $summary")
            }.onFailure { e ->
                _reviewSummary.value = null
                Log.e("DEBUG_REVIEW_SUMMARY", "Lỗi lấy thống kê: ${e.message}")
            }
        }
    }

    fun generateDescriptionStream(
        productName: String,
        category: String = "",
        characteristics: String = "",
        onToken: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ): Job {
        return viewModelScope.launch {
            try {
                val request = GenerateDescriptionRequest(
                    productName = productName,
                    category = category.trim(),
                    characteristics = characteristics.trim()
                )

                aiRepository.generateDescriptionStream(request)
                    .collect { token ->
                        onToken(token)
                    }

                onComplete()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Lỗi tạo mô tả")
            }
        }
    }
}
