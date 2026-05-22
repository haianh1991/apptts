package com.example.webreader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.webreader.data.GeminiManager
import com.example.webreader.data.SettingsRepository
import com.example.webreader.data.TtsManager
import com.example.webreader.data.QueueItem
import com.example.webreader.data.QueueFolder
import com.example.webreader.data.QueueRepository
import com.example.webreader.data.BookmarkItem
import com.example.webreader.data.BookmarkRepository
import com.example.webreader.data.TransactionLog
import com.example.webreader.data.TransactionLogRepository
import com.example.webreader.data.AppUpdateInfo
import com.example.webreader.data.UpdateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import com.example.webreader.data.AuthManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        @Volatile
        var activeInstance: BrowserViewModel? = null
            private set
    }

    val settings = SettingsRepository(application)
    private val geminiManager = GeminiManager()
    val ttsManager = TtsManager(application)
    val queueRepository = QueueRepository(application)
    val bookmarkRepository = BookmarkRepository(application)
    val transactionLogRepository = TransactionLogRepository(application)

    val authManager = AuthManager(application)
    private val _currentUser = MutableStateFlow<FirebaseUser?>(authManager.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        _currentUser.value = auth.currentUser
    }

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue

    private val _folders = MutableStateFlow<List<QueueFolder>>(emptyList())
    val folders: StateFlow<List<QueueFolder>> = _folders

    private val _translationLogs = MutableStateFlow<List<TransactionLog>>(emptyList())
    val translationLogs: StateFlow<List<TransactionLog>> = _translationLogs

    private val _foregroundTranslationStep = MutableStateFlow("")
    val foregroundTranslationStep: StateFlow<String> = _foregroundTranslationStep

    private val _foregroundTranslationSteps = MutableStateFlow<List<String>>(emptyList())
    val foregroundTranslationSteps: StateFlow<List<String>> = _foregroundTranslationSteps

    private val _activeTranslations = MutableStateFlow<List<ActiveTranslation>>(emptyList())
    val activeTranslations: StateFlow<List<ActiveTranslation>> = _activeTranslations

    private val _currentQueueItemIndex = MutableStateFlow<Int>(-1)
    val currentQueueItemIndex: StateFlow<Int> = _currentQueueItemIndex

    private val _url = MutableStateFlow("https://news.google.com")
    val url: StateFlow<String> = _url

    private val _bookmarks = MutableStateFlow<List<BookmarkItem>>(emptyList())
    val bookmarks: StateFlow<List<BookmarkItem>> = _bookmarks

    private val _isCurrentPageBookmarked = MutableStateFlow(false)
    val isCurrentPageBookmarked: StateFlow<Boolean> = _isCurrentPageBookmarked

    private val _navigationRequest = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val navigationRequest: SharedFlow<String> = _navigationRequest

    private val _title = MutableStateFlow("Trình duyệt")
    val title: StateFlow<String> = _title

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack

    private val _canGoForward = MutableStateFlow(false)
    val canGoForward: StateFlow<Boolean> = _canGoForward

    // Reader state
    private val _paragraphs = MutableStateFlow<List<String>>(emptyList())
    val paragraphs: StateFlow<List<String>> = _paragraphs

    private val _currentParagraphIndex = MutableStateFlow(-1)
    val currentParagraphIndex: StateFlow<Int> = _currentParagraphIndex

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying

    private val _isTranslating = MutableStateFlow(false)
    val isTranslating: StateFlow<Boolean> = _isTranslating

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _showReaderSheet = MutableStateFlow(false)
    val showReaderSheet: StateFlow<Boolean> = _showReaderSheet

    // App Update flows
    private val _updateInfo = MutableStateFlow<AppUpdateInfo?>(null)
    val updateInfo: StateFlow<AppUpdateInfo?> = _updateInfo

    private val _showUpdateDialog = MutableStateFlow(false)
    val showUpdateDialog: StateFlow<Boolean> = _showUpdateDialog

    private val _isForceUpdate = MutableStateFlow(false)
    val isForceUpdate: StateFlow<Boolean> = _isForceUpdate

    init {
        activeInstance = this
        val queueData = queueRepository.getQueueData()
        _folders.value = queueData.folders
        _queue.value = sortQueueItems(queueData.items, queueData.folders)
        _bookmarks.value = bookmarkRepository.getBookmarks()
        updateBookmarkStatus()
        viewModelScope.launch {
            _translationLogs.value = transactionLogRepository.getLogs()
        }
        ttsManager.setCallbacks(
            onStart = { index ->
                _currentParagraphIndex.value = index
            },
            onDone = { index ->
                playNextParagraph(index)
            },
            onError = { errorMsg ->
                _errorMessage.value = errorMsg
                _isPlaying.value = false
            }
        )
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
        checkAppUpdate()
    }

    private fun sortQueueItems(items: List<QueueItem>, foldersList: List<QueueFolder>): List<QueueItem> {
        val sortedFolders = foldersList.sortedBy { it.createdAt }
        val folderIds = sortedFolders.map { it.id }.toSet()
        val groupedItems = items.groupBy { it.folderId }
        val result = mutableListOf<QueueItem>()
        
        sortedFolders.forEach { folder ->
            val folderItems = groupedItems[folder.id] ?: emptyList()
            result.addAll(folderItems.sortedBy { it.createdAt })
        }
        
        val rootItems = items.filter { it.folderId == null || !folderIds.contains(it.folderId) }
        result.addAll(rootItems.sortedByDescending { it.createdAt })
        
        return result
    }

    private fun playNextParagraph(completedIndex: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val nextIndex = if (completedIndex == -2) 0 else completedIndex + 1
            if (nextIndex in _paragraphs.value.indices) {
                _currentParagraphIndex.value = nextIndex
                val text = _paragraphs.value[nextIndex]
                ttsManager.speak(text, nextIndex, settings.ttsSpeed, settings.ttsPitch)
            } else {
                if (_isTranslating.value) {
                    // Still translating! Just update the current index to the completed index so we know we finished it,
                    // and wait for more paragraphs.
                    _currentParagraphIndex.value = completedIndex
                    // We don't change _isPlaying, so it remains active and waiting.
                } else {
                    // Finished paragraphs of the current item! Check if there's a next queue item
                    val qList = _queue.value
                    val qIndex = _currentQueueItemIndex.value
                    if (qIndex != -1 && qIndex + 1 < qList.size) {
                        val nextQIndex = qIndex + 1
                        _currentQueueItemIndex.value = nextQIndex
                        val nextItem = qList[nextQIndex]
                        _paragraphs.value = nextItem.paragraphs
                        _title.value = nextItem.title
                        _currentParagraphIndex.value = -2 // Announces title first
                        
                        val announceText = "Bắt đầu đọc bài viết: ${nextItem.title}"
                        ttsManager.speak(announceText, -2, settings.ttsSpeed, settings.ttsPitch)
                    } else {
                        _isPlaying.value = false
                        _currentParagraphIndex.value = -1
                        _currentQueueItemIndex.value = -1
                        ttsManager.stop()
                    }
                }
            }
        }
    }

    fun playParagraph(index: Int) {
        val paragraphsList = _paragraphs.value
        if (index in paragraphsList.indices) {
            _currentParagraphIndex.value = index
            _isPlaying.value = true
            ttsManager.speak(paragraphsList[index], index, settings.ttsSpeed, settings.ttsPitch)
        }
    }

    fun pauseReading() {
        ttsManager.stop()
        _isPlaying.value = false
    }

    fun resumeReading() {
        val index = _currentParagraphIndex.value
        val paragraphsList = _paragraphs.value
        if (index == -2) {
            _isPlaying.value = true
            val announceText = "Bắt đầu đọc bài viết: ${_title.value}"
            ttsManager.speak(announceText, -2, settings.ttsSpeed, settings.ttsPitch)
        } else if (paragraphsList.isNotEmpty()) {
            val targetIndex = if (index in paragraphsList.indices) index else 0
            _currentParagraphIndex.value = targetIndex
            _isPlaying.value = true
            ttsManager.speak(paragraphsList[targetIndex], targetIndex, settings.ttsSpeed, settings.ttsPitch)
        }
    }

    fun playNext() {
        val index = _currentParagraphIndex.value
        val paragraphsList = _paragraphs.value
        if (index == -2) {
            if (paragraphsList.isNotEmpty()) {
                playParagraph(0)
            }
        } else if (index + 1 < paragraphsList.size) {
            playParagraph(index + 1)
        } else {
            val qList = _queue.value
            val qIndex = _currentQueueItemIndex.value
            if (qIndex != -1 && qIndex + 1 < qList.size) {
                playQueueItem(qIndex + 1)
            }
        }
    }

    fun playPrevious() {
        val index = _currentParagraphIndex.value
        if (index == -2) {
            val qIndex = _currentQueueItemIndex.value
            if (qIndex > 0) {
                playQueueItem(qIndex - 1)
            }
        } else if (index - 1 >= 0) {
            playParagraph(index - 1)
        } else if (index == 0) {
            val qIndex = _currentQueueItemIndex.value
            if (qIndex != -1) {
                playQueueItem(qIndex)
            }
        } else {
            val qIndex = _currentQueueItemIndex.value
            if (qIndex > 0) {
                playQueueItem(qIndex - 1)
            }
        }
    }

    fun translateWebpage(text: String, title: String, url: String, folderId: String? = null) {
        if (settings.geminiApiKeys.isEmpty()) {
            val errMsg = "Vui lòng nhập API Key trong phần Cài đặt để dịch trang web."
            _errorMessage.value = errMsg
            _showReaderSheet.value = true // Show reader sheet to display the error and prompt user
            
            val logSteps = mutableListOf("Lỗi khởi tạo: Danh sách khóa API Gemini trống. Vui lòng thiết lập trong Cài đặt.")
            val newLog = TransactionLog(
                type = "Đọc ngay",
                title = title,
                url = url,
                status = "Thất bại",
                usedApiKeys = emptyList(),
                steps = logSteps,
                geminiResponse = null,
                errorMessage = errMsg
            )
            viewModelScope.launch {
                transactionLogRepository.addLog(newLog)
                _translationLogs.value = transactionLogRepository.getLogs()
            }
            return
        }

        val logId = java.util.UUID.randomUUID().toString()
        viewModelScope.launch {
            _isTranslating.value = true
            _showReaderSheet.value = true
            _errorMessage.value = null
            _paragraphs.value = emptyList()
            _currentParagraphIndex.value = -1
            _foregroundTranslationStep.value = "Đang dịch tiêu đề bài viết..."
            _foregroundTranslationSteps.value = listOf("Đang dịch tiêu đề bài viết...")

            val logSteps = mutableListOf("Đang dịch tiêu đề bài viết...")
            val translatedTitle = geminiManager.translateTitle(
                title = title,
                apiKeys = settings.geminiApiKeys,
                modelName = settings.geminiModel,
                sourceLang = settings.sourceLanguage,
                targetLang = settings.targetLanguage
            )
            logSteps.add("Đã dịch tiêu đề: $title -> $translatedTitle")
            _foregroundTranslationStep.value = "Tiêu đề: $translatedTitle"
            _foregroundTranslationSteps.value = _foregroundTranslationSteps.value + "Tiêu đề: $translatedTitle"

            val initialLog = TransactionLog(
                id = logId,
                type = "Đọc ngay",
                title = translatedTitle,
                url = url,
                status = "Đang chạy",
                usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                steps = logSteps.toList(),
                geminiResponse = null,
                errorMessage = null
            )
            transactionLogRepository.addLog(initialLog)
            _translationLogs.value = transactionLogRepository.getLogs()

            val result = geminiManager.translateContent(
                text = text,
                apiKeys = settings.geminiApiKeys,
                modelName = settings.geminiModel,
                sourceLang = settings.sourceLanguage,
                targetLang = settings.targetLanguage,
                customInstructions = settings.customInstructions,
                logSteps = logSteps,
                onStepAdded = { step ->
                    _foregroundTranslationStep.value = step
                    _foregroundTranslationSteps.value = _foregroundTranslationSteps.value + step
                    viewModelScope.launch {
                        val updatedLog = TransactionLog(
                            id = logId,
                            type = "Đọc ngay",
                            title = translatedTitle,
                            url = url,
                            status = "Đang chạy",
                            usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                            steps = logSteps.toList(),
                            geminiResponse = null,
                            errorMessage = null
                        )
                        transactionLogRepository.addLog(updatedLog)
                        _translationLogs.value = transactionLogRepository.getLogs()
                    }
                },
                onContentUpdated = { totalText ->
                    viewModelScope.launch(Dispatchers.Main) {
                        val rawParagraphs = totalText.split("\n\n")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        
                        if (rawParagraphs != _paragraphs.value) {
                            val wasEmpty = _paragraphs.value.isEmpty()
                            _paragraphs.value = rawParagraphs
                            
                            if (wasEmpty && rawParagraphs.isNotEmpty()) {
                                _title.value = translatedTitle
                                _url.value = url
                                updateBookmarkStatus()
                                _currentQueueItemIndex.value = -1
                                
                                // Auto-play paragraph 0
                                _currentParagraphIndex.value = 0
                                _isPlaying.value = true
                                ttsManager.speak(rawParagraphs[0], 0, settings.ttsSpeed, settings.ttsPitch)
                            } else if (_isPlaying.value && !wasEmpty) {
                                val currentIndex = _currentParagraphIndex.value
                                val nextIndex = if (currentIndex == -2) 0 else currentIndex + 1
                                if (nextIndex in rawParagraphs.indices && !ttsManager.isSpeaking()) {
                                    _currentParagraphIndex.value = nextIndex
                                    ttsManager.speak(rawParagraphs[nextIndex], nextIndex, settings.ttsSpeed, settings.ttsPitch)
                                }
                            }
                        }
                    }
                }
            )

            _isTranslating.value = false
            result.onSuccess { translatedText ->
                val rawParagraphs = translatedText.split("\n\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                
                _paragraphs.value = rawParagraphs
                _title.value = translatedTitle
                _url.value = url
                updateBookmarkStatus()
                
                if (rawParagraphs.isNotEmpty()) {
                    val newItem = QueueItem(
                        id = java.util.UUID.randomUUID().toString(),
                        title = translatedTitle,
                        url = url,
                        paragraphs = rawParagraphs,
                        folderId = folderId
                    )
                    val allItems = _queue.value.toMutableList().apply { add(newItem) }
                    val sortedItems = sortQueueItems(allItems, _folders.value)
                    _queue.value = sortedItems
                    queueRepository.saveQueueData(_folders.value, sortedItems)

                    val newIndex = sortedItems.indexOfFirst { it.id == newItem.id }
                    if (newIndex != -1) {
                        _currentQueueItemIndex.value = newIndex
                    }

                    if (_isPlaying.value) {
                        val currentIndex = _currentParagraphIndex.value
                        val nextIndex = if (currentIndex == -2) 0 else currentIndex + 1
                        if (nextIndex in rawParagraphs.indices) {
                            if (!ttsManager.isSpeaking()) {
                                _currentParagraphIndex.value = nextIndex
                                ttsManager.speak(rawParagraphs[nextIndex], nextIndex, settings.ttsSpeed, settings.ttsPitch)
                            }
                        } else {
                            playNextParagraph(currentIndex)
                        }
                    } else if (_currentParagraphIndex.value == -1) {
                        playParagraph(0)
                    }
                } else {
                    _errorMessage.value = "Bản dịch rỗng hoặc không phân tích được đoạn văn."
                }

                val finalLog = TransactionLog(
                    id = logId,
                    type = "Đọc ngay",
                    title = translatedTitle,
                    url = url,
                    status = "Thành công",
                    usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                    steps = logSteps.toList(),
                    geminiResponse = translatedText,
                    errorMessage = null
                )
                transactionLogRepository.addLog(finalLog)
                _translationLogs.value = transactionLogRepository.getLogs()
            }.onFailure { exception ->
                val errMsg = exception.message ?: exception.localizedMessage ?: "Không xác định"
                _errorMessage.value = "Lỗi dịch thuật:\n$errMsg"

                val finalLog = TransactionLog(
                    id = logId,
                    type = "Đọc ngay",
                    title = translatedTitle,
                    url = url,
                    status = "Thất bại",
                    usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                    steps = logSteps.toList(),
                    geminiResponse = null,
                    errorMessage = errMsg
                )
                transactionLogRepository.addLog(finalLog)
                _translationLogs.value = transactionLogRepository.getLogs()
            }
        }
    }

    fun translateAndAddToQueue(text: String, title: String, url: String, folderId: String? = null) {
        if (settings.geminiApiKeys.isEmpty()) {
            val errMsg = "Vui lòng nhập API Key trong phần Cài đặt để dịch trang web."
            _errorMessage.value = errMsg
            _showReaderSheet.value = true
            
            val logSteps = mutableListOf("Lỗi khởi tạo: Danh sách khóa API Gemini trống. Vui lòng thiết lập trong Cài đặt.")
            val newLog = TransactionLog(
                type = "Hàng chờ",
                title = title,
                url = url,
                status = "Thất bại",
                usedApiKeys = emptyList(),
                steps = logSteps,
                geminiResponse = null,
                errorMessage = errMsg
            )
            viewModelScope.launch {
                transactionLogRepository.addLog(newLog)
                _translationLogs.value = transactionLogRepository.getLogs()
            }
            return
        }

        // Show a non-blocking toast indicating background translation started
        android.widget.Toast.makeText(
            getApplication(),
            "Bắt đầu dịch nền bài viết: $title...",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        val job = ActiveTranslation(
            title = title,
            url = url,
            text = text,
            status = TranslationStatus.TRANSLATING
        )
        _activeTranslations.value = _activeTranslations.value + job

        val logId = job.id
        viewModelScope.launch {
            val logSteps = mutableListOf("Bắt đầu dịch nền bài viết...")
            val translatedTitle = geminiManager.translateTitle(
                title = title,
                apiKeys = settings.geminiApiKeys,
                modelName = settings.geminiModel,
                sourceLang = settings.sourceLanguage,
                targetLang = settings.targetLanguage
            )
            logSteps.add("Đã dịch tiêu đề: $title -> $translatedTitle")
            
            // Update the job title to the translated title so the user sees it in the "Đang dịch" section
            _activeTranslations.value = _activeTranslations.value.map {
                if (it.id == job.id) it.copy(title = translatedTitle) else it
            }

            val initialLog = TransactionLog(
                id = logId,
                type = "Hàng chờ",
                title = translatedTitle,
                url = url,
                status = "Đang chạy",
                usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                steps = logSteps.toList(),
                geminiResponse = null,
                errorMessage = null
            )
            transactionLogRepository.addLog(initialLog)
            _translationLogs.value = transactionLogRepository.getLogs()

            val result = geminiManager.translateContent(
                text = text,
                apiKeys = settings.geminiApiKeys,
                modelName = settings.geminiModel,
                sourceLang = settings.sourceLanguage,
                targetLang = settings.targetLanguage,
                customInstructions = settings.customInstructions,
                logSteps = logSteps,
                onStepAdded = { step ->
                    _activeTranslations.value = _activeTranslations.value.map {
                        if (it.id == job.id) it.copy(currentStep = step) else it
                    }
                    viewModelScope.launch {
                        val updatedLog = TransactionLog(
                            id = logId,
                            type = "Hàng chờ",
                            title = translatedTitle,
                            url = url,
                            status = "Đang chạy",
                            usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                            steps = logSteps.toList(),
                            geminiResponse = null,
                            errorMessage = null
                        )
                        transactionLogRepository.addLog(updatedLog)
                        _translationLogs.value = transactionLogRepository.getLogs()
                    }
                }
            )

            result.onSuccess { translatedText ->
                val rawParagraphs = translatedText.split("\n\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                
                _activeTranslations.value = _activeTranslations.value.filter { it.id != job.id }

                if (rawParagraphs.isNotEmpty()) {
                    val newItem = QueueItem(
                        id = java.util.UUID.randomUUID().toString(),
                        title = translatedTitle,
                        url = url,
                        paragraphs = rawParagraphs,
                        folderId = folderId
                    )
                    val allItems = _queue.value.toMutableList().apply { add(newItem) }
                    val sortedItems = sortQueueItems(allItems, _folders.value)
                    _queue.value = sortedItems
                    queueRepository.saveQueueData(_folders.value, sortedItems)
                    
                    if (_currentQueueItemIndex.value == -1 && _paragraphs.value.isEmpty()) {
                        val qList = _queue.value
                        val newIndex = qList.indexOfFirst { it.id == newItem.id }
                        if (newIndex != -1) {
                            val item = qList[newIndex]
                            _currentQueueItemIndex.value = newIndex
                            _paragraphs.value = item.paragraphs
                            _title.value = item.title
                            _currentParagraphIndex.value = -1
                            _isPlaying.value = false
                        }
                    }
                    
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Đã thêm vào hàng chờ: $translatedTitle",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()

                    val finalLog = TransactionLog(
                        id = logId,
                        type = "Hàng chờ",
                        title = translatedTitle,
                        url = url,
                        status = "Thành công",
                        usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                        steps = logSteps.toList(),
                        geminiResponse = translatedText,
                        errorMessage = null
                    )
                    transactionLogRepository.addLog(finalLog)
                    _translationLogs.value = transactionLogRepository.getLogs()
                } else {
                    _activeTranslations.value = _activeTranslations.value.map {
                        if (it.id == job.id) it.copy(status = TranslationStatus.FAILED, errorMessage = "Không phân tích được đoạn văn") else it
                    }
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Lỗi: Không phân tích được đoạn văn cho bài viết: $translatedTitle",
                        android.widget.Toast.LENGTH_LONG
                    ).show()

                    val finalLog = TransactionLog(
                        id = logId,
                        type = "Hàng chờ",
                        title = translatedTitle,
                        url = url,
                        status = "Thất bại",
                        usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                        steps = logSteps.toList(),
                        geminiResponse = null,
                        errorMessage = "Không phân tích được đoạn văn"
                    )
                    transactionLogRepository.addLog(finalLog)
                    _translationLogs.value = transactionLogRepository.getLogs()
                }
            }.onFailure { exception ->
                val errMsg = exception.message ?: exception.localizedMessage ?: "Lỗi không xác định"
                _activeTranslations.value = _activeTranslations.value.map {
                    if (it.id == job.id) it.copy(
                        status = TranslationStatus.FAILED,
                        errorMessage = errMsg
                    ) else it
                }
                android.widget.Toast.makeText(
                    getApplication(),
                    "Lỗi dịch thuật bài viết \"$translatedTitle\":\n$errMsg",
                    android.widget.Toast.LENGTH_LONG
                ).show()

                val finalLog = TransactionLog(
                    id = logId,
                    type = "Hàng chờ",
                    title = translatedTitle,
                    url = url,
                    status = "Thất bại",
                    usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                    steps = logSteps.toList(),
                    geminiResponse = null,
                    errorMessage = errMsg
                )
                transactionLogRepository.addLog(finalLog)
                _translationLogs.value = transactionLogRepository.getLogs()
            }
        }
    }

    fun loadQueueItem(index: Int) {
        val qList = _queue.value
        if (index in qList.indices) {
            val item = qList[index]
            _currentQueueItemIndex.value = index
            _paragraphs.value = item.paragraphs
            _title.value = item.title
            _currentParagraphIndex.value = -1
            _isPlaying.value = false
        }
    }

    fun playQueueItem(index: Int) {
        val qList = _queue.value
        if (index in qList.indices) {
            val item = qList[index]
            _currentQueueItemIndex.value = index
            _paragraphs.value = item.paragraphs
            _title.value = item.title
            _currentParagraphIndex.value = -2 // Announces title first
            _isPlaying.value = true
            val announceText = "Bắt đầu đọc bài viết: ${item.title}"
            ttsManager.speak(announceText, -2, settings.ttsSpeed, settings.ttsPitch)
        }
    }

    fun removeQueueItem(index: Int) {
        val qList = _queue.value.toMutableList()
        if (index in qList.indices) {
            val currentIdx = _currentQueueItemIndex.value
            if (currentIdx == index) {
                pauseReading()
                _paragraphs.value = emptyList()
                _title.value = "Trình duyệt"
                _currentParagraphIndex.value = -1
                _currentQueueItemIndex.value = -1
            } else if (currentIdx > index) {
                _currentQueueItemIndex.value = currentIdx - 1
            }
            qList.removeAt(index)
            _queue.value = qList
            queueRepository.saveQueueData(_folders.value, qList)
        }
    }

    fun clearQueue() {
        pauseReading()
        _paragraphs.value = emptyList()
        _title.value = "Trình duyệt"
        _currentParagraphIndex.value = -1
        _currentQueueItemIndex.value = -1
        _queue.value = emptyList()
        _folders.value = emptyList()
        _activeTranslations.value = emptyList()
        queueRepository.clearQueue()
    }

    fun retryTranslation(job: ActiveTranslation) {
        if (settings.geminiApiKeys.isEmpty()) {
            val errMsg = "Vui lòng nhập API Key trong phần Cài đặt để dịch trang web."
            _activeTranslations.value = _activeTranslations.value.map {
                if (it.id == job.id) it.copy(
                    status = TranslationStatus.FAILED,
                    errorMessage = errMsg
                ) else it
            }
            
            val logSteps = mutableListOf("Lỗi khởi tạo: Danh sách khóa API Gemini trống. Vui lòng thiết lập trong Cài đặt.")
            val newLog = TransactionLog(
                type = "Hàng chờ",
                title = job.title,
                url = job.url,
                status = "Thất bại",
                usedApiKeys = emptyList(),
                steps = logSteps,
                geminiResponse = null,
                errorMessage = errMsg
            )
            viewModelScope.launch {
                transactionLogRepository.addLog(newLog)
                _translationLogs.value = transactionLogRepository.getLogs()
            }
            return
        }

        _activeTranslations.value = _activeTranslations.value.map {
            if (it.id == job.id) it.copy(status = TranslationStatus.TRANSLATING, errorMessage = null) else it
        }
        
        val logId = job.id
        viewModelScope.launch {
            val logSteps = mutableListOf("Bắt đầu thử lại dịch thuật...")
            val translatedTitle = geminiManager.translateTitle(
                title = job.title,
                apiKeys = settings.geminiApiKeys,
                modelName = settings.geminiModel,
                sourceLang = settings.sourceLanguage,
                targetLang = settings.targetLanguage
            )
            logSteps.add("Đã dịch tiêu đề: ${job.title} -> $translatedTitle")
            
            _activeTranslations.value = _activeTranslations.value.map {
                if (it.id == job.id) it.copy(title = translatedTitle) else it
            }

            val initialLog = TransactionLog(
                id = logId,
                type = "Hàng chờ",
                title = translatedTitle,
                url = job.url,
                status = "Đang chạy",
                usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                steps = logSteps.toList(),
                geminiResponse = null,
                errorMessage = null
            )
            transactionLogRepository.addLog(initialLog)
            _translationLogs.value = transactionLogRepository.getLogs()

            val result = geminiManager.translateContent(
                text = job.text,
                apiKeys = settings.geminiApiKeys,
                modelName = settings.geminiModel,
                sourceLang = settings.sourceLanguage,
                targetLang = settings.targetLanguage,
                customInstructions = settings.customInstructions,
                logSteps = logSteps,
                onStepAdded = { step ->
                    _activeTranslations.value = _activeTranslations.value.map {
                        if (it.id == job.id) it.copy(currentStep = step) else it
                    }
                    viewModelScope.launch {
                        val updatedLog = TransactionLog(
                            id = logId,
                            type = "Hàng chờ",
                            title = translatedTitle,
                            url = job.url,
                            status = "Đang chạy",
                            usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                            steps = logSteps.toList(),
                            geminiResponse = null,
                            errorMessage = null
                        )
                        transactionLogRepository.addLog(updatedLog)
                        _translationLogs.value = transactionLogRepository.getLogs()
                    }
                }
            )
            
            result.onSuccess { translatedText ->
                val rawParagraphs = translatedText.split("\n\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                
                if (rawParagraphs.isNotEmpty()) {
                    _activeTranslations.value = _activeTranslations.value.filter { it.id != job.id }
                    
                    val currentItem = _currentQueueItemIndex.value.let { idx ->
                        if (idx in _queue.value.indices) _queue.value[idx] else null
                    }
                    val folderId = currentItem?.folderId

                    val newItem = QueueItem(
                        id = java.util.UUID.randomUUID().toString(),
                        title = translatedTitle,
                        url = job.url,
                        paragraphs = rawParagraphs,
                        folderId = folderId
                    )
                    val allItems = _queue.value.toMutableList().apply { add(newItem) }
                    val sortedItems = sortQueueItems(allItems, _folders.value)
                    _queue.value = sortedItems
                    queueRepository.saveQueueData(_folders.value, sortedItems)
                    
                    if (_currentQueueItemIndex.value == -1 && _paragraphs.value.isEmpty()) {
                        val qList = _queue.value
                        val newIndex = qList.indexOfFirst { it.id == newItem.id }
                        if (newIndex != -1) {
                            val item = qList[newIndex]
                            _currentQueueItemIndex.value = newIndex
                            _paragraphs.value = item.paragraphs
                            _title.value = item.title
                            _currentParagraphIndex.value = -1
                            _isPlaying.value = false
                        }
                    }
                    
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Đã thêm vào hàng chờ: $translatedTitle",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
 
                    val finalLog = TransactionLog(
                        id = logId,
                        type = "Hàng chờ",
                        title = translatedTitle,
                        url = job.url,
                        status = "Thành công",
                        usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                        steps = logSteps.toList(),
                        geminiResponse = translatedText,
                        errorMessage = null
                    )
                    transactionLogRepository.addLog(finalLog)
                    _translationLogs.value = transactionLogRepository.getLogs()
                } else {
                    _activeTranslations.value = _activeTranslations.value.map {
                        if (it.id == job.id) it.copy(status = TranslationStatus.FAILED, errorMessage = "Không phân tích được đoạn văn") else it
                    }
 
                    val finalLog = TransactionLog(
                        id = logId,
                        type = "Hàng chờ",
                        title = translatedTitle,
                        url = job.url,
                        status = "Thất bại",
                        usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                        steps = logSteps.toList(),
                        geminiResponse = null,
                        errorMessage = "Không phân tích được đoạn văn"
                    )
                    transactionLogRepository.addLog(finalLog)
                    _translationLogs.value = transactionLogRepository.getLogs()
                }
            }.onFailure { exception ->
                val errMsg = exception.message ?: exception.localizedMessage ?: "Lỗi không xác định"
                _activeTranslations.value = _activeTranslations.value.map {
                    if (it.id == job.id) it.copy(
                        status = TranslationStatus.FAILED,
                        errorMessage = errMsg
                    ) else it
                }
 
                val finalLog = TransactionLog(
                    id = logId,
                    type = "Hàng chờ",
                    title = translatedTitle,
                    url = job.url,
                    status = "Thất bại",
                    usedApiKeys = settings.geminiApiKeys.map { if (it.length > 8) it.take(4) + "..." + it.takeLast(4) else "ShortKey" },
                    steps = logSteps.toList(),
                    geminiResponse = null,
                    errorMessage = errMsg
                )
                transactionLogRepository.addLog(finalLog)
                _translationLogs.value = transactionLogRepository.getLogs()
            }
        }
    }

    fun removeActiveTranslation(jobId: String) {
        _activeTranslations.value = _activeTranslations.value.filter { it.id != jobId }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val currentList = _queue.value.toMutableList()
        if (fromIndex in currentList.indices && toIndex in currentList.indices) {
            val currentIdx = _currentQueueItemIndex.value
            val itemToMove = currentList.removeAt(fromIndex)
            currentList.add(toIndex, itemToMove)
            
            if (currentIdx != -1) {
                if (currentIdx == fromIndex) {
                    _currentQueueItemIndex.value = toIndex
                } else if (fromIndex < currentIdx && toIndex >= currentIdx) {
                    _currentQueueItemIndex.value = currentIdx - 1
                } else if (fromIndex > currentIdx && toIndex <= currentIdx) {
                    _currentQueueItemIndex.value = currentIdx + 1
                }
            }
            
            _queue.value = currentList
            queueRepository.saveQueueData(_folders.value, currentList)
        }
    }

    fun createFolder(name: String): String {
        val folderId = java.util.UUID.randomUUID().toString()
        val newFolder = QueueFolder(
            id = folderId,
            name = name
        )
        val updatedFolders = _folders.value.toMutableList().apply { add(newFolder) }
        _folders.value = updatedFolders
        
        val sortedItems = sortQueueItems(_queue.value, updatedFolders)
        _queue.value = sortedItems
        
        queueRepository.saveQueueData(updatedFolders, sortedItems)
        return folderId
    }

    fun renameFolder(folderId: String, newName: String) {
        val updatedFolders = _folders.value.map {
            if (it.id == folderId) it.copy(name = newName) else it
        }
        _folders.value = updatedFolders
        queueRepository.saveQueueData(updatedFolders, _queue.value)
    }

    fun deleteFolder(folderId: String, deleteItems: Boolean) {
        val updatedFolders = _folders.value.filter { it.id != folderId }
        _folders.value = updatedFolders
        
        val currentQueue = _queue.value
        val updatedQueue = if (deleteItems) {
            val activeItemIndex = _currentQueueItemIndex.value
            val isPlayingItemDeleted = activeItemIndex != -1 && currentQueue[activeItemIndex].folderId == folderId
            if (isPlayingItemDeleted) {
                pauseReading()
                _paragraphs.value = emptyList()
                _title.value = "Trình duyệt"
                _currentParagraphIndex.value = -1
                _currentQueueItemIndex.value = -1
            }
            val remainingItems = currentQueue.filter { it.folderId != folderId }
            if (_currentQueueItemIndex.value != -1) {
                val currentActiveItem = currentQueue[_currentQueueItemIndex.value]
                _currentQueueItemIndex.value = remainingItems.indexOfFirst { it.id == currentActiveItem.id }
            }
            remainingItems
        } else {
            currentQueue.map {
                if (it.folderId == folderId) it.copy(folderId = null) else it
            }
        }
        
        val sortedItems = sortQueueItems(updatedQueue, updatedFolders)
        _queue.value = sortedItems
        
        if (_currentQueueItemIndex.value != -1) {
            val currentActiveItem = currentQueue.getOrNull(_currentQueueItemIndex.value)
            if (currentActiveItem != null) {
                _currentQueueItemIndex.value = sortedItems.indexOfFirst { it.id == currentActiveItem.id }
            }
        }
        
        queueRepository.saveQueueData(updatedFolders, sortedItems)
    }

    fun moveQueueItemToFolder(itemId: String, folderId: String?) {
        val currentQueue = _queue.value
        val activeIndex = _currentQueueItemIndex.value
        val activeItem = if (activeIndex != -1) currentQueue.getOrNull(activeIndex) else null

        val updatedQueue = currentQueue.map {
            if (it.id == itemId) it.copy(folderId = folderId) else it
        }
        
        val sortedItems = sortQueueItems(updatedQueue, _folders.value)
        _queue.value = sortedItems
        
        if (activeItem != null) {
            _currentQueueItemIndex.value = sortedItems.indexOfFirst { it.id == activeItem.id }
        }
        
        queueRepository.saveQueueData(_folders.value, sortedItems)
    }

    fun playQueueItemById(itemId: String) {
        val qList = _queue.value
        val index = qList.indexOfFirst { it.id == itemId }
        if (index != -1) {
            playQueueItem(index)
        }
    }

    fun removeQueueItemById(itemId: String) {
        val qList = _queue.value.toMutableList()
        val index = qList.indexOfFirst { it.id == itemId }
        if (index != -1) {
            val currentIdx = _currentQueueItemIndex.value
            if (currentIdx == index) {
                pauseReading()
                _paragraphs.value = emptyList()
                _title.value = "Trình duyệt"
                _currentParagraphIndex.value = -1
                _currentQueueItemIndex.value = -1
            } else if (currentIdx > index) {
                _currentQueueItemIndex.value = currentIdx - 1
            }
            qList.removeAt(index)
            _queue.value = qList
            queueRepository.saveQueueData(_folders.value, qList)
        }
    }

    fun setUrl(newUrl: String) {
        var cleanUrl = newUrl
        if (cleanUrl.contains("#googtrans")) {
            cleanUrl = cleanUrl.substringBefore("#googtrans")
        }
        _url.value = cleanUrl
        updateBookmarkStatus()
    }

    fun setTitle(newTitle: String) {
        _title.value = newTitle
    }

    fun setIsLoading(loading: Boolean) {
        _isLoading.value = loading
    }

    fun setNavigationState(canGoBack: Boolean, canGoForward: Boolean) {
        _canGoBack.value = canGoBack
        _canGoForward.value = canGoForward
    }

    fun setShowReaderSheet(show: Boolean) {
        _showReaderSheet.value = show
    }

    private fun updateBookmarkStatus() {
        val currentUrl = _url.value
        _isCurrentPageBookmarked.value = _bookmarks.value.any { it.url == currentUrl }
    }

    fun toggleBookmarkCurrentPage() {
        val currentUrl = _url.value
        val currentTitle = _title.value
        val currentList = _bookmarks.value.toMutableList()
        val existingIndex = currentList.indexOfFirst { it.url == currentUrl }
        
        if (existingIndex != -1) {
            currentList.removeAt(existingIndex)
        } else {
            currentList.add(
                BookmarkItem(
                    id = java.util.UUID.randomUUID().toString(),
                    title = currentTitle,
                    url = currentUrl
                )
            )
        }
        
        _bookmarks.value = currentList
        bookmarkRepository.saveBookmarks(currentList)
        updateBookmarkStatus()
    }

    fun deleteBookmark(item: BookmarkItem) {
        val currentList = _bookmarks.value.filter { it.id != item.id }
        _bookmarks.value = currentList
        bookmarkRepository.saveBookmarks(currentList)
        updateBookmarkStatus()
    }

    fun loadUrlInBrowser(newUrl: String) {
        _navigationRequest.tryEmit(newUrl)
    }

    fun updateTtsSettings(speed: Float, pitch: Float, engine: String) {
        settings.ttsSpeed = speed
        settings.ttsPitch = pitch
        
        val engineChanged = settings.ttsEngine != engine
        if (engineChanged) {
            settings.ttsEngine = engine
            ttsManager.reinitialize()
        }
        
        // If speaking, restart current paragraph with new settings
        if (_isPlaying.value && _currentParagraphIndex.value in _paragraphs.value.indices) {
            playParagraph(_currentParagraphIndex.value)
        }
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearTranslationLogs() {
        viewModelScope.launch {
            transactionLogRepository.clearLogs()
            _translationLogs.value = emptyList()
        }
    }

    fun checkAppUpdate() {
        viewModelScope.launch {
            val updateManager = UpdateManager(getApplication())
            val configUrl = settings.updateConfigUrl
            updateManager.checkUpdate(configUrl).onSuccess { info ->
                val currentCode = updateManager.currentVersionCode
                val resolverResult = com.example.webreader.data.UpdateCheckResolver.resolve(currentCode, info)
                if (resolverResult.showDialog) {
                    _updateInfo.value = info
                    _isForceUpdate.value = resolverResult.isForce
                    _showUpdateDialog.value = true
                }
            }.onFailure {
                // Thất bại thì bỏ qua (offline hoặc ngoại tuyến, tránh chặn ứng dụng)
            }
        }
    }

    fun dismissUpdateDialog() {
        _showUpdateDialog.value = false
    }

    fun signInWithGoogle(idToken: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val result = authManager.signInWithGoogle(idToken)
            if (result.isSuccess) {
                onSuccess()
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Đăng nhập thất bại"
                onError(errorMsg)
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authManager.signOut()
        }
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
        if (activeInstance == this) {
            activeInstance = null
        }
        ttsManager.shutdown()
    }
}

enum class TranslationStatus {
    TRANSLATING,
    FAILED
}

data class ActiveTranslation(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val url: String,
    val text: String,
    val status: TranslationStatus,
    val errorMessage: String? = null,
    val currentStep: String? = null
)
