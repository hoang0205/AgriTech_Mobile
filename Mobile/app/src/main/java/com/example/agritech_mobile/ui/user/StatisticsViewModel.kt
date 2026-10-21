package com.example.agritech_mobile.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.dto.StatisticsOverviewDto
import com.example.agritech_mobile.data.remote.dto.StatisticsSectionDto
import com.example.agritech_mobile.data.repository.StatisticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.util.Calendar
import java.util.TimeZone
import javax.inject.Inject

enum class StatisticsTab(val title: String) {
    REVENUE("Doanh thu"),
    PROFIT("Lợi nhuận"),
    SPENDING("Chi tiêu")
}

data class StatisticsUiState(
    val selectedYear: Int,
    val selectedMonth: Int? = null,
    val selectedTab: StatisticsTab = StatisticsTab.SPENDING,
    val isLoading: Boolean = false,
    val overview: StatisticsOverviewDto? = null,
    val error: String? = null
) {
    val availableTabs: List<StatisticsTab>
        get() = listOf(
            StatisticsTab.REVENUE,
            StatisticsTab.PROFIT,
            StatisticsTab.SPENDING
        )

    val activeSection: StatisticsSectionDto?
        get() = when (selectedTab) {
            StatisticsTab.REVENUE -> overview?.revenue
            StatisticsTab.PROFIT -> overview?.profit
            StatisticsTab.SPENDING -> overview?.spending
        }
}

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val repository: StatisticsRepository
) : ViewModel() {

    val currentYear: Int = Calendar.getInstance(
        TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
    ).get(Calendar.YEAR)

    private val _uiState = MutableStateFlow(
        StatisticsUiState(
            selectedYear = currentYear
        )
    )

    val uiState = _uiState.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadOverview(
            year = currentYear,
            month = null
        )
    }

    fun selectTab(tab: StatisticsTab) {
        if (tab !in _uiState.value.availableTabs) {
            return
        }

        _uiState.update {
            it.copy(selectedTab = tab)
        }
    }

    fun selectYear(year: Int) {
        if (year !in 1..currentYear) {
            return
        }

        if (year == _uiState.value.selectedYear) {
            return
        }

        loadOverview(
            year = year,
            month = null
        )
    }

    fun selectMonth(month: Int?) {
        if (month != null && month !in 1..12) {
            return
        }

        val current = _uiState.value

        if (month == current.selectedMonth) {
            return
        }

        loadOverview(
            year = current.selectedYear,
            month = month
        )
    }

    fun retry() {
        val current = _uiState.value

        if (current.isLoading) {
            return
        }

        loadOverview(
            year = current.selectedYear,
            month = current.selectedMonth
        )
    }

    private fun loadOverview(
        year: Int,
        month: Int?
    ) {
        loadJob?.cancel()

        _uiState.update {
            it.copy(
                selectedYear = year,
                selectedMonth = month,
                isLoading = true,
                error = null
            )
        }

        loadJob = viewModelScope.launch {
            try {
                val overview = repository.getOverview(
                    year = year,
                    month = month
                )

                coroutineContext.ensureActive()

                _uiState.update { state ->
                    state.copy(
                        selectedYear = year,
                        selectedMonth = month,
                        isLoading = false,
                        overview = overview,
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                coroutineContext.ensureActive()

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMessage(e)
                    )
                }
            }
        }
    }

    private fun errorMessage(exception: Exception): String {
        return when (exception) {
            is HttpException -> when (exception.code()) {
                400 -> "Năm hoặc tháng thống kê không hợp lệ."
                401 -> "Phiên đăng nhập hết hạn. Vui lòng đăng nhập lại."
                403 -> "Bạn không có quyền xem thống kê này."
                404 -> "Không tìm thấy API thống kê. Kiểm tra backend."
                else -> "Không tải được thống kê. " +
                        "Mã lỗi: ${exception.code()}."
            }

            is IOException ->
                "Không kết nối được máy chủ. Vui lòng thử lại."

            else ->
                "Không tải được thống kê. Vui lòng thử lại."
        }
    }
}