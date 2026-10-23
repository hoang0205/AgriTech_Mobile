package com.example.agritech_mobile.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.remote.dto.CartResponse
import com.example.agritech_mobile.data.remote.dto.DeliveryQuoteResponse
import com.example.agritech_mobile.data.remote.dto.OrderBuyerResponse
import com.example.agritech_mobile.data.remote.dto.OrderItem
import com.example.agritech_mobile.data.remote.dto.OrderResponse
import com.example.agritech_mobile.data.remote.dto.OrderStatusCountResponse
import com.example.agritech_mobile.data.remote.dto.ShippingDetails
import com.example.agritech_mobile.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OrderState {
    object Idle : OrderState()
    object Loading : OrderState()
    data class OrderItemSuccess(val items: List<OrderResponse>) : OrderState()
    data class OrderBuyerItemSuccess(val items: List<OrderBuyerResponse>) : OrderState()
    data class Success(val message: String, val orderId: Long? = null) : OrderState()
    data class Error(val error: String) : OrderState()
}

data class DeliveryQuoteInput(
    val address: ShippingDetails,
    val items: List<CartResponse>
)

data class DeliveryQuoteState(
    val input: DeliveryQuoteInput? = null,
    val isLoading: Boolean = false,
    val quote: DeliveryQuoteResponse? = null,
    val error: String? = null
)

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val repository: OrderRepository,
    private val tokenManager: TokenManager
) : ViewModel() {
    private val _orderState = MutableStateFlow<OrderState>(OrderState.Idle)
    val orderState: StateFlow<OrderState> = _orderState.asStateFlow()

    val userAvatar: StateFlow<String> = tokenManager.avatarUrlFlow

    private val _orderCounts = MutableStateFlow(OrderStatusCountResponse())
    val orderCounts: StateFlow<OrderStatusCountResponse> = _orderCounts.asStateFlow()

    private val _deliveryQuoteState = MutableStateFlow(
        DeliveryQuoteState()
    )
    val deliveryQuoteState = _deliveryQuoteState.asStateFlow()

    private var deliveryQuoteJob: Job? = null
    private var deliveryQuoteVersion = 0L

    fun loadDeliveryQuote(input: DeliveryQuoteInput?) {
        deliveryQuoteJob?.cancel()
        val version = ++deliveryQuoteVersion

        if (input == null) {
            _deliveryQuoteState.value = DeliveryQuoteState()
            return
        }

        _deliveryQuoteState.value = DeliveryQuoteState(
            input = input,
            isLoading = true
        )

        deliveryQuoteJob = viewModelScope.launch {
            val result = repository.getDeliveryQuote(
                addressId = input.address.id,
                selectedCartItemIds = input.items.map { it.cartItemId }
            )

            if (version != deliveryQuoteVersion) return@launch

            result.onSuccess { response ->
                _deliveryQuoteState.value = DeliveryQuoteState(
                    input = input,
                    quote = response
                )
            }.onFailure { exception ->
                _deliveryQuoteState.value = DeliveryQuoteState(
                    input = input,
                    error = exception.message
                        ?: "Không tính được phí giao hàng."
                )
            }
        }
    }

    fun checkout(
        shippingAddress: String,
        phoneNumber: String,
        selectedCartItemIds: List<String>,
        addressId: String,
        expectedShippingFee: Long,
        paymentMethod: String = "COD"
    ) {
        if (_orderState.value is OrderState.Loading) return

        val delivery = _deliveryQuoteState.value
        val requestedIds = selectedCartItemIds.distinct().sorted()

        val quotedIds = delivery.input
            ?.items
            ?.map { it.cartItemId }
            ?.distinct()
            ?.sorted()

        val validQuote =
            !delivery.isLoading &&
                    delivery.error == null &&
                    delivery.input?.address?.id == addressId &&
                    quotedIds == requestedIds &&
                    delivery.quote?.allDeliverable == true &&
                    delivery.quote?.totalShippingFee == expectedShippingFee

        if (!validQuote) {
            _orderState.value = OrderState.Error(
                "Thông tin giao hàng đã thay đổi. Vui lòng lấy báo phí mới."
            )
            return
        }

        _orderState.value = OrderState.Loading

        viewModelScope.launch {
            repository.checkout(
                shippingAddress = shippingAddress,
                phoneNumber = phoneNumber,
                selectedCartItemIds = selectedCartItemIds,
                addressId = addressId,
                expectedShippingFee = expectedShippingFee,
                paymentMethod = paymentMethod
            ).onSuccess { response ->
                _orderState.value = OrderState.Success(
                    message = response.message,
                    orderId = response.orderId
                )
            }.onFailure { exception ->
                _orderState.value = OrderState.Error(
                    exception.message ?: "Không đặt được hàng."
                )
            }
        }
    }
    fun updateOrderStatus(orderId: String, status: String) {
        _orderState.value = OrderState.Loading
        viewModelScope.launch {
            val result = repository.updateOrderStatus(orderId, status)
            result.onSuccess { response ->
                _orderState.value = OrderState.Success(response.message)
            }.onFailure { exception ->
                _orderState.value = OrderState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun getSellerOrders() {
        _orderState.value = OrderState.Loading
        viewModelScope.launch {
            val result = repository.getSellerOrders()
            result.onSuccess { items ->
                _orderState.value = OrderState.OrderItemSuccess(items)
                }.onFailure { exception ->
                _orderState.value = OrderState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun getBuyerOrders(status: String ? = null) {
        _orderState.value = OrderState.Loading
        viewModelScope.launch {
            val result = repository.getBuyerOrders(status)
            result.onSuccess { items ->
                _orderState.value = OrderState.OrderBuyerItemSuccess(items)
                }.onFailure { exception ->
                _orderState.value = OrderState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun loadOrderCounts() {
        viewModelScope.launch {
            val result = repository.getOrderStatusCounts()
            result.onSuccess { counts ->
                _orderCounts.value = counts
            }.onFailure {
                _orderCounts.value = OrderStatusCountResponse()
            }
        }
    }

    fun cancelOrder(orderId: Long, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.cancelOrderByBuyer(orderId)
            result.onSuccess {
                loadOrderCounts()
                onSuccess()
            }.onFailure { exception ->
                onError(exception.message ?: "Không thể hủy đơn hàng")
            }
        }
    }

    fun payWithVnpay(orderId: Long, onUrlReady: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = repository.getVnpayPaymentUrl(orderId)
            result.onSuccess { url ->
                onUrlReady(url)
            }.onFailure { exception ->
                onError(exception.message ?: "Không thể kết nối đến VNPay")
            }
        }
    }

    fun confirmOrderPaid(orderId: Long, params: Map<String, String>, onError: (String) -> Unit, onSuccess: () -> Unit) {
        viewModelScope.launch {
            val result = repository.markOrderAsPaid(orderId, params)
            result.onSuccess {
                onSuccess()
            }.onFailure { onError(it.message ?: "Không xác nhận được thanh toán. Vui lòng kiểm tra đơn hàng.") }
        }
    }
    fun resetState() {
        _orderState.value = OrderState.Idle
    }
}