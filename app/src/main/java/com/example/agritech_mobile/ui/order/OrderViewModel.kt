package com.example.agritech_mobile.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.dto.OrderItem
import com.example.agritech_mobile.data.remote.dto.OrderResponse
import com.example.agritech_mobile.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class OrderState {
    object Idle : OrderState()
    object Loading : OrderState()
    data class OrderItemSuccess(val items: List<OrderResponse>) : OrderState()
    data class Success(val message: String) : OrderState()
    data class Error(val error: String) : OrderState()
}

@HiltViewModel
class OrderViewModel @Inject constructor(
    private val repository: OrderRepository
) : ViewModel() {
    private val _orderState = MutableStateFlow<OrderState>(OrderState.Idle)
    val orderState: StateFlow<OrderState> = _orderState.asStateFlow()

    fun checkout(shippingAddress: String, phoneNumber: String, selectedCartItemIds: List<String>) {
        _orderState.value = OrderState.Loading
        viewModelScope.launch {
            val result = repository.checkout(shippingAddress, phoneNumber, selectedCartItemIds)
            result.onSuccess { response ->
                _orderState.value = OrderState.Success(response.message)
            }.onFailure { exception ->
                _orderState.value = OrderState.Error(exception.message ?: "Lỗi hệ thống")
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
}