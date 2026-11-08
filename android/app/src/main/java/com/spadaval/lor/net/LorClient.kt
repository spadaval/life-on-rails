package com.spadaval.lor.net

import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** The server's /device endpoints. Blocking: call from Dispatchers.IO. */
class LorClient(baseUrl: String) {
    private val base = baseUrl.trimEnd('/')

    fun state(device: String): JSONObject = request("GET", "/device/state?device=${URLEncoder.encode(device, "UTF-8")}")

    /** What the agent page shows from LoR ([DeviceHome]). */
    fun home(device: String): DeviceHome = DeviceHome.parse(request("GET", "/device/home?device=${URLEncoder.encode(device, "UTF-8")}"))

    /** The open tasks and those done today, for the `lor` panel ([DeviceTasks]). */
    fun tasks(): DeviceTasks = DeviceTasks.parse(request("GET", "/device/tasks"))

    /** Who to be in touch with, for the `lor` panel's People ([DevicePeople]). */
    fun people(): DevicePeople = DevicePeople.parse(request("GET", "/device/people"))

    fun timer(action: String, device: String, intentId: String? = null, title: String? = null): JSONObject {
        val body = JSONObject().put("action", action).put("device", device)
        if (intentId != null) body.put("intent_id", intentId)
        if (!title.isNullOrBlank()) body.put("title", title)
        return request("POST", "/device/timer", body)
    }

    /** Tasks flagged for you that this call hands over; each comes back once. */

    fun ingest(device: String, observations: List<String>): JSONObject {
        val array = JSONArray()
        for (o in observations) array.put(JSONObject(o))
        return request("POST", "/device/ingest", JSONObject().put("device", device).put("observations", array), readTimeoutMs = 120_000)
    }

    /** Streams a file to the server as the request body; for the WhatsApp backup, which can be hundreds of MB. */
    fun uploadWhatsappBackup(device: String, file: File): JSONObject {
        val conn = URL("$base/device/whatsapp-backup?device=${URLEncoder.encode(device, "UTF-8")}").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 15_000
            conn.readTimeout = 600_000
            conn.doOutput = true
            conn.setFixedLengthStreamingMode(file.length())
            conn.setRequestProperty("content-type", "application/octet-stream")
            file.inputStream().use { input -> conn.outputStream.use { input.copyTo(it, 1 shl 16) } }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) throw IOException("$code: ${text.take(200)}")
            return JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    private fun request(method: String, path: String, body: JSONObject? = null, readTimeoutMs: Int = 20_000): JSONObject {
        val conn = URL(base + path).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 15_000
            conn.readTimeout = readTimeoutMs
            conn.setRequestProperty("accept", "application/json")
            if (body != null) {
                conn.doOutput = true
                conn.setRequestProperty("content-type", "application/json")
                conn.outputStream.use { it.write(body.toString().toByteArray()) }
            }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code !in 200..299) {
                val message = runCatching { JSONObject(text).getJSONObject("error").getString("message") }.getOrNull()
                    ?: if (code == 403) "the server did not recognise you: is Tailscale on?" else text.take(200)
                throw IOException("$code: $message")
            }
            return JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        /**
         * Why a call to LoR failed, in plain words (launcher polish 3.3): no answer at all is most
         * often Tailscale being off; an answer that refused says what the server said.
         */
        fun describe(e: Throwable): String = when (e) {
            is java.net.UnknownHostException, is java.net.ConnectException, is java.net.NoRouteToHostException,
            is java.net.SocketTimeoutException -> "LoR didn't answer. Is Tailscale on?"
            else -> (e.message ?: e.javaClass.simpleName).let { if (it.endsWith(".") || it.endsWith("?")) it else "$it." }
        }
    }
}
