package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.CartApiService
import com.example.agritech_mobile.data.remote.dto.CartMessageResponse
import com.example.agritech_mobile.data.remote.dto.CartQuantityRequest
import com.example.agritech_mobile.data.remote.dto.CartRequest
import com.example.agritech_mobile.data.remote.dto.CartResponse
import javax.inject.Inject

class CartRepository @Inject constructor(
    private val cartApiService: CartApiService
) {
    suspend fun getCartItems(): Result<List<CartResponse>> {
        return try {
            val response = cartApiService.getCartItems()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun addToCart(productId: String, quantity: Double): Result<CartMessageResponse> {
        return try {
            val request = CartRequest(productId, quantity)
            val response = cartApiService.addToCart(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                try {
                    val errorBody = response.errorBody()?.string()
                    val errorMessage = if (!errorBody.isNullOrEmpty()) {
                        try {
                            val jsonObject = org.json.JSONObject(errorBody)
                            jsonObject.optString("message", "Lỗi từ server: ${response.code()}")
                        } catch (e: Exception) {
                            errorBody
                        }
                    } else {
                        "Lỗi từ server: ${response.code()}"
                    }
                    Result.failure(Exception(errorMessage))
                } catch (e: Exception) {
                    Result.failure(Exception("Lỗi từ server: ${response.code()}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }


    suspend fun updateQuantity(cartItemId: String, quantity: Double): Result<CartMessageResponse> {
        return try {
            val request = CartQuantityRequest(quantity)
            val response = cartApiService.updateQuantity(cartItemId, request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun deleteCartItem(cartItemId: String): Result<CartMessageResponse> {
        return try {
            val response = cartApiService.deleteCartItem(cartItemId)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }
}
