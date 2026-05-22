package com.example.webreader.data

import org.json.JSONArray
import org.json.JSONObject

data class TransactionLog(
    val id: String = java.util.UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val type: String, // "Đọc ngay" or "Hàng chờ"
    val title: String,
    val url: String,
    val status: String, // "Thành công" or "Thất bại"
    val usedApiKeys: List<String>,
    val steps: List<String>,
    val geminiResponse: String?,
    val errorMessage: String?
) {
    fun toJSONObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("timestamp", timestamp)
            put("type", type)
            put("title", title)
            put("url", url)
            put("status", status)
            
            val apiKeysArray = JSONArray()
            usedApiKeys.forEach { apiKeysArray.put(it) }
            put("usedApiKeys", apiKeysArray)
            
            val stepsArray = JSONArray()
            steps.forEach { stepsArray.put(it) }
            put("steps", stepsArray)
            
            put("geminiResponse", geminiResponse ?: JSONObject.NULL)
            put("errorMessage", errorMessage ?: JSONObject.NULL)
        }
    }

    companion object {
        fun fromJSONObject(jsonObject: JSONObject): TransactionLog {
            val apiKeysList = mutableListOf<String>()
            val apiKeysArray = jsonObject.optJSONArray("usedApiKeys")
            if (apiKeysArray != null) {
                for (i in 0 until apiKeysArray.length()) {
                    apiKeysList.add(apiKeysArray.getString(i))
                }
            }
            
            val stepsList = mutableListOf<String>()
            val stepsArray = jsonObject.optJSONArray("steps")
            if (stepsArray != null) {
                for (i in 0 until stepsArray.length()) {
                    stepsList.add(stepsArray.getString(i))
                }
            }

            return TransactionLog(
                id = jsonObject.optString("id", java.util.UUID.randomUUID().toString()),
                timestamp = jsonObject.optLong("timestamp", System.currentTimeMillis()),
                type = jsonObject.optString("type", ""),
                title = jsonObject.optString("title", ""),
                url = jsonObject.optString("url", ""),
                status = jsonObject.optString("status", ""),
                usedApiKeys = apiKeysList,
                steps = stepsList,
                geminiResponse = if (jsonObject.isNull("geminiResponse")) null else jsonObject.optString("geminiResponse"),
                errorMessage = if (jsonObject.isNull("errorMessage")) null else jsonObject.optString("errorMessage")
            )
        }
    }
}
