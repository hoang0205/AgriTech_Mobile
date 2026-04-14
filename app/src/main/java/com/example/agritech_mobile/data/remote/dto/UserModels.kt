package com.example.agritech_mobile.data.remote.dto

data class UpdateProfileRequest(
    val fullName: String,
    val avatarUrl: String,
)

data class UpdateProfileResponse(
    val message: String,
    val success: Boolean = true
)