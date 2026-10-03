package com.example.agritech_mobile.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.agritech_mobile.MainActivity
import com.example.agritech_mobile.R
import com.example.agritech_mobile.data.repository.AuthRepository
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var authRepository: AuthRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "Nhận được token mới: $token")
        serviceScope.launch {
            authRepository.registerFcmToken(token)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val type = remoteMessage.data["type"]

        if (type == "ORDER_STATUS") {
            val title = remoteMessage.data["title"]
                ?: remoteMessage.notification?.title
                ?: "Cập nhật đơn hàng"
            val body = remoteMessage.data["body"]
                ?: remoteMessage.notification?.body
                ?: "Trạng thái đơn hàng của bạn đã có thay đổi."
            val status = remoteMessage.data["status"] ?: "PENDING"
            val orderId = remoteMessage.data["orderId"]?.toLongOrNull() ?: System.currentTimeMillis()

            showOrderStatusNotification(
                title = title,
                message = body,
                status = status,
                orderId = orderId
            )
        } else {
            val title = remoteMessage.notification?.title
                ?: remoteMessage.data["senderName"]
                ?: "Tin nhắn mới"
            val body = remoteMessage.notification?.body
                ?: "Bạn có tin nhắn mới"
            val roomId = remoteMessage.data["roomId"]

            showNotification(title, body, roomId)
        }
    }

    private fun showOrderStatusNotification(
        title: String,
        message: String,
        status: String,
        orderId: Long
    ) {
        val channelId = "order_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Thông báo đơn hàng",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Cập nhật tiến trình mua hàng và vận chuyển sản phẩm"
                enableLights(true)
                lightColor = Color.GREEN
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("targetScreen", "buyer_orders")
            putExtra("orderStatus", status)
            putExtra("orderId", orderId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            orderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setColor(0xFF1B5E20.toInt())
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        when (status) {
            "PENDING" -> {
                builder.setSubText("Chờ xác nhận")
            }
            "CONFIRMED" -> {
                builder.setSubText("Đã xác nhận")
            }
            "SHIPPING" -> {
                builder.setSubText("Đang giao hàng")
                builder.setProgress(0, 0, true)
            }
            "COMPLETED" -> {
                builder.setSubText("Giao thành công")
            }
            "CANCELLED" -> {
                builder.setSubText("Đã hủy")
            }
        }

        notificationManager.notify(orderId.toInt(), builder.build())
    }

    private fun showNotification(title: String, message: String, roomId: String?) {
        val channelId = "chat_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Thông báo tin nhắn",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("chatRoomId", roomId)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notifications_black_24dp)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}