package com.example.agritech_mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TokenManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences("agritech_prefs", Context.MODE_PRIVATE)

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
    }

    fun getUserName(): String {
        return prefs.getString("USER_NAME", "Khách") ?: "Khách"
    }

    fun saveAvatarUrl(url: String) {
        prefs.edit().putString("AVATAR_URL", url).apply()
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