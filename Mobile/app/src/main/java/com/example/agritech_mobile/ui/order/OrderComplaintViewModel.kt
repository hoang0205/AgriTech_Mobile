package com.example.agritech_mobile.ui.order

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.OrderApiService
import com.example.agritech_mobile.data.remote.dto.*
import com.example.agritech_mobile.data.repository.UploadRepository
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Response
import java.io.ByteArrayOutputStream

data class OrderComplaintUiState(val loading: Boolean = true, val busy: Boolean = false,
    val context: OrderComplaintContext? = null, val error: String? = null, val submitted: Boolean = false)

@HiltViewModel
class OrderComplaintViewModel @Inject constructor(private val api: OrderApiService,
    private val uploads: UploadRepository, @ApplicationContext private val appContext: Context) : ViewModel() {
    private val _state = MutableStateFlow(OrderComplaintUiState())
    val state = _state.asStateFlow()
    private var job: Job? = null

    fun load(id: Long) {
        if (_state.value.busy) return
        job?.cancel()
        _state.value = OrderComplaintUiState()
        job = viewModelScope.launch {
            try { _state.value = OrderComplaintUiState(loading = false, context = body(api.getOrderComplaint(id))) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.value = OrderComplaintUiState(loading = false, error = e.message ?: "Không tải được khiếu nại.") }
        }
    }

    fun submit(id: Long, reason: String, description: String, images: List<Uri>) {
        if (_state.value.busy || _state.value.loading || _state.value.context?.canCreate != true) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                require(description.trim().length in 10..2000) { "Mô tả cần từ 10 đến 2000 ký tự." }
                val urls = upload(images)
                val result = body(api.createOrderComplaint(id, CreateOrderComplaintRequest(reason, description.trim(), urls)))
                _state.update { it.copy(context = OrderComplaintContext(result, false), submitted = true) }
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { _state.update { it.copy(error = e.message ?: "Không gửi được khiếu nại. Vui lòng tải lại đơn trước khi thử lại.") } }
            finally { _state.update { it.copy(busy = false) } }
        }
    }

    private suspend fun upload(images: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        require(images.size <= 3)
        if (images.isEmpty()) return@withContext emptyList()
        val parts = images.mapIndexed { index, uri ->
            val type = appContext.contentResolver.getType(uri).orEmpty()
            require(type.startsWith("image/")) { "Vui lòng chọn ảnh hợp lệ." }
            val bytes = appContext.contentResolver.openInputStream(uri)?.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= 5 * 1024 * 1024) { "Mỗi ảnh không được vượt quá 5 MB." }
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            } ?: error("Không đọc được ảnh. Vui lòng chọn lại.")
            MultipartBody.Part.createFormData("files", "evidence_$index.img", bytes.toRequestBody(type.toMediaType()))
        }
        val urls = uploads.uploadImages(parts).getOrThrow()
        require(urls.size == images.size) { "Chưa tải đủ ảnh. Vui lòng thử lại." }
        urls
    }

    private fun <T> body(response: Response<T>): T {
        if (response.isSuccessful) return response.body() ?: error("Máy chủ trả kết quả rỗng.")
        val text = runCatching { Gson().fromJson(response.errorBody()?.string(), ErrorResponse::class.java)?.message }.getOrNull()
        error(text ?: "Không xử lý được yêu cầu. Vui lòng tải lại và thử lại.")
    }
}
