package com.example.agritech_mobile.ui.cart

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.dto.CartResponse
import com.example.agritech_mobile.data.repository.CartRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class CartState {
    object Idle : CartState()
    object Loading : CartState()
    data class CartItemsSuccess(val items: List<CartResponse>) : CartState()
    data class ActionSuccess(val message: String) : CartState()
    data class Error(val error: String) : CartState()
}

@HiltViewModel
class CartViewModel @Inject constructor(
    private val repository: CartRepository
) : ViewModel() {
    private val _cartState = MutableStateFlow<CartState>(CartState.Idle)
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    init {
        loadCartItems()
    }

    fun loadCartItems() {
        _cartState.value = CartState.Loading
        viewModelScope.launch {
            val result = repository.getCartItems()
            result.onSuccess { response ->
                _cartState.value = CartState.CartItemsSuccess(response)
            }.onFailure { exception ->
                _cartState.value = CartState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun deleteCartItem(cartItemId: String) {
        _cartState.value = CartState.Loading
        viewModelScope.launch {
            val result = repository.deleteCartItem(cartItemId)
            result.onSuccess { response ->
                _cartState.value = CartState.ActionSuccess(response.message)
                loadCartItems()
            }.onFailure { exception ->
                _cartState.value = CartState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun updateQuantity(cartItemId: String, quantity: Double) {
        _cartState.value = CartState.Loading
        viewModelScope.launch {
            val result = repository.updateQuantity(cartItemId, quantity)
            result.onSuccess { response ->
                _cartState.value = CartState.ActionSuccess(response.message)
                loadCartItems()
            }.onFailure { exception ->
                _cartState.value = CartState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun addToCart(productId: String, quantity: Double) {
        _cartState.value = CartState.Loading
        viewModelScope.launch {
            val result = repository.addToCart(productId, quantity)
            result.onSuccess { response ->
                _cartState.value = CartState.ActionSuccess(response.message)
            }.onFailure { exception ->
                _cartState.value = CartState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }
}