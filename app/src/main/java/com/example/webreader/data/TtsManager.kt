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

    private fun initializeTts() {
        val selectedEngine = settings.ttsEngine
        val initListener = TextToSpeech.OnInitListener { status ->
            if (status == TextToSpeech.SUCCESS) {
                val localeVi = Locale("vi", "VN")
                val result = tts?.setLanguage(localeVi)
                
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TtsManager", "Tiếng Việt không được hỗ trợ trên thiết bị này.")
                    _isLanguageSupported.value = false
                } else {
                    Log.d("TtsManager", "Đã cấu hình tiếng Việt thành công cho TTS.")
                    _isLanguageSupported.value = true
                }
                
                _isInitialized.value = true
                setupUtteranceListener()
            } else {
                Log.e("TtsManager", "Không thể khởi tạo TextToSpeech.")
                _isInitialized.value = false
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

    private fun setupUtteranceListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                utteranceId?.toIntOrNull()?.let { index ->
                    onParagraphStartListener?.invoke(index)
                }
            }

            override fun onDone(utteranceId: String?) {
                utteranceId?.toIntOrNull()?.let { index ->
                    onParagraphDoneListener?.invoke(index)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                onErrorListener?.invoke("Lỗi giọng đọc ở đoạn $utteranceId")
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                onErrorListener?.invoke("Lỗi giọng đọc (Mã lỗi: $errorCode) ở đoạn $utteranceId")
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
        tts?.apply {
            setSpeechRate(speed)
            setPitch(pitch)
            val params = android.os.Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, paragraphIndex.toString())
            }
            speak(text, TextToSpeech.QUEUE_FLUSH, params, paragraphIndex.toString())
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
    }
}
