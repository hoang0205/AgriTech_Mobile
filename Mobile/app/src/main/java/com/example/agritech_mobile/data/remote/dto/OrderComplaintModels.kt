package com.example.agritech_mobile.data.remote.dto

data class CreateOrderComplaintRequest(val reason: String, val description: String, val imageUrls: List<String>)
data class OrderComplaintEvent(val status: String, val message: String, val occurredAt: String)
data class OrderComplaintResponse(
    val id: Long, val orderId: Long, val reporterName: String, val reason: String,
    val description: String, val imageUrls: List<String>, val status: String,
    val adminReply: String?, val createdAt: String, val updatedAt: String,
    val orderStatus: String, val sellerDeliveredAt: String?, val buyerReceivedAt: String?,
    val events: List<OrderComplaintEvent>,
    val outcome: String? = null
)
data class OrderComplaintContext(val complaint: OrderComplaintResponse?, val canCreate: Boolean)
