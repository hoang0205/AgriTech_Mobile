package com.example.agritech_mobile

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.firebase.auth.FirebaseAuth
import com.example.agritech_mobile.data.local.TokenManager
import com.example.agritech_mobile.data.repository.AuthRepository
import com.example.agritech_mobile.ui.AppNavigation
import com.example.agritech_mobile.ui.theme.AgritechTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var tokenManager: TokenManager

    @Inject
    lateinit var authRepository: AuthRepository

    private var pendingChatRoomId by mutableStateOf<String?>(null)

    private var pendingTargetScreen by mutableStateOf<String?>(null)
    private var pendingOrderStatus by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
        super.onCreate(savedInstanceState)

        extractNotificationExtras(intent)

        val accessToken = tokenManager.getAccessToken()
        val startDestination = if (!accessToken.isNullOrEmpty()) {
            lifecycleScope.launch { authRepository.registerFcmToken() }
            "main_screen"
        } else {
            "auth_graph"
        }

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            val isLoggedIn by tokenManager.isLoggedIn.collectAsStateWithLifecycle()
            LaunchedEffect(isLoggedIn) {
                if (!isLoggedIn) {
                    FirebaseAuth.getInstance().signOut()
                    pendingChatRoomId = null
                    pendingTargetScreen = null
                    pendingOrderStatus = null
                }
            }
            AgritechTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        startRoute = startDestination,
                        isLoggedIn = isLoggedIn,
                        pendingChatRoomId = pendingChatRoomId,
                        onChatNavigated = { pendingChatRoomId = null },
                        pendingTargetScreen = pendingTargetScreen,
                        pendingOrderStatus = pendingOrderStatus,
                        onOrderNavigated = {
                            pendingTargetScreen = null
                            pendingOrderStatus = null
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractNotificationExtras(intent)
    }

    private fun extractNotificationExtras(intent: Intent?) {
        pendingChatRoomId = intent?.getStringExtra("chatRoomId")
        pendingTargetScreen = intent?.getStringExtra("targetScreen")
        pendingOrderStatus = intent?.getStringExtra("orderStatus")
    }
}
