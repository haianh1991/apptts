package com.example.webreader.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

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
    val activeEngineLabel = availableEngines.find { it.name == selectedEngine }?.label ?: selectedEngine.ifBlank { "Mặc định hệ thống" }

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
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
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
                            text = "Lấy khóa API miễn phí từ Google AI Studio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
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
