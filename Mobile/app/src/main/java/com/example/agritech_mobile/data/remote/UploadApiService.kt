package com.example.agritech_mobile.data.remote

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface UploadApiService {
    @Multipart
    @POST("api/upload/images")
    suspend fun uploadImages(
        @Part files: List<MultipartBody.Part>
    ): Response<List<String>>
}