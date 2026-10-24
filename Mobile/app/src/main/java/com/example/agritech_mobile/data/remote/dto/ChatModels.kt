package com.example.agritech_mobile.data.remote.dto

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderAvatar: String? = null,
    val text: String = "",
    val bargainId: Long? = null,
    val timestamp: Long = System.currentTimeMillis(),

    val productId: String? = null,
    val productName: String? = null,
    val productPrice: Double? = null,
    val productImage: String? = null
)
