package com.example.webreader.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class UpdateManager(private val context: Context) {
    
    val currentVersionCode: Int
        get() = try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                packageInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }

    val currentVersionName: String
        get() = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }

    private fun fetchUrl(urlStr: String): Result<AppUpdateInfo> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlStr)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.useCaches = false
            
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()
                
                val json = JSONObject(response.toString())
                val updateInfo = AppUpdateInfo(
                    latestVersionCode = json.getInt("versionCode"),
                    versionName = json.getString("versionName"),
                    minVersionCode = json.getInt("minVersionCode"),
                    updateUrl = json.getString("updateUrl"),
                    releaseNotes = json.optString("releaseNotes", "")
                )
                Result.success(updateInfo)
            } else {
                Result.failure(Exception("Lỗi kết nối HTTP: $responseCode"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    suspend fun checkUpdate(configUrl: String): Result<AppUpdateInfo> = withContext(Dispatchers.IO) {
        val primaryResult = fetchUrl(configUrl)
        if (primaryResult.isSuccess) {
            return@withContext primaryResult
        }
        
        // Nếu thất bại và URL là github raw mặc định, thử fallback qua jsDelivr CDN
        if (configUrl == "https://raw.githubusercontent.com/haianh1991/apptts/main/version.json") {
            val fallbackUrl = "https://cdn.jsdelivr.net/gh/haianh1991/apptts@main/version.json"
            val fallbackResult = fetchUrl(fallbackUrl)
            if (fallbackResult.isSuccess) {
                return@withContext fallbackResult
            }
        }
        
        primaryResult
    }
}

object UpdateCheckResolver {
    data class Result(
        val showDialog: Boolean,
        val isForce: Boolean
    )

    fun resolve(currentVersionCode: Int, info: AppUpdateInfo): Result {
        return when {
            currentVersionCode < info.minVersionCode -> Result(showDialog = true, isForce = true)
            currentVersionCode < info.latestVersionCode -> Result(showDialog = true, isForce = false)
            else -> Result(showDialog = false, isForce = false)
        }
    }
}
