package com.example.agritech_mobile.ui.dashboard

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.dto.EditableProductResponse
import com.example.agritech_mobile.data.remote.dto.ErrorResponse
import com.example.agritech_mobile.data.remote.dto.GenerateDescriptionRequest
import com.example.agritech_mobile.data.remote.dto.UpdateMyProductRequest
import com.example.agritech_mobile.data.repository.AiRepository
import com.example.agritech_mobile.data.repository.ProductRepository
import com.example.agritech_mobile.data.repository.UploadRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File
import java.math.BigDecimal
import javax.inject.Inject

val editableProductCategories = listOf("Rau củ", "Trái cây", "Thịt", "Thủy hải sản", "Sản phẩm khác")

private fun canonicalCategory(value: String) = mapOf(
    "Vegetables" to "Rau củ", "Fruits" to "Trái cây", "Meat" to "Thịt",
    "Seafood" to "Thủy hải sản", "Others" to "Sản phẩm khác"
)[value] ?: value

private fun numberInput(value: Double) = if (value.isFinite())
    BigDecimal.valueOf(value).stripTrailingZeros().toPlainString() else ""

fun parseEditNumber(input: String): Double? = input.trim().replace(',', '.').toDoubleOrNull()?.takeIf { it.isFinite() }

data class ProductEditUiState(
    val original: EditableProductResponse? = null,
    val name: String = "",
    val category: String = "",
    val price: String = "",
    val costPrice: String = "",
    val quantity: String = "",
    val unit: String = "",
    val description: String = "",
    val characteristics: String = "",
    val isGeneratingDescription: Boolean = false,
    val descriptionError: String? = null,
    val imageUrls: List<String> = emptyList(),
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploading: Boolean = false,
    val saved: Boolean = false,
    val error: String? = null,
    val fieldErrors: Map<String, String> = emptyMap(),
    val stockConflict: Boolean = false
) {
    val isBusy get() = isLoading || isSaving || isUploading || isGeneratingDescription
    val hasChanges get() = original?.let {
        name.trim() != it.name || category != canonicalCategory(it.category) ||
            parseEditNumber(price) != it.price || parseEditNumber(quantity) != it.quantity ||
            parseEditNumber(costPrice) != it.costPrice ||
            (costPrice.isNotBlank() && parseEditNumber(costPrice) == null) || unit.trim() != it.unit ||
            description.trim() != it.description || imageUrls != it.imageUrls
    } ?: false
}

@HiltViewModel
class EditProductViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val products: ProductRepository,
    private val uploads: UploadRepository,
    private val ai: AiRepository
) : ViewModel() {
    private val productId = savedStateHandle.get<String>("productId").orEmpty()
    private val _uiState = MutableStateFlow(ProductEditUiState())
    val uiState = _uiState.asStateFlow()

    init { load() }

    fun load() {
        if (_uiState.value.isSaving || _uiState.value.isUploading || _uiState.value.isGeneratingDescription) return
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            products.getEditableProduct(productId).onSuccess { product ->
                _uiState.value = ProductEditUiState(
                    original = product, name = product.name, category = canonicalCategory(product.category),
                    price = numberInput(product.price), costPrice = product.costPrice?.let(::numberInput).orEmpty(),
                    quantity = numberInput(product.quantity), unit = product.unit,
                    description = product.description, imageUrls = product.imageUrls, isLoading = false
                )
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false, error = requestError(error)) }
            }
        }
    }

    fun change(field: String, value: String) {
        if (_uiState.value.isBusy) return
        _uiState.update { state ->
            val changed = when (field) {
                "name" -> state.copy(name = value)
                "category" -> state.copy(category = value)
                "price" -> state.copy(price = value)
                "costPrice" -> state.copy(costPrice = value)
                "quantity" -> state.copy(quantity = value)
                "unit" -> state.copy(unit = value)
                "description" -> state.copy(description = value)
                "characteristics" -> if (value.length <= 1000) state.copy(characteristics = value, descriptionError = null) else state
                else -> state
            }
            changed.copy(fieldErrors = state.fieldErrors - field, error = state.error.takeIf { state.stockConflict })
        }
    }

    fun removeImage(index: Int) {
        if (_uiState.value.isBusy) return
        _uiState.update { state ->
            state.copy(imageUrls = state.imageUrls.filterIndexed { position, _ -> position != index },
                fieldErrors = state.fieldErrors - "images", error = state.error.takeIf { state.stockConflict })
        }
    }

    fun generateDescription() {
        val current = _uiState.value
        if (current.isBusy || current.saved) return
        if (current.name.isBlank() || current.category.isBlank() || current.characteristics.isBlank()) {
            _uiState.update { it.copy(descriptionError = "Nhập tên, danh mục và đặc điểm sản phẩm trước khi tạo mô tả.") }
            return
        }
        _uiState.update { it.copy(isGeneratingDescription = true, descriptionError = null) }
        viewModelScope.launch {
            try {
                val generated = StringBuilder()
                ai.generateDescriptionStream(GenerateDescriptionRequest(
                    productName = current.name.trim(), category = current.category,
                    characteristics = current.characteristics.trim()
                )).collect { generated.append(it.replace("*", "")) }
                currentCoroutineContext().ensureActive()
                val description = generated.toString().trim()
                check(description.isNotBlank()) { "AI chưa trả về mô tả. Vui lòng thử lại." }
                _uiState.update { state ->
                    if (state.name != current.name || state.category != current.category ||
                        state.characteristics != current.characteristics) {
                        state.copy(descriptionError = "Thông tin đã thay đổi. Vui lòng tạo lại mô tả.")
                    } else state.copy(description = description, fieldErrors = state.fieldErrors - "description")
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(descriptionError = "Không tạo được mô tả. Kiểm tra đặc điểm sản phẩm và thử lại.") }
            } finally {
                _uiState.update { it.copy(isGeneratingDescription = false) }
            }
        }
    }

    fun addImages(context: Context, uris: List<Uri>) {
        val current = _uiState.value
        if (current.isBusy || uris.isEmpty() || current.imageUrls.size >= 5) return
        val selected = uris.distinct().take(5 - current.imageUrls.size)
        _uiState.update { state -> state.copy(isUploading = true, error = state.error.takeIf { state.stockConflict }) }
        viewModelScope.launch {
            try {
                for (uri in selected) {
                    val file = withContext(Dispatchers.IO) {
                        val target = File.createTempFile("product_edit_", ".jpg", context.cacheDir)
                        try {
                            context.contentResolver.openInputStream(uri)?.use { input ->
                                target.outputStream().use { output -> input.copyTo(output) }
                            } ?: error("Không đọc được ảnh đã chọn")
                            target
                        } catch (e: Exception) {
                            target.delete()
                            throw e
                        }
                    }
                    try {
                        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
                        val part = MultipartBody.Part.createFormData("file", file.name,
                            file.asRequestBody(mime.toMediaTypeOrNull()))
                        val prediction = ai.predictImage(part).getOrThrow()
                        currentCoroutineContext().ensureActive()
                        val label = prediction.label.orEmpty()
                        val agricultural = listOf("Fruit", "Vegetable", "Meat", "Seafood", "Other",
                            "rice", "grain", "coffee", "nut", "seed").any { label.contains(it, ignoreCase = true) }
                        check(prediction.success && agricultural && (prediction.confidence ?: 0.0) > 0.5) {
                            "Ảnh chưa nhận diện được là nông sản. Vui lòng chọn ảnh rõ hơn."
                        }
                        val urls = uploads.uploadImages(listOf(part)).getOrThrow()
                        currentCoroutineContext().ensureActive()
                        check(urls.size == 1 && urls.first().isNotBlank()) { "Không tải được ảnh sản phẩm" }
                        _uiState.update { it.copy(imageUrls = it.imageUrls + urls.first(), fieldErrors = it.fieldErrors - "images") }
                    } finally {
                        file.delete()
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = if (e is IllegalStateException) e.message else "Không tải được ảnh. Vui lòng thử lại.") }
            } finally {
                _uiState.update { it.copy(isUploading = false) }
            }
        }
    }

    fun refreshStock() {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            products.getEditableProduct(productId).onSuccess { fresh ->
                _uiState.update { it.copy(original = it.original?.copy(quantity = fresh.quantity),
                    quantity = numberInput(fresh.quantity), isSaving = false, stockConflict = false,
                    error = null, fieldErrors = it.fieldErrors - "quantity") }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, error = requestError(error)) }
            }
        }
    }

    fun save() {
        val current = _uiState.value
        val original = current.original ?: return
        if (current.isBusy || !current.hasChanges || current.stockConflict || current.saved) return
        val price = parseEditNumber(current.price)
        val cost = parseEditNumber(current.costPrice)
        val quantity = parseEditNumber(current.quantity)
        val errors = buildMap {
            if (current.name.isBlank() || current.name.trim().length > 255) put("name", "Nhập tên sản phẩm, tối đa 255 ký tự")
            if (current.category !in editableProductCategories) put("category", "Vui lòng chọn danh mục")
            if (current.unit.isBlank() || current.unit.trim().length > 255) put("unit", "Nhập đơn vị tính, tối đa 255 ký tự")
            if (price == null || price <= 0.0) put("price", "Giá bán phải lớn hơn 0")
            if (current.costPrice.isNotBlank() && (cost == null || cost < 0.0)) put("costPrice", "Giá vốn phải là số không âm")
            if (quantity == null || quantity < 0.0) put("quantity", "Tồn kho phải là số không âm")
            if (current.description.length > 10000) put("description", "Mô tả tối đa 10.000 ký tự")
            if (current.imageUrls.size !in 1..5) put("images", "Vui lòng chọn từ 1 đến 5 ảnh")
        }
        if (errors.isNotEmpty()) {
            _uiState.update { it.copy(fieldErrors = errors, error = "Kiểm tra lại các thông tin được đánh dấu bên dưới.") }
            return
        }
        _uiState.update { it.copy(isSaving = true, error = null, fieldErrors = emptyMap()) }
        viewModelScope.launch {
            val changedStock = quantity != original.quantity
            val request = UpdateMyProductRequest(
                name = current.name.trim(), category = current.category, price = requireNotNull(price),
                unit = current.unit.trim(), description = current.description.trim(), imageUrls = current.imageUrls,
                costPrice = cost, quantity = quantity.takeIf { changedStock },
                expectedQuantity = original.quantity.takeIf { changedStock }
            )
            products.updateMyProduct(productId, request).onSuccess {
                _uiState.update { it.copy(isSaving = false, saved = true) }
            }.onFailure { error ->
                _uiState.update { it.copy(isSaving = false, error = requestError(error),
                    stockConflict = error is HttpException && error.code() == 409) }
            }
        }
    }

    private fun requestError(error: Throwable): String {
        if (error is HttpException) {
            val message = runCatching {
                Gson().fromJson(error.response()?.errorBody()?.string(), ErrorResponse::class.java)?.message
            }.getOrNull()
            return message?.takeIf { it.isNotBlank() } ?: when (error.code()) {
                401 -> "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."
                403 -> "Bạn chỉ được sửa sản phẩm của mình."
                404 -> "Sản phẩm không còn tồn tại."
                409 -> "Tồn kho đã thay đổi. Vui lòng đọc lại tồn kho trước khi lưu."
                else -> "Không lưu được sản phẩm. Vui lòng thử lại."
            }
        }
        return "Không kết nối được máy chủ. Vui lòng thử lại."
    }
}
