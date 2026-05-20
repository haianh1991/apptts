package com.example.webreader.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiManager {

    suspend fun translateToVietnamese(
        text: String,
        apiKeys: List<String>,
        modelName: String = "gemini-1.5-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKeys.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("Danh sách khóa API Gemini trống. Vui lòng thiết lập trong Cài đặt."))
        }
        if (text.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Không tìm thấy nội dung văn bản để dịch."))
        }

        val systemInstruction = """
            Bạn là một dịch giả chuyên nghiệp. Hãy dịch nội dung trang web sau sang tiếng Việt một cách tự nhiên, mượt mà và trôi chảy.
            YÊU CẦU:
            1. Hãy giữ nguyên cấu trúc các đoạn văn bản (phân tách rõ ràng giữa các đoạn) để làm nổi bật cấu trúc văn bản.
            2. Không thêm bất kỳ phần tự giới thiệu hay chú thích nào (như "Dưới đây là bản dịch..."). Chỉ trả về nội dung văn bản đã dịch.
            3. Đảm bảo ngôn ngữ dịch phù hợp với ngữ cảnh đọc báo/tin tức Việt Nam.
        """.trimIndent()

        val errors = mutableListOf<String>()
        for ((index, apiKey) in apiKeys.withIndex()) {
            try {
                val generativeModel = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey,
                    systemInstruction = content {
                        text(systemInstruction)
                    }
                )

                val prompt = "Dưới đây là văn bản trang web cần dịch sang tiếng Việt:\n\n$text"
                val response = generativeModel.generateContent(prompt)
                val translatedText = response.text
                if (translatedText != null) {
                    return@withContext Result.success(translatedText)
                } else {
                    throw Exception("Gemini API không trả về nội dung dịch.")
                }
            } catch (e: Exception) {
                val keySnippet = if (apiKey.length > 8) {
                    apiKey.take(4) + "..." + apiKey.takeLast(4)
                } else {
                    "Key ${index + 1}"
                }
                errors.add("[$keySnippet]: ${e.localizedMessage ?: "Lỗi không xác định"}")
            }
        }
        
        Result.failure(Exception("Dịch thuật thất bại với toàn bộ các API Key đã thử:\n" + errors.joinToString("\n")))
    }
}
