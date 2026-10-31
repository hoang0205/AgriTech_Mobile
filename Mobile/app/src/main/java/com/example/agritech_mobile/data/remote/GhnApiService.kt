package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.GhnArea
import com.example.agritech_mobile.data.remote.dto.GhnShop
import com.example.agritech_mobile.data.remote.dto.GhnShopRequest
import com.example.agritech_mobile.data.remote.dto.GhnSenderRequest
import com.example.agritech_mobile.data.remote.dto.GhnParcelRequest
import com.example.agritech_mobile.data.remote.dto.GhnQuote
import com.example.agritech_mobile.data.remote.dto.GhnShipment
import com.example.agritech_mobile.data.remote.dto.DeliveryManagement
import com.example.agritech_mobile.data.remote.dto.ChooseDeliveryRequest
import com.example.agritech_mobile.data.remote.dto.ShippingCostRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface GhnApiService {
    @GET("api/delivery/ghn/shop")
    suspend fun shop(): Response<GhnShop>
    @POST("api/delivery/ghn/shop/connect")
    suspend fun connectShop(): Response<GhnShop>
    @POST("api/delivery/ghn/shop")
    suspend fun registerShop(@Body request: GhnShopRequest): Response<GhnShop>
    @PUT("api/delivery/ghn/shop/sender-address")
    suspend fun senderAddress(@Body request: GhnSenderRequest): Response<GhnShop>
    @GET("api/delivery/ghn/shop/areas/{level}")
    suspend fun shopAreas(@Path("level") level: String, @Query("parent") parent: Int?): Response<List<GhnArea>>
    @GET("api/orders/{id}/delivery")
    suspend fun delivery(@Path("id") id: String): Response<DeliveryManagement>
    @PUT("api/orders/{id}/delivery/method")
    suspend fun chooseDelivery(@Path("id") id: String, @Body request: ChooseDeliveryRequest): Response<DeliveryManagement>
    @PUT("api/orders/{id}/delivery/cost")
    suspend fun shippingCost(@Path("id") id: String, @Body request: ShippingCostRequest): Response<DeliveryManagement>
    @GET("api/delivery/ghn/areas/{level}")
    suspend fun areas(@Path("level") level: String, @Query("parent") parent: Int?): Response<List<GhnArea>>
    @GET("api/delivery/ghn/orders/{id}")
    suspend fun view(@Path("id") id: String): Response<GhnShipment>
    @POST("api/delivery/ghn/orders/{id}/quote")
    suspend fun quote(@Path("id") id: String, @Body request: GhnParcelRequest): Response<GhnQuote>
    @POST("api/delivery/ghn/orders/{id}/reference")
    suspend fun reference(@Path("id") id: String, @Body request: GhnParcelRequest): Response<GhnShipment>
    @POST("api/delivery/ghn/orders/{id}")
    suspend fun create(@Path("id") id: String, @Body request: GhnParcelRequest): Response<GhnShipment>
    @POST("api/delivery/ghn/orders/{id}/{action}")
    suspend fun action(@Path("id") id: String, @Path("action") action: String): Response<GhnShipment>
}
