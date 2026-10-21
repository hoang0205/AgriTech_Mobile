package com.example.agritech_mobile.data.remote

import com.example.agritech_mobile.data.remote.dto.FcmTokenRequest
import com.example.agritech_mobile.data.remote.dto.SendNotificationRequest
import com.example.agritech_mobile.data.remote.dto.NotificationInboxResponse
import com.example.agritech_mobile.data.remote.dto.UnreadNotificationCount
import com.example.agritech_mobile.data.remote.dto.ReadAllNotificationsRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface NotificationApiService {

    @GET("api/notifications")
    suspend fun getNotifications(@Query("page") page: Int, @Query("size") size: Int = 20): Response<NotificationInboxResponse>

    @GET("api/notifications/unread-count")
    suspend fun unreadCount(): Response<UnreadNotificationCount>

    @POST("api/notifications/{id}/read")
    suspend fun markRead(@Path("id") id: Long): Response<UnreadNotificationCount>

    @POST("api/notifications/read-all")
    suspend fun markAllRead(@Body request: ReadAllNotificationsRequest): Response<UnreadNotificationCount>

    @PUT("/api/user/fcm-token")
    suspend fun updateFcmToken(
        @Body request: FcmTokenRequest
    ): Response<Map<String, String>>

    @POST("/api/notifications/send-chat")
    suspend fun sendChatNotification(
        @Body request: SendNotificationRequest
    ): Response<Map<String, String>>
}
