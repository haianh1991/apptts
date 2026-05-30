package com.example.webreader.ui

import com.example.webreader.data.QueueFolder
import com.example.webreader.data.QueueItem
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.lazy.LazyListItemInfo
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.zIndex
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Slider
import kotlin.math.roundToInt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@Composable
fun ReaderSheet(
    viewModel: BrowserViewModel,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appStrings = LocalAppStrings.current
    val isTranslating by viewModel.isTranslating.collectAsState()
    val foregroundTranslationStep by viewModel.foregroundTranslationStep.collectAsState()
    val foregroundTranslationSteps by viewModel.foregroundTranslationSteps.collectAsState()
    val paragraphs by viewModel.paragraphs.collectAsState()
    val currentIndex by viewModel.currentParagraphIndex.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val title by viewModel.title.collectAsState()
    val queue by viewModel.queue.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val currentQueueItemIndex by viewModel.currentQueueItemIndex.collectAsState()
    val lastReadQueueItemId by viewModel.lastReadQueueItemId.collectAsState()
    var activeTab by remember { mutableIntStateOf(0) }

    val folders by viewModel.folders.collectAsState()
    var expandedFolderIds by remember { mutableStateOf(setOf<String>()) }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }

    var folderToRename by remember { mutableStateOf<QueueFolder?>(null) }
    var renameFolderName by remember { mutableStateOf("") }

    var folderToDelete by remember { mutableStateOf<QueueFolder?>(null) }

    var itemToMove by remember { mutableStateOf<QueueItem?>(null) }

    val currentItem = queue.getOrNull(currentQueueItemIndex)

    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    // Tự động cuộn đến đoạn đang đọc
    LaunchedEffect(currentIndex) {
        if (currentIndex in paragraphs.indices) {
            scope.launch {
                listState.animateScrollToItem(currentIndex)
            }
        }
    }

    var sliderPosition by remember { mutableFloatStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }

    // Đồng bộ vị trí slider với đoạn đang đọc khi không kéo
    LaunchedEffect(currentIndex, paragraphs.size) {
        if (!isDragging) {
            sliderPosition = when {
                currentIndex >= 0 -> currentIndex.toFloat().coerceIn(0f, maxOf(0f, (paragraphs.size - 1).toFloat()))
                else -> 0f
            }
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Thanh tiêu đề của Sheet
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    val displayTitle = remember(activeTab, currentItem, folders, appStrings.readerTitle) {
                        if (activeTab == 0 && currentItem != null) {
                            val folderName = folders.firstOrNull { it.id == currentItem.folderId }?.name
                            if (folderName != null) {
                                "${currentItem.title} ($folderName)"
                            } else {
                                currentItem.title
                            }
                        } else {
                            appStrings.readerTitle
                        }
                    }
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (paragraphs.isNotEmpty()) String.format(appStrings.readerPanelTitlePlaying, currentIndex + 1, paragraphs.size) else appStrings.readerPanelTitleOffline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Row {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = appStrings.browserMenuSettings)
                    }
                    IconButton(onClick = { viewModel.setShowReaderSheet(false) }) {
                        Icon(Icons.Filled.Close, contentDescription = appStrings.btnClose)
                    }
                }
            }

            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .padding(horizontal = 16.dp)
            )

            // Giao diện chuyển đổi Tab (Nội dung, Hàng chờ & Đánh dấu)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Tab Nội dung đang đọc
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (activeTab == 0) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { activeTab = 0 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = appStrings.readerTabContent,
                        color = if (activeTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Tab Đã dịch
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (activeTab == 1) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { activeTab = 1 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(appStrings.readerTabQueueFormat, queue.size),
                        color = if (activeTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                // Tab Đánh dấu trang
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (activeTab == 2) MaterialTheme.colorScheme.primary else Color.Transparent
                        )
                        .clickable { activeTab = 2 },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format(appStrings.readerTabBookmarksFormat, bookmarks.size),
                        color = if (activeTab == 2) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Vùng hiển thị nội dung chính
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                when (activeTab) {
                    0 -> {
                        when {
                            isTranslating && paragraphs.isEmpty() -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(40.dp),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = appStrings.readerTranslatingPrompt,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (foregroundTranslationSteps.isNotEmpty()) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        val stepsListState = rememberLazyListState()
                                        LaunchedEffect(foregroundTranslationSteps.size) {
                                            if (foregroundTranslationSteps.isNotEmpty()) {
                                                stepsListState.animateScrollToItem(foregroundTranslationSteps.lastIndex)
                                            }
                                        }
                                        LazyColumn(
                                            state = stepsListState,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
                                                .padding(horizontal = 24.dp)
                                                .background(
                                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .padding(12.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            items(foregroundTranslationSteps.size) { idx ->
                                                val step = foregroundTranslationSteps[idx]
                                                val isLast = idx == foregroundTranslationSteps.lastIndex
                                                val isError = step.contains("Thất bại", ignoreCase = true) || step.contains("Lỗi", ignoreCase = true) ||
                                                              step.contains("Failed", ignoreCase = true) || step.contains("Error", ignoreCase = true) ||
                                                              step.contains("失败", ignoreCase = true) || step.contains("错误", ignoreCase = true)
                                                val isSuccess = step.contains("Thành công", ignoreCase = true) ||
                                                                step.contains("Success", ignoreCase = true) ||
                                                                step.contains("成功", ignoreCase = true)
                                                
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    verticalAlignment = Alignment.Top
                                                ) {
                                                    if (isLast && !isError && !isSuccess) {
                                                        CircularProgressIndicator(
                                                            modifier = Modifier
                                                                .size(14.dp)
                                                                .padding(top = 2.dp),
                                                            strokeWidth = 2.dp,
                                                            color = MaterialTheme.colorScheme.primary
                                                        )
                                                    } else {
                                                        val iconColor = when {
                                                            isError -> MaterialTheme.colorScheme.error
                                                            isSuccess -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
                                                            else -> MaterialTheme.colorScheme.outline
                                                         }
                                                         val iconChar = when {
                                                             isError -> "✕"
                                                             isSuccess -> "✓"
                                                             else -> "•"
                                                         }
                                                         Text(
                                                             text = iconChar,
                                                             color = iconColor,
                                                             style = MaterialTheme.typography.bodySmall,
                                                             fontWeight = FontWeight.Bold,
                                                             modifier = Modifier.width(14.dp)
                                                         )
                                                    }
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = step,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = when {
                                                            isError -> MaterialTheme.colorScheme.error
                                                            isSuccess -> androidx.compose.ui.graphics.Color(0xFF2E7D32)
                                                            isLast -> MaterialTheme.colorScheme.primary
                                                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                                                        },
                                                        fontWeight = if (isLast) FontWeight.Bold else FontWeight.Normal
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = appStrings.readerTranslatingSubPrompt,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.padding(horizontal = 24.dp),
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            errorMessage != null -> {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = appStrings.readerErrorTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.error,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = errorMessage ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    if (errorMessage!!.contains("API Key")) {
                                        Button(onClick = onOpenSettings) {
                                            Text(appStrings.btnGoToSettings)
                                        }
                                    } else {
                                        OutlinedButton(onClick = { viewModel.clearError() }) {
                                            Text(appStrings.btnIgnoreError)
                                        }
                                    }
                                }
                            }

                            paragraphs.isEmpty() -> {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        text = appStrings.readerNoContent,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.outline
                                    )
                                    Text(
                                        text = appStrings.readerNoContentSub,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.outline,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            else -> {
                                LazyColumn(
                                    state = listState,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    item {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = title,
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )
                                    }

                                    itemsIndexed(paragraphs) { index, paragraph ->
                                        val isHighlighted = index == currentIndex
                                        val containerColor by animateColorAsState(
                                            targetValue = if (isHighlighted) {
                                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                            },
                                            animationSpec = tween(durationMillis = 300),
                                            label = "containerColor"
                                        )

                                        val borderStroke = if (isHighlighted) {
                                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                                        } else {
                                            null
                                        }

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(12.dp))
                                                .clickable { viewModel.playParagraph(index) },
                                            colors = CardDefaults.cardColors(containerColor = containerColor),
                                            border = borderStroke
                                        ) {
                                            Text(
                                                text = paragraph,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.padding(14.dp),
                                                lineHeight = MaterialTheme.typography.bodyMedium.lineHeight * 1.25f
                                            )
                                        }
                                    }
                                    if (isTranslating) {
                                        item {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 16.dp),
                                                horizontalArrangement = Arrangement.Center,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(20.dp),
                                                    strokeWidth = 2.dp,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = appStrings.readerTranslatingMore,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.outline
                                                )
                                            }
                                        }
                                    } else {
                                        item {
                                            Spacer(modifier = Modifier.height(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    1 -> {
                        val activeTranslations by viewModel.activeTranslations.collectAsState()
                        val translatingItems = activeTranslations.filter { 
                            it.status == TranslationStatus.TRANSLATING || it.status == TranslationStatus.WAITING 
                        }
                        val failedItems = activeTranslations.filter { it.status == TranslationStatus.FAILED }
                        
                        val listItems = remember(translatingItems, failedItems, folders, queue, expandedFolderIds) {
                            val list = mutableListOf<QueueListItem>()
                            
                            if (translatingItems.isNotEmpty()) {
                                list.add(QueueListItem.TranslatingHeader(translatingItems.size))
                                translatingItems.forEach { list.add(QueueListItem.TranslatingItem(it)) }
                            }
                            
                            if (failedItems.isNotEmpty()) {
                                list.add(QueueListItem.FailedHeader(failedItems.size))
                                failedItems.forEach { list.add(QueueListItem.FailedItem(it)) }
                            }
                            
                            if (folders.isNotEmpty()) {
                                list.add(QueueListItem.FoldersHeader)
                                folders.forEach { folder ->
                                    val folderItems = queue.filter { it.folderId == folder.id }
                                    val isExpanded = expandedFolderIds.contains(folder.id)
                                    list.add(QueueListItem.FolderHeaderItem(folder, folderItems.size, isExpanded))
                                    if (isExpanded) {
                                        folderItems.forEach { list.add(QueueListItem.FolderQueueItem(it, folder.id)) }
                                    }
                                }
                                
                                val folderIds = folders.map { it.id }.toSet()
                                val rootItems = queue.filter { it.folderId == null || !folderIds.contains(it.folderId) }
                                if (rootItems.isNotEmpty()) {
                                    list.add(QueueListItem.RootItemsHeader(rootItems.size))
                                    rootItems.forEach { list.add(QueueListItem.RootQueueItem(it)) }
                                }
                            } else {
                                if (queue.isNotEmpty()) {
                                    list.add(QueueListItem.FinishedHeader(queue.size))
                                    queue.forEach { list.add(QueueListItem.FlatQueueItem(it)) }
                                }
                            }
                            list
                        }

                        val dragDropListState = rememberLazyListState()
                        val dragDropState = rememberDragDropState(
                            lazyListState = dragDropListState,
                            isDraggable = { index ->
                                if (index in listItems.indices) {
                                    val item = listItems[index]
                                    item is QueueListItem.FolderQueueItem || 
                                    item is QueueListItem.RootQueueItem || 
                                    item is QueueListItem.FlatQueueItem
                                } else {
                                    false
                                }
                            },
                            onMove = { fromIndex, toIndex ->
                                if (fromIndex in listItems.indices && toIndex in listItems.indices) {
                                    val fromItem = listItems[fromIndex]
                                    val toItem = listItems[toIndex]
                                    
                                    val fromId = when (fromItem) {
                                        is QueueListItem.FolderQueueItem -> fromItem.item.id
                                        is QueueListItem.RootQueueItem -> fromItem.item.id
                                        is QueueListItem.FlatQueueItem -> fromItem.item.id
                                        else -> null
                                    }
                                    
                                    val toId = when (toItem) {
                                        is QueueListItem.FolderQueueItem -> toItem.item.id
                                        is QueueListItem.RootQueueItem -> toItem.item.id
                                        is QueueListItem.FlatQueueItem -> toItem.item.id
                                        else -> null
                                    }
                                    
                                    if (fromId != null && toId != null) {
                                        when {
                                            fromItem is QueueListItem.FolderQueueItem && toItem is QueueListItem.FolderQueueItem -> {
                                                if (fromItem.folderId == toItem.folderId) {
                                                    viewModel.reorderQueueItems(fromId, toId)
                                                    true
                                                } else {
                                                    false
                                                }
                                            }
                                            fromItem is QueueListItem.RootQueueItem && toItem is QueueListItem.RootQueueItem -> {
                                                viewModel.reorderQueueItems(fromId, toId)
                                                true
                                            }
                                            fromItem is QueueListItem.FlatQueueItem && toItem is QueueListItem.FlatQueueItem -> {
                                                viewModel.reorderQueueItems(fromId, toId)
                                                true
                                            }
                                            else -> false
                                        }
                                    } else {
                                        false
                                    }
                                } else {
                                    false
                                }
                            }
                        )

                        LaunchedEffect(dragDropState.initiallyDraggedElement) {
                            if (dragDropState.initiallyDraggedElement != null) {
                                while (true) {
                                    val scrollAmount = dragDropState.checkAutoScroll()
                                    if (scrollAmount != 0f) {
                                        dragDropListState.scrollBy(scrollAmount)
                                        dragDropState.checkSwapsAfterScroll()
                                    }
                                    kotlinx.coroutines.delay(10)
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = appStrings.readerArticleListTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { showCreateFolderDialog = true }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Add,
                                            contentDescription = appStrings.dialogNewFolderTitle,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (queue.isNotEmpty() || activeTranslations.isNotEmpty()) {
                                        TextButton(onClick = { viewModel.clearQueue() }) {
                                            Text(
                                                text = appStrings.btnDeleteAll,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }

                            if (queue.isEmpty() && activeTranslations.isEmpty() && folders.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Danh sách trống",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Bạn có thể dịch nhiều trang để thêm vào đây và nghe liên tục.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    state = dragDropListState,
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(
                                        count = listItems.size,
                                        key = { index ->
                                            when (val item = listItems[index]) {
                                                is QueueListItem.TranslatingHeader -> "translating_header"
                                                is QueueListItem.TranslatingItem -> "translating_${item.item.id}"
                                                is QueueListItem.FailedHeader -> "failed_header"
                                                is QueueListItem.FailedItem -> "failed_${item.item.id}"
                                                is QueueListItem.FoldersHeader -> "folders_header"
                                                is QueueListItem.FolderHeaderItem -> "folder_${item.folder.id}"
                                                is QueueListItem.FolderQueueItem -> "folder_item_${item.item.id}"
                                                is QueueListItem.RootItemsHeader -> "root_header"
                                                is QueueListItem.RootQueueItem -> "root_item_${item.item.id}"
                                                is QueueListItem.FinishedHeader -> "finished_header"
                                                is QueueListItem.FlatQueueItem -> "flat_item_${item.item.id}"
                                            }
                                        }
                                    ) { index ->
                                        when (val item = listItems[index]) {
                                            is QueueListItem.TranslatingHeader -> {
                                                Text(
                                                    text = appStrings.readerTranslatingCount.format(item.count),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                                )
                                            }
                                            is QueueListItem.TranslatingItem -> {
                                                val transItem = item.item
                                                val isWaiting = transItem.status == TranslationStatus.WAITING
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isWaiting) {
                                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                        } else {
                                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f)
                                                        }
                                                    ),
                                                    border = BorderStroke(
                                                        1.dp,
                                                        if (isWaiting) {
                                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                        } else {
                                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                                        }
                                                    )
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        if (transItem.status == TranslationStatus.TRANSLATING) {
                                                            CircularProgressIndicator(
                                                                modifier = Modifier.size(20.dp),
                                                                strokeWidth = 2.dp,
                                                                color = MaterialTheme.colorScheme.primary
                                                            )
                                                        } else {
                                                            Icon(
                                                                imageVector = Icons.Filled.History,
                                                                contentDescription = "Waiting",
                                                                tint = MaterialTheme.colorScheme.outline,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                        Spacer(modifier = Modifier.width(12.dp))
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = transItem.title,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1,
                                                                color = if (isWaiting) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurface
                                                            )
                                                            Text(
                                                                text = transItem.currentStep ?: appStrings.readerPreparingTranslation,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = if (isWaiting) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.primary,
                                                                fontWeight = FontWeight.Medium,
                                                                maxLines = 2,
                                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                            )
                                                        }
                                                        IconButton(onClick = { viewModel.removeActiveTranslation(transItem.id) }) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Close,
                                                                contentDescription = appStrings.btnCancel,
                                                                tint = MaterialTheme.colorScheme.outline
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            is QueueListItem.FailedHeader -> {
                                                Text(
                                                    text = appStrings.readerTranslatingFailedCount.format(item.count),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.error,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                                )
                                            }
                                            is QueueListItem.FailedItem -> {
                                                val transItem = item.item
                                                Card(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                                                    ),
                                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(12.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = transItem.title,
                                                                style = MaterialTheme.typography.bodyMedium,
                                                                fontWeight = FontWeight.Bold,
                                                                maxLines = 1
                                                            )
                                                            Text(
                                                                text = transItem.errorMessage ?: appStrings.readerErrorTitle,
                                                                style = MaterialTheme.typography.bodySmall,
                                                                color = MaterialTheme.colorScheme.error,
                                                                maxLines = 2
                                                            )
                                                        }
                                                        IconButton(onClick = { viewModel.retryTranslation(transItem) }) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Refresh,
                                                                contentDescription = appStrings.btnRetry,
                                                                tint = MaterialTheme.colorScheme.primary
                                                            )
                                                        }
                                                        IconButton(onClick = { viewModel.removeActiveTranslation(transItem.id) }) {
                                                            Icon(
                                                                imageVector = Icons.Filled.Close,
                                                                contentDescription = appStrings.btnDelete,
                                                                tint = MaterialTheme.colorScheme.error
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                            is QueueListItem.FoldersHeader -> {
                                                Text(
                                                    text = appStrings.readerFolderCount.format(folders.size),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                                )
                                            }
                                            is QueueListItem.FolderHeaderItem -> {
                                                FolderRow(
                                                    folder = item.folder,
                                                    itemCount = item.itemCount,
                                                    isExpanded = item.isExpanded,
                                                    onToggleExpand = {
                                                        expandedFolderIds = if (item.isExpanded) {
                                                            expandedFolderIds - item.folder.id
                                                        } else {
                                                            expandedFolderIds + item.folder.id
                                                        }
                                                    },
                                                    onRename = {
                                                        folderToRename = item.folder
                                                        renameFolderName = item.folder.name
                                                    },
                                                    onDelete = { folderToDelete = item.folder }
                                                )
                                            }
                                            is QueueListItem.FolderQueueItem -> {
                                                val qItem = item.item
                                                val isCurrent = qItem.id == currentItem?.id
                                                QueueItemCard(
                                                    item = qItem,
                                                    isCurrent = isCurrent,
                                                    isPlaying = isPlaying,
                                                    isReading = qItem.id == lastReadQueueItemId,
                                                    onPlayPause = {
                                                        if (isCurrent) {
                                                            if (isPlaying) {
                                                                viewModel.pauseReading()
                                                            } else {
                                                                viewModel.resumeReading()
                                                                activeTab = 0
                                                            }
                                                        } else {
                                                            viewModel.playQueueItemById(qItem.id)
                                                            activeTab = 0
                                                        }
                                                    },
                                                    onRemove = { viewModel.removeQueueItemById(qItem.id) },
                                                    onMoveClick = { itemToMove = qItem },
                                                    onMoveDown = { viewModel.moveQueueItemDown(qItem.id) },
                                                    onMoveToBottom = { viewModel.moveQueueItemToBottom(qItem.id) },
                                                    modifier = Modifier.padding(start = 16.dp)
                                                )
                                            }
                                            is QueueListItem.RootItemsHeader -> {
                                                Text(
                                                    text = appStrings.readerRootItemsCount.format(item.count),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                                                )
                                            }
                                            is QueueListItem.RootQueueItem -> {
                                                val qItem = item.item
                                                val isCurrent = qItem.id == currentItem?.id
                                                QueueItemCard(
                                                    item = qItem,
                                                    isCurrent = isCurrent,
                                                    isPlaying = isPlaying,
                                                    isReading = qItem.id == lastReadQueueItemId,
                                                    onPlayPause = {
                                                        if (isCurrent) {
                                                            if (isPlaying) {
                                                                viewModel.pauseReading()
                                                            } else {
                                                                viewModel.resumeReading()
                                                                activeTab = 0
                                                            }
                                                        } else {
                                                            viewModel.playQueueItemById(qItem.id)
                                                            activeTab = 0
                                                        }
                                                    },
                                                    onRemove = { viewModel.removeQueueItemById(qItem.id) },
                                                    onMoveClick = { itemToMove = qItem },
                                                    onMoveDown = { viewModel.moveQueueItemDown(qItem.id) },
                                                    onMoveToBottom = { viewModel.moveQueueItemToBottom(qItem.id) }
                                                )
                                            }
                                            is QueueListItem.FinishedHeader -> {
                                                Text(
                                                    text = appStrings.readerFinishedCount.format(item.count),
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                                                )
                                            }
                                            is QueueListItem.FlatQueueItem -> {
                                                val qItem = item.item
                                                val isCurrent = qItem.id == currentItem?.id
                                                QueueItemCard(
                                                    item = qItem,
                                                    isCurrent = isCurrent,
                                                    isPlaying = isPlaying,
                                                    isReading = qItem.id == lastReadQueueItemId,
                                                    onPlayPause = {
                                                        if (isCurrent) {
                                                            if (isPlaying) {
                                                                viewModel.pauseReading()
                                                            } else {
                                                                viewModel.resumeReading()
                                                                activeTab = 0
                                                            }
                                                        } else {
                                                            viewModel.playQueueItemById(qItem.id)
                                                            activeTab = 0
                                                        }
                                                    },
                                                    onRemove = { viewModel.removeQueueItemById(qItem.id) },
                                                    onMoveClick = { itemToMove = qItem },
                                                    onMoveDown = { viewModel.moveQueueItemDown(qItem.id) },
                                                    onMoveToBottom = { viewModel.moveQueueItemToBottom(qItem.id) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    2 -> {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = appStrings.readerSavedPagesTitle,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            if (bookmarks.isEmpty()) {
                                Box(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                        modifier = Modifier.padding(16.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Bookmark,
                                            contentDescription = appStrings.readerNoBookmarks,
                                            tint = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = appStrings.readerNoBookmarks,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = appStrings.readerNoBookmarksSub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.outline,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            } else {
                                LazyColumn(
                                    modifier = Modifier.weight(1f).fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    itemsIndexed(bookmarks) { index, item ->
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    viewModel.loadUrlInBrowser(item.url)
                                                    viewModel.setShowReaderSheet(false)
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                                            ),
                                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                                            ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Filled.Bookmark,
                                                    contentDescription = appStrings.readerTabBookmarks,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = item.title,
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 2
                                                    )
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Text(
                                                        text = item.url,
                                                        style = MaterialTheme.typography.bodySmall,
                                                        color = MaterialTheme.colorScheme.outline,
                                                        maxLines = 1
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                IconButton(
                                                    onClick = { viewModel.deleteBookmark(item) }
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Filled.Close,
                                                        contentDescription = appStrings.btnDelete,
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    item {
                                        Spacer(modifier = Modifier.height(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bảng điều khiển âm thanh ở dưới cùng
            if (paragraphs.isNotEmpty()) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Thanh Timeline (Progress Bar)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val currentText = when {
                                currentIndex >= 0 -> "${currentIndex + 1}"
                                currentIndex == -2 -> "Tiêu đề"
                                else -> "0"
                            }
                            Text(
                                text = currentText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.Start
                            )

                            Slider(
                                value = sliderPosition.coerceIn(0f, maxOf(1f, (paragraphs.size - 1).toFloat())),
                                onValueChange = {
                                    isDragging = true
                                    sliderPosition = it
                                },
                                onValueChangeFinished = {
                                    isDragging = false
                                    val targetIndex = sliderPosition.roundToInt().coerceIn(0, paragraphs.size - 1)
                                    viewModel.playParagraph(targetIndex)
                                },
                                valueRange = 0f..maxOf(1f, (paragraphs.size - 1).toFloat()),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(32.dp)
                            )

                            Text(
                                text = "${paragraphs.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(48.dp),
                                textAlign = TextAlign.End
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Hàng nút điều khiển thu nhỏ
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Nút lùi lại đoạn trước
                            IconButton(
                                onClick = { viewModel.playPrevious() },
                                enabled = currentIndex > 0
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FastRewind,
                                    contentDescription = appStrings.ttsRewind,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Nút Phát / Tạm dừng
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(24.dp))
                                    .clickable {
                                        if (isPlaying) viewModel.pauseReading() else viewModel.resumeReading()
                                    }
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                            contentDescription = if (isPlaying) appStrings.ttsPause else appStrings.ttsPlay,
                                            tint = Color.White,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }
                            }

                            // Nút tiến tới đoạn tiếp theo
                            IconButton(
                                onClick = { viewModel.playNext() },
                                enabled = currentIndex < paragraphs.size - 1
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.FastForward,
                                    contentDescription = appStrings.ttsForward,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialogs for foldering operations
    if (showCreateFolderDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateFolderDialog = false
                newFolderName = ""
            },
            title = { Text(appStrings.dialogNewFolderTitle) },
            text = {
                OutlinedTextField(
                    value = newFolderName,
                    onValueChange = { newFolderName = it },
                    label = { Text(appStrings.folderNameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newFolderName.isNotBlank()) {
                            viewModel.createFolder(newFolderName.trim())
                        }
                        showCreateFolderDialog = false
                        newFolderName = ""
                    }
                ) {
                    Text(appStrings.btnCreate)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCreateFolderDialog = false
                        newFolderName = ""
                    }
                ) {
                    Text(appStrings.btnCancel)
                }
            }
        )
    }

    if (folderToRename != null) {
        AlertDialog(
            onDismissRequest = {
                folderToRename = null
                renameFolderName = ""
            },
            title = { Text(appStrings.dialogRenameFolderTitle) },
            text = {
                OutlinedTextField(
                    value = renameFolderName,
                    onValueChange = { renameFolderName = it },
                    label = { Text(appStrings.folderRenameLabel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val folder = folderToRename
                        if (folder != null && renameFolderName.isNotBlank()) {
                            viewModel.renameFolder(folder.id, renameFolderName.trim())
                        }
                        folderToRename = null
                        renameFolderName = ""
                    }
                ) {
                    Text(appStrings.btnSave)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        folderToRename = null
                        renameFolderName = ""
                    }
                ) {
                    Text(appStrings.btnCancel)
                }
            }
        )
    }

    if (folderToDelete != null) {
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text(appStrings.dialogDeleteFolderTitle) },
            text = { Text(appStrings.dialogDeleteFolderPrompt) },
            confirmButton = {
                Button(
                    onClick = {
                        val folder = folderToDelete
                        if (folder != null) {
                            viewModel.deleteFolder(folder.id, deleteItems = false)
                        }
                        folderToDelete = null
                    }
                ) {
                    Text(appStrings.btnKeepArticles)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            val folder = folderToDelete
                            if (folder != null) {
                                viewModel.deleteFolder(folder.id, deleteItems = true)
                            }
                            folderToDelete = null
                        }
                    ) {
                        Text(appStrings.btnDeleteAll, color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(
                        onClick = { folderToDelete = null }
                    ) {
                        Text(appStrings.btnCancel)
                    }
                }
            }
        )
    }

    if (itemToMove != null) {
        val item = itemToMove!!
        AlertDialog(
            onDismissRequest = { itemToMove = null },
            title = { Text(appStrings.dialogMoveArticleTitle) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 300.dp)
                ) {
                    Text(
                        text = appStrings.dialogMoveArticlePrompt.format(item.title),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.moveQueueItemToFolder(item.id, null)
                                        itemToMove = null
                                    }
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (item.folderId == null) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = appStrings.optionRootFolder,
                                        fontWeight = if (item.folderId == null) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }

                        items(folders.size) { idx ->
                            val folder = folders[idx]
                            val isSelected = item.folderId == folder.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        viewModel.moveQueueItemToFolder(item.id, folder.id)
                                        itemToMove = null
                                    }
                                    .padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Folder,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = folder.name,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { itemToMove = null }) {
                    Text(appStrings.btnClose)
                }
            }
        )
    }
}

// Drag and drop helper state & modifiers for LazyColumn
class DragDropState(
    val lazyListState: LazyListState,
    private val isDraggable: (Int) -> Boolean,
    private val onMove: (Int, Int) -> Boolean
) {
    var draggedDistance by mutableFloatStateOf(0f)
        private set

    var initiallyDraggedElement by mutableStateOf<LazyListItemInfo?>(null)
        private set

    var currentDraggedElement by mutableStateOf<LazyListItemInfo?>(null)
        private set

    private var clickOffset = 0f

    var currentPointerOffset by mutableStateOf(Offset.Zero)
        private set

    val draggedIndex: Int?
        get() = initiallyDraggedElement?.index

    fun onDragStart(itemIndex: Int, offset: Offset) {
        lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == itemIndex }
            .also { item ->
                if (item != null && isDraggable(item.index)) {
                    initiallyDraggedElement = item
                    currentDraggedElement = item
                    currentPointerOffset = Offset(offset.x, item.offset + offset.y)
                    clickOffset = offset.y
                    draggedDistance = 0f
                }
            }
    }

    fun onDragInterrupted() {
        initiallyDraggedElement = null
        currentDraggedElement = null
        draggedDistance = 0f
        currentPointerOffset = Offset.Zero
        clickOffset = 0f
    }

    fun onDrag(dragAmount: Offset) {
        val initiallyDragged = initiallyDraggedElement ?: return
        currentPointerOffset += dragAmount
        
        val currentStart = currentPointerOffset.y - clickOffset
        val currentEnd = currentStart + initiallyDragged.size
        
        draggedDistance = currentStart - initiallyDragged.offset

        val targetElement = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                if (item.index == initiallyDragged.index) return@firstOrNull false
                val itemStart = item.offset
                val itemEnd = item.offset + item.size
                if (initiallyDragged.index < item.index) {
                    currentEnd > itemStart + item.size / 2
                } else {
                    currentStart < itemEnd - item.size / 2
                }
            }
            
        if (targetElement != null) {
            val fromIndex = initiallyDragged.index
            val toIndex = targetElement.index
            if (onMove(fromIndex, toIndex)) {
                initiallyDraggedElement = targetElement
                draggedDistance = currentStart - targetElement.offset
            }
        }
    }

    fun checkAutoScroll(): Float {
        if (initiallyDraggedElement == null) return 0f
        val pointerY = currentPointerOffset.y
        val viewportHeight = lazyListState.layoutInfo.viewportSize.height.toFloat()
        
        val threshold = 150f // px
        val maxScrollSpeed = 20f
        
        return when {
            pointerY < threshold -> {
                val ratio = (threshold - pointerY) / threshold
                -maxScrollSpeed * ratio.coerceIn(0f, 1f)
            }
            pointerY > viewportHeight - threshold -> {
                val ratio = (pointerY - (viewportHeight - threshold)) / threshold
                maxScrollSpeed * ratio.coerceIn(0f, 1f)
            }
            else -> 0f
        }
    }

    fun checkSwapsAfterScroll() {
        val initiallyDragged = initiallyDraggedElement ?: return
        val currentStart = currentPointerOffset.y - clickOffset
        val currentEnd = currentStart + initiallyDragged.size
        
        val currentElement = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.index == initiallyDragged.index }
        if (currentElement != null) {
            draggedDistance = currentStart - currentElement.offset
        }
        
        val targetElement = lazyListState.layoutInfo.visibleItemsInfo
            .firstOrNull { item ->
                if (item.index == initiallyDragged.index) return@firstOrNull false
                val itemStart = item.offset
                val itemEnd = item.offset + item.size
                if (initiallyDragged.index < item.index) {
                    currentEnd > itemStart + item.size / 2
                } else {
                    currentStart < itemEnd - item.size / 2
                }
            }
            
        if (targetElement != null) {
            val fromIndex = initiallyDragged.index
            val toIndex = targetElement.index
            if (onMove(fromIndex, toIndex)) {
                initiallyDraggedElement = targetElement
                draggedDistance = currentStart - targetElement.offset
            }
        }
    }
}

@Composable
fun rememberDragDropState(
    lazyListState: LazyListState,
    isDraggable: (Int) -> Boolean,
    onMove: (Int, Int) -> Boolean
): DragDropState {
    val currentIsDraggable by rememberUpdatedState(isDraggable)
    val currentOnMove by rememberUpdatedState(onMove)
    return remember(lazyListState) {
        DragDropState(
            lazyListState = lazyListState,
            isDraggable = { currentIsDraggable(it) },
            onMove = { from, to -> currentOnMove(from, to) }
        )
    }
}

fun Modifier.dragItem(
    itemIndex: Int,
    dragDropState: DragDropState,
    enabled: Boolean = true
): Modifier {
    if (!enabled) return this
    val draggedIndex = dragDropState.draggedIndex
    val isDragging = draggedIndex == itemIndex
    
    return this
        .zIndex(if (isDragging) 1f else 0f)
        .graphicsLayer {
            translationY = if (isDragging) dragDropState.draggedDistance else 0f
        }
        .pointerInput(dragDropState, itemIndex) {
            detectDragGesturesAfterLongPress(
                onDragStart = { offset -> 
                    dragDropState.onDragStart(itemIndex, offset) 
                },
                onDragEnd = { dragDropState.onDragInterrupted() },
                onDragCancel = { dragDropState.onDragInterrupted() },
                onDrag = { change, dragAmount ->
                    change.consume()
                    dragDropState.onDrag(dragAmount)
                }
            )
        }
}

@Composable
fun FolderRow(
    folder: QueueFolder,
    itemCount: Int,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appStrings = LocalAppStrings.current
    var showFolderMenu by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onToggleExpand() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowDown else Icons.Filled.KeyboardArrowRight,
                contentDescription = if (isExpanded) appStrings.contentDescriptionCollapse else appStrings.contentDescriptionExpand,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Filled.Folder,
                contentDescription = appStrings.folderTitle,
                tint = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Text(
                    text = appStrings.folderItemCountTemplate.format(itemCount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            Box {
                IconButton(onClick = { showFolderMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = appStrings.folderRenameMenu,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }
                DropdownMenu(
                    expanded = showFolderMenu,
                    onDismissRequest = { showFolderMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(appStrings.folderRenameMenu) },
                        onClick = {
                            showFolderMenu = false
                            onRename()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Edit, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(appStrings.folderDeleteMenu, color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showFolderMenu = false
                            onDelete()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun QueueItemCard(
    item: QueueItem,
    isCurrent: Boolean,
    isPlaying: Boolean,
    isReading: Boolean,
    onPlayPause: () -> Unit,
    onRemove: () -> Unit,
    onMoveClick: () -> Unit,
    onMoveDown: () -> Unit,
    onMoveToBottom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val appStrings = LocalAppStrings.current
    val containerColor = if (isCurrent) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }
    val borderColor = if (isCurrent) {
        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = borderColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.url,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = appStrings.paragraphCountTemplate.format(item.paragraphs.size) + if (isReading) appStrings.readingLabel else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = onPlayPause) {
                Icon(
                    imageVector = if (isCurrent && isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = appStrings.ttsPlay,
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            var showItemMenu by remember { mutableStateOf(false) }

            Box {
                IconButton(onClick = { showItemMenu = true }) {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = appStrings.contentDescriptionExpand,
                        tint = MaterialTheme.colorScheme.outline
                    )
                }

                DropdownMenu(
                    expanded = showItemMenu,
                    onDismissRequest = { showItemMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text(appStrings.queueItemMoveMenu) },
                        onClick = {
                            showItemMenu = false
                            onMoveClick()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Folder, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(appStrings.queueItemMoveDown) },
                        onClick = {
                            showItemMenu = false
                            onMoveDown()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.ArrowDownward, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(appStrings.queueItemMoveToBottom) },
                        onClick = {
                            showItemMenu = false
                            onMoveToBottom()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.FastForward, contentDescription = null)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(appStrings.btnDelete, color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showItemMenu = false
                            onRemove()
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        }
                    )
                }
            }
        }
    }
}

sealed class QueueListItem {
    data class TranslatingHeader(val count: Int) : QueueListItem()
    data class TranslatingItem(val item: ActiveTranslation) : QueueListItem()
    data class FailedHeader(val count: Int) : QueueListItem()
    data class FailedItem(val item: ActiveTranslation) : QueueListItem()
    object FoldersHeader : QueueListItem()
    data class FolderHeaderItem(val folder: QueueFolder, val itemCount: Int, val isExpanded: Boolean) : QueueListItem()
    data class FolderQueueItem(val item: QueueItem, val folderId: String) : QueueListItem()
    data class RootItemsHeader(val count: Int) : QueueListItem()
    data class RootQueueItem(val item: QueueItem) : QueueListItem()
    data class FinishedHeader(val count: Int) : QueueListItem()
    data class FlatQueueItem(val item: QueueItem) : QueueListItem()
}
