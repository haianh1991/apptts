package com.example.webreader.data

import android.content.Context
import org.json.JSONArray
import java.io.File

class BookmarkRepository(context: Context) {
    private val bookmarkFile = File(context.filesDir, "bookmarks.json")

    fun getBookmarks(): List<BookmarkItem> {
        if (!bookmarkFile.exists()) return emptyList()
        return try {
            val content = bookmarkFile.readText()
            val arr = JSONArray(content)
            val list = mutableListOf<BookmarkItem>()
            for (i in 0 until arr.length()) {
                list.add(BookmarkItem.fromJsonObject(arr.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveBookmarks(bookmarks: List<BookmarkItem>) {
        try {
            val arr = JSONArray()
            bookmarks.forEach { arr.put(it.toJsonObject()) }
            bookmarkFile.writeText(arr.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
