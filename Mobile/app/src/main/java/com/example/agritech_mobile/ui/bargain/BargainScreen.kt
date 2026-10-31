package com.example.agritech_mobile.ui.bargain

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.UserApiService
import com.example.agritech_mobile.data.remote.ProductApiService
import com.example.agritech_mobile.data.remote.dto.ProductResponse
import com.example.agritech_mobile.data.remote.dto.ShippingDetails
import com.example.agritech_mobile.data.remote.dto.SellerDeliveryQuote
import com.example.agritech_mobile.data.remote.dto.DeliveryStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.http.*
import javax.inject.Inject

data class Offer(val id: Long, val productId: String, val productName: String, val unit: String,
    val quantity: Double, val unitPrice: Double, val listedPrice: Double, val message: String,
    val buyerName: String, val sellerName: String, val isBuyer: Boolean, val status: String,
    val expiresAt: String, val orderId: Long?)
data class NewOffer(val productId: String, val quantity: Double, val unitPrice: Long, val message: String)
data class OfferCheckout(val addressId: Long, val expectedShippingFee: Long)
data class OfferOrder(val orderId: Long)
interface BargainApi {
    @GET("api/bargains") suspend fun list(): List<Offer>
    @POST("api/bargains") suspend fun create(@Body request: NewOffer): Offer
    @POST("api/bargains/{id}/{action}") suspend fun act(@Path("id") id: Long, @Path("action") action: String): Offer
    @GET("api/bargains/{id}/delivery-quote") suspend fun quote(@Path("id") id: Long, @Query("addressId") addressId: Long): SellerDeliveryQuote
    @POST("api/bargains/{id}/checkout") suspend fun checkout(@Path("id") id: Long, @Body request: OfferCheckout): OfferOrder
}
data class BargainState(val offers: List<Offer> = emptyList(), val busy: Boolean = false,
    val error: String? = null, val notice: String? = null, val addresses: List<ShippingDetails> = emptyList(),
    val selected: Offer? = null, val address: ShippingDetails? = null, val quote: SellerDeliveryQuote? = null,
    val product: ProductResponse? = null)

@HiltViewModel
class BargainViewModel @Inject constructor(retrofit: Retrofit, private val users: UserApiService): ViewModel() {
    private val api = retrofit.create(BargainApi::class.java)
    private val products = retrofit.create(ProductApiService::class.java)
    private val mutable = MutableStateFlow(BargainState())
    val state = mutable.asStateFlow()
    private fun run(block: suspend () -> Unit) {
        if (mutable.value.busy) return
        mutable.value = mutable.value.copy(busy = true, error = null, notice = null)
        viewModelScope.launch {
            try { block() } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                val detail = if (e is HttpException) runCatching {
                    val json = org.json.JSONObject(e.response()?.errorBody()?.string().orEmpty())
                    json.optString("detail").ifBlank { json.optString("message") }
                }.getOrNull() else null
                mutable.value = mutable.value.copy(error = bargainErrorMessage((e as? HttpException)?.code(), detail))
            } finally { mutable.value = mutable.value.copy(busy = false) }
        }
    }
    fun load() = run { mutable.value = mutable.value.copy(offers = api.list()) }
    fun product(id: String) = run {
        val result = products.getProductById(id)
        if (!result.isSuccessful) throw HttpException(result)
        mutable.value = mutable.value.copy(product = result.body())
    }
    fun create(r: NewOffer, done: () -> Unit) = run { api.create(r); done() }
    fun act(o: Offer, action: String) = run {
        api.act(o.id, action)
        mutable.value = mutable.value.copy(offers = api.list(), notice = "Đã cập nhật đề nghị.")
    }
    fun open(o: Offer) = run {
        val result = users.getUserAddresses()
        if (!result.isSuccessful) throw HttpException(result)
        mutable.value = mutable.value.copy(selected = o, addresses = result.body().orEmpty(), address = null, quote = null)
    }
    fun close() { if (!mutable.value.busy) mutable.value = mutable.value.copy(selected = null, quote = null, address = null) }
    fun select(a: ShippingDetails) = run {
        val offer = mutable.value.selected ?: return@run
        mutable.value = mutable.value.copy(address = a, quote = null)
        val q = api.quote(offer.id, a.id.toLong())
        mutable.value = mutable.value.copy(quote = q)
    }
    fun checkout() = run {
        val s = mutable.value
        val o = s.selected ?: return@run
        val a = s.address ?: return@run
        val fee = s.quote?.takeIf { it.status == DeliveryStatus.AVAILABLE }?.shippingFee ?: return@run
        // A failed response may follow a successful commit. Retrying returns the same order.
        val result = api.checkout(o.id, OfferCheckout(a.id.toLong(), fee))
        mutable.value = mutable.value.copy(selected = null, address = null, quote = null,
            notice = "Đặt đơn COD #${result.orderId} thành công.")
        mutable.value = mutable.value.copy(offers = api.list())
    }
}
