package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.ProductCostResponse
import com.example.agritech_mobile.data.remote.dto.UpdateProductCostRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path

interface ProfitApiService {

    @GET("api/products/my-costs")
    suspend fun getMyProductCosts(): List<ProductCostResponse>

    @PUT("api/products/{productId}/cost")
    suspend fun updateProductCost(
        @Path("productId") productId: String,
        @Body request: UpdateProductCostRequest
    ): ProductCostResponse
}