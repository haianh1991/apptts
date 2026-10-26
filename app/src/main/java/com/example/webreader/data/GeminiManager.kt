package com.example.webreader.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.SafetySetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.seconds

class GeminiManager {

    private val safetySettings = listOf(
        SafetySetting(HarmCategory.HARASSMENT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.HATE_SPEECH, BlockThreshold.NONE),
        SafetySetting(HarmCategory.SEXUALLY_EXPLICIT, BlockThreshold.NONE),
        SafetySetting(HarmCategory.DANGEROUS_CONTENT, BlockThreshold.NONE)
    )

    private val keyCooldowns = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private class TranslationNode(
        val originalText: String,
        var isLeaf: Boolean = true,
        var left: TranslationNode? = null,
        var right: TranslationNode? = null,
        var translatedText: String = ""
    ) {
        fun getTranslation(): String {
            return if (isLeaf) {
                if (translatedText.isNotEmpty()) translatedText else originalText
            } else {
                val l = left?.getTranslation() ?: ""
                val r = right?.getTranslation() ?: ""
                if (l.isNotEmpty() && r.isNotEmpty()) "$l\n\n$r" else l + r
            }
        }
    }

    private fun findGoodSplitPointForSafety(text: String): Int {
        val len = text.length
        if (len <= 10) return len / 2
        val mid = len / 2
        val scanRange = len / 4 // Quét 25% xung quanh trung điểm
        val closingQuotes = setOf('”', '』', '」', '\'', '"', '）', ')')

        // Ưu tiên 1: Dấu xuống dòng \n
        var bestIndex = -1
        var minDiff = Int.MAX_VALUE
        for (i in (mid - scanRange)..(mid + scanRange)) {
            if (i in text.indices && text[i] == '\n') {
                val diff = kotlin.math.abs(i - mid)
                if (diff < minDiff) {
                    minDiff = diff
                    bestIndex = i
                }
            }
        }
        if (bestIndex != -1) return bestIndex + 1

        // Ưu tiên 2: Dấu kết thúc câu
        val puncs = setOf('。', '！', '？', '.', '!', '?')
        for (i in (mid - scanRange)..(mid + scanRange)) {
            if (i in text.indices && text[i] in puncs) {
                val diff = kotlin.math.abs(i - mid)
                if (diff < minDiff) {
                    minDiff = diff
                    bestIndex = i
                }
            }
        }
        if (bestIndex != -1) {
            var splitPos = bestIndex + 1
            while (splitPos < len && text[splitPos] in closingQuotes) {
                splitPos++
            }
            return splitPos
        }

        // Ưu tiên 3: Dấu phẩy hoặc dấu ngắt khác
        val commas = setOf('，', ',', ';', '；')
        for (i in (mid - scanRange)..(mid + scanRange)) {
            if (i in text.indices && text[i] in commas) {
                val diff = kotlin.math.abs(i - mid)
                if (diff < minDiff) {
                    minDiff = diff
                    bestIndex = i
                }
            }
        }
        if (bestIndex != -1) {
            var splitPos = bestIndex + 1
            while (splitPos < len && text[splitPos] in closingQuotes) {
                splitPos++
            }
            return splitPos
        }

        // Ưu tiên 4: Trung điểm
        return mid
    }

    private fun isSafetyError(e: Throwable): Boolean {
        val errText = getDetailedErrorMessage(e).lowercase()
        
        // 1. Loại trừ các lỗi hệ thống và HTTP đã biết
        val isQuota = errText.contains("429") || errText.contains("resource_exhausted") || errText.contains("quota")
        val isPermissionDenied = errText.contains("403") || errText.contains("permission_denied") || errText.contains("permission denied") || errText.contains("api key")
        val isNotFound = errText.contains("404") || errText.contains("not_found") || errText.contains("not found")
        val isServerErr = errText.contains("500") || errText.contains("503") || errText.contains("504") || errText.contains("internal") || errText.contains("unavailable") || errText.contains("high demand")
        val isBadRequest = errText.contains("400") || errText.contains("invalid_argument") || errText.contains("failed_precondition")
        
        if (isQuota || isPermissionDenied || isNotFound || isServerErr || isBadRequest) {
            return false
        }
        
        // 2. Kiểm tra từ khóa an toàn, ngắt quãng giữa chừng (MAX_TOKENS/RECITATION) hoặc sót tiếng Trung (incomplete_translation)
        val safetyOrSplitKeywords = listOf(
            "safety",
            "blocked",
            "prohibited",
            "finishreason",
            "provided content",
            "max_tokens",
            "recitation",
            "blocklist",
            "spii",
            "reason: other",
            "responsestoppedexception",
            "blockedpromptexception",
            "incomplete_translation"
        )
        val hasSafetyKeyword = safetyOrSplitKeywords.any { errText.contains(it) }
        if (hasSafetyKeyword) return true
        
        // 3. Xử lý lỗi phân tích cú pháp JSON của SDK
        val isSerialization = errText.contains("serializationexception") || errText.contains("missingfieldexception")
        if (isSerialization) {
            return errText.contains("safety") || errText.contains("block") || errText.contains("content") || errText.contains("candidates")
        }
        
        return false
    }

    private fun isNetworkError(e: Throwable): Boolean {
        var cause: Throwable? = e
        while (cause != null) {
            if (cause is java.io.IOException || 
                cause is java.net.ConnectException || 
                cause is java.net.UnknownHostException || 
                cause is java.net.SocketTimeoutException ||
                cause::class.simpleName == "ConnectException" ||
                cause::class.simpleName == "UnknownHostException" ||
                cause::class.simpleName == "SocketTimeoutException" ||
                cause.message?.contains("timeout", ignoreCase = true) == true ||
                cause.message?.contains("connect", ignoreCase = true) == true
            ) {
                return true
            }
            cause = cause.cause
        }
        return false
    }


    private fun getSecondsUntilDailyReset(): Long {
        val calendar = java.util.Calendar.getInstance(java.util.TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))
        val now = calendar.timeInMillis
        
        // Thiết lập mốc 15:00 hôm nay
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 15)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        var resetTime = calendar.timeInMillis
        if (now >= resetTime) {
            // Nếu đã qua 15:00, mốc reset tiếp theo là 15:00 ngày mai
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
            resetTime = calendar.timeInMillis
        }
        
        return (resetTime - now) / 1000
    }

    suspend fun translateTitle(
        title: String,
        apiKeys: List<String>,
        modelName: String = "gemini-3.8-flash",
        sourceLang: String = "Auto",
        targetLang: String = "Tiếng Việt",
        uiLanguage: String = "vi"
    ): String = withContext(Dispatchers.IO) {
        if (apiKeys.isEmpty() || title.isBlank()) return@withContext title
        var currentKeyIndex = 0
        val keysCount = apiKeys.size
        
        val instruction = when (uiLanguage) {
            "vi" -> {
                val srcLangText = if (sourceLang.equals("Auto", ignoreCase = true)) "tự động phát hiện" else sourceLang
                "Bạn là trợ lý dịch thuật. Hãy dịch tiêu đề chương truyện hoặc bài viết từ ngôn ngữ gốc ($srcLangText) sang ngôn ngữ đích ($targetLang) một cách tự nhiên, ngắn gọn và chính xác nhất. Chỉ trả về bản dịch tiêu đề, không giải thích, không thêm dấu ngoặc kép hay bất kỳ thông tin nào khác."
            }
            "zh" -> {
                val srcLangTextZh = if (sourceLang.equals("Auto", ignoreCase = true)) "自动检测" else sourceLang
                "您是翻译助手。请将章节或文章标题从源语言（$srcLangTextZh）自然且简练地翻译为目标语言（$targetLang）。仅返回翻译后的标题，不进行解释，不添加引号或任何其他信息。"
            }
            else -> {
                val srcLangTextEn = if (sourceLang.equals("Auto", ignoreCase = true)) "automatically detected" else sourceLang
                "You are a professional translation assistant. Translate the chapter or article title from the source language ($srcLangTextEn) into the target language ($targetLang) naturally and concisely. Return ONLY the translated title, without any quotes, explanations, or introductory text."
            }
        }

        for (attempt in 0 until keysCount) {
            val keyIdx = (currentKeyIndex + attempt) % keysCount
            val apiKey = apiKeys[keyIdx]
            try {
                val generativeModel = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey,
                    requestOptions = RequestOptions(timeout = 15.seconds),
                    safetySettings = safetySettings,
                    systemInstruction = content {
                        text(instruction)
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

    private fun getSystemInstructions(
        uiLanguage: String,
        sourceLang: String,
        targetLang: String,
        customInstructions: String,
        disclaimerText: String,
        hasTitle: Boolean
    ): String {
        val hasCustom = customInstructions.isNotBlank()
        val hasDisclaimer = disclaimerText.isNotBlank()

        val srcTextVi = if (sourceLang.equals("Auto", ignoreCase = true)) "tự động phát hiện" else sourceLang
        val srcTextZh = if (sourceLang.equals("Auto", ignoreCase = true)) "自动检测" else sourceLang
        val srcTextEn = if (sourceLang.equals("Auto", ignoreCase = true)) "automatically detected" else sourceLang

        val disclaimerPart = if (hasDisclaimer) {
            "\n\n<system_warning>\n${disclaimerText.trim()}\n</system_warning>"
        } else ""

        var systemInstruction = ""

        if (uiLanguage == "vi") {
            val titleRuleVi = if (hasTitle) {
                "1. **Dịch tiêu đề**: Dịch tiêu đề gốc từ ngôn ngữ gốc ($srcTextVi) sang ngôn ngữ đích ($targetLang) một cách tự nhiên. Dòng đầu tiên của kết quả trả về BẮT BUỘC phải tuân thủ định dạng chính xác sau đây:\n   Title: [Tiêu đề dịch]\n   Nội dung bản dịch sạch sẽ nằm ở phía dưới, cách tiêu đề dịch bởi hai dấu xuống dòng (\\n\\n)."
            } else {
                "1. **Không dịch tiêu đề**: Văn bản này là phân đoạn tiếp theo và không chứa tiêu đề chương truyện. Tuyệt đối không tự ý thêm dòng 'Title: ...' ở đầu."
            }

            val fewShotVi = if (hasTitle) {
                """
                <few_shot_examples>
                Ví dụ:
                Input:
                Tiêu đề gốc: 第一章: 宗門
                Văn bản gốc:
                第一章: 宗門
                [上一章] [目錄] [下一章]
                &emsp;&emsp;進屋一米不到，是一塊立著的大木板。<br />
                <script>loadAdv(5,0);</script><br />
                &emsp;&emsp;木板左側是空道。
                
                Output:
                Title: Chương 1: Tông Môn
                
                Vào cửa chưa đầy một mét là một tấm gỗ lớn dựng đứng.
                
                Phía bên trái tấm gỗ là một lối đi trống.
                </few_shot_examples>
                """.trimIndent()
            } else {
                """
                <few_shot_examples>
                Ví dụ:
                Input:
                &emsp;&emsp;進屋一米不到，是一塊立著的大木板。<br />
                <script>loadAdv(5,0);</script><br />
                &emsp;&emsp;木板左側是空道。
                
                Output:
                Vào cửa chưa đầy một mét là một tấm gỗ lớn dựng đứng.
                
                Phía bên trái tấm gỗ là một lối đi trống.
                </few_shot_examples>
                """.trimIndent()
            }

            val rule6Vi = if (hasCustom) {
                "\n6. **Tuân thủ quy tắc bổ sung**:\n${customInstructions.trim()}"
            } else ""

            systemInstruction = """
                <role>
                Bạn là chuyên gia dịch thuật văn học và trích xuất nội dung sạch từ trang web.
                </role>
                
                <rules>
                $titleRuleVi
                2. **Lọc sạch rác trang web**: Nhận diện phần nội dung bài viết/chương truyện chính trong văn bản thô. Loại bỏ hoàn toàn các thành phần thừa, quảng cáo và menu điều hướng bao gồm:
                   - Các nút bấm, liên kết chuyển trang (ví dụ: "Chương sau", "Mục lục", "Trang chủ", "上一章", "目錄", "下一章", "投票推薦", "加入書籤", "小說報錯", "關燈", "字體-", "字體+").
                   - Các quảng cáo xen kẽ giữa các câu (ví dụ: thẻ script quảng cáo như `<script>loadAdv(5,0);</script>`).
                   - Các bình luận bên lề của độc giả hoặc quảng cáo bằng chữ khác không liên quan đến cốt truyện chính.
                3. **Dịch văn học đầy đủ 100%**: Dịch TOÀN BỘ 100% nội dung chính vừa lọc được từ ngôn ngữ gốc ($srcTextVi) sang ngôn ngữ đích ($targetLang) một cách tự nhiên, trôi chảy, đúng ngữ cảnh văn học phong cách truyện chữ. Sử dụng từ Hán-Việt mượt mà, xưng hô phù hợp bối cảnh nhân vật. Tuyệt đối KHÔNG được bỏ sót đoạn văn nào, KHÔNG được tóm tắt hay cắt bớt, và KHÔNG được giữ nguyên chữ Hán/ngôn ngữ gốc chưa dịch ở bất kỳ câu hay đoạn văn nào.
                4. **Giữ nguyên định dạng đoạn văn**: BẮT BUỘC phân tách rõ ràng từng đoạn văn bằng hai dấu xuống dòng (\n\n). Không được gộp dán liền tất cả các đoạn văn thành một khối văn bản duy nhất.
                5. **Đầu ra nghiêm ngặt**: Tuyệt đối không thêm bất kỳ văn bản giới thiệu, chú thích ngoài lề hay lời thoại dẫn dắt nào của AI (như "Dưới đây là bản dịch...", "Đây là kết quả dịch:..."). Chỉ trả về kết quả dịch sạch theo đúng yêu cầu định dạng.$rule6Vi
                </rules>
                
                $fewShotVi
            """.trimIndent()
        } else if (uiLanguage == "zh") {
            val titleRuleZh = if (hasTitle) {
                "1. **翻译标题**: 将原始标题从源语言 ($srcTextZh) 自然且专业地翻译为目标语言 ($targetLang)。第一行必须且只能使用以下格式返回：\n   Title: [翻译后的标题]\n   翻译内容应该在标题下方，并用两个换行符 (\\n\\n) 分隔。"
            } else {
                "1. **不翻译标题**: 该文本是后续段落且不包含章节标题。绝对不要在开头添加 'Title: ...' 格式。"
            }

            val fewShotZh = if (hasTitle) {
                """
                <few_shot_examples>
                例如：
                输入：
                原始标题：第一章: 宗门
                原始文本：
                第一章: 宗门
                [上一章] [目录] [下一章]
                &emsp;&emsp;進屋一米不到，是一塊立著的大木板。<br />
                <script>loadAdv(5,0);</script><br />
                &emsp;&emsp;木板左側是空道。
                
                输出：
                Title: Chapter 1: Sect
                
                Entering the room less than a meter away is a large standing wooden board.
                
                The left side of the wooden board is an empty path.
                </few_shot_examples>
                """.trimIndent()
            } else {
                """
                <few_shot_examples>
                例如：
                输入：
                &emsp;&emsp;進屋一米不到，是一塊立著的大木板。<br />
                <script>loadAdv(5,0);</script><br />
                &emsp;&emsp;木板左側是空道。
                
                输出：
                Entering the room less than a meter away is a large standing wooden board.
                
                The left side of the wooden board is an empty path.
                </few_shot_examples>
                """.trimIndent()
            }

            val rule6Zh = if (hasCustom) {
                "\n6. **遵守附加规则**:\n${customInstructions.trim()}"
            } else ""

            systemInstruction = """
                <role>
                您是一个智能网页噪点提取和文学翻译专家。
                </role>
                
                <rules>
                $titleRuleZh
                2. **提取并净化核心内容**: 识别原始文本中的文章/章节主体。彻底清除所有噪点，包括：
                   - 导航链接/翻页按钮（例如：“上一章”、“目录”、“下一章”、“投票推荐”、“加入书签”、“小说报错”、“关灯”、“字体-”、“字体+”）。
                   - 广告代码或内嵌广告脚本（例如：`<script>loadAdv(5,0);</script>`）。
                   - 读者无关评论或非主体文字。
                3. **100%完整高质量翻译**: 将净化后的主体内容从源语言 ($srcTextZh) 100%完整地翻译为目标语言 ($targetLang)，匹配目标文体的专业特点，使行文自然流畅。绝对不要遗漏、概括、截断或保留任何未翻译的源语言句子或段落。
                4. **保留段落结构**: 必须明确使用两个换行符 (\n\n) 分隔每一个段落。绝对不要将所有段落合并为一个长文本块。
                5. **无额外输出**: 绝对不要包含任何介绍性或解释性文本（例如“以下是翻译内容...”）。仅返回符合格式要求的翻译核心内容。$rule6Zh
                </rules>
                
                $fewShotZh
            """.trimIndent()
        } else {
            val titleRuleEn = if (hasTitle) {
                "1. **Translate Title**: Translate the original title from the source language ($srcTextEn) into the target language ($targetLang) naturally and professionally. The first line of the output MUST be in this exact format:\n   Title: [translated title]\n   The cleaned translation content must be placed below, separated by two newlines (\\n\\n)."
            } else {
                "1. **Do Not Translate Title**: This text block is a continuation and does not contain a chapter title. Do NOT add any 'Title: ...' prefix at the beginning."
            }

            val fewShotEn = if (hasTitle) {
                """
                <few_shot_examples>
                Example:
                Input:
                Original Title: 第一章: 宗门
                Raw Text:
                第一章: 宗门
                [上一章] [目录] [下一章]
                &emsp;&emsp;進屋一米不到，是一塊立著的大木板。<br />
                <script>loadAdv(5,0);</script><br />
                &emsp;&emsp;木板左側是空道。
                
                Output:
                Title: Chapter 1: The Sect
                
                Entering the room less than a meter away is a large standing wooden board.
                
                The left side of the wooden board is an empty path.
                </few_shot_examples>
                """.trimIndent()
            } else {
                """
                <few_shot_examples>
                Example:
                Input:
                &emsp;&emsp;進屋一米不到，是一塊立著的大木板。<br />
                <script>loadAdv(5,0);</script><br />
                &emsp;&emsp;木板左側是空道。
                
                Output:
                Entering the room less than a meter away is a large standing wooden board.
                
                The left side of the wooden board is an empty path.
                </few_shot_examples>
                """.trimIndent()
            }

            val rule6En = if (hasCustom) {
                "\n6. **Adhere to custom rules**:\n${customInstructions.trim()}"
            } else ""

            systemInstruction = """
                <role>
                You are a professional webpage content extractor and literary translator.
                </role>
                
                <rules>
                $titleRuleEn
                2. **Extract and Clean Content**: Identify the main content of the article or chapter. Completely remove all webpage clutter, including:
                   - Navigation links/buttons (e.g., "Next Chapter", "Table of Contents", "Home", "上一章", "目錄", "下一章", "投票推薦", "加入書籤", "小說報錯").
                   - In-between advertisements and script tags (e.g., `<script>loadAdv(5,0);</script>`).
                   - Irrelevant comments or non-main text.
                3. **100% Complete High-Quality Translation**: Translate 100% of the cleaned core content from the source language ($srcTextEn) into the target language ($targetLang) in a natural, smooth, and fluent manner, matching the target language's literary style. Never skip, summarize, truncate, or leave any sentence or paragraph untranslated in the source language.
                4. **Preserve Paragraph Structure**: MUST clearly separate each paragraph with two newlines (\n\n). Never merge paragraphs into a single solid block of text.
                5. **Strict Output Formatting**: Do NOT include any introductory or explanatory text (such as "Here is the translation..."). Only return the clean translation matching the required format.$rule6En
                </rules>
                
                $fewShotEn
            """.trimIndent()
        }

        // Apply Disclaimer
        var finalPrompt = systemInstruction
        if (hasDisclaimer) {
            finalPrompt = "$disclaimerPart\n\n$finalPrompt"
        }

        return finalPrompt
    }

    private fun isTargetLanguageCjk(targetLang: String): Boolean {
        val lower = targetLang.lowercase()
        return lower.contains("trung") || lower.contains("chinese") || lower.contains("中文") ||
               lower.contains("nhật") || lower.contains("japanese") ||
               lower.contains("hàn") || lower.contains("korean")
    }

    private fun countCjkChars(text: String): Int {
        var count = 0
        for (c in text) {
            if (isCjk(c)) count++
        }
        return count
    }

    private suspend fun repairUntranslatedParagraphs(
        translatedText: String,
        apiKey: String,
        modelName: String,
        targetLang: String,
        apiProvider: String,
        baseUrl: String,
        addStep: (String) -> Unit
    ): String {
        val paragraphs = translatedText.split("\n")
        val repairedParagraphs = paragraphs.toMutableList()
        var repairedCount = 0

        for (i in repairedParagraphs.indices) {
            val para = repairedParagraphs[i].trim()
            if (para.isEmpty()) continue
            val cjkCount = countCjkChars(para)
            if (cjkCount >= 4) {
                try {
                    val repairInstruction = "Bạn là chuyên gia dịch thuật văn học. Hãy dịch trọn vẹn 100% đoạn văn dưới đây sang $targetLang. Tuyệt đối KHÔNG để sót bất kỳ chữ Hán/ký tự ngôn ngữ gốc nào, không giải thích, chỉ trả về đúng nội dung bản dịch."
                    val repairPrompt = "Dịch đầy đủ đoạn văn tiểu thuyết sau sang $targetLang:\n\n$para"
                    if (apiProvider == "nvidia" || apiProvider == "openai") {
                        val tempNode = TranslationNode(para)
                        callNvidiaOpenAiApiStream(
                            apiKey = apiKey,
                            baseUrl = baseUrl,
                            modelName = modelName,
                            sysInstruction = repairInstruction,
                            prompt = repairPrompt,
                            node = tempNode,
                            onContentUpdated = null,
                            addStep = {}
                        )
                        val fixed = tempNode.translatedText.trim()
                        if (fixed.isNotEmpty() && countCjkChars(fixed) < cjkCount) {
                            repairedParagraphs[i] = fixed
                            repairedCount++
                        }
                    } else {
                        val repairModel = GenerativeModel(
                            modelName = modelName,
                            apiKey = apiKey,
                            generationConfig = generationConfig {
                                temperature = 0.2f
                                topP = 0.95f
                            },
                            requestOptions = RequestOptions(timeout = 60.seconds),
                            safetySettings = safetySettings,
                            systemInstruction = content {
                                text(repairInstruction)
                            }
                        )
                        val resp = repairModel.generateContent(repairPrompt)
                        val fixed = resp.text?.trim()
                        if (!fixed.isNullOrEmpty() && countCjkChars(fixed) < cjkCount) {
                            repairedParagraphs[i] = fixed
                            repairedCount++
                        }
                    }
                } catch (_: Exception) {
                    // Nếu đoạn đơn lẻ bị lỗi sẽ được kiểm tra lại ở bước tổng thể phía dưới
                }
            }
        }
        if (repairedCount > 0) {
            addStep("Đã dịch bổ sung thành công $repairedCount đoạn văn còn sót chữ Hán.")
        }
        return repairedParagraphs.joinToString("\n")
    }

    private suspend fun verifyAndRepairTranslation(
        chunk: String,
        translatedText: String,
        apiKey: String,
        modelName: String,
        targetLang: String,
        apiProvider: String,
        baseUrl: String,
        chunkInfo: String,
        node: TranslationNode,
        onContentUpdated: (() -> Unit)?,
        addStep: (String) -> Unit
    ): String {
        if (isTargetLanguageCjk(targetLang)) return translatedText

        val inputCjk = countCjkChars(chunk)
        if (inputCjk < 15) return translatedText

        // 1. Kiểm tra hiện tượng bị cắt cụt (Truncation) do model tự dừng sớm
        val outputWords = countWords(translatedText)
        if (inputCjk >= 350 && outputWords < ((inputCjk - 80) * 0.45).toInt()) {
            throw Exception("incomplete_translation: Bản dịch $chunkInfo bị thiếu hụt nội dung ($outputWords từ dịch so với $inputCjk chữ Hán gốc). Tự động chia nhỏ để dịch đầy đủ.")
        }

        // 2. Kiểm tra chữ Hán còn sót lại trong bản dịch
        val remainingCjk = countCjkChars(translatedText)
        if (remainingCjk <= 3) return translatedText

        val paragraphsWithCjk = translatedText.split("\n").count { countCjkChars(it) >= 4 }
        // Nếu còn sót quá nhiều chữ Hán (> 25% văn bản gốc hoặc > 6 đoạn văn), chia đôi chunk để dịch lại chuẩn xác
        if (remainingCjk > maxOf(120, (inputCjk * 0.25).toInt()) || paragraphsWithCjk > 6) {
            throw Exception("incomplete_translation: Phát hiện $remainingCjk chữ Hán chưa được dịch trong $chunkInfo ($paragraphsWithCjk đoạn). Tự động chia nhỏ để dịch lại.")
        }

        // Nếu chỉ sót một vài câu/đoạn nhỏ (1..6 đoạn), tự động chạy dịch bổ sung cho chính các đoạn đó
        if (paragraphsWithCjk > 0 || remainingCjk >= 6) {
            addStep("Phát hiện $paragraphsWithCjk đoạn còn sót chữ Hán ($remainingCjk ký tự) trong $chunkInfo, đang dịch bổ sung tự động...")
            val repaired = repairUntranslatedParagraphs(
                translatedText = translatedText,
                apiKey = apiKey,
                modelName = modelName,
                targetLang = targetLang,
                apiProvider = apiProvider,
                baseUrl = baseUrl,
                addStep = addStep
            )
            node.translatedText = repaired
            onContentUpdated?.invoke()

            val afterRepairCjk = countCjkChars(repaired)
            if (afterRepairCjk >= 12) {
                throw Exception("incomplete_translation: Vẫn còn $afterRepairCjk chữ Hán chưa dịch sau khi dịch bổ sung ở $chunkInfo. Tự động chia nhỏ đoạn văn.")
            }
            return repaired
        }

        return translatedText
    }

    private suspend fun translateChunkRecursive(
        chunk: String,
        apiKeys: List<String>,
        currentKeyIndexRef: IntArray,
        modelName: String,
        sourceLang: String,
        targetLang: String,
        customInstructions: String,
        disclaimerText: String,
        logSteps: MutableList<String>,
        onStepAdded: ((String) -> Unit)?,
        onContentUpdated: (() -> Unit)?,
        node: TranslationNode,
        uiLanguage: String,
        isFirstChunk: Boolean,
        title: String?,
        systemInstructionWithTitle: String,
        systemInstructionStandard: String,
        chunkInfo: String,
        depth: Int = 0,
        apiProvider: String = "gemini",
        baseUrl: String = "https://integrate.api.nvidia.com/v1"
    ): Boolean = withContext(Dispatchers.IO) {
        fun addStep(step: String) {
            logSteps.add(step)
            onStepAdded?.invoke(step)
        }

        var success = false
        val now = System.currentTimeMillis()
        val activeKeys = apiKeys.filter { apiKey ->
            val modelCooldown = keyCooldowns["$apiKey:$modelName"] ?: 0L
            modelCooldown <= now
        }
        val currentKeys = if (activeKeys.isNotEmpty()) activeKeys else apiKeys
        val keysCount = currentKeys.size
        val chunkErrors = mutableListOf<String>()

        val lastSuccessfulKey = apiKeys.getOrNull(currentKeyIndexRef[0])
        val startIndexInCurrentKeys = if (lastSuccessfulKey != null) {
            currentKeys.indexOf(lastSuccessfulKey).coerceAtLeast(0)
        } else {
            0
        }

        for (attempt in 0 until keysCount) {
            val keyIdx = (startIndexInCurrentKeys + attempt) % keysCount
            val apiKey = currentKeys[keyIdx]
            val originalIndex = apiKeys.indexOf(apiKey)
            val keyNum = originalIndex + 1
            val keySnippet = if (apiKey.length > 8) {
                apiKey.take(4) + "..." + apiKey.takeLast(4)
            } else {
                "Key $keyNum"
            }

            val stepMsg = if (depth > 0) {
                "Đang thử phần con $chunkInfo (Kích thước: ${chunk.length} ký tự) với API Key số $keyNum ($keySnippet)..."
            } else {
                "Đang thử dịch $chunkInfo (Kích thước: ${chunk.length} ký tự) với API Key số $keyNum ($keySnippet)..."
            }
            addStep(stepMsg)

            try {
                val literaryContextNote = if (depth >= 2) {
                    "\n[Lưu ý ngữ cảnh: Đây là trích đoạn tiểu thuyết văn học giả tưởng. Hãy dịch đầy đủ 100% toàn bộ các câu sang $targetLang một cách khách quan, tuyệt đối không để sót chữ Hán gốc.]\n"
                } else ""

                val prompt = if (isFirstChunk && !title.isNullOrBlank()) {
                    when (uiLanguage) {
                        "vi" -> {
                            """
                                Tiêu đề gốc cần dịch:
                                $title
                                $literaryContextNote
                                Dưới đây là văn bản trang web cần dịch đầy đủ 100% sang ngôn ngữ đích ($targetLang):
                                
                                $chunk
                            """.trimIndent()
                        }
                        "zh" -> {
                            """
                                要翻译的原始标题：
                                $title
                                $literaryContextNote
                                以下是要100%完整翻译为 $targetLang 的网页原始文本：
                                
                                $chunk
                            """.trimIndent()
                        }
                        else -> {
                            """
                                Original title to translate:
                                $title
                                $literaryContextNote
                                Here is the raw webpage text to translate 100% completely into $targetLang:
                                
                                $chunk
                            """.trimIndent()
                        }
                    }
                } else {
                    when (uiLanguage) {
                        "vi" -> {
                            val srcLangText = if (sourceLang.equals("Auto", ignoreCase = true)) "tự động phát hiện" else sourceLang
                            """
                                Hãy dịch đầy đủ 100% văn bản thô dưới đây từ ngôn ngữ gốc ($srcLangText) sang ngôn ngữ đích ($targetLang). Hãy lọc bỏ các thành phần quảng cáo hoặc nút điều hướng nếu có, dịch sát nghĩa, tự nhiên nhất và không để sót bất kỳ câu tiếng Trung/ngôn ngữ gốc nào:$literaryContextNote
                                
                                $chunk
                            """.trimIndent()
                        }
                        "zh" -> {
                            val srcLangTextZh = if (sourceLang.equals("Auto", ignoreCase = true)) "自动检测" else sourceLang
                            """
                                请将以下原始文本从源语言 ($srcLangTextZh) 100%完整地翻译为目标语言 ($targetLang)。如果存在任何广告或导航链接，请进行过滤，并自然且专业地翻译核心内容：$literaryContextNote
                                
                                $chunk
                            """.trimIndent()
                        }
                        else -> {
                            val srcLangTextEn = if (sourceLang.equals("Auto", ignoreCase = true)) "automatically detected" else sourceLang
                            """
                                Please translate 100% of the following raw text from the source language ($srcLangTextEn) into the target language ($targetLang). Filter out any advertisements or navigation links if present, and translate the core content naturally and completely:$literaryContextNote
                                
                                $chunk
                            """.trimIndent()
                        }
                    }
                }

                val sysInstruction = if (isFirstChunk && !title.isNullOrBlank()) systemInstructionWithTitle else systemInstructionStandard

                if (apiProvider == "nvidia" || apiProvider == "openai") {
                    callNvidiaOpenAiApiStream(
                        apiKey = apiKey,
                        baseUrl = baseUrl,
                        modelName = modelName,
                        sysInstruction = sysInstruction,
                        prompt = prompt,
                        node = node,
                        onContentUpdated = onContentUpdated,
                        addStep = ::addStep
                    )
                    verifyAndRepairTranslation(
                        chunk = chunk,
                        translatedText = node.translatedText,
                        apiKey = apiKey,
                        modelName = modelName,
                        targetLang = targetLang,
                        apiProvider = apiProvider,
                        baseUrl = baseUrl,
                        chunkInfo = chunkInfo,
                        node = node,
                        onContentUpdated = onContentUpdated,
                        addStep = ::addStep
                    )
                } else {
                    val generativeModel = GenerativeModel(
                        modelName = modelName,
                        apiKey = apiKey,
                        generationConfig = generationConfig {
                            temperature = 0.3f
                            topP = 0.95f
                        },
                        requestOptions = RequestOptions(timeout = 180.seconds),
                        safetySettings = safetySettings,
                        systemInstruction = content {
                            text(sysInstruction)
                        }
                    )

                    val previewPromptText = if (prompt.length > 300) {
                        prompt.take(150) + "\n...\n[Đã rút ngắn nội dung dài: ${prompt.length} ký tự]\n...\n" + prompt.takeLast(100)
                    } else {
                        prompt
                    }
                    
                    val apiPayloadPreview = """
                    {
                      "contents": [
                        {
                          "parts": [
                            {
                              "text": ${escapeJsonString(previewPromptText)}
                            }
                          ]
                        }
                      ],
                      "systemInstruction": {
                        "parts": [
                          {
                            "text": ${escapeJsonString(sysInstruction)}
                          }
                        ]
                      },
                      "generationConfig": {
                        "temperature": 0.3,
                        "topP": 0.95
                      },
                      "safetySettings": [
                        { "category": "HARM_CATEGORY_HARASSMENT", "threshold": "BLOCK_NONE" },
                        { "category": "HARM_CATEGORY_HATE_SPEECH", "threshold": "BLOCK_NONE" },
                        { "category": "HARM_CATEGORY_SEXUALLY_EXPLICIT", "threshold": "BLOCK_NONE" },
                        { "category": "HARM_CATEGORY_DANGEROUS_CONTENT", "threshold": "BLOCK_NONE" }
                      ]
                    }
                    """.trimIndent()
                    addStep("[API REQUEST PAYLOAD]:\n$apiPayloadPreview")

                    val responseStream = generativeModel.generateContentStream(prompt)
                    val chunkBuilder = StringBuilder()
                    var abnormalFinishReason: String? = null
                    var lastNotifyTime = 0L
                    
                    responseStream.collect { response ->
                        val candidate = response.candidates.firstOrNull()
                        val finishName = candidate?.finishReason?.name
                        if (finishName == "SAFETY" ||
                            finishName == "PROHIBITED_CONTENT" ||
                            finishName == "RECITATION" ||
                            finishName == "MAX_TOKENS" ||
                            finishName == "BLOCKLIST" ||
                            finishName == "SPII"
                        ) {
                            abnormalFinishReason = finishName
                            throw Exception("Gemini API ngắt kết quả (finishReason: $finishName)")
                        }
                        val chunkText = response.text
                        if (chunkText != null) {
                            chunkBuilder.append(chunkText)
                            node.translatedText = chunkBuilder.toString()
                            val nowTime = System.currentTimeMillis()
                            if (nowTime - lastNotifyTime >= 150L) {
                                lastNotifyTime = nowTime
                                onContentUpdated?.invoke()
                            }
                        }
                    }
                    onContentUpdated?.invoke()

                    if (abnormalFinishReason != null) {
                        throw Exception("Gemini API ngắt kết quả (finishReason: $abnormalFinishReason)")
                    }

                    val translatedText = chunkBuilder.toString()
                    if (translatedText.isNotEmpty()) {
                        node.translatedText = translatedText
                        verifyAndRepairTranslation(
                            chunk = chunk,
                            translatedText = translatedText,
                            apiKey = apiKey,
                            modelName = modelName,
                            targetLang = targetLang,
                            apiProvider = apiProvider,
                            baseUrl = baseUrl,
                            chunkInfo = chunkInfo,
                            node = node,
                            onContentUpdated = onContentUpdated,
                            addStep = ::addStep
                        )
                    } else {
                        throw Exception("Gemini API không trả về nội dung dịch.")
                    }
                }

                success = true
                currentKeyIndexRef[0] = originalIndex
                val successMsg = if (depth > 0) {
                    "Dịch thành công phần con $chunkInfo với API Key số $keyNum ($keySnippet)."
                } else {
                    "Dịch thành công $chunkInfo với API Key số $keyNum ($keySnippet)."
                }
                addStep(successMsg)
                break
            } catch (e: Exception) {
                val detailedErr = getDetailedErrorMessage(e)
                val isSafety = isSafetyError(e)
                val isNetwork = isNetworkError(e)
                val errText = detailedErr.lowercase()
                
                if (isNetwork) {
                    val errLabel = "Lỗi kết nối mạng (Internet/Timeout)"
                    addStep("Thất bại $chunkInfo với API Key số $keyNum ($keySnippet) [$errLabel]. Chi tiết: $detailedErr")
                    chunkErrors.add("[$keySnippet] [$errLabel]: $detailedErr")
                } else if (isSafety) {
                    val errLabel = "Cần chia nhỏ đoạn văn (Safety / MaxTokens / Sót chữ Hán)"
                    addStep("Cảnh báo $chunkInfo với API Key số $keyNum ($keySnippet) [$errLabel]. Chi tiết: $detailedErr")
                    chunkErrors.add("[$keySnippet] [$errLabel]: $detailedErr")
                    
                    if (chunk.length > 60) {
                        addStep("Kích hoạt cơ chế tự động chia nhỏ đoạn văn $chunkInfo (kích thước: ${chunk.length} ký tự > 60 ký tự) làm hai để dịch chuẩn xác 100%.")
                        
                        val splitIdx = findGoodSplitPointForSafety(chunk)
                        val leftText = chunk.substring(0, splitIdx)
                        val rightText = chunk.substring(splitIdx)
                        
                        node.isLeaf = false
                        val leftNode = TranslationNode(leftText)
                        val rightNode = TranslationNode(rightText)
                        node.left = leftNode
                        node.right = rightNode
                        
                        val leftSuccess = translateChunkRecursive(
                            chunk = leftText,
                            apiKeys = apiKeys,
                            currentKeyIndexRef = currentKeyIndexRef,
                            modelName = modelName,
                            sourceLang = sourceLang,
                            targetLang = targetLang,
                            customInstructions = customInstructions,
                            disclaimerText = disclaimerText,
                            logSteps = logSteps,
                            onStepAdded = onStepAdded,
                            onContentUpdated = onContentUpdated,
                            node = leftNode,
                            uiLanguage = uiLanguage,
                            isFirstChunk = isFirstChunk,
                            title = title,
                            systemInstructionWithTitle = systemInstructionWithTitle,
                            systemInstructionStandard = systemInstructionStandard,
                            chunkInfo = "$chunkInfo.1",
                            depth = depth + 1,
                            apiProvider = apiProvider,
                            baseUrl = baseUrl
                        )
                        
                        val rightSuccess = translateChunkRecursive(
                            chunk = rightText,
                            apiKeys = apiKeys,
                            currentKeyIndexRef = currentKeyIndexRef,
                            modelName = modelName,
                            sourceLang = sourceLang,
                            targetLang = targetLang,
                            customInstructions = customInstructions,
                            disclaimerText = disclaimerText,
                            logSteps = logSteps,
                            onStepAdded = onStepAdded,
                            onContentUpdated = onContentUpdated,
                            node = rightNode,
                            uiLanguage = uiLanguage,
                            isFirstChunk = false,
                            title = null,
                            systemInstructionWithTitle = systemInstructionWithTitle,
                            systemInstructionStandard = systemInstructionStandard,
                            chunkInfo = "$chunkInfo.2",
                            depth = depth + 1,
                            apiProvider = apiProvider,
                            baseUrl = baseUrl
                        )
                        
                        success = leftSuccess && rightSuccess
                        return@withContext success
                    } else {
                        addStep("Phân đoạn cực nhỏ ($chunkInfo, ${chunk.length} ký tự) bị chặn. Ngừng chia nhỏ.")
                        break // Break out of key attempt loop
                    }
                } else {
                    val limitRegex = Regex("limit:\\s*(\\d+)", RegexOption.IGNORE_CASE)
                    val limitMatch = limitRegex.find(errText)
                    val limitValue = limitMatch?.groupValues?.get(1)?.toIntOrNull()

                    val isDailyQuota = errText.contains("daily") || 
                        errText.contains("per_day") || 
                        errText.contains("perday") || 
                        errText.contains("free_tier_requests_per_day") ||
                        (limitValue != null && limitValue == 20)
                    
                    val isQuota = errText.contains("429") || errText.contains("resource_exhausted") || errText.contains("quota")
                    val isPermissionDenied = errText.contains("403") || errText.contains("permission_denied") || errText.contains("permission denied") || errText.contains("api key")
                    val isNotFound = errText.contains("404") || errText.contains("not_found") || errText.contains("not found")
                    val isBadRequest = errText.contains("400") || errText.contains("invalid_argument") || errText.contains("failed_precondition") || errText.contains("failed precondition")
                    val isServerErr = errText.contains("500") || errText.contains("503") || errText.contains("504") || errText.contains("internal") || errText.contains("unavailable") || errText.contains("high demand")

                    val errLabel = when {
                        isDailyQuota -> "Lỗi Giới hạn Hàng ngày (Daily Limit Exceeded)"
                        isQuota -> "Lỗi Tốc độ Hạn ngạch (RPM/TPM Quota Exceeded)"
                        isPermissionDenied -> "Lỗi Khóa API không hợp lệ (Forbidden/Invalid Key)"
                        isNotFound -> "Lỗi Không tìm thấy mô hình (Model Not Found)"
                        isBadRequest -> "Lỗi Yêu cầu không hợp lệ (Bad Request)"
                        isServerErr -> "Lỗi Máy chủ Gemini/NVIDIA (Server Error)"
                        else -> "Lỗi Dịch thuật"
                    }
                    
                    if (isDailyQuota) {
                        val secondsRemaining = getSecondsUntilDailyReset()
                        val cooldownUntil = now + (secondsRemaining * 1000L)
                        keyCooldowns["$apiKey:$modelName"] = cooldownUntil
                        val hours = secondsRemaining / 3600
                        val mins = (secondsRemaining % 3600) / 60
                        addStep("Thất bại $chunkInfo với API Key số $keyNum ($keySnippet) [$errLabel]. Tạm khóa API Key này cho mô hình $modelName trong ${hours}h ${mins}m (đến khi reset ngày mới). Chi tiết: $detailedErr")
                    } else if (isQuota) {
                        val cooldownUntil = now + 60_000L
                        keyCooldowns["$apiKey:$modelName"] = cooldownUntil
                        addStep("Thất bại $chunkInfo với API Key số $keyNum ($keySnippet) [$errLabel]. Tạm khóa API Key này cho mô hình $modelName trong 60 giây. Chi tiết: $detailedErr")
                    } else if (isPermissionDenied || isNotFound || isBadRequest) {
                        val cooldownUntil = now + 86400_000L
                        keyCooldowns["$apiKey:$modelName"] = cooldownUntil
                        addStep("Thất bại $chunkInfo với API Key số $keyNum ($keySnippet) [$errLabel]. Khóa API Key bị vô hiệu hóa 24h. Chi tiết: $detailedErr")
                    } else {
                        addStep("Thất bại $chunkInfo với API Key số $keyNum ($keySnippet) [$errLabel]. Chi tiết: $detailedErr")
                    }
                    chunkErrors.add("[$keySnippet] [$errLabel]: $detailedErr")
                }
            }
        }

        if (!success) {
            val fallbackHeader = if (chunkErrors.any { it.lowercase().contains("daily") || it.lowercase().contains("hạn ngạch") || it.lowercase().contains("quota") }) {
                when (uiLanguage) {
                    "vi" -> "--- [Lỗi hết hạn ngạch API, hiển thị văn bản gốc] ---"
                    "zh" -> "--- [API 配额已用尽，显示原文] ---"
                    else -> "--- [API Quota Exceeded, showing original text] ---"
                }
            } else {
                when (uiLanguage) {
                    "vi" -> "--- [Lỗi dịch thuật kỹ thuật, hiển thị văn bản gốc] ---"
                    "zh" -> "--- [翻译失败，显示原文] ---"
                    else -> "--- [Translation error, showing original text] ---"
                }
            }
            node.translatedText = "$fallbackHeader\n\n$chunk"
            onContentUpdated?.invoke()
            addStep("Kết quả $chunkInfo: Thất bại sau khi thử tất cả API Keys. Sử dụng văn bản gốc làm fallback.")
        }

        return@withContext success
    }

    private fun normalizeNvidiaText(text: String): String {
        var clean = text.replace(Regex("(?s)<think>.*?</think>"), "").trim()
        if (clean.startsWith("<think>", ignoreCase = true)) {
            val endThinkIdx = clean.indexOf("</think>", ignoreCase = true)
            clean = if (endThinkIdx != -1) {
                clean.substring(endThinkIdx + 8).trim()
            } else {
                ""
            }
        }
        if (clean.isEmpty()) return ""

        // 1. Chuyển đổi các thẻ ngắt dòng/đoạn văn HTML (<br>, <p>, </p>) thành \n\n
        clean = clean.replace(Regex("(?i)<br\\s*/?>|</?p>"), "\n\n")

        // 2. Chuyển đổi các khoảng trắng thụt lề Hán tự/HTML (&emsp;, &nbsp;, \u3000) thành \n\n
        clean = clean.replace(Regex("(?:&emsp;|&nbsp;|\\u3000){1,}"), "\n\n")

        // 3. Nếu văn bản không có \n\n nhưng có \n, đổi \n thành \n\n
        if (!clean.contains("\n\n") && clean.contains("\n")) {
            clean = clean.replace(Regex("(?<!\r?\n)\r?\n(?!\r?\n)"), "\n\n")
        }

        // 4. Nếu hoàn toàn không có dấu xuống dòng \n, tách đoạn theo khoảng trắng thụt lề (2+ spaces)
        if (!clean.contains("\n")) {
            clean = clean.replace(Regex(" {2,}"), "\n\n")
        }

        // 5. Chuẩn hóa 3+ dấu xuống dòng thành 2 dấu xuống dòng (\n\n)
        clean = clean.replace(Regex("(\r?\n){3,}"), "\n\n")
        return clean.trim()
    }

    private fun callNvidiaOpenAiApiStream(
        apiKey: String,
        baseUrl: String,
        modelName: String,
        sysInstruction: String,
        prompt: String,
        node: TranslationNode,
        onContentUpdated: (() -> Unit)?,
        addStep: (String) -> Unit
    ) {
        val cleanBaseUrl = baseUrl.trim().trimEnd('/')
        val endpointUrl = if (cleanBaseUrl.endsWith("/chat/completions")) {
            cleanBaseUrl
        } else {
            "$cleanBaseUrl/chat/completions"
        }

        val jsonPayload = """
        {
          "model": ${escapeJsonString(modelName)},
          "messages": [
            {
              "role": "system",
              "content": ${escapeJsonString(sysInstruction)}
            },
            {
              "role": "user",
              "content": ${escapeJsonString(prompt)}
            }
          ],
          "temperature": 0.3,
          "top_p": 0.95,
          "stream": true
        }
        """.trimIndent()

        val previewPromptText = if (prompt.length > 300) {
            prompt.take(150) + "\n...\n[Đã rút ngắn nội dung dài: ${prompt.length} ký tự]\n...\n" + prompt.takeLast(100)
        } else {
            prompt
        }

        val apiPayloadPreview = """
        {
          "model": ${escapeJsonString(modelName)},
          "messages": [
            { "role": "system", "content": ${escapeJsonString(sysInstruction)} },
            { "role": "user", "content": ${escapeJsonString(previewPromptText)} }
          ],
          "temperature": 0.3,
          "top_p": 0.95,
          "stream": true
        }
        """.trimIndent()

        addStep("[API REQUEST PAYLOAD - NVIDIA NIM]:\nEndpoint: $endpointUrl\n$apiPayloadPreview")

        val url = java.net.URL(endpointUrl)
        val conn = url.openConnection() as java.net.HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 30_000
            conn.readTimeout = 180_000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            conn.setRequestProperty("Accept", "text/event-stream")
            if (apiKey.isNotBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $apiKey")
            }

            conn.outputStream.use { os ->
                os.write(jsonPayload.toByteArray(Charsets.UTF_8))
                os.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode !in 200..299) {
                val errorMsg = try {
                    conn.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() } ?: "HTTP $responseCode"
                } catch (e: Exception) {
                    "HTTP $responseCode"
                }
                throw Exception("NVIDIA NIM API error (HTTP $responseCode): $errorMsg")
            }

            val chunkBuilder = StringBuilder()
            var lastNotifyTime = 0L
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    val currentLine = line?.trim() ?: continue
                    if (currentLine.startsWith("data: ")) {
                        val data = currentLine.substring(6).trim()
                        if (data == "[DONE]") break
                        try {
                            val jsonObj = org.json.JSONObject(data)
                            val choices = jsonObj.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val choice = choices.getJSONObject(0)
                                val delta = choice.optJSONObject("delta")
                                val contentChunk = delta?.optString("content", "")
                                if (!contentChunk.isNullOrEmpty()) {
                                    chunkBuilder.append(contentChunk)
                                    node.translatedText = normalizeNvidiaText(chunkBuilder.toString())
                                    val now = System.currentTimeMillis()
                                    if (now - lastNotifyTime >= 150L) {
                                        lastNotifyTime = now
                                        onContentUpdated?.invoke()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            // Ignore json parse error for partial lines
                        }
                    }
                }
            }
            onContentUpdated?.invoke()

            val resultText = normalizeNvidiaText(chunkBuilder.toString())
            if (resultText.isEmpty()) {
                throw Exception("NVIDIA NIM API không trả về nội dung dịch.")
            }
            node.translatedText = resultText
        } finally {
            conn.disconnect()
        }
    }

    suspend fun translateContent(
        text: String,
        apiKeys: List<String>,
        modelName: String = "gemini-3.8-flash",
        sourceLang: String = "Auto",
        targetLang: String = "Tiếng Việt",
        customInstructions: String = "",
        disclaimerText: String = "",
        logSteps: MutableList<String>,
        title: String? = null,
        onStepAdded: ((String) -> Unit)? = null,
        onContentUpdated: ((String) -> Unit)? = null,
        uiLanguage: String = "vi",
        maxWordCount: Int = 2500,
        apiProvider: String = "gemini",
        baseUrl: String = "https://integrate.api.nvidia.com/v1"
    ): Result<String> = withContext(Dispatchers.IO) {
        fun addStep(step: String) {
            logSteps.add(step)
            onStepAdded?.invoke(step)
        }

        if (apiKeys.isEmpty()) {
            val errMsg = if (apiProvider == "nvidia") {
                "Danh sách khóa API NVIDIA NIM trống. Vui lòng thiết lập trong Cài đặt."
            } else {
                "Danh sách khóa API Gemini trống. Vui lòng thiết lập trong Cài đặt."
            }
            addStep("Lỗi khởi tạo: $errMsg")
            return@withContext Result.failure(IllegalArgumentException(errMsg))
        }
        if (text.isBlank()) {
            val errMsg = "Không tìm thấy nội dung văn bản để dịch."
            addStep("Lỗi khởi tạo: $errMsg")
            return@withContext Result.failure(IllegalArgumentException(errMsg))
        }
        val chunks = splitTextIntoChunks(text, maxWordCount)
        val totalWords = countWords(text)
        val providerName = if (apiProvider == "nvidia") "NVIDIA NIM / DeepSeek API" else "Google Gemini"
        addStep("Khởi chạy tiến trình dịch thuật ($providerName). Kích thước văn bản gốc: ${text.length} ký tự (khoảng $totalWords từ), chia thành ${chunks.size} phần.")
        addStep("Ngôn ngữ dịch: $sourceLang -> $targetLang")
        addStep("Sử dụng mô hình AI: $modelName")
        if (apiProvider == "nvidia") {
            addStep("Endpoint Base URL: $baseUrl")
        }
        
        val promptLangName = when (uiLanguage) {
            "vi" -> "Tiếng Việt"
            "zh" -> "简体中文 (Chinese)"
            else -> "Tiếng Anh (English)"
        }
        addStep("Ngôn ngữ hiển thị UI: $uiLanguage | Ngôn ngữ Prompt chỉ thị: $promptLangName")
        if (customInstructions.isNotBlank()) {
            addStep("Chỉ dẫn dịch thuật cá nhân hóa: \"$customInstructions\"")
        } else {
            addStep("Chỉ dẫn dịch thuật cá nhân hóa: Không có")
        }
        if (disclaimerText.isNotBlank()) {
            addStep("Disclaimer: \"$disclaimerText\"")
        } else {
            addStep("Disclaimer: Không sử dụng")
        }

        val systemInstructionWithTitle = getSystemInstructions(
            uiLanguage = uiLanguage,
            sourceLang = sourceLang,
            targetLang = targetLang,
            customInstructions = customInstructions,
            disclaimerText = disclaimerText,
            hasTitle = true
        )

        val systemInstructionStandard = getSystemInstructions(
            uiLanguage = uiLanguage,
            sourceLang = sourceLang,
            targetLang = targetLang,
            customInstructions = customInstructions,
            disclaimerText = disclaimerText,
            hasTitle = false
        )

        val currentKeyIndexRef = intArrayOf(0)
        val chunkNodes = chunks.map { TranslationNode(it) }

        fun triggerGlobalUpdate() {
            val totalText = chunkNodes.joinToString("\n\n") { it.getTranslation() }
            onContentUpdated?.invoke(totalText)
        }

        for ((chunkIndex, chunk) in chunks.withIndex()) {
            val chunkInfo = if (chunks.size > 1) "phần ${chunkIndex + 1}/${chunks.size}" else "toàn bộ văn bản"
            translateChunkRecursive(
                chunk = chunk,
                apiKeys = apiKeys,
                currentKeyIndexRef = currentKeyIndexRef,
                modelName = modelName,
                sourceLang = sourceLang,
                targetLang = targetLang,
                customInstructions = customInstructions,
                disclaimerText = disclaimerText,
                logSteps = logSteps,
                onStepAdded = onStepAdded,
                onContentUpdated = ::triggerGlobalUpdate,
                node = chunkNodes[chunkIndex],
                uiLanguage = uiLanguage,
                isFirstChunk = (chunkIndex == 0),
                title = title,
                systemInstructionWithTitle = systemInstructionWithTitle,
                systemInstructionStandard = systemInstructionStandard,
                chunkInfo = chunkInfo,
                apiProvider = apiProvider,
                baseUrl = baseUrl
            )
        }

        val finalResultText = chunkNodes.joinToString("\n\n") { it.getTranslation() }
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

    private fun normalizeInputTextForChunking(text: String): String {
        return text
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replace(Regex("(?i)<br\\s*/?>|</?p>"), "\n")
            .replace(Regex("(?<=\\S)\\s*(?:\\u3000{2,}|(?:&emsp;){2,})"), "\n\u3000\u3000")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }

    private fun splitTextIntoChunks(text: String, maxWordCount: Int = 2500): List<String> {
        val normalizedText = normalizeInputTextForChunking(text)
        val totalWords = countWords(normalizedText)
        if (totalWords <= maxWordCount) return listOf(normalizedText)

        val paragraphs = normalizedText.split("\n")
        
        // 1. Pre-split paragraphs using maxWordCount to determine exact greedy chunk count
        val flatParagraphsMax = splitParagraphs(paragraphs, maxWordCount)
        val flatParagraphsMaxWordCounts = flatParagraphsMax.map { countWords(it) }
        val numChunks = getFlatGreedyChunksCount(flatParagraphsMaxWordCounts, maxWordCount)
        
        if (numChunks <= 1) return listOf(normalizedText)

        // 2. Calculate dynamic target chunk size
        val targetChunkSize = kotlin.math.ceil(totalWords.toDouble() / numChunks).toInt()

        // 3. Pre-split paragraphs using targetChunkSize to allow even distribution
        val flatParagraphs = splitParagraphs(paragraphs, targetChunkSize)

        // 4. Partition flatParagraphs into exactly `numChunks` chunks using binary search
        return partitionParagraphs(flatParagraphs, numChunks, maxWordCount)
    }

    private fun splitParagraphs(paragraphs: List<String>, maxWords: Int): List<String> {
        val result = mutableListOf<String>()
        for (paragraph in paragraphs) {
            val paragraphWords = countWords(paragraph)
            if (paragraphWords > maxWords) {
                var remaining = paragraph
                while (countWords(remaining) > maxWords) {
                    val splitIndex = findGoodWordSplitPoint(remaining, maxWords)
                    val part = remaining.substring(0, splitIndex)
                    result.add(part)
                    remaining = remaining.substring(splitIndex)
                }
                if (remaining.isNotEmpty()) {
                    result.add(remaining)
                }
            } else {
                result.add(paragraph)
            }
        }
        return result
    }

    private fun getFlatGreedyChunksCount(wordCounts: List<Int>, maxWordCount: Int): Int {
        var count = 1
        var currentSum = 0
        for (words in wordCounts) {
            if (currentSum + words > maxWordCount) {
                count++
                currentSum = words
            } else {
                currentSum += words
            }
        }
        return count
    }

    private fun partitionParagraphs(paragraphs: List<String>, numChunks: Int, maxWordCount: Int): List<String> {
        val paragraphWordCounts = paragraphs.map { countWords(it) }
        val totalWords = paragraphWordCounts.sum()
        
        var low = paragraphWordCounts.maxOrNull() ?: 0
        low = maxOf(low, kotlin.math.ceil(totalWords.toDouble() / numChunks).toInt())
        
        var high = maxWordCount
        high = maxOf(high, low)
        
        var optimalLimit = high
        
        while (low <= high) {
            val mid = (low + high) / 2
            if (canPartition(paragraphWordCounts, numChunks, mid)) {
                optimalLimit = mid
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        
        val chunks = mutableListOf<String>()
        var currentChunk = StringBuilder()
        var currentChunkWords = 0
        
        for (paragraph in paragraphs) {
            val paragraphWords = countWords(paragraph)
            if (currentChunk.isNotEmpty() && currentChunkWords + paragraphWords > optimalLimit) {
                chunks.add(currentChunk.toString())
                currentChunk = StringBuilder()
                currentChunkWords = 0
            }
            if (currentChunk.isNotEmpty()) {
                currentChunk.append("\n")
            }
            currentChunk.append(paragraph)
            currentChunkWords += paragraphWords
        }
        if (currentChunk.isNotEmpty()) {
            chunks.add(currentChunk.toString())
        }
        return chunks
    }

    private fun canPartition(wordCounts: List<Int>, numChunks: Int, limit: Int): Boolean {
        var chunksCount = 1
        var currentSum = 0
        for (words in wordCounts) {
            if (words > limit) return false
            if (currentSum + words > limit) {
                chunksCount++
                currentSum = words
            } else {
                currentSum += words
            }
        }
        return chunksCount <= numChunks
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
        val closingQuotes = setOf('”', '』', '」', '\'', '"', '）', ')')
        val punctuation = listOf('.', '?', '!', '。', '？', '！', ',', '，', ';', '；')
        for (i in candidate.length - 1 downTo splitIndex / 2) {
            if (candidate[i] in punctuation) {
                var splitPos = i + 1
                while (splitPos < text.length && text[splitPos] in closingQuotes) {
                    splitPos++
                }
                return splitPos
            }
        }
        
        val lastSpace = candidate.lastIndexOf(' ')
        if (lastSpace != -1 && lastSpace > splitIndex / 2) {
            return lastSpace + 1
        }
        
        return splitIndex
    }

    private fun escapeJsonString(str: String): String {
        val escaped = str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
