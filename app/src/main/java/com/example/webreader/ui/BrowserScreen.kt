package com.example.webreader.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import org.json.JSONTokener
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.clickable

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel,
    onOpenSettings: () -> Unit
) {
    val currentUrl by viewModel.url.collectAsState()
    val isLoadingPage by viewModel.isLoading.collectAsState()
    val canGoBack by viewModel.canGoBack.collectAsState()
    val canGoForward by viewModel.canGoForward.collectAsState()
    val showReaderSheet by viewModel.showReaderSheet.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var urlInputText by remember { mutableStateOf(currentUrl) }
    var pageLoadProgress by remember { mutableFloatStateOf(0f) }
    val focusManager = LocalFocusManager.current

    // Đồng bộ ô nhập địa chỉ khi URL thay đổi (ví dụ: nhấn link trong WebView)
    LaunchedEffect(currentUrl) {
        if (currentUrl != urlInputText) {
            urlInputText = currentUrl
        }
    }

    // Xử lý nút Back của hệ thống
    BackHandler(enabled = canGoBack || showReaderSheet) {
        if (showReaderSheet) {
            viewModel.setShowReaderSheet(false)
        } else {
            webViewInstance?.goBack()
        }
    }

    Scaffold(
        topBar = {
            Column {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút Back
                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = canGoBack
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }

                    // Nút Forward
                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Tiến tới")
                    }

                    // Ô nhập URL thiết kế bo góc, mờ
                    OutlinedTextField(
                        value = urlInputText,
                        onValueChange = { urlInputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = {
                                focusManager.clearFocus()
                                var formattedUrl = urlInputText.trim()
                                if (!formattedUrl.startsWith("http://") && !formattedUrl.startsWith("https://")) {
                                    formattedUrl = "https://$formattedUrl"
                                }
                                webViewInstance?.loadUrl(formattedUrl)
                            }
                        ),
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    // Nút Refresh
                    IconButton(onClick = { webViewInstance?.reload() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Tải lại")
                    }

                    // Nút Cài đặt
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Cài đặt")
                    }
                }

                // Thanh tiến trình tải trang web
                if (isLoadingPage) {
                    LinearProgressIndicator(
                        progress = { pageLoadProgress },
                        modifier = Modifier.fillMaxWidth().height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // WebView Android trượt mượt mà
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            databaseEnabled = true
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                viewModel.setIsLoading(true)
                                url?.let { viewModel.setUrl(it) }
                            }

                            override fun onPageFinished(view: WebView?, url: String?) {
                                viewModel.setIsLoading(false)
                                url?.let { viewModel.setUrl(it) }
                                viewModel.setNavigationState(
                                    canGoBack = view?.canGoBack() ?: false,
                                    canGoForward = view?.canGoForward() ?: false
                                )
                                view?.title?.let { viewModel.setTitle(it) }
                            }

                            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                                return false // Tiếp tục mở link trong chính WebView
                            }
                        }

                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                                pageLoadProgress = newProgress / 100f
                            }

                            override fun onReceivedTitle(view: WebView?, title: String?) {
                                title?.let { viewModel.setTitle(it) }
                            }
                        }

                        loadUrl(currentUrl)
                        webViewInstance = this
                    }
                },
                update = {
                    // Cập nhật cấu hình WebView nếu cần
                },
                modifier = Modifier.fillMaxSize()
            )

            // Nút nổi trích xuất văn bản & Dịch sang Tiếng Việt (FAB)
            if (!showReaderSheet) {
                FloatingActionButton(
                    onClick = {
                        webViewInstance?.let { webView ->
                            // Script trích xuất nội dung văn bản chất lượng cao từ DOM
                            val jsExtractor = """
                                (function() {
                                    var elements = document.querySelectorAll('h1, h2, h3, p, article');
                                    var textList = [];
                                    var seen = new Set();
                                    elements.forEach(function(el) {
                                        var text = el.innerText.trim();
                                        if (text.length > 25 && !seen.has(text)) {
                                            textList.push(text);
                                            seen.add(text);
                                        }
                                    });
                                    if (textList.length < 3) {
                                        return document.body.innerText;
                                    }
                                    return textList.join('\n\n');
                                })()
                            """.trimIndent()

                            webView.evaluateJavascript(jsExtractor) { result ->
                                if (result != null && result != "null" && result != "\"\"") {
                                    val cleanText = try {
                                        JSONTokener(result).nextValue() as String
                                    } catch (e: Exception) {
                                        // Giải mã thủ công nếu JSONTokener thất bại
                                        result.replace("\"", "").replace("\\n", "\n")
                                    }
                                    viewModel.translateWebpage(cleanText)
                                } else {
                                    viewModel.translateWebpage("")
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Filled.Translate,
                        contentDescription = "Dịch & Đọc",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Reader Sheet kéo lên từ dưới (Có hoạt ảnh trượt)
            AnimatedVisibility(
                visible = showReaderSheet,
                enter = slideInVertically(
                    initialOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(350)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { fullHeight -> fullHeight },
                    animationSpec = tween(300)
                ),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                // Thêm một lớp phủ mờ phía sau Reader Sheet để tăng tính premium
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f))
                        .clickable { viewModel.setShowReaderSheet(false) }
                ) {
                    ReaderSheet(
                        viewModel = viewModel,
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .clickable(enabled = false) {} // Không tắt sheet khi nhấn trong sheet
                    )
                }
            }
        }
    }
}
