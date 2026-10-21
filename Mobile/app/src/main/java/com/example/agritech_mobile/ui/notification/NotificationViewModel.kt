package com.example.agritech_mobile.ui.notification

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.remote.dto.InboxNotification
import com.example.agritech_mobile.data.repository.ChatRepository
import com.example.agritech_mobile.data.repository.NotificationRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class HomeBadgeViewModel @Inject constructor(
    private val notifications: NotificationRepository,
    chat: ChatRepository,
    private val tokens: TokenManager
) : ViewModel() {
    val unreadNotifications = notifications.unreadCount
    private var refreshJob: Job? = null
    private val firebaseUser = callbackFlow<String?> {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { trySend(it.currentUser?.uid) }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
    val unreadMessages = combine(firebaseUser, tokens.isLoggedIn) { userId, loggedIn -> userId.takeIf { loggedIn } }
        .distinctUntilChanged()
        .flatMapLatest { userId ->
            if (userId == null) flowOf(0L)
            else chat.observeUnreadCount(userId).onStart { emit(0L) }.retryWhen { _, attempt ->
                delay(if (attempt < 3) 3000L else 30000L)
                true
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    init { notifications.clearCount() }

    fun startUpdates() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            while (true) {
                if (!tokens.isLoggedIn.value) {
                    notifications.clearCount()
                    return@launch
                }
                try { notifications.refreshUnreadCount() }
                catch (e: CancellationException) { throw e }
                catch (_: Exception) { }
                delay(30000)
            }
        }
    }

    fun stopUpdates() { refreshJob?.cancel(); refreshJob = null }
}

data class NotificationUiState(
    val items: List<InboxNotification> = emptyList(),
    val isLoading: Boolean = false,
    val hasMore: Boolean = false,
    val error: String? = null,
    val busy: Boolean = false
)

@HiltViewModel
class NotificationViewModel @Inject constructor(private val repository: NotificationRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState = _uiState.asStateFlow()
    val unreadCount = repository.unreadCount
    private var nextPage = 0

    fun load(refresh: Boolean = false) {
        val state = _uiState.value
        if (state.isLoading || state.busy || (!refresh && nextPage > 0 && !state.hasMore)) return
        val page = if (refresh) 0 else nextPage
        _uiState.update { it.copy(isLoading = true, error = null) }
        viewModelScope.launch {
            try {
                val result = repository.getNotifications(page)
                nextPage = page + 1
                _uiState.update { it.copy(items = ((if (refresh) emptyList() else it.items) + result.items).distinctBy { item -> item.id },
                    isLoading = false, hasMore = result.hasMore) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(isLoading = false, error = "Không tải được thông báo. Vui lòng thử lại.") } }
        }
    }

    fun open(item: InboxNotification, onOpen: (InboxNotification) -> Unit) {
        if (_uiState.value.busy || _uiState.value.isLoading) return
        if (item.read) { onOpen(item); return }
        _uiState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                repository.markRead(item.id)
                _uiState.update { it.copy(busy = false, items = it.items.map { row -> if (row.id == item.id) row.copy(read = true) else row }) }
                onOpen(item)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(busy = false, error = "Không cập nhật được thông báo. Vui lòng thử lại.") } }
        }
    }

    fun readAll() {
        val state = _uiState.value
        val upToId = state.items.maxOfOrNull { it.id } ?: return
        if (state.busy || state.isLoading) return
        _uiState.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            try {
                repository.markAllRead(upToId)
                _uiState.update { it.copy(busy = false, items = it.items.map { row -> row.copy(read = true) }) }
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { _uiState.update { it.copy(busy = false, error = "Không đánh dấu đã đọc được. Vui lòng thử lại.") } }
        }
    }
}
