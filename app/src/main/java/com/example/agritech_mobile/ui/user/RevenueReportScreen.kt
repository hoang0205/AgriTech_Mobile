package com.example.agritech_mobile.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.agritech_mobile.data.remote.dto.CategoryRevenueDto
import com.example.agritech_mobile.data.remote.dto.RevenueSummaryDto
import com.example.agritech_mobile.ui.RevenueReportUiState
import com.example.agritech_mobile.ui.RevenueReportViewModel
import com.example.agritech_mobile.ui.theme.AgritechTheme
import java.text.NumberFormat
import java.util.Locale

private val RevenueGreen = Color(0xFF14532D)

private val RevenueColors = listOf(
    Color(0xFF14532D),
    Color(0xFF22C55E),
    Color(0xFF86EFAC),
    Color(0xFF0EA5E9),
    Color(0xFFF59E0B),
    Color(0xFFA855F7)
)

private fun formatRevenue(amount: Double): String {
    return NumberFormat
        .getNumberInstance(Locale("vi", "VN"))
        .format(amount) + " đ"
}

@Composable
fun RevenueReportScreen(
    onBackClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onExportReportClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onCategoryDetailClick: (String) -> Unit = {},
    viewModel: RevenueReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    RevenueReportContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onRetryClick = { viewModel.loadRevenue() }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenueReportContent(
    uiState: RevenueReportUiState,
    onBackClick: () -> Unit = {},
    onRetryClick: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Báo cáo doanh thu",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRetryClick,
                        enabled = !uiState.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Tải lại"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.White
                )
            )
        },
        containerColor = Color(0xFFFAFBFA)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = RevenueGreen
                    )
                }

                uiState.error != null -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = uiState.error,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(onClick = onRetryClick) {
                            Text("Thử lại")
                        }
                    }
                }

                else -> {
                    uiState.summary?.let { summary ->
                        RevenueSummaryContent(summary = summary)
                    }
                }
            }
        }
    }
}

@Composable
private fun RevenueSummaryContent(
    summary: RevenueSummaryDto
) {
    val categories = summary.categoryRevenues
        .filter { it.revenue > 0.0 }
        .sortedByDescending { it.revenue }

    val categoryTotal = categories.sumOf { it.revenue }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Toàn thời gian",
                color = Color(0xFF6B7280),
                fontWeight = FontWeight.Medium
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = RevenueGreen
                )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "TỔNG DOANH THU",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = formatRevenue(summary.totalRevenue),
                        color = Color.White,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Từ các đơn hàng đã hoàn thành",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        if (categories.isEmpty()) {
            item {
                Text(
                    text = "Chưa có doanh thu từ đơn hàng đã hoàn thành.",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    color = Color(0xFF6B7280),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Doanh thu theo danh mục",
                            modifier = Modifier.fillMaxWidth(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        RevenueDonutChart(
                            categories = categories,
                            modifier = Modifier.size(180.dp)
                        )
                    }
                }
            }

            items(categories.size) { index ->
                val category = categories[index]

                RevenueCategoryRow(
                    category = category,
                    percentage = category.revenue / categoryTotal * 100.0,
                    color = RevenueColors[index % RevenueColors.size]
                )
            }
        }
    }
}

@Composable
private fun RevenueDonutChart(
    categories: List<CategoryRevenueDto>,
    modifier: Modifier = Modifier
) {
    val total = categories.sumOf { it.revenue }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (total > 0.0) {
                val ringWidth = 24.dp.toPx()
                val inset = ringWidth / 2f
                var startAngle = -90f

                categories.forEachIndexed { index, category ->
                    val sweepAngle =
                        (category.revenue / total * 360.0).toFloat()

                    drawArc(
                        color = RevenueColors[index % RevenueColors.size],
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(
                            width = size.width - ringWidth,
                            height = size.height - ringWidth
                        ),
                        style = Stroke(width = ringWidth)
                    )

                    startAngle += sweepAngle
                }
            }
        }

        Text(
            text = "Danh mục",
            fontWeight = FontWeight.Bold,
            color = RevenueGreen
        )
    }
}

@Composable
private fun RevenueCategoryRow(
    category: CategoryRevenueDto,
    percentage: Double,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, RoundedCornerShape(6.dp))
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = category.category,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = String.format(
                    Locale("vi", "VN"),
                    "Tỷ trọng %.1f%%",
                    percentage
                ),
                color = Color(0xFF6B7280),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        Text(
            text = formatRevenue(category.revenue),
            fontWeight = FontWeight.Bold,
            color = RevenueGreen
        )
    }
}

// Dữ liệu mẫu chỉ dùng cho Preview.
@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun RevenueReportSuccessPreview() {
    AgritechTheme {
        RevenueReportContent(
            uiState = RevenueReportUiState(
                isLoading = false,
                summary = RevenueSummaryDto(
                    totalRevenue = 10_000_000.0,
                    categoryRevenues = listOf(
                        CategoryRevenueDto("Trái cây", 5_000_000.0),
                        CategoryRevenueDto("Rau củ", 3_000_000.0),
                        CategoryRevenueDto("Nấm", 2_000_000.0)
                    )
                )
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RevenueReportLoadingPreview() {
    AgritechTheme {
        RevenueReportContent(
            uiState = RevenueReportUiState(isLoading = true)
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RevenueReportErrorPreview() {
    AgritechTheme {
        RevenueReportContent(
            uiState = RevenueReportUiState(
                isLoading = false,
                error = "Không thể kết nối đến server"
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun RevenueReportEmptyPreview() {
    AgritechTheme {
        RevenueReportContent(
            uiState = RevenueReportUiState(
                isLoading = false,
                summary = RevenueSummaryDto(
                    totalRevenue = 0.0,
                    categoryRevenues = emptyList()
                )
            )
        )
    }
}