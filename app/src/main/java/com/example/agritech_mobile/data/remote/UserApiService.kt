package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.AddressRequest
import com.example.agritech_mobile.data.remote.dto.ProvinceResponse
import com.example.agritech_mobile.data.remote.dto.ShippingAddressResponce
import com.example.agritech_mobile.data.remote.dto.ShippingDetails
import com.example.agritech_mobile.data.remote.dto.UpdatePasswordRequest
import com.example.agritech_mobile.data.remote.dto.UpdateProfileRequest
import com.example.agritech_mobile.data.remote.dto.UpdateProfileResponse
import com.example.agritech_mobile.data.remote.dto.WardResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface UserApiService {
    @GET("api/user/profile")
    suspend fun getUserProfile(): Response<UpdateProfileRequest>

    @PUT("api/user/profile")
    suspend fun updateUserProfile(
        @Body request: UpdateProfileRequest
    ): Response<UpdateProfileResponse>

    @PUT("api/user/password")
    suspend fun updateUserPassword(
        @Body request: UpdatePasswordRequest
    ): Response<UpdateProfileResponse>

    @GET("api/user/addresses")
    suspend fun getUserAddresses(): Response<List<ShippingDetails>>

    @POST("api/user/addresses")
    suspend fun addAddress(
        @Body request: AddressRequest
    ): Response<ShippingAddressResponce>

    @PATCH("api/user/addresses/{id}/default")
    suspend fun setDefaultAddress(
        @Path("id") addressId: String
    ): Response<ShippingAddressResponce>

    @GET("api/addresses/provinces")
    suspend fun getProvinces(): Response<List<ProvinceResponse>>

    @GET("api/addresses/wards/{provinceCode}")
    suspend fun getWards(
        @Path("provinceCode") provinceCode: String
    ): Response<List<WardResponse>>
}