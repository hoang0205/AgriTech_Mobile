package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.ReviewApiService
import com.example.agritech_mobile.data.remote.dto.ReviewModelsRequest
import com.example.agritech_mobile.data.remote.dto.ReviewModelsResponse
import com.example.agritech_mobile.data.remote.dto.ReviewSummaryResponse
import javax.inject.Inject

class ReviewRepository @Inject constructor(
    private val reviewApiService: ReviewApiService
) {
    suspend fun createReview(
        productId: String,
        rating: Int,
        comment: String,
        imagesUrl: List<String>
    ) : Result<ReviewModelsResponse> {
        return try {
            val reviewRequest = ReviewModelsRequest(productId, rating, comment, imagesUrl)

            val response = reviewApiService.createReview(reviewRequest)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun getReviewsByProductId(productId: String): Result<List<ReviewModelsResponse>> {
        return try {
            val response = reviewApiService.getReviewsByProductId(productId)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body.content)
                } else {
                    Result.success(emptyList())
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun getReviewSummaryByProductId(productId: String): Result<ReviewSummaryResponse> {
        return try {
            val response = reviewApiService.getReviewSummaryByProductId(productId)

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }
}