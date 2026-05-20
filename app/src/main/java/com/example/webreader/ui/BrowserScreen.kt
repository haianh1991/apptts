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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
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
    var showModeDialog by remember { mutableStateOf(false) }
    var extractedTextForQueue by remember { mutableStateOf("") }
    var capturedTitleForQueue by remember { mutableStateOf("") }
    var capturedUrlForQueue by remember { mutableStateOf("") }
    var isAddressBarFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    // Đồng bộ ô nhập địa chỉ khi URL thay đổi (ví dụ: nhấn link trong WebView)
    LaunchedEffect(currentUrl) {
        if (currentUrl != urlInputText) {
            urlInputText = currentUrl
        }
    }

    // Lắng nghe yêu cầu điều hướng từ ViewModel (ví dụ: khi nhấn vào bookmark)
    LaunchedEffect(Unit) {
        viewModel.navigationRequest.collect { targetUrl ->
            webViewInstance?.loadUrl(targetUrl)
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
            Column(modifier = Modifier.statusBarsPadding()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAddressBarFocused) {
                        // Nút Back để hủy focus và thoát chế độ nhập URL (tương tự Chrome)
                        IconButton(onClick = { focusManager.clearFocus() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Hủy nhập")
                        }
                    }

                    // Ô nhập URL thiết kế bo góc, mờ, tự động giãn rộng khi focus
                    OutlinedTextField(
                        value = urlInputText,
                        onValueChange = { urlInputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .onFocusChanged { focusState ->
                                isAddressBarFocused = focusState.isFocused
                            },
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
                        trailingIcon = if (isAddressBarFocused && urlInputText.isNotEmpty()) {
                            {
                                IconButton(onClick = { urlInputText = "" }) {
                                    Icon(Icons.Filled.Close, contentDescription = "Xóa")
                                }
                            }
                        } else {
                            null
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    )

                    if (!isAddressBarFocused) {
                        // Nút Đánh dấu trang (Star) - nằm bên phải, luôn xuất hiện khi không nhập URL
                        val isBookmarked by viewModel.isCurrentPageBookmarked.collectAsState()
                        IconButton(onClick = { viewModel.toggleBookmarkCurrentPage() }) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Đánh dấu trang",
                                tint = if (isBookmarked) {
                                    Color(0xFFFFC107) // Màu vàng Gold của ngôi sao đã đánh dấu
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                }
                            )
                        }

                        // Nút Menu 3 chấm dọc gom Refresh và Settings (tương tự Google Chrome)
                        var showMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showMenu = true }) {
                                Icon(Icons.Filled.MoreVert, contentDescription = "Menu tùy chọn")
                            }
                            DropdownMenu(
                                expanded = showMenu,
                                onDismissRequest = { showMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Tải lại trang") },
                                    onClick = {
                                        showMenu = false
                                        webViewInstance?.reload()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Refresh, contentDescription = "Tải lại")
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Cài đặt") },
                                    onClick = {
                                        showMenu = false
                                        onOpenSettings()
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Filled.Settings, contentDescription = "Cài đặt")
                                    }
                                )
                            }
                        }
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
                val queue by viewModel.queue.collectAsState()
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    val bookmarks by viewModel.bookmarks.collectAsState()
                    val paragraphs by viewModel.paragraphs.collectAsState()
                    if (queue.isNotEmpty() || bookmarks.isNotEmpty() || paragraphs.isNotEmpty()) {
                        FloatingActionButton(
                            onClick = {
                                viewModel.setShowReaderSheet(true)
                            },
                            modifier = Modifier.padding(bottom = 16.dp),
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        ) {
                            Icon(
                                imageVector = Icons.Filled.List,
                                contentDescription = "Xem thư viện",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    FloatingActionButton(
                        onClick = {
                            webViewInstance?.let { webView ->
                                // Script trích xuất nội dung văn bản thô chất lượng cao (Hybrid Approach)
                                val jsExtractor = """
                                    (function() {
                                        if (!document.body) return "";
                                        // 1. Nhân bản body để làm sạch mà không ảnh hưởng DOM hiển thị trên màn hình
                                        var temp = document.body.cloneNode(true);

                                        // 2. Loại bỏ các thẻ rác, quảng cáo, menu điều hướng để tiết kiệm token
                                        var trashSelectors = [
                                            'script', 'style', 'iframe', 'noscript', 'header', 'footer', 'nav',
                                            '.ads', '.advertisement', '#header', '#footer', '.footer', '.header',
                                            '.sidebar', '#sidebar', '.nav', '.navigation', '.menu', '#menu',
                                            '.banner', '.popup', '.comment-list', '#comments', '.toolbar-box', '.toolbar'
                                        ];
                                        trashSelectors.forEach(function(sel) {
                                            var elements = temp.querySelectorAll(sel);
                                            elements.forEach(function(el) { el.remove(); });
                                        });

                                        // 3. Lấy innerText thô chứa nội dung truyện/bài viết chính và các thông tin liên quan
                                        var text = temp.innerText.trim();
                                        
                                        // 4. Fallback dự phòng nếu text quá ngắn
                                        if (text.length < 100) {
                                            text = document.body.innerText.trim();
                                        }
                                        
                                        return text;
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
                                        extractedTextForQueue = cleanText
                                        capturedTitleForQueue = viewModel.title.value
                                        capturedUrlForQueue = viewModel.url.value
                                        showModeDialog = true
                                    } else {
                                        extractedTextForQueue = ""
                                        capturedTitleForQueue = viewModel.title.value
                                        capturedUrlForQueue = viewModel.url.value
                                        showModeDialog = true
                                    }
                                }
                            }
                        },
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
            }

            if (showModeDialog) {
                AlertDialog(
                    onDismissRequest = { showModeDialog = false },
                    title = { Text(text = "Lựa chọn chế độ đọc", fontWeight = FontWeight.Bold) },
                    text = { Text(text = "Bạn muốn dịch và nghe trang này ngay lập tức, hay dịch dưới nền và thêm vào hàng chờ để nghe liên tục?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showModeDialog = false
                                viewModel.translateWebpage(extractedTextForQueue, capturedTitleForQueue, capturedUrlForQueue)
                            }
                        ) {
                            Text("Đọc ngay")
                        }
                    },
                    dismissButton = {
                        Row {
                            OutlinedButton(
                                onClick = {
                                    showModeDialog = false
                                    viewModel.translateAndAddToQueue(extractedTextForQueue, capturedTitleForQueue, capturedUrlForQueue)
                                }
                            ) {
                                Text("Thêm vào hàng chờ")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { showModeDialog = false }) {
                                Text("Hủy")
                            }
                        }
                    }
                )
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
