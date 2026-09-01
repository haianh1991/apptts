package com.example.webreader.data

data class NovelSeries(
    val seriesId: String,
    val title: String,
    val hostDomain: String,
    val items: List<QueueItem>,
    val folderId: String? = null,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val chapterCount: Int
        get() = items.size

    val totalParagraphs: Int
        get() = items.sumOf { it.paragraphs.size }

    val readProgressPercent: Float
        get() {
            if (items.isEmpty()) return 0f
            val totalParas = totalParagraphs
            if (totalParas == 0) return 0f
            var readParas = 0
            items.forEach { item ->
                readParas += item.lastReadParagraphIndex.coerceIn(0, item.paragraphs.size)
            }
            return (readParas.toFloat() / totalParas.toFloat()).coerceIn(0f, 1f)
        }

    val lastReadChapterItem: QueueItem?
        get() {
            return items.firstOrNull { it.lastReadParagraphIndex > 0 } ?: items.lastOrNull()
        }

    val lastReadChapterTitle: String?
        get() {
            val item = items.firstOrNull { it.lastReadParagraphIndex > 0 }
                ?: items.lastOrNull()
            return item?.title
        }

    companion object {
        fun extractNovelTitle(title: String): String {
            val cleaned = title.trim()
            val chapterKeywords = listOf("chương", "chuong", "tập", "tap", "hồi", "hoi", "chapter", "chap", "c1", "c2", "c3", "c4", "c5", "c6", "c7", "c8", "c9")
            
            val delimiters = listOf(" - ", " – ", " : ", ": ", " | ")
            for (delim in delimiters) {
                if (cleaned.contains(delim)) {
                    val parts = cleaned.split(delim)
                    if (parts.isNotEmpty()) {
                        val firstPart = parts[0].trim()
                        val lowerFirst = firstPart.lowercase()
                        if (!chapterKeywords.any { lowerFirst.contains(it) }) {
                            return firstPart
                        }
                    }
                }
            }

            val regexChapter = Regex("(?i)\\s*[-–|:]?\\s*(chương|chuong|chapter|chap|tập|tap)\\s*\\d+.*")
            val result = cleaned.replace(regexChapter, "").trim()
            return if (result.isNotBlank()) result else cleaned
        }

        fun generateSeriesId(hostDomain: String, storyTitle: String): String {
            val normHost = hostDomain.trim().lowercase()
            val normTitle = extractNovelTitle(storyTitle).trim().lowercase()
            return "${normHost}_${normTitle.hashCode()}"
        }

        fun groupItemsIntoSeries(
            queueItems: List<QueueItem>,
            folders: List<QueueFolder> = emptyList()
        ): List<NovelSeries> {
            if (queueItems.isEmpty() && folders.isEmpty()) return emptyList()

            val seriesList = mutableListOf<NovelSeries>()
            val processedItemIds = mutableSetOf<String>()

            // 1. Ưu tiên hàng đầu: Mỗi Thư mục của người dùng là 1 Bộ truyện
            folders.forEach { folder ->
                val folderItems = queueItems.filter { it.folderId == folder.id }.sortedBy { it.createdAt }
                val host = folderItems.firstOrNull()?.getEffectiveHostDomain() ?: "Tủ sách"
                val seriesId = "folder_${folder.id}"
                val maxUpdated = folderItems.maxOfOrNull { it.createdAt } ?: folder.createdAt
                val hasFav = folderItems.any { it.isFavorite }
                val hasPin = folderItems.any { it.isPinned }

                seriesList.add(
                    NovelSeries(
                        seriesId = seriesId,
                        title = folder.name,
                        hostDomain = host,
                        items = folderItems,
                        folderId = folder.id,
                        isFavorite = hasFav,
                        isPinned = hasPin,
                        updatedAt = maxUpdated
                    )
                )
                folderItems.forEach { processedItemIds.add(it.id) }
            }

            // 2. Với các chương chưa thuộc thư mục nào: gom nhóm theo Tên truyện + Tên miền web
            val remainingItems = queueItems.filter { !processedItemIds.contains(it.id) }
            if (remainingItems.isNotEmpty()) {
                val groups = mutableMapOf<String, MutableList<QueueItem>>()
                remainingItems.forEach { item ->
                    val host = item.getEffectiveHostDomain()
                    val seriesTitle = item.novelTitle ?: extractNovelTitle(item.title)
                    val key = "${host.lowercase()}_${seriesTitle.lowercase()}"
                    groups.getOrPut(key) { mutableListOf() }.add(item)
                }

                groups.forEach { (key, itemsInGroup) ->
                    val firstItem = itemsInGroup.first()
                    val host = firstItem.getEffectiveHostDomain()
                    val seriesTitle = firstItem.novelTitle ?: extractNovelTitle(firstItem.title)
                    val seriesId = generateSeriesId(host, seriesTitle)
                    val maxUpdated = itemsInGroup.maxOfOrNull { it.createdAt } ?: System.currentTimeMillis()
                    val hasFav = itemsInGroup.any { it.isFavorite }
                    val hasPin = itemsInGroup.any { it.isPinned }

                    seriesList.add(
                        NovelSeries(
                            seriesId = seriesId,
                            title = seriesTitle,
                            hostDomain = host,
                            items = itemsInGroup.sortedBy { it.createdAt },
                            folderId = null,
                            isFavorite = hasFav,
                            isPinned = hasPin,
                            updatedAt = maxUpdated
                        )
                    )
                }
            }

            return seriesList.sortedWith(
                compareByDescending<NovelSeries> { it.isPinned }
                    .thenByDescending { it.updatedAt }
            )
        }
    }
}
