package com.example.agritech_mobile.di

import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.remote.AuthApiService
import com.example.agritech_mobile.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Provider

class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    private val apiServiceProvider: Provider<AuthApiService>
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.contains("/api/auth/refresh-token")) {
            tokenManager.clearTokens()
            return null
        }

        synchronized(this) {
            val currentAccessToken = tokenManager.getAccessToken()
            val requestAccessToken = response.request.header("Authorization")?.removePrefix("Bearer ")

            if (currentAccessToken != null && currentAccessToken != requestAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .build()
            }

            val refreshToken = tokenManager.getRefreshToken()
            if (refreshToken.isNullOrEmpty()) {
                tokenManager.clearTokens()
                return null
            }

            return runBlocking {
                try {
                    val authService = apiServiceProvider.get()
                    val refreshResponse = authService.refreshToken(RefreshTokenRequest(refreshToken))

                    if (refreshResponse.isSuccessful) {
                        val newTokens = refreshResponse.body()
                        if (newTokens != null) {
                            tokenManager.saveTokens(newTokens.accessToken, newTokens.refreshToken)

                            response.request.newBuilder()
                                .header("Authorization", "Bearer ${newTokens.accessToken}")
                                .build()
                        } else {
                            tokenManager.clearTokens()
                            null
                        }
                    } else {
                        tokenManager.clearTokens()
                        null
                    }
                } catch (e: Exception) {
                    tokenManager.clearTokens()
                    null
                }
            }
        }
    }
}