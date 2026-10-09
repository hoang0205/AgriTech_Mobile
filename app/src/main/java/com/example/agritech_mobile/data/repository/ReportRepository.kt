package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.ReportApiService
import com.example.agritech_mobile.data.remote.dto.CreateProductReportRequest
import com.example.agritech_mobile.data.remote.dto.ErrorResponse
import com.example.agritech_mobile.data.remote.dto.PageResponse
import com.example.agritech_mobile.data.remote.dto.ProductReportResponse
import com.example.agritech_mobile.data.remote.dto.ReportEligibilityResponse
import com.example.agritech_mobile.data.remote.dto.ReportStatus
import com.example.agritech_mobile.data.remote.dto.UpdateProductReportRequest
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import java.io.IOException
import javax.inject.Inject

class ReportRepository @Inject constructor(
    private val apiService: ReportApiService
) {
    suspend fun createProductReport(
        request: CreateProductReportRequest
    ): Result<ProductReportResponse> {
        return executeRequest {
            apiService.createProductReport(request)
        }
    }

    suspend fun checkEligibility(
        productId: String
    ): Result<ReportEligibilityResponse> {
        return executeRequest {
            apiService.checkEligibility(productId)
        }
    }

    suspend fun getMyReports(
        page: Int = 0,
        size: Int = 10
    ): Result<PageResponse<ProductReportResponse>> {
        return executeRequest {
            apiService.getMyReports(page, size)
        }
    }

    suspend fun getAdminReports(
        page: Int = 0,
        size: Int = 10,
        status: ReportStatus? = null
    ): Result<PageResponse<ProductReportResponse>> {
        return executeRequest {
            apiService.getAdminReports(page, size, status)
        }
    }

    suspend fun updateReport(
        reportId: Long,
        request: UpdateProductReportRequest
    ): Result<ProductReportResponse> {
        return executeRequest {
            apiService.updateReport(reportId, request)
        }
    }

    private suspend fun <T> executeRequest(
        call: suspend () -> Response<T>
    ): Result<T> {
        return try {
            val response = call()

            if (response.isSuccessful) {
                val body = response.body()

                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(
                        Exception("Máy chủ trả về dữ liệu rỗng")
                    )
                }
            } else {
                val errorJson = response.errorBody()?.string()

                val errorResponse = runCatching {
                    Gson().fromJson(
                        errorJson,
                        ErrorResponse::class.java
                    )
                }.getOrNull()

                val message = errorResponse?.message
                    ?.takeIf { it.isNotBlank() }
                    ?: when (response.code()) {
                        401 -> "Phiên đăng nhập đã hết hạn"
                        403 -> "Bạn không có quyền thực hiện thao tác này"
                        404 -> "Sản phẩm hoặc báo cáo không tồn tại"
                        409 -> "Trạng thái báo cáo đã thay đổi"
                        else -> "Yêu cầu thất bại (${response.code()})"
                    }

                Result.failure(Exception(message))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Result.failure(
                Exception(
                    "Không thể kết nối máy chủ. Vui lòng kiểm tra mạng.",
                    e
                )
            )
        } catch (e: Exception) {
            Result.failure(
                Exception(
                    "Không xử lý được phản hồi từ máy chủ",
                    e
                )
            )
        }
    }
}