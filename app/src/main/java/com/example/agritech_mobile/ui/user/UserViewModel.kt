package com.example.agritech_mobile.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.remote.dto.ProvinceResponse
import com.example.agritech_mobile.data.remote.dto.ShippingDetails
import com.example.agritech_mobile.data.remote.dto.WardResponse
import com.example.agritech_mobile.data.repository.AuthRepository
import com.example.agritech_mobile.data.repository.UploadRepository
import com.example.agritech_mobile.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import javax.inject.Inject

sealed class AddressState {
    object Idle : AddressState()
    object Loading : AddressState()
    data class Success(val addresses: List<ShippingDetails>) : AddressState()
    data class Error(val message: String) : AddressState()
}

sealed class UserState {
    object Idle : UserState()
    object Loading : UserState()
    data class Success(val message: String) : UserState()
    data class Error(val message: String) : UserState()
}

sealed class UserProfileState {
    object Loading : UserProfileState()
    data class Success(val fullName: String, val avatarUrl: String) : UserProfileState()
    data class Error(val message: String) : UserProfileState()
}

@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val uploadRepository: UploadRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _addressState = MutableStateFlow<AddressState>(AddressState.Idle)
    val addressState: StateFlow<AddressState> = _addressState.asStateFlow()

    private val _userState = MutableStateFlow<UserState>(UserState.Idle)
    val userState: StateFlow<UserState> = _userState.asStateFlow()

    private val _profileState = MutableStateFlow<UserProfileState>(UserProfileState.Loading)
    val profileState: StateFlow<UserProfileState> = _profileState.asStateFlow()

    init {
        getUser()
    }

    private val _provinces = MutableStateFlow<List<ProvinceResponse>>(emptyList())
    val provinces: StateFlow<List<ProvinceResponse>> = _provinces.asStateFlow()

    private val _wards = MutableStateFlow<List<WardResponse>>(emptyList())
    val wards: StateFlow<List<WardResponse>> = _wards.asStateFlow()


    fun getUserAddresses() {
        _addressState.value = AddressState.Loading
        viewModelScope.launch {
            val result = userRepository.getUserAddress()
            result.onSuccess { addresses ->
                _addressState.value = AddressState.Success(addresses)
            }.onFailure { exception ->
                _addressState.value = AddressState.Error(exception.message ?: "Lỗi tải địa chỉ")
            }
        }
    }

    fun addNewAddress(name: String, phone: String, address: String, isDefault: Boolean = false) {
        _addressState.value = AddressState.Loading
        viewModelScope.launch {
            val result = userRepository.addUserAddress(name, phone, address, isDefault)
            result.onSuccess {
                getUserAddresses()
            }.onFailure { exception ->
                _addressState.value = AddressState.Error(exception.message ?: "Lỗi thêm địa chỉ")
            }
        }
    }


    fun setDefaultAddress(addressId: String) {
        _addressState.value = AddressState.Loading
        viewModelScope.launch {
            val result = userRepository.setDefaultAddress(addressId)
            result.onSuccess {
                getUserAddresses()
            }.onFailure { exception ->
                _addressState.value =
                    AddressState.Error(exception.message ?: "Lỗi cập nhật địa chỉ")
            }
        }
    }


    fun getProvinces() {
        viewModelScope.launch {
            val result = userRepository.getProvinces()
            result.onSuccess { provinceList ->
                _provinces.value = provinceList
            }.onFailure { exception ->
                _provinces.value = emptyList()
            }
        }
    }

    fun getWards(provinceCode: String) {
        viewModelScope.launch {
            val result = userRepository.getWards(provinceCode)
            result.onSuccess { wardList ->
                _wards.value = wardList
            }.onFailure { exception ->
                _wards.value = emptyList()
            }
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String, confirmPassword: String) {
        _userState.value = UserState.Loading
        viewModelScope.launch {
            val result =
                userRepository.updateUserPassword(currentPassword, newPassword, confirmPassword)
            result.onSuccess {
                _userState.value = UserState.Success("Cập nhật mật khẩu thành công")
            }.onFailure { exception ->
                _userState.value = UserState.Error(exception.message ?: "Lỗi cập nhật mật khẩu")
            }
        }
    }

    fun getUser() {
        _profileState.value = UserProfileState.Loading
        viewModelScope.launch {
            val result = userRepository.getUserProfile()
            result.onSuccess { profile ->
                _profileState.value = UserProfileState.Success(
                    fullName = profile.fullName,
                    avatarUrl = profile.avatarUrl
                )
            }.onFailure { exception ->
                _profileState.value = UserProfileState.Error(exception.message ?: "Lỗi tải thông tin")
            }
        }
    }

    fun updateProfileWithImage(
        fullName: String,
        imagePart: MultipartBody.Part?,
        currentAvatarUrl: String
    ) {
        _userState.value = UserState.Loading
        viewModelScope.launch {
            var finalAvatarUrl = currentAvatarUrl

            if (imagePart != null) {
                try {
                    val uploadResult = uploadRepository.uploadImages(listOf(imagePart))
                    if (uploadResult.isSuccess) {
                        val urls = uploadResult.getOrNull()
                        if (!urls.isNullOrEmpty()) {
                            finalAvatarUrl = urls.first()
                        }
                    } else {
                        _userState.value = UserState.Error("Lỗi tải ảnh lên server")
                        return@launch
                    }
                } catch (e: Exception) {
                    _userState.value = UserState.Error("Lỗi tải ảnh: ${e.message}")
                    return@launch
                }
            }

            val result = userRepository.updateUserProfile(fullName, finalAvatarUrl)
            result.onSuccess {
                tokenManager.saveAvatarUrl(finalAvatarUrl)
                tokenManager.saveUserName(fullName)
                _userState.value = UserState.Success("Cập nhật thông tin thành công")
                getUser()
            }.onFailure { exception ->
                _userState.value = UserState.Error(exception.message ?: "Lỗi cập nhật thông tin")
            }
        }
    }

    fun performLogout(onNavigateToLogin: () -> Unit) {
        viewModelScope.launch {
            val token = tokenManager.getAccessToken()

            if (!token.isNullOrEmpty()) {
                try {
                    authRepository.logout(token)
                } catch (e: Exception) {
                }
            }

            tokenManager.clearAll()

            onNavigateToLogin()
        }
    }

    fun clearUserState() {
        _userState.value = UserState.Idle
    }
}