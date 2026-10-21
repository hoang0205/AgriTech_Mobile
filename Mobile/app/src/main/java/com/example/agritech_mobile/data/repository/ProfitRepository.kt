package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.ProfitApiService
import com.example.agritech_mobile.data.remote.dto.ProductCostResponse
import com.example.agritech_mobile.data.remote.dto.UpdateProductCostRequest
import retrofit2.Retrofit
import javax.inject.Inject

class ProfitRepository @Inject constructor(
    retrofit: Retrofit
) {
    private val api = retrofit.create(ProfitApiService::class.java)

    suspend fun getMyProductCosts(): List<ProductCostResponse> {
        return api.getMyProductCosts()
    }

    suspend fun updateProductCost(
        productId: String,
        costPrice: Double
    ): ProductCostResponse {
        return api.updateProductCost(
            productId = productId,
            request = UpdateProductCostRequest(
                costPrice = costPrice
            )
        )
    }
}