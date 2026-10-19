package com.example.agritech_mobile.ui.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.agritech_mobile.data.remote.dto.ProductCostResponse
import com.example.agritech_mobile.data.repository.ProfitRepository
import com.example.agritech_mobile.ui.theme.AgritechTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import retrofit2.HttpException
import javax.inject.Inject

data class ProductCostsUiState(
    val products: List<ProductCostResponse> = emptyList(),
    val isLoading: Boolean = false,
    val savingProductId: String? = null,
    val error: String? = null,
    val saveError: String? = null
)

@HiltViewModel
class ProductCostsViewModel @Inject constructor(
    private val repository: ProfitRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductCostsUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadProducts()
    }

    fun loadProducts() {
        if (_uiState.value.isLoading ||
            _uiState.value.savingProductId != null
        ) {
            return
        }

        _uiState.update {
            it.copy(isLoading = true, error = null)
        }

        viewModelScope.launch {
            try {
                val products = repository.getMyProductCosts()

                _uiState.update {
                    it.copy(
                        products = products,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = requestError(e)
                    )
                }
            }
        }
    }

    fun clearSaveError() {
        _uiState.update { it.copy(saveError = null) }
    }

    fun saveCost(
        productId: String,
        input: String,
        onSaved: () -> Unit
    ) {
        if (_uiState.value.isLoading ||
            _uiState.value.savingProductId != null
        ) {
            return
        }

        val cost = input.trim().toDoubleOrNull()

        if (cost == null || !cost.isFinite() || cost < 0.0) {
            _uiState.update {
                it.copy(saveError = "Nhập số không âm, ví dụ 15000.")
            }
            return
        }

        _uiState.update {
            it.copy(
                savingProductId = productId,
                saveError = null
            )
        }

        viewModelScope.launch {
            try {
                val updated = repository.updateProductCost(
                    productId = productId,
                    costPrice = cost
                )

                _uiState.update { state ->
                    state.copy(
                        products = state.products.map {
                            if (it.productId == productId) updated else it
                        },
                        savingProductId = null,
                        saveError = null
                    )
                }

                onSaved()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        savingProductId = null,
                        saveError = requestError(e)
                    )
                }
            }
        }
    }

    private fun requestError(exception: Exception): String {
        return when (exception) {
            is HttpException -> when (exception.code()) {
                400 -> "Giá vốn không hợp lệ."
                401 -> "Vui lòng đăng nhập lại."
                403 -> "Bạn không có quyền sửa vốn sản phẩm này."
                404 -> "Không tìm thấy sản phẩm hoặc API."
                else -> "Thao tác thất bại (${exception.code()})."
            }
            else -> "Không kết nối được máy chủ. Vui lòng thử lại."
        }
    }
}

@Composable
fun ProductCostsScreen(
    onBackClick: () -> Unit,
    viewModel: ProductCostsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    ProductCostsContent(
        state = state,
        onBackClick = onBackClick,
        onRetry = viewModel::loadProducts,
        onClearSaveError = viewModel::clearSaveError,
        onSaveCost = viewModel::saveCost
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCostsContent(
    state: ProductCostsUiState,
    onBackClick: () -> Unit = {},
    onRetry: () -> Unit = {},
    onClearSaveError: () -> Unit = {},
    onSaveCost: (String, String, () -> Unit) -> Unit = { _, _, _ -> }
) {
    var editingId by rememberSaveable {
        mutableStateOf<String?>(null)
    }

    val editingProduct = state.products.firstOrNull {
        it.productId == editingId
    }

    val saving = state.savingProductId != null

    Scaffold(
        containerColor = Color(0xFFFAFBFA),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Quản lý giá vốn",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            "Quay lại"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRetry,
                        enabled = !state.isLoading && !saving
                    ) {
                        Icon(Icons.Default.Refresh, "Tải lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = StatisticsGreen,
                    navigationIconContentColor = StatisticsGreen,
                    actionIconContentColor = StatisticsGreen
                ),
            )
        }
    ) { padding ->
        if (state.isLoading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = StatisticsGreen)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    ProductCostsOverview(
                        products = state.products
                    )
                }

                item {
                    Text(
                        "Nhập vốn cho 1 đơn vị sản phẩm (kg, hộp…). " +
                                "Vốn mới áp dụng cho đơn đặt sau khi lưu. " +
                                "Giá vốn và lợi nhuận các đơn cũ giữ nguyên.",
                        color = Color.Gray
                    )
                }

                state.error?.let { error ->
                    item {
                        Text(
                            error,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = onRetry,
                            enabled = !saving
                        ) {
                            Text("Thử lại")
                        }
                    }
                }

                if (state.products.isEmpty() && state.error == null) {
                    item {
                        Text("Bạn chưa có sản phẩm để nhập giá vốn.")
                    }
                }

                items(
                    items = state.products,
                    key = { it.productId }
                ) { product ->
                    ProductCostCard(
                        product = product,
                        enabled = !saving,
                        onEditClick = {
                            onClearSaveError()
                            editingId = product.productId
                        }
                    )
                }
            }
        }
    }

    editingProduct?.let { product ->
        var input by rememberSaveable(product.productId) {
            mutableStateOf(product.costPrice?.toString() ?: "")
        }

        val cost = input.trim().toDoubleOrNull()
            ?.takeIf { it.isFinite() && it >= 0.0 }

        AlertDialog(
            containerColor = Color.White,
            titleContentColor = StatisticsGreen,
            shape = RoundedCornerShape(24.dp),
            icon = {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = StatisticsGreen,
                        modifier = Modifier.padding(14.dp).size(28.dp)
                    )
                }
            },
            onDismissRequest = {
                if (!saving) {
                    editingId = null
                    onClearSaveError()
                }
            },
            title = { Text(product.productName) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                            onClearSaveError()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !saving,
                        singleLine = true,
                        label = { Text("Giá vốn (đ/${product.unit})") },
                        placeholder = { Text("Ví dụ: 15000") },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        ),
                        isError = input.isNotBlank() && cost == null,
                        supportingText = {
                            Text("Nhập số không âm. Nhập 0 nếu vốn bằng 0.")
                        }
                    )

                    cost?.let {
                        Text(
                            "Lãi dự kiến/đơn vị: " +
                                    statisticsMoney(product.sellingPrice - it)
                        )
                    }

                    Text(
                        "Không thay đổi giá vốn của đơn đã đặt.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )

                    state.saveError?.let {
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = !saving && cost != null,
                    onClick = {
                        onSaveCost(product.productId, input) {
                            editingId = null
                        }
                    }
                ) {
                    Text(if (saving) "Đang lưu…" else "Lưu")
                }
            },
            dismissButton = {
                TextButton(
                    enabled = !saving,
                    onClick = {
                        editingId = null
                        onClearSaveError()
                    }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
private fun ProductCostsOverview(
    products: List<ProductCostResponse>
) {
    val entered = products.count { it.costPrice != null }
    val missing = products.size - entered

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF14532D),
                        Color(0xFF23834B)
                    )
                )
            )
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                color = Color.White.copy(alpha = 0.15f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalanceWallet,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.padding(12.dp).size(26.dp)
                )
            }

            Column {
                Text(
                    text = "Giá vốn sản phẩm",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = "${products.size} sản phẩm của bạn",
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                "Đã nhập vốn" to entered,
                "Chưa nhập vốn" to missing
            ).forEach { (label, count) ->
                Surface(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.13f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCostCard(
    product: ProductCostResponse,
    enabled: Boolean,
    onEditClick: () -> Unit
) {
    val missing = product.costPrice == null

    val statusColor = if (missing) {
        Color(0xFFB45309)
    } else {
        Color(0xFF15803D)
    }

    val statusBackground = if (missing) {
        Color(0xFFFFF7ED)
    } else {
        Color(0xFFECFDF3)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        border = BorderStroke(
            1.dp,
            if (missing) Color(0xFFFED7AA) else Color(0xFFDCF0E2)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    color = Color(0xFFE8F5E9),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = StatisticsGreen,
                        modifier = Modifier.padding(12.dp).size(24.dp)
                    )
                }

                Text(
                    text = product.productName,
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )

                Surface(
                    color = statusBackground,
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (missing) "Chưa nhập" else "Đã nhập",
                        modifier = Modifier.padding(
                            horizontal = 10.dp,
                            vertical = 6.dp
                        ),
                        color = statusColor,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Giá bán / ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )

                    Text(
                        statisticsMoney(product.sellingPrice),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "Giá vốn / ${product.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6B7280)
                    )

                    Text(
                        product.costPrice?.let {
                            statisticsMoney(it)
                        } ?: "Chưa có dữ liệu",
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            product.costPrice?.let { cost ->
                val profit = product.sellingPrice - cost
                val loss = profit < 0.0

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = if (loss) {
                        Color(0xFFFEF2F2)
                    } else {
                        Color(0xFFF0FDF4)
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val profitColor = if (loss) {
                            Color(0xFFDC2626)
                        } else {
                            StatisticsGreen
                        }

                        Icon(
                            imageVector = if (loss) {
                                Icons.Default.TrendingDown
                            } else {
                                Icons.Default.TrendingUp
                            },
                            contentDescription = null,
                            tint = profitColor
                        )

                        Column {
                            Text(
                                "Lãi gộp dự kiến / ${product.unit}",
                                style = MaterialTheme.typography.bodySmall,
                                color = profitColor
                            )

                            Text(
                                statisticsMoney(profit),
                                fontWeight = FontWeight.Bold,
                                color = profitColor
                            )
                        }
                    }
                }
            }

            Button(
                onClick = onEditClick,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (missing) {
                        StatisticsGreen
                    } else {
                        Color(0xFFE8F5E9)
                    },
                    contentColor = if (missing) {
                        Color.White
                    } else {
                        StatisticsGreen
                    }
                )
            ) {
                Icon(
                    imageVector = if (missing) {
                        Icons.Default.Add
                    } else {
                        Icons.Default.Edit
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(Modifier.width(8.dp))

                Text(if (missing) "Nhập giá vốn" else "Chỉnh sửa giá vốn")
            }
        }
    }
}

@Preview(
    name = "Quản lý giá vốn",
    showBackground = true,
    locale = "vi",
    widthDp = 411,
    heightDp = 850
)
@Composable
private fun ProductCostsPreview() {
    AgritechTheme {
        ProductCostsContent(
            state = ProductCostsUiState(
                products = listOf(
                    ProductCostResponse(
                        productId = "01",
                        productName = "Cà chua",
                        unit = "kg",
                        sellingPrice = 25_000.0,
                        costPrice = 15_000.0,
                        quantity = 100.0
                    ),
                    ProductCostResponse(
                        productId = "02",
                        productName = "Cam",
                        unit = "kg",
                        sellingPrice = 40_000.0,
                        costPrice = null,
                        quantity = 50.0
                    )
                )
            )
        )
    }
}