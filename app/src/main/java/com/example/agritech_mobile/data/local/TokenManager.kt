package com.example.agritech_mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.apply

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("agritech_prefs", Context.MODE_PRIVATE)

    private val _userNameFlow = MutableStateFlow(prefs.getString("USER_NAME", "Khách") ?: "Khách")
    val userNameFlow: StateFlow<String> = _userNameFlow.asStateFlow()

    private val _avatarUrlFlow = MutableStateFlow(prefs.getString("AVATAR_URL", "") ?: "")
    val avatarUrlFlow: StateFlow<String> = _avatarUrlFlow.asStateFlow()

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit().apply {
            putString("ACCESS_TOKEN", accessToken)
            putString("REFRESH_TOKEN", refreshToken)
            apply()
        }
    }

    fun getAccessToken(): String? = prefs.getString("ACCESS_TOKEN", null)

    fun getRefreshToken(): String? = prefs.getString("REFRESH_TOKEN", null)

    fun saveUserName(name: String) {
        prefs.edit().putString("USER_NAME", name).apply()
        _userNameFlow.value = name
    }

    fun getUserName(): String {
        return prefs.getString("USER_NAME", "Khách") ?: "Khách"
    }

    fun saveAvatarUrl(url: String?) {
        val finalUrl = url ?: ""
        prefs.edit().putString("AVATAR_URL", finalUrl).apply()
        _avatarUrlFlow.value = finalUrl
    }

    fun getAvatarUrl(): String {
        return prefs.getString("AVATAR_URL", "") ?: ""
    }

    fun clearTokens() {
        prefs.edit().clear().apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}