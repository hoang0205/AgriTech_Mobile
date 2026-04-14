package com.example.agritech_mobile.ui.order

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.agritech_mobile.ui.theme.AgritechTheme

data class ShippingAddress(
    val id: String,
    val name: String,
    val phone: String,
    val address: String,
    val isDefault: Boolean = false
)

@Composable
fun AddressSelectionScreen(
    onBackClick: () -> Unit,
    onAddNewClick: () -> Unit,
    onConfirmClick: (ShippingAddress) -> Unit
) {
    // Dummy Data
    val addresses = remember {
        listOf(
            ShippingAddress(
                "1",
                "Nguyễn Văn A",
                "0901234567",
                "123 Lê Lợi, Phường Bến Nghé, Quận 1, TP. HCM",
                true
            ),
            ShippingAddress(
                "2",
                "Trần Thị B",
                "0988776655",
                "456 Võ Văn Kiệt, Phường Cô Giang, Quận 1, TP. HCM",
                false
            ),
            ShippingAddress(
                "3",
                "Lê Hoàng Nam",
                "0912334455",
                "789 Nguyễn Huệ, Phường Bến Nghé, Quận 1, TP. HCM",
                false
            )
        )
    }

    var selectedAddressId by remember {
        mutableStateOf(
            addresses.firstOrNull { it.isDefault }?.id ?: ""
        )
    }

    AddressSelectionContent(
        addresses = addresses,
        selectedAddressId = selectedAddressId,
        onAddressSelect = { selectedAddressId = it },
        onBackClick = onBackClick,
        onAddNewClick = onAddNewClick,
        onConfirmClick = {
            val selected = addresses.find { it.id == selectedAddressId }
            if (selected != null) onConfirmClick(selected)
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressSelectionContent(
    addresses: List<ShippingAddress>,
    selectedAddressId: String,
    onAddressSelect: (String) -> Unit,
    onBackClick: () -> Unit,
    onAddNewClick: () -> Unit,
    onConfirmClick: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF8FAF9),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Chọn địa chỉ nhận hàng",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Xác nhận", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                        .clickable { onAddNewClick() }
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
                        "Thêm địa chỉ mới",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    "ĐỊA CHỈ ĐÃ LƯU",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
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
                        .clickable { onAddressSelect(address.id) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = bgColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(address.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                if (address.isDefault) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF2E7D32))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "MẶC ĐỊNH",
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(address.phone, color = Color.DarkGray, fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                address.address,
                                color = Color.Gray,
                                fontSize = 14.sp,
                                lineHeight = 20.sp
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { /* TODO: */ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color(0xFF81C784),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            RadioButton(
                                selected = isSelected,
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

@Preview(showBackground = true)
@Composable
fun AddressSelectionPreview() {
    AgritechTheme { AddressSelectionScreen({}, {}, {}) }
}