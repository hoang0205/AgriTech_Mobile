package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.PredictImageRequest
import com.example.agritech_mobile.data.remote.dto.PredictImageResponse
import com.example.agritech_mobile.data.remote.dto.PricePredictionRequest
import com.example.agritech_mobile.data.remote.dto.PricePredictionResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface AiApiService {
    @POST("api/predict-image")
    suspend fun predictImage(
        @Body request: PredictImageRequest
    ): Response<PredictImageResponse>

    @POST("api/predict-price")
    suspend fun predictPrice(
        @Body request: PricePredictionRequest
    ): Response<PricePredictionResponse>
}