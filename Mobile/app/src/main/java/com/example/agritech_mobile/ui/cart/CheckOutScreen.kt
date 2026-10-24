package com.example.agritech_mobile.ui.cart

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.agritech_mobile.data.remote.dto.DeliveryStatus
import com.example.agritech_mobile.data.remote.dto.SellerDeliveryQuote
import com.example.agritech_mobile.ui.order.DeliveryQuoteInput
import com.example.agritech_mobile.ui.order.OrderState
import com.example.agritech_mobile.ui.order.OrderViewModel
import com.example.agritech_mobile.ui.theme.AgritechTheme
import com.example.agritech_mobile.ui.user.AddressState
import com.example.agritech_mobile.ui.user.UserViewModel

data class CheckoutItem(
    val id: String,
    val name: String,
    val description: String,
    val quantityDesc: String,
    val price: Double,
    val imageUrl: String
)

enum class PaymentMethodType {
    COD, VNPAY, BANK_TRANSFER
}

data class CheckoutUiState(
    val userName: String = "",
    val phone: String = "",
    val address: String = "",
    val items: List<CheckoutItem> = emptyList(),
    val subtotal: Double = 0.0,
    val shippingFee: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val selectedPaymentMethod: PaymentMethodType = PaymentMethodType.COD,
    val stockAvailable: Boolean = false,
    val deliveryLoading: Boolean = false,
    val deliveryAvailable: Boolean = false,
    val deliveryError: String? = null,
    val deliveryQuotes: List<SellerDeliveryQuote> = emptyList()
)

@Composable
fun CheckoutScreen(
    selectedCartItemIds: List<String>,
    selectedAddressId: String? = null,
    onBackClick: () -> Unit,
    onPlaceOrderSuccess: () -> Unit,
    onNavigateToAddressSelection: (String?) -> Unit,
    onNavigateToVnpay: (orderId: Long, paymentUrl: String) -> Unit = { _, _ -> },
    viewModel: OrderViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel(),
    userViewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val orderState by viewModel.orderState.collectAsState()
    val cartState by cartViewModel.cartState.collectAsState()
    val addressState by userViewModel.addressState.collectAsState()
    val homeDeliveryAddressId by userViewModel.homeDeliveryAddressId.collectAsState()
    val deliveryState by viewModel.deliveryQuoteState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }
    var loadingMessage by remember { mutableStateOf("Đang xử lý đơn hàng...") }

    var uiState by remember {
        mutableStateOf(
            CheckoutUiState(
                userName = "Đang tải...",
                phone = "",
                address = "",
                items = emptyList(),
                subtotal = 0.0,
                shippingFee = 0.0,
                discount = 0.0,
                totalAmount = 0.0,
                selectedPaymentMethod = PaymentMethodType.COD
            )
        )
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        cartViewModel.loadCartItems()
        userViewModel.getUserAddresses()
    }

    val addresses = (addressState as? AddressState.Success)
        ?.addresses
        .orEmpty()

    val chosenAddress = addresses.find { it.id == selectedAddressId }
        ?: addresses.find { it.id == homeDeliveryAddressId }
        ?: addresses.find { it.isDefault }
        ?: addresses.firstOrNull()

    val requestedIds = selectedCartItemIds.distinct().sorted()

    val selectedItems = (cartState as? CartState.CartItemsSuccess)
        ?.items
        .orEmpty()
        .filter { it.cartItemId in requestedIds }
        .sortedBy { it.cartItemId }

    val validSelection = selectedItems.isNotEmpty() &&
            selectedItems.size == requestedIds.size

    val deliveryInput = if (chosenAddress != null && validSelection) {
        DeliveryQuoteInput(
            address = chosenAddress,
            items = selectedItems
        )
    } else {
        null
    }

    LaunchedEffect(deliveryInput) {
        viewModel.loadDeliveryQuote(deliveryInput)
    }

    val quoteMatches = deliveryInput != null &&
            deliveryState.input == deliveryInput

    val quote = if (
        quoteMatches &&
        !deliveryState.isLoading &&
        deliveryState.error == null
    ) {
        deliveryState.quote
    } else {
        null
    }

    val shippingReady = quote?.allDeliverable == true &&
            quote.totalShippingFee != null

    val subtotal = selectedItems.sumOf { it.price * it.quantity }
    val shippingFee = quote?.totalShippingFee?.toDouble() ?: 0.0

    val displayState = uiState.copy(
        userName = chosenAddress?.receiverName ?: "Chưa có địa chỉ",
        phone = chosenAddress?.phoneNumber.orEmpty(),
        address = chosenAddress?.addressDetail
            ?: "Vui lòng chọn địa chỉ nhận hàng",
        items = selectedItems.map { item ->
            CheckoutItem(
                id = item.cartItemId,
                name = item.productName,
                description = "Đơn giá: ${item.price} đ / ${item.unit}",
                quantityDesc = "${item.quantity} ${item.unit}",
                price = item.price * item.quantity,
                imageUrl = item.thumbnail
            )
        },
        subtotal = subtotal,
        shippingFee = shippingFee,
        totalAmount = subtotal + shippingFee - uiState.discount,
        stockAvailable = validSelection && selectedItems.all {
            it.available &&
                    it.quantity > 0.0 &&
                    it.quantity <= it.availableQuantity
        },
        deliveryLoading = addressState is AddressState.Loading ||
                cartState is CartState.Loading ||
                (deliveryInput != null &&
                        (!quoteMatches || deliveryState.isLoading)),
        deliveryAvailable = shippingReady,
        deliveryError = when (val state = addressState) {
            is AddressState.Error -> state.message
            else -> when {
                addressState is AddressState.Success &&
                        chosenAddress == null ->
                    "Vui lòng chọn địa chỉ nhận hàng."

                quoteMatches -> deliveryState.error
                else -> null
            }
        },
        deliveryQuotes = quote?.sellers.orEmpty()
    )

    LaunchedEffect(orderState) {
        when (val state = orderState) {
            is OrderState.Loading -> {
                isLoading = true
                loadingMessage = "Đang tạo đơn hàng..."
            }

            is OrderState.Success -> {
                if (uiState.selectedPaymentMethod == PaymentMethodType.VNPAY) {
                    val orderId = state.orderId ?: 0L
                    if (orderId > 0L) {
                        loadingMessage = "Đang kết nối tới cổng VNPay..."
                        viewModel.payWithVnpay(
                            orderId = orderId,
                            onUrlReady = { paymentUrl ->
                                isLoading = false
                                viewModel.resetState()
                                onNavigateToVnpay(orderId, paymentUrl)
                            },
                            onError = { error ->
                                isLoading = false
                                viewModel.resetState()
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                                onPlaceOrderSuccess()
                            }
                        )
                    } else {
                        isLoading = false
                        viewModel.resetState()
                        Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        onPlaceOrderSuccess()
                    }
                } else {
                    isLoading = false
                    viewModel.resetState()
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                    onPlaceOrderSuccess()
                }
            }

            is OrderState.Error -> {
                isLoading = false

                Toast.makeText(
                    context,
                    state.error,
                    Toast.LENGTH_LONG
                ).show()

                viewModel.loadDeliveryQuote(null)
                cartViewModel.loadCartItems()
                userViewModel.getUserAddresses()

                viewModel.resetState()
            }

            else -> isLoading = false
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        CheckoutContent(
            uiState = displayState,
            onRetryDelivery = {
                cartViewModel.loadCartItems()
                userViewModel.getUserAddresses()
            },
            onBackClick = onBackClick,
            onChangeAddressClick = {
                onNavigateToAddressSelection(chosenAddress?.id)
            },
            onPaymentMethodSelect = { method ->
                uiState = uiState.copy(selectedPaymentMethod = method)
            },
            onPlaceOrderClick = {
                if (orderState is OrderState.Loading) {
                    return@CheckoutContent
                }

                if (!displayState.stockAvailable) {
                    Toast.makeText(
                        context,
                        "Có sản phẩm hết hàng hoặc không đủ tồn kho.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@CheckoutContent
                }

                if (
                    chosenAddress == null ||
                    displayState.phone.isBlank()
                ) {
                    Toast.makeText(
                        context,
                        "Vui lòng chọn địa chỉ nhận hàng.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@CheckoutContent
                }

                if (!displayState.deliveryAvailable) {
                    Toast.makeText(
                        context,
                        "Chưa có báo phí hợp lệ hoặc có người bán ngoài phạm vi giao hàng.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@CheckoutContent
                }

                val checkoutAddressId = chosenAddress?.id
                val expectedShippingFee = quote?.totalShippingFee

                if (
                    checkoutAddressId == null ||
                    expectedShippingFee == null
                ) {
                    Toast.makeText(
                        context,
                        "Vui lòng cập nhật báo phí giao hàng.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@CheckoutContent
                }

                val method = if (
                    uiState.selectedPaymentMethod == PaymentMethodType.VNPAY
                ) {
                    "VNPAY"
                } else {
                    "COD"
                }

                viewModel.checkout(
                    shippingAddress = displayState.address,
                    phoneNumber = displayState.phone,
                    selectedCartItemIds = selectedCartItemIds,
                    addressId = checkoutAddressId,
                    expectedShippingFee = expectedShippingFee,
                    paymentMethod = method
                )
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1B5E20))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = loadingMessage,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "vi", device = "spec:width=411dp,height=1400dp,dpi=420")
@Composable
fun CheckoutScreenPreview() {
    AgritechTheme {
        CheckoutContent(
            uiState = CheckoutUiState(
                userName = "Khánh Ly",
                phone = "036 363 2030",
                address = "18 ngõ 219 đường Nguyễn Ngọc Vũ",
                items = listOf(
                    CheckoutItem(
                        "1",
                        "Organic Baby Carrots",
                        "Farm: Dalat Green Fields",
                        "1.5kg",
                        65000.0,
                        "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?q=80&w=600"
                    ),
                    CheckoutItem(
                        "2",
                        "Wild Forest Honey",
                        "U Minh Forest Reserve",
                        "500ml",
                        320000.0,
                        "https://images.unsplash.com/photo-1587049352847-4d4b1ed74db4?q=80&w=600"
                    )
                ),
                subtotal = 385000.0,
                shippingFee = 25000.0,
                discount = 0.0,
                totalAmount = 410000.0,
                stockAvailable = true,
                deliveryAvailable = true,
                deliveryQuotes = listOf(SellerDeliveryQuote("seller", "Nông trại Đà Lạt", listOf(1L, 2L),
                    DeliveryStatus.AVAILABLE, "Trong phạm vi giao hàng.", distanceKm = 8.5, shippingFee = 25000L)),
                selectedPaymentMethod = PaymentMethodType.COD
            ),
            onBackClick = {},
            onChangeAddressClick = {},
            onPaymentMethodSelect = {},
            onPlaceOrderClick = {}
        )
    }
}
