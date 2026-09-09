package com.example.agritech_mobile.data.remote.dto

data class ProductRequest(
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Double,
    val unit: String,
    val description: String,
    val imageUrls: List<String>
)

data class PageResponse<T>(
    val content: List<T>,
    val last: Boolean,
    val totalPages: Int,
    val totalElements: Int
)

data class ProductResponse(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Double,
    val unit: String,
    val description: String,
    val imageUrls: List<String>,
    val farmerName: String,
    val rating: Double,
    val reviewCount: Int,
    val farmerId: String,
    val farmerAvatar: String?,
    val farmerPhone: String?
)


