package com.example.agritech_mobile.ui.order

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.agritech_mobile.ui.theme.AgritechTheme


@SuppressLint("SetJavaScriptEnabled")
@Composable
fun VnpayPaymentScreen(
    paymentUrl: String,
    onPaymentSuccess: () -> Unit,
    onPaymentFailed: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(true) }

    VnpayPaymentContent(
        isLoading = isLoading,
        onBackClick = onBackClick,
        modifier = modifier,
        webViewContent = {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.loadWithOverviewMode = true
                        settings.useWideViewPort = true

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: ""

                                if (url.startsWith("agritech://payment-result")) {
                                    val uri = Uri.parse(url)
                                    val responseCode = uri.getQueryParameter("vnp_ResponseCode")

                                    if (responseCode == "00") {
                                        Toast.makeText(context, "Thanh toán thành công!", Toast.LENGTH_SHORT).show()
                                        onPaymentSuccess()
                                    } else {
                                        val message = when (responseCode) {
                                            "24" -> "Bạn đã hủy giao dịch thanh toán"
                                            "51" -> "Tài khoản không đủ số dư"
                                            else -> "Giao dịch không thành công (Mã lỗi: $responseCode)"
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                        onPaymentFailed(message)
                                    }
                                    return true
                                }
                                return false
                            }
                        }
                        loadUrl(paymentUrl)
                    }
                }
            )
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VnpayPaymentContent(
    isLoading: Boolean,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    webViewContent: @Composable BoxScope.() -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Cổng thanh toán VNPay",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color(0xFF1B5E20),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            webViewContent()

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.White.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = Color(0xFF1B5E20),
                            strokeWidth = 3.dp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Đang kết nối tới VNPay Sandbox...",
                            fontSize = 13.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}

@Preview(name = "1. Giao diện Cổng VNPay (Preview)", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun VnpayPaymentPreviewSuccess() {
    AgritechTheme {
        VnpayPaymentContent(
            isLoading = false,
            onBackClick = {},
            webViewContent = {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF4F6F8))
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "VNPAY SANDBOX GATEWAY",
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF005BAA),
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Đơn hàng: Thanh toan don hang AgriTech #101",
                                fontSize = 14.sp,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Số tiền: 250.000 VND",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFFD32F2F)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CreditCard, contentDescription = null, tint = Color(0xFF005BAA))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Thẻ ATM / Tài khoản ngân hàng (NCB)", fontWeight = FontWeight.SemiBold)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFAFAFA))
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Text("9704 1985 2619 1432 198", color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        )
    }
}

@Preview(name = "2. Trạng thái đang tải (Loading)", showBackground = true, device = "spec:width=411dp,height=891dp")
@Composable
private fun VnpayPaymentPreviewLoading() {
    AgritechTheme {
        VnpayPaymentContent(
            isLoading = true,
            onBackClick = {},
            webViewContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFF4F6F8))
                )
            }
        )
    }
}