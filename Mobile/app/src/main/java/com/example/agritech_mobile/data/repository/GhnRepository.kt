package com.example.agritech_mobile.data.repository

import com.example.agritech_mobile.data.remote.GhnApiService
import com.example.agritech_mobile.data.remote.dto.GhnShopRequest
import com.example.agritech_mobile.data.remote.dto.GhnSenderRequest
import com.example.agritech_mobile.data.remote.dto.GhnParcelRequest
import com.example.agritech_mobile.data.remote.dto.ChooseDeliveryRequest
import com.example.agritech_mobile.data.remote.dto.ShippingCostRequest
import com.google.gson.Gson
import kotlinx.coroutines.CancellationException
import retrofit2.Response
import javax.inject.Inject

class GhnRepository @Inject constructor(
    private val api: GhnApiService
) {
    suspend fun shop() = body { api.shop() }
    suspend fun connectShop() = body { api.connectShop() }
    suspend fun registerShop(request: GhnShopRequest) = body { api.registerShop(request) }
    suspend fun senderAddress(request: GhnSenderRequest) = body { api.senderAddress(request) }
    suspend fun shopAreas(level: String, parent: Int? = null) = body { api.shopAreas(level, parent) }
    suspend fun areas(level: String, parent: Int? = null) = body { api.areas(level, parent) }
    suspend fun view(id: String) = body { api.view(id) }
    suspend fun quote(id: String, request: GhnParcelRequest) = body { api.quote(id, request) }
    suspend fun reference(id: String, request: GhnParcelRequest) = body { api.reference(id, request) }
    suspend fun create(id: String, request: GhnParcelRequest) = body { api.create(id, request) }
    suspend fun action(id: String, action: String) = body { api.action(id, action) }
    suspend fun delivery(id: String) = body { api.delivery(id) }
    suspend fun chooseDelivery(id: String, method: String) = body { api.chooseDelivery(id, ChooseDeliveryRequest(method)) }
    suspend fun shippingCost(id: String, actualCost: Long?) = body { api.shippingCost(id, ShippingCostRequest(actualCost)) }

    private suspend fun <T> body(call: suspend () -> Response<T>): T {
        val response = try { call() } catch (e: CancellationException) { throw e } catch (e: Exception) {
            throw IllegalStateException("Không nhận được phản hồi. Mở lại GHN để kiểm tra hoặc thử lại yêu cầu đang chờ.")
        }
        if (!response.isSuccessful) {
            val message = runCatching {
                Gson().fromJson(response.errorBody()?.string(), ErrorBody::class.java)?.message
            }.getOrNull()
            throw IllegalStateException(message ?: "Không xử lý được yêu cầu GHN (${response.code()}).")
        }
        return response.body() ?: throw IllegalStateException("Máy chủ chưa trả dữ liệu GHN.")
    }
    private data class ErrorBody(val message: String?)
}
