package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.CreateProductReportRequest
import com.example.agritech_mobile.data.remote.dto.PageResponse
import com.example.agritech_mobile.data.remote.dto.ProductReportResponse
import com.example.agritech_mobile.data.remote.dto.ReportEligibilityResponse
import com.example.agritech_mobile.data.remote.dto.ReportStatus
import com.example.agritech_mobile.data.remote.dto.UpdateProductReportRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ReportApiService {

    @POST("api/reports/products")
    suspend fun createProductReport(
        @Body request: CreateProductReportRequest
    ): Response<ProductReportResponse>

    @GET("api/reports/products/{productId}/eligibility")
    suspend fun checkEligibility(
        @Path("productId") productId: String
    ): Response<ReportEligibilityResponse>

    @GET("api/reports/mine")
    suspend fun getMyReports(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<PageResponse<ProductReportResponse>>

    @GET("api/admin/reports")
    suspend fun getAdminReports(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("status") status: ReportStatus? = null
    ): Response<PageResponse<ProductReportResponse>>

    @PATCH("api/admin/reports/{id}")
    suspend fun updateReport(
        @Path("id") reportId: Long,
        @Body request: UpdateProductReportRequest
    ): Response<ProductReportResponse>
}