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
            Bạn là một trợ lý dịch thuật và trích xuất nội dung thông minh.
            Nhiệm vụ của bạn là nhận văn bản thô được trích xuất từ một trang web (có chứa nhiều thành phần thừa như menu điều hướng, quảng cáo xen kẽ, các nút bấm chuyển trang, bình luận bên lề).
            Hãy thực hiện các bước sau một cách cẩn thận:
            1. Nhận diện phần nội dung bài viết/chương truyện chính trong văn bản thô. Loại bỏ hoàn toàn các thành phần thừa, quảng cáo xen kẽ giữa các câu, các nút bấm (như "Chương sau", "Mục lục", "Trang chủ"), và bình luận không liên quan của độc giả.
            2. Dịch phần nội dung chính vừa lọc được sang tiếng Việt một cách tự nhiên, mượt mà, trôi chảy nhất và chuẩn ngữ cảnh văn học/báo chí Việt Nam.
            3. Giữ nguyên cấu trúc phân đoạn (phân tách rõ ràng bằng các dòng trống \n\n).
            4. Tuyệt đối không thêm bất kỳ văn bản giới thiệu hay chú thích nào khác (như "Dưới đây là bản dịch..."). Chỉ trả về văn bản dịch sạch của nội dung chính.
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
                errors.add("[$keySnippet]: ${getDetailedErrorMessage(e)}")
            }
        }
        
        Result.failure(Exception("Dịch thuật thất bại với toàn bộ các API Key đã thử:\n" + errors.joinToString("\n")))
    }

    private fun getDetailedErrorMessage(e: Throwable): String {
        val sb = StringBuilder()
        val className = e::class.simpleName ?: e.javaClass.simpleName
        sb.append("$className: ${e.message ?: "Không có thông tin chi tiết"}")
        
        var cause = e.cause
        var depth = 0
        while (cause != null && depth < 5) {
            val causeClassName = cause::class.simpleName ?: cause.javaClass.simpleName
            sb.append("\n  -> Nguyên nhân: $causeClassName: ${cause.message ?: "Không có thông tin"}")
            cause = cause.cause
            depth++
        }
        return sb.toString()
    }
}
