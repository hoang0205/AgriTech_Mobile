package com.example.agritech_mobile.data.remote.dto

data class UpdateProfileRequest(
    val fullName: String,
    val avatarUrl: String,
)

data class UpdateProfileResponse(
    val fullName: String,
    val avatarUrl: String
)

data class UpdatePasswordRequest(
    val oldPassword: String,
    val newPassword: String,
    val confirmPassword: String
)