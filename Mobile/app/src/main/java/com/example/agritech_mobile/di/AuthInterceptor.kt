package com.example.agritech_mobile.di

import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.local.SessionTokens
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        if (request.url.encodedPath.contains("/api/auth/")) {
            return chain.proceed(request)
        }

        val requestBuilder = request.newBuilder()
        val session = tokenManager.sessionTokens()
        val accessToken = session.accessToken

        if (!accessToken.isNullOrEmpty()) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
                .tag(SessionTokens::class.java, session)
        }

        return chain.proceed(requestBuilder.build())
    }
}
