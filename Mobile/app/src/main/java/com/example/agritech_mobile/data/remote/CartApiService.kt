package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.CartMessageResponse
import com.example.agritech_mobile.data.remote.dto.CartQuantityRequest
import com.example.agritech_mobile.data.remote.dto.CartRequest
import com.example.agritech_mobile.data.remote.dto.CartResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface CartApiService {
    @POST("api/cart/add")
    suspend fun addToCart(
        @Body request: CartRequest
    ): Response<CartMessageResponse>

    @GET("api/cart")
    suspend fun getCartItems(): Response<List<CartResponse>>

    @PUT("api/cart/{cartItemId}")
    suspend fun updateQuantity(
        @Path("cartItemId") cartItemId: String,
        @Body request: CartQuantityRequest
    ): Response<CartMessageResponse>

    @DELETE("api/cart/{cartItemId}")
    suspend fun deleteCartItem(
        @Path("cartItemId") cartItemId: String
    ): Response<CartMessageResponse>
}