package com.example.webreader.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class GeminiManager {

    suspend fun translateToVietnamese(
        text: String,
        apiKey: String,
        modelName: String = "gemini-1.5-flash"
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            if (apiKey.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Khóa API Gemini không được để trống. Vui lòng thiết lập trong Cài đặt."))
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
                Result.success(translatedText)
            } else {
                Result.failure(Exception("Gemini API không trả về nội dung dịch."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
