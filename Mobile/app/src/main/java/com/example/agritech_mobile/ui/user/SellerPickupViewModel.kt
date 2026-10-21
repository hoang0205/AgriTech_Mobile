package com.example.agritech_mobile.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SellerPickupUiState(
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val loadFailed: Boolean = false,
    val error: String? = null,
    val saved: Boolean = false
)

@HiltViewModel
class SellerPickupViewModel @Inject constructor(
    private val repository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SellerPickupUiState())
    val uiState = _uiState.asStateFlow()

    private var lastSelectionId: String? = null

    init {
        load()
    }

    fun load() {
        if (_uiState.value.isLoading || _uiState.value.isSaving) return

        _uiState.update {
            it.copy(isLoading = true, error = null)
        }

        viewModelScope.launch {
            repository.getSellerPickupLocation()
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            address = response.address.orEmpty(),
                            latitude = response.latitude,
                            longitude = response.longitude,
                            isLoading = false,
                            loadFailed = false,
                            error = null
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadFailed = true,
                            error = exception.message
                                ?: "Không tải được địa điểm lấy hàng"
                        )
                    }
                }
        }
    }

    fun changeAddress(address: String) {
        if (_uiState.value.isSaving) return

        _uiState.update {
            it.copy(
                address = address,
                latitude = null,
                longitude = null,
                error = null,
                saved = false
            )
        }
    }

    fun applyPickedLocation(
        selectionId: String,
        address: String,
        latitude: Double,
        longitude: Double
    ) {
        if (selectionId == lastSelectionId) return
        if (_uiState.value.isLoading || _uiState.value.isSaving) return

        lastSelectionId = selectionId

        _uiState.update {
            it.copy(
                address = address,
                latitude = latitude,
                longitude = longitude,
                error = null,
                saved = false
            )
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving || state.loadFailed) return

        val address = state.address.trim()
        val latitude = state.latitude
        val longitude = state.longitude

        val error = when {
            address.isBlank() -> "Vui lòng nhập địa chỉ lấy hàng"
            address.length > 1000 -> "Địa chỉ không được vượt quá 1000 ký tự"
            latitude == null || longitude == null ->
                "Vui lòng chọn vị trí lấy hàng trên bản đồ"
            !latitude.isFinite() || !longitude.isFinite() ||
                    latitude !in -90.0..90.0 ||
                    longitude !in -180.0..180.0 ->
                "Tọa độ lấy hàng không hợp lệ"
            else -> null
        }

        if (error != null) {
            _uiState.update { it.copy(error = error) }
            return
        }

        _uiState.update {
            it.copy(isSaving = true, error = null, saved = false)
        }

        viewModelScope.launch {
            repository.updateSellerPickupLocation(
                address = address,
                latitude = requireNotNull(latitude),
                longitude = requireNotNull(longitude)
            ).onSuccess { response ->
                _uiState.update {
                    it.copy(
                        address = response.address.orEmpty(),
                        latitude = response.latitude,
                        longitude = response.longitude,
                        isSaving = false,
                        saved = true
                    )
                }
            }.onFailure { exception ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        error = exception.message
                            ?: "Không lưu được địa điểm lấy hàng"
                    )
                }
            }
        }
    }

    fun clearSaved() {
        _uiState.update { it.copy(saved = false) }
    }
}