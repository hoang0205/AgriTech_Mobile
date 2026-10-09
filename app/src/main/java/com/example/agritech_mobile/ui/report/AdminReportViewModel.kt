package com.example.agritech_mobile.ui.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.dto.ProductReportResponse
import com.example.agritech_mobile.data.remote.dto.ReportStatus
import com.example.agritech_mobile.data.remote.dto.UpdateProductReportRequest
import com.example.agritech_mobile.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AdminReportUiState(
    val reports: List<ProductReportResponse> = emptyList(),
    val filter: ReportStatus? = ReportStatus.PENDING,
    val page: Int = 0,
    val totalPages: Int = 0,
    val isLoading: Boolean = false,
    val isSubmitting: Boolean = false,
    val selectedReport: ProductReportResponse? = null,
    val adminReply: String = "",
    val error: String? = null,
    val successMessage: String? = null
) {
    val isBusy: Boolean
        get() = isLoading || isSubmitting
}

@HiltViewModel
class AdminReportViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminReportUiState())
    val uiState = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        loadPage(_uiState.value.page)
    }

    fun selectFilter(status: ReportStatus?) {
        val state = _uiState.value
        if (state.isBusy || state.filter == status) return

        _uiState.value = state.copy(
            filter = status,
            reports = emptyList(),
            page = 0,
            totalPages = 0,
            selectedReport = null,
            adminReply = "",
            error = null,
            successMessage = null
        )

        loadPage(0)
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

    fun selectReport(reportId: Long) {
        val state = _uiState.value
        if (state.isBusy) return

        val report = state.reports.firstOrNull {
            it.id == reportId
        } ?: return

        _uiState.value = state.copy(
            selectedReport = report,
            adminReply = report.adminReply.orEmpty(),
            error = null,
            successMessage = null
        )
    }

    fun dismissReport() {
        if (_uiState.value.isSubmitting) return

        _uiState.value = _uiState.value.copy(
            selectedReport = null,
            adminReply = "",
            error = null
        )
    }

    fun onReplyChange(text: String) {
        if (_uiState.value.isBusy) return

        _uiState.value = _uiState.value.copy(
            adminReply = text.take(2000),
            error = null,
            successMessage = null
        )
    }

    fun updateStatus(status: ReportStatus) {
        val state = _uiState.value
        if (state.isBusy) return

        val report = state.selectedReport ?: return
        val reply = state.adminReply.trim()

        val validationError = when {
            report.status == ReportStatus.RESOLVED.name ||
                    report.status == ReportStatus.REJECTED.name ->
                "Báo cáo đã được đóng"

            status == ReportStatus.PENDING ->
                "Không thể chuyển về trạng thái chờ xử lý"

            (status == ReportStatus.RESOLVED ||
                    status == ReportStatus.REJECTED) &&
                    reply.isBlank() ->
                "Cần nhập phản hồi khi giải quyết hoặc từ chối"

            else -> null
        }

        if (validationError != null) {
            _uiState.value = state.copy(
                error = validationError
            )
            return
        }

        _uiState.value = state.copy(
            isSubmitting = true,
            error = null,
            successMessage = null
        )

        viewModelScope.launch {
            val request = UpdateProductReportRequest(
                status = status,
                adminReply = reply.takeIf { it.isNotEmpty() }
            )

            reportRepository.updateReport(report.id, request).fold(
                onSuccess = { updatedReport ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        selectedReport = updatedReport,
                        adminReply = updatedReport.adminReply.orEmpty(),
                        successMessage = "Đã cập nhật báo cáo"
                    )

                    loadPage(0)
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isSubmitting = false,
                        error = exception.message
                            ?: "Không cập nhật được báo cáo"
                    )
                }
            )
        }
    }

    private fun loadPage(page: Int) {
        val state = _uiState.value
        if (state.isBusy || page < 0) return

        _uiState.value = state.copy(
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            var requestedPage = page
            var result = reportRepository.getAdminReports(
                page = requestedPage,
                status = state.filter
            )

            result.getOrNull()?.let { response ->
                val lastPage = (response.totalPages - 1).coerceAtLeast(0)

                if (requestedPage > lastPage) {
                    requestedPage = lastPage
                    result = reportRepository.getAdminReports(
                        page = requestedPage,
                        status = state.filter
                    )
                }
            }

            result.fold(
                onSuccess = { response ->
                    _uiState.value = _uiState.value.copy(
                        reports = response.content,
                        page = requestedPage,
                        totalPages = response.totalPages,
                        isLoading = false
                    )
                },
                onFailure = { exception ->
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = exception.message
                            ?: "Không tải được danh sách báo cáo"
                    )
                }
            )
        }
    }
}