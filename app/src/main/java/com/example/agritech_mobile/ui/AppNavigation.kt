import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
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
import com.example.agritech_mobile.ui.checkout.CheckoutScreen
import com.example.agritech_mobile.ui.dashboard.HomeScreen
import com.example.agritech_mobile.ui.dashboard.ProductDetailScreen
import com.example.agritech_mobile.ui.dashboard.SellProductScreen
import com.example.agritech_mobile.ui.main.MainScreen

@Composable
fun AppNavigation(
    startRoute: String
) {
    val navController = rememberNavController()

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
                scaleIn(
                    initialScale = 0.8f,
                    animationSpec = tween(300)
                ) + fadeIn(animationSpec = tween(300))
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

            CheckoutScreen(
                selectedCartItemIds = selectedCartItemIds,
                onBackClick = { navController.popBackStack() },
                onPlaceOrderSuccess = {
                    navController.popBackStack("main_screen", inclusive = false)
                }
            )
        }
    }
}