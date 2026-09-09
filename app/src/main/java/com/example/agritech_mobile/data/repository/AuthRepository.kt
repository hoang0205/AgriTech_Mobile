package com.example.agritech_mobile.data.repository

import android.util.Log
import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.remote.AuthApiService
import com.example.agritech_mobile.data.remote.NotificationApiService
import com.example.agritech_mobile.data.remote.dto.ErrorResponse
import com.example.agritech_mobile.data.remote.dto.FcmTokenRequest
import com.example.agritech_mobile.data.remote.dto.ForgotPasswordRequest
import com.example.agritech_mobile.data.remote.dto.LoginRequest
import com.example.agritech_mobile.data.remote.dto.LoginResponse
import com.example.agritech_mobile.data.remote.dto.LogoutRequest
import com.example.agritech_mobile.data.remote.dto.MessageResponse
import com.example.agritech_mobile.data.remote.dto.RegisterRequest
import com.example.agritech_mobile.data.remote.dto.RegisterResponse
import com.example.agritech_mobile.data.remote.dto.ResetPasswordRequest
import com.example.agritech_mobile.data.remote.dto.VerifyEmail
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val apiService: AuthApiService,
    private val notificationApiService: NotificationApiService,
    private val tokenManager: TokenManager
) {
    suspend fun login(phone: String, password: String): Result<LoginResponse> {
        return try {
            val response = apiService.login(LoginRequest(phone, password))

            if (response.isSuccessful && response.body() != null) {
                val loginResponse = response.body()!!

                tokenManager.saveTokens(loginResponse.accessToken, loginResponse.refreshToken)

                loginResponse.firebaseToken?.let { token ->
                    try {
                        FirebaseAuth.getInstance().signInWithCustomToken(token).await()
                        Log.d("AuthRepository", "Firebase Auth thành công: ${FirebaseAuth.getInstance().currentUser?.uid}")
                    } catch (e: Exception) {
                        Log.e("AuthRepository", "Firebase Auth thất bại: ${e.localizedMessage}")
                    }
                }

                try {
                    val fcmToken = FirebaseMessaging.getInstance().token.await()
                    notificationApiService.updateFcmToken(FcmTokenRequest(fcmToken))
                    Log.d("AuthRepository", "Đã cập nhật FCM Token lên server thành công: $fcmToken")
                } catch (e: Exception) {
                    Log.e("AuthRepository", "Cập nhật FCM Token thất bại: ${e.localizedMessage}")
                }

                Result.success(loginResponse)
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

    suspend fun logout(accessToken: String): Result<MessageResponse> {
        return try {
            FirebaseAuth.getInstance().signOut()

            val request = LogoutRequest(accessToken)
            val response = apiService.logout(request)

            tokenManager.clearTokens()

            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Lỗi đăng xuất từ Server: ${response.code()}"))
            }
        } catch (e: Exception) {
            FirebaseAuth.getInstance().signOut()
            tokenManager.clearTokens()
            Result.failure(Exception("Lỗi kết nối: ${e.localizedMessage}"))
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

    suspend fun requestResetPassword(email: String): Result<MessageResponse> {
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

    suspend fun verifyEmail(email: String, otp: String): Result<MessageResponse> {
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

    suspend fun resetPassword(email: String, newPassword: String): Result<MessageResponse> {
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