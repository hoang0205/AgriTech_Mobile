package com.example.agritech_mobile.ui.cart

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.ui.theme.AgritechTheme

data class CartItem(
    val id: String,
    val name: String,
    val price: Double,
    val unit: String,
    val farmer: String,
    val farmerId: String,
    val quantity: Double,
    val imageUrl: String,
    val isSelected: Boolean = false
)

@Composable
fun CartScreen(
    cartViewModel: CartViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val cartState by cartViewModel.cartState.collectAsState()

    var cartItems by remember { mutableStateOf<List<CartItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        cartViewModel.loadCartItems()
    }

    LaunchedEffect(cartState) {
        when (val state = cartState) {
            is CartState.Loading -> {
                isLoading = true
            }

            is CartState.CartItemsSuccess -> {
                isLoading = false
                cartItems = state.items.map { res ->
                    val oldItem = cartItems.find { it.id == res.cartItemId }
                    CartItem(
                        id = res.cartItemId,
                        name = res.productName,
                        price = res.price,
                        unit = res.unit,
                        farmer = res.farmerName,
                        farmerId = res.farmerId,
                        quantity = res.quantity,
                        imageUrl = res.thumbnail,
                        isSelected = oldItem?.isSelected ?: false
                    )
                }
            }

            is CartState.ActionSuccess -> {
                isLoading = false
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }

            is CartState.Error -> {
                isLoading = false
                Toast.makeText(context, state.error, Toast.LENGTH_SHORT).show()
            }

            is CartState.Idle -> {
                isLoading = false
            }
        }
    }

    CartContent(
        cartItems = cartItems,
        isLoading = isLoading,
        onShopCheckedChange = { sellerId, isChecked ->
            cartItems = cartItems.map {
                if (it.farmerId == sellerId) it.copy(isSelected = isChecked) else it
            }
        },
        onItemCheckedChange = { itemId, isChecked ->
            cartItems =
                cartItems.map { if (it.id == itemId) it.copy(isSelected = isChecked) else it }
        },
        onIncrease = { item -> cartViewModel.updateQuantity(item.id, item.quantity + 1.0) },
        onDecrease = { item ->
            if (item.quantity > 1.0) cartViewModel.updateQuantity(
                item.id,
                item.quantity - 1.0
            )
        },
        onRemove = { itemId -> cartViewModel.deleteCartItem(itemId) },
        onCheckout = { }
    )
}

@Composable
fun CartContent(
    cartItems: List<CartItem>,
    isLoading: Boolean,
    onShopCheckedChange: (String, Boolean) -> Unit,
    onItemCheckedChange: (String, Boolean) -> Unit,
    onIncrease: (CartItem) -> Unit,
    onDecrease: (CartItem) -> Unit,
    onRemove: (String) -> Unit,
    onCheckout: () -> Unit
) {
    val selectedItems = cartItems.filter { it.isSelected }
    val subtotal = selectedItems.sumOf { it.price * it.quantity }
    val deliveryFee = if (selectedItems.isEmpty()) 0.0 else 8.50
    val tax = 0.00
    val total = subtotal + deliveryFee + tax

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            CartTopBar(itemCount = cartItems.size)
            val groupedItems = cartItems.groupBy { it.farmerId }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                groupedItems.forEach { (sellerId, itemsOfSeller) ->
                    val sellerName = itemsOfSeller.first().farmer

                    item {
                        val isAllShopItemSelected =
                            itemsOfSeller.isNotEmpty() && itemsOfSeller.all { it.isSelected }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp, bottom = 4.dp)
                        ) {
                            Checkbox(
                                checked = isAllShopItemSelected,
                                onCheckedChange = { isChecked ->
                                    onShopCheckedChange(sellerId, isChecked)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                Icons.Default.Storefront,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = sellerName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    items(itemsOfSeller) { item ->
                        Box(modifier = Modifier.padding(start = 12.dp)) {
                            CartItemCard(
                                item = item,
                                onCheckedChange = { isChecked ->
                                    onItemCheckedChange(
                                        item.id,
                                        isChecked
                                    )
                                },
                                onIncrease = { onIncrease(item) },
                                onDecrease = { onDecrease(item) },
                                onRemove = { onRemove(item.id) }
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    OrderSummaryCard(
                        subtotal = subtotal,
                        itemCount = selectedItems.size,
                        deliveryFee = deliveryFee,
                        tax = tax,
                        total = total,
                        isButtonEnabled = selectedItems.isNotEmpty(),
                        onCheckout = onCheckout
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }

        if (isLoading && cartItems.isEmpty()) {
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
fun CartTopBar(itemCount: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.cart_title, itemCount),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = {}) {
            Icon(
                Icons.Default.Notifications,
                contentDescription = "Notification",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CartItemCard(
    item: CartItem,
    onCheckedChange: (Boolean) -> Unit,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    val displayQuantity = if (item.quantity % 1.0 == 0.0) {
        item.quantity.toLong().toString()
    } else {
        item.quantity.toString()
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.isSelected,
            onCheckedChange = { onCheckedChange(it) },
            colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary)
        )

        Spacer(modifier = Modifier.width(8.dp))

        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                AsyncImage(
                    model = item.imageUrl,
                    contentDescription = item.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = item.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${String.format("%.2f", item.price)} VND/${item.unit}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Eco,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.cart_grown_by),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = item.farmer,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDecrease, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = "Decrease",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = displayQuantity,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                            IconButton(onClick = onIncrease, modifier = Modifier.size(32.dp)) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = "Increase",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .clickable { onRemove() }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.DeleteOutline,
                                contentDescription = "Remove",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.cart_remove),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderSummaryCard(
    subtotal: Double,
    itemCount: Int,
    deliveryFee: Double,
    tax: Double,
    total: Double,
    isButtonEnabled: Boolean,
    onCheckout: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.cart_order_summary),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            SummaryRow(
                stringResource(R.string.cart_subtotal, itemCount),
                "${String.format("%.2f", subtotal)} VND"
            )
            Spacer(modifier = Modifier.height(12.dp))
            SummaryRow(
                stringResource(R.string.cart_delivery_fee),
                "${String.format("%.2f", deliveryFee)} VND"
            )
            Spacer(modifier = Modifier.height(12.dp))
            SummaryRow(
                stringResource(R.string.cart_tax_exempt),
                "${String.format("%.2f", tax)} VND",
                isValueGreen = true
            )

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.cart_total_payable),
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${String.format("%.2f", total)} VND",
                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Button(
                    onClick = onCheckout,
                    enabled = isButtonEnabled,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = stringResource(R.string.checkout), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isValueGreen: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            color = if (isValueGreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true, locale = "vi")
@Composable
fun CartScreenPreview() {
    AgritechTheme {
        CartContent(
            cartItems = listOf(
                CartItem(
                    id = "1",
                    name = "Heirloom Rainbow Carrots",
                    price = 4.5,
                    unit = "kg",
                    farmer = "Meadowbrook Farms",
                    farmerId = "f1",
                    quantity = 2.0,
                    imageUrl = "https://images.unsplash.com/photo-1598170845058-32b9d6a5da37?q=80&w=600&auto=format&fit=crop",
                    isSelected = true
                ),
                CartItem(
                    id = "2",
                    name = "Russet Earth-Bound Potatoes",
                    price = 12.0,
                    unit = "5kg",
                    farmer = "Green Valley Coop",
                    farmerId = "f2",
                    quantity = 1.0,
                    imageUrl = "https://images.unsplash.com/photo-1518977676601-b53f82aba655?q=80&w=600&auto=format&fit=crop",
                    isSelected = false
                )
            ),
            isLoading = false,
            onShopCheckedChange = { _, _ -> },
            onItemCheckedChange = { _, _ -> },
            onIncrease = {},
            onDecrease = {},
            onRemove = {},
            onCheckout = {}
        )
    }
}