package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.OrderApiService
import com.example.agritech_mobile.data.remote.dto.CheckOutRequest
import com.example.agritech_mobile.data.remote.dto.DeliveryQuoteRequest
import com.example.agritech_mobile.data.remote.dto.DeliveryQuoteResponse
import com.example.agritech_mobile.data.remote.dto.ErrorResponse
import com.example.agritech_mobile.data.remote.dto.OrderBuyerResponse
import com.example.agritech_mobile.data.remote.dto.OrderMessageResponse
import com.example.agritech_mobile.data.remote.dto.OrderResponse
import com.example.agritech_mobile.data.remote.dto.OrderStatusCountResponse
import com.example.agritech_mobile.data.remote.dto.RevenueSummaryDto
import com.example.agritech_mobile.data.remote.dto.StatusRequest
import com.google.gson.Gson
import okhttp3.Address
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

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
        addressId: String,
        expectedShippingFee: Long,
        paymentMethod: String = "COD"
    ): Result<OrderMessageResponse> {
        val parsedAddressId = addressId.toLongOrNull()

        if (parsedAddressId == null || parsedAddressId <= 0) {
            return Result.failure(
                Exception("Vui lòng chọn địa chỉ nhận hàng hợp lệ.")
            )
        }

        val parsedCartIds = selectedCartItemIds.mapNotNull {
            it.toLongOrNull()?.takeIf { id -> id > 0 }
        }

        if (
            parsedCartIds.isEmpty() ||
            parsedCartIds.size != selectedCartItemIds.size
        ) {
            return Result.failure(
                Exception("Giỏ hàng không hợp lệ. Vui lòng tải lại.")
            )
        }

        if (expectedShippingFee < 0) {
            return Result.failure(
                Exception("Phí giao hàng không hợp lệ.")
            )
        }

        return try {
            val request = CheckOutRequest(
                shippingAddress = shippingAddress,
                phoneNumber = phoneNumber,
                selectedCartItemIds = parsedCartIds.distinct(),
                addressId = parsedAddressId,
                expectedShippingFee = expectedShippingFee,
                paymentMethod = paymentMethod
            )

            val response = orderApiService.checkout(request)

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(
                        Exception("Máy chủ không trả về kết quả đặt hàng.")
                    )
                }
            } else {
                val errorMessage = runCatching {
                    Gson().fromJson(
                        response.errorBody()?.string(),
                        ErrorResponse::class.java
                    )?.message
                }.getOrNull()?.takeIf { it.isNotBlank() }

                Result.failure(
                    Exception(
                        errorMessage
                            ?: "Không đặt được hàng. Vui lòng thử lại."
                    )
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    "Không nhận được kết quả đặt hàng. " +
                            "Vui lòng kiểm tra danh sách đơn hàng và kết nối mạng."
                )
            )
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

    suspend fun getRevenueSummary(): Result<RevenueSummaryDto> {
        return try {
            val response = orderApiService.getRevenueSummary()

            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(
                    Exception("Không thể tải doanh thu (${response.code()})")
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(
                Exception("Không thể kết nối đến server: ${e.localizedMessage}")
            )
        }
    }

    suspend fun getDeliveryQuote(
        addressId: String,
        selectedCartItemIds: List<String>
    ): Result<DeliveryQuoteResponse> {
        val parsedAddressId = addressId.toLongOrNull()

        if (parsedAddressId == null || parsedAddressId <= 0) {
            return Result.failure(
                Exception("Vui lòng chọn địa chỉ nhận hàng hợp lệ.")
            )
        }

        val parsedCartIds = selectedCartItemIds.mapNotNull {
            it.toLongOrNull()?.takeIf { id -> id > 0 }
        }

        if (
            parsedCartIds.isEmpty() ||
            parsedCartIds.size != selectedCartItemIds.size
        ) {
            return Result.failure(
                Exception("Danh sách sản phẩm không hợp lệ. Vui lòng tải lại giỏ hàng.")
            )
        }

        return try {
            val request = DeliveryQuoteRequest(
                addressId = parsedAddressId,
                selectedCartItemIds = parsedCartIds.distinct()
            )

            val response = orderApiService.getDeliveryQuote(request)

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(
                        Exception("Máy chủ không trả về thông tin giao hàng.")
                    )
                }
            } else {
                val errorMessage = runCatching {
                    Gson().fromJson(
                        response.errorBody()?.string(),
                        ErrorResponse::class.java
                    )?.message
                }.getOrNull()?.takeIf { it.isNotBlank() }

                val fallbackMessage = when (response.code()) {
                    401 -> "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."
                    403 -> "Không có quyền sử dụng địa chỉ hoặc giỏ hàng này."
                    404 -> "Không tìm thấy địa chỉ nhận hàng."
                    else -> "Không tính được phí giao hàng. Vui lòng thử lại."
                }

                Result.failure(
                    Exception(errorMessage ?: fallbackMessage)
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(
                Exception("Không kết nối được máy chủ. Vui lòng thử lại.")
            )
        }
    }
}