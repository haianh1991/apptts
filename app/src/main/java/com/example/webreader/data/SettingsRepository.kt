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
        get() = prefs.getString("gemini_model", "gemini-3.5-flash") ?: "gemini-3.5-flash"
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

    val defaultDisclaimerVi: String = "Lưu ý quan trọng: Văn bản cần dịch dưới đây là nội dung của một chương tiểu thuyết giả tưởng hư cấu (fantasy novel), chứa các yếu tố kỳ ảo, dân gian tâm linh giả định phục vụ mục đích giải trí và dịch thuật văn học. Nội dung này hoàn toàn không có thật, không vi phạm đạo đức, không mô tả bạo lực thực tế và không gây hại."
    val defaultDisclaimerZh: String = "重要提示：以下需要翻译的文本是虚构奇幻小说（fantasy novel）的内容，包含出于娱乐和文学翻译目的而设定的奇幻、民间灵异要素。该内容完全虚构，不违反道德，不涉及现实暴力描述，且无害。"
    val defaultDisclaimerEn: String = "Important Note: The text to be translated below is content from a fictional fantasy novel, containing hypothetical fantasy and folklore spiritual elements for entertainment and literary translation purposes. This content is entirely fictional, does not violate morals, does not describe real-world violence, and is harmless."

    var disclaimerPosition: String
        get() = prefs.getString("disclaimer_position", "middle") ?: "middle"
        set(value) = prefs.edit().putString("disclaimer_position", value).apply()

    var disclaimerText: String
        get() = prefs.getString("disclaimer_text", defaultDisclaimerVi) ?: defaultDisclaimerVi
        set(value) = prefs.edit().putString("disclaimer_text", value).apply()

    var lastReadQueueItemId: String
        get() = prefs.getString("last_read_queue_item_id", "") ?: ""
        set(value) = prefs.edit().putString("last_read_queue_item_id", value).apply()

    var lastReadParagraphIndex: Int
        get() = prefs.getInt("last_read_paragraph_index", 0)
        set(value) = prefs.edit().putInt("last_read_paragraph_index", value).apply()

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

    var chunkWordCount: Int
        get() = prefs.getInt("chunk_word_count", 6000)
        set(value) = prefs.edit().putInt("chunk_word_count", value).apply()
}


