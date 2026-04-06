package com.example.agritech_mobile.data.remote.dto

data class CheckOutRequest(
    val shippingAddress: String,
    val phoneNumber: String,
    val selectedCartItemIds: List<String>
)

data class StatusRequest(
    val status: String
)