package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.OrderApiService
import com.example.agritech_mobile.data.remote.dto.CheckOutRequest
import com.example.agritech_mobile.data.remote.dto.OrderBuyerResponse
import com.example.agritech_mobile.data.remote.dto.OrderMessageResponse
import com.example.agritech_mobile.data.remote.dto.OrderResponse
import com.example.agritech_mobile.data.remote.dto.OrderStatusCountResponse
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
        selectedCartItemIds: List<String>,
        paymentMethod: String = "COD"
    ): Result<OrderMessageResponse> {
        return try {
            val request = CheckOutRequest(shippingAddress, phoneNumber, selectedCartItemIds, paymentMethod)
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

    suspend fun getBuyerOrders(status: String? = null): Result<List<OrderBuyerResponse>> {
        return try {
            val response = orderApiService.getBuyerOrders(status)
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

    suspend fun getOrderStatusCounts(): Result<OrderStatusCountResponse> {
        return try {
            val response = orderApiService.getOrderStatusCounts()
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
    suspend fun getVnpayPaymentUrl(orderId: Long): Result<String> {
        return try {
            val response = orderApiService.getVnpayPaymentUrl(orderId)
            if (response.isSuccessful) {
                val url = response.body()?.get("paymentUrl")
                if (!url.isNullOrBlank()) {
                    Result.success(url)
                } else {
                    Result.failure(Exception("Không tìm thấy đường dẫn thanh toán"))
                }
            } else {
                Result.failure(Exception("Lỗi máy chủ (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markOrderAsPaid(orderId: Long): Result<Unit> {
        return try {
            val response = orderApiService.markOrderAsPaid(orderId)
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Không thể cập nhật trạng thái đơn"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun cancelOrderByBuyer(orderId: Long): Result<OrderMessageResponse> {
        return try {
            val response = orderApiService.cancelOrderByBuyer(orderId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                Result.failure(Exception("Không thể hủy đơn hàng (${response.code()})"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}