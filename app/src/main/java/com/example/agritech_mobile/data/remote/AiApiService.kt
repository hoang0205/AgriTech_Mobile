package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.ChatBotRequest
import com.example.agritech_mobile.data.remote.dto.ChatBotResponse
import com.example.agritech_mobile.data.remote.dto.ChatHistoryResponse
import com.example.agritech_mobile.data.remote.dto.GenerateDescriptionRequest
import com.example.agritech_mobile.data.remote.dto.GenerateDescriptionResponse
import com.example.agritech_mobile.data.remote.dto.PredictImageRequest
import com.example.agritech_mobile.data.remote.dto.PredictImageResponse
import com.example.agritech_mobile.data.remote.dto.PricePredictionRequest
import com.example.agritech_mobile.data.remote.dto.PricePredictionResponse
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Streaming

interface AiApiService {
    @Multipart
    @POST("api/predict-image")
    suspend fun predictImage(@Part file: MultipartBody.Part): Response<PredictImageResponse>

    @POST("api/predict-price")
    suspend fun predictPrice(
        @Body request: PricePredictionRequest
    ): Response<PricePredictionResponse>

    @Streaming
    @POST("/api/chat/stream")
    suspend fun chatBotStream(
        @Body request: ChatBotRequest
    ): ResponseBody

    @POST("api/chat")
    suspend fun chatBot(
        @Body request: ChatBotRequest
    ): Response<ChatBotResponse>

    @GET("/api/chat/history/{sessionId}")
    suspend fun getChatHistory(
        @Path("sessionId") sessionId: String
    ): Response<ChatHistoryResponse>

    @POST("/api/generate-description")
    suspend fun generateDescription(
        @Body request: GenerateDescriptionRequest
    ): Response<GenerateDescriptionResponse>

    @Streaming
    @POST("/api/generate-description/stream")
    suspend fun generateDescriptionStream(
        @Body request: GenerateDescriptionRequest
    ): ResponseBody

}