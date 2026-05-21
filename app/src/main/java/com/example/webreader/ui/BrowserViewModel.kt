package com.example.webreader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.webreader.data.GeminiManager
import com.example.webreader.data.SettingsRepository
import com.example.webreader.data.TtsManager
import com.example.webreader.data.QueueItem
import com.example.webreader.data.QueueRepository
import com.example.webreader.data.BookmarkItem
import com.example.webreader.data.BookmarkRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

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

    private val _queue = MutableStateFlow<List<QueueItem>>(emptyList())
    val queue: StateFlow<List<QueueItem>> = _queue

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

    init {
        activeInstance = this
        _queue.value = queueRepository.getQueue()
        _bookmarks.value = bookmarkRepository.getBookmarks()
        updateBookmarkStatus()
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
    }

    private fun playNextParagraph(completedIndex: Int) {
        viewModelScope.launch(Dispatchers.Default) {
            val nextIndex = if (completedIndex == -2) 0 else completedIndex + 1
            if (nextIndex in _paragraphs.value.indices) {
                _currentParagraphIndex.value = nextIndex
                val text = _paragraphs.value[nextIndex]
                ttsManager.speak(text, nextIndex, settings.ttsSpeed, settings.ttsPitch)
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

    fun translateWebpage(text: String, title: String, url: String) {
        if (settings.geminiApiKeys.isEmpty()) {
            _errorMessage.value = "Vui lòng nhập API Key trong phần Cài đặt để dịch trang web."
            _showReaderSheet.value = true // Show reader sheet to display the error and prompt user
            return
        }

        viewModelScope.launch {
            _isTranslating.value = true
            _showReaderSheet.value = true
            _errorMessage.value = null
            _paragraphs.value = emptyList()
            _currentParagraphIndex.value = -1

            val result = geminiManager.translateToVietnamese(
                text = text,
                apiKeys = settings.geminiApiKeys,
                modelName = settings.geminiModel
            )

            _isTranslating.value = false
            result.onSuccess { translatedText ->
                val rawParagraphs = translatedText.split("\n\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                
                _paragraphs.value = rawParagraphs
                _title.value = title
                _url.value = url
                updateBookmarkStatus()
                if (rawParagraphs.isNotEmpty()) {
                    _currentQueueItemIndex.value = -1
                    playParagraph(0)
                } else {
                    _errorMessage.value = "Bản dịch rỗng hoặc không phân tích được đoạn văn."
                }
            }.onFailure { exception ->
                _errorMessage.value = "Lỗi dịch thuật:\n${exception.message ?: exception.localizedMessage ?: "Không xác định"}"
            }
        }
    }

    fun translateAndAddToQueue(text: String, title: String, url: String) {
        if (settings.geminiApiKeys.isEmpty()) {
            _errorMessage.value = "Vui lòng nhập API Key trong phần Cài đặt để dịch trang web."
            _showReaderSheet.value = true
            return
        }

        // Show a non-blocking toast indicating background translation started
        android.widget.Toast.makeText(
            getApplication(),
            "Bắt đầu dịch nền bài viết: $title...",
            android.widget.Toast.LENGTH_SHORT
        ).show()

        viewModelScope.launch {
            val result = geminiManager.translateToVietnamese(
                text = text,
                apiKeys = settings.geminiApiKeys,
                modelName = settings.geminiModel
            )

            result.onSuccess { translatedText ->
                val rawParagraphs = translatedText.split("\n\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                
                if (rawParagraphs.isNotEmpty()) {
                    val newItem = QueueItem(
                        id = java.util.UUID.randomUUID().toString(),
                        title = title,
                        url = url,
                        paragraphs = rawParagraphs
                    )
                    val updatedQueue = _queue.value.toMutableList().apply { add(newItem) }
                    _queue.value = updatedQueue
                    queueRepository.saveQueue(updatedQueue)
                    
                    if (_currentQueueItemIndex.value == -1 && _paragraphs.value.isEmpty()) {
                        // Load the item if queue is empty
                        val qList = _queue.value
                        if (updatedQueue.lastIndex in qList.indices) {
                            val item = qList[updatedQueue.lastIndex]
                            _currentQueueItemIndex.value = updatedQueue.lastIndex
                            _paragraphs.value = item.paragraphs
                            _title.value = item.title
                            _currentParagraphIndex.value = -1
                            _isPlaying.value = false
                        }
                    }
                    
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Đã thêm vào hàng chờ: $title",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                } else {
                    android.widget.Toast.makeText(
                        getApplication(),
                        "Lỗi: Không phân tích được đoạn văn cho bài viết: $title",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }.onFailure { exception ->
                android.widget.Toast.makeText(
                    getApplication(),
                    "Lỗi dịch thuật bài viết \"$title\":\n${exception.message ?: exception.localizedMessage ?: "Không xác định"}",
                    android.widget.Toast.LENGTH_LONG
                ).show()
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
            queueRepository.saveQueue(qList)
        }
    }

    fun clearQueue() {
        pauseReading()
        _paragraphs.value = emptyList()
        _title.value = "Trình duyệt"
        _currentParagraphIndex.value = -1
        _currentQueueItemIndex.value = -1
        _queue.value = emptyList()
        queueRepository.clearQueue()
    }

    fun setUrl(newUrl: String) {
        _url.value = newUrl
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

    override fun onCleared() {
        super.onCleared()
        if (activeInstance == this) {
            activeInstance = null
        }
        ttsManager.shutdown()
    }
}
