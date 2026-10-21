package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.remote.NotificationApiService
import com.example.agritech_mobile.data.remote.dto.ReadAllNotificationsRequest
import com.example.agritech_mobile.data.remote.dto.NotificationInboxResponse
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import retrofit2.HttpException
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicLong

@Singleton
class NotificationRepository @Inject constructor(private val api: NotificationApiService, private val tokens: TokenManager) {
    private val _unreadCount = MutableStateFlow(0L)
    val unreadCount = _unreadCount.asStateFlow()
    private val countRevision = AtomicLong(0)
    private val countLock = Any()

    fun clearCount() = synchronized(countLock) { countRevision.incrementAndGet(); _unreadCount.value = 0 }

    private fun publishCount(count: Long, generation: Long, revision: Long? = null): Unit = synchronized(countLock) {
        if (tokens.sessionTokens().generation == generation && (revision == null || countRevision.get() == revision)) {
            if (revision == null) countRevision.incrementAndGet()
            _unreadCount.value = count.coerceAtLeast(0)
        }
    }

    suspend fun refreshUnreadCount() {
        val session = tokens.sessionTokens()
        val revision = countRevision.get()
        val result = body(api.unreadCount())
        publishCount(result.count, session.generation, revision)
    }

    suspend fun getNotifications(page: Int): NotificationInboxResponse {
        val session = tokens.sessionTokens()
        val revision = countRevision.get()
        val result = body(api.getNotifications(page))
        publishCount(result.unreadCount, session.generation, revision)
        return result
    }

    suspend fun markRead(id: Long) {
        val session = tokens.sessionTokens()
        val result = body(api.markRead(id))
        publishCount(result.count, session.generation)
    }

    suspend fun markAllRead(upToId: Long) {
        val session = tokens.sessionTokens()
        val result = body(api.markAllRead(ReadAllNotificationsRequest(upToId)))
        publishCount(result.count, session.generation)
    }

    private suspend fun <T> body(response: Response<T>): T {
        currentCoroutineContext().ensureActive()
        if (!response.isSuccessful) throw HttpException(response)
        return response.body() ?: error("Máy chủ không trả về thông báo")
    }
}
