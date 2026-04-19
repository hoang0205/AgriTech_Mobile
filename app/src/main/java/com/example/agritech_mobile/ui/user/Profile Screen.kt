package com.example.agritech_mobile.ui.user

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.agritech_mobile.R
import com.example.agritech_mobile.di.bitmapToUri
import com.example.agritech_mobile.di.uriToMultipartBodyPart
import com.example.agritech_mobile.ui.theme.AgritechTheme


@Composable
fun ProfileScreen(
    onBackClick: () -> Unit,
    onNavigateToAddress: () -> Unit,
    onNavigateToOrders: (String) -> Unit,
    onLogoutClick: () -> Unit,
    userViewModel: UserViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val userState by userViewModel.userState.collectAsState()
    val profileState by userViewModel.profileState.collectAsState()

    var showEditDialog by remember { mutableStateOf(false) }

    LaunchedEffect(userState) {
        when (userState) {
            is UserState.Success -> {
                val message = (userState as UserState.Success).message
                if (message.isNotBlank()) {
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                }
            }

            is UserState.Error -> {
                Toast.makeText(context, (userState as UserState.Error).message, Toast.LENGTH_SHORT)
                    .show()
            }

            else -> {}
        }
    }

    val name = when (profileState) {
        is UserProfileState.Success -> (profileState as UserProfileState.Success).fullName
        is UserProfileState.Loading -> "Đang tải..."
        else -> "Khách hàng"
    }

    val imageUrl = when (profileState) {
        is UserProfileState.Success -> {
            val url = (profileState as UserProfileState.Success).avatarUrl
            if (url.isNotBlank()) url else ""
        }

        else -> ""
    }

    Box(modifier = Modifier.fillMaxSize()) {
        ProfileContent(
            name = name,
            imageUrl = imageUrl,
            onBackClick = onBackClick,
            onNavigateToAddress = onNavigateToAddress,
            onNavigateToOrders = onNavigateToOrders,
            onLogoutClick = onLogoutClick,
            onEditClick = { showEditDialog = true }
        )

        if (showEditDialog) {
            EditProfileDialog(
                context = context,
                currentName = if (name == "Đang tải...") "" else name,
                currentAvatarUrl = imageUrl,
                onDismiss = { showEditDialog = false },
                onSave = { newName, newImagePart ->
                    userViewModel.updateProfileWithImage(
                        fullName = newName,
                        imagePart = newImagePart,
                        currentAvatarUrl = imageUrl
                    )
                    showEditDialog = false
                }
            )
        }

        if (userState is UserState.Loading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1B5E20))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(
    context: Context,
    currentName: String,
    currentAvatarUrl: String,
    onDismiss: () -> Unit,
    onSave: (String, okhttp3.MultipartBody.Part?) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri -> if (uri != null) selectedImageUri = uri }
    )

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap: Bitmap? ->
            if (bitmap != null) {
                selectedImageUri = bitmapToUri(context, bitmap)
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Cập nhật thông tin",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20)
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9))
                        .border(2.dp, Color(0xFF1B5E20), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (selectedImageUri != null) {
                        AsyncImage(
                            model = selectedImageUri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else if (currentAvatarUrl.isNotBlank()) {
                        AsyncImage(
                            model = currentAvatarUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color(0xFF1B5E20),
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TextButton(onClick = { cameraLauncher.launch(null) }) {
                        Icon(
                            Icons.Default.PhotoCamera,
                            contentDescription = null,
                            tint = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Chụp ảnh", color = Color(0xFF1B5E20))
                    }
                    TextButton(
                        onClick = {
                            galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        }
                    ) {
                        Icon(
                            Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = Color(0xFF1B5E20)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Thư viện", color = Color(0xFF1B5E20))
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Họ và tên") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val imagePart =
                        selectedImageUri?.let { uriToMultipartBodyPart(context, it, "files") }
                    onSave(newName, imagePart)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
            ) {
                Text("Lưu")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Hủy", color = Color(0xFF1B5E20)) }
        },
        containerColor = Color.White
    )
}

@Composable
fun ProfileContent(
    name: String,
    imageUrl: String,
    onBackClick: () -> Unit,
    onNavigateToAddress: () -> Unit,
    onNavigateToOrders: (String) -> Unit,
    onLogoutClick: () -> Unit,
    onEditClick: () -> Unit
) {
    Scaffold(
        containerColor = Color(0xFFF8FAF9),
        contentWindowInsets = WindowInsets(0.dp),
        topBar = { ProfileTopBar(onBackClick) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            ProfileHeader(
                name = name,
                imageUrl = imageUrl,
                onEditClick = onEditClick // Truyền hàm xuống
            )

            Spacer(modifier = Modifier.height(32.dp))

            MyOrdersSection(onNavigateToOrders = onNavigateToOrders)

            Spacer(modifier = Modifier.height(24.dp))

            AccountSettingsSection(onNavigateToAddress = onNavigateToAddress)

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedButton(
                onClick = onLogoutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .height(54.dp),
                border = BorderStroke(1.dp, Color(0xFFE57373)),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFFF0F0))
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Color(0xFFD32F2F)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    stringResource(R.string.profile_logout),
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTopBar(onBackClick: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.profile_title),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B5E20),
                fontSize = 18.sp
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
        actions = {
            Text(
                text = stringResource(R.string.profile_app_name),
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1B5E20),
                fontSize = 18.sp,
                modifier = Modifier.padding(end = 16.dp)
            )
        },
        windowInsets = WindowInsets(0.dp),
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun ProfileHeader(name: String, imageUrl: String, onEditClick: () -> Unit) {
    Box(contentAlignment = Alignment.BottomEnd) {

        if (imageUrl.isNotBlank()) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(4.dp, Color(0xFF1B5E20), RoundedCornerShape(24.dp))
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Avatar",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        } else {
            val initial = if (name.isNotBlank()) name.trim().first().uppercase() else "?"

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color(0xFF4CAF50))
                    .border(4.dp, Color(0xFF1B5E20), RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initial,
                    fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }

        Box(
            modifier = Modifier
                .offset(x = 8.dp, y = 8.dp)
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFF1B5E20))
                .clickable { onEditClick() }
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = "Edit",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Text(name, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF212121))
    Spacer(modifier = Modifier.height(4.dp))
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE8F5E9))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            stringResource(R.string.profile_badge_premium),
            color = Color(0xFF2E7D32),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

@Composable
fun MyOrdersSection(onNavigateToOrders: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.profile_orders_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF212121)
                )
                Text(
                    stringResource(R.string.profile_orders_see_all),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1B5E20),
                    modifier = Modifier.clickable { onNavigateToOrders("ALL") })
            }
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OrderStatusItem(
                    Icons.Default.PendingActions,
                    stringResource(R.string.order_status_pending),
                    2,
                    { onNavigateToOrders("PENDING") })
                OrderStatusItem(
                    Icons.Default.Inventory,
                    stringResource(R.string.order_status_confirmed),
                    0,
                    { onNavigateToOrders("CONFIRMED") })
                OrderStatusItem(
                    Icons.Default.LocalShipping,
                    stringResource(R.string.order_status_shipping),
                    0,
                    { onNavigateToOrders("SHIPPING") })
                OrderStatusItem(
                    Icons.Default.CheckCircle,
                    stringResource(R.string.order_status_completed),
                    0,
                    { onNavigateToOrders("COMPLETED") })
                OrderStatusItem(
                    Icons.Default.Cancel,
                    stringResource(R.string.order_status_cancelled),
                    0,
                    { onNavigateToOrders("CANCELLED") })
            }
        }
    }
}

@Composable
fun OrderStatusItem(icon: ImageVector, label: String, badgeCount: Int, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable { onClick() }) {
        Box {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF5F5F5)), contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = Color(0xFF1B5E20),
                    modifier = Modifier.size(28.dp)
                )
            }
            if (badgeCount > 0) {
                Box(
                    modifier = Modifier
                        .offset(x = 6.dp, y = (-6).dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFD32F2F))
                        .align(Alignment.TopEnd)
                        .border(2.dp, Color.White, CircleShape), contentAlignment = Alignment.Center
                ) {
                    Text(
                        badgeCount.toString(),
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF424242),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun AccountSettingsSection(onNavigateToAddress: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            stringResource(R.string.profile_settings_title),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Column {
                SettingsMenuItem(
                    Icons.Default.LocationOn,
                    stringResource(R.string.profile_menu_address),
                    onNavigateToAddress
                )
                HorizontalDivider(
                    color = Color(0xFFF0F0F0),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsMenuItem(
                    Icons.Default.Payments,
                    stringResource(R.string.profile_menu_payment),
                    {})
                HorizontalDivider(
                    color = Color(0xFFF0F0F0),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsMenuItem(
                    Icons.Default.Lock,
                    stringResource(R.string.profile_menu_password),
                    {})
                HorizontalDivider(
                    color = Color(0xFFF0F0F0),
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                SettingsMenuItem(
                    Icons.Default.HelpCenter,
                    stringResource(R.string.profile_menu_help),
                    {})
            }
        }
    }
}

@Composable
fun SettingsMenuItem(icon: ImageVector, title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFE8F5E9)), contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = Color(0xFF1B5E20),
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF212121),
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = Color.LightGray
        )
    }
}

@Preview(showBackground = true, locale = "vi", device = "spec:width=411dp,height=1200dp")
@Composable
fun ProfileScreenPreview() {
    AgritechTheme {
        ProfileContent(
            name = "Preview User",
            imageUrl = "https://images.unsplash.com/photo-1595956553066-fe24a8c33395?q=80&w=400",
            onBackClick = {},
            onNavigateToAddress = {},
            onNavigateToOrders = {},
            onLogoutClick = {},
            onEditClick = {}
        )
    }
}