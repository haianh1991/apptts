package com.example.webreader.data

import org.json.JSONObject

data class QueueFolder(
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("name", name)
            put("createdAt", createdAt)
        }
    }

    companion object {
        fun fromJsonObject(obj: JSONObject): QueueFolder {
            return QueueFolder(
                id = obj.getString("id"),
                name = obj.getString("name"),
                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
            )
        }
    }
}
