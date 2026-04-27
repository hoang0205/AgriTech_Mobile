package com.example.agritech_mobile.data.remote.dto

import android.R
import com.example.agritech_mobile.ui.dashboard.Product

data class CheckOutRequest(
    val shippingAddress: String,
    val phoneNumber: String,
    val selectedCartItemIds: List<String>
)

data class StatusRequest(
    val status: String
)

data class OrderMessageResponse(
    val message: String
)

data class OrderItem(
    val productName: String,
    val quantity: Double,
    val unit: String,
    val price: Double,
    val thumbnail: String
)

data class OrderBuyerItem(
    val productId: String,
    val productName: String,
    val farmerName: String,
    val quantity: Double,
    val unit: String,
    val price: Double,
    val thumbnail: String
)

data class OrderResponse(
    val orderId: String,
    val orderDate: String,
    val buyerName: String,
    val buyerPhone: String,
    val buyerId: String,
    val shippingAddress: String,
    val status: String,
    val orderItems: List<OrderItem>,
    val totalRevenueFromThisOrder: Double
)

data class OrderBuyerResponse(
    val orderId: String,
    val orderDate: String,
    val status: String,
    val shippingAddress: String,
    val totalAmount: Double,
    val items: List<OrderBuyerItem>
)
