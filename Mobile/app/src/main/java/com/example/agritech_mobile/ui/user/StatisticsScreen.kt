package com.example.agritech_mobile.ui.user

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.agritech_mobile.data.remote.dto.StatisticsCategoryDto
import com.example.agritech_mobile.data.remote.dto.StatisticsMonthDto
import com.example.agritech_mobile.data.remote.dto.StatisticsOrderDto
import com.example.agritech_mobile.data.remote.dto.StatisticsOrderItemDto
import com.example.agritech_mobile.data.remote.dto.StatisticsOverviewDto
import com.example.agritech_mobile.data.remote.dto.StatisticsSectionDto
import com.example.agritech_mobile.data.remote.dto.SellerFinanceSummary
import com.example.agritech_mobile.data.remote.dto.SellerOrderFinance
import com.example.agritech_mobile.data.remote.dto.SellerTaxEstimate
import com.example.agritech_mobile.ui.theme.AgritechTheme
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import com.example.agritech_mobile.ui.order.DeliveryManagementDialog
import com.example.agritech_mobile.ui.order.GhnShipmentDialog
import com.example.agritech_mobile.R

internal val StatisticsGreen = Color(0xFF14532D)

internal fun statisticsMoney(value: Double): String {
    return NumberFormat.getNumberInstance(Locale("vi", "VN"))
        .format(value) + " đ"
}

@Composable
fun StatisticsScreen(
    onBackClick: () -> Unit,
    onEditProduct: (String) -> Unit = {},
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.retry() }

    StatisticsScreenContent(
        state = state,
        currentYear = viewModel.currentYear,
        onBackClick = onBackClick,
        onRetry = viewModel::retry,
        onSelectTab = viewModel::selectTab,
        onSelectYear = viewModel::selectYear,
        onSelectMonth = viewModel::selectMonth,
        onEditProduct = onEditProduct,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreenContent(
    state: StatisticsUiState,
    currentYear: Int,
    onBackClick: () -> Unit = {},
    onRetry: () -> Unit = {},
    onSelectTab: (StatisticsTab) -> Unit = {},
    onSelectYear: (Int) -> Unit = {},
    onSelectMonth: (Int?) -> Unit = {},
    onEditProduct: (String) -> Unit = {}
) {
    var financeOrderId by remember { mutableStateOf<String?>(null) }
    var ghnOrderId by remember { mutableStateOf<String?>(null) }
    financeOrderId?.let { id ->
        DeliveryManagementDialog(id, onDismiss = { financeOrderId = null }, onChanged = onRetry,
            onGhn = { financeOrderId = null; ghnOrderId = id })
    }
    ghnOrderId?.let { id ->
        GhnShipmentDialog(id, seller = true, onDismiss = { ghnOrderId = null }, onChanged = onRetry)
    }
    val finance = state.overview?.sellerFinance
    val tabs = state.availableTabs
    val tabIndex = tabs.indexOf(state.selectedTab).coerceAtLeast(0)
    val listState = rememberLazyListState()
    val chartScroll = rememberScrollState()

    val period = state.selectedMonth?.let {
        "Tháng $it/${state.selectedYear}"
    } ?: "Cả năm ${state.selectedYear}"

    val isProfit = state.selectedTab == StatisticsTab.PROFIT

    Scaffold(
        containerColor = Color(0xFFFAFBFA),
        topBar = {
            TopAppBar(
                title = { Text("Thống kê", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRetry,
                        enabled = !state.isLoading
                    ) {
                        Icon(Icons.Default.Refresh, "Tải lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding)
        ) {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = Color.White,
                contentColor = StatisticsGreen
            ) {
                tabs.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { onSelectTab(tab) },
                        text = {
                            Text(tab.title, fontWeight = FontWeight.Bold)
                        }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    enabled = state.selectedYear > 1,
                    onClick = { onSelectYear(state.selectedYear - 1) }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        "Năm trước"
                    )
                }

                Text(
                    "Năm ${state.selectedYear}",
                    fontWeight = FontWeight.Bold
                )

                IconButton(
                    enabled = state.selectedYear < currentYear,
                    onClick = { onSelectYear(state.selectedYear + 1) }
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        "Năm sau"
                    )
                }
            }

            Box(Modifier.fillMaxWidth().weight(1f)) {
                when {
                    state.isLoading -> {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center),
                            color = StatisticsGreen
                        )
                    }

                    state.error != null -> {
                        Column(
                            modifier = Modifier.align(Alignment.Center)
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                state.error.orEmpty(),
                                color = MaterialTheme.colorScheme.error
                            )
                            TextButton(onClick = onRetry) {
                                Text("Thử lại")
                            }
                        }
                    }

                    else -> {
                        state.activeSection?.let { section ->
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                state = listState,
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                if (state.selectedTab != StatisticsTab.SPENDING && finance != null) {
                                    item { SellerFinanceCard(finance, isProfit = isProfit, period = period) }
                                }
                                if (isProfit) {
                                    state.overview?.sellerTax?.let { tax ->
                                        item { SellerTaxCard(tax, state.selectedMonth) }
                                    }
                                }
                                if (state.selectedTab == StatisticsTab.SPENDING || finance == null) item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = StatisticsGreen
                                        )
                                    ) {
                                        Column(
                                            Modifier.padding(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(period, color = Color.White)

                                            Text(
                                                text = if (isProfit) {
                                                    "Lãi gộp đã tính"
                                                } else {
                                                    state.selectedTab.title
                                                },
                                                color = Color.White.copy(alpha = 0.8f)
                                            )

                                            Text(
                                                statisticsMoney(section.totalAmount),
                                                style = MaterialTheme.typography.headlineMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )

                                            Text(
                                                "${section.orderCount} đơn hoàn thành",
                                                color = Color.White
                                            )

                                            if (isProfit) {
                                                Text(
                                                    "Vốn hàng đã bán: " +
                                                            statisticsMoney(section.totalCost),
                                                    color = Color.White
                                                )
                                                Text(
                                                    "Chưa trừ vận chuyển, phí và chi phí khác.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = Color.White.copy(alpha = 0.8f)
                                                )
                                            }
                                        }
                                    }
                                }
                                if (isProfit && finance == null && section.missingCostItems > 0) {
                                    item {
                                        Text(
                                            text = "${section.missingCostItems} dòng sản phẩm " +
                                                    "chưa nhập giá vốn, tạm tính vốn 0 đ.",
                                            color = Color(0xFF9A3412)
                                        )
                                    }
                                }

                                item {
                                    StatisticsCard {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                (if (isProfit) "Lãi gộp · " else "12 tháng · ") + state.selectedYear,
                                                fontWeight = FontWeight.Bold
                                            )
                                            TextButton(
                                                onClick = { onSelectMonth(null) },
                                                enabled = state.selectedMonth != null
                                            ) {
                                                Text("Cả năm")
                                            }
                                        }

                                        StatisticsMonthlyChart(
                                            months = section.monthlyAmounts,
                                            selectedMonth = state.selectedMonth,
                                            scrollState = chartScroll,
                                            onSelectMonth = onSelectMonth
                                        )

                                        Text(
                                            "Chạm tháng để lọc · Theo ngày đặt hàng" +
                                                    (if (isProfit) " · Chưa gồm vận chuyển" else ""),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                if (!isProfit) item {
                                    StatisticsCategories(
                                        categories = section.categories,
                                        isProfit = isProfit,
                                        period = period
                                    )
                                }

                                if (isProfit && finance != null) {
                                    item { Text("Kết quả từng đơn · $period", fontWeight = FontWeight.Bold) }
                                    if (finance.orders.isEmpty()) item {
                                        Text("Chưa có đơn hoàn thành hoặc chi phí đơn hủy trong khoảng này.",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    items(finance.orders, key = { "finance-${it.orderId}" }) { row ->
                                        SellerFinanceOrderCard(row,
                                            onEditCost = { financeOrderId = row.orderId.toString() },
                                            onEditProduct = onEditProduct)
                                    }
                                }
                                if (!isProfit || finance == null) {
                                item {
                                    Text(
                                        (if (isProfit) "Lãi gộp theo sản phẩm · " else "Đơn hoàn thành · ") + period,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                if (section.orders.isEmpty()) {
                                    item {
                                        Text("Chưa có đơn hoàn thành trong khoảng này.")
                                    }
                                }

                                items(
                                    items = section.orders,
                                    key = { it.orderId }
                                ) { order ->
                                    StatisticsOrderCard(order, isProfit = isProfit,
                                        isSpending = state.selectedTab == StatisticsTab.SPENDING,
                                        onEditProduct = onEditProduct.takeIf { state.selectedTab != StatisticsTab.SPENDING })
                                }
                                }

                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun StatisticsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
internal fun StatisticsValueRow(
    label: String,
    amount: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Text(
            statisticsMoney(amount),
            fontWeight = FontWeight.Bold,
            color = if (amount < 0) {
                MaterialTheme.colorScheme.error
            } else {
                StatisticsGreen
            }
        )
    }
}

@Composable
internal fun StatisticsMonthlyChart(
    months: List<StatisticsMonthDto>,
    selectedMonth: Int?,
    scrollState: ScrollState,
    onSelectMonth: (Int?) -> Unit
) {
    val ordered = (1..12).map { month ->
        months.firstOrNull { it.month == month }
            ?: StatisticsMonthDto(month, 0.0, 0, 0)
    }

    val maximum = (ordered.maxOfOrNull { it.amount } ?: 0.0)
        .coerceAtLeast(0.0)

    val minimum = (ordered.minOfOrNull { it.amount } ?: 0.0)
        .coerceAtMost(0.0)

    val range = (maximum - minimum).takeIf { it > 0.0 } ?: 1.0

    val positiveHeight = if (maximum == 0.0 && minimum == 0.0) {
        160f
    } else {
        (160.0 * maximum / range).toFloat()
    }

    val negativeHeight = 160f - positiveHeight

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Cao nhất: ${statisticsMoney(maximum)}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )

        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ordered.forEach { month ->
                val selected = month.month == selectedMonth
                val barHeight = (160.0 * abs(month.amount) / range).toFloat()

                val barColor = when {
                    month.amount < 0 -> Color(0xFFDC2626)
                    selected -> StatisticsGreen
                    else -> Color(0xFF86C99A)
                }

                Column(
                    modifier = Modifier
                        .width(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (selected) Color(0xFFE8F5E9)
                            else Color.Transparent
                        )
                        .selectable(
                            selected = selected,
                            onClick = { onSelectMonth(month.month) }
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription =
                                "Tháng ${month.month}, " +
                                        "${statisticsMoney(month.amount)}, " +
                                        "${month.orderCount} đơn, " +
                                        "${month.missingCostItems} dòng chưa nhập vốn, tính 0 đ"
                        },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .height(positiveHeight.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (month.amount > 0) {
                            Box(
                                Modifier.width(24.dp)
                                    .height(barHeight.dp)
                                    .clip(RoundedCornerShape(topStart = 5.dp, topEnd = 5.dp))
                                    .background(barColor)
                            )
                        }
                    }

                    Box(
                        Modifier.fillMaxWidth().height(1.dp)
                            .background(Color(0xFF9CA3AF))
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth()
                            .height(negativeHeight.dp),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        if (month.amount < 0) {
                            Box(
                                Modifier.width(24.dp)
                                    .height(barHeight.dp)
                                    .clip(
                                        RoundedCornerShape(
                                            bottomStart = 5.dp,
                                            bottomEnd = 5.dp
                                        )
                                    )
                                    .background(barColor)
                            )
                        }
                    }

                    Text(
                        text = month.month.toString(),
                        modifier = Modifier.padding(top = 8.dp),
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = StatisticsGreen
                    )

                    Text(
                        text = if (month.missingCostItems > 0) "!" else "",
                        color = Color(0xFF9A3412),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        if (minimum < 0) {
            Text(
                "Thấp nhất: ${statisticsMoney(minimum)} · Đỏ: lỗ",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFFDC2626)
            )
        }

        if (ordered.any { it.missingCostItems > 0 }) {
            Text(
                "! Có sản phẩm chưa nhập vốn, tạm tính vốn 0 đ",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF9A3412)
            )
        }
    }
}

private val StatisticsCategoryColors = listOf(
    Color(0xFF16A34A), // Rau củ
    Color(0xFF2563EB), // Trái cây
    Color(0xFFEA580C), // Thịt
    Color(0xFF9333EA), // Thủy hải sản
    Color(0xFFDC2626)  // Khác
)

private fun statisticsCategoryIndex(category: String): Int {
    return when (category.trim().lowercase(Locale.ROOT)) {
        "rau củ", "vegetables" -> 0
        "trái cây", "fruits" -> 1
        "thịt", "meat" -> 2
        "thủy hải sản", "seafood" -> 3
        else -> 4
    }
}

private fun statisticsCategoryColor(category: String): Color {
    return StatisticsCategoryColors[statisticsCategoryIndex(category)]
}

private fun statisticsCategoryIcon(category: String): Int? {
    return when (statisticsCategoryIndex(category)) {
        0 -> R.drawable.vegetable_food_salad_lettuce_cabbage_svgrepo_com
        1 -> R.drawable.fruit_fruits_grape_svgrepo_com
        2 -> R.drawable.meat_svgrepo_com
        3 -> R.drawable.seafood_prawn_shrimp_lobster_svgrepo_com
        else -> null
    }
}

@Composable
internal fun StatisticsCategories(
    categories: List<StatisticsCategoryDto>,
    isProfit: Boolean,
    period: String
) {
    val total = categories.sumOf { it.amount.coerceAtLeast(0.0) }
    val percentUnits = remember(categories) {
        categoryPercentUnits(categories)
    }
    val maximumAbsolute = categories.maxOfOrNull {
        abs(it.amount)
    }?.takeIf { it > 0.0 } ?: 1.0

    StatisticsCard {
        Text(
            (if (isProfit) "Lãi gộp theo danh mục" else "Theo danh mục"),
            fontWeight = FontWeight.Bold
        )

        if (categories.isEmpty()) {
            Text("Chưa có dữ liệu danh mục.", color = Color.Gray)
        } else {
            if (!isProfit) {
                Box(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .size(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val ringWidth = 22.dp.toPx()
                        val diameter = size.minDimension - ringWidth
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )

                        drawArc(
                            color = Color(0xFFE5E7EB),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(ringWidth)
                        )

                        if (total > 0.0) {
                            var startAngle = -90f

                            categories.forEachIndexed { index, category ->
                                val sweep = (
                                        category.amount.coerceAtLeast(0.0) /
                                                total * 360.0
                                        ).toFloat()

                                if (sweep > 0f) {
                                    drawArc(
                                        color = statisticsCategoryColor(category.category),
                                        startAngle = startAngle,
                                        sweepAngle = sweep,
                                        useCenter = false,
                                        topLeft = topLeft,
                                        size = Size(diameter, diameter),
                                        style = Stroke(ringWidth)
                                    )
                                }

                                startAngle += sweep
                            }
                        }
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Tiền hàng",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Text(
                            statisticsMoney(total),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            categories.forEachIndexed { index, category ->
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val categoryColor = statisticsCategoryColor(
                        category.category
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Color.White,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    color = categoryColor.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val iconRes = statisticsCategoryIcon(
                                category.category
                            )

                            if (iconRes != null) {
                                Icon(
                                    painter = painterResource(iconRes),
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.MoreHoriz,
                                    contentDescription = null,
                                    tint = categoryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = category.category,
                                fontWeight = FontWeight.Bold
                            )

                            if (!isProfit) {
                                val units = percentUnits[index]

                                Text(
                                    text = "Tỷ trọng ${units / 10},${units % 10}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF6B7280)
                                )
                            }
                        }

                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = statisticsMoney(category.amount),
                            fontWeight = FontWeight.Bold,
                            color = if (category.amount < 0) {
                                MaterialTheme.colorScheme.error
                            } else {
                                StatisticsGreen
                            }
                        )
                    }

                    if (isProfit) {
                        val fraction = (abs(category.amount) / maximumAbsolute)
                            .toFloat().coerceIn(0f, 1f)

                        Box(Modifier.fillMaxWidth().height(14.dp)) {
                            Row(Modifier.fillMaxSize()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                        .background(Color(0xFFFEF2F2)),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    if (category.amount < 0) {
                                        Box(
                                            Modifier.fillMaxWidth(fraction)
                                                .height(10.dp)
                                                .background(Color(0xFFDC2626))
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier.weight(1f).fillMaxHeight()
                                        .background(Color(0xFFF0FDF4)),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (category.amount > 0) {
                                        Box(
                                            Modifier.fillMaxWidth(fraction)
                                                .height(10.dp)
                                                .background(StatisticsGreen)
                                        )
                                    }
                                }
                            }

                            Box(
                                Modifier.align(Alignment.Center)
                                    .width(1.dp).fillMaxHeight()
                                    .background(Color.Gray)
                            )
                        }
                    }

                    if (category.missingCostItems > 0) {
                        Text(
                            "${category.missingCostItems} dòng chưa nhập vốn, tính 0 đ",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9A3412)
                        )
                    }
                }
            }
        }
    }
}

private fun statisticsSampleSection(
    year: Int,
    month: Int?,
    profit: Boolean = false,
    spending: Boolean = false
): StatisticsSectionDto {
    val yearlyOrders = (1..12).map { number ->
        val revenue = if (spending) {
            60_000.0 * number
        } else {
            100_000.0 * number
        }

        val missing = profit && number == 7

        val profitAmount = when (number) {
            4 -> -50_000.0
            else -> 20_000.0 * number
        }

        val amount = when {
            missing -> revenue
            profit -> profitAmount
            else -> revenue
        }

        StatisticsOrderDto(
            orderId = number.toLong(),
            orderDate = "$year-${number.toString().padStart(2, '0')}-15T10:30:00",
            amount = amount,
            missingCostItems = if (missing) 1 else 0,
            buyerName = if (number % 2 == 0) "Nguyễn Minh Anh" else "Trần Ngọc Hà",
            buyerPhone = "0900000000",
            sellerName = "Nông sản An Bình",
            sellerPhone = "0911111111",
            items = listOf(
                StatisticsOrderItemDto(
                    productId = "product-$number",
                    productName = if (number % 2 == 0) "Cà chua" else "Cam",
                    quantity = 2.0,
                    unit = "kg",
                    unitPrice = revenue / 2,
                    amount = amount
                )
            )
        )
    }

    val selected = if (month == null) {
        yearlyOrders
    } else {
        yearlyOrders.filter { it.orderId.toInt() == month }
    }

    val known = selected.filter { it.missingCostItems == 0 }

    val knownRevenue = known.sumOf { order ->
        order.items.sumOf { it.quantity * it.unitPrice }
    }

    return StatisticsSectionDto(
        totalAmount = selected.sumOf { it.amount },
        orderCount = selected.size,
        monthlyAmounts = yearlyOrders.map {
            StatisticsMonthDto(
                month = it.orderId.toInt(),
                amount = it.amount,
                orderCount = 1,
                missingCostItems = it.missingCostItems
            )
        },
        categories = selected.groupBy {
            if (it.orderId % 2L == 0L) "Rau củ" else "Trái cây"
        }.map { (category, orders) ->
            StatisticsCategoryDto(
                category = category,
                amount = orders.sumOf { it.amount },
                missingCostItems = orders.sumOf { it.missingCostItems }
            )
        },
        orders = selected.sortedByDescending { it.orderDate },
        missingCostItems = selected.sumOf { it.missingCostItems },
        knownCostRevenue = if (profit) knownRevenue else 0.0,
        totalCost = if (profit) {
            knownRevenue - known.sumOf { it.amount }
        } else {
            0.0
        }
    )
}

private fun categoryPercentUnits(
    categories: List<StatisticsCategoryDto>
): List<Int> {
    val target = 1000

    val amounts = categories.map {
        if (it.amount.isFinite() && it.amount > 0.0) {
            BigDecimal.valueOf(it.amount)
        } else {
            BigDecimal.ZERO
        }
    }

    val total = amounts.fold(BigDecimal.ZERO) { sum, amount ->
        sum.add(amount)
    }

    if (total.signum() == 0) {
        return List(categories.size) { 0 }
    }

    val shares = amounts.map { amount ->
        amount.multiply(BigDecimal.valueOf(target.toLong()))
            .divideAndRemainder(total)
    }

    val result = shares.map {
        it[0].toInt()
    }.toMutableList()

    val remaining = target - result.sum()

    val priority = shares.indices.sortedWith(
        compareByDescending<Int> { shares[it][1] }
            .thenBy { it }
    )

    repeat(remaining) { index ->
        val categoryIndex = priority[index]
        result[categoryIndex] = result[categoryIndex] + 1
    }

    return result
}

@Composable
private fun StatisticsInteractiveDemo(
    initialTab: StatisticsTab,
    initialMonth: Int? = null,
    missingShipping: Boolean = false
) {
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var year by rememberSaveable { mutableStateOf(2026) }
    var month by rememberSaveable { mutableStateOf(initialMonth) }

    val overview = remember(year, month, missingShipping) {
        statisticsSampleOverview(year, month, missingShipping)
    }

    AgritechTheme {
        StatisticsScreenContent(
            state = StatisticsUiState(
                selectedYear = year,
                selectedMonth = month,
                selectedTab = tab,
                overview = overview
            ),
            currentYear = 2026,
            onSelectTab = { tab = it },
            onSelectYear = {
                year = it
                month = null
            },
            onSelectMonth = { month = it }
        )
    }
}

// Feed previews through the same finance/tax branches as a current overview response.
// Keep fixtures independent of Android services, the network and the live ViewModel.
private fun statisticsSampleOverview(year: Int, month: Int?, missingShipping: Boolean): StatisticsOverviewDto {
    val yearlyRevenue = statisticsSampleSection(year, null)
    val yearlyProfit = statisticsSampleSection(year, null, profit = true)
    val profitById = yearlyProfit.orders.associateBy { it.orderId }
    val yearlyOrders = yearlyRevenue.orders.map { order ->
        val profit = requireNotNull(profitById[order.orderId])
        val gross = profit.amount
        val expense = if (missingShipping && order.orderId == 12L) null else 20_000L
        SellerOrderFinance(orderId = order.orderId, orderDate = order.orderDate, status = "COMPLETED",
            deliveryMethod = "SELF", merchandiseRevenue = order.amount,
            costOfGoods = order.amount - gross, grossProfit = gross,
            shippingCollected = 15_000L, actualShippingCost = expense,
            shippingMargin = expense?.let { 15_000L - it } ?: 0L,
            profitAfterShipping = gross + (expense?.let { 15_000L - it } ?: 0L),
            missingCostItems = profit.missingCostItems, buyerName = order.buyerName,
            buyerAvatar = order.buyerAvatar, buyerPhone = order.buyerPhone, items = order.items)
    }
    fun summarize(orders: List<SellerOrderFinance>): SellerFinanceSummary {
        val included = orders
        val gross = included.sumOf { requireNotNull(it.grossProfit) }
        val missing = included.count { it.actualShippingCost == null }
        val margin = included.sumOf { requireNotNull(it.shippingMargin) }
        return SellerFinanceSummary(merchandiseRevenue = orders.sumOf { it.merchandiseRevenue },
            costOfGoods = included.sumOf { requireNotNull(it.costOfGoods) }, grossProfit = gross,
            shippingCollected = orders.sumOf { it.shippingCollected },
            recordedShippingCost = orders.sumOf { it.actualShippingCost ?: 0L }, shippingMargin = margin,
            profitAfterShipping = gross + margin, cancelledShippingCost = 0L,
            missingCostItems = orders.sumOf { it.missingCostItems }, missingShippingCostOrders = missing,
            orders = orders)
    }
    val annualFinance = summarize(yearlyOrders)
    val selectedFinance = if (month == null) annualFinance else
        summarize(yearlyOrders.filter { it.orderId == month.toLong() })
    val supported = year >= 2026
    // This sample dataset stays below the annual threshold. The dedicated tax-card
    // previews cover the above-threshold breakdown separately.
    val taxRevenue = yearlyOrders.filter { (it.costOfGoods ?: 0.0) > 0.0 }.sumOf { it.merchandiseRevenue }
    return StatisticsOverviewDto(year = year, selectedMonth = month,
        revenue = statisticsSampleSection(year, month), profit = statisticsSampleSection(year, month, profit = true),
        spending = statisticsSampleSection(year, month, spending = true), sellerFinance = selectedFinance,
        sellerTax = SellerTaxEstimate(year = year, status = if (supported) "BELOW_THRESHOLD" else "OUTSIDE_POLICY_YEAR",
            annualRevenue = taxRevenue, excludedRevenue = annualFinance.merchandiseRevenue - taxRevenue,
            qualifyingItems = yearlyOrders.count { (it.costOfGoods ?: 0.0) > 0.0 }, threshold = 1_000_000_000.0,
            vatRate = 0.01, incomeTaxRate = 0.005, incomeTaxReductionRate = if (year in 2026..2027) 0.3 else 0.0,
            vat = if (supported) 0.0 else null, incomeTaxBase = if (supported) 0.0 else null,
            incomeTaxBeforeReduction = if (supported) 0.0 else null, incomeTaxReduction = if (supported) 0.0 else null,
            incomeTax = if (supported) 0.0 else null, totalTax = if (supported) 0.0 else null,
            annualProfitAfterEstimatedTax = annualFinance.profitAfterShipping.takeIf { supported }))
}

@Preview(
    name = "Thống kê - Doanh thu",
    showBackground = true,
    locale = "vi",
    widthDp = 411,
    heightDp = 1100
)
@Composable
private fun StatisticsRevenuePreview() {
    StatisticsInteractiveDemo(StatisticsTab.REVENUE)
}

@Preview(
    name = "Thống kê - Lợi nhuận",
    showBackground = true,
    locale = "vi",
    widthDp = 411,
    heightDp = 1100
)
@Composable
private fun StatisticsProfitPreview() {
    StatisticsInteractiveDemo(StatisticsTab.PROFIT)
}

@Preview(
    name = "Thống kê - Chi tiêu",
    showBackground = true,
    locale = "vi",
    widthDp = 411,
    heightDp = 1100
)
@Composable
private fun StatisticsSpendingPreview() {
    StatisticsInteractiveDemo(StatisticsTab.SPENDING)
}

@Preview(name = "Thống kê - Đơn cũ thiếu vốn", showBackground = true, locale = "vi", widthDp = 411, heightDp = 1100)
@Composable
private fun StatisticsMissingCostPreview() {
    StatisticsInteractiveDemo(StatisticsTab.PROFIT, initialMonth = 7)
}

@Preview(name = "Thống kê - Chưa có phí giao", showBackground = true, locale = "vi", widthDp = 411, heightDp = 1100)
@Composable
private fun StatisticsMissingShippingPreview() {
    StatisticsInteractiveDemo(StatisticsTab.PROFIT, initialMonth = 12, missingShipping = true)
}
