package com.example.webreader.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.webreader.data.TransactionLog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: BrowserViewModel,
    onBackClick: () -> Unit
) {
    val settings = viewModel.settings

    var apiKey by remember { mutableStateOf(settings.geminiApiKey) }
    var selectedModel by remember { mutableStateOf(settings.geminiModel) }
    var ttsSpeed by remember { mutableFloatStateOf(settings.ttsSpeed) }
    var ttsPitch by remember { mutableFloatStateOf(settings.ttsPitch) }
    var selectedEngine by remember { mutableStateOf(settings.ttsEngine) }

    var apiKeyVisible by remember { mutableStateOf(false) }
    var isDropdownExpanded by remember { mutableStateOf(false) }
    var isEngineDropdownExpanded by remember { mutableStateOf(false) }

    val availableEngines = remember { viewModel.ttsManager.getAvailableTtsEngines() }
    val activeEngineLabel = availableEngines.find { it.name == selectedEngine }?.label 
        ?: if (selectedEngine == "com.google.android.tts") "Google TTS (Buộc kích hoạt)" else selectedEngine.ifBlank { "Mặc định hệ thống" }

    val models = listOf(
        "gemini-3.5-flash",
        "gemini-3-flash-preview",
        "gemini-3.1-flash-lite",
        "gemini-2.5-flash",
        "gemini-1.5-flash"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt ứng dụng") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Card cấu hình Gemini API
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cấu hình Gemini API",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        label = { Text("Gemini API Key") },
                        placeholder = { Text("Dán các API key tại đây, cách nhau bởi dấu phẩy hoặc dòng mới") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 4,
                        visualTransformation = if (apiKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { apiKeyVisible = !apiKeyVisible }) {
                                Icon(
                                    imageVector = if (apiKeyVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                    contentDescription = if (apiKeyVisible) "Ẩn API Key" else "Hiện API Key"
                                )
                            }
                        }
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Info,
                                contentDescription = "Info",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "Hỗ trợ nhiều khóa API để tự động xoay vòng khi có lỗi.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "Lấy khóa API miễn phí từ Google AI Studio. Có thể dán danh sách khóa cách nhau bởi dấu phẩy hoặc ngắt dòng.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Dropdown chọn Model
                    ExposedDropdownMenuBox(
                        expanded = isDropdownExpanded,
                        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedModel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Mô hình AI") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = isDropdownExpanded,
                            onDismissRequest = { isDropdownExpanded = false }
                        ) {
                            models.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model) },
                                    onClick = {
                                        selectedModel = model
                                        isDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Card cấu hình giọng đọc TTS
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Cài đặt giọng đọc (TTS)",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Dropdown chọn Công cụ TTS
                    ExposedDropdownMenuBox(
                        expanded = isEngineDropdownExpanded,
                        onExpandedChange = { isEngineDropdownExpanded = !isEngineDropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = activeEngineLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Công cụ giọng đọc (TTS Engine)") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isEngineDropdownExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                        )
                        ExposedDropdownMenu(
                            expanded = isEngineDropdownExpanded,
                            onDismissRequest = { isEngineDropdownExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Mặc định hệ thống") },
                                onClick = {
                                    selectedEngine = ""
                                    isEngineDropdownExpanded = false
                                }
                            )
                            // check if Google TTS is already in availableEngines
                            val hasGoogleTts = availableEngines.any { it.name == "com.google.android.tts" }
                            if (!hasGoogleTts) {
                                DropdownMenuItem(
                                    text = { Text("Google TTS (Buộc kích hoạt)") },
                                    onClick = {
                                        selectedEngine = "com.google.android.tts"
                                        isEngineDropdownExpanded = false
                                    }
                                )
                            }
                            availableEngines.forEach { engine ->
                                DropdownMenuItem(
                                    text = { Text(engine.label) },
                                    onClick = {
                                        selectedEngine = engine.name
                                        isEngineDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Điều chỉnh tốc độ
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tốc độ đọc: ${String.format("%.1fx", ttsSpeed)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(onClick = { ttsSpeed = 1.0f }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Khôi phục tốc độ")
                            }
                        }
                        Slider(
                            value = ttsSpeed,
                            onValueChange = { ttsSpeed = it },
                            valueRange = 0.5f..2.0f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Điều chỉnh cao độ
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Cao độ giọng: ${String.format("%.1fx", ttsPitch)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            IconButton(onClick = { ttsPitch = 1.0f }) {
                                Icon(Icons.Filled.Refresh, contentDescription = "Khôi phục cao độ")
                            }
                        }
                        Slider(
                            value = ttsPitch,
                            onValueChange = { ttsPitch = it },
                            valueRange = 0.5f..1.5f,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Nút chạy thử
                    OutlinedButton(
                        onClick = {
                            viewModel.ttsManager.speak(
                                "Xin chào, đây là giọng đọc thử tiếng Việt trên ứng dụng của bạn.",
                                -999, // ID đặc biệt để thử giọng
                                ttsSpeed,
                                ttsPitch
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = "Chạy thử")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Nghe thử giọng đọc Tiếng Việt")
                    }
                }
            }

            // Card Nhật ký dịch thuật
            val translationLogs by viewModel.translationLogs.collectAsState(initial = emptyList())
            val clipboardManager = LocalClipboardManager.current
            val sdf = remember { SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()) }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = "Lịch sử dịch",
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "Nhật ký dịch thuật",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (translationLogs.isNotEmpty()) {
                            IconButton(
                                onClick = { viewModel.clearTranslationLogs() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = "Xóa tất cả nhật ký",
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }

                    if (translationLogs.isEmpty()) {
                        Text(
                            text = "Chưa có nhật ký dịch thuật nào.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            translationLogs.forEach { log ->
                                TransactionLogItem(
                                    log = log,
                                    sdf = sdf,
                                    onCopyClick = {
                                        val logDetail = """
                                            --- NHẬT KÝ DỊCH THUẬT ---
                                            ID: ${log.id}
                                            Thời gian: ${sdf.format(Date(log.timestamp))}
                                            Loại dịch: ${log.type}
                                            Bài viết: ${log.title}
                                            URL: ${log.url}
                                            Trạng thái: ${log.status}
                                            Khóa API đã dùng: ${log.usedApiKeys.joinToString(", ")}
                                            
                                            Tiến trình chi tiết:
                                            ${log.steps.mapIndexed { idx, step -> "${idx + 1}. $step" }.joinToString("\n")}
                                            
                                            ${if (log.status == "Thành công") "Phản hồi dịch từ Gemini:" else "Chi tiết lỗi:"}
                                            ${if (log.status == "Thành công") log.geminiResponse else log.errorMessage}
                                            -------------------------
                                        """.trimIndent()
                                        clipboardManager.setText(AnnotatedString(logDetail))
                                        android.widget.Toast.makeText(
                                            viewModel.getApplication(),
                                            "Đã sao chép nhật ký vào Clipboard",
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Nút Lưu cấu hình
            Button(
                onClick = {
                    settings.geminiApiKey = apiKey.trim()
                    settings.geminiModel = selectedModel
                    viewModel.updateTtsSettings(ttsSpeed, ttsPitch, selectedEngine)
                    onBackClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Filled.Save, contentDescription = "Lưu")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Lưu thiết lập")
            }
        }
    }
}

@Composable
fun TransactionLogItem(
    log: TransactionLog,
    sdf: SimpleDateFormat,
    onCopyClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Clickable to toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Type Badge
                    val typeBg = if (log.type == "Đọc ngay") MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    val typeFg = if (log.type == "Đọc ngay") MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                    Card(
                        colors = CardDefaults.cardColors(containerColor = typeBg)
                    ) {
                        Text(
                            text = log.type,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            color = typeFg
                        )
                    }

                    // Status Badge
                    val statusBg = if (log.status == "Thành công") Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                    val statusFg = if (log.status == "Thành công") Color(0xFF2E7D32) else Color(0xFFC62828)
                    Card(
                        colors = CardDefaults.cardColors(containerColor = statusBg)
                    ) {
                        Text(
                            text = log.status,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontWeight = FontWeight.Bold,
                            color = statusFg
                        )
                    }

                    // Time
                    Text(
                        text = sdf.format(Date(log.timestamp)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                        contentDescription = if (expanded) "Thu gọn" else "Mở rộng"
                    )
                }
            }

            // Title and URL (always visible, but clickable)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
            ) {
                Text(
                    text = log.title.ifBlank { "Không có tiêu đề" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                if (log.url.isNotBlank()) {
                    Text(
                        text = log.url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            if (expanded) {
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
                )

                // API Keys
                if (log.usedApiKeys.isNotEmpty()) {
                    Text(
                        text = "Khóa API đã dùng: " + log.usedApiKeys.joinToString(", "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Steps
                if (log.steps.isNotEmpty()) {
                    Text(
                        text = "Tiến trình chi tiết:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.padding(start = 4.dp)
                    ) {
                        log.steps.forEachIndexed { i, step ->
                            Text(
                                text = "${i + 1}. $step",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Response / Error Detail
                val isSuccess = log.status == "Thành công"
                val detailTitle = if (isSuccess) "Phản hồi dịch từ Gemini:" else "Chi tiết lỗi:"
                val detailText = (if (isSuccess) log.geminiResponse else log.errorMessage) ?: "Không có dữ liệu"
                val detailBoxBg = if (isSuccess) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                val detailBoxBorderColor = if (isSuccess) MaterialTheme.colorScheme.outline.copy(alpha = 0.3f) else MaterialTheme.colorScheme.error.copy(alpha = 0.3f)

                Text(
                    text = detailTitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = detailBoxBg),
                    border = androidx.compose.foundation.BorderStroke(1.dp, detailBoxBorderColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (isSuccess) 120.dp else 80.dp)
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = detailText,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }

                // Actions row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onCopyClick,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Sao chép",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Sao chép nhật ký", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}
