package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.AiApiService
import com.example.agritech_mobile.data.remote.dto.PredictImageRequest
import com.example.agritech_mobile.data.remote.dto.PredictImageResponse
import com.example.agritech_mobile.data.remote.dto.PricePredictionRequest
import com.example.agritech_mobile.data.remote.dto.PricePredictionResponse
import javax.inject.Inject

class AiRepository @Inject constructor(
    private val aiApiService: AiApiService
) {
    suspend fun predictImage(imageUrl: String): Result<PredictImageResponse> {
        return try {
            val request = PredictImageRequest(imageUrl)
            val response = aiApiService.predictImage(request)
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

    suspend fun predictPrice(productName: String): Result<PricePredictionResponse> { // Sửa kiểu trả về
        return try {
            val request = PricePredictionRequest(productName)
            val response = aiApiService.predictPrice(request)
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