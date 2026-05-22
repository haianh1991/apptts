package com.example.webreader.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class QueueData(
    val folders: List<QueueFolder>,
    val items: List<QueueItem>
)

class QueueRepository(context: Context) {
    private val queueFile = File(context.filesDir, "tts_queue.json")

    fun getQueueData(): QueueData {
        if (!queueFile.exists()) return QueueData(emptyList(), emptyList())
        return try {
            val content = queueFile.readText().trim()
            if (content.startsWith("[")) {
                // Legacy format: JSON Array of QueueItems
                val arr = JSONArray(content)
                val items = mutableListOf<QueueItem>()
                for (i in 0 until arr.length()) {
                    items.add(QueueItem.fromJsonObject(arr.getJSONObject(i)))
                }
                QueueData(emptyList(), items)
            } else {
                // New format: JSON Object containing folders and items
                val obj = JSONObject(content)
                val foldersList = mutableListOf<QueueFolder>()
                if (obj.has("folders")) {
                    val foldersArr = obj.getJSONArray("folders")
                    for (i in 0 until foldersArr.length()) {
                        foldersList.add(QueueFolder.fromJsonObject(foldersArr.getJSONObject(i)))
                    }
                }
                val itemsList = mutableListOf<QueueItem>()
                if (obj.has("items")) {
                    val itemsArr = obj.getJSONArray("items")
                    for (i in 0 until itemsArr.length()) {
                        itemsList.add(QueueItem.fromJsonObject(itemsArr.getJSONObject(i)))
                    }
                }
                QueueData(foldersList, itemsList)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            QueueData(emptyList(), emptyList())
        }
    }

    fun saveQueueData(folders: List<QueueFolder>, items: List<QueueItem>) {
        try {
            val obj = JSONObject().apply {
                val foldersArr = JSONArray()
                folders.forEach { foldersArr.put(it.toJsonObject()) }
                put("folders", foldersArr)

                val itemsArr = JSONArray()
                items.forEach { itemsArr.put(it.toJsonObject()) }
                put("items", itemsArr)
            }
            queueFile.writeText(obj.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearQueue() {
        try {
            if (queueFile.exists()) {
                queueFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
