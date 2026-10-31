package com.example.agritech_mobile.ui.order

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.dto.OrderDetailResponse
import com.example.agritech_mobile.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderDetailUiState(
    val order: OrderDetailResponse? = null,
    val loading: Boolean = true,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(private val repository: OrderRepository) : ViewModel() {
    private val _state = MutableStateFlow(OrderDetailUiState())
    val state = _state.asStateFlow()
    private var loadJob: Job? = null

    fun load(id: Long) {
        if (_state.value.busy) return
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            fetch(id)
        }
    }

    private suspend fun fetch(id: Long) {
        repository.getOrderDetail(id).fold(
            onSuccess = { order -> _state.update { it.copy(order = order, loading = false, error = null) } },
            onFailure = { error -> _state.update { it.copy(loading = false, error = error.message) } }
        )
    }

    fun updateStatus(status: String) {
        val order = _state.value.order ?: return
        if (_state.value.busy || _state.value.loading) return
        loadJob?.cancel()
        _state.update { it.copy(busy = true, error = null, notice = null) }
        viewModelScope.launch {
            try {
                val result = if (status == "COMPLETED" && order.viewerRole == "BUYER")
                    repository.confirmOrderReceived(order.orderId)
                else if (status == "CANCELLED" && order.viewerRole == "BUYER")
                    repository.cancelOrderByBuyer(order.orderId)
                else repository.updateOrderStatus(order.orderId.toString(), status)
                result.fold(
                    onSuccess = { response -> _state.update { it.copy(notice = response.message) } },
                    onFailure = { error -> _state.update { it.copy(error = error.message) } }
                )
                // Reload even on failure: the server may have committed before the connection failed.
                val actionError = _state.value.error
                fetch(order.orderId)
                if (actionError != null) _state.update { it.copy(error = actionError) }
            } catch (e: CancellationException) { throw e }
            finally { _state.update { it.copy(busy = false) } }
        }
    }

    fun pay(onReady: (Long, String) -> Unit) {
        val order = _state.value.order ?: return
        if (!order.canPay || _state.value.busy || _state.value.loading) return
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                repository.getVnpayPaymentUrl(order.orderId).fold(
                    onSuccess = { onReady(order.orderId, it) },
                    onFailure = { error -> _state.update { it.copy(error = error.message) } }
                )
            } finally { _state.update { it.copy(busy = false) } }
        }
    }
}
