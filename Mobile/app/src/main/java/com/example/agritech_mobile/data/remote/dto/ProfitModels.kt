package com.example.agritech_mobile.data.remote.dto
data class ProductCostResponse(
    val productId: String,
    val productName: String,
    val unit: String,
    val sellingPrice: Double,
    val costPrice: Double?,
    val quantity: Double
)

data class UpdateProductCostRequest(
    val costPrice: Double
)