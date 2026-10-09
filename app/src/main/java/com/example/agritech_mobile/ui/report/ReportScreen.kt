package com.example.agritech_mobile.ui.report

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.agritech_mobile.data.remote.dto.ReportReason
import com.example.agritech_mobile.ui.theme.AgritechTheme

@Composable
fun ReportScreen(
    productId: String,
    productName: String,
    onBackClick: () -> Unit,
    onReportSuccess: () -> Unit,
    viewModel: ReportProductViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var reasonName by rememberSaveable(productId) {
        mutableStateOf<String?>(null)
    }
    var description by rememberSaveable(productId) {
        mutableStateOf("")
    }
    var imagePaths by rememberSaveable(productId) {
        mutableStateOf(arrayListOf<String>())
    }

    val selectedReason = ReportReason.entries.firstOrNull {
        it.name == reasonName
    }
    val selectedImages = imagePaths.map { Uri.parse(it) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(3)
    ) { uris ->
        if (!uiState.isSubmitting && uris.isNotEmpty()) {
            val combined = (
                    imagePaths + uris.map { it.toString() }
                    ).distinct()

            if (combined.size > 3) {
                Toast.makeText(
                    context,
                    "Chỉ được đính kèm tối đa 3 ảnh",
                    Toast.LENGTH_SHORT
                ).show()
            }

            val retainedPaths = combined.take(3)

            retainedPaths.forEach { path ->
                try {
                    context.contentResolver.takePersistableUriPermission(
                        Uri.parse(path),
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: SecurityException) {
                }
            }

            imagePaths = ArrayList(retainedPaths)
        }
    }

    BackHandler(enabled = uiState.isSubmitting) {
    }

    LaunchedEffect(uiState.submittedReport?.id) {
        if (uiState.submittedReport != null) {
            Toast.makeText(
                context,
                "Đã gửi báo cáo đến quản trị viên",
                Toast.LENGTH_LONG
            ).show()

            onReportSuccess()
        }
    }

    ReportProductContent(
        productName = productName,
        selectedReason = selectedReason,
        description = description,
        selectedImages = selectedImages,
        isSubmitting = uiState.isSubmitting,
        error = uiState.error,
        onBackClick = onBackClick,
        onReasonChange = { reason ->
            reasonName = reason.name
        },
        onDescriptionChange = { text ->
            description = text.take(2000)
        },
        onAddImagesClick = {
            imagePicker.launch(
                PickVisualMediaRequest(
                    ActivityResultContracts.PickVisualMedia.ImageOnly
                )
            )
        },
        onRemoveImage = { uri ->
            imagePaths = ArrayList(
                imagePaths.filterNot { it == uri.toString() }
            )
        },
        onSubmitClick = {
            viewModel.submit(
                productId = productId,
                reason = selectedReason,
                description = description,
                imageUris = selectedImages
            )
        }
    )
}

@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)
@Composable
fun ReportProductContent(
    productName: String,
    selectedReason: ReportReason?,
    description: String,
    selectedImages: List<Uri>,
    isSubmitting: Boolean = false,
    error: String? = null,
    onBackClick: () -> Unit = {},
    onReasonChange: (ReportReason) -> Unit = {},
    onDescriptionChange: (String) -> Unit = {},
    onAddImagesClick: () -> Unit = {},
    onRemoveImage: (Uri) -> Unit = {},
    onSubmitClick: () -> Unit = {}
) {
    val isKeyboardVisible = WindowInsets.isImeVisible
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Báo cáo sản phẩm",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBackClick,
                        enabled = !isSubmitting
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại"
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (!isKeyboardVisible) {
                Surface(
                    modifier = Modifier.navigationBarsPadding(),
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp
                ) {
                    Button(
                        onClick = onSubmitClick,
                        enabled = !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .heightIn(min = 48.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Flag,
                                contentDescription = null
                            )
                        }

                        Spacer(Modifier.width(8.dp))

                        Text(
                            if (isSubmitting) "Đang gửi báo cáo..."
                            else "Gửi báo cáo"
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .consumeWindowInsets(paddingValues)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = productName.ifBlank { "Sản phẩm" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        text = "Hãy mô tả vấn đề và cung cấp bằng chứng " +
                                "để quản trị viên xem xét.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            Text(
                text = "Lý do báo cáo",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup()
            ) {
                ReportReason.entries.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedReason == reason,
                                enabled = !isSubmitting,
                                role = Role.RadioButton,
                                onClick = { onReasonChange(reason) }
                            )
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = null,
                            enabled = !isSubmitting
                        )

                        Spacer(Modifier.width(12.dp))

                        Text(
                            text = reason.displayName(),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }

            OutlinedTextField(
                value = description,
                onValueChange = onDescriptionChange,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Mô tả chi tiết") },
                placeholder = {
                    Text("Mô tả cụ thể vấn đề bạn gặp phải...")
                },
                minLines = 4,
                maxLines = 8,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Tối thiểu 10 ký tự")
                        Text("${description.length}/2000")
                    }
                }
            )

            Text(
                text = "Ảnh bằng chứng",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Không bắt buộc. Tối đa 3 ảnh, mỗi ảnh không quá 10 MB.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (selectedImages.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    selectedImages.forEachIndexed { index, uri ->
                        Card(
                            modifier = Modifier.size(108.dp)
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Ảnh bằng chứng ${index + 1}",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                FilledIconButton(
                                    onClick = { onRemoveImage(uri) },
                                    enabled = !isSubmitting,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Xóa ảnh ${index + 1}",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onAddImagesClick,
                enabled = !isSubmitting && selectedImages.size < 3
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoLibrary,
                    contentDescription = null
                )

                Spacer(Modifier.width(8.dp))

                Text("Thêm ảnh (${selectedImages.size}/3)")
            }

            error?.let { message ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        text = message,
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

private fun ReportReason.displayName(): String = when (this) {
    ReportReason.SCAM ->
        "Nghi ngờ lừa đảo"

    ReportReason.MISLEADING_INFORMATION ->
        "Thông tin sản phẩm sai lệch"

    ReportReason.INAPPROPRIATE_CONTENT ->
        "Nội dung hoặc hình ảnh không phù hợp"

    ReportReason.OTHER ->
        "Lý do khác"
}

@Preview(
    name = "Báo cáo sản phẩm",
    showBackground = true,
    widthDp = 411,
    heightDp = 1000
)
@Composable
fun ReportProductContentPreview() {
    AgritechTheme {
        ReportProductContent(
            productName = "Gạo ST25 hữu cơ",
            selectedReason = ReportReason.MISLEADING_INFORMATION,
            description = "Thông tin nguồn gốc trên bao bì " +
                    "không giống nội dung người bán đăng.",
            selectedImages = emptyList()
        )
    }
}

@Preview(
    name = "Báo cáo có lỗi",
    showBackground = true,
    widthDp = 411,
    heightDp = 1000
)
@Composable
fun ReportProductErrorPreview() {
    AgritechTheme {
        ReportProductContent(
            productName = "Gạo ST25 hữu cơ",
            selectedReason = ReportReason.SCAM,
            description = "Người bán yêu cầu chuyển tiền " +
                    "ngoài ứng dụng rồi không phản hồi.",
            selectedImages = emptyList(),
            error = "Bạn đã có báo cáo đang chờ xử lý cho sản phẩm này"
        )
    }
}