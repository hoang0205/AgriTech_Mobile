package com.example.agritech_mobile.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.remote.dto.ProductResponse
import com.example.agritech_mobile.data.repository.AiRepository
import com.example.agritech_mobile.data.repository.ProductRepository
import com.example.agritech_mobile.data.repository.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: ProductRepository,
    private val uploadRepository: UploadRepository,
    private val tokenManager: TokenManager,
    private val aiRepository: AiRepository
) : ViewModel() {
    private val _dashboardState = MutableStateFlow<DashboardState>(DashboardState.Idle)
    val dashboardState: StateFlow<DashboardState> = _dashboardState.asStateFlow()

    private val _productState = MutableStateFlow<ProductResponse?>(null)
    val productState: StateFlow<ProductResponse?> = _productState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private val _textSuggestions = MutableStateFlow<List<String>>(emptyList())
    val textSuggestions: StateFlow<List<String>> = _textSuggestions.asStateFlow()

    private val _searchResults = MutableStateFlow<List<ProductResponse>>(emptyList())
    val searchResults: StateFlow<List<ProductResponse>> = _searchResults.asStateFlow()

    val userName: StateFlow<String> = tokenManager.userNameFlow
    val userAvatar: StateFlow<String> = tokenManager.avatarUrlFlow

    init {
        loadHomeData()
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isNotBlank()) {
                        val result = repository.getSuggestedProducts(query)
                        result.onSuccess { suggestions ->
                            _textSuggestions.value = suggestions
                        }.onFailure {
                            _textSuggestions.value = emptyList()
                        }
                    } else {
                        _textSuggestions.value = emptyList()
                    }
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
        _searchResults.value = emptyList()
    }

    fun executeSearch(query: String) {
        _dashboardState.value = DashboardState.Loading
        viewModelScope.launch {
            val result = repository.searchProducts(query)
            result.onSuccess { response ->
                _dashboardState.value = DashboardState.Idle
                _searchResults.value = response.content
                _textSuggestions.value = emptyList()
            }.onFailure { exception ->
                _dashboardState.value = DashboardState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun loadHomeData() {
        _dashboardState.value = DashboardState.Loading
        viewModelScope.launch {
            try {
                val newProductsDeferred = async { repository.getProducts() }
                val randomProductsDeferred = async { repository.getRecommendations() }
                val newProductsResult = newProductsDeferred.await()
                val randomProductsResult = randomProductsDeferred.await()
                val newProducts = newProductsResult.getOrNull()?.content ?: emptyList()
                val suggestedProducts = randomProductsResult.getOrNull() ?: emptyList()
                _dashboardState.value = DashboardState.HomeDataSuccess(
                    newProducts = newProducts,
                    suggestedProducts = suggestedProducts
                )
            } catch (e: Exception) {
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
        imageUrls: List<String>
    ) {
        _dashboardState.value = DashboardState.Loading
        viewModelScope.launch {
            val result = repository.createProduct(
                name,
                category,
                price,
                quantity,
                unit,
                description,
                imageUrls
            )
            result.onSuccess { response ->
                _dashboardState.value =
                    DashboardState.ActionSuccess("Đăng bán sản phẩm thành công!")
            }.onFailure { exception ->
                _dashboardState.value = DashboardState.Error(exception.message ?: "Lỗi hệ thống")
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
        imageParts: List<MultipartBody.Part>
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
                name, category, price, quantity, unit, description, uploadedUrls
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

    fun searchProducts(query: String) {
        _dashboardState.value = DashboardState.Loading
        viewModelScope.launch {
            val result = repository.searchProducts(query)
            result.onSuccess { response ->
                _dashboardState.value = DashboardState.ProductListSuccess(response.content)
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

    fun getPricePrediction(productName: String, onResult: (String?, String) -> Unit) {
        viewModelScope.launch {
            val result = aiRepository.predictPrice(productName)
            result.onSuccess { response ->
                if (response.success && response.price != null) {

                    val cleanString = response.price.replace(".", "").replace(",", "")

                    val numberRegex = "\\d+".toRegex()
                    val numbers = numberRegex.findAll(cleanString)
                        .mapNotNull { it.value.toDoubleOrNull() }
                        .toList()

                    if (numbers.size >= 2) {
                        val min = numbers[0]
                        val max = numbers[1]
                        val average = ((min + max) / 2).toLong().toString()

                        val msg = "AI gợi ý: ${response.price}. Đã tự điền mức giá trung bình!"
                        onResult(average, msg)

                    } else if (numbers.size == 1) {
                        val price = numbers[0].toLong().toString()
                        onResult(price, "AI đã gợi ý giá thành công!")

                    } else {
                        onResult(null, "AI không tìm thấy mức giá cụ thể!")
                    }
                } else {
                    onResult(null, "AI không nhận diện được giá!")
                }
            }.onFailure {
                onResult(null, "Lỗi kết nối đến máy chủ AI!")
            }
        }
    }

    fun verifyImageWithAI(imagePart: okhttp3.MultipartBody.Part, onResult: (isValid: Boolean, category: String?) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d("AI_IMAGE_DEBUG", "1. Gửi ảnh trực tiếp cho AI nhận diện...")

                val aiResult = aiRepository.predictImage(imagePart)

                aiResult.onSuccess { response ->
                    Log.d("AI_IMAGE_DEBUG", "2. AI TRẢ VỀ: success=${response.success}, label='${response.label}', confidence=${response.confidence}")

                    if (response.success && response.label != null) {
                        val validKeywords = listOf(
                            "Fruit", "Vegetable", "Meat", "Seafood", "Other",
                            "rice", "grain", "coffee", "nut", "seed"  // ← Thêm keywords mới
                        )
                        val isAgricultural = validKeywords.any { keyword ->
                            response.label.contains(keyword, ignoreCase = true)
                        }
                        val isConfident = (response.confidence ?: 0.0) > 0.50

                        Log.d("AI_IMAGE_DEBUG", "3. Phân tích: Có từ khóa = $isAgricultural | Đủ độ tin cậy = $isConfident")

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
}