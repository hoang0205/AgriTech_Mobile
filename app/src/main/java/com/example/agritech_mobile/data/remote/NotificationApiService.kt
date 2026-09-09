package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.FcmTokenRequest
import com.example.agritech_mobile.data.remote.dto.SendNotificationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT

interface NotificationApiService {

    @PUT("/api/users/fcm-token")
    suspend fun updateFcmToken(
        @Body request: FcmTokenRequest
    ): Response<Map<String, String>>

    @POST("/api/notifications/send-chat")
    suspend fun sendChatNotification(
        @Body request: SendNotificationRequest
    ): Response<Map<String, String>>
}