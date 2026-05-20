package com.example.webreader.data

import org.json.JSONObject

data class BookmarkItem(
    val id: String,
    val title: String,
    val url: String
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("url", url)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): BookmarkItem {
            return BookmarkItem(
                id = obj.getString("id"),
                title = obj.getString("title"),
                url = obj.getString("url")
            )
        }
    }
}
