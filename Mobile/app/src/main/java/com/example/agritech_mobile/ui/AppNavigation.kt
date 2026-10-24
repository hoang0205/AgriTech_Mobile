package com.example.agritech_mobile.ui

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.agritech_mobile.ui.auth.ForgotPasswordScreen
import com.example.agritech_mobile.ui.auth.LoginScreen
import com.example.agritech_mobile.ui.auth.ResetPasswordScreen
import com.example.agritech_mobile.ui.auth.SignUpScreen
import com.example.agritech_mobile.ui.auth.VerifyEmailScreen
import com.example.agritech_mobile.ui.chat.ChatListScreen
import com.example.agritech_mobile.ui.chat.ChatScreen
import com.example.agritech_mobile.ui.cart.CheckoutScreen
import com.example.agritech_mobile.ui.chat.AiChatScreen
import com.example.agritech_mobile.ui.dashboard.MyProductsScreen
import com.example.agritech_mobile.ui.dashboard.EditProductScreen
import com.example.agritech_mobile.ui.dashboard.ProductDetailScreen
import com.example.agritech_mobile.ui.dashboard.SellProductScreen
import com.example.agritech_mobile.ui.dashboard.SellerStoreScreen
import com.example.agritech_mobile.ui.main.MainScreen
import com.example.agritech_mobile.ui.order.AddressSelectionScreen
import com.example.agritech_mobile.ui.notification.NotificationScreen
import com.example.agritech_mobile.ui.order.MapPickerScreen
import com.example.agritech_mobile.ui.order.OrderViewModel
import com.example.agritech_mobile.ui.order.VnpayPaymentScreen
import com.example.agritech_mobile.ui.report.AdminReportScreen
import com.example.agritech_mobile.ui.report.ReportHistoryScreen
import com.example.agritech_mobile.ui.report.ReportScreen
import com.example.agritech_mobile.ui.user.BuyerOrdersScreen
import com.example.agritech_mobile.ui.user.AddAddressScreen
import com.example.agritech_mobile.ui.user.ReviewProductScreen
import com.example.agritech_mobile.ui.user.SellerPickupScreen
import com.example.agritech_mobile.ui.user.StatisticsScreen
import com.example.agritech_mobile.ui.user.UserViewModel
import com.google.firebase.auth.FirebaseAuth
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavigation(
    startRoute: String,
    isLoggedIn: Boolean,
    pendingChatRoomId: String? = null,
    onChatNavigated: () -> Unit = {},
    pendingTargetScreen: String? = null,
    pendingOrderStatus: String? = null,
    onOrderNavigated: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val currentEntry by navController.currentBackStackEntryAsState()

    LaunchedEffect(isLoggedIn, currentEntry) {
        val destination = currentEntry?.destination ?: return@LaunchedEffect
        if (!isLoggedIn && destination.hierarchy.none { it.route == "auth_graph" }) {
            navController.navigate("auth_graph") {
                popUpTo(navController.graph.id) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    var targetMainTab by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(pendingChatRoomId, isLoggedIn) {
        if (!pendingChatRoomId.isNullOrBlank() && isLoggedIn) {
            val myId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val partnerId = pendingChatRoomId.split("_").firstOrNull { it != myId } ?: ""

            navController.navigate("chat/$pendingChatRoomId/$partnerId/${Uri.encode("Tin nhắn")}")
            onChatNavigated()
        }
    }

    LaunchedEffect(pendingTargetScreen, pendingOrderStatus, isLoggedIn) {
        if (isLoggedIn) {
            when (pendingTargetScreen) {
                "bargains" -> {
                    navController.navigate("bargains") { launchSingleTop = true }
                    onOrderNavigated()
                }
                "my_reports" -> {
                    navController.navigate("my_reports") { launchSingleTop = true }
                    onOrderNavigated()
                }
                "buyer_orders" -> {
                    val status = pendingOrderStatus ?: "ALL"
                    navController.navigate("buyer_orders/$status") {
                        launchSingleTop = true
                    }
                    onOrderNavigated()
                }
                "seller_orders" -> {
                    targetMainTab = 1
                    navController.popBackStack("main_screen", inclusive = false)
                    onOrderNavigated()
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute,
        enterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(350)
            ) + fadeIn(animationSpec = tween(350))
        },
        exitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(350)
            ) + fadeOut(animationSpec = tween(350))
        },
        popEnterTransition = {
            slideInHorizontally(
                initialOffsetX = { fullWidth -> -fullWidth / 4 },
                animationSpec = tween(350)
            ) + fadeIn(animationSpec = tween(350))
        },
        popExitTransition = {
            slideOutHorizontally(
                targetOffsetX = { fullWidth -> fullWidth },
                animationSpec = tween(350)
            ) + fadeOut(animationSpec = tween(350))
        }
    ) {
        navigation(
            route = "auth_graph",
            startDestination = "login"
        ) {
            composable(
                route = "login",
                enterTransition = { slideInHorizontally(initialOffsetX = { -1000 }) + fadeIn() },
                exitTransition = { slideOutHorizontally(targetOffsetX = { -1000 }) + fadeOut() }
            ) {
                LoginScreen(
                    onLoginSuccess = {

                        navController.navigate("main_screen") {
                            popUpTo("auth_graph") { inclusive = true }
                        }
                    },
                    onSignUpClick = { navController.navigate("signup") },
                    onForgotClick = { navController.navigate("forgot_password") }
                )
            }

            composable(
                route = "signup",
                enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
                exitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }
            ) {
                SignUpScreen(
                    onLoginClick = { navController.popBackStack() },
                    onRegisterSucces = {
                        navController.popBackStack("login", inclusive = false)
                    }
                )
            }

            composable(
                route = "forgot_password",
                enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
                exitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }
            ) {
                ForgotPasswordScreen(
                    onBackClick = { navController.popBackStack() },
                    onSendCodeClick = { email -> navController.navigate("verify_email/$email") }
                )
            }

            composable(
                route = "verify_email/{email}",
                enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
                exitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }
            ) {
                val email = it.arguments?.getString("email") ?: ""
                VerifyEmailScreen(
                    email = email,
                    onBackClick = { navController.popBackStack() },
                    onVerifyClick = { otp -> navController.navigate("reset_password/$email/$otp") }
                )
            }

            composable(
                route = "reset_password/{email}/{otp}",
                enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
                exitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }
            ) {
                val email = it.arguments?.getString("email") ?: ""
                val otp = it.arguments?.getString("otp") ?: ""

                ResetPasswordScreen(
                    email = email,
                    otp = otp,
                    onBackClick = { navController.popBackStack() },
                    onUpdateClick = {
                        navController.navigate("login") {
                            popUpTo("auth_graph") { inclusive = true }
                        }
                    }
                )
            }
        }


        composable(
            route = "main_screen",
            enterTransition = {
                scaleIn(initialScale = 0.8f, animationSpec = tween(300)) + fadeIn(
                    animationSpec = tween(300)
                )
            },
            exitTransition = { fadeOut() }
        ) {
            MainScreen(
                targetTab = targetMainTab,
                onTabHandled = { targetMainTab = null },
                onNavigateToDetail = { productId ->
                    navController.navigate("product_detail/$productId")
                },
                onNavigateToAddress = {
                    navController.navigate("address_selection") {
                        launchSingleTop = true
                    }
                },
                onNavigateToDeliveryAddress = {
                    navController.navigate("address_selection?source=home") {
                        launchSingleTop = true
                    }
                },
                onNavigateToCreateProduct = {
                    navController.navigate("create_product")
                },
                onNavigateToCheckout = { selectedIds ->
                    navController.navigate("checkout/$selectedIds")
                },
                onLogoutSuccess = {
                },
                onNavigateToBuyerOrders = { status ->
                    navController.navigate("buyer_orders/$status")
                },
                onNavigateToChat = {
                    navController.navigate("chat_list")
                },
                onNavigateToNotifications = {
                    navController.navigate("notifications") { launchSingleTop = true }
                },
                onNavigateToBargains = { navController.navigate("bargains") },
                onNavigateToReports = {
                    navController.navigate("my_reports") {
                        launchSingleTop = true
                    }
                },
                onNavigateToStatistics = {
                    navController.navigate("statistics") {
                        launchSingleTop = true
                    }
                },
                onNavigateToAdminReports = {
                    navController.navigate("admin_reports") {
                        launchSingleTop = true
                    }
                },
                onNavigateToMyProducts = {
                    navController.navigate("my_products") {
                        launchSingleTop = true
                    }
                },
                onNavigateToPickupLocation = {
                    navController.navigate("seller_pickup") {
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = "product_detail/{productId}",
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""

            ProductDetailScreen(
                productId = productId,
                onBargainClick = { navController.navigate("create_bargain/${Uri.encode(productId)}") },
                onBackClick = { navController.popBackStack() },
                onReportClick = { reportProductId, reportProductName ->
                    navController.navigate(
                        "report_product/${Uri.encode(reportProductId)}" +
                                "?productName=${Uri.encode(reportProductName)}"
                    )
                },
                onViewShopClick = { sellerId, sellerName, sellerAvatar, sellerPhone ->
                    val encodedName = Uri.encode(sellerName)
                    val encodedAvatar = Uri.encode(sellerAvatar ?: "")
                    val phone = sellerPhone ?: ""
                    navController.navigate("seller_store/$sellerId?sellerName=$encodedName&sellerAvatar=$encodedAvatar&sellerPhone=$phone")
                },
                onChatClick = { sellerId, sellerName, sellerAvatar, sellerPhone, pId, pName, pPrice, pImage ->
                    val myUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val roomId = com.example.agritech_mobile.data.repository.ChatRepository.generateRoomId(myUserId, sellerId)

                    navController.navigate(
                        "chat/$roomId/$sellerId/${Uri.encode(sellerName)}?" +
                                "partnerAvatar=${Uri.encode(sellerAvatar ?: "")}&" +
                                "partnerPhone=${sellerPhone ?: ""}&" +
                                "productId=$pId&" +
                                "productName=${Uri.encode(pName)}&" +
                                "productPrice=$pPrice&" +
                                "productImage=${Uri.encode(pImage ?: "")}"
                    )
                },
                onAiChatClick = { pId, pName ->
                    val encodedName = Uri.encode(pName)
                    navController.navigate("ai_chat/$pId?productName=$encodedName")
                }
            )
        }

        composable(
            route = "ai_chat/{productId}?productName={productName}",
            arguments = listOf(
                navArgument("productId") { type = NavType.StringType },
                navArgument("productName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""
            val productName = backStackEntry.arguments?.getString("productName") ?: ""

            AiChatScreen(
                productId = productId,
                productName = productName,
                onBackClick = { navController.popBackStack() },
                onProductClick = { pId ->
                    navController.navigate("product_detail/$pId")
                }
            )
        }

        composable(
            route = "create_product",
            enterTransition = {
                slideInVertically(
                    initialOffsetY = { it },
                    animationSpec = tween(durationMillis = 400)
                ) + fadeIn(animationSpec = tween(400))
            },
            exitTransition = {
                slideOutVertically(
                    targetOffsetY = { it },
                    animationSpec = tween(durationMillis = 400)
                ) + fadeOut(animationSpec = tween(400))
            }
        ) {
            SellProductScreen(
                onBackClick = { navController.popBackStack() },
            )
        }

        composable(
            route = "checkout/{selectedIds}",
            arguments = listOf(
                navArgument("selectedIds") { type = NavType.StringType }
            ),
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { 1000 },
                    animationSpec = tween(400)
                ) + fadeIn()
            },
            exitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { -1000 },
                    animationSpec = tween(400)
                ) + fadeOut()
            },
            popEnterTransition = {
                slideInHorizontally(
                    initialOffsetX = { -1000 },
                    animationSpec = tween(400)
                ) + fadeIn()
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { 1000 },
                    animationSpec = tween(400)
                ) + fadeOut()
            }
        ) { backStackEntry ->
            val idsString = backStackEntry.arguments?.getString("selectedIds") ?: ""
            val selectedCartItemIds = idsString.split(",").filter { it.isNotEmpty() }

            val selectedAddressId by backStackEntry.savedStateHandle
                .getStateFlow<String?>("selectedAddressId", null).collectAsState()

            CheckoutScreen(
                selectedCartItemIds = selectedCartItemIds,
                selectedAddressId = selectedAddressId,
                onBackClick = { navController.popBackStack() },
                onPlaceOrderSuccess = {
                    navController.popBackStack("main_screen", inclusive = false)
                },
                onNavigateToAddressSelection = { currentAddressId ->
                    navController.currentBackStackEntry?.savedStateHandle?.set(
                        "currentAddressId",
                        currentAddressId
                    )
                    navController.navigate("address_selection")
                },
                onNavigateToVnpay = { orderId, rawPaymentUrl ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("vnpay_raw_url", rawPaymentUrl)
                    navController.navigate("vnpay_payment/$orderId")
                }
            )
        }

        composable(
            route = "vnpay_payment/{orderId}",
            arguments = listOf(navArgument("orderId") { type = NavType.LongType })
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L

            val paymentUrl = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("vnpay_raw_url") ?: ""

            val orderViewModel: OrderViewModel = hiltViewModel()

            VnpayPaymentScreen(
                paymentUrl = paymentUrl,
                onPaymentSuccess = { params ->
                    orderViewModel.confirmOrderPaid(orderId, params, onError = { message ->
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                        navController.navigate("buyer_orders/ALL") { popUpTo("main_screen") { inclusive = false } }
                    }) {
                        navController.navigate("buyer_orders/PENDING") {
                            popUpTo("main_screen") { inclusive = false }
                        }
                    }
                },
                onPaymentFailed = {
                    navController.navigate("buyer_orders/ALL") {
                        popUpTo("main_screen") { inclusive = false }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "address_selection?source={source}",
            arguments = listOf(navArgument("source") {
                type = NavType.StringType
                defaultValue = "checkout"
            })
        ) { backStackEntry ->
            val addressViewModel: UserViewModel = hiltViewModel()
            val homeAddressId by addressViewModel.homeDeliveryAddressId.collectAsState()
            val fromHome = backStackEntry.arguments?.getString("source") == "home"
            val currentAddressId = if (fromHome) homeAddressId else navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("currentAddressId")
            AddressSelectionScreen(
                viewModel = addressViewModel,
                currentSelectedAddressId = currentAddressId,
                onBackClick = { navController.popBackStack() },
                onAddNewClick = { navController.navigate("add_address") },
                onEditClick = { address ->
                    navController.navigate("add_address?addressId=${Uri.encode(address.id)}")
                },
                onConfirmClick = { selectedAddress ->
                    if (fromHome) {
                        addressViewModel.selectHomeDeliveryAddress(selectedAddress)
                    } else {
                        navController.previousBackStackEntry
                            ?.savedStateHandle
                            ?.set("selectedAddressId", selectedAddress.id)
                    }

                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "add_address?addressId={addressId}",
            arguments = listOf(navArgument("addressId") {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            })
        ) { backStackEntry ->
            val pickedProvince = backStackEntry.savedStateHandle
                .getStateFlow<String?>("picked_province", null)
                .collectAsState()

            val pickedWard = backStackEntry.savedStateHandle
                .getStateFlow<String?>("picked_ward", null)
                .collectAsState()

            val pickedDetail = backStackEntry.savedStateHandle
                .getStateFlow<String?>("picked_detail", null)
                .collectAsState()

            val pickedLatitude = backStackEntry.savedStateHandle
                .getStateFlow<Double?>("picked_latitude", null)
                .collectAsState()

            val pickedLongitude = backStackEntry.savedStateHandle
                .getStateFlow<Double?>("picked_longitude", null)
                .collectAsState()

            val pickedSelectionId = backStackEntry.savedStateHandle
                .getStateFlow<String?>("picked_selection_id", null)
                .collectAsState()

            AddAddressScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() },
                onPickMapClick = { addressQuery, areaQuery ->
                    backStackEntry.savedStateHandle.apply {
                        set("map_initial_address", addressQuery)
                        set("map_initial_area", areaQuery)
                    }

                    navController.navigate("map_picker")
                },
                pickedProvince = pickedProvince.value,
                pickedWard = pickedWard.value,
                pickedDetail = pickedDetail.value,
                pickedLatitude = pickedLatitude.value,
                pickedLongitude = pickedLongitude.value,
                pickedSelectionId = pickedSelectionId.value,
                addressId = backStackEntry.arguments?.getString("addressId")
            )
        }

        composable(
            route = "buyer_orders/{initialStatus}",
            arguments = listOf(navArgument("initialStatus") { type = NavType.StringType })
        ) { backStackEntry ->
            val initialStatus = backStackEntry.arguments?.getString("initialStatus") ?: "ALL"

            BuyerOrdersScreen(
                initialStatus = initialStatus,
                onBackClick = { navController.popBackStack() },
                onReviewClick = { productId, productName, productImageUrl, shopName ->
                    navController.currentBackStackEntry?.savedStateHandle?.apply {
                        set("productName", productName)
                        set("productImageUrl", productImageUrl)
                        set("shopName", shopName)
                    }
                    navController.navigate("review_screen/$productId")
                }
            )
        }

        composable(
            route = "review_screen/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId") ?: ""

            val previousBackStack = navController.previousBackStackEntry
            val productName = previousBackStack?.savedStateHandle?.get<String>("productName") ?: ""
            val productImageUrl = previousBackStack?.savedStateHandle?.get<String>("productImageUrl") ?: ""
            val shopName = previousBackStack?.savedStateHandle?.get<String>("shopName") ?: ""

            ReviewProductScreen(
                productID = productId,
                productName = productName,
                productImageUrl = productImageUrl,
                shopName = shopName,
                onBackClick = { navController.popBackStack() },
                onReviewSuccess = { navController.popBackStack() }
            )
        }

        composable(
            route = "chat/{roomId}/{partnerId}/{partnerName}?partnerAvatar={partnerAvatar}&partnerPhone={partnerPhone}&productId={productId}&productName={productName}&productPrice={productPrice}&productImage={productImage}",
            arguments = listOf(
                navArgument("roomId") { type = NavType.StringType },
                navArgument("partnerId") { type = NavType.StringType },
                navArgument("partnerName") { type = NavType.StringType },
                navArgument("partnerAvatar") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("partnerPhone") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("productId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("productName") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("productPrice") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("productImage") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val roomId = backStackEntry.arguments?.getString("roomId") ?: ""
            val partnerId = backStackEntry.arguments?.getString("partnerId") ?: ""
            val partnerName = backStackEntry.arguments?.getString("partnerName") ?: ""
            val partnerAvatar = backStackEntry.arguments?.getString("partnerAvatar")
            val partnerPhone = backStackEntry.arguments?.getString("partnerPhone")

            val productId = backStackEntry.arguments?.getString("productId")
            val productName = backStackEntry.arguments?.getString("productName")
            val productPrice = backStackEntry.arguments?.getString("productPrice")?.toDoubleOrNull()
            val productImage = backStackEntry.arguments?.getString("productImage")

            ChatScreen(
                roomId = roomId,
                onNavigateToBargains = { navController.navigate("bargains") },
                partnerId = partnerId,
                partnerName = partnerName,
                partnerAvatar = partnerAvatar,
                partnerPhone = partnerPhone,
                initialProductId = productId,
                initialProductName = productName,
                initialProductPrice = productPrice,
                initialProductImage = productImage,
                onBack = { navController.popBackStack() },
                onNavigateToProductDetail = { id ->
                    navController.navigate("product_detail/$id")
                }
            )
        }

        composable("map_picker") {
            MapPickerScreen(
                onAddressSelected = { parsedAddress, latLng ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.apply {
                            set("picked_full_address", parsedAddress.fullAddress)
                            set("picked_province", parsedAddress.province)
                            set("picked_ward", parsedAddress.ward)
                            set("picked_detail", parsedAddress.detail)
                            set("picked_latitude", latLng.latitude)
                            set("picked_longitude", latLng.longitude)
                            set(
                                "picked_selection_id",
                                java.util.UUID.randomUUID().toString()
                            )
                        }

                    navController.popBackStack()
                },
                initialAddress = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("map_initial_address")
                    .orEmpty(),

                initialArea = navController.previousBackStackEntry
                    ?.savedStateHandle
                    ?.get<String>("map_initial_area")
                    .orEmpty(),
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable("chat_list") {
            ChatListScreen(
                onBackClick = { navController.popBackStack() },
                onRoomClick = { roomId, partnerId, partnerName, partnerAvatar ->
                    navController.navigate(
                        "chat/$roomId/$partnerId/${Uri.encode(partnerName)}?partnerAvatar=${Uri.encode(partnerAvatar ?: "")}"
                    )
                }
            )
        }

        composable(
            route = "vnpay_payment/{orderId}/{paymentUrl}",
            arguments = listOf(
                navArgument("orderId") { type = NavType.LongType },
                navArgument("paymentUrl") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L
            val rawUrl = backStackEntry.arguments?.getString("paymentUrl") ?: ""
            val decodedUrl = URLDecoder.decode(rawUrl, StandardCharsets.UTF_8.toString())

            val orderViewModel: OrderViewModel = hiltViewModel()

            VnpayPaymentScreen(
                paymentUrl = decodedUrl,
                onPaymentSuccess = { params ->
                    orderViewModel.confirmOrderPaid(orderId, params, onError = { message ->
                        android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_LONG).show()
                        navController.navigate("buyer_orders/ALL") { popUpTo("main_screen") { inclusive = false } }
                    }) {
                        navController.navigate("buyer_orders/ALL") {
                            popUpTo("main_screen") { inclusive = false }
                        }
                    }
                },
                onPaymentFailed = {
                    navController.navigate("buyer_orders/ALL") {
                        popUpTo("main_screen") { inclusive = false }
                    }
                },
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(
            route = "seller_store/{sellerId}?sellerName={sellerName}&sellerAvatar={sellerAvatar}&sellerPhone={sellerPhone}",
            arguments = listOf(
                navArgument("sellerId") { type = NavType.StringType },
                navArgument("sellerName") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("sellerAvatar") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("sellerPhone") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            )
        ) { backStackEntry ->
            val sellerId = backStackEntry.arguments?.getString("sellerId") ?: ""
            val sellerName = backStackEntry.arguments?.getString("sellerName") ?: ""
            val sellerAvatar = backStackEntry.arguments?.getString("sellerAvatar")
            val sellerPhone = backStackEntry.arguments?.getString("sellerPhone")

            SellerStoreScreen(
                sellerId = sellerId,
                sellerName = sellerName,
                sellerAvatar = sellerAvatar,
                sellerPhone = sellerPhone,
                onBackClick = { navController.popBackStack() },
                onProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                },
                onChatClick = {
                    val myUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                    val roomId = com.example.agritech_mobile.data.repository.ChatRepository.generateRoomId(myUserId, sellerId)

                    navController.navigate(
                        "chat/$roomId/$sellerId/${Uri.encode(sellerName)}?" +
                                "partnerAvatar=${Uri.encode(sellerAvatar ?: "")}&" +
                                "partnerPhone=${sellerPhone ?: ""}"
                    )
                }
            )
        }

        composable(
            route = "report_product/{productId}?productName={productName}",
            arguments = listOf(
                navArgument("productId") {
                    type = NavType.StringType
                },
                navArgument("productName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val reportProductId = backStackEntry.arguments
                ?.getString("productId")
                .orEmpty()

            val reportProductName = backStackEntry.arguments
                ?.getString("productName")
                .orEmpty()

            ReportScreen(
                productId = reportProductId,
                productName = reportProductName,
                onBackClick = {
                    navController.popBackStack()
                },
                onReportSuccess = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = "my_reports") {
            ReportHistoryScreen(
                onBackClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(route = "admin_reports") {
            val accessViewModel: UserViewModel = hiltViewModel()
            val isAdmin by accessViewModel.isAdmin.collectAsStateWithLifecycle()

            if (isLoggedIn && isAdmin) {
                AdminReportScreen(
                    onBackClick = {
                        navController.popBackStack()
                    }
                )
            } else {
                LaunchedEffect(isLoggedIn, isAdmin) {
                    if (isLoggedIn && !isAdmin) {
                        val returned = navController.popBackStack()

                        if (!returned) {
                            navController.navigate("main_screen") {
                                popUpTo("admin_reports") {
                                    inclusive = true
                                }
                                launchSingleTop = true
                            }
                        }
                    }
                }
            }
        }

        composable(route = "statistics") {
            StatisticsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onEditProduct = { productId ->
                    navController.navigate("edit_product/${Uri.encode(productId)}") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable("bargains") {
            com.example.agritech_mobile.ui.bargain.BargainScreen(
                onBack = { navController.popBackStack() },
                onAddresses = { navController.navigate("address_selection?source=home") },
                onOrders = { navController.navigate("buyer_orders/ALL") })
        }
        composable("create_bargain/{productId}") { entry ->
            com.example.agritech_mobile.ui.bargain.CreateBargainScreen(
                productId = entry.arguments?.getString("productId").orEmpty(),
                onBack = { navController.popBackStack() },
                onCreated = { navController.popBackStack(); navController.navigate("bargains") })
        }

        composable("notifications") {
            NotificationScreen(
                onBackClick = { navController.popBackStack() },
                onOpenNotification = { notification ->
                    if (notification.targetScreen == "bargains") {
                        navController.navigate("bargains") { launchSingleTop = true }
                    } else if (notification.targetScreen == "my_reports") {
                        navController.navigate("my_reports") { launchSingleTop = true }
                    } else if (notification.targetScreen == "seller_orders") {
                        targetMainTab = 1
                        navController.popBackStack("main_screen", inclusive = false)
                    } else {
                        navController.navigate("buyer_orders/ALL") { launchSingleTop = true }
                    }
                }
            )
        }

        composable(route = "my_products") {
            MyProductsScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onProductClick = { productId ->
                    navController.navigate("product_detail/$productId")
                },
                onEditProduct = { productId ->
                    navController.navigate("edit_product/${Uri.encode(productId)}")
                }
            )
        }

        composable(
            route = "edit_product/{productId}",
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) {
            EditProductScreen(onBackClick = { navController.popBackStack() })
        }

        composable("seller_pickup") { backStackEntry ->
            val pickedAddress = backStackEntry.savedStateHandle
                .getStateFlow<String?>("picked_full_address", null)
                .collectAsState()

            val pickedLatitude = backStackEntry.savedStateHandle
                .getStateFlow<Double?>("picked_latitude", null)
                .collectAsState()

            val pickedLongitude = backStackEntry.savedStateHandle
                .getStateFlow<Double?>("picked_longitude", null)
                .collectAsState()

            val pickedSelectionId = backStackEntry.savedStateHandle
                .getStateFlow<String?>("picked_selection_id", null)
                .collectAsState()

            val pickedProvince = backStackEntry.savedStateHandle
                .getStateFlow<String?>("picked_province", null)
                .collectAsState()

            val pickedWard = backStackEntry.savedStateHandle
                .getStateFlow<String?>("picked_ward", null)
                .collectAsState()

            val pickedDetail = backStackEntry.savedStateHandle
                .getStateFlow<String?>("picked_detail", null)
                .collectAsState()

            SellerPickupScreen(
                onBackClick = {
                    navController.popBackStack()
                },
                onSaveSuccess = {
                    navController.popBackStack()
                },
                onPickMapClick = { addressQuery, areaQuery ->
                    backStackEntry.savedStateHandle.apply {
                        set("map_initial_address", addressQuery)
                        set("map_initial_area", areaQuery)
                    }

                    navController.navigate("map_picker")
                },
                pickedProvince = pickedProvince.value,
                pickedWard = pickedWard.value,
                pickedDetail = pickedDetail.value,
                pickedAddress = pickedAddress.value,
                pickedLatitude = pickedLatitude.value,
                pickedLongitude = pickedLongitude.value,
                pickedSelectionId = pickedSelectionId.value
            )
        }
    }
}
