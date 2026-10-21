package com.example.agritech_mobile.data.remote.dto

data class InboxNotification(
    val id: Long,
    val title: String,
    val body: String,
    val orderId: Long,
    val status: String,
    val targetScreen: String,
    val createdAt: String,
    val read: Boolean
)

data class NotificationInboxResponse(val items: List<InboxNotification>, val hasMore: Boolean, val unreadCount: Long)
data class UnreadNotificationCount(val count: Long)
data class ReadAllNotificationsRequest(val upToId: Long)
