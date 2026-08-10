package com.punishdave.homeapps

import android.graphics.Bitmap
import android.view.ViewGroup
import android.webkit.HttpAuthHandler
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

@Composable
fun WebAppScreen(
    title: String,
    url: String,
    username: String = "",
    password: String = "",
    connectionLabel: String? = null,
    onBack: () -> Unit
) {
    var webView by androidx.compose.runtime.remember { mutableStateOf<WebView?>(null) }
    var loading by androidx.compose.runtime.remember { mutableStateOf(true) }
    var loadedRootUrl by androidx.compose.runtime.remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val currentUsername by androidx.compose.runtime.rememberUpdatedState(username)
    val currentPassword by androidx.compose.runtime.rememberUpdatedState(password)

    BackHandler {
        val view = webView
        if (view?.canGoBack() == true) view.goBack() else onBack()
    }

    Scaffold(
        topBar = {
            Surface(color = Color(0xFF0F0F0F), shadowElevation = 4.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .height(56.dp)
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onBack) { Text("Back") }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = listOfNotNull(connectionLabel, url).joinToString(" · "),
                            color = Color(0xFF8D8D8D),
                            fontSize = 10.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val refreshLayout = SwipeRefreshLayout(context)
                    val browser = WebView(context).apply {
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                loading = true
                                errorMessage = null
                            }

                            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                                if (request?.isForMainFrame == true) {
                                    loading = false
                                    errorMessage = error?.description?.toString()?.ifBlank { null } ?: "This service is unavailable"
                                    (view?.parent as? SwipeRefreshLayout)?.isRefreshing = false
                                }
                            }

                            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, response: WebResourceResponse?) {
                                if (request?.isForMainFrame == true && (response?.statusCode ?: 0) >= 400) {
                                    loading = false
                                    errorMessage = "The service returned HTTP ${response?.statusCode}"
                                    (view?.parent as? SwipeRefreshLayout)?.isRefreshing = false
                                }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                loading = false
                                (view?.parent as? SwipeRefreshLayout)?.isRefreshing = false
                            }

                            override fun onReceivedHttpAuthRequest(
                                view: WebView?,
                                handler: HttpAuthHandler?,
                                host: String?,
                                realm: String?
                            ) {
                                if (currentUsername.isNotBlank() || currentPassword.isNotBlank()) {
                                    handler?.proceed(currentUsername, currentPassword)
                                } else {
                                    handler?.cancel()
                                }
                            }
                        }
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.cacheMode = android.webkit.WebSettings.LOAD_CACHE_ELSE_NETWORK
                        loadUrl(url)
                        loadedRootUrl = url
                        webView = this
                    }
                    refreshLayout.setColorSchemeColors(0xFFE66A64.toInt())
                    refreshLayout.setProgressBackgroundColorSchemeColor(0xFF222222.toInt())
                    refreshLayout.setOnChildScrollUpCallback { _, _ -> browser.canScrollVertically(-1) }
                    refreshLayout.setOnRefreshListener { errorMessage = null; browser.reload() }
                    refreshLayout.addView(
                        browser,
                        ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    )
                    refreshLayout
                },
                update = { refreshLayout ->
                    val browser = refreshLayout.getChildAt(0) as? WebView
                    webView = browser
                    if (browser != null && loadedRootUrl != url) {
                        loadedRootUrl = url
                        browser.loadUrl(url)
                    }
                }
            )
            if (loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = Color(0xFFE66A64),
                    trackColor = Color.Transparent
                )
            }
            errorMessage?.let { message ->
                Surface(
                    modifier = Modifier.align(Alignment.Center).padding(24.dp),
                    color = Color(0xFF242424),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Service unavailable", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(message, color = Color(0xFFBDBDBD), fontSize = 13.sp)
                        Button(
                            onClick = { errorMessage = null; webView?.reload() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE66A64))
                        ) { Text("Retry") }
                    }
                }
            }
        }
    }
}

