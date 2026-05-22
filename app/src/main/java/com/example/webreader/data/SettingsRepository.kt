package com.example.webreader.data

import android.content.Context
import android.content.SharedPreferences

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("webreader_prefs", Context.MODE_PRIVATE)

    var geminiApiKey: String
        get() = prefs.getString("gemini_api_key", "") ?: ""
        set(value) = prefs.edit().putString("gemini_api_key", value).apply()

    val geminiApiKeys: List<String>
        get() {
            val raw = geminiApiKey
            if (raw.isBlank()) return emptyList()
            return raw.split(Regex("[,\n]"))
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        }

    var geminiModel: String
        get() = prefs.getString("gemini_model", "gemini-1.5-flash") ?: "gemini-1.5-flash"
        set(value) = prefs.edit().putString("gemini_model", value).apply()

    var ttsSpeed: Float
        get() = prefs.getFloat("tts_speed", 1.0f)
        set(value) = prefs.edit().putFloat("tts_speed", value).apply()

    var ttsPitch: Float
        get() = prefs.getFloat("tts_pitch", 1.0f)
        set(value) = prefs.edit().putFloat("tts_pitch", value).apply()

    var ttsEngine: String
        get() = prefs.getString("tts_engine", "") ?: ""
        set(value) = prefs.edit().putString("tts_engine", value).apply()

    var updateConfigUrl: String
        get() = prefs.getString("update_config_url", "https://raw.githubusercontent.com/haianh1991/apptts/main/version.json") ?: "https://raw.githubusercontent.com/haianh1991/apptts/main/version.json"
        set(value) = prefs.edit().putString("update_config_url", value).apply()

    val defaultCustomInstructions: String = "Đối với văn bản gốc là Tiếng Trung (truyện chữ Trung Quốc), hãy giữ nguyên cách xưng hô, đại từ nhân xưng, danh từ xưng gọi và các thuật ngữ Hán-Việt cổ truyền thống (ví dụ: ngươi - ta, hắn, ả, chúng, ca, muội, tiểu muội, đại ca, nhị ca, thúc, bá, a di, thái thái, nãi nãi, công công, bà bà, tỷ tỷ, muội muội, đại nhân, phu nhân, sư phụ, thẩm, nữ nhân, nam nhân, Gia Trấn, bài phường...). Tuyệt đối không dịch các từ này thành từ ngữ hiện đại thuần Việt như \"bạn - tôi\", \"anh ấy\", \"cô ấy\", \"bà\", \"thím\", \"người phụ nữ\", \"người đàn ông\", \"thị trấn gia đình\", \"cổng chào\"."

    var sourceLanguage: String
        get() = prefs.getString("source_language", "Auto") ?: "Auto"
        set(value) = prefs.edit().putString("source_language", value).apply()

    var targetLanguage: String
        get() = prefs.getString("target_language", "Tiếng Việt") ?: "Tiếng Việt"
        set(value) = prefs.edit().putString("target_language", value).apply()

    var customInstructions: String
        get() = prefs.getString("custom_instructions", defaultCustomInstructions) ?: defaultCustomInstructions
        set(value) = prefs.edit().putString("custom_instructions", value).apply()

    var appDisplayLanguage: String
        get() = prefs.getString("app_display_language", "vi") ?: "vi"
        set(value) = prefs.edit().putString("app_display_language", value).apply()
}


