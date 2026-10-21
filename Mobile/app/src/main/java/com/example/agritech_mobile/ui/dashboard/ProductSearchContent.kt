package com.example.agritech_mobile.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ProductSearchContent(
    state: ProductSearchUiState,
    onExecuteSearch: (String) -> Unit,
    onProductClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isSearching || state.isSuggesting) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    key(state.query, state.farmerId, state.hasSearched) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.hasSearched) {
                item {
                    Text(
                        text = "Tìm thấy ${state.totalElements} sản phẩm",
                        style = MaterialTheme.typography.titleSmall
                    )
                }

                if (state.products.isEmpty()) {
                    item {
                        Text("Không tìm thấy sản phẩm phù hợp.")
                    }
                } else {
                    items(state.products.chunked(2)) { rowProducts ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowProducts.forEach { product ->
                                SuggestedProductCard(
                                    product = Product(
                                        id = product.id,
                                        name = product.name,
                                        priceStr = "${product.price.toLong()} đ",
                                        seller = product.farmerName,
                                        imageUrl = product.imageUrls.firstOrNull().orEmpty(),
                                        rating = product.rating,
                                        reviewCount = product.reviewCount,
                                        availableQuantity = product.quantity,
                                        unit = product.unit,
                                        distanceKm = product.distanceKm,
                                        deliveryStatus = product.deliveryStatus
                                    ),
                                    modifier = Modifier.weight(1f),
                                    onClick = { onProductClick(product.id) }
                                )
                            }

                            if (rowProducts.size == 1) {
                                Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            } else {
                if (state.suggestions.isNotEmpty()) {
                    items(state.suggestions) { suggestion ->
                        TextSuggestionItem(
                            text = suggestion,
                            onClick = { onExecuteSearch(suggestion) }
                        )
                    }
                } else if (state.error == null) {
                    item {
                        Text(
                            text = if (state.query.isBlank()) {
                                "Nhập tên sản phẩm để tìm kiếm."
                            } else {
                                "Chưa có gợi ý. Bấm tìm kiếm để xem kết quả."
                            }
                        )
                    }
                }
            }

            state.error?.let { error ->
                item {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                if (!state.hasSearched && state.query.isNotBlank()) {
                    item {
                        OutlinedButton(
                            onClick = { onExecuteSearch(state.query) }
                        ) {
                            Text("Tìm kiếm lại")
                        }
                    }
                }
            }

            if (state.hasSearched && state.hasMore) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isLoadingMore) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(28.dp)
                            )
                        } else {
                            OutlinedButton(onClick = onLoadMore) {
                                Text(
                                    if (state.error != null) {
                                        "Thử tải lại"
                                    } else {
                                        "Tải thêm"
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
