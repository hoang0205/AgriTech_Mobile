package com.example.agritech_mobile.data.repository

import android.util.Log
import com.example.agritech_mobile.data.remote.AuthApiService
import com.example.agritech_mobile.data.remote.dto.ErrorResponse
import com.example.agritech_mobile.data.remote.dto.ForgotPasswordRequest
import com.example.agritech_mobile.data.remote.dto.LoginRequest
import com.example.agritech_mobile.data.remote.dto.LoginResponse
import com.example.agritech_mobile.data.remote.dto.MessageResponse
import com.example.agritech_mobile.data.remote.dto.RegisterRequest
import com.example.agritech_mobile.data.remote.dto.RegisterResponse
import com.example.agritech_mobile.data.remote.dto.ResetPasswordRequest
import com.example.agritech_mobile.data.remote.dto.VerifyEmail
import okhttp3.Response
import javax.inject.Inject
import com.google.gson.Gson

class AuthRepository @Inject constructor(
    private val apiService: AuthApiService
) {
    suspend fun login(phone: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(phone, password))

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorJson = response.errorBody()?.string()
                val errorObj = try {
                    Gson().fromJson(errorJson, ErrorResponse::class.java)
                } catch (e: Exception) { null }

                val errorMessage = errorObj?.message ?: "Có lỗi xảy ra, vui lòng thử lại!"
                Log.d("AuthRepository", "Error response: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun register(
        fullName: String,
        phone: String,
        email: String,
        password: String
    ): Result<RegisterResponse> {
        return try {
            val response = apiService.register(RegisterRequest(fullName, phone, email, password))

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorJson = response.errorBody()?.string()
                val errorObj = try {
                    Gson().fromJson(errorJson, ErrorResponse::class.java)
                } catch (e: Exception) { null }

                val errorMessage = errorObj?.message ?: "Có lỗi xảy ra, vui lòng thử lại!"
                Log.d("AuthRepository", "Error response: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun requestResetPassword(
        email: String
    ): Result<MessageResponse> {
        return try {
            val response = apiService.forgotPassword(ForgotPasswordRequest(email))

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                val errorJson = response.errorBody()?.string()
                val errorObj = Gson().fromJson(errorJson, ErrorResponse::class.java)
                val errorMessage = errorObj?.message ?: "Có lỗi xảy ra, vui lòng thử lại!"

                Log.d("AuthRepository", "Error response: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun verifyEmail(
        email: String,
        otp: String
    ): Result<MessageResponse> {
        return try {
            val response = apiService.verifyEmail(VerifyEmail(email, otp))

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                val errorJson = response.errorBody()?.string()
                val errorObj = Gson().fromJson(errorJson, ErrorResponse::class.java)
                val errorMessage = errorObj?.message ?: "Có lỗi xảy ra, vui lòng thử lại!"

                Log.d("AuthRepository", "Error response: $errorMessage")
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun resetPassword(
        email: String,
        newPassword: String
    ): Result<MessageResponse> {
        return try {
            val response = apiService.resetPassword(ResetPasswordRequest(email, newPassword))

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                val errorJson = response.errorBody()?.string()
                val errorObj = Gson().fromJson(errorJson, ErrorResponse::class.java)
                val errorMessage = errorObj?.message ?: "Có lỗi xảy ra, vui lòng thử lại!"

                Log.d("AuthRepository", "Error response: $errorMessage")
                Result.failure(Exception(errorMessage))

            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }
}