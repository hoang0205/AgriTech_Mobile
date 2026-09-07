package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.UserApiService
import com.example.agritech_mobile.data.remote.dto.AddressRequest
import com.example.agritech_mobile.data.remote.dto.ProvinceResponse
import com.example.agritech_mobile.data.remote.dto.ShippingAddressResponce
import com.example.agritech_mobile.data.remote.dto.ShippingDetails
import com.example.agritech_mobile.data.remote.dto.UpdatePasswordRequest
import com.example.agritech_mobile.data.remote.dto.UpdateProfileRequest
import com.example.agritech_mobile.data.remote.dto.UpdateProfileResponse
import com.example.agritech_mobile.data.remote.dto.WardResponse
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
    ): Result<UpdateProfileResponse> {
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
        isDefault: Boolean = false
    ): Result<ShippingAddressResponce> {
        return try {
            val request = AddressRequest(receiverName, receiverPhone, detail, isDefault)
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
    ): Result<UpdateProfileResponse> {
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
}