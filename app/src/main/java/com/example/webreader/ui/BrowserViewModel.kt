package com.example.webreader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.webreader.data.GeminiManager
import com.example.webreader.data.SettingsRepository
import com.example.webreader.data.TtsManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class BrowserViewModel(application: Application) : AndroidViewModel(application) {
    val settings = SettingsRepository(application)
    private val geminiManager = GeminiManager()
    val ttsManager = TtsManager(application)

    private val _url = MutableStateFlow("https://news.google.com")
    val url: StateFlow<String> = _url

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
        viewModelScope.launch {
            val nextIndex = completedIndex + 1
            if (nextIndex < _paragraphs.value.size) {
                _currentParagraphIndex.value = nextIndex
                val text = _paragraphs.value[nextIndex]
                ttsManager.speak(text, nextIndex, settings.ttsSpeed, settings.ttsPitch)
            } else {
                _isPlaying.value = false
                _currentParagraphIndex.value = -1
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
        if (paragraphsList.isNotEmpty()) {
            val targetIndex = if (index in paragraphsList.indices) index else 0
            _currentParagraphIndex.value = targetIndex
            _isPlaying.value = true
            ttsManager.speak(paragraphsList[targetIndex], targetIndex, settings.ttsSpeed, settings.ttsPitch)
        }
    }

    fun playNext() {
        val index = _currentParagraphIndex.value
        if (index + 1 < _paragraphs.value.size) {
            playParagraph(index + 1)
        }
    }

    fun playPrevious() {
        val index = _currentParagraphIndex.value
        if (index - 1 >= 0) {
            playParagraph(index - 1)
        }
    }

    fun translateWebpage(text: String) {
        if (settings.geminiApiKey.isBlank()) {
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
                apiKey = settings.geminiApiKey,
                modelName = settings.geminiModel
            )

            _isTranslating.value = false
            result.onSuccess { translatedText ->
                val rawParagraphs = translatedText.split("\n\n")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                
                _paragraphs.value = rawParagraphs
                if (rawParagraphs.isNotEmpty()) {
                    playParagraph(0)
                } else {
                    _errorMessage.value = "Bản dịch rỗng hoặc không phân tích được đoạn văn."
                }
            }.onFailure { exception ->
                _errorMessage.value = "Lỗi dịch thuật: ${exception.localizedMessage ?: "Không xác định"}"
            }
        }
    }

    fun setUrl(newUrl: String) {
        _url.value = newUrl
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
        if (!show) {
            pauseReading()
        }
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
        ttsManager.shutdown()
    }
}
