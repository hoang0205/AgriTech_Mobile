package com.example.agritech_mobile.ui.order

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.webkit.*
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView

@OptIn(ExperimentalMaterial3Api::class)
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
    var progress by remember { mutableIntStateOf(0) }
    var currentWebUrl by remember { mutableStateOf(paymentUrl) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
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
                actions = {
                    // Nút mở bằng trình duyệt ngoài nếu muốn test trực tiếp trên Chrome
                    IconButton(onClick = {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(currentWebUrl))
                        context.startActivity(browserIntent)
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Mở bằng trình duyệt",
                            tint = Color.DarkGray
                        )
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
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        // 1. Cho phép Cookies & Session
                        val cookieManager = CookieManager.getInstance()
                        cookieManager.setAcceptCookie(true)
                        cookieManager.setAcceptThirdPartyCookies(this, true)

                        // 2. Cấu hình chống lệch tọa độ cảm ứng
                        isClickable = true
                        isFocusable = true
                        isFocusableInTouchMode = true

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            javaScriptCanOpenWindowsAutomatically = true
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                            // TẮT ZOOM ĐỂ TRÁNH LỆCH VỊ TRÍ NÚT
                            useWideViewPort = false
                            loadWithOverviewMode = false
                            setSupportZoom(false)
                            builtInZoomControls = false
                            displayZoomControls = false
                        }

                        // 3. Xử lý hộp thoại confirm khi bấm nút "Hủy / Thoát"
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                progress = newProgress
                            }

                            override fun onJsConfirm(
                                view: WebView?,
                                url: String?,
                                message: String?,
                                result: JsResult?
                            ): Boolean {
                                AlertDialog.Builder(ctx)
                                    .setTitle("Xác nhận")
                                    .setMessage(message)
                                    .setPositiveButton("Đồng ý") { _, _ -> result?.confirm() }
                                    .setNegativeButton("Không") { _, _ -> result?.cancel() }
                                    .setOnCancelListener { result?.cancel() }
                                    .show()
                                return true
                            }

                            override fun onJsAlert(
                                view: WebView?,
                                url: String?,
                                message: String?,
                                result: JsResult?
                            ): Boolean {
                                AlertDialog.Builder(ctx)
                                    .setTitle("Thông báo")
                                    .setMessage(message)
                                    .setPositiveButton("OK") { _, _ -> result?.confirm() }
                                    .show()
                                return true
                            }
                        }

                        // 4. Bắt URL kết quả thanh toán
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                currentWebUrl = url ?: paymentUrl
                            }

                            override fun onReceivedSslError(
                                view: WebView?,
                                handler: SslErrorHandler?,
                                error: SslError?
                            ) {
                                handler?.proceed()
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?,
                                request: WebResourceRequest?
                            ): Boolean {
                                val url = request?.url?.toString() ?: ""
                                currentWebUrl = url

                                if (url.contains("payment-result")) {
                                    val uri = Uri.parse(url)
                                    val responseCode = uri.getQueryParameter("vnp_ResponseCode")

                                    if (responseCode == "00") {
                                        Toast.makeText(context, "Thanh toán thành công!", Toast.LENGTH_SHORT).show()
                                        onPaymentSuccess()
                                    } else {
                                        val message = when (responseCode) {
                                            "24" -> "Bạn đã hủy giao dịch thanh toán"
                                            "51" -> "Tài khoản không đủ số dư"
                                            else -> "Giao dịch không thành công (Mã: $responseCode)"
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

            // Thanh tiến trình chạy ở cạnh trên, không bao giờ che cảm ứng
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.TopCenter),
                    color = Color(0xFF1B5E20),
                    trackColor = Color.Transparent
                )
            }
        }
    }
}