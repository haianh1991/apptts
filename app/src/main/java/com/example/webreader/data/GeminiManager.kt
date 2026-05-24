package com.example.webreader.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
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
        if (bestIndex != -1) return bestIndex + 1

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
        if (bestIndex != -1) return bestIndex + 1

        // Ưu tiên 4: Trung điểm
        return mid
    }

    private fun isSafetyError(e: Throwable): Boolean {
        val errText = getDetailedErrorMessage(e).lowercase()
        val keywords = listOf(
            "safety",
            "blocked",
            "prohibited",
            "finishreason",
            "serializationexception",
            "missingfieldexception",
            "provided content"
        )
        return keywords.any { errText.contains(it) }
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
        modelName: String = "gemini-3.5-flash",
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

        val customPart = if (hasCustom) {
            "\n\n# CHỈ DẪN DỊCH THUẬT BỔ SUNG\n${customInstructions.trim()}"
        } else ""

        val customPartEn = if (hasCustom) {
            "\n\n# ADDITIONAL TRANSLATION INSTRUCTIONS\n${customInstructions.trim()}"
        } else ""

        val customPartZh = if (hasCustom) {
            "\n\n# 附加翻译说明\n${customInstructions.trim()}"
        } else ""

        val srcTextVi = if (sourceLang.equals("Auto", ignoreCase = true)) "tự động phát hiện" else sourceLang
        val srcTextZh = if (sourceLang.equals("Auto", ignoreCase = true)) "自动检测" else sourceLang
        val srcTextEn = if (sourceLang.equals("Auto", ignoreCase = true)) "automatically detected" else sourceLang

        var systemInstruction = ""

        if (uiLanguage == "vi") {
            systemInstruction = if (hasTitle) {
                """
                    # VAI TRÒ & MỤC TIÊU
                    Bạn là một trợ lý dịch thuật và trích xuất nội dung thông minh. Nhiệm vụ của bạn là nhận tiêu đề gốc và văn bản thô được trích xuất từ một trang web (chứa các thành phần thừa như menu điều hướng, quảng cáo xen kẽ, các nút bấm chuyển trang, bình luận bên lề), lọc bỏ rác và dịch sang ngôn ngữ đích một cách tự nhiên và chính xác nhất.
                    
                    # QUY TRÌNH THỰC HIỆN
                    1. **Dịch tiêu đề**: Dịch tiêu đề gốc từ ngôn ngữ gốc ($srcTextVi) sang ngôn ngữ đích ($targetLang) một cách tự nhiên.
                    2. **Định dạng tiêu đề**: Trả về tiêu đề dịch ở dòng đầu tiên dưới định dạng chính xác sau đây:
                       Title: [Tiêu đề đã dịch]
                    3. **Lọc nội dung chính**: Nhận diện phần nội dung bài viết/chương truyện chính trong văn bản thô. Loại bỏ hoàn toàn các thành phần thừa, quảng cáo xen kẽ giữa các câu, các nút bấm (như "Chương sau", "Mục lục", "Trang chủ") và bình luận không liên quan của độc giả.
                    4. **Dịch nội dung**: Dịch phần nội dung chính vừa lọc được từ ngôn ngữ gốc ($srcTextVi) sang ngôn ngữ đích ($targetLang) một cách tự nhiên, mượt mà, trôi chảy nhất và chuẩn ngữ cảnh văn học/báo chí của ngôn ngữ đích.
                    5. **Giữ cấu trúc**: Giữ nguyên cấu trúc phân đoạn (phân tách rõ ràng bằng các dòng trống \n\n).
                    6. **Không thêm chú thích**: Trả về nội dung dịch bên dưới tiêu đề dịch, cách nhau bởi hai dấu xuống dòng (\n\n). Tuyệt đối không thêm bất kỳ văn bản giới thiệu hay chú thích ngoài lề nào khác. Chỉ trả về tiêu đề dịch định dạng Title: ... và văn bản dịch sạch của nội dung chính.$customPart
                """.trimIndent()
            } else {
                """
                    # VAI TRÒ & MỤC TIÊU
                    Bạn là một trợ lý dịch thuật và trích xuất nội dung thông minh. Nhiệm vụ của bạn là nhận văn bản thô được trích xuất từ một trang web (chứa các thành phần thừa như menu điều hướng, quảng cáo xen kẽ, các nút bấm chuyển trang, bình luận bên lề), lọc bỏ rác và dịch sang ngôn ngữ đích một cách tự nhiên và chính xác nhất.
                    
                    # QUY TRÌNH THỰC HIỆN
                    1. **Lọc nội dung chính**: Nhận diện phần nội dung bài viết/chương truyện chính trong văn bản thô. Loại bỏ hoàn toàn các thành phần thừa, quảng cáo xen kẽ giữa các câu, các nút bấm (như "Chương sau", "Mục lục", "Trang chủ") và bình luận không liên quan của độc giả.
                    2. **Dịch nội dung**: Dịch phần nội dung chính vừa lọc được từ ngôn ngữ gốc ($srcTextVi) sang ngôn ngữ đích ($targetLang) một cách tự nhiên, mượt mà, trôi chảy nhất và chuẩn ngữ cảnh văn học/báo chí của ngôn ngữ đích.
                    3. **Giữ cấu trúc**: Giữ nguyên cấu trúc phân đoạn (phân tách rõ ràng bằng các dòng trống \n\n).
                    4. **Không thêm chú thích**: Tuyệt đối không thêm bất kỳ văn bản giới thiệu hay chú thích ngoài lề nào khác. Chỉ trả về văn bản dịch sạch của nội dung chính.$customPart
                """.trimIndent()
            }
        } else if (uiLanguage == "zh") {
            systemInstruction = if (hasTitle) {
                """
                    # 角色与目标
                    您是一个智能翻译和内容提取助手。您的任务是接收原始标题和从网页提取的原始文本（其中可能包含导航菜单、广告、翻页按钮和无关评论等噪点），过滤噪点并进行高质量翻译。
                    
                    # 执行步骤
                    1. **翻译标题**: 将原始标题从源语言 ($srcTextZh) 自然且专业地翻译为目标语言 ($targetLang)。
                    2. **格式化标题**: 在第一行以以下确切格式返回翻译后的标题：
                       Title: [翻译后的标题]
                    3. **提取主体内容**: 识别原始文本中文章或章节的主体内容。完全过滤并移除所有噪点，包括广告、导航链接/按钮（如“下一章”、“目录”、“首页”）以及无关评论。
                    4. **翻译主体内容**: 将过滤后的主体内容从源语言 ($srcTextZh) 自然、通顺且流畅地翻译为目标语言 ($targetLang)，并匹配目标语言的文学或新闻等专业文体特点。
                    5. **保留结构**: 保留段落结构（使用空行 \n\n 明确分隔）。
                    6. **无额外说明**: 在翻译后的标题下方返回翻译内容，用两个换行符 (\n\n) 分隔。不要包含任何介绍性或解释性文本（例如“以下是翻译内容...”）。仅返回格式为 'Title: ...' 的翻译标题 and 干净的主体翻译内容。$customPartZh
                """.trimIndent()
            } else {
                """
                    # 角色与目标
                    您是一个智能翻译和内容提取助手。您的任务是接收从网页提取的原始文本（其中可能包含导航菜单、广告、翻页按钮和无关评论等噪点），过滤噪点并进行高质量翻译。
                    
                    # 执行步骤
                    1. **提取主体内容**: 识别原始文本中文章或章节的主体内容。完全过滤并移除所有噪点，包括广告、导航链接/按钮（如“下一章”、“目录”、“首页”）以及无关评论。
                    2. **翻译主体内容**: 将过滤后的主体内容从源语言 ($srcTextZh) 自然、通顺且流畅地翻译为目标语言 ($targetLang)，并匹配目标语言的文学或新闻等专业文体特点。
                    3. **保留结构**: 保留段落结构（使用空行 \n\n 明确分隔）。
                    4. **无额外说明**: 不要包含任何介绍性或解释性文本（例如“以下是翻译内容...”）。仅返回干净的主体翻译内容。$customPartZh
                """.trimIndent()
            }
        } else {
            systemInstruction = if (hasTitle) {
                """
                    # ROLE & OBJECTIVE
                    You are a professional translator and clean content extractor. Your task is to receive the original title and raw text extracted from a webpage (which may contain clutter such as navigation menus, advertisements, page buttons, and irrelevant comments), remove all clutter, and translate it into the target language.
                    
                    # EXECUTION STEPS
                    1. **Translate Title**: Translate the original title from the source language ($srcTextEn) into the target language ($targetLang) naturally and professionally.
                    2. **Format Title**: Return the translated title on the first line in this exact format:
                       Title: [translated title]
                    3. **Extract Main Content**: Identify the main content of the article or chapter in the raw text. Completely remove all clutter, including advertisements, navigation links/buttons (such as "Next Chapter", "Table of Contents", "Home"), and irrelevant comments.
                    4. **Translate Main Content**: Translate the cleaned main content from the source language ($srcTextEn) into the target language ($targetLang) in a natural, smooth, and fluent manner, matching the professional literary or journalistic style of the target language.
                    5. **Preserve Structure**: Preserve the paragraph structure (clearly separated by empty lines \n\n).
                    6. **No Extra Annotations**: Return the translated content below the translated title, separated by two newlines (\n\n). Do NOT include any introductory or explanatory text. Only return the translated title formatted as 'Title: ...' and the clean translated main content.$customPartEn
                """.trimIndent()
            } else {
                """
                    # ROLE & OBJECTIVE
                    You are a professional translator and clean content extractor. Your task is to receive raw text extracted from a webpage (which may contain clutter such as navigation menus, advertisements, page buttons, and irrelevant comments), remove all clutter, and translate it into the target language.
                    
                    # EXECUTION STEPS
                    1. **Extract Main Content**: Identify the main content of the article or chapter in the raw text. Completely remove all clutter, including advertisements, navigation links/buttons (such as "Next Chapter", "Table of Contents", "Home"), and irrelevant comments.
                    2. **Translate Main Content**: Translate the cleaned main content from the source language ($srcTextEn) into the target language ($targetLang) in a natural, smooth, and fluent manner, matching the professional literary or journalistic style of the target language.
                    3. **Preserve Structure**: Preserve the paragraph structure (clearly separated by empty lines \n\n).
                    4. **No Extra Annotations**: Do NOT include any introductory or explanatory text. Only return the clean translated main content.$customPartEn
                """.trimIndent()
            }
        }

        // Always put Disclaimer at the top of system instructions if available
        if (hasDisclaimer) {
            val titleText = if (uiLanguage == "vi") "LƯU Ý HỆ THỐNG (DISCLAIMER)"
            else if (uiLanguage == "zh") "系统提示 (DISCLAIMER)"
            else "SYSTEM WARNING / DISCLAIMER"
            systemInstruction = "# $titleText\n${disclaimerText.trim()}\n\n$systemInstruction"
        }

        return systemInstruction
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
        depth: Int = 0
    ): Boolean = withContext(Dispatchers.IO) {
        fun addStep(step: String) {
            logSteps.add(step)
            onStepAdded?.invoke(step)
        }

        var success = false
        val now = System.currentTimeMillis()
        val activeKeys = apiKeys.filter { (keyCooldowns[it] ?: 0L) <= now }
        val currentKeys = if (activeKeys.isNotEmpty()) activeKeys else apiKeys
        val keysCount = currentKeys.size
        val chunkErrors = mutableListOf<String>()

        for (attempt in 0 until keysCount) {
            val keyIdx = (currentKeyIndexRef[0] + attempt) % keysCount
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
                val generativeModel = GenerativeModel(
                    modelName = modelName,
                    apiKey = apiKey,
                    requestOptions = RequestOptions(timeout = 180.seconds),
                    safetySettings = safetySettings,
                    systemInstruction = content {
                        text(if (isFirstChunk && !title.isNullOrBlank()) systemInstructionWithTitle else systemInstructionStandard)
                    }
                )

                val prompt = if (isFirstChunk && !title.isNullOrBlank()) {
                    when (uiLanguage) {
                        "vi" -> {
                            """
                                Tiêu đề gốc cần dịch:
                                $title
                                
                                Dưới đây là văn bản trang web cần dịch sang ngôn ngữ đích ($targetLang):
                                
                                $chunk
                            """.trimIndent()
                        }
                        "zh" -> {
                            """
                                要翻译的原始标题：
                                $title
                                
                                以下是要翻译为 $targetLang 的网页原始文本：
                                
                                $chunk
                            """.trimIndent()
                        }
                        else -> {
                            """
                                Original title to translate:
                                $title
                                
                                Here is the raw webpage text to translate into $targetLang:
                                
                                $chunk
                            """.trimIndent()
                        }
                    }
                } else {
                    when (uiLanguage) {
                        "vi" -> {
                            val srcLangText = if (sourceLang.equals("Auto", ignoreCase = true)) "tự động phát hiện" else sourceLang
                            """
                                Hãy dịch văn bản thô dưới đây từ ngôn ngữ gốc ($srcLangText) sang ngôn ngữ đích ($targetLang). Hãy lọc bỏ các thành phần quảng cáo hoặc nút điều hướng nếu có, dịch sát nghĩa và tự nhiên nhất:
                                
                                $chunk
                            """.trimIndent()
                        }
                        "zh" -> {
                            val srcLangTextZh = if (sourceLang.equals("Auto", ignoreCase = true)) "自动检测" else sourceLang
                            """
                                请将以下原始文本从源语言 ($srcLangTextZh) 翻译为目标语言 ($targetLang)。如果存在任何广告或导航链接，请进行过滤，并自然且专业地翻译核心内容：
                                
                                $chunk
                            """.trimIndent()
                        }
                        else -> {
                            val srcLangTextEn = if (sourceLang.equals("Auto", ignoreCase = true)) "automatically detected" else sourceLang
                            """
                                Please translate the following raw text from the source language ($srcLangTextEn) into the target language ($targetLang). Filter out any advertisements or navigation links if present, and translate the core content naturally and professionally:
                                
                                $chunk
                            """.trimIndent()
                        }
                    }
                }

                val sysInstruction = if (isFirstChunk && !title.isNullOrBlank()) systemInstructionWithTitle else systemInstructionStandard
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
                var hasSafetyFinishReason = false
                
                responseStream.collect { response ->
                    val candidate = response.candidates.firstOrNull()
                    if (candidate?.finishReason?.name == "SAFETY" || candidate?.finishReason?.name == "PROHIBITED_CONTENT") {
                        hasSafetyFinishReason = true
                        throw Exception("Gemini API chặn nội dung do an toàn (finishReason: ${candidate.finishReason?.name})")
                    }
                    val chunkText = response.text
                    if (chunkText != null) {
                        chunkBuilder.append(chunkText)
                        node.translatedText = chunkBuilder.toString()
                        onContentUpdated?.invoke()
                    }
                }

                if (hasSafetyFinishReason) {
                    throw Exception("Gemini API chặn nội dung do an toàn (SAFETY finishReason)")
                }

                val translatedText = chunkBuilder.toString()
                if (translatedText.isNotEmpty()) {
                    node.translatedText = translatedText
                    success = true
                    currentKeyIndexRef[0] = keyIdx
                    val successMsg = if (depth > 0) {
                        "Dịch thành công phần con $chunkInfo với API Key số $keyNum ($keySnippet)."
                    } else {
                        "Dịch thành công $chunkInfo với API Key số $keyNum ($keySnippet)."
                    }
                    addStep(successMsg)
                    break
                } else {
                    throw Exception("Gemini API không trả về nội dung dịch.")
                }
            } catch (e: Exception) {
                val detailedErr = getDetailedErrorMessage(e)
                val isSafety = isSafetyError(e)
                val errText = detailedErr.lowercase()
                val isQuota = errText.contains("429") || errText.contains("resource_exhausted") || errText.contains("quota")
                
                if (isQuota) {
                    val cooldownSec = getSecondsUntilDailyReset()
                    val isDaily = errText.contains("day") || errText.contains("daily") || errText.contains("perday")
                    val finalCooldownSec = if (isDaily) cooldownSec else 60L
                    keyCooldowns[apiKey] = System.currentTimeMillis() + (finalCooldownSec * 1000L)
                    
                    val durationStr = if (isDaily) {
                        val hr = finalCooldownSec / 3600
                        val min = (finalCooldownSec % 3600) / 60
                        "${hr} giờ ${min} phút (sẽ tự động reset lúc 15:00)"
                    } else {
                        "60 giây"
                    }
                    val quotaType = if (isDaily) "ngày (Per Day)" else "phút (Per Minute)"
                    addStep("API Key số $keyNum ($keySnippet) bị tạm khóa trong $durationStr do vượt hạn ngạch $quotaType (HTTP 429).")
                }

                val errLabel = if (isSafety) "Lỗi An toàn (Safety Blocked)" else if (isQuota) "Lỗi Hạn ngạch (Quota Exceeded)" else "Lỗi kỹ thuật"
                
                addStep("Thất bại $chunkInfo với API Key số $keyNum ($keySnippet) [$errLabel]. Chi tiết: $detailedErr")
                chunkErrors.add("[$keySnippet] [$errLabel]: $detailedErr")

                if (isSafety && chunk.length > 250) {
                    addStep("Kích hoạt cơ chế tự động chia nhỏ đoạn văn $chunkInfo (kích thước: ${chunk.length} ký tự > 250 ký tự) làm hai để gửi lại.")
                    
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
                        depth = depth + 1
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
                        depth = depth + 1
                    )
                    
                    success = leftSuccess && rightSuccess
                    return@withContext success
                }
            }
        }

        if (!success) {
            val hasSafetyErr = chunkErrors.any { it.contains("Lỗi An toàn") }
            val fallbackHeader = if (hasSafetyErr) {
                when (uiLanguage) {
                    "vi" -> "--- [Đoạn này bị bộ lọc Gemini API chặn dịch thuật do chính sách nội dung, hiển thị văn bản gốc] ---"
                    "zh" -> "--- [该段落被 Gemini API 安全过滤器拦截，显示原文] ---"
                    else -> "--- [This section was blocked by Gemini API safety filter, showing original text] ---"
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

    suspend fun translateContent(
        text: String,
        apiKeys: List<String>,
        modelName: String = "gemini-3.5-flash",
        sourceLang: String = "Auto",
        targetLang: String = "Tiếng Việt",
        customInstructions: String = "",
        disclaimerText: String = "",
        logSteps: MutableList<String>,
        title: String? = null,
        onStepAdded: ((String) -> Unit)? = null,
        onContentUpdated: ((String) -> Unit)? = null,
        uiLanguage: String = "vi"
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
        addStep("Ngôn ngữ dịch: $sourceLang -> $targetLang")
        addStep("Sử dụng mô hình AI: $modelName")
        
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
                chunkInfo = chunkInfo
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

    private fun splitTextIntoChunks(text: String, maxWordCount: Int = 8000): List<String> {
        val totalWords = countWords(text)
        if (totalWords <= maxWordCount) return listOf(text)

        val paragraphs = text.split("\n")
        
        // 1. Pre-split paragraphs using maxWordCount to determine exact greedy chunk count
        val flatParagraphsMax = splitParagraphs(paragraphs, maxWordCount)
        val flatParagraphsMaxWordCounts = flatParagraphsMax.map { countWords(it) }
        val numChunks = getFlatGreedyChunksCount(flatParagraphsMaxWordCounts, maxWordCount)
        
        if (numChunks <= 1) return listOf(text)

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

    private fun escapeJsonString(str: String): String {
        val escaped = str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
        return "\"$escaped\""
    }
}
