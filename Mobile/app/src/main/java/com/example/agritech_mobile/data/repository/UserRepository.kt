package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.UserApiService
import com.example.agritech_mobile.data.remote.dto.AddressRequest
import com.example.agritech_mobile.data.remote.dto.ErrorResponse
import com.example.agritech_mobile.data.remote.dto.MessageResponse
import com.example.agritech_mobile.data.remote.dto.ProvinceResponse
import com.example.agritech_mobile.data.remote.dto.SellerPickupResponse
import com.example.agritech_mobile.data.remote.dto.ShippingAddressResponce
import com.example.agritech_mobile.data.remote.dto.ShippingDetails
import com.example.agritech_mobile.data.remote.dto.UpdatePasswordRequest
import com.example.agritech_mobile.data.remote.dto.UpdateProfileRequest
import com.example.agritech_mobile.data.remote.dto.UpdateProfileResponse
import com.example.agritech_mobile.data.remote.dto.UpdateSellerPickupRequest
import com.example.agritech_mobile.data.remote.dto.WardResponse
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userApiService: UserApiService
) {
    suspend fun getUserProfile(): Result<UpdateProfileRequest> {
        return try {
            val response = userApiService.getUserProfile()
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

    suspend fun updateUserProfile(
        fullName: String,
        avatarUrl: String
    ): Result<MessageResponse> {
        return try {
            val request = UpdateProfileRequest(fullName, avatarUrl)
            val response = userApiService.updateUserProfile(request)
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

    suspend fun getUserAddress(): Result<List<ShippingDetails>> {
        return try {
            val response = userApiService.getUserAddresses()
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

    suspend fun addUserAddress(
        receiverName: String,
        receiverPhone: String,
        detail: String,
        isDefault: Boolean = false,
        latitude: Double? = null,
        longitude: Double? = null
    ): Result<ShippingAddressResponce> {
        return try {
            val request = AddressRequest(
                receiverName = receiverName,
                receiverPhone = receiverPhone,
                detail = detail,
                isDefault = isDefault,
                latitude = latitude,
                longitude = longitude
            )
            val response = userApiService.addAddress(request)
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

    suspend fun updateAddress(addressId: String, request: AddressRequest): Result<MessageResponse> {
        return addressMutationResult { userApiService.updateAddress(addressId, request) }
    }

    suspend fun deleteAddress(addressId: String): Result<MessageResponse> {
        return addressMutationResult { userApiService.deleteAddress(addressId) }
    }

    private suspend fun addressMutationResult(
        request: suspend () -> Response<MessageResponse>
    ): Result<MessageResponse> {
        return try {
            val response = request()
            if (response.isSuccessful) {
                response.body()?.let { Result.success(it) }
                    ?: Result.failure(Exception("Máy chủ trả về phản hồi rỗng"))
            } else {
                val message = runCatching {
                    Gson().fromJson(response.errorBody()?.string(), ErrorResponse::class.java)?.message
                }.getOrNull()?.takeIf { it.isNotBlank() }
                val fallback = when (response.code()) {
                    401 -> "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."
                    404 -> "Địa chỉ không còn tồn tại. Vui lòng tải lại danh sách."
                    else -> "Không xử lý được địa chỉ. Vui lòng thử lại."
                }
                Result.failure(Exception(message ?: fallback))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(Exception("Không kết nối được máy chủ. Vui lòng thử lại."))
        }
    }

    suspend fun setDefaultAddress(addressId: String): Result<ShippingAddressResponce> {
        return try {
            val response = userApiService.setDefaultAddress(addressId)
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

    suspend fun getProvinces(): Result<List<ProvinceResponse>> {
        return try {
            val response = userApiService.getProvinces()
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

    suspend fun getWards(provinceCode: String): Result<List<WardResponse>> {
        return try {
            val response = userApiService.getWards(provinceCode)
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

    suspend fun updateUserPassword(
        currentPassword: String,
        newPassword: String,
        confirmPassword: String
    ): Result<MessageResponse> {
        return try {
            val request = UpdatePasswordRequest(currentPassword, newPassword, confirmPassword)
            val response = userApiService.updateUserPassword(request)
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

    suspend fun getSellerPickupLocation(): Result<SellerPickupResponse> {
        return pickupRequestResult {
            userApiService.getSellerPickupLocation()
        }
    }

    suspend fun updateSellerPickupLocation(
        address: String,
        latitude: Double,
        longitude: Double
    ): Result<SellerPickupResponse> {
        val request = UpdateSellerPickupRequest(
            address = address.trim(),
            latitude = latitude,
            longitude = longitude
        )

        return pickupRequestResult {
            userApiService.updateSellerPickupLocation(request)
        }
    }

    private suspend fun pickupRequestResult(
        request: suspend () -> Response<SellerPickupResponse>
    ): Result<SellerPickupResponse> {
        return try {
            val response = request()

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(
                        Exception("Máy chủ không trả về địa điểm lấy hàng")
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
                    403 -> "Chỉ người bán được quản lý địa điểm lấy hàng."
                    else -> "Không xử lý được địa điểm lấy hàng. Vui lòng thử lại."
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
