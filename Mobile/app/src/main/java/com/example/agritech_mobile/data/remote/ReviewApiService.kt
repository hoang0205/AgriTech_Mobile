package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.GetReviewResponse
import com.example.agritech_mobile.data.remote.dto.ReviewModelsRequest
import com.example.agritech_mobile.data.remote.dto.ReviewModelsResponse
import com.example.agritech_mobile.data.remote.dto.ReviewSummaryResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReviewApiService {
    @POST("api/reviews")
    suspend fun createReview(
        @Body reviewRequest: ReviewModelsRequest
    ): Response<ReviewModelsResponse>

    @GET("api/reviews/product/{productId}")
    suspend fun getReviewsByProductId(
        @Path("productId") productId: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<GetReviewResponse>

    @GET("api/reviews/product/{productId}/summary")
    suspend fun getReviewSummaryByProductId(
        @Path("productId") productId: String
    ): Response<ReviewSummaryResponse>
}