package com.example.agritech_mobile.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.Eco
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.agritech_mobile.ui.theme.AgritechTheme

data class RevenueCategoryItem(
    val id: String,
    val name: String,
    val percentage: Int,
    val amountStr: String,
    val growthStr: String,
    val color: Color
)

data class RevenueReportUiState(
    val selectedPeriodIndex: Int = 2,
    val dateRangeText: String = "01/10/2024 - 31/10/2024",
    val reconciliationStatus: String = "ĐÃ ĐỐI SOÁT",
    val totalRevenue: String = "128.450.000 đ",
    val growthRate: String = "+18.4%",
    val successfulOrdersCount: Int = 486,
    val averageOrderValue: String = "264.300 đ",
    val chartTotalText: String = "128.45M",
    val categories: List<RevenueCategoryItem> = emptyList(),
    val isLoading: Boolean = false
)



@Composable
fun RevenueReportScreen(
    onBackClick: () -> Unit = {},
    onCalendarClick: () -> Unit = {},
    onExportReportClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onCategoryDetailClick: (String) -> Unit = {}
) {
    val sampleCategories = remember {
        listOf(
            RevenueCategoryItem(
                id = "cat_1",
                name = "Trái cây cao cấp",
                percentage = 45,
                amountStr = "57.800.000 đ",
                growthStr = "+22.5%",
                color = Color(0xFF14532D)
            ),
            RevenueCategoryItem(
                id = "cat_2",
                name = "Rau củ VietGAP",
                percentage = 28,
                amountStr = "35.960.000 đ",
                growthStr = "+14.2%",
                color = Color(0xFF22C55E)
            ),
            RevenueCategoryItem(
                id = "cat_3",
                name = "Nấm & Dược liệu",
                percentage = 17,
                amountStr = "21.840.000 đ",
                growthStr = "+18.7%",
                color = Color(0xFF86EFAC)
            ),
            RevenueCategoryItem(
                id = "cat_4",
                name = "Nông sản chế biến",
                percentage = 10,
                amountStr = "12.850.000 đ",
                growthStr = "+8.1%",
                color = Color(0xFFCBD5E1)
            )
        )
    }

    var uiState by remember {
        mutableStateOf(
            RevenueReportUiState(
                selectedPeriodIndex = 2,
                dateRangeText = "01/10/2024 - 31/10/2024",
                reconciliationStatus = "ĐÃ ĐỐI SOÁT",
                totalRevenue = "128.450.000 đ",
                growthRate = "+18.4%",
                successfulOrdersCount = 486,
                averageOrderValue = "264.300 đ",
                chartTotalText = "128.45M",
                categories = sampleCategories
            )
        )
    }

    RevenueReportContent(
        uiState = uiState,
        onPeriodSelected = { index ->
            uiState = uiState.copy(selectedPeriodIndex = index)
        },
        onCalendarClick = onCalendarClick,
        onExportReportClick = onExportReportClick,
        onProfileClick = onProfileClick,
        onCategoryDetailClick = onCategoryDetailClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RevenueReportContent(
    uiState: RevenueReportUiState,
    onPeriodSelected: (Int) -> Unit,
    onCalendarClick: () -> Unit,
    onExportReportClick: () -> Unit,
    onProfileClick: () -> Unit,
    onCategoryDetailClick: (String) -> Unit
) {
    val periods = listOf("Hôm nay", "Tuần này", "Tháng này", "Năm nay")
    val primaryGreen = Color(0xFF14532D)
    val lightBg = Color(0xFFFAFBFA)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDCFCE7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Eco,
                                contentDescription = null,
                                tint = primaryGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Báo Cáo Doanh Thu",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = Color(0xFF1F2937)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCalendarClick) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Chọn ngày",
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onExportReportClick) {
                        Icon(
                            imageVector = Icons.Default.FileDownload,
                            contentDescription = "Xuất báo cáo",
                            tint = Color(0xFF4B5563),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(primaryGreen)
                            .clickable { onProfileClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Tài khoản",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = lightBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFFF3F4F6))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    periods.forEachIndexed { index, title ->
                        val isSelected = uiState.selectedPeriodIndex == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) primaryGreen else Color.Transparent)
                                .clickable { onPeriodSelected(index) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF4B5563)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = Color(0xFF6B7280),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = uiState.dateRangeText,
                            fontSize = 12.sp,
                            color = Color(0xFF4B5563),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = uiState.reconciliationStatus,
                            color = Color(0xFF15803D),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = primaryGreen),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "TỔNG DOANH THU THUẦN",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.85f),
                                letterSpacing = 0.5.sp
                            )
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = uiState.growthRate,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = uiState.totalRevenue,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Đơn thành công",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "${uiState.successfulOrdersCount} đơn",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Giá trị trung bình",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.averageOrderValue,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(18.dp))
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "BÁO CÁO DOANH THU",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1F2937)
                                )
                                Text(
                                    text = "Tỷ trọng theo nhóm nông sản",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6B7280)
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF3F4F6))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "${uiState.categories.size} danh mục",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF4B5563)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            DonutChart(
                                categories = uiState.categories,
                                modifier = Modifier.size(190.dp)
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "TỔNG CỘNG",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B7280),
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = uiState.chartTotalText,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF111827)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "100% vụ mùa",
                                    fontSize = 10.sp,
                                    color = Color(0xFF15803D),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            uiState.categories.forEach { item ->
                                CategoryRevenueRowItem(
                                    item = item,
                                    onClick = { onCategoryDetailClick(item.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DonutChart(
    categories: List<RevenueCategoryItem>,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 62f
) {
    Canvas(modifier = modifier) {
        var startAngle = -90f
        categories.forEach { category ->
            val sweepAngle = (category.percentage / 100f) * 360f
            drawArc(
                color = category.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            startAngle += sweepAngle
        }
    }
}

@Composable
fun CategoryRevenueRowItem(
    item: RevenueCategoryItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF9FAFB))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(item.color)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = item.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1F2937)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Tỷ trọng ${item.percentage}%",
                    fontSize = 11.sp,
                    color = Color(0xFF6B7280)
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = item.amountStr,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.growthStr,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF15803D)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RevenueReportScreenPreview() {
    AgritechTheme {
        RevenueReportScreen()
    }
}