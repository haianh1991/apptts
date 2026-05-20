package com.example.webreader.data

import android.content.Context
import org.json.JSONArray
import java.io.File

class QueueRepository(context: Context) {
    private val queueFile = File(context.filesDir, "tts_queue.json")

    fun getQueue(): List<QueueItem> {
        if (!queueFile.exists()) return emptyList()
        return try {
            val content = queueFile.readText()
            val arr = JSONArray(content)
            val list = mutableListOf<QueueItem>()
            for (i in 0 until arr.length()) {
                list.add(QueueItem.fromJsonObject(arr.getJSONObject(i)))
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun saveQueue(queue: List<QueueItem>) {
        try {
            val arr = JSONArray()
            queue.forEach { arr.put(it.toJsonObject()) }
            queueFile.writeText(arr.toString())
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
