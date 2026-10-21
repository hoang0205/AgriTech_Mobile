package com.example.agritech_mobile.data.remote.dto

data class SellerPickupResponse(
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val configured: Boolean = false
)

data class UpdateSellerPickupRequest(
    val address: String,
    val latitude: Double,
    val longitude: Double
)