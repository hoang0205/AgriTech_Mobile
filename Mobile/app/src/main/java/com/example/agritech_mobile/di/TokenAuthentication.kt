package com.example.agritech_mobile.di

import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.local.SessionTokens
import com.example.agritech_mobile.data.remote.AuthApiService
import com.example.agritech_mobile.data.remote.dto.RefreshTokenRequest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.CancellationException
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import javax.inject.Inject
import javax.inject.Named

class TokenAuthenticator @Inject constructor(
    private val tokenManager: TokenManager,
    @Named("refreshAuth") private val authService: AuthApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.url.encodedPath.startsWith("/api/auth/")) return null
        var previous = response.priorResponse
        while (previous != null) {
            if (previous.code == 401) return null
            previous = previous.priorResponse
        }
        val requestSession = response.request.tag(SessionTokens::class.java) ?: return null

        synchronized(this) {
            val session = tokenManager.sessionTokens()
            if (session.generation != requestSession.generation) return null
            val currentAccessToken = session.accessToken
            if (currentAccessToken.isNullOrBlank()) return null
            val requestAccessToken = requestSession.accessToken

            if (currentAccessToken != requestAccessToken) {
                return response.request.newBuilder()
                    .header("Authorization", "Bearer $currentAccessToken")
                    .tag(SessionTokens::class.java, session)
                    .build()
            }

            val refreshToken = session.refreshToken
            if (refreshToken.isNullOrEmpty()) {
                tokenManager.clearSessionIfCurrent(session)
                return null
            }

            return runBlocking {
                try {
                    val refreshResponse = authService.refreshToken(RefreshTokenRequest(refreshToken))

                    if (refreshResponse.isSuccessful) {
                        val newTokens = refreshResponse.body()
                        if (newTokens != null && newTokens.accessToken.isNotBlank() && newTokens.refreshToken.isNotBlank()) {
                            val rotated = tokenManager.rotateTokens(
                                expected = session,
                                accessToken = newTokens.accessToken,
                                refreshToken = newTokens.refreshToken,
                                role = newTokens.role
                            )

                            if (!rotated) {
                                return@runBlocking null
                            }

                            response.request.newBuilder()
                                .header("Authorization", "Bearer ${newTokens.accessToken}")
                                .tag(SessionTokens::class.java, session.copy(
                                    accessToken = newTokens.accessToken,
                                    refreshToken = newTokens.refreshToken
                                ))
                                .build()
                        } else {
                            null
                        }
                    } else {
                        if (refreshResponse.code() == 401 || refreshResponse.code() == 403) {
                            tokenManager.clearSessionIfCurrent(session)
                        }
                        null
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    null
                }
            }
        }
    }
}
