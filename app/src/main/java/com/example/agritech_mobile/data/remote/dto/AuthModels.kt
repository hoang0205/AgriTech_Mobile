package com.example.agritech_mobile.data.remote.dto

import com.example.agritech_mobile.ui.auth.AuthState

data class LoginRequest(
    val phoneNumber: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String
)

data class RegisterResponse(
    val message: String,
    val success: Boolean = true
)

data class RefreshTokenRequest(
    val refreshToken: String
)

data class RegisterRequest(
    val fullName: String,
    val phoneNumber: String,
    val email: String,
    val password: String,
    val role: String = "FARMER"
)

data class ForgotPasswordRequest(
    val email: String
)

data class ResetPasswordRequest(
    val email: String,
    val otp: String,
    val newPassword: String
)
