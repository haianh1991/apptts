package com.example.webreader.data

import org.json.JSONArray
import org.json.JSONObject

data class QueueItem(
    val id: String,
    val title: String,
    val url: String,
    val paragraphs: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val folderId: String? = null,
    val novelTitle: String? = null,
    val baseUrlHost: String? = null,
    val lastReadParagraphIndex: Int = 0,
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false
) {
    fun getEffectiveHostDomain(): String {
        if (!baseUrlHost.isNullOrBlank()) return baseUrlHost
        return try {
            val uri = java.net.URI(url)
            val host = uri.host
            if (!host.isNullOrBlank()) {
                if (host.startsWith("www.", ignoreCase = true)) host.substring(4) else host
            } else {
                "trangweb.com"
            }
        } catch (e: Exception) {
            "trangweb.com"
        }
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("url", url)
            put("createdAt", createdAt)
            put("folderId", folderId ?: JSONObject.NULL)
            put("novelTitle", novelTitle ?: JSONObject.NULL)
            put("baseUrlHost", getEffectiveHostDomain())
            put("lastReadParagraphIndex", lastReadParagraphIndex)
            put("isFavorite", isFavorite)
            put("isPinned", isPinned)
            val arr = JSONArray()
            paragraphs.forEach { arr.put(it) }
            put("paragraphs", arr)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): QueueItem {
            val id = obj.getString("id")
            val title = obj.getString("title")
            val url = obj.getString("url")
            val createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            val folderId = if (obj.has("folderId") && !obj.isNull("folderId")) obj.getString("folderId") else null
            val novelTitle = if (obj.has("novelTitle") && !obj.isNull("novelTitle")) obj.getString("novelTitle") else null
            val baseUrlHost = if (obj.has("baseUrlHost") && !obj.isNull("baseUrlHost")) obj.getString("baseUrlHost") else null
            val lastReadParagraphIndex = obj.optInt("lastReadParagraphIndex", 0)
            val isFavorite = obj.optBoolean("isFavorite", false)
            val isPinned = obj.optBoolean("isPinned", false)

            val arr = obj.getJSONArray("paragraphs")
            val paras = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                paras.add(arr.getString(i))
            }
            return QueueItem(
                id = id,
                title = title,
                url = url,
                paragraphs = paras,
                createdAt = createdAt,
                folderId = folderId,
                novelTitle = novelTitle,
                baseUrlHost = baseUrlHost,
                lastReadParagraphIndex = lastReadParagraphIndex,
                isFavorite = isFavorite,
                isPinned = isPinned
            )
        }
    }
}
