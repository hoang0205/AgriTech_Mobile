package com.example.agritech_mobile.ui.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.NotificationApiService
import com.example.agritech_mobile.data.remote.dto.ChatMessage
import com.example.agritech_mobile.data.remote.dto.SendNotificationRequest
import com.example.agritech_mobile.data.repository.ChatRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChatUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)

data class ChatRoomItem(
    val roomId: String = "",
    val partnerId: String = "",
    val partnerName: String = "",
    val partnerAvatar: String? = null,
    val lastMessage: String = "",
    val lastUpdated: Long = System.currentTimeMillis(),
    val unreadCount: Int = 0
)

data class ChatListUiState(
    val chatRooms: List<ChatRoomItem> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val notificationApiService: NotificationApiService
) : ViewModel() {

    private val _chatListUiState = MutableStateFlow(ChatListUiState())
    val chatListUiState: StateFlow<ChatListUiState> = _chatListUiState.asStateFlow()

    fun loadChatRooms() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("CHAT_DEBUG", "[loadChatRooms] Bắt đầu tải danh sách. CurrentUser UID = $currentUserId")

        if (currentUserId.isNullOrBlank()) {
            Log.e("CHAT_DEBUG", "[loadChatRooms] LỖI: FirebaseAuth.currentUser bị NULL!")
            _chatListUiState.update { it.copy(isLoading = false, errorMessage = "Chưa đăng nhập Firebase Auth") }
            return
        }

        _chatListUiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            chatRepository.getChatRooms(currentUserId)
                .catch { error ->
                    Log.e("CHAT_DEBUG", "[loadChatRooms] Lỗi Flow getChatRooms: ${error.message}", error)
                    _chatListUiState.update {
                        it.copy(isLoading = false, errorMessage = error.localizedMessage)
                    }
                }
                .collect { rooms ->
                    Log.d("CHAT_DEBUG", "[loadChatRooms] Đã tải ${rooms.size} phòng chat")
                    _chatListUiState.update {
                        it.copy(chatRooms = rooms, isLoading = false, errorMessage = null)
                    }
                }
        }
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentRoomId: String = ""
    private var participantIds: List<String> = emptyList()

    fun initChatRoom(
        roomId: String,
        partnerId: String,
        partnerName: String,
        partnerAvatar: String?,
        myName: String,
        myAvatar: String?
    ) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("CHAT_DEBUG", "[initChatRoom] Gọi khởi tạo phòng:")
        Log.d("CHAT_DEBUG", "   -> roomId: $roomId")
        Log.d("CHAT_DEBUG", "   -> currentUserId: $currentUserId")
        Log.d("CHAT_DEBUG", "   -> partnerId: $partnerId")
        Log.d("CHAT_DEBUG", "   -> partnerName: $partnerName")

        if (currentUserId.isNullOrBlank()) {
            Log.e("CHAT_DEBUG", "[initChatRoom] currentUser bị NULL! App chưa đăng nhập Firebase Auth nên Firestore sẽ chặn.")
            _uiState.update { it.copy(isLoading = false, errorMessage = "Chưa đăng nhập tài khoản Firebase!") }
            return
        }

        if (partnerId.isBlank()) {
            Log.e("CHAT_DEBUG", "[initChatRoom] partnerId bị rỗng! Kiểm tra lại tham số truyền từ Navigation.")
            _uiState.update { it.copy(isLoading = false, errorMessage = "ID đối phương không hợp lệ") }
            return
        }

        currentRoomId = roomId
        participantIds = listOf(currentUserId, partnerId)

        viewModelScope.launch {
            chatRepository.saveChatRoomMetadata(
                roomId = roomId,
                participantIds = participantIds,
                myId = currentUserId,
                myName = myName,
                myAvatar = myAvatar,
                partnerId = partnerId,
                partnerName = partnerName,
                partnerAvatar = partnerAvatar
            )
            chatRepository.markRoomAsRead(roomId, currentUserId)
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            Log.d("CHAT_DEBUG", "[initChatRoom] Bắt đầu lắng nghe tin nhắn Firestore cho roomId: $roomId")
            chatRepository.getMessages(roomId)
                .catch { error ->
                    Log.e("CHAT_DEBUG", "[initChatRoom] LỖI getMessages Firestore: ${error.message}", error)
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = "Lỗi Firestore: ${error.localizedMessage}")
                    }
                }
                .collect { messageList ->
                    Log.d("CHAT_DEBUG", "[initChatRoom] Nhận được ${messageList.size} tin nhắn từ Firestore")
                    _uiState.update {
                        it.copy(messages = messageList, isLoading = false)
                    }
                }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendMessageWithOptionalProduct(
        product: ChatMessage?,
        senderName: String,
        senderAvatar: String? = null
    ) {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank() || currentRoomId.isBlank()) return
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        Log.d("CHAT_DEBUG", "[sendMessage] Bắt đầu gửi tin nhắn đến roomId: $currentRoomId")

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, inputText = "") }

            if (product != null) {
                val prodMessage = ChatMessage(
                    senderId = currentUserId,
                    senderName = senderName,
                    senderAvatar = senderAvatar,
                    text = "Tôi quan tâm đến sản phẩm này",
                    productId = product.productId,
                    productName = product.productName,
                    productPrice = product.productPrice,
                    productImage = product.productImage,
                    timestamp = System.currentTimeMillis()
                )
                val prodResult = chatRepository.sendMessage(currentRoomId, prodMessage, participantIds)
                Log.d("CHAT_DEBUG", "[sendMessage] Gửi kèm thẻ sản phẩm: success = ${prodResult.isSuccess}")
            }

            val textMessage = ChatMessage(
                senderId = currentUserId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                text = content,
                timestamp = System.currentTimeMillis() + 1
            )
            val result = chatRepository.sendMessage(currentRoomId, textMessage, participantIds)
            Log.d("CHAT_DEBUG", "[sendMessage] Gửi text message: success = ${result.isSuccess}")

            _uiState.update { it.copy(isSending = false) }

            if (result.isSuccess) {
                val recipientId = participantIds.firstOrNull { it != currentUserId }
                if (!recipientId.isNullOrBlank()) {
                    try {
                        notificationApiService.sendChatNotification(
                            SendNotificationRequest(
                                recipientId = recipientId,
                                senderName = senderName.ifBlank { "Tin nhắn mới" },
                                messageText = content,
                                roomId = currentRoomId
                            )
                        )
                        Log.d("CHAT_DEBUG", "[sendMessage] Bắn FCM notification thành công tới: $recipientId")
                    } catch (e: Exception) {
                        Log.e("CHAT_DEBUG", "[sendMessage] Lỗi gọi FCM Notification API: ${e.message}")
                    }
                }
            } else {
                _uiState.update { it.copy(errorMessage = "Gửi tin nhắn thất bại!") }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}