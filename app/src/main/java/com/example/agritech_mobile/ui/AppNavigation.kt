package com.example.agritech_mobile.ui

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
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
import com.example.agritech_mobile.ui.chat.ChatScreen
import com.example.agritech_mobile.ui.checkout.CheckoutScreen
import com.example.agritech_mobile.ui.dashboard.ProductDetailScreen
import com.example.agritech_mobile.ui.dashboard.SellProductScreen
import com.example.agritech_mobile.ui.main.MainScreen
import com.example.agritech_mobile.ui.order.AddressSelectionScreen
import com.example.agritech_mobile.ui.user.BuyerOrdersScreen
import com.example.agritech_mobile.ui.user.AddAddressScreen
import com.example.agritech_mobile.ui.user.ReviewProductScreen
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation(
    startRoute: String,
    pendingChatRoomId: String? = null,
    onChatNavigated: () -> Unit = {}
) {
    val navController = rememberNavController()

    LaunchedEffect(pendingChatRoomId) {
        if (!pendingChatRoomId.isNullOrBlank() && startRoute == "main_screen") {
            val myId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            val partnerId = pendingChatRoomId.split("_").firstOrNull { it != myId } ?: ""

            navController.navigate("chat/$pendingChatRoomId/$partnerId/${Uri.encode("Tin nhắn")}")
            onChatNavigated()
        }
    }

    NavHost(
        navController = navController,
        startDestination = startRoute
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
                onNavigateToDetail = { productId ->
                    navController.navigate("product_detail/$productId")
                },
                onNavigateToCreateProduct = {
                    navController.navigate("create_product")
                },
                onNavigateToCheckout = { selectedIds ->
                    navController.navigate("checkout/$selectedIds")
                },
                onLogoutSuccess = {
                    navController.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToBuyerOrders = { status ->
                    navController.navigate("buyer_orders/$status")
                }
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
                onBackClick = { navController.popBackStack() },
                productId = productId
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

            val selectedAddressId = backStackEntry.savedStateHandle.get<String>("selectedAddressId")

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
                }
            )
        }
        composable("address_selection") { backStackEntry ->
            val currentAddressId = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("currentAddressId")
            AddressSelectionScreen(
                currentSelectedAddressId = currentAddressId,
                onBackClick = { navController.popBackStack() },
                onAddNewClick = { navController.navigate("add_address") },
                onConfirmClick = { selectedAddress ->
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("selectedAddressId", selectedAddress.id)

                    navController.popBackStack()
                }
            )
        }

        composable("add_address") {
            AddAddressScreen(
                onBackClick = { navController.popBackStack() },
                onSaveSuccess = {
                    navController.popBackStack()
                }
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
    }
}