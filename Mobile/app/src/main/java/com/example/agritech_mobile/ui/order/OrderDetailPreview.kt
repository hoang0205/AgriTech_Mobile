package com.example.agritech_mobile.ui.order

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.agritech_mobile.data.remote.dto.OrderDetailResponse
import com.example.agritech_mobile.data.remote.dto.OrderLineDetail
import com.example.agritech_mobile.data.remote.dto.OrderPartyDetail
import com.example.agritech_mobile.ui.theme.AgritechTheme

private fun previewOrder() = OrderDetailResponse(
    orderId = 1024, status = "CONFIRMED", orderDate = "2026-10-24T09:30:00", viewerRole = "BUYER",
    buyer = OrderPartyDetail("buyer", "Nguyễn Minh Hoàng", null, "0901234567"),
    sellers = listOf(OrderPartyDetail("shop", "Nông trại xanh Ba Vì", null, "0902345678")),
    receiverName = "Nguyễn Minh Hoàng", receiverPhone = "0901234567",
    shippingAddress = "144 Xuân Thủy, phường Cầu Giấy, Hà Nội",
    pickupAddress = "Nông trại xanh, Ba Vì, Hà Nội",
    items = listOf(OrderLineDetail(1, "product", "Cà chua hữu cơ thu hoạch trong ngày", null,
        2.5, "kg", 30000.0, 75000.0, "Nông trại xanh Ba Vì")),
    subtotal = 75000.0, shippingFee = 15000, totalAmount = 90000.0,
    paymentMethod = "COD", paymentStatus = "COD", transactionNo = null,
    deliveryMethod = "SELF", actualShippingCost = null, ghnOrderCode = null, ghnStatus = null,
    expectedDeliveryTime = null, canCancel = false, canPay = false, nextStatus = null,
    canManageDelivery = false
)

@Preview(name = "Chi tiết đơn • Người mua", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun BuyerOrderDetailPreview() {
    AgritechTheme {
        OrderDetailContent(OrderDetailUiState(order = previewOrder(), loading = false),
            {}, {}, {}, {}, {}, {}, {})
    }
}

@Preview(name = "Chi tiết đơn • Người bán", showBackground = true, widthDp = 360, heightDp = 800, fontScale = 1.2f)
@Composable
private fun SellerOrderDetailPreview() {
    AgritechTheme {
        OrderDetailContent(OrderDetailUiState(order = previewOrder().copy(
            viewerRole = "SELLER", canCancel = true, nextStatus = "SHIPPING", canManageDelivery = true
        ), loading = false), {}, {}, {}, {}, {}, {}, {})
    }
}
