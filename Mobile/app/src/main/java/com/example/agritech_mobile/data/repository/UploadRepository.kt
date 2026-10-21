package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.UploadApiService
import okhttp3.MultipartBody
import javax.inject.Inject

class UploadRepository @Inject constructor(
    private val apiService: UploadApiService
)  {
    suspend fun uploadImages(imageParts: List<MultipartBody.Part>): Result<List<String>> {
        return try {
            val response = apiService.uploadImages(imageParts)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.localizedMessage}"))
        }
    }
}