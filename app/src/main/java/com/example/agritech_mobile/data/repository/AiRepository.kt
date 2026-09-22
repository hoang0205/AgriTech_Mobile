package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.AiApiService
import com.example.agritech_mobile.data.remote.dto.ChatBotRequest
import com.example.agritech_mobile.data.remote.dto.ChatBotResponse
import com.example.agritech_mobile.data.remote.dto.ChatHistoryMessage
import com.example.agritech_mobile.data.remote.dto.PredictImageResponse
import com.example.agritech_mobile.data.remote.dto.PricePredictionRequest
import com.example.agritech_mobile.data.remote.dto.PricePredictionResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.MultipartBody
import javax.inject.Inject

class AiRepository @Inject constructor(
    private val aiApiService: AiApiService
) {
    suspend fun predictImage(imagePart: MultipartBody.Part): Result<PredictImageResponse> {
        return try {
            val response = aiApiService.predictImage(imagePart)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun predictPrice(productName: String): Result<PricePredictionResponse> {
        return try {
            val request = PricePredictionRequest(productName)
            val response = aiApiService.predictPrice(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    fun chatBotStream(
        query: String,
        productName: String,
        userId: String,
        sessionId: String
    ): Flow<String> = flow {
        val request = ChatBotRequest(query, productName, userId, sessionId)
        val responseBody = aiApiService.chatBotStream(request)
        val reader = responseBody.byteStream().bufferedReader(Charsets.UTF_8)

        val buffer = CharArray(128)
        var charsRead: Int
        while (reader.read(buffer).also { charsRead = it } != -1) {
            if (charsRead > 0) {
                val chunk = String(buffer, 0, charsRead)
                emit(chunk)
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun chatBot(query: String, productName: String, userId: String, sessionId: String): Result<ChatBotResponse> {
        return try {
            val request = ChatBotRequest(query, productName, userId, sessionId)
            val response = aiApiService.chatBot(request)
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Phản hồi từ server bị rỗng!"))
                }
            } else {
                Result.failure(Exception("Lỗi từ server: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Không thể kết nối đến server: ${e.localizedMessage}"))
        }
    }

    suspend fun getChatHistory(sessionId: String): Result<List<ChatHistoryMessage>> {
        return try {
            val response = aiApiService.getChatHistory(sessionId)
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!.messages)
            } else {
                Result.failure(Exception("Không thể tải lịch sử chat"))
            }
        } catch (e: Exception) {
            Result.failure(Exception("Lỗi kết nối: ${e.localizedMessage}"))
        }
    }
}