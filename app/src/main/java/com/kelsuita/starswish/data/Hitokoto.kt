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
        // Read at most 8 KiB so a misbehaving endpoint cannot make us buffer a huge body.
        val body = connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            val builder = StringBuilder(1024)
            val buffer = CharArray(2048)
            while (builder.length < 8192) {
                val count = reader.read(buffer)
                if (count < 0) break
                builder.append(buffer, 0, minOf(count, 8192 - builder.length))
            }
            builder.toString()
        }
        JSONObject(body).optString("hitokoto").trim().takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    } finally {
        connection?.disconnect()
    }
}
