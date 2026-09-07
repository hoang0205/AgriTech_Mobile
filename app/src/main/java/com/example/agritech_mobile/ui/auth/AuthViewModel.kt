package com.example.agritech_mobile.ui.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.internal.notify
import javax.inject.Inject

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Success(val message: String, val token: String = "") : AuthState()
    data class Error(val error: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun login(phone: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.login(phone, password)
            result.onSuccess { response ->
                tokenManager.saveTokens(response.accessToken, response.refreshToken)
                tokenManager.saveUserName(response.fullName)

                if (!response.avatarUrl.isNullOrBlank()) {
                    tokenManager.saveAvatarUrl(response.avatarUrl)
                } else {
                    tokenManager.saveAvatarUrl("")
                }

                _authState.value = AuthState.Success("Thành công", response.accessToken)
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Lỗi không xác định")
                Log.d("AuthViewModel", "Error response: $exception")
            }
        }
    }

    fun register(
        fullName: String,
        phone: String,
        email: String,
        password: String
    ) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.register(fullName, phone, email, password)
            result.onSuccess { response ->
                if (response.success) {
                    _authState.value = AuthState.Success(response.message)
                } else {
                    _authState.value = AuthState.Error(response.message)
                }
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Lỗi hệ thống")
                Log.e("AuthViewModel", "Register Error: ${exception.message}")
            }
        }
    }

    fun requestResetPassword(
        email: String
    ) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.requestResetPassword(email)
            result.onSuccess { response ->
                if (response.success) {
                    _authState.value = AuthState.Success(response.message)
                } else {
                    _authState.value = AuthState.Error(response.message)
                }
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun verifyEmail(
        email: String,
        otp: String
    ) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.verifyEmail(email, otp)
            result.onSuccess { response ->
                if (response.success) {
                    _authState.value = AuthState.Success(response.message)
                } else {
                    _authState.value = AuthState.Error(response.message)
                }
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }

    fun resetPassword(
        email: String,
        newPassword: String
    ) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val result = repository.resetPassword(email, newPassword)
            result.onSuccess { response ->
                if (response.success) {
                    _authState.value = AuthState.Success(response.message)
                } else {
                    _authState.value = AuthState.Error(response.message)
                }
            }.onFailure { exception ->
                _authState.value = AuthState.Error(exception.message ?: "Lỗi hệ thống")
            }
        }
    }
}