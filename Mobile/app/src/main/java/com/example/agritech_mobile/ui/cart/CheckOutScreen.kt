package com.example.agritech_mobile.ui.cart

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.data.remote.dto.DeliveryStatus
import com.example.agritech_mobile.data.remote.dto.SellerDeliveryQuote
import com.example.agritech_mobile.ui.order.DeliveryQuoteInput
import com.example.agritech_mobile.ui.order.OrderState
import com.example.agritech_mobile.ui.order.OrderViewModel
import com.example.agritech_mobile.ui.theme.AgritechTheme
import com.example.agritech_mobile.ui.user.AddressState
import com.example.agritech_mobile.ui.user.UserViewModel
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.DecimalFormat

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

@Composable
fun CheckoutContent(
    uiState: CheckoutUiState,
    onBackClick: () -> Unit,
    onRetryDelivery: () -> Unit = {},
    onChangeAddressClick: () -> Unit,
    onPaymentMethodSelect: (PaymentMethodType) -> Unit,
    onPlaceOrderClick: () -> Unit
) {
    val formatter = DecimalFormat("#,###")

    val buttonText = when (uiState.selectedPaymentMethod) {
        PaymentMethodType.VNPAY -> "Thanh toán qua VNPay"
        else -> stringResource(R.string.checkout_place_order)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { CheckoutTopBar(onBackClick) },
        bottomBar = {
            Column {
                if (!uiState.stockAvailable && uiState.items.isNotEmpty()) {
                    Text(
                        "Có sản phẩm hết hàng hoặc không đủ tồn kho. Vui lòng quay lại giỏ hàng.",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                CheckoutBottomBar(
                    buttonText = buttonText,
                    enabled = uiState.stockAvailable &&
                            uiState.deliveryAvailable &&
                            !uiState.deliveryLoading,
                    onPlaceOrderClick = onPlaceOrderClick
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            item {
                SectionHeader(stringResource(R.string.checkout_shipping_address))
                Spacer(modifier = Modifier.height(12.dp))
                ShippingAddressCard(
                    userName = uiState.userName,
                    address = uiState.address,
                    phone = uiState.phone,
                    onChangeClick = onChangeAddressClick
                )
            }

            item {
                DeliveryInfoCard(
                    state = uiState,
                    onRetry = onRetryDelivery
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionHeader(stringResource(R.string.checkout_order_summary))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFC8E6C9))
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = stringResource(
                                R.string.checkout_items_count,
                                uiState.items.size
                            ),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF2E7D32)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            items(uiState.items) { item ->
                OrderItemCard(item, formatter)
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SectionHeader(stringResource(R.string.checkout_payment_method))
                Spacer(modifier = Modifier.height(12.dp))

                PaymentMethodCard(
                    icon = Icons.Default.LocalAtm,
                    title = "COD",
                    description = stringResource(R.string.checkout_cod),
                    isSelected = uiState.selectedPaymentMethod == PaymentMethodType.COD,
                    onClick = { onPaymentMethodSelect(PaymentMethodType.COD) }
                )
                Spacer(modifier = Modifier.height(12.dp))

                PaymentMethodCard(
                    icon = Icons.Default.AccountBalance,
                    title = stringResource(R.string.checkout_bank_transfer),
                    description = stringResource(R.string.checkout_bank_transfer_desc),
                    isSelected = uiState.selectedPaymentMethod == PaymentMethodType.BANK_TRANSFER,
                    onClick = { onPaymentMethodSelect(PaymentMethodType.BANK_TRANSFER) }
                )
            }

            item {
                PriceSummaryCard(
                    subtotal = uiState.subtotal,
                    shippingFee = uiState.shippingFee,
                    discount = uiState.discount,
                    totalAmount = uiState.totalAmount,
                    formatter = formatter,
                    shippingFeeKnown = uiState.deliveryAvailable,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.checkout_title),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = Color(0xFF1B5E20)
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1B5E20)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
    )
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        ),
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun ShippingAddressCard(
    userName: String,
    address: String,
    phone: String,
    onChangeClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF2E7D32)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.checkout_default_address),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = stringResource(R.string.checkout_change),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.clickable { onChangeClick() }
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = userName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = phone,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun OrderItemCard(item: CheckoutItem, formatter: DecimalFormat) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = item.imageUrl,
                contentDescription = item.name,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.quantityDesc,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${formatter.format(item.price).replace(',', '.')} đ",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color(0xFF1B5E20)
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentMethodCard(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF1B5E20) else Color.Transparent
    val backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(width = 1.5.dp, color = borderColor, shape = RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF1B5E20),
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DeliveryInfoCard(
    state: CheckoutUiState,
    onRetry: () -> Unit
) {
    val money = DecimalFormat("#,###")
    val distance = DecimalFormat("0.0")
    var showFeeInfo by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Giao hàng", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { showFeeInfo = true }) { Text("Cách tính phí") }
        }
        if (state.deliveryLoading) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Đang tính phí…", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            state.deliveryError?.let { error ->
                Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = onRetry) { Text("Thử lại") }
            }
            state.deliveryQuotes.forEachIndexed { index, seller ->
                if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                val available = seller.status == DeliveryStatus.AVAILABLE && seller.shippingFee != null
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(seller.sellerName, style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        val detail = listOfNotNull(
                            seller.pickupArea?.takeIf { it.isNotBlank() },
                            seller.distanceKm?.let { "≈ ${distance.format(it)} km" }
                        ).joinToString(" · ")
                        if (detail.isNotEmpty()) Text(detail, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        if (!available) Text(seller.message, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error)
                    }
                    if (available) Text("${money.format(seller.shippingFee).replace(',', '.')} đ",
                        style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary)
                }
            }
            if (state.deliveryQuotes.isNotEmpty() && !state.deliveryAvailable) {
                Text("Đổi địa chỉ hoặc bỏ chọn shop chưa hỗ trợ giao.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(2.dp))
    }
    if (showFeeInfo) AlertDialog(onDismissRequest = { showFeeInfo = false },
        title = { Text("Cách tính phí giao") },
        text = { Text("Mỗi người bán có phí giao riêng, dựa trên khoảng cách đường thẳng ước tính đến địa chỉ nhận.\n\nĐến 5 km: 15.000 đ\nTrên 5–15 km: 25.000 đ\nTrên 15–30 km: 40.000 đ\n\nPhạm vi hỗ trợ: tối đa 30 km.",
            style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { TextButton(onClick = { showFeeInfo = false }) { Text("Đã hiểu") } })
}

@Composable
fun PriceSummaryCard(
    subtotal: Double,
    shippingFee: Double,
    discount: Double,
    totalAmount: Double,
    formatter: DecimalFormat,
    shippingFeeKnown: Boolean = true
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(20.dp)
    ) {
        SummaryLine(
            stringResource(R.string.checkout_subtotal),
            "${formatter.format(subtotal).replace(',', '.')} đ"
        )
        Spacer(modifier = Modifier.height(12.dp))
        SummaryLine(
            stringResource(R.string.checkout_shipping_fee),
            if (shippingFeeKnown) {
                "${formatter.format(shippingFee).replace(',', '.')} đ"
            } else {
                "Chưa xác định"
            }
        )
        Spacer(modifier = Modifier.height(12.dp))
        SummaryLine(
            stringResource(R.string.checkout_discount),
            "-${formatter.format(discount).replace(',', '.')} đ",
            isGreen = true
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.checkout_total_amount),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (shippingFeeKnown) {
                    "${formatter.format(totalAmount).replace(',', '.')} đ"
                } else {
                    "Chưa xác định"
                },
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color = Color(0xFF1B5E20)
            )
        }
    }
}

@Composable
fun SummaryLine(label: String, value: String, isGreen: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
            color = if (isGreen) Color(0xFF1B5E20) else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun CheckoutBottomBar(
    buttonText: String,
    onPlaceOrderClick: () -> Unit,
    enabled: Boolean = false
) {
    Surface(
        shadowElevation = 24.dp,
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding()
        ) {
            Button(
                onClick = onPlaceOrderClick,
                enabled = enabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = buttonText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
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
                subtotal = 570000.0,
                shippingFee = 35000.0,
                discount = 15000.0,
                totalAmount = 590000.0,
                selectedPaymentMethod = PaymentMethodType.COD
            ),
            onBackClick = {},
            onChangeAddressClick = {},
            onPaymentMethodSelect = {},
            onPlaceOrderClick = {}
        )
    }
}
