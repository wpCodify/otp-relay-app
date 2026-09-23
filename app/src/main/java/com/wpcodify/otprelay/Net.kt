package com.wpcodify.otprelay

import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object Net {
    /** POST form-encoded params to <serverUrl>/ingest.php. Returns the HTTP body (or error text). */
    fun ingest(serverUrl: String, params: Map<String, String>): String {
        val url = serverUrl.trimEnd('/') + "/ingest.php"
        val body = params.entries.joinToString("&") { enc(it.key) + "=" + enc(it.value) }
        return try {
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 8000
                readTimeout = 8000
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            conn.outputStream.use { os: OutputStream -> os.write(body.toByteArray()) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
            "HTTP $code: $text"
        } catch (e: Exception) {
            "ERROR: " + (e.message ?: e.toString())
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}
