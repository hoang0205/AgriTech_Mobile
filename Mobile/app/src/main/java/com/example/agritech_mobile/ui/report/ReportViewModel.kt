package com.example.agritech_mobile.ui.report

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.dto.CreateProductReportRequest
import com.example.agritech_mobile.data.remote.dto.ProductReportResponse
import com.example.agritech_mobile.data.remote.dto.ReportReason
import com.example.agritech_mobile.data.repository.ReportRepository
import com.example.agritech_mobile.data.repository.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject

data class ReportUiState(
    val isSubmitting: Boolean = false,
    val submittedReport: ProductReportResponse? = null,
    val error: String? = null
)

@HiltViewModel
class ReportProductViewModel @Inject constructor(
    private val reportRepository: ReportRepository,
    private val uploadRepository: UploadRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()

    fun submit(
        productId: String,
        reason: ReportReason?,
        description: String,
        imageUris: List<Uri>
    ) {
        if (_uiState.value.isSubmitting ||
            _uiState.value.submittedReport != null
        ) {
            return
        }

        val cleanDescription = description.trim()

        val validationError = when {
            productId.isBlank() ->
                "Sản phẩm không hợp lệ"

            reason == null ->
                "Vui lòng chọn lý do báo cáo"

            cleanDescription.length !in 10..2000 ->
                "Mô tả phải có từ 10 đến 2000 ký tự"

            imageUris.size > 3 ->
                "Chỉ được đính kèm tối đa 3 ảnh"

            else -> null
        }

        if (validationError != null) {
            _uiState.value = ReportUiState(error = validationError)
            return
        }

        val selectedReason = reason ?: return

        val selectedImages = imageUris.toList()
        _uiState.value = ReportUiState(isSubmitting = true)

        viewModelScope.launch {
            try {
                val eligibility = reportRepository
                    .checkEligibility(productId)
                    .getOrThrow()

                if (!eligibility.canReport) {
                    _uiState.value = ReportUiState(
                        error = eligibility.message
                            ?: "Bạn không thể báo cáo sản phẩm này"
                    )
                    return@launch
                }

                val imageUrls = uploadEvidence(selectedImages)

                val request = CreateProductReportRequest(
                    productId = productId,
                    reason = selectedReason,
                    description = cleanDescription,
                    imageUrls = imageUrls
                )

                val report = reportRepository
                    .createProductReport(request)
                    .getOrThrow()

                _uiState.value = ReportUiState(
                    submittedReport = report
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = ReportUiState(
                    error = e.message ?: "Không gửi được báo cáo"
                )
            }
        }
    }

    private suspend fun uploadEvidence(
        imageUris: List<Uri>
    ): List<String> = withContext(Dispatchers.IO) {
        if (imageUris.isEmpty()) {
            return@withContext emptyList()
        }

        val temporaryFiles = mutableListOf<File>()

        try {
            val parts = imageUris.map { uri ->
                val mimeType = context.contentResolver.getType(uri)
                    ?: throw IllegalArgumentException(
                        "Không nhận diện được ảnh. Vui lòng chọn lại."
                    )

                require(mimeType.startsWith("image/")) {
                    "File được chọn phải là ảnh"
                }

                val file = File.createTempFile(
                    "report_",
                    ".img",
                    context.cacheDir
                )
                temporaryFiles.add(file)

                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException(
                        "Không đọc được ảnh. Vui lòng chọn lại."
                    )

                input.use { source ->
                    file.outputStream().use { destination ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        var totalBytes = 0L

                        while (true) {
                            currentCoroutineContext().ensureActive()

                            val count = source.read(buffer)
                            if (count == -1) break

                            totalBytes += count
                            require(totalBytes <= 10L * 1024 * 1024) {
                                "Mỗi ảnh không được lớn hơn 10 MB"
                            }

                            destination.write(buffer, 0, count)
                        }

                        require(totalBytes > 0) {
                            "Ảnh được chọn không có dữ liệu"
                        }
                    }
                }

                MultipartBody.Part.createFormData(
                    "files",
                    file.name,
                    file.asRequestBody(mimeType.toMediaTypeOrNull())
                )
            }

            val uploadResult = uploadRepository.uploadImages(parts)

            currentCoroutineContext().ensureActive()

            val urls = uploadResult.getOrThrow()

            require(
                urls.size == parts.size &&
                        urls.all { it.startsWith("https://") }
            ) {
                "Máy chủ chưa trả đủ ảnh đã tải lên"
            }

            urls
        } finally {
            temporaryFiles.forEach { it.delete() }
        }
    }
}