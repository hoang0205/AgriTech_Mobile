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