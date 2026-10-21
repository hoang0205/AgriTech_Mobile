package com.example.agritech_mobile.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportEligibilityUiState(
    val productId: String = "",
    val canReport: Boolean = false,
    val isLoading: Boolean = false,
    val message: String? = null
)

@HiltViewModel
class ReportEligibilityViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportEligibilityUiState())
    val uiState = _uiState.asStateFlow()

    private var checkJob: Job? = null

    fun checkEligibility(productId: String) {
        checkJob?.cancel()

        _uiState.value = ReportEligibilityUiState(
            productId = productId,
            isLoading = true
        )

        checkJob = viewModelScope.launch {
            reportRepository.checkEligibility(productId).fold(
                onSuccess = { result ->
                    _uiState.value = ReportEligibilityUiState(
                        productId = productId,
                        canReport = result.canReport,
                        message = result.message
                    )
                },
                onFailure = { exception ->
                    _uiState.value = ReportEligibilityUiState(
                        productId = productId,
                        canReport = false,
                        message = exception.message
                            ?: "Không kiểm tra được quyền báo cáo"
                    )
                }
            )
        }
    }
}