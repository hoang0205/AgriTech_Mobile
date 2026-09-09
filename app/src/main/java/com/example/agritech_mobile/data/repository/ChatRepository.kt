package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.dto.ChatMessage
import com.example.agritech_mobile.ui.chat.ChatRoomItem
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
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

    suspend fun sendMessage(roomId: String, message: ChatMessage, participantIds: List<String>): Result<Unit> {
        return try {
            val roomRef = firestore.collection("chats").document(roomId)
            val messageRef = roomRef.collection("messages").document()
            val finalMessage = message.copy(id = messageRef.id)

            messageRef.set(finalMessage).await()

            val previewText = if (!finalMessage.productId.isNullOrBlank() && finalMessage.text.isBlank()) {
                "[Đã gửi một sản phẩm]"
            } else {
                finalMessage.text
            }

            val recipientId = participantIds.firstOrNull { it != finalMessage.senderId }

            val roomUpdates = mutableMapOf<String, Any?>(
                "roomId" to roomId,
                "participants" to participantIds,
                "lastMessage" to previewText,
                "lastUpdated" to finalMessage.timestamp,
                "partnerName_${finalMessage.senderId}" to finalMessage.senderName,
                "partnerAvatar_${finalMessage.senderId}" to finalMessage.senderAvatar
            )

            if (!recipientId.isNullOrBlank()) {
                roomUpdates["unreadCount_$recipientId"] = FieldValue.increment(1)
            }

            roomRef.set(roomUpdates, SetOptions.merge()).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getChatRooms(currentUserId: String): Flow<List<ChatRoomItem>> = callbackFlow {
        val listener = firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val rooms = snapshot?.documents?.mapNotNull { doc ->
                    val roomId = doc.getString("roomId") ?: doc.id
                    val participants = doc.get("participants") as? List<String> ?: emptyList()
                    val lastMessage = doc.getString("lastMessage") ?: ""
                    val lastUpdated = doc.getLong("lastUpdated") ?: 0L
                    val partnerId = participants.firstOrNull { it != currentUserId } ?: ""

                    val unread = doc.getLong("unreadCount_$currentUserId")?.toInt() ?: 0

                    ChatRoomItem(
                        roomId = roomId,
                        partnerId = partnerId,
                        partnerName = doc.getString("partnerName_$partnerId") ?: "Người dùng",
                        partnerAvatar = doc.getString("partnerAvatar_$partnerId"),
                        lastMessage = lastMessage,
                        lastUpdated = lastUpdated,
                        unreadCount = unread
                    )
                } ?: emptyList()

                trySend(rooms)
            }

        awaitClose { listener.remove() }
    }

    suspend fun markRoomAsRead(roomId: String, currentUserId: String) {
        try {
            firestore.collection("chats").document(roomId)
                .update("unreadCount_$currentUserId", 0)
                .await()
        } catch (_: Exception) {}
    }

    suspend fun saveChatRoomMetadata(
        roomId: String,
        participantIds: List<String>,
        myId: String,
        myName: String,
        myAvatar: String?,
        partnerId: String,
        partnerName: String,
        partnerAvatar: String?
    ) {
        try {
            val roomRef = firestore.collection("chats").document(roomId)
            val updates = mutableMapOf<String, Any?>(
                "roomId" to roomId,
                "participants" to participantIds
            )

            if (myName.isNotBlank()) updates["partnerName_$myId"] = myName
            if (!myAvatar.isNullOrBlank()) updates["partnerAvatar_$myId"] = myAvatar

            if (partnerName.isNotBlank() && partnerName != "Tin nhắn" && partnerName != "Người dùng") {
                updates["partnerName_$partnerId"] = partnerName
            }
            if (!partnerAvatar.isNullOrBlank()) {
                updates["partnerAvatar_$partnerId"] = partnerAvatar
            }

            roomRef.set(updates, SetOptions.merge()).await()
        } catch (_: Exception) {}
    }
}