package com.kelsuita.starswish.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/** Request a sentence only on first launch or an explicit tap; never block the UI thread. */
suspend fun fetchHitokoto(): String? = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        connection = (URL("https://v1.hitokoto.cn/").openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            requestMethod = "GET"
        }
        if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText().take(8192) }
        JSONObject(body).optString("hitokoto").trim().takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}
