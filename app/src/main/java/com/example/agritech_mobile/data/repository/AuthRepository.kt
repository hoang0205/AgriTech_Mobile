package com.example.agritech_mobile.data.repository

import android.util.Log
import com.example.agritech_mobile.data.remote.AuthApiService
import com.example.agritech_mobile.data.remote.dto.ErrorResponse
import com.example.agritech_mobile.data.remote.dto.LoginRequest
import com.example.agritech_mobile.data.remote.dto.LoginResponse
import com.example.agritech_mobile.data.remote.dto.RegisterRequest
import com.example.agritech_mobile.data.remote.dto.RegisterResponse
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
                val errorObj = Gson().fromJson(errorJson, ErrorResponse::class.java)
                Log.d("AuthRepository", "Error response: $errorObj")
                Result.failure(Exception(errorObj.message))

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
                val errorObj = Gson().fromJson(errorJson, ErrorResponse::class.java)
                Log.d("AuthRepository", "Error response: $errorObj")
                Result.failure(Exception(errorObj.message))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }
}