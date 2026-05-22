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
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Add
import androidx.compose.foundation.border
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.DialogProperties

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
    val showUpdateDialog by viewModel.showUpdateDialog.collectAsState()
    val isForceUpdate by viewModel.isForceUpdate.collectAsState()
    val updateInfo by viewModel.updateInfo.collectAsState()

    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var urlInputText by remember { mutableStateOf(currentUrl) }
    var pageLoadProgress by remember { mutableFloatStateOf(0f) }
    var showModeDialog by remember { mutableStateOf(false) }
    var selectedFolderId by remember { mutableStateOf<String?>(null) }
    var showCreateFolderDialogInTranslation by remember { mutableStateOf(false) }
    var newFolderNameInTranslation by remember { mutableStateOf("") }
    var extractedTextForQueue by remember { mutableStateOf("") }
    var capturedTitleForQueue by remember { mutableStateOf("") }
    var capturedUrlForQueue by remember { mutableStateOf("") }
    var isAddressBarFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

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
                                val currentWebViewUrl = webViewInstance?.url ?: currentUrl
                                val hasTranslateCookie = android.webkit.CookieManager.getInstance().getCookie(currentWebViewUrl)?.contains("googtrans=/auto/vi") == true
                                val hasTranslateHash = currentWebViewUrl.contains("#googtrans(auto|vi)")
                                val isTranslated = hasTranslateCookie || hasTranslateHash

                                val menuText = if (isTranslated) "Xem trang gốc" else "Dịch trang web"
                                val menuIcon = if (isTranslated) Icons.Filled.Close else Icons.Filled.Translate

                                DropdownMenuItem(
                                    text = { Text(menuText) },
                                    onClick = {
                                        showMenu = false
                                        val cookieManager = android.webkit.CookieManager.getInstance()
                                        cookieManager.setAcceptCookie(true)
                                        cookieManager.setAcceptThirdPartyCookies(webViewInstance, true)
                                        
                                        if (isTranslated) {
                                            android.widget.Toast.makeText(context, "Đang tải lại trang gốc...", android.widget.Toast.LENGTH_SHORT).show()
                                            cookieManager.setCookie(currentWebViewUrl, "googtrans=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT")
                                            try {
                                                val uri = android.net.Uri.parse(currentWebViewUrl)
                                                val host = uri.host
                                                if (host != null) {
                                                    val parts = host.split(".")
                                                    if (parts.size > 2) {
                                                        val domain = parts.takeLast(2).joinToString(".")
                                                        cookieManager.setCookie(currentWebViewUrl, "googtrans=; path=/; domain=.$domain; expires=Thu, 01 Jan 1970 00:00:00 GMT")
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                            cookieManager.flush()
                                            
                                            var cleanUrl = currentWebViewUrl
                                            if (cleanUrl.contains("#googtrans")) {
                                                cleanUrl = cleanUrl.substringBefore("#googtrans")
                                            }
                                            webViewInstance?.loadUrl(cleanUrl)
                                        } else {
                                            android.widget.Toast.makeText(context, "Đang dịch trang web sang Tiếng Việt...", android.widget.Toast.LENGTH_SHORT).show()
                                            cookieManager.setCookie(currentWebViewUrl, "googtrans=/auto/vi; path=/")
                                            try {
                                                val uri = android.net.Uri.parse(currentWebViewUrl)
                                                val host = uri.host
                                                if (host != null) {
                                                    val parts = host.split(".")
                                                    if (parts.size > 2) {
                                                        val domain = parts.takeLast(2).joinToString(".")
                                                        cookieManager.setCookie(currentWebViewUrl, "googtrans=/auto/vi; path=/; domain=.$domain")
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                e.printStackTrace()
                                            }
                                            cookieManager.flush()
                                            
                                            val targetUrl = if (!currentWebViewUrl.contains("#googtrans")) {
                                                val separator = if (currentWebViewUrl.contains("#")) "" else "#"
                                                currentWebViewUrl + separator + "googtrans(auto|vi)"
                                            } else {
                                                currentWebViewUrl
                                            }
                                            webViewInstance?.loadUrl(targetUrl)
                                        }
                                    },
                                    leadingIcon = {
                                        Icon(menuIcon, contentDescription = menuText)
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
                        // Cho phép và quản lý cookie đầy đủ để Google Translate Element hoạt động
                        android.webkit.CookieManager.getInstance().setAcceptCookie(true)
                        android.webkit.CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            builtInZoomControls = true
                            displayZoomControls = false
                            useWideViewPort = true
                            loadWithOverviewMode = true
                            databaseEnabled = true
                            // Cho phép mixed content để tránh bị chặn file CSS/JS của Google dịch trên trang HTTPS
                            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            javaScriptCanOpenWindowsAutomatically = true
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

                                // 1. Trích xuất và lưu trữ văn bản gốc khi trang tải xong trước khi dịch toàn trang
                                val cacheScript = """
                                    (function() {
                                        if (!document.body) return;
                                        var temp = document.body.cloneNode(true);
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
                                        window.originalWebContent = temp.innerText.trim();
                                    })()
                                """.trimIndent()
                                view?.evaluateJavascript(cacheScript, null)

                                // 2. Tự động tiêm script Google Dịch Element nếu có cookie hoặc hash dịch
                                val currentUrlStr = url ?: ""
                                val hasTransCookie = android.webkit.CookieManager.getInstance().getCookie(currentUrlStr)?.contains("googtrans=/auto/vi") == true
                                val hasTransHash = currentUrlStr.contains("#googtrans(auto|vi)")
                                
                                if (hasTransCookie || hasTransHash) {
                                    val translateInjectScript = """
                                        (function() {
                                            var id = 'google_translate_element';
                                            if (!document.getElementById(id)) {
                                                var div = document.createElement('div');
                                                div.id = id;
                                                div.style.display = 'none';
                                                document.body.insertBefore(div, document.body.firstChild);
                                            }
                                            
                                            window.googleTranslateElementInit = function() {
                                                new google.translate.TranslateElement({
                                                    pageLanguage: 'auto',
                                                    layout: google.translate.TranslateElement.InlineLayout.SIMPLE,
                                                    autoDisplay: true
                                                }, id);
                                            };
                                            
                                            if (!window.googleTranslateScriptLoaded) {
                                                window.googleTranslateScriptLoaded = true;
                                                var script = document.createElement('script');
                                                script.type = 'text/javascript';
                                                script.src = 'https://translate.google.com/translate_a/element.js?cb=googleTranslateElementInit';
                                                document.body.appendChild(script);
                                            }
                                        })();
                                    """.trimIndent()
                                    view?.evaluateJavascript(translateInjectScript, null)
                                }
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

                            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                                android.util.Log.d("WebViewConsole", "${consoleMessage?.message()} -- From line ${consoleMessage?.lineNumber()} of ${consoleMessage?.sourceId()}")
                                return true
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
                                val jsExtractor = """
                                    (function() {
                                        // 1. Ưu tiên lấy văn bản gốc đã được lưu trữ (từ trước khi dịch toàn trang)
                                        if (window.originalWebContent && window.originalWebContent.trim().length > 100) {
                                            return window.originalWebContent;
                                        }
                                        
                                        // 2. Fallback trích xuất lại từ DOM hiện tại nếu cache bị trống
                                        if (!document.body) return "";
                                        var temp = document.body.cloneNode(true);
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
                                        var text = temp.innerText.trim();
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
                                        val queueList = viewModel.queue.value
                                        val activeIndex = viewModel.currentQueueItemIndex.value
                                        val activeItem = if (activeIndex in queueList.indices) queueList[activeIndex] else null
                                        selectedFolderId = activeItem?.folderId
                                        showModeDialog = true
                                    } else {
                                        extractedTextForQueue = ""
                                        capturedTitleForQueue = viewModel.title.value
                                        capturedUrlForQueue = viewModel.url.value
                                        val queueList = viewModel.queue.value
                                        val activeIndex = viewModel.currentQueueItemIndex.value
                                        val activeItem = if (activeIndex in queueList.indices) queueList[activeIndex] else null
                                        selectedFolderId = activeItem?.folderId
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
                    text = {
                        Column {
                            Text(
                                text = "Bạn muốn dịch và nghe trang này ngay lập tức, hay dịch dưới nền và thêm vào hàng chờ để nghe liên tục?",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text(
                                text = "Lưu vào thư mục:",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val folders by viewModel.folders.collectAsState()
                            var dropdownExpanded by remember { mutableStateOf(false) }
                            
                            val selectedFolderName = if (selectedFolderId == null) {
                                "Thư mục gốc"
                            } else {
                                folders.find { it.id == selectedFolderId }?.name ?: "Thư mục gốc"
                            }
                            
                            Box {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                        .clickable { dropdownExpanded = true }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = selectedFolderName,
                                        style = MaterialTheme.typography.bodyLarge,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.KeyboardArrowDown,
                                        contentDescription = "Chọn thư mục",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                
                                DropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.7f)
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Thư mục gốc") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                        },
                                        onClick = {
                                            selectedFolderId = null
                                            dropdownExpanded = false
                                        }
                                    )
                                    
                                    folders.forEach { folder ->
                                        DropdownMenuItem(
                                            text = { Text(folder.name) },
                                            leadingIcon = {
                                                Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                            },
                                            onClick = {
                                                selectedFolderId = folder.id
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                    
                                    DropdownMenuItem(
                                        text = { Text("Tạo thư mục mới...") },
                                        leadingIcon = {
                                            Icon(Icons.Filled.Add, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                                        },
                                        onClick = {
                                            dropdownExpanded = false
                                            showCreateFolderDialogInTranslation = true
                                        }
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showModeDialog = false
                                viewModel.translateWebpage(extractedTextForQueue, capturedTitleForQueue, capturedUrlForQueue, selectedFolderId)
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
                                    viewModel.translateAndAddToQueue(extractedTextForQueue, capturedTitleForQueue, capturedUrlForQueue, selectedFolderId)
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

            if (showCreateFolderDialogInTranslation) {
                AlertDialog(
                    onDismissRequest = {
                        showCreateFolderDialogInTranslation = false
                        newFolderNameInTranslation = ""
                    },
                    title = { Text("Tạo thư mục mới") },
                    text = {
                        OutlinedTextField(
                            value = newFolderNameInTranslation,
                            onValueChange = { newFolderNameInTranslation = it },
                            label = { Text("Tên thư mục") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val name = newFolderNameInTranslation.trim()
                                if (name.isNotEmpty()) {
                                    val newId = viewModel.createFolder(name)
                                    selectedFolderId = newId
                                }
                                showCreateFolderDialogInTranslation = false
                                newFolderNameInTranslation = ""
                            }
                        ) {
                            Text("Tạo")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                showCreateFolderDialogInTranslation = false
                                newFolderNameInTranslation = ""
                            }
                        ) {
                            Text("Hủy")
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

            if (showUpdateDialog && updateInfo != null) {
                AlertDialog(
                    onDismissRequest = {
                        if (!isForceUpdate) {
                            viewModel.dismissUpdateDialog()
                        }
                    },
                    properties = DialogProperties(
                        dismissOnBackPress = !isForceUpdate,
                        dismissOnClickOutside = !isForceUpdate
                    ),
                    title = {
                        Text(
                            text = if (isForceUpdate) "Yêu cầu cập nhật bắt buộc" else "Đã có phiên bản mới",
                            fontWeight = FontWeight.Bold
                        )
                    },
                    text = {
                        Column {
                            Text(
                                text = "Ứng dụng ${updateInfo?.versionName} đã sẵn sàng để tải xuống.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            if (updateInfo?.releaseNotes?.isNotEmpty() == true) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nhật ký thay đổi:",
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = updateInfo?.releaseNotes ?: "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(updateInfo?.updateUrl))
                                context.startActivity(intent)
                            }
                        ) {
                            Text("Cập nhật ngay")
                        }
                    },
                    dismissButton = if (!isForceUpdate) {
                        {
                            TextButton(
                                onClick = { viewModel.dismissUpdateDialog() }
                            ) {
                                Text("Để sau")
                            }
                        }
                    } else null
                )
            }
        }
    }
}
