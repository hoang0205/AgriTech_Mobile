package com.example.agritech_mobile.data.remote.dto

enum class ReportReason {
    SCAM,
    MISLEADING_INFORMATION,
    INAPPROPRIATE_CONTENT,
    OTHER
}

data class CreateProductReportRequest(
    val productId: String,
    val reason: ReportReason,
    val description: String,
    val imageUrls: List<String> = emptyList()
)

data class ProductReportResponse(
    val id: Long,
    val productId: String,
    val productName: String,
    val reason: ReportReason,
    val description: String,
    val imageUrls: List<String>,
    val status: String,
    val adminReply: String?,
    val createdAt: String
)

data class ReportEligibilityResponse(
    val canReport: Boolean,
    val message: String? = null
)

enum class ReportStatus {
    PENDING,
    REVIEWING,
    RESOLVED,
    REJECTED
}

data class UpdateProductReportRequest(
    val status: ReportStatus,
    val adminReply: String? = null
)