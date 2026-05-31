package top.chengdongqing.wechat.feature.common

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import top.chengdongqing.wechat.core.designsystem.R
import top.chengdongqing.wechat.core.designsystem.components.topbar.WeTopBar
import top.chengdongqing.wechat.core.designsystem.theme.WeTheme
import top.chengdongqing.wechat.core.designsystem.theme.White

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    url: String,
    onBack: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var canGoBack by remember { mutableStateOf(false) }
    var webView: WebView? by remember { mutableStateOf(null) }

    // 处理物理返回键
    BackHandler(enabled = canGoBack) {
        webView?.goBack()
    }

    Scaffold(
        topBar = {
            WebViewTopBar(title, onBack)
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .background(White)
                .padding(innerPadding)
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            setSupportZoom(true)
                            builtInZoomControls = true
                            displayZoomControls = false
                            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                        }

                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(
                                view: WebView?, url: String?, favicon: Bitmap?
                            ) {
                                isLoading = true
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                isLoading = false
                                title = view?.title ?: ""
                                canGoBack = view?.canGoBack() ?: false
                            }

                            override fun shouldOverrideUrlLoading(
                                view: WebView?, request: WebResourceRequest?
                            ): Boolean {
                                // 返回 false 表示在当前 WebView 中加载
                                return false
                            }
                        }

                        loadUrl(url)
                    }
                },
                update = { view ->
                    webView = view
                    canGoBack = view.canGoBack()
                },
                modifier = Modifier.fillMaxSize()
            )

            // 加载进度条
            if (isLoading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = WeTheme.colorScheme.primary
                )
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            webView?.destroy()
        }
    }
}

@Composable
private fun WebViewTopBar(
    title: String,
    onBack: () -> Unit
) {
    WeTopBar(
        title = title.ifEmpty { "加载中..." },
        onBack = onBack,
        backIconResId = R.drawable.ic_close_outlined
    )
}