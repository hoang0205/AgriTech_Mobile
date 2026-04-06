package com.example.agritech_mobile.data.remote.dto


data class CartResponse(
    val cartItemId: String,
    val productId: String,
    val productName: String,
    val price: Double,
    val quantity: Double,
    val thumbnail: String,
    val unit: String,
    val totalPrice: Double,
    val farmerId: String,
    val farmerName: String,
    val farmerPhone: String,
)

data class CartRequest(
    val productId: String,
    val quantity: Double
)

data class CartQuantityRequest(
    val quantity: Double
)

data class CartMessageResponse(
    val message: String
)