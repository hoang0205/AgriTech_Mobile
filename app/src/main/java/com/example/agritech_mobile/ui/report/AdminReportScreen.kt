package com.example.agritech_mobile.ui.report

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.agritech_mobile.data.remote.dto.ProductReportResponse
import com.example.agritech_mobile.data.remote.dto.ReportReason
import com.example.agritech_mobile.data.remote.dto.ReportStatus
import com.example.agritech_mobile.ui.theme.AgritechTheme

@Composable
fun AdminReportScreen(
    onBackClick: () -> Unit,
    viewModel: AdminReportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = state.isSubmitting) {}

    AdminReportContent(
        state = state,
        onBackClick = onBackClick,
        onRefresh = viewModel::refresh,
        onFilterChange = viewModel::selectFilter,
        onPreviousPage = viewModel::previousPage,
        onNextPage = viewModel::nextPage,
        onSelectReport = viewModel::selectReport,
        onDismissReport = viewModel::dismissReport,
        onReplyChange = viewModel::onReplyChange,
        onUpdateStatus = viewModel::updateStatus
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminReportContent(
    state: AdminReportUiState,
    onBackClick: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onFilterChange: (ReportStatus?) -> Unit = {},
    onPreviousPage: () -> Unit = {},
    onNextPage: () -> Unit = {},
    onSelectReport: (Long) -> Unit = {},
    onDismissReport: () -> Unit = {},
    onReplyChange: (String) -> Unit = {},
    onUpdateStatus: (ReportStatus) -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý báo cáo") },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        enabled = !state.isSubmitting
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onRefresh,
                        enabled = !state.isBusy
                    ) {
                        Icon(
                            Icons.Default.Refresh,
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
                            enabled = !state.isBusy && state.page > 0
                        ) {
                            Text("Trang trước")
                        }

                        Text("${state.page + 1}/${state.totalPages}")

                        TextButton(
                            onClick = onNextPage,
                            enabled = !state.isBusy &&
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
            val filters = listOf<ReportStatus?>(null) + ReportStatus.entries

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
                    .background(Color(0xFFF1F8F4))
                    .padding(vertical = 12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = filters,
                    key = { it?.name ?: "ALL" }
                ) { status ->
                    val isSelected = state.filter == status

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                if (isSelected) Color(0xFF1B5E20)
                                else Color(0xFFE0E0E0)
                            )
                            .selectable(
                                selected = isSelected,
                                enabled = !state.isBusy,
                                role = Role.Tab,
                                onClick = { onFilterChange(status) }
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = status?.let {
                                reportStatusLabel(it.name)
                            } ?: "Tất cả",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isSelected) {
                                Color.White
                            } else {
                                Color(0xFF616161)
                            }
                        )
                    }
                }
            }

            if (state.isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            if (state.selectedReport == null) {
                state.error?.let { message ->
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            message,
                            color = MaterialTheme.colorScheme.error
                        )
                        TextButton(
                            onClick = onRefresh,
                            enabled = !state.isBusy
                        ) {
                            Text("Thử lại")
                        }
                    }
                }

                state.successMessage?.let { message ->
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
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
                        when {
                            state.isLoading -> "Đang tải báo cáo..."
                            state.error != null ->
                                "Chưa tải được danh sách"
                            else -> "Không có báo cáo trong mục này"
                        }
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.reports, key = { it.id }) { report ->
                        Card(
                            onClick = { onSelectReport(report.id) },
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    report.productName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Báo cáo #${report.id} · " +
                                            reportStatusLabel(report.status)
                                )
                                Text(
                                    reportReasonLabel(report.reason),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    report.createdAt.take(19).replace('T', ' '),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    "Xem và xử lý",
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.selectedReport != null) {
        AdminReportDetailDialog(
            state = state,
            onDismiss = onDismissReport,
            onReplyChange = onReplyChange,
            onUpdateStatus = onUpdateStatus
        )
    }
}

internal fun reportStatusLabel(status: String): String = when (status) {
    "PENDING" -> "Chờ xử lý"
    "REVIEWING" -> "Đang xử lý"
    "RESOLVED" -> "Đã giải quyết"
    "REJECTED" -> "Đã từ chối"
    else -> "Chưa xác định"
}

internal fun reportReasonLabel(reason: ReportReason): String = when (reason) {
    ReportReason.SCAM -> "Nghi ngờ lừa đảo"
    ReportReason.MISLEADING_INFORMATION -> "Thông tin sản phẩm sai lệch"
    ReportReason.INAPPROPRIATE_CONTENT -> "Nội dung không phù hợp"
    ReportReason.OTHER -> "Lý do khác"
}

internal val adminPreviewReport = ProductReportResponse(
    id = 101L,
    productId = "product-1",
    productName = "Gạo ST25 hữu cơ",
    reason = ReportReason.MISLEADING_INFORMATION,
    description = "Thông tin nguồn gốc trên bao bì " +
            "không giống nội dung người bán đăng.",
    imageUrls = emptyList(),
    status = "PENDING",
    adminReply = null,
    createdAt = "2026-10-08T09:30:00"
)

@Composable
fun AdminReportDetailDialog(
    state: AdminReportUiState,
    onDismiss: () -> Unit = {},
    onReplyChange: (String) -> Unit = {},
    onUpdateStatus: (ReportStatus) -> Unit = {}
) {
    val report = state.selectedReport ?: return

    val canProcess =
        report.status == ReportStatus.PENDING.name ||
                report.status == ReportStatus.REVIEWING.name

    AlertDialog(
        onDismissRequest = {
            if (!state.isSubmitting) {
                onDismiss()
            }
        },
        title = {
            Text("Báo cáo #${report.id}")
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    report.productName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Text("Mã sản phẩm: ${report.productId}")
                Text("Trạng thái: ${reportStatusLabel(report.status)}")
                Text("Lý do: ${reportReasonLabel(report.reason)}")
                Text(
                    "Ngày gửi: ${
                        report.createdAt.take(19).replace('T', ' ')
                    }"
                )

                HorizontalDivider()

                Text(
                    "Nội dung báo cáo",
                    fontWeight = FontWeight.SemiBold
                )
                Text(report.description)

                if (report.imageUrls.isNotEmpty()) {
                    Text(
                        "Ảnh bằng chứng",
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        report.imageUrls.forEachIndexed { index, url ->
                            Card(modifier = Modifier.size(140.dp)) {
                                AsyncImage(
                                    model = url,
                                    contentDescription =
                                        "Ảnh bằng chứng ${index + 1}",
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                HorizontalDivider()

                if (canProcess) {
                    OutlinedTextField(
                        value = state.adminReply,
                        onValueChange = onReplyChange,
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Phản hồi cho người gửi") },
                        minLines = 3,
                        maxLines = 6,
                        supportingText = {
                            Text("${state.adminReply.length}/2000")
                        }
                    )

                    Text(
                        "Phải nhập phản hồi khi giải quyết hoặc từ chối.",
                        style = MaterialTheme.typography.bodySmall
                    )

                    if (report.status == ReportStatus.PENDING.name) {
                        OutlinedButton(
                            onClick = {
                                onUpdateStatus(ReportStatus.REVIEWING)
                            },
                            enabled = !state.isBusy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Nhận xử lý")
                        }
                    }

                    Button(
                        onClick = {
                            onUpdateStatus(ReportStatus.RESOLVED)
                        },
                        enabled = !state.isBusy &&
                                state.adminReply.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Đánh dấu đã giải quyết")
                    }

                    OutlinedButton(
                        onClick = {
                            onUpdateStatus(ReportStatus.REJECTED)
                        },
                        enabled = !state.isBusy &&
                                state.adminReply.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Từ chối báo cáo")
                    }
                } else {
                    Text(
                        "Phản hồi quản trị viên",
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        report.adminReply
                            ?.takeIf { it.isNotBlank() }
                            ?: "Chưa có phản hồi"
                    )
                    Text(
                        "Báo cáo này không thể cập nhật.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                if (state.isSubmitting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Đang cập nhật báo cáo...")
                }

                state.error?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                state.successMessage?.let {
                    Text(
                        it,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !state.isSubmitting
            ) {
                Text("Đóng")
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 411, heightDp = 850)
@Composable
private fun AdminReportSuccessPreview() {
    AgritechTheme {
        AdminReportContent(
            state = AdminReportUiState(
                reports = listOf(adminPreviewReport),
                totalPages = 3
            )
        )
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 850)
@Composable
private fun AdminReportEmptyPreview() {
    AgritechTheme {
        AdminReportContent(state = AdminReportUiState())
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 850)
@Composable
private fun AdminReportLoadingPreview() {
    AgritechTheme {
        AdminReportContent(
            state = AdminReportUiState(isLoading = true)
        )
    }
}

@Preview(showBackground = true, widthDp = 411, heightDp = 850)
@Composable
private fun AdminReportErrorPreview() {
    AgritechTheme {
        AdminReportContent(
            state = AdminReportUiState(
                error = "Không thể kết nối máy chủ."
            )
        )
    }
}