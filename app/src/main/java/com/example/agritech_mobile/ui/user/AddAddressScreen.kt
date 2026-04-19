package com.example.agritech_mobile.ui.user

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.ui.theme.AgritechTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAddressScreen(
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: UserViewModel = hiltViewModel()
) {
    val addressState by viewModel.addressState.collectAsState()

    val provinces by viewModel.provinces.collectAsState()
    val wards by viewModel.wards.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.getProvinces()
    }

    AddAddressContent(
        onBackClick = onBackClick,
        onSaveSuccess = onSaveSuccess,
        addressState = addressState,

        provinces = provinces.map { it.name },
        wards = wards.map { it.name },

        onProvinceSelected = { selectedProvinceName ->
            val provinceCode = provinces.find { it.name == selectedProvinceName }?.code

            if (provinceCode != null) {
                viewModel.getWards(provinceCode)
            }
        },

        onAddNewAddress = { name, phone, address, isDefault ->
            viewModel.addNewAddress(name, phone, address, isDefault)
        }
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
    onProvinceSelected: (String) -> Unit,
    onAddNewAddress: (String, String, String, Boolean) -> Unit
) {
    val context = LocalContext.current

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var ward by remember { mutableStateOf("") }
    var detail by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }

    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(addressState) {
        if (isSubmitting) {
            when (val state = addressState) {
                is AddressState.Success -> {
                    isSubmitting = false
                    Toast.makeText(context, "Thêm địa chỉ thành công", Toast.LENGTH_SHORT).show()
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
                            stringResource(R.string.add_address_topbar_title),
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
                                if (name.isBlank() || phone.isBlank() || detail.isBlank()) {
                                    Toast.makeText(
                                        context,
                                        "Vui lòng nhập đủ thông tin",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }
                                isSubmitting = true
                                val fullAddress = "$detail, $ward, $city"
                                onAddNewAddress(name, phone, fullAddress, isDefault)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.add_address_save),
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
                    stringResource(R.string.add_address_header),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    stringResource(R.string.add_address_description),
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
                        CustomTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = stringResource(R.string.add_address_fullname)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        CustomTextField(
                            value = phone,
                            onValueChange = { phone = it },
                            label = stringResource(R.string.add_address_phone)
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        CustomDropdownField(
                            value = city,
                            label = stringResource(R.string.add_address_city),
                            options = provinces,
                            onOptionSelected = { selectedCity ->
                                city = selectedCity
                                ward = ""
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
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        CustomTextField(
                            value = detail,
                            onValueChange = { detail = it },
                            label = stringResource(R.string.add_address_detail),
                            placeholder = stringResource(R.string.add_address_detail_placeholder),
                            singleLine = false,
                            lines = 3
                        )

                        Spacer(modifier = Modifier.height(24.dp))

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
                                Column {
                                    Text(
                                        stringResource(R.string.add_address_set_default),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
//                                    Text(
//                                        stringResource(R.string.add_address_set_default_desc),
//                                        color = Color.Gray,
//                                        fontSize = 12.sp
//                                    )
                                }
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

                Spacer(modifier = Modifier.height(24.dp))

//                Box(
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .height(180.dp)
//                        .clip(RoundedCornerShape(16.dp))
//                ) {
//                    AsyncImage(
//                        model = "https://images.unsplash.com/photo-1524661135-423995f22d0b?q=80&w=800",
//                        contentDescription = "Map",
//                        contentScale = ContentScale.Crop,
//                        modifier = Modifier.fillMaxSize()
//                    )
//                    Box(
//                        modifier = Modifier
//                            .fillMaxSize()
//                            .background(Color.Black.copy(alpha = 0.5f))
//                    )
//                    Icon(
//                        Icons.Default.LocationOn,
//                        contentDescription = null,
//                        tint = Color(0xFF4CAF50),
//                        modifier = Modifier
//                            .align(Alignment.Center)
//                            .size(40.dp)
//                    )
//
//                    Button(
//                        onClick = { /* Lấy vị trí */ },
//                        modifier = Modifier
//                            .align(Alignment.BottomEnd)
//                            .padding(12.dp),
//                        colors = ButtonDefaults.buttonColors(containerColor = Color.White),
//                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
//                    ) {
//                        Icon(
//                            Icons.Default.MyLocation,
//                            contentDescription = null,
//                            tint = Color(0xFF1B5E20),
//                            modifier = Modifier.size(16.dp)
//                        )
//                        Spacer(modifier = Modifier.width(4.dp))
//                        Text(
//                            stringResource(R.string.add_address_my_location),
//                            color = Color(0xFF212121),
//                            fontWeight = FontWeight.Medium,
//                            fontSize = 12.sp
//                        )
//                    }
//                }
//                Spacer(modifier = Modifier.height(24.dp))
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
            ) },
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
            onAddNewAddress = { name, phone, address, isDefault -> }
        )
    }
}