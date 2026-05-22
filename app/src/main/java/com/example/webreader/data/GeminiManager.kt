package com.example.webreader.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class GeminiManager {

    suspend fun translateTitle(
        title: String,
        apiKeys: List<String>,
        modelName: String = "gemini-1.5-flash"
    ): String = withContext(Dispatchers.IO) {
        if (apiKeys.isEmpty() || title.isBlank()) return@withContext title
        var currentKeyIndex = 0
        val keysCount = apiKeys.size
        for (attempt in 0 until keysCount) {
            val keyIdx = (currentKeyIndex + attempt) % keysCount
            val apiKey = apiKeys[keyIdx]
            try {
                val generativeModel = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey,
                    requestOptions = RequestOptions(timeout = 15.seconds),
                    systemInstruction = content {
                        text("Bạn là trợ lý dịch thuật. Hãy dịch tiêu đề chương truyện hoặc bài viết sang tiếng Việt một cách tự nhiên, ngắn gọn và chính xác nhất. Chỉ trả về bản dịch tiêu đề, không giải thích, không thêm dấu ngoặc kép hay bất kỳ thông tin nào khác.")
                    }
                )
                val response = generativeModel.generateContent(title)
                val translated = response.text?.trim()
                if (!translated.isNullOrEmpty()) {
                    return@withContext translated
                }
            } catch (e: Exception) {
                // Ignore and try next key
            }
        }
        return@withContext title
    }

    suspend fun translateToVietnamese(
        text: String,
        apiKeys: List<String>,
        modelName: String = "gemini-1.5-flash",
        logSteps: MutableList<String>,
        onStepAdded: ((String) -> Unit)? = null,
        onContentUpdated: ((String) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        fun addStep(step: String) {
            logSteps.add(step)
            onStepAdded?.invoke(step)
        }

        if (apiKeys.isEmpty()) {
            val errMsg = "Danh sách khóa API Gemini trống. Vui lòng thiết lập trong Cài đặt."
            addStep("Lỗi khởi tạo: $errMsg")
            return@withContext Result.failure(IllegalArgumentException(errMsg))
        }
        if (text.isBlank()) {
            val errMsg = "Không tìm thấy nội dung văn bản để dịch."
            addStep("Lỗi khởi tạo: $errMsg")
            return@withContext Result.failure(IllegalArgumentException(errMsg))
        }
        val chunks = splitTextIntoChunks(text, 8000)
        val totalWords = countWords(text)
        addStep("Khởi chạy tiến trình dịch thuật. Kích thước văn bản gốc: ${text.length} ký tự (khoảng $totalWords từ), chia thành ${chunks.size} phần.")
        addStep("Sử dụng mô hình AI: $modelName")

        val systemInstruction = """
            Bạn là một trợ lý dịch thuật và trích xuất nội dung thông minh.
            Nhiệm vụ của bạn là nhận văn bản thô được trích xuất từ một trang web (có chứa nhiều thành phần thừa như menu điều hướng, quảng cáo xen kẽ, các nút bấm chuyển trang, bình luận bên lề).
            Hãy thực hiện các bước sau một cách cẩn thận:
            1. Nhận diện phần nội dung bài viết/chương truyện chính trong văn bản thô. Loại bỏ hoàn toàn các thành phần thừa, quảng cáo xen kẽ giữa các câu, các nút bấm (như "Chương sau", "Mục lục", "Trang chủ"), và bình luận không liên quan của độc giả.
            2. Dịch phần nội dung chính vừa lọc được sang tiếng Việt một cách tự nhiên, mượt mà, trôi chảy nhất và chuẩn ngữ cảnh văn học/báo chí Việt Nam.
            3. Đối với văn bản gốc là Tiếng Trung (truyện chữ Trung Quốc), hãy giữ nguyên cách xưng hô, đại từ nhân xưng, danh từ xưng gọi và các thuật ngữ Hán-Việt cổ truyền thống (ví dụ: ngươi - ta, hắn, ả, chúng, ca, muội, tiểu muội, đại ca, nhị ca, thúc, bá, a di, thái thái, nãi nãi, công công, bà bà, tỷ tỷ, muội muội, đại nhân, phu nhân, sư phụ, thẩm, nữ nhân, nam nhân, Gia Trấn, bài phường...). Tuyệt đối không dịch các từ này thành từ ngữ hiện đại thuần Việt như "bạn - tôi", "anh ấy", "cô ấy", "bà", "thím", "người phụ nữ", "người đàn ông", "thị trấn gia đình", "cổng chào".
            4. Giữ nguyên cấu trúc phân đoạn (phân tách rõ ràng bằng các dòng trống \n\n).
            5. Tuyệt đối không thêm bất kỳ văn bản giới thiệu hay chú thích nào khác (như "Dưới đây là văn bản dịch..."). Chỉ trả về văn bản dịch sạch của nội dung chính.
        """.trimIndent()

        val translatedChunks = mutableListOf<String>()
        var currentKeyIndex = 0

        for ((chunkIndex, chunk) in chunks.withIndex()) {
            val chunkWords = countWords(chunk)
            if (chunks.size > 1) {
                addStep("Đang dịch phần ${chunkIndex + 1}/${chunks.size} (Kích thước: ${chunk.length} ký tự, khoảng $chunkWords từ)...")
            }

            var chunkSuccess = false
            var chunkResult = ""
            val chunkErrors = mutableListOf<String>()
            val keysCount = apiKeys.size

            for (attempt in 0 until keysCount) {
                val keyIdx = (currentKeyIndex + attempt) % keysCount
                val apiKey = apiKeys[keyIdx]
                val keySnippet = if (apiKey.length > 8) {
                    apiKey.take(4) + "..." + apiKey.takeLast(4)
                } else {
                    "Key ${keyIdx + 1}"
                }

                if (chunks.size > 1) {
                    addStep("Đang thử phần ${chunkIndex + 1}/${chunks.size} với API Key số ${keyIdx + 1} ($keySnippet)...")
                } else {
                    addStep("Đang thử dịch với API Key số ${keyIdx + 1} ($keySnippet)...")
                }

                try {
                    val generativeModel = GenerativeModel(
                        modelName = modelName,
                        apiKey = apiKey,
                        requestOptions = RequestOptions(timeout = 180.seconds),
                        systemInstruction = content {
                            text(systemInstruction)
                        }
                    )

                    val prompt = "Dưới đây là văn bản trang web cần dịch sang tiếng Việt:\n\n$chunk"
                    val responseStream = generativeModel.generateContentStream(prompt)
                    val chunkBuilder = StringBuilder()
                    responseStream.collect { response ->
                        val chunkText = response.text
                        if (chunkText != null) {
                            chunkBuilder.append(chunkText)
                            val currentTotalText = (translatedChunks + chunkBuilder.toString()).joinToString("\n\n")
                            onContentUpdated?.invoke(currentTotalText)
                        }
                    }
                    val translatedText = chunkBuilder.toString()
                    if (translatedText.isNotEmpty()) {
                        chunkResult = translatedText
                        chunkSuccess = true
                        currentKeyIndex = keyIdx
                        if (chunks.size > 1) {
                            addStep("Dịch thành công phần ${chunkIndex + 1}/${chunks.size} với API Key số ${keyIdx + 1} ($keySnippet).")
                        } else {
                            addStep("Dịch thành công với API Key số ${keyIdx + 1} ($keySnippet).")
                        }
                        break
                    } else {
                        throw Exception("Gemini API không trả về nội dung dịch.")
                    }
                } catch (e: Exception) {
                    val detailedErr = getDetailedErrorMessage(e)
                    if (chunks.size > 1) {
                        addStep("Thất bại phần ${chunkIndex + 1}/${chunks.size} với API Key số ${keyIdx + 1} ($keySnippet). Chi tiết lỗi: $detailedErr")
                    } else {
                        addStep("Thất bại với API Key số ${keyIdx + 1} ($keySnippet). Chi tiết lỗi: $detailedErr")
                    }
                    chunkErrors.add("[$keySnippet]: $detailedErr")
                }
            }

            if (chunkSuccess) {
                translatedChunks.add(chunkResult)
            } else {
                val finalErrMsg = if (chunks.size > 1) {
                    "Không thể dịch phần ${chunkIndex + 1}/${chunks.size} sau khi thử toàn bộ các API Key:\n" + chunkErrors.joinToString("\n")
                } else {
                    "Dịch thuật thất bại với toàn bộ các API Key đã thử:\n" + chunkErrors.joinToString("\n")
                }
                addStep("Kết quả: $finalErrMsg")
                return@withContext Result.failure(Exception(finalErrMsg))
            }
        }

        val finalResultText = translatedChunks.joinToString("\n\n")
        Result.success(finalResultText)
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

    private fun countWords(text: String): Int {
        if (text.isBlank()) return 0
        var wordCount = 0
        val currentWord = StringBuilder()
        for (char in text) {
            if (isCjk(char)) {
                if (currentWord.isNotEmpty()) {
                    wordCount += countSpaceSeparatedWords(currentWord.toString())
                    currentWord.clear()
                }
                wordCount++
            } else {
                currentWord.append(char)
            }
        }
        if (currentWord.isNotEmpty()) {
            wordCount += countSpaceSeparatedWords(currentWord.toString())
        }
        return wordCount
    }

    private fun isCjk(c: Char): Boolean {
        return c in '\u4e00'..'\u9fff' || 
               c in '\u3400'..'\u4dbf' || 
               c in '\uf900'..'\ufaff' || 
               c in '\u3040'..'\u30ff' || 
               c in '\uac00'..'\ud7af'
    }

    private fun countSpaceSeparatedWords(text: String): Int {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return 0
        return trimmed.split(Regex("\\s+")).size
    }

    private fun splitTextIntoChunks(text: String, maxWordCount: Int = 8000): List<String> {
        val totalWords = countWords(text)
        if (totalWords <= maxWordCount) return listOf(text)

        val chunks = mutableListOf<String>()
        val paragraphs = text.split("\n")
        var currentChunk = StringBuilder()
        var currentChunkWords = 0

        for (paragraph in paragraphs) {
            val paragraphWords = countWords(paragraph)
            
            if (currentChunk.isNotEmpty() && currentChunkWords + paragraphWords > maxWordCount) {
                chunks.add(currentChunk.toString())
                currentChunk = StringBuilder()
                currentChunkWords = 0
            }

            if (paragraphWords > maxWordCount) {
                if (currentChunk.isNotEmpty()) {
                    chunks.add(currentChunk.toString())
                    currentChunk = StringBuilder()
                    currentChunkWords = 0
                }
                var remaining = paragraph
                while (countWords(remaining) > maxWordCount) {
                    val splitIndex = findGoodWordSplitPoint(remaining, maxWordCount)
                    val part = remaining.substring(0, splitIndex)
                    chunks.add(part)
                    remaining = remaining.substring(splitIndex)
                }
                if (remaining.isNotEmpty()) {
                    currentChunk.append(remaining)
                    currentChunkWords = countWords(remaining)
                }
            } else {
                if (currentChunk.isNotEmpty()) {
                    currentChunk.append("\n")
                }
                currentChunk.append(paragraph)
                currentChunkWords += paragraphWords
            }
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        return chunks
    }

    private fun findGoodWordSplitPoint(text: String, maxWordCount: Int): Int {
        var wordCount = 0
        var splitIndex = text.length
        val currentWord = StringBuilder()
        
        for (i in text.indices) {
            val char = text[i]
            if (isCjk(char)) {
                if (currentWord.isNotEmpty()) {
                    wordCount += countSpaceSeparatedWords(currentWord.toString())
                    currentWord.clear()
                }
                wordCount++
            } else {
                currentWord.append(char)
            }
            
            if (wordCount >= maxWordCount) {
                splitIndex = i + 1
                break
            }
        }
        
        if (splitIndex >= text.length) {
            return text.length
        }
        
        val candidate = text.substring(0, splitIndex)
        val punctuation = listOf('.', '?', '!', '。', '？', '！', ',', '，', ';', '；')
        for (i in candidate.length - 1 downTo splitIndex / 2) {
            if (candidate[i] in punctuation) {
                return i + 1
            }
        }
        
        val lastSpace = candidate.lastIndexOf(' ')
        if (lastSpace != -1 && lastSpace > splitIndex / 2) {
            return lastSpace + 1
        }
        
        return splitIndex
    }
}
