package com.example.agritech_mobile.data.remote.dto

data class LoginRequest(
    val phoneNumber: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val fullName: String,
    val avatarUrl: String,
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

data class VerifyEmail(
    val email: String,
    val otp: String
)
data class ResetPasswordRequest(
    val email: String,
    val newPassword: String
)

data class MessageResponse(
    val message: String,
    val success: Boolean = true
)

data class LogoutRequest(
    val accessToken: String,
)