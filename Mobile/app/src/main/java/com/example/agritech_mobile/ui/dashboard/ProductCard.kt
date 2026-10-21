package com.example.agritech_mobile.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.data.remote.dto.DeliveryStatus
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.ui.tooling.preview.Preview
import com.example.agritech_mobile.ui.theme.AgritechTheme

fun formatProductPrice(price: Double): String {
    val formatter = NumberFormat.getNumberInstance(
        Locale.forLanguageTag("vi-VN")
    ).apply {
        maximumFractionDigits = 2
    }

    return "${formatter.format(price)} đ"
}

@Composable
fun ProductImage(
    imageUrl: String,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    var showLogo by remember(imageUrl) {
        mutableStateOf(true)
    }

    Box(
        modifier = modifier.background(
            MaterialTheme.colorScheme.surfaceVariant
        ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = imageUrl.takeIf { it.isNotBlank() },
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onLoading = { showLogo = true },
            onSuccess = { showLogo = false },
            onError = { showLogo = true }
        )

        if (showLogo) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            R.drawable.farm_svgrepo_com
                        ),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Box {
            ProductImage(
                imageUrl = product.imageUrl,
                contentDescription = product.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )

            if (product.availableQuantity <= 0.0) {
                Surface(
                    modifier = Modifier.align(Alignment.BottomStart).padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ) {
                    Text("Hết hàng", modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.labelMedium)
                }
            }

            if (product.isNew) {
                Surface(
                    modifier = Modifier.padding(8.dp),
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Text(
                        text = "Mới",
                        modifier = Modifier.padding(
                            horizontal = 8.dp,
                            vertical = 4.dp
                        ),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (product.deliveryStatus != null) {
                val distance = product.distanceKm?.let {
                    String.format(Locale.forLanguageTag("vi-VN"), "%.1f km", it)
                }
                val label = when (product.deliveryStatus) {
                    DeliveryStatus.AVAILABLE -> distance?.let { "Cách bạn khoảng $it" } ?: "Trong phạm vi giao hàng"
                    DeliveryStatus.OUT_OF_RANGE -> "Ngoài phạm vi giao hàng${distance?.let { " · $it" }.orEmpty()}"
                    DeliveryStatus.SELLER_ADDRESS_REQUIRED -> "Người bán chưa có vị trí lấy hàng"
                    else -> "Cần bổ sung vị trí nhận hàng"
                }
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (product.deliveryStatus == DeliveryStatus.AVAILABLE)
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Text(
                text = product.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = buildString {
                    append(product.priceStr)
                    if (product.unit.isNotBlank()) {
                        append(" / ")
                        append(product.unit)
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text(
                    text = product.seller.ifBlank { "Nhà vườn" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = if (product.reviewCount > 0) {
                        val ratingText = String.format(
                            Locale.forLanguageTag("vi-VN"),
                            "%.1f",
                            product.rating
                        )
                        "$ratingText (${product.reviewCount})"
                    } else {
                        "Chưa có đánh giá"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Preview(
    name = "Product cards",
    showBackground = true,
    widthDp = 380,
    locale = "vi"
)
@Composable
private fun ProductCardPreview() {
    AgritechTheme {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProductCard(
                    product = Product(
                        id = "1",
                        name = "Hạt điều rang muối Bình Phước",
                        priceStr = formatProductPrice(150000.0),
                        seller = "Nông sản Hoàng",
                        isNew = true,
                        imageUrl = "",
                        unit = "hộp 500g",
                        rating = 4.8,
                        reviewCount = 24
                    ),
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )

                ProductCard(
                    product = Product(
                        id = "2",
                        name = "Cà chua bi",
                        priceStr = formatProductPrice(45000.0),
                        seller = "Nhà vườn Đà Lạt",
                        imageUrl = "",
                        unit = "kg"
                    ),
                    modifier = Modifier.weight(1f),
                    onClick = {}
                )
            }
        }
    }
}
