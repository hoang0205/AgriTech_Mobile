package com.example.agritech_mobile.ui.order

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.agritech_mobile.data.ParsedAddress
import com.example.agritech_mobile.data.getAddressComponentsFromLatLng
import com.example.agritech_mobile.ui.theme.AgritechTheme
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun MapPickerScreen(
    onAddressSelected: (ParsedAddress, LatLng) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val isPreview = LocalInspectionMode.current

    val defaultLocation = LatLng(21.028511, 105.854444)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    var parsedResult by remember { mutableStateOf(ParsedAddress(fullAddress = "Đang xác định vị trí...")) }
    var currentLatLng by remember { mutableStateOf(defaultLocation) }
    var isLoadingAddress by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val moveToCurrentLocation: () -> Unit = {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    coroutineScope.launch {
                        cameraPositionState.animate(
                            CameraUpdateFactory.newLatLngZoom(userLatLng, 16f)
                        )
                    }
                } else {
                    Toast.makeText(context, "Không thể xác định vị trí GPS", Toast.LENGTH_SHORT).show()
                }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val granted = fineGranted || coarseGranted
        hasLocationPermission = granted
        if (granted) {
            moveToCurrentLocation()
        } else {
            Toast.makeText(context, "Cần cấp quyền vị trí để tự động định vị", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            moveToCurrentLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(cameraPositionState.isMoving) {
        if (!cameraPositionState.isMoving && !isPreview) {
            isLoadingAddress = true
            val target = cameraPositionState.position.target
            currentLatLng = target
            parsedResult = getAddressComponentsFromLatLng(context, target.latitude, target.longitude)
            isLoadingAddress = false
        }
    }

    MapPickerContent(
        modifier = modifier,
        addressText = parsedResult.fullAddress,
        isLoadingAddress = isLoadingAddress,
        isConfirmEnabled = !cameraPositionState.isMoving && !isLoadingAddress && parsedResult.fullAddress.isNotBlank(),
        onConfirmClick = { onAddressSelected(parsedResult, currentLatLng) },
        onMyLocationClick = {
            if (hasLocationPermission) {
                moveToCurrentLocation()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        onBackClick = onBackClick,
        mapContent = {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(
                    zoomControlsEnabled = false,
                    myLocationButtonEnabled = false
                ),
                properties = MapProperties(isMyLocationEnabled = hasLocationPermission)
            )
        }
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapPickerContent(
    addressText: String,
    isLoadingAddress: Boolean,
    isConfirmEnabled: Boolean,
    onConfirmClick: () -> Unit,
    onMyLocationClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    mapContent: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Ghim địa chỉ giao hàng", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            mapContent()

            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = "Pin",
                tint = Color.Red,
                modifier = Modifier
                    .size(48.dp)
                    .align(Alignment.Center)
                    .offset(y = (-24).dp)
            )

            SmallFloatingActionButton(
                onClick = onMyLocationClick,
                containerColor = Color.White,
                contentColor = Color(0xFF1B5E20),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 210.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "GPS")
            }

            // Bottom Card hiển thị thông tin địa chỉ
            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Vị trí đã chọn:",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.Gray,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (isLoadingAddress) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFF1B5E20)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Đang tải tên địa chỉ...",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )
                        }
                    } else {
                        Text(
                            text = addressText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            maxLines = 3,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onConfirmClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        enabled = isConfirmEnabled,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Xác nhận địa chỉ này", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

@Preview(name = "1. Trạng thái đã tải xong địa chỉ", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun MapPickerPreviewSuccess() {
    AgritechTheme {
        MapPickerContent(
            addressText = "Số 144 Xuân Thủy, Phường Dịch Vọng Hậu, Quận Cầu Giấy, Thành phố Hà Nội",
            isLoadingAddress = false,
            isConfirmEnabled = true,
            onConfirmClick = {},
            onMyLocationClick = {},
            onBackClick = {},
            mapContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE3EAE5)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "🗺️ Google Map Mockup",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }
}

@Preview(name = "2. Trạng thái đang tải (Loading)", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun MapPickerPreviewLoading() {
    AgritechTheme {
        MapPickerContent(
            addressText = "",
            isLoadingAddress = true,
            isConfirmEnabled = false,
            onConfirmClick = {},
            onMyLocationClick = {},
            onBackClick = {},
            mapContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE8ECEF)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Đang di chuyển trên bản đồ...",
                        color = Color.Gray
                    )
                }
            }
        )
    }
}