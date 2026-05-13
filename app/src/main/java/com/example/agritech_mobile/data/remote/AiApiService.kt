package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.PredictImageRequest
import com.example.agritech_mobile.data.remote.dto.PredictImageResponse
import com.example.agritech_mobile.data.remote.dto.PricePredictionRequest
import com.example.agritech_mobile.data.remote.dto.PricePredictionResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface AiApiService {
    @Multipart
    @POST("api/predict-image")
    suspend fun predictImage(@Part file: MultipartBody.Part): Response<PredictImageResponse>

    @POST("api/predict-price")
    suspend fun predictPrice(
        @Body request: PricePredictionRequest
    ): Response<PricePredictionResponse>
}