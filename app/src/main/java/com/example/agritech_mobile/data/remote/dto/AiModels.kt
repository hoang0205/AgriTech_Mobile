package com.example.agritech_mobile.data.remote.dto

import com.google.gson.annotations.SerializedName

data class PredictImageRequest(
    val image_url: String
)

data class PredictImageResponse(
    val success: Boolean,
    val label: String? = null,
    val confidence: Double? = null,
    val error: String? = null
)

data class PricePredictionRequest(
    @SerializedName("product_name")
    val productName: String
)

data class PricePredictionResponse(
    val success: Boolean,
    val productName: String? = null,
    val price: String? = null,
    val error: String? = null
)

data class ChatBotRequest(
    @SerializedName("query") val query: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("session_id") val sessionId: String? = null
)

data class ChatBotResponse(
    @SerializedName("status") val status: String,
    @SerializedName("session_id") val sessionId: String?,
    @SerializedName("product_name") val productName: String,
    @SerializedName("answer") val answer: String,
    @SerializedName("context_used") val contextUsed: String?
)

data class ChatHistoryMessage(
    @SerializedName("role") val role: String,
    @SerializedName("content") val content: String
)

data class ChatHistoryResponse(
    @SerializedName("status") val status: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("messages") val messages: List<ChatHistoryMessage>
)

data class GenerateDescriptionRequest(
    @SerializedName("product_name") val productName: String,
    @SerializedName("category") val category: String
)

data class GenerateDescriptionResponse(
    @SerializedName("status") val status: String,
    @SerializedName("product_name") val productName: String,
    @SerializedName("category") val category: String,
    @SerializedName("description") val description: String
)