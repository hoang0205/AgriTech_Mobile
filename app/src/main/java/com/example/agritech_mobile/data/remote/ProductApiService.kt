package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.PageResponse
import com.example.agritech_mobile.data.remote.dto.ProductRequest
import com.example.agritech_mobile.data.remote.dto.ProductResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ProductApiService {

    @GET("api/products")
    suspend fun getProducts(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<PageResponse<ProductResponse>>

    @POST("api/products")
    suspend fun createProduct(
        @Body productRequest: ProductRequest
    ): Response<ProductResponse>

    @GET("api/products/{id}")
    suspend fun getProductById(
        @Path("id") productId: String
    ): Response<ProductResponse>

    @GET("api/products/search")
    suspend fun searchProducts(
        @Query("keyword") query: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<PageResponse<ProductResponse>>

    @GET("api/products/random")
    suspend fun getRandomProducts(
        @Query("limit") limit: Int = 10
    ): Response<List<ProductResponse>>

    @GET("api/products/recommendations")
    suspend fun getRecommendations(
        @Query("limit") limit: Int = 10
    ): Response<List<ProductResponse>>

    @GET("api/products/category/{category}")
    suspend fun getProductsByCategory(
        @Path("category") category: String,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<PageResponse<ProductResponse>>

    @GET("api/products/suggest")
    suspend fun getSuggestedProducts(
        @Query("keyword") query: String,
    ) : Response<List<String>>

}

