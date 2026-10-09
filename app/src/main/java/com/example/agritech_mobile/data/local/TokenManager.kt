package com.example.agritech_mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SessionTokens(val accessToken: String?, val refreshToken: String?, val generation: Long)

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("agritech_prefs", Context.MODE_PRIVATE)
    private var generation = 0L
    private val _isLoggedIn = MutableStateFlow(!prefs.getString("ACCESS_TOKEN", null).isNullOrBlank())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userNameFlow = MutableStateFlow(prefs.getString("USER_NAME", "Khách") ?: "Khách")
    val userNameFlow: StateFlow<String> = _userNameFlow.asStateFlow()

    private val _avatarUrlFlow = MutableStateFlow(prefs.getString("AVATAR_URL", "") ?: "")
    val avatarUrlFlow: StateFlow<String> = _avatarUrlFlow.asStateFlow()

    private val _isAdmin = MutableStateFlow(
        _isLoggedIn.value &&
                prefs.getString("USER_ROLE", null) == "ADMIN"
    )
    val isAdmin: StateFlow<Boolean> = _isAdmin.asStateFlow()

    @Synchronized
    fun saveTokens(
        accessToken: String,
        refreshToken: String,
        role: String? = null
    ) {
        generation++
        writeTokens(accessToken, refreshToken, role)
    }

    private fun writeTokens(
        accessToken: String,
        refreshToken: String,
        role: String?
    ) {
        val savedRole = role ?: ""

        prefs.edit().apply {
            putString("ACCESS_TOKEN", accessToken)
            putString("REFRESH_TOKEN", refreshToken)
            putString("USER_ROLE", savedRole)
            apply()
        }

        _isAdmin.value =
            accessToken.isNotBlank() && savedRole == "ADMIN"

        _isLoggedIn.value = accessToken.isNotBlank()
        Log.d(
            "AdminRole",
            "Received role=$role, savedRole=$savedRole, isAdmin=${_isAdmin.value}"
        )
    }

    @Synchronized
    fun rotateTokens(
        expected: SessionTokens,
        accessToken: String,
        refreshToken: String,
        role: String?
    ): Boolean {
        if (sessionTokens() != expected) return false

        writeTokens(accessToken, refreshToken, role)
        return true
    }

    @Synchronized
    fun sessionTokens(): SessionTokens = SessionTokens(getAccessToken(), getRefreshToken(), generation)

    @Synchronized
    fun clearSessionIfCurrent(expected: SessionTokens) {
        if (sessionTokens() == expected) clearTokens()
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

    @Synchronized
    fun clearTokens() {
        generation++
        prefs.edit().clear().apply()
        _userNameFlow.value = "Khách"
        _avatarUrlFlow.value = ""
        _isAdmin.value = false
        _isLoggedIn.value = false
    }

    fun clearAll() {
        clearTokens()
    }
}
