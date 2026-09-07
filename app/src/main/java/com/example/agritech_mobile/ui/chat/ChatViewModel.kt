package com.example.agritech_mobile.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.dto.ChatMessage
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

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentRoomId: String = ""
    private var participantIds: List<String> = emptyList()

    fun initChatRoom(roomId: String, participants: List<String>) {
        if (currentRoomId == roomId) return
        currentRoomId = roomId
        participantIds = participants

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

    fun sendTextMessage(senderName: String) {
        val content = _uiState.value.inputText.trim()
        if (content.isBlank() || currentRoomId.isBlank()) return

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val message = ChatMessage(
            senderId = currentUserId,
            senderName = senderName,
            text = content,
            timestamp = System.currentTimeMillis()
        )

        executeSendMessage(message)
    }

    fun sendProductCard(
        senderName: String,
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

            if (result.isFailure) {
                _uiState.update {
                    it.copy(
                        isSending = false,
                        errorMessage = "Không thể gửi tin nhắn. Vui lòng thử lại!"
                    )
                }
            } else {
                _uiState.update { it.copy(isSending = false) }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}