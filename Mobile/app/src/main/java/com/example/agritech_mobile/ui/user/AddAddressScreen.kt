package com.example.agritech_mobile.ui.user

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.agritech_mobile.R
import com.example.agritech_mobile.data.remote.dto.AddressRequest
import com.example.agritech_mobile.data.remote.dto.ShippingDetails
import com.example.agritech_mobile.ui.theme.AgritechTheme

private fun normalizeName(name: String): String {
    return name
        .replace(
            Regex(
                "^(Thành phố|Tỉnh|TP\\.?|Quận|Huyện|Thị xã|TX\\.?|Q\\.?|H\\.?|Phường|Xã|Thị trấn|P\\.?|X\\.?|TT\\.?)\\s+",
                RegexOption.IGNORE_CASE
            ), ""
        )
        .trim()
        .lowercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAddressScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    onPickMapClick: (String, String) -> Unit = { _, _ -> },
    pickedProvince: String? = null,
    pickedWard: String? = null,
    pickedDetail: String? = null,
    pickedLatitude: Double? = null,
    pickedLongitude: Double? = null,
    pickedSelectionId: String? = null,
    addressId: String? = null,
    viewModel: UserViewModel = hiltViewModel()
) {
    val addressState by viewModel.addressState.collectAsState()
    val provinces by viewModel.provinces.collectAsState()
    val wards by viewModel.wards.collectAsState()
    var initialAddress by remember(addressId) { mutableStateOf<ShippingDetails?>(null) }

    LaunchedEffect(addressId) {
        viewModel.getProvinces()
        if (addressId != null) viewModel.getUserAddresses()
    }

    LaunchedEffect(addressState, addressId) {
        if (addressId != null && initialAddress == null) {
            initialAddress = (addressState as? AddressState.Success)
                ?.addresses?.find { it.id == addressId }
        }
    }

    if (addressId != null && initialAddress == null) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            when (val state = addressState) {
                is AddressState.Error, is AddressState.Success -> {
                    Text((state as? AddressState.Error)?.message ?: "Địa chỉ không còn tồn tại")
                    TextButton(onClick = { viewModel.getUserAddresses() }) { Text("Tải lại") }
                    TextButton(onClick = onBackClick) { Text("Quay lại") }
                }
                else -> CircularProgressIndicator()
            }
        }
        return
    }

    AddAddressContent(
        onBackClick = onBackClick,
        onSaveSuccess = onSaveSuccess,
        onPickMapClick = onPickMapClick,
        pickedProvince = pickedProvince,
        pickedWard = pickedWard,
        pickedDetail = pickedDetail,
        initialAddress = initialAddress,
        addressState = addressState,
        provinces = provinces.map { it.name },
        wards = wards.map { it.name },
        onProvinceSelected = { selectedProvinceName ->
            val provinceCode = provinces.find { it.name == selectedProvinceName }?.code
            if (provinceCode != null) {
                viewModel.getWards(provinceCode)
            }
        },
        onAddNewAddress = { name, phone, address, isDefault, latitude, longitude ->

            if (addressId != null) {
                viewModel.updateAddress(
                    addressId,
                    AddressRequest(name, phone, address, isDefault, latitude, longitude)
                )
            } else viewModel.addNewAddress(
                name = name,
                phone = phone,
                address = address,
                isDefault = isDefault,
                latitude = latitude,
                longitude = longitude
            )
        },
        pickedLatitude = pickedLatitude,
        pickedLongitude = pickedLongitude,
        pickedSelectionId = pickedSelectionId,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAddressContent(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    addressState: AddressState,
    provinces: List<String>,
    wards: List<String>,
    pickedProvince: String? = null,
    pickedWard: String? = null,
    pickedDetail: String? = null,
    pickedLatitude: Double? = null,
    pickedLongitude: Double? = null,
    pickedSelectionId: String? = null,
    isPickupLocation: Boolean = false,
    initialAddress: ShippingDetails? = null,
    onPickMapClick: (String, String) -> Unit = { _, _ -> },
    onProvinceSelected: (String) -> Unit,
    onAddNewAddress: (
        String,
        String,
        String,
        Boolean,
        Double?,
        Double?
    ) -> Unit
) {
    val context = LocalContext.current

    val storedParts = remember(initialAddress?.id) {
        initialAddress?.addressDetail.orEmpty()
            .replace(Regex(",\\s*(Việt Nam|Vietnam)\\s*$", RegexOption.IGNORE_CASE), "")
            .split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    var name by rememberSaveable { mutableStateOf(initialAddress?.receiverName.orEmpty()) }
    var phone by rememberSaveable { mutableStateOf(initialAddress?.phoneNumber.orEmpty()) }
    var city by rememberSaveable {
        mutableStateOf(if (storedParts.size >= 3) storedParts.last() else "")
    }
    var ward by rememberSaveable {
        mutableStateOf(if (storedParts.size >= 3) storedParts[storedParts.lastIndex - 1] else "")
    }
    var detail by rememberSaveable {
        mutableStateOf(if (storedParts.size >= 3) storedParts.dropLast(2).joinToString(", ")
            else initialAddress?.addressDetail.orEmpty())
    }
    var isDefault by rememberSaveable { mutableStateOf(initialAddress?.isDefault ?: false) }

    var latitude by rememberSaveable {
        mutableStateOf<Double?>(initialAddress?.latitude)
    }
    var longitude by rememberSaveable {
        mutableStateOf<Double?>(initialAddress?.longitude)
    }
    var appliedSelectionId by rememberSaveable {
        mutableStateOf<String?>(null)
    }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(provinces) {
        if (city.isNotBlank() && city in provinces) onProvinceSelected(city)
    }

    LaunchedEffect(
        pickedSelectionId,
        pickedLatitude,
        pickedLongitude
    ) {
        if (
            pickedSelectionId != null &&
            pickedSelectionId != appliedSelectionId &&
            pickedLatitude != null &&
            pickedLongitude != null
        ) {
            latitude = pickedLatitude
            longitude = pickedLongitude
            appliedSelectionId = pickedSelectionId
        }
    }

    LaunchedEffect(pickedDetail) {
        if (!pickedDetail.isNullOrBlank()) {
            detail = pickedDetail
        }
    }


    LaunchedEffect(pickedProvince, provinces) {
        if (!pickedProvince.isNullOrBlank() && provinces.isNotEmpty()) {
            val cleanMapProvince = normalizeName(pickedProvince)
            val matchedCity = provinces.find { p ->
                val cleanP = normalizeName(p)
                cleanP == cleanMapProvince || cleanP.contains(cleanMapProvince) || cleanMapProvince.contains(
                    cleanP
                )
            }
            if (matchedCity != null && city != matchedCity) {
                city = matchedCity
                ward = ""
                onProvinceSelected(matchedCity)
            }
        }
    }

    LaunchedEffect(pickedWard, wards) {
        if (!pickedWard.isNullOrBlank() && wards.isNotEmpty()) {
            val cleanMapWard = normalizeName(pickedWard)
            val matchedWard = wards.find { w ->
                val cleanW = normalizeName(w)
                cleanW == cleanMapWard || cleanW.contains(cleanMapWard) || cleanMapWard.contains(
                    cleanW
                )
            }
            if (matchedWard != null) {
                ward = matchedWard
            } else {
                ward = pickedWard
            }
        }
    }

    LaunchedEffect(addressState) {
        if (isSubmitting) {
            when (val state = addressState) {
                is AddressState.Success -> {
                    isSubmitting = false
                    Toast.makeText(
                        context,
                        if (isPickupLocation) {
                            "Đã lưu địa điểm lấy hàng"
                        } else if (initialAddress != null) {
                            "Cập nhật địa chỉ thành công"
                        } else {
                            "Thêm địa chỉ thành công"
                        },
                        Toast.LENGTH_SHORT
                    ).show()
                    onSaveSuccess()
                }

                is AddressState.Error -> {
                    isSubmitting = false
                    Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = Color(0xFFF8FAF9),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            if (isPickupLocation) {
                                "Địa điểm lấy hàng"
                            } else if (initialAddress != null) {
                                "Sửa địa chỉ"
                            } else {
                                stringResource(R.string.add_address_topbar_title)
                            },
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
                Surface(
                    shadowElevation = 16.dp,
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    Box(
                        modifier = Modifier
                            .padding(16.dp)
                            .navigationBarsPadding()
                    ) {
                        Button(
                            onClick = {
                                val missingContact = !isPickupLocation &&
                                        (name.isBlank() || phone.isBlank())

                                if (
                                    missingContact ||
                                    detail.isBlank() ||
                                    city.isBlank() ||
                                    ward.isBlank()
                                ) {
                                    Toast.makeText(
                                        context,
                                        "Vui lòng nhập đủ thông tin địa chỉ",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                if (latitude == null || longitude == null) {
                                    Toast.makeText(
                                        context,
                                        "Vui lòng chọn vị trí trên bản đồ",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                val fullAddress = listOf(
                                    detail.trim(),
                                    ward.trim(),
                                    city.trim()
                                ).joinToString(", ")

                                if (
                                    isPickupLocation &&
                                    fullAddress.length > 1000
                                ) {
                                    Toast.makeText(
                                        context,
                                        "Địa chỉ không được vượt quá 1000 ký tự",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }

                                isSubmitting = true
                                onAddNewAddress(
                                    name,
                                    phone,
                                    fullAddress,
                                    isDefault,
                                    latitude,
                                    longitude
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSubmitting,
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isPickupLocation) {
                                    "Lưu địa điểm lấy hàng"
                                } else if (initialAddress != null) {
                                    "Lưu thay đổi"
                                } else {
                                    stringResource(R.string.add_address_save)
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
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
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                Text(
                    if (isPickupLocation) {
                        "Địa chỉ lấy hàng"
                    } else if (initialAddress != null) {
                        "Cập nhật địa chỉ"
                    } else {
                        stringResource(R.string.add_address_header)
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    if (isPickupLocation) {
                        "Chọn địa chỉ và vị trí lấy hàng cho các sản phẩm bạn đăng bán."
                    } else {
                        stringResource(R.string.add_address_description)
                    },
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(24.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        if (!isPickupLocation) {
                            CustomTextField(
                                value = name,
                                onValueChange = { name = it },
                                label = stringResource(
                                    R.string.add_address_fullname
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            CustomTextField(
                                value = phone,
                                onValueChange = { phone = it },
                                label = stringResource(
                                    R.string.add_address_phone
                                )
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        CustomDropdownField(
                            value = city,
                            label = stringResource(R.string.add_address_city),
                            options = provinces,
                            onOptionSelected = { selectedCity ->
                                city = selectedCity
                                ward = ""
                                latitude = null
                                longitude = null
                                onProvinceSelected(selectedCity)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CustomDropdownField(
                            value = ward,
                            label = stringResource(R.string.add_address_ward),
                            options = wards,
                            onOptionSelected = { selectedWard ->
                                ward = selectedWard
                                latitude = null
                                longitude = null
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        CustomTextField(
                            value = detail,
                            onValueChange = {
                                detail = it
                                latitude = null
                                longitude = null
                            },
                            label = stringResource(R.string.add_address_detail),
                            placeholder = stringResource(R.string.add_address_detail_placeholder),
                            singleLine = false,
                            lines = 3
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                val addressParts = listOf(detail, ward, city)
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }

                                val areaParts = listOf(ward, city)
                                    .map { it.trim() }
                                    .filter { it.isNotBlank() }

                                val addressQuery = if (addressParts.isEmpty()) {
                                    ""
                                } else {
                                    (addressParts + "Việt Nam").joinToString(", ")
                                }

                                val areaQuery = if (areaParts.isEmpty()) {
                                    ""
                                } else {
                                    (areaParts + "Việt Nam").joinToString(", ")
                                }

                                onPickMapClick(addressQuery, areaQuery)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFF1B5E20)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color(0xFFE8F5E9).copy(alpha = 0.5f),
                                contentColor = Color(0xFF1B5E20)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Chọn vị trí trên bản đồ",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        if (!isPickupLocation) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE8F5E9)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        stringResource(R.string.add_address_set_default),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                                Switch(
                                    checked = isDefault,
                                    onCheckedChange = { isDefault = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF1B5E20)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        if (isSubmitting) {
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
fun CustomTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    singleLine: Boolean = true,
    lines: Int = 1
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                label,
                color = Color(0xFF1B5E20),
                fontWeight = FontWeight.Bold,
            )
        },
        placeholder = { Text(placeholder, color = Color.Gray) },
        modifier = Modifier
            .fillMaxWidth()
            .height(if (singleLine) 64.dp else (64 + (lines - 1) * 20).dp),
        singleLine = singleLine,
        maxLines = lines,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Color(0xFF1B5E20),
            unfocusedBorderColor = Color(0xFF707070),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomDropdownField(
    value: String,
    label: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = {
                Text(
                    label,
                    color = Color(0xFF1B5E20),
                    fontWeight = FontWeight.Bold
                )
            },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF1B5E20),
                unfocusedBorderColor = Color(0xFF707070),
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
            ),
            shape = RoundedCornerShape(8.dp)
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            if (options.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("Loading...", color = Color.Gray) },
                    onClick = { expanded = false }
                )
            } else {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onOptionSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, locale = "vi", device = "spec:width=411dp,height=1200dp")
@Composable
fun AddAddressPreview() {
    AgritechTheme {
        AddAddressContent(
            onBackClick = {},
            onSaveSuccess = {},
            addressState = AddressState.Idle,
            provinces = listOf("Hà Nội", "Hồ Chí Minh"),
            wards = listOf("Phường 1", "Phường 2"),
            onProvinceSelected = {},
            onAddNewAddress = { _, _, _, _, _, _ -> }
        )
    }
}
