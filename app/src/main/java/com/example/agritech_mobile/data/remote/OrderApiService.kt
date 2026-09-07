package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.CheckOutRequest
import com.example.agritech_mobile.data.remote.dto.OrderBuyerResponse
import com.example.agritech_mobile.data.remote.dto.OrderMessageResponse
import com.example.agritech_mobile.data.remote.dto.OrderResponse
import com.example.agritech_mobile.data.remote.dto.OrderStatusCountResponse
import com.example.agritech_mobile.data.remote.dto.StatusRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface OrderApiService {
    @POST("api/orders/checkout")
    suspend fun checkout(
        @Body request: CheckOutRequest
    ): Response<OrderMessageResponse>

    @PATCH("api/orders/{orderId}/status")
    suspend fun updateOrderStatus(
        @Path("orderId") orderId: String,
        @Body request: StatusRequest
    ): Response<OrderMessageResponse>

    @GET("api/orders/seller")
    suspend fun getSellerOrders(): Response<List<OrderResponse>>

    @GET("api/orders/my-orders")
    suspend fun getBuyerOrders(
        @Query("status") status: String?
    ): Response<List<OrderBuyerResponse>>

    @GET("/api/orders/my-orders/count")
    suspend fun getOrderStatusCounts(): Response<OrderStatusCountResponse>
}