package com.example.agritech_mobile.ui.report

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.agritech_mobile.data.remote.dto.ProductReportResponse
import com.example.agritech_mobile.data.remote.dto.ReportReason
import com.example.agritech_mobile.ui.theme.AgritechTheme

@Composable
fun ReportHistoryScreen(
    onBackClick: () -> Unit,
    viewModel: ReportHistoryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var expandedReportIds by rememberSaveable {
        mutableStateOf(arrayListOf<Long>())
    }

    ReportHistoryContent(
        state = state,
        expandedReportIds = expandedReportIds.toSet(),
        onBackClick = onBackClick,
        onRefresh = viewModel::refresh,
        onPreviousPage = viewModel::previousPage,
        onNextPage = viewModel::nextPage,
        onToggleReport = { reportId ->
            expandedReportIds = ArrayList(
                if (reportId in expandedReportIds) {
                    expandedReportIds.filterNot { it == reportId }
                } else {
                    expandedReportIds + reportId
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportHistoryContent(
    state: ReportHistoryUiState,
    expandedReportIds: Set<Long> = emptySet(),
    onBackClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onToggleReport: (Long) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Báo cáo của tôi") },
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
                        onClick = onRefresh,
                        enabled = !state.isLoading
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Tải lại báo cáo"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (state.totalPages > 1) {
                Surface(tonalElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = onPreviousPage,
                            enabled = !state.isLoading && state.page > 0
                        ) {
                            Text("Trang trước")
                        }

                        Text("${state.page + 1}/${state.totalPages}")

                        TextButton(
                            onClick = onNextPage,
                            enabled = !state.isLoading &&
                                    state.page + 1 < state.totalPages
                        ) {
                            Text("Trang sau")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            state.error?.let { message ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )

                        TextButton(
                            onClick = onRefresh,
                            enabled = !state.isLoading
                        ) {
                            Text("Thử lại")
                        }
                    }
                }
            }

            if (state.reports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when {
                            state.isLoading -> "Đang tải báo cáo..."
                            state.error != null -> "Chưa tải được danh sách"
                            else -> "Bạn chưa gửi báo cáo nào"
                        },
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.reports,
                        key = { it.id }
                    ) { report ->
                        ReportHistoryCard(
                            report = report,
                            expanded = report.id in expandedReportIds,
                            onToggleExpanded = {
                                onToggleReport(report.id)
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportHistoryCard(
    report: ProductReportResponse,
    expanded: Boolean,
    onToggleExpanded: () -> Unit
) {

    val statusText = when (report.status) {
        "PENDING" -> "Chờ xử lý"
        "REVIEWING" -> "Đang xử lý"
        "RESOLVED" -> "Đã giải quyết"
        "REJECTED" -> "Đã từ chối"
        else -> "Chưa xác định"
    }

    val reasonText = when (report.reason) {
        ReportReason.SCAM -> "Nghi ngờ lừa đảo"
        ReportReason.MISLEADING_INFORMATION ->
            "Thông tin sản phẩm sai lệch"
        ReportReason.INAPPROPRIATE_CONTENT ->
            "Nội dung hoặc hình ảnh không phù hợp"
        ReportReason.OTHER -> "Lý do khác"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = report.productName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Báo cáo #${report.id}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                color = if (report.status == "REJECTED") {
                    MaterialTheme.colorScheme.errorContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer
                },
                shape = MaterialTheme.shapes.small
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(
                        horizontal = 12.dp,
                        vertical = 6.dp
                    ),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Text(
                text = reasonText,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Ngày gửi: ${
                    report.createdAt.take(19).replace('T', ' ')
                }",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (expanded) {
                HorizontalDivider()

                Text(
                    text = "Nội dung báo cáo",
                    fontWeight = FontWeight.SemiBold
                )

                Text(report.description)

                if (report.imageUrls.isNotEmpty()) {
                    Text(
                        text = "Ảnh bằng chứng",
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        report.imageUrls.forEachIndexed { index, url ->
                            Card(modifier = Modifier.size(120.dp)) {
                                AsyncImage(
                                    model = url,
                                    contentDescription =
                                        "Ảnh bằng chứng ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Phản hồi quản trị viên",
                    fontWeight = FontWeight.SemiBold
                )

                Text(
                    text = report.adminReply
                        ?.takeIf { it.isNotBlank() }
                        ?: "Chưa có phản hồi"
                )
            }

            TextButton(
                onClick = onToggleExpanded
            ) {
                Text(
                    if (expanded) "Thu gọn" else "Xem chi tiết"
                )
            }
        }
    }
}

private val previewReports = listOf(
    ProductReportResponse(
        id = 101L,
        productId = "product-1",
        productName = "Gạo ST25 hữu cơ",
        reason = ReportReason.MISLEADING_INFORMATION,
        description = "Thông tin nguồn gốc trên bao bì " +
                "không giống nội dung người bán đăng.",
        imageUrls = emptyList(),
        status = "RESOLVED",
        adminReply = "Đã kiểm tra và yêu cầu người bán " +
                "cập nhật thông tin nguồn gốc sản phẩm.",
        createdAt = "2026-10-08T09:30:00"
    ),
    ProductReportResponse(
        id = 102L,
        productId = "product-2",
        productName = "Rau cải hữu cơ",
        reason = ReportReason.SCAM,
        description = "Người bán yêu cầu chuyển tiền " +
                "ngoài ứng dụng rồi không phản hồi.",
        imageUrls = emptyList(),
        status = "PENDING",
        adminReply = null,
        createdAt = "2026-10-07T15:20:00"
    )
)

@Preview(
    name = "Có báo cáo",
    showBackground = true,
    widthDp = 411,
    heightDp = 900
)
@Composable
private fun ReportHistorySuccessPreview() {
    AgritechTheme {
        ReportHistoryContent(
            state = ReportHistoryUiState(
                reports = previewReports,
                page = 0,
                totalPages = 3
            )
        )
    }
}

@Preview(
    name = "Chi tiết và phản hồi admin",
    showBackground = true,
    widthDp = 411,
    heightDp = 1000
)
@Composable
private fun ReportHistoryExpandedPreview() {
    AgritechTheme {
        ReportHistoryContent(
            state = ReportHistoryUiState(
                reports = previewReports,
                totalPages = 1
            ),
            expandedReportIds = setOf(101L)
        )
    }
}

@Preview(
    name = "Chưa có báo cáo",
    showBackground = true,
    widthDp = 411,
    heightDp = 800
)
@Composable
private fun ReportHistoryEmptyPreview() {
    AgritechTheme {
        ReportHistoryContent(
            state = ReportHistoryUiState()
        )
    }
}

@Preview(
    name = "Đang tải",
    showBackground = true,
    widthDp = 411,
    heightDp = 800
)
@Composable
private fun ReportHistoryLoadingPreview() {
    AgritechTheme {
        ReportHistoryContent(
            state = ReportHistoryUiState(
                isLoading = true
            )
        )
    }
}

@Preview(
    name = "Lỗi kết nối",
    showBackground = true,
    widthDp = 411,
    heightDp = 800
)
@Composable
private fun ReportHistoryErrorPreview() {
    AgritechTheme {
        ReportHistoryContent(
            state = ReportHistoryUiState(
                error = "Không thể kết nối máy chủ. " +
                        "Vui lòng kiểm tra mạng."
            )
        )
    }
}