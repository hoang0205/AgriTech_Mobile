package com.example.agritech_mobile.data.remote.dto

data class GhnArea(val id: String, val name: String)
data class GhnShop(
    val connected: Boolean, val pending: Boolean, val environment: String,
    val shopId: Int?, val name: String?, val address: String?, val outcome: String? = null,
    val senderAddress: GhnSenderAddress? = null, val suggestedPickupAddress: String? = null
)
data class GhnShopRequest(val name: String, val address: String, val provinceId: Int, val districtId: Int, val wardCode: String)
data class GhnSenderRequest(val address: String, val provinceId: Int, val wardCode: String)
data class GhnSenderAddress(val address: String, val provinceId: Int, val wardCode: String, val provinceName: String, val wardName: String)
data class GhnParcelRequest(
    val provinceId: Int, val districtId: Int, val wardCode: String,
    val weight: Int, val length: Int, val width: Int, val height: Int,
    val expectedFee: Long? = null
)
data class GhnQuote(val fee: Long, val customerShippingFee: Long, val codAmount: Long, val environment: String)
data class GhnShipment(
    val orderCode: String?, val status: String?, val shippingFee: Long?,
    val expectedDeliveryTime: String?, val updatedAt: String?, val environment: String?,
    val pendingCreation: Boolean, val recipientName: String?, val recipientAddress: String,
    val customerShippingFee: Long, val orderStatus: String? = null
)
data class DeliveryManagement(
    val method: String?, val actualCost: Long?, val customerShippingFee: Long,
    val ghnEstimatedFee: Long?, val ghnActive: Boolean, val status: String, val costUpdatedAt: String?
)
data class ChooseDeliveryRequest(val method: String)
data class ShippingCostRequest(val actualCost: Long?)
