package com.example.agritech_mobile.data.remote.dto

data class ShippingDetails(
    val id: String,
    val receiverName: String,
    val phoneNumber: String,
    val addressDetail: String,
    val isDefault: Boolean
)

data class AddressRequest(
    val receiverName: String,
    val receiverPhone: String,
    val detail: String,
    val isDefault: Boolean
)

data class ShippingAddressResponce(
    val message: String,
    val success: Boolean,
)

data class ProvinceResponse(
    val code: String,
    val name: String,
    val type: String
)

data class WardResponse(
    val code: String,
    val name: String
)