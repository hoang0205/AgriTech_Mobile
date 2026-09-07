package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.dto.ChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        fun generateRoomId(userId1: String, userId2: String): String {
            return if (userId1 < userId2) "${userId1}_${userId2}" else "${userId2}_${userId1}"
        }
    }

    fun getMessages(roomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection("chats")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    suspend fun sendMessage(
        roomId: String,
        message: ChatMessage,
        participantIds: List<String>
    ): Result<Unit> {
        return try {
            val roomRef = firestore.collection("chats").document(roomId)
            val messageRef = roomRef.collection("messages").document()
            val finalMessage = message.copy(id = messageRef.id)

            messageRef.set(finalMessage).await()

            val previewText = if (finalMessage.productId != null && finalMessage.text.isBlank()) {
                "[Đã gửi một sản phẩm]"
            } else {
                finalMessage.text
            }

            roomRef.set(
                mapOf(
                    "roomId" to roomId,
                    "participants" to participantIds,
                    "lastMessage" to previewText,
                    "lastUpdated" to finalMessage.timestamp
                )
            ).await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}