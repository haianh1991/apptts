package com.example.webreader.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.File

class TransactionLogRepository(private val context: Context) {
    private val logFile = File(context.filesDir, "translation_logs.json")

    suspend fun getLogs(): List<TransactionLog> = withContext(Dispatchers.IO) {
        if (!logFile.exists()) return@withContext emptyList()
        try {
            val content = logFile.readText()
            if (content.isBlank()) return@withContext emptyList()
            val array = JSONArray(content)
            val logs = mutableListOf<TransactionLog>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                logs.add(TransactionLog.fromJSONObject(obj))
            }
            logs.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun addLog(log: TransactionLog) = withContext(Dispatchers.IO) {
        val currentLogs = getLogs().toMutableList()
        currentLogs.add(0, log) // Thêm lên đầu (mới nhất)
        
        // Giới hạn 50 log
        val trimmedLogs = if (currentLogs.size > 50) currentLogs.take(50) else currentLogs
        
        try {
            val array = JSONArray()
            trimmedLogs.forEach { array.put(it.toJSONObject()) }
            logFile.writeText(array.toString(2))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun clearLogs() = withContext(Dispatchers.IO) {
        try {
            if (logFile.exists()) {
                logFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
