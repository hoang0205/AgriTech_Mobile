package com.example.agritech_mobile.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.repository.AiRepository
import com.example.agritech_mobile.ui.chat.AiMessage
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class AiChatUiState(
    val messages: List<AiMessage> = emptyList(),
    val inputText: String = "",
    val isStreaming: Boolean = false,
    val sessionId: String = ""
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val aiRepository: AiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiChatUiState())
    val uiState: StateFlow<AiChatUiState> = _uiState.asStateFlow()

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "guest"
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    fun initSession(productId: String, productName: String) {
        val fixedSessionId = "${currentUserId}_${productId}"
        _uiState.update { it.copy(sessionId = fixedSessionId) }

        viewModelScope.launch {
            val result = aiRepository.getChatHistory(fixedSessionId)
            result.onSuccess { history ->
                if (history.isNotEmpty()) {
                    val restoredMessages = history.mapIndexed { index, msg ->
                        AiMessage(
                            id = "history_$index",
                            text = msg.content,
                            isBot = msg.role == "assistant",
                            timestamp = timeFormat.format(Date()),
                            subNote = if (msg.role == "assistant") "Cẩm nang Nông sản" else null
                        )
                    }
                    _uiState.update { it.copy(messages = restoredMessages) }
                } else {
                    val welcomeMsg = AiMessage(
                        id = "welcome_msg",
                        text = "Dạ chào bạn! Tôi là AgriBot \n\nTôi là trợ lý AI của ứng dụng. Bạn đang quan tâm đến \"$productName\" đúng không ạ? Bạn cần tư vấn gì về sản phẩm này ạ?",
                        isBot = true,
                        timestamp = timeFormat.format(Date()),
                        subNote = "AgriTech 3.5 AI"
                    )
                    _uiState.update { it.copy(messages = listOf(welcomeMsg)) }
                }
            }.onFailure {
                val welcomeMsg = AiMessage(
                    id = "welcome_msg",
                    text = "Dạ chào bạn! Tôi là AgriBot \n\nBạn đang xem \"$productName\". Bạn cần giải đáp gì về sản phẩm này không ạ?",
                    isBot = true,
                    timestamp = timeFormat.format(Date())
                )
                _uiState.update { it.copy(messages = listOf(welcomeMsg)) }
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendStreamMessage(productName: String, presetQuery: String? = null) {
        val query = presetQuery ?: _uiState.value.inputText.trim()
        if (query.isBlank() || _uiState.value.isStreaming) return

        val nowTime = timeFormat.format(Date())
        val userMessageId = System.currentTimeMillis().toString()
        val botMessageId = (System.currentTimeMillis() + 1).toString()

        val userMsg = AiMessage(
            id = userMessageId,
            text = query,
            isBot = false,
            timestamp = nowTime
        )
        val botPlaceholder = AiMessage(
            id = botMessageId,
            text = "",
            isBot = true,
            timestamp = nowTime,
            subNote = "Đang phản hồi trực tiếp..."
        )

        _uiState.update {
            it.copy(
                messages = it.messages + userMsg + botPlaceholder,
                inputText = "",
                isStreaming = true
            )
        }

        viewModelScope.launch {
            aiRepository.chatBotStream(
                query = query,
                productName = productName,
                userId = currentUserId,
                sessionId = _uiState.value.sessionId
            )
                .catch { error ->
                    _uiState.update { state ->
                        val updated = state.messages.map { msg ->
                            if (msg.id == botMessageId) {
                                msg.copy(
                                    text = "Lỗi kết nối máy chủ AI: ${error.localizedMessage}",
                                    subNote = "Lỗi"
                                )
                            } else msg
                        }
                        state.copy(messages = updated, isStreaming = false)
                    }
                }
                .collect { token ->
                    _uiState.update { state ->
                        val updated = state.messages.map { msg ->
                            if (msg.id == botMessageId) {
                                msg.copy(text = msg.text + token)
                            } else msg
                        }
                        state.copy(messages = updated)
                    }
                }

            _uiState.update { state ->
                val updated = state.messages.map { msg ->
                    if (msg.id == botMessageId) {
                        msg.copy(subNote = "Dữ liệu đối chiếu Cẩm nang Nông sản")
                    } else msg
                }
                state.copy(messages = updated, isStreaming = false)
            }
        }
    }
}