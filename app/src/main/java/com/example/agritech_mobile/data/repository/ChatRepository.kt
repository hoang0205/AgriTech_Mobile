package com.example.agritech_mobile.data.repository

import android.util.Log
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
        Log.d("CHAT_DEBUG", "[ChatRepo.getMessages] Đăng ký snapshot listener cho room: $roomId")
        val listener = firestore.collection("chats")
            .document(roomId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CHAT_DEBUG", "[ChatRepo.getMessages] LỖI Snapshot: ${error.message} (Code: ${error.code})", error)
                    close(error)
                    return@addSnapshotListener
                }
                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                Log.d("CHAT_DEBUG", "[ChatRepo.getMessages] Snapshot trả về ${messages.size} documents")
                trySend(messages)
            }

        awaitClose {
            Log.d("CHAT_DEBUG", "[ChatRepo.getMessages] Đã hủy lắng nghe room: $roomId")
            listener.remove()
        }
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
            Log.d("CHAT_DEBUG", "[ChatRepo.sendMessage] Đã ghi thành công message vào Firestore (ID: ${finalMessage.id})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("CHAT_DEBUG", "[ChatRepo.sendMessage] Lỗi ghi Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun getChatRooms(currentUserId: String): Flow<List<ChatRoomItem>> = callbackFlow {
        Log.d("CHAT_DEBUG", "[ChatRepo.getChatRooms] Lắng nghe danh sách phòng của user: $currentUserId")
        val listener = firestore.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .orderBy("lastUpdated", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("CHAT_DEBUG", "[ChatRepo.getChatRooms] LỖI Snapshot phòng: ${error.message} (Code: ${error.code})", error)
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

                Log.d("CHAT_DEBUG", "[ChatRepo.getChatRooms] Tìm thấy ${rooms.size} phòng chat")
                trySend(rooms)
            }

        awaitClose { listener.remove() }
    }

    suspend fun markRoomAsRead(roomId: String, currentUserId: String) {
        try {
            firestore.collection("chats").document(roomId)
                .update("unreadCount_$currentUserId", 0)
                .await()
        } catch (e: Exception) {
            Log.w("CHAT_DEBUG", "[ChatRepo.markRoomAsRead] Không thể cập nhật unreadCount: ${e.message}")
        }
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
            Log.d("CHAT_DEBUG", "[ChatRepo.saveChatRoomMetadata] Đã cập nhật metadata phòng $roomId thành công")
        } catch (e: Exception) {
            Log.e("CHAT_DEBUG", "[ChatRepo.saveChatRoomMetadata] LỖI cập nhật metadata: ${e.message}", e)
        }
    }
}