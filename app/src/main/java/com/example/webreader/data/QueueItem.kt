package com.example.webreader.data

import org.json.JSONArray
import org.json.JSONObject

data class QueueItem(
    val id: String,
    val title: String,
    val url: String,
    val paragraphs: List<String>,
    val createdAt: Long = System.currentTimeMillis(),
    val folderId: String? = null
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("url", url)
            put("createdAt", createdAt)
            put("folderId", folderId ?: JSONObject.NULL)
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
            val arr = obj.getJSONArray("paragraphs")
            val paras = mutableListOf<String>()
            for (i in 0 until arr.length()) {
                paras.add(arr.getString(i))
            }
            return QueueItem(id, title, url, paras, createdAt, folderId)
        }
    }
}
