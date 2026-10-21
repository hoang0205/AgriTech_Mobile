package com.example.agritech_mobile.ui.order

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.example.agritech_mobile.R
import com.example.agritech_mobile.data.remote.dto.ShippingDetails
import com.example.agritech_mobile.ui.theme.AgritechTheme
import com.example.agritech_mobile.ui.user.AddressState
import com.example.agritech_mobile.ui.user.UserViewModel
import com.example.agritech_mobile.ui.user.UserState

@Composable
fun AddressSelectionScreen(
    currentSelectedAddressId: String? = null,
    onBackClick: () -> Unit,
    onAddNewClick: () -> Unit,
    onConfirmClick: (ShippingDetails) -> Unit,
    onEditClick: (ShippingDetails) -> Unit = {},
    viewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val addressState by viewModel.addressState.collectAsState()
    val deletionState by viewModel.addressDeletionState.collectAsState()
    var pendingDelete by remember { mutableStateOf<ShippingDetails?>(null) }
    val isDeleting = deletionState is UserState.Loading

    var addresses by remember { mutableStateOf<List<ShippingDetails>>(emptyList()) }
    var selectedAddressId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.getUserAddresses()
    }

    LaunchedEffect(deletionState) {
        val message = when (val state = deletionState) {
            is UserState.Success -> state.message
            is UserState.Error -> state.message
            else -> null
        }
        if (message != null) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearAddressDeletionState()
        }
    }

    LaunchedEffect(currentSelectedAddressId) {
        if (!currentSelectedAddressId.isNullOrEmpty()) {
            selectedAddressId = currentSelectedAddressId
        }
    }


    LaunchedEffect(addressState) {
        when (val state = addressState) {
            is AddressState.Loading -> {
                isLoading = true
            }

            is AddressState.Success -> {
                isLoading = false
                addresses = state.addresses
                if (addresses.none { it.id == selectedAddressId }) {
                    selectedAddressId = addresses.firstOrNull { it.isDefault }?.id
                        ?: addresses.firstOrNull()?.id.orEmpty()
                }
            }

            is AddressState.Error -> {
                isLoading = false
                addresses = emptyList()
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }

            else -> isLoading = false
        }
    }

    pendingDelete?.let { address ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Xóa địa chỉ?") },
            text = { Text("Bạn có muốn xóa địa chỉ ${address.addressDetail}? Địa chỉ trên các đơn đã đặt vẫn được giữ nguyên.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    viewModel.deleteAddress(address.id)
                }) { Text("Xóa", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Hủy") }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AddressSelectionContent(
            addresses = addresses,
            selectedAddressId = selectedAddressId,
            onAddressSelect = { selectedAddressId = it },
            onBackClick = onBackClick,
            onAddNewClick = onAddNewClick,
            onEditClick = onEditClick,
            onDeleteClick = { pendingDelete = it },
            isBusy = isLoading || isDeleting,
            errorMessage = (addressState as? AddressState.Error)?.message,
            onRetry = { viewModel.getUserAddresses() },
            onConfirmClick = {
                val selected = addresses.find { it.id == selectedAddressId }
                if (selected != null) {
                    onConfirmClick(selected)
                } else {
                    Toast.makeText(context, "Vui lòng chọn 1 địa chỉ", Toast.LENGTH_SHORT).show()
                }
            }
        )

        if (isLoading || isDeleting) {
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSelectionContent(
    addresses: List<ShippingDetails>,
    selectedAddressId: String,
    onAddressSelect: (String) -> Unit,
    onBackClick: () -> Unit,
    onAddNewClick: () -> Unit,
    onConfirmClick: () -> Unit,
    onEditClick: (ShippingDetails) -> Unit = {},
    onDeleteClick: (ShippingDetails) -> Unit = {},
    isBusy: Boolean = false,
    errorMessage: String? = null,
    onRetry: () -> Unit = {}
) {
    Scaffold(
        containerColor = Color(0xFFF8FAF9),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.address_selection_title),
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B5E20)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF1B5E20)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            Button(
                onClick = onConfirmClick,
                enabled = !isBusy && addresses.any { it.id == selectedAddressId },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    stringResource(R.string.address_selection_confirm),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF1F8F4))
                        .clickable(enabled = !isBusy) { onAddNewClick() }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF1B5E20)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        stringResource(R.string.address_selection_add_new),
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    stringResource(R.string.address_selection_saved_addresses),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                if (errorMessage != null) {
                    Text(errorMessage, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = onRetry, enabled = !isBusy) { Text("Tải lại") }
                } else if (addresses.isEmpty() && !isBusy) {
                    Text("Chưa có địa chỉ. Hãy thêm địa chỉ nhận hàng.", color = Color.Gray)
                }
            }

            items(addresses) { address ->
                val isSelected = address.id == selectedAddressId
                val borderColor = if (isSelected) Color(0xFF1B5E20) else Color.Transparent
                val bgColor = Color.White

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .border(BorderStroke(2.dp, borderColor), RoundedCornerShape(12.dp))
                        .clickable(enabled = !isBusy) { onAddressSelect(address.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = address.receiverName ?: "",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp
                                )
                                if (address.isDefault) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF2E7D32))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            stringResource(R.string.address_selection_default_badge),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = address.phoneNumber ?: "",
                                color = Color.DarkGray,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = address.addressDetail ?: "",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                            val hasLocation = address.latitude?.let {
                                it.isFinite() && it in -90.0..90.0
                            } == true && address.longitude?.let {
                                it.isFinite() && it in -180.0..180.0
                            } == true
                            if (!hasLocation) {
                                Text(
                                    "Cần bổ sung vị trí trên bản đồ",
                                    color = MaterialTheme.colorScheme.error,
                                    fontSize = 13.sp,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            Row {
                                TextButton(enabled = !isBusy, onClick = { onEditClick(address) }) {
                                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (hasLocation) "Sửa" else "Bổ sung vị trí")
                                }
                                TextButton(enabled = !isBusy, onClick = { onDeleteClick(address) }) {
                                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Xóa", color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = isSelected,
                                enabled = !isBusy,
                                onClick = { onAddressSelect(address.id) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF1B5E20),
                                    unselectedColor = Color.LightGray
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "vi")
@Composable
fun AddressSelectionPreview() {
    AgritechTheme {
        AddressSelectionContent(
            addresses = listOf(
                ShippingDetails(
                    "1",
                    "Nguyễn Văn A",
                    "0901234567",
                    "123 Lê Lợi, Phường Bến Nghé, Quận 1, TP. HCM",
                    true
                )
            ),
            selectedAddressId = "1",
            onAddressSelect = {},
            onBackClick = {},
            onAddNewClick = {},
            onConfirmClick = {}
        )
    }
}
