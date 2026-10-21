package com.example.agritech_mobile.data.remote.dto

data class EditableProductResponse(
    val id: String,
    val name: String,
    val category: String,
    val price: Double,
    val quantity: Double,
    val unit: String,
    val description: String,
    val imageUrls: List<String>,
    val costPrice: Double?
)

data class UpdateMyProductRequest(
    val name: String,
    val category: String,
    val price: Double,
    val unit: String,
    val description: String,
    val imageUrls: List<String>,
    val costPrice: Double?,
    val quantity: Double? = null,
    val expectedQuantity: Double? = null
)
