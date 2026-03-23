import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import com.example.agritech_mobile.ui.auth.ForgotPasswordScreen
import com.example.agritech_mobile.ui.auth.LoginScreen
import com.example.agritech_mobile.ui.auth.ResetPasswordScreen
import com.example.agritech_mobile.ui.auth.SignUpScreen
import com.example.agritech_mobile.ui.auth.VerifyEmailScreen
import com.example.agritech_mobile.ui.dashboard.HomeScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "auth_graph"
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
                    onSendCodeClick = { navController.navigate("verify_email") }
                )
            }

            composable(
                route = "verify_email",
                enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
                exitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }
            ) {
                VerifyEmailScreen(
                    onBackClick = { navController.popBackStack() },
                    onVerifyClick = { navController.navigate("reset_password") }
                )
            }

            composable(
                route = "reset_password",
                enterTransition = { slideInHorizontally(initialOffsetX = { 1000 }) + fadeIn() },
                exitTransition = { slideOutHorizontally(targetOffsetX = { 1000 }) + fadeOut() }
            ) {
                ResetPasswordScreen (
                    onBackClick = { navController.popBackStack() },
                    onUpdateClick = { navController.navigate("login") }
                )
            }
        }


        composable(
            route = "main_screen",
            enterTransition = {
                scaleIn(initialScale = 0.8f, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
            },
            exitTransition = { fadeOut() }
        ) {
            HomeScreen()
        }
    }
}