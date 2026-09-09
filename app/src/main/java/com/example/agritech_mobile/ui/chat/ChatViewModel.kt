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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        _chatListUiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            chatRepository.getChatRooms(currentUserId)
                .catch { error ->
                    _chatListUiState.update {
                        it.copy(isLoading = false, errorMessage = error.localizedMessage)
                    }
                }
                .collect { rooms ->
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
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
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

        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            chatRepository.getMessages(roomId)
                .catch { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.localizedMessage)
                    }
                }
                .collect { messageList ->
                    _uiState.update {
                        it.copy(messages = messageList, isLoading = false)
                    }
                }
        }
    }

    fun onInputTextChanged(newText: String) {
        _uiState.update { it.copy(inputText = newText) }
    }

    fun sendTextMessage(senderName: String, senderAvatar: String? = null) {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank() || currentRoomId.isBlank()) return

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val message = ChatMessage(
            senderId = currentUserId,
            senderName = senderName,
            senderAvatar = senderAvatar,
            text = content,
            timestamp = System.currentTimeMillis()
        )

        executeSendMessage(message)
    }

    fun sendProductCard(
        senderName: String,
        senderAvatar: String? = null,
        productId: String,
        productName: String,
        productPrice: Double,
        productImage: String
    ) {
        if (currentRoomId.isBlank()) return
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val message = ChatMessage(
            senderId = currentUserId,
            senderName = senderName,
            senderAvatar = senderAvatar,
            text = "Tôi quan tâm đến sản phẩm này",
            productId = productId,
            productName = productName,
            productPrice = productPrice,
            productImage = productImage,
            timestamp = System.currentTimeMillis()
        )

        executeSendMessage(message)
    }

    private fun executeSendMessage(message: ChatMessage) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, inputText = "") }

            val result = chatRepository.sendMessage(
                roomId = currentRoomId,
                message = message,
                participantIds = participantIds
            )

            if (result.isSuccess) {
                _uiState.update { it.copy(isSending = false) }

                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                val recipientId = participantIds.firstOrNull { it != currentUserId }

                if (!recipientId.isNullOrBlank()) {
                    try {
                        val previewText = message.productName?.let { "[Sản phẩm] $it" } ?: message.text
                        notificationApiService.sendChatNotification(
                            SendNotificationRequest(
                                recipientId = recipientId,
                                senderName = message.senderName.ifBlank { "Tin nhắn mới" },
                                messageText = previewText,
                                roomId = currentRoomId
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Lỗi kích hoạt thông báo: ${e.localizedMessage}")
                    }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = "Không thể gửi tin nhắn. Vui lòng thử lại!"
                    )
                }
            }
        }
    }

    fun sendMessageWithOptionalProduct(
        product: ChatMessage?,
        senderName: String,
        senderAvatar: String? = null
    ) {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank() || currentRoomId.isBlank()) return
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

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
                chatRepository.sendMessage(currentRoomId, prodMessage, participantIds)
            }

            val textMessage = ChatMessage(
                senderId = currentUserId,
                senderName = senderName,
                senderAvatar = senderAvatar,
                text = content,
                timestamp = System.currentTimeMillis() + 1
            )
            val result = chatRepository.sendMessage(currentRoomId, textMessage, participantIds)

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
                    } catch (e: Exception) {
                        Log.e("ChatViewModel", "Lỗi notification: ${e.localizedMessage}")
                    }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}