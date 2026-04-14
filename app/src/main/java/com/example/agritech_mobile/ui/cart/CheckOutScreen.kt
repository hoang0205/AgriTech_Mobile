package com.example.agritech_mobile.ui.checkout

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.LocalAtm
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Wallet
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
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.ui.cart.CartState
import com.example.agritech_mobile.ui.cart.CartViewModel
import com.example.agritech_mobile.ui.order.OrderState
import com.example.agritech_mobile.ui.order.OrderViewModel
import com.example.agritech_mobile.ui.theme.AgritechTheme
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
    COD, E_WALLET, BANK_TRANSFER
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
    val selectedPaymentMethod: PaymentMethodType = PaymentMethodType.COD
)

@Composable
fun CheckoutScreen(
    selectedCartItemIds: List<String>,
    onBackClick: () -> Unit,
    onPlaceOrderSuccess: () -> Unit,
    onNavigateToAddressSelection: () -> Unit,
    viewModel: OrderViewModel = hiltViewModel(),
    cartViewModel: CartViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    val orderState by viewModel.orderState.collectAsState()
    val cartState by cartViewModel.cartState.collectAsState()

    var isLoading by remember { mutableStateOf(false) }

    var uiState by remember {
        mutableStateOf(
            CheckoutUiState(
                userName = "Khánh Ly",
                phone = "036 363 2030",
                address = "18 ngõ 219 đường Nguyễn Ngọc Vũ",
                items = emptyList(),
                subtotal = 0.0,
                shippingFee = 0.0,
                discount = 0.0,
                totalAmount = 0.0,
                selectedPaymentMethod = PaymentMethodType.COD
            )
        )
    }

    LaunchedEffect(Unit) {
        cartViewModel.loadCartItems()
    }

    LaunchedEffect(cartState) {
        if (cartState is CartState.CartItemsSuccess) {
            val allCartItems = (cartState as CartState.CartItemsSuccess).items

            val selectedItems = allCartItems.filter { it.cartItemId in selectedCartItemIds }

            val mappedItems = selectedItems.map { res ->
                CheckoutItem(
                    id = res.cartItemId,
                    name = res.productName,
                    description = "Đơn giá: ${res.price} đ / ${res.unit}",
                    quantityDesc = "${res.quantity} ${res.unit}",
                    price = res.price * res.quantity,
                    imageUrl = res.thumbnail
                )
            }

            val subtotal = selectedItems.sumOf { it.price * it.quantity }
            val shippingFee = if (mappedItems.isEmpty()) 0.0 else 35000.0
            val discount = 0.0
            val totalAmount = subtotal + shippingFee - discount

            uiState = uiState.copy(
                items = mappedItems,
                subtotal = subtotal,
                shippingFee = shippingFee,
                discount = discount,
                totalAmount = totalAmount
            )
        }
    }

    LaunchedEffect(orderState) {
        when (val state = orderState) {
            is OrderState.Loading -> isLoading = true
            is OrderState.Success -> {
                isLoading = false
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                onPlaceOrderSuccess()
            }
            is OrderState.Error -> {
                isLoading = false
                Toast.makeText(context, state.error, Toast.LENGTH_LONG).show()
            }
            else -> isLoading = false
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CheckoutContent(
            uiState = uiState,
            onBackClick = onBackClick,
            onChangeAddressClick = onNavigateToAddressSelection,
            onPaymentMethodSelect = { method ->
                uiState = uiState.copy(selectedPaymentMethod = method)
            },
            onPlaceOrderClick = {
                viewModel.checkout(
                    shippingAddress = uiState.address,
                    phoneNumber = uiState.phone,
                    selectedCartItemIds = selectedCartItemIds
                )
            }
        )

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
fun CheckoutContent(
    uiState: CheckoutUiState,
    onBackClick: () -> Unit,
    onChangeAddressClick: () -> Unit,
    onPaymentMethodSelect: (PaymentMethodType) -> Unit,
    onPlaceOrderClick: () -> Unit
) {
    val formatter = DecimalFormat("#,###")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { CheckoutTopBar(onBackClick) },
        bottomBar = { CheckoutBottomBar(onPlaceOrderClick) }
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
                    icon = Icons.Default.Wallet,
                    title = stringResource(R.string.checkout_e_wallet),
                    description = stringResource(R.string.checkout_e_wallet_desc),
                    isSelected = uiState.selectedPaymentMethod == PaymentMethodType.E_WALLET,
                    onClick = { onPaymentMethodSelect(PaymentMethodType.E_WALLET) }
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
                    formatter = formatter
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
fun PriceSummaryCard(
    subtotal: Double,
    shippingFee: Double,
    discount: Double,
    totalAmount: Double,
    formatter: DecimalFormat
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
            "${formatter.format(shippingFee).replace(',', '.')} đ"
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
                text = "${formatter.format(totalAmount).replace(',', '.')} đ",
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
fun CheckoutBottomBar(onPlaceOrderClick: () -> Unit) {
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
                        text = stringResource(R.string.checkout_place_order),
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