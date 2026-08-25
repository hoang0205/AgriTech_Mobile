package com.example.agritech_mobile.ui.user

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.repository.ReviewRepository
import com.example.agritech_mobile.data.repository.UploadRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

sealed class ReviewState {
    object Idle : ReviewState()
    object Loading : ReviewState()
    data class Success(val message: String) : ReviewState()
    data class Error(val message: String) : ReviewState()
}

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val reviewRepository: ReviewRepository,
    private val uploadRepository: UploadRepository
) : ViewModel() {

    private val _reviewState = MutableStateFlow<ReviewState>(ReviewState.Idle)
    val reviewState: StateFlow<ReviewState> = _reviewState.asStateFlow()

    fun submitReview(
        productId: String,
        rating: Int,
        comment: String,
        imagePart: MultipartBody.Part?
    ) {
        _reviewState.value = ReviewState.Loading
        viewModelScope.launch {
            try {
                val imageUrls = mutableListOf<String>()

                if (imagePart != null) {
                    val uploadResult = uploadRepository.uploadImages(listOf(imagePart))

                    if (uploadResult.isSuccess) {
                        val urls = uploadResult.getOrNull()
                        if (!urls.isNullOrEmpty()) {
                            imageUrls.addAll(urls)
                        }
                    } else {
                        val errorMsg = uploadResult.exceptionOrNull()?.message
                        _reviewState.value = ReviewState.Error("Lỗi tải ảnh lên server")
                        return@launch
                    }
                } else {
                }

                val result = reviewRepository.createReview(productId, rating, comment, imageUrls)

                result.onSuccess {
                    _reviewState.value = ReviewState.Success("Đánh giá sản phẩm thành công!")
                }.onFailure { exception ->
                    _reviewState.value = ReviewState.Error(exception.message ?: "Lỗi gửi đánh giá")
                }
            } catch (e: Exception) {
                _reviewState.value = ReviewState.Error("Lỗi hệ thống: ${e.message}")
            }
        }
    }



    fun clearReviewState() {
        _reviewState.value = ReviewState.Idle
    }
}