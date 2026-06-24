package com.example.webreader.data

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class TtsManager(private val context: Context) {

    private val settings = SettingsRepository(context)
    private var tts: TextToSpeech? = null
    
    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized

    private val _isLanguageSupported = MutableStateFlow<Boolean?>(null)
    val isLanguageSupported: StateFlow<Boolean?> = _isLanguageSupported

    private var onParagraphStartListener: ((Int) -> Unit)? = null
    private var onParagraphDoneListener: ((Int) -> Unit)? = null
    private var onErrorListener: ((String) -> Unit)? = null

    init {
        initializeTts()
    }

    private fun configureTtsAfterInit(): Boolean {
        val localeVi = Locale("vi", "VN")
        val result = tts?.setLanguage(localeVi)
        return if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TtsManager", "Tiếng Việt không được hỗ trợ trên thiết bị này.")
            _isLanguageSupported.value = false
            false
        } else {
            Log.d("TtsManager", "Đã cấu hình tiếng Việt thành công cho TTS.")
            _isLanguageSupported.value = true
            true
        }
    }

    private fun initializeTts() {
        val selectedEngine = settings.ttsEngine
        val initListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                configureTtsAfterInit()
                _isInitialized.value = true
                setupUtteranceListener()
            } else {
                Log.e("TtsManager", "Không thể khởi tạo TextToSpeech với engine: $selectedEngine. Thử lại với mặc định.")
                if (selectedEngine.isNotBlank()) {
                    tts = TextToSpeech(context) { defaultStatus ->
                        if (defaultStatus == TextToSpeech.SUCCESS) {
                            configureTtsAfterInit()
                            _isInitialized.value = true
                            setupUtteranceListener()
                        } else {
                            _isInitialized.value = false
                        }
                    }
                } else {
                    _isInitialized.value = false
                }
            }
        }

        tts = if (selectedEngine.isBlank()) {
            TextToSpeech(context, initListener)
        } else {
            TextToSpeech(context, initListener, selectedEngine)
        }
    }

    fun reinitialize() {
        shutdown()
        initializeTts()
    }

    fun getAvailableTtsEngines(): List<TextToSpeech.EngineInfo> {
        val currentTts = tts
        if (currentTts != null) {
            return currentTts.engines
        }
        val tempTts = TextToSpeech(context, null)
        val engines = tempTts.engines
        tempTts.shutdown()
        return engines
    }

    private fun splitTextIntoChunks(text: String, maxLength: Int = 1000): List<String> {
        if (text.length <= maxLength) return listOf(text)
        
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()
        val sentenceBoundaries = setOf('.', '?', '!', '\n', ';', '。', '？', '！', '；')
        
        var i = 0
        val len = text.length
        while (i < len) {
            val char = text[i]
            currentChunk.append(char)
            
            if (sentenceBoundaries.contains(char)) {
                // Consume any trailing whitespace or quotes/brackets
                while (i + 1 < len && (text[i + 1] == ' ' || text[i + 1] == '"' || text[i + 1] == '\'' || text[i + 1] == ')' || text[i + 1] == ']' || text[i + 1] == '»' || text[i + 1] == '”')) {
                    i++
                    currentChunk.append(text[i])
                }
                chunks.add(currentChunk.toString())
                currentChunk = StringBuilder()
            } else if (currentChunk.length >= maxLength) {
                // Fallback if no punctuation: find the last space to split
                val lastSpaceIdx = currentChunk.lastIndexOf(" ")
                if (lastSpaceIdx > maxLength / 2) {
                    val part1 = currentChunk.substring(0, lastSpaceIdx)
                    val part2 = currentChunk.substring(lastSpaceIdx)
                    chunks.add(part1)
                    currentChunk = StringBuilder(part2)
                } else {
                    chunks.add(currentChunk.toString())
                    currentChunk = StringBuilder()
                }
            }
            i++
        }
        
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        
        return chunks.map { it.trim() }.filter { it.isNotEmpty() }
    }

    fun openTtsSettings() {
        try {
            val intent = android.content.Intent("com.android.settings.TTS_SETTINGS").apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("TtsManager", "Không thể mở cài đặt TTS: ${e.message}", e)
        }
    }

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                if (utteranceId == null) return
                val parts = utteranceId.split("_")
                if (parts.size == 3) {
                    val paragraphIndex = parts[0].toIntOrNull()
                    val chunkIndex = parts[1].toIntOrNull()
                    if (paragraphIndex != null && chunkIndex == 0) {
                        onParagraphStartListener?.invoke(paragraphIndex)
                    }
                } else {
                    utteranceId.toIntOrNull()?.let { index ->
                        onParagraphStartListener?.invoke(index)
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                if (utteranceId == null) return
                val parts = utteranceId.split("_")
                if (parts.size == 3) {
                    val paragraphIndex = parts[0].toIntOrNull()
                    val chunkIndex = parts[1].toIntOrNull()
                    val totalChunks = parts[2].toIntOrNull()
                    if (paragraphIndex != null && chunkIndex != null && totalChunks != null) {
                        if (chunkIndex == totalChunks - 1) {
                            onParagraphDoneListener?.invoke(paragraphIndex)
                        }
                    }
                } else {
                    utteranceId.toIntOrNull()?.let { index ->
                        onParagraphDoneListener?.invoke(index)
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                val paragraphId = utteranceId?.split("_")?.firstOrNull() ?: utteranceId
                onErrorListener?.invoke("Lỗi giọng đọc ở đoạn $paragraphId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                val paragraphId = utteranceId?.split("_")?.firstOrNull() ?: utteranceId
                if (errorCode == -8) {
                    onErrorListener?.invoke("TTS_ERROR_NOT_INSTALLED_YET")
                } else {
                    onErrorListener?.invoke("Lỗi giọng đọc (Mã lỗi: $errorCode) ở đoạn $paragraphId")
                }
            }
        })
    }

    fun setCallbacks(
        onStart: (Int) -> Unit,
        onDone: (Int) -> Unit,
        onError: (String) -> Unit
    ) {
        onParagraphStartListener = onStart
        onParagraphDoneListener = onDone
        onErrorListener = onError
    }

    fun speak(text: String, paragraphIndex: Int, speed: Float = 1.0f, pitch: Float = 1.0f) {
        val notificationText = if (text.length > 80) text.substring(0, 77) + "..." else text
        TtsService.start(context, notificationText)

        tts?.apply {
            setSpeechRate(speed)
            setPitch(pitch)
            
            val chunks = splitTextIntoChunks(text, maxLength = 1000)
            if (chunks.isEmpty()) {
                val params = android.os.Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, paragraphIndex.toString())
                }
                speak("", TextToSpeech.QUEUE_FLUSH, params, paragraphIndex.toString())
                return
            }
            
            val total = chunks.size
            for (i in 0 until total) {
                val chunkText = chunks[i]
                val queueMode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                val utteranceId = "${paragraphIndex}_${i}_$total"
                val params = android.os.Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }
                speak(chunkText, queueMode, params, utteranceId)
            }
        }
    }

    fun isSpeaking(): Boolean {
        return tts?.isSpeaking == true
    }

    fun stop() {
        tts?.stop()
        TtsService.stop(context, fromApp = true)
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        TtsService.stop(context, fromApp = true)
    }
}
