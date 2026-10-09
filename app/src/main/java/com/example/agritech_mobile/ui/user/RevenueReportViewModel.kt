package com.example.agritech_mobile.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.dto.RevenueSummaryDto
import com.example.agritech_mobile.data.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RevenueReportUiState(
    val isLoading: Boolean = true,
    val summary: RevenueSummaryDto? = null,
    val error: String? = null
)

@HiltViewModel
class RevenueReportViewModel @Inject constructor(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RevenueReportUiState())
    val uiState: StateFlow<RevenueReportUiState> = _uiState.asStateFlow()

    init {
        loadRevenue()
    }

    fun loadRevenue() {
        viewModelScope.launch {
            _uiState.value = RevenueReportUiState(isLoading = true)

            orderRepository.getRevenueSummary().fold(
                onSuccess = { summary ->
                    _uiState.value = RevenueReportUiState(
                        isLoading = false,
                        summary = summary
                    )
                },
                onFailure = { exception ->
                    _uiState.value = RevenueReportUiState(
                        isLoading = false,
                        error = exception.message ?: "Không thể tải doanh thu"
                    )
                }
            )
        }
    }
}