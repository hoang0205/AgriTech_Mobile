package com.example.agritech_mobile.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.dto.ProductReportResponse
import com.example.agritech_mobile.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportHistoryUiState(
    val reports: List<ProductReportResponse> = emptyList(),
    val isLoading: Boolean = false,
    val page: Int = 0,
    val totalPages: Int = 0,
    val error: String? = null
)

@HiltViewModel
class ReportHistoryViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadPage(_uiState.value.page)
    }

    fun previousPage() {
        val state = _uiState.value
        if (state.page > 0) {
            loadPage(state.page - 1)
        }
    }

    fun nextPage() {
        val state = _uiState.value
        if (state.page + 1 < state.totalPages) {
            loadPage(state.page + 1)
        }
    }

    private fun loadPage(page: Int) {
        if (_uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            reportRepository.getMyReports(page = page).fold(
                onSuccess = { response ->
                    _uiState.value = ReportHistoryUiState(
                        reports = response.content,
                        page = page,
                        totalPages = response.totalPages
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                            ?: "Không tải được lịch sử báo cáo"
                    )
                }
            )
        }
    }
}