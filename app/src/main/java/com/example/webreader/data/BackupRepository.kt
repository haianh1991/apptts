package com.example.webreader.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

class BackupRepository(private val context: Context) {

    fun createBackupJson(queueData: QueueData, bookmarks: List<BookmarkItem>): String {
        val rootObj = JSONObject().apply {
            put("app", "WebAIReader")
            put("version", 1)
            put("exportedAt", System.currentTimeMillis())

            // Folders
            val foldersArr = JSONArray()
            queueData.folders.forEach { foldersArr.put(it.toJsonObject()) }
            put("folders", foldersArr)

            // Items
            val itemsArr = JSONArray()
            queueData.items.forEach { itemsArr.put(it.toJsonObject()) }
            put("items", itemsArr)

            // Bookmarks
            val bookmarksArr = JSONArray()
            bookmarks.forEach { bookmarksArr.put(it.toJsonObject()) }
            put("bookmarks", bookmarksArr)
        }
        return rootObj.toString(2)
    }

    fun parseBackupJson(jsonString: String): Pair<QueueData, List<BookmarkItem>>? {
        return try {
            val rootObj = JSONObject(jsonString)

            val foldersList = mutableListOf<QueueFolder>()
            if (rootObj.has("folders")) {
                val foldersArr = rootObj.getJSONArray("folders")
                for (i in 0 until foldersArr.length()) {
                    foldersList.add(QueueFolder.fromJsonObject(foldersArr.getJSONObject(i)))
                }
            }

            val itemsList = mutableListOf<QueueItem>()
            if (rootObj.has("items")) {
                val itemsArr = rootObj.getJSONArray("items")
                for (i in 0 until itemsArr.length()) {
                    itemsList.add(QueueItem.fromJsonObject(itemsArr.getJSONObject(i)))
                }
            }

            val bookmarksList = mutableListOf<BookmarkItem>()
            if (rootObj.has("bookmarks")) {
                val bookmarksArr = rootObj.getJSONArray("bookmarks")
                for (i in 0 until bookmarksArr.length()) {
                    bookmarksList.add(BookmarkItem.fromJsonObject(bookmarksArr.getJSONObject(i)))
                }
            }

            Pair(QueueData(foldersList, itemsList), bookmarksList)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
