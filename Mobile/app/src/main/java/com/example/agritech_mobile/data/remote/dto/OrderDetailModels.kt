package com.example.agritech_mobile.data.remote.dto

data class OrderPartyDetail(val id: String, val name: String, val avatar: String?, val phone: String)

data class OrderLineDetail(
    val id: Long, val productId: String, val name: String, val image: String?,
    val quantity: Double, val unit: String, val unitPrice: Double,
    val subtotal: Double, val sellerName: String
)

data class OrderDetailResponse(
    val orderId: Long,
    val status: String,
    val orderDate: String,
    val viewerRole: String,
    val buyer: OrderPartyDetail,
    val sellers: List<OrderPartyDetail>,
    val receiverName: String,
    val receiverPhone: String,
    val shippingAddress: String,
    val pickupAddress: String?,
    val items: List<OrderLineDetail>,
    val subtotal: Double,
    val shippingFee: Long,
    val totalAmount: Double,
    val paymentMethod: String?,
    val paymentStatus: String,
    val transactionNo: String?,
    val deliveryMethod: String?,
    val actualShippingCost: Long?,
    val ghnOrderCode: String?,
    val ghnStatus: String?,
    val expectedDeliveryTime: String?,
    val canCancel: Boolean,
    val canPay: Boolean,
    val nextStatus: String?,
    val canManageDelivery: Boolean,
    val sellerDeliveredAt: String? = null,
    val buyerReceivedAt: String? = null,
    val autoCompleteAt: String? = null,
    val completedAt: String? = null,
    val completionMethod: String? = null,
    val complaintOpen: Boolean = false,
    val complaintId: Long? = null,
    val canComplain: Boolean = false
)
