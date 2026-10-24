package com.example.agritech_mobile.ui.main

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.agritech_mobile.R
import com.example.agritech_mobile.ui.cart.CartScreen
import com.example.agritech_mobile.ui.dashboard.HomeScreen
import com.example.agritech_mobile.ui.order.SellerOrdersScreen
import com.example.agritech_mobile.ui.user.ProfileScreen
import com.example.agritech_mobile.ui.BottomNavItem
import com.example.agritech_mobile.ui.GooeyBottomNavigation
import com.example.agritech_mobile.ui.BubbleBottomBar
import com.example.agritech_mobile.ui.user.UserViewModel
import kotlin.math.roundToInt

@Composable
fun MainScreen(
    targetTab: Int? = null,
    onTabHandled: () -> Unit = {},
    onNavigateToDetail: (String) -> Unit,
    onNavigateToCreateProduct: () -> Unit,
    onNavigateToCheckout: (String) -> Unit,
    onNavigateToAddress: () -> Unit,
    onLogoutSuccess: () -> Unit,
    onNavigateToBuyerOrders: (String) -> Unit,
    onNavigateToChat: () -> Unit,
    onNavigateToStatistics: () -> Unit = {},
    onNavigateToBargains: () -> Unit = {},
    onNavigateToReports: () -> Unit = {},
    onNavigateToAdminReports: () -> Unit = {},
    onNavigateToMyProducts: () -> Unit = {},
    onNavigateToPickupLocation: () -> Unit = {},
    onNavigateToDeliveryAddress: () -> Unit = {},
    onNavigateToNotifications: () -> Unit = {},
) {
    val userViewModel: UserViewModel = hiltViewModel()
    val pickupRequirement by userViewModel.pickupRequirement
        .collectAsStateWithLifecycle()
    val context = LocalContext.current

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        userViewModel.checkPickupRequirement()
    }

    LaunchedEffect(pickupRequirement.error) {
        pickupRequirement.error?.let { message ->
            Toast.makeText(
                context,
                message,
                Toast.LENGTH_LONG
            ).show()

            userViewModel.clearPickupRequirementError()
        }
    }

    if (pickupRequirement.showReminder) {
        AlertDialog(
            onDismissRequest = {
                userViewModel.dismissPickupReminder()
            },
            title = {
                Text("Cập nhật địa điểm lấy hàng")
            },
            text = {
                Text(
                    "Bạn cần cập nhật địa chỉ và chọn vị trí lấy hàng " +
                            "trên bản đồ để hệ thống tính phí giao hàng. " +
                            "Vui lòng cập nhật trước khi đăng bán sản phẩm."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        userViewModel.dismissPickupReminder()
                        onNavigateToPickupLocation()
                    }
                ) {
                    Text("Cập nhật ngay")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        userViewModel.dismissPickupReminder()
                    }
                ) {
                    Text("Để sau")
                }
            }
        )
    }
    var selectedIndex by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(targetTab) {
        targetTab?.let { tabIndex ->
            selectedIndex = tabIndex
            onTabHandled()
        }
    }

    val bottomBarHeight = 130.dp
    val bottomBarHeightPx = with(LocalDensity.current) { bottomBarHeight.toPx() }
    var bottomBarOffsetHeightPx by remember { mutableFloatStateOf(0f) }

    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                val newOffset = bottomBarOffsetHeightPx - delta
                bottomBarOffsetHeightPx = newOffset.coerceIn(0f, bottomBarHeightPx)
                return Offset.Zero
            }
        }
    }

    val navItems = listOf(
        BottomNavItem(stringResource(R.string.nav_home), Icons.Default.Home),
        BottomNavItem(stringResource(R.string.nav_orders), Icons.Outlined.ReceiptLong),
        BottomNavItem(stringResource(R.string.nav_cart), Icons.Outlined.ShoppingCart),
        BottomNavItem(stringResource(R.string.nav_account), Icons.Outlined.AccountCircle)
    )

    val currentTab = when (selectedIndex) {
        0 -> "HOME"
        1 -> "ORDERS"
        2 -> "CART"
        3 -> "ACCOUNT"
        else -> "HOME"
    }

    Scaffold(
        modifier = Modifier.nestedScroll(nestedScrollConnection),
        bottomBar = {
            Row(
                modifier = Modifier
                    .offset { IntOffset(x = 0, y = bottomBarOffsetHeightPx.roundToInt()) }
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                BubbleBottomBar(
                    items = navItems.map { it.icon },
                    selectedIndex = selectedIndex,
                    pillColor = Color.White,
                    activeIconColor = MaterialTheme.colorScheme.primary,
                    inactiveIconColor = Color.Gray.copy(alpha = 0.6f),
                    modifier = Modifier.weight(1f),
                    onItemSelected = { index -> selectedIndex = index }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .graphicsLayer {
                            shadowElevation = 4.dp.toPx()
                            shape = CircleShape
                            clip = true
                        }
                        .background(Color.White)
                        .border(
                            width = 1.2.dp,
                            color = Color.LightGray.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable(
                            enabled = !pickupRequirement.isChecking
                        ) {
                            userViewModel.checkPickupRequirement(
                                onAllowed = onNavigateToCreateProduct
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    bottom = 0.dp
                )
        ) {
            when (currentTab) {
                "HOME" -> HomeScreen(
                    onNavigateToDetail = onNavigateToDetail,
                    onNavigateToChat = onNavigateToChat,
                    onNavigateToDeliveryAddress = onNavigateToDeliveryAddress,
                    addressViewModel = userViewModel,
                    onNavigateToNotifications = onNavigateToNotifications,
                )

                "ORDERS" -> SellerOrdersScreen()

                "CART" -> CartScreen(
                    onNavigateToCheckout = onNavigateToCheckout
                )

                "ACCOUNT" -> ProfileScreen(
                    onNavigateToAddress = onNavigateToAddress,
                    onNavigateToOrders = { status ->
                        onNavigateToBuyerOrders(status)
                    },
                    onLogoutClick = onLogoutSuccess,
                    onNavigateToStatistics = onNavigateToStatistics,
                    onNavigateToBargains = onNavigateToBargains,
                onNavigateToReports = onNavigateToReports,
                    onNavigateToAdminReports = onNavigateToAdminReports,
                    onNavigateToMyProducts = onNavigateToMyProducts,
                    onNavigateToPickupLocation = onNavigateToPickupLocation,
                )
            }
        }
    }
}
