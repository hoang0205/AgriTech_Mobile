package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.OrderApiService
import com.example.agritech_mobile.data.remote.dto.CheckOutRequest
import com.example.agritech_mobile.data.remote.dto.OrderBuyerResponse
import com.example.agritech_mobile.data.remote.dto.OrderMessageResponse
import com.example.agritech_mobile.data.remote.dto.OrderResponse
import com.example.agritech_mobile.data.remote.dto.StatusRequest
import okhttp3.Address
import javax.inject.Inject

class OrderRepository @Inject constructor(
    private val orderApiService: OrderApiService
) {
    suspend fun getSellerOrders(): Result<List<OrderResponse>> {
        return try {
            val response = orderApiService.getSellerOrders()
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

    suspend fun checkout(
        shippingAddress: String,
        phoneNumber: String,
        selectedCartItemIds: List<String>
    ): Result<OrderMessageResponse> {
        return try {
            val request = CheckOutRequest(shippingAddress, phoneNumber, selectedCartItemIds)
            val response = orderApiService.checkout(request)
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

    suspend fun updateOrderStatus(orderId: String, status: String): Result<OrderMessageResponse> {
        return try {
            val request = StatusRequest(status)
            val response = orderApiService.updateOrderStatus(orderId, request)
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

    suspend fun getBuyerOrders(): Result<List<OrderBuyerResponse>> {
        return try {
            val response = orderApiService.getBuyerOrders()
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