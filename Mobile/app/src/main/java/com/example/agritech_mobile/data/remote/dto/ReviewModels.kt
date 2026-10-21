package com.example.agritech_mobile.data.remote.dto

data class ReviewModelsRequest (
    val productId: String,
    val rating: Int,
    val comment: String,
    val imageUrls: List<String>
)

data class ReviewModelsResponse (
    val id: String,
    val productId: String,
    val userId: String,
    val userName: String,
    val rating: Int,
    val comment: String,
    val imageUrls: List<String>,
    val createdAt: String,
)

data class GetReviewResponse(
    val totalElements: Int,
    val totalPages: Int,
    val size: Int,
    val content: List<ReviewModelsResponse>,
    val number: Int,
    val last: Boolean
)

data class ReviewSummaryResponse(
    val totalReviews: Int,
    val averageRating: Double,
    val star5: Int,
    val star4: Int,
    val star3: Int,
    val star2: Int,
    val star1: Int
)