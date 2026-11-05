package com.spadaval.lor.t3

import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** The server refused the credential: pair again. */
class T3AuthRejected(message: String) : IOException(message)

/**
 * T3's session auth over HTTP, as a bearer client (`bootstrapRemoteBearerSession`). Blocking: call
 * from Dispatchers.IO. No message here carries a token.
 */
object T3Auth {
    /** Trades a one-time pairing credential for a long-lived bearer access token (30 days). */
    fun pair(target: PairTarget, label: String): String {
        val form = listOf(
            "grant_type" to "urn:ietf:params:oauth:grant-type:token-exchange",
            "subject_token" to target.credential,
            "subject_token_type" to "urn:t3:params:oauth:token-type:environment-bootstrap",
            "requested_token_type" to "urn:ietf:params:oauth:token-type:access_token",
            "client_label" to label,
            "client_device_type" to "mobile",
            "client_os" to "android",
        ).joinToString("&") { (k, v) -> "$k=${URLEncoder.encode(v, "UTF-8")}" }
        return post("${target.httpBase}/oauth/token", form, "application/x-www-form-urlencoded", null).getString("access_token")
    }

    /** A short-lived, single-use ticket for one `/ws` upgrade. */
    fun webSocketTicket(httpBase: String, accessToken: String): String =
        post("$httpBase/api/auth/websocket-ticket/rpc", null, null, accessToken).getString("ticket")

    private fun post(url: String, body: String?, contentType: String?, bearer: String?): JSONObject {
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.setRequestProperty("accept", "application/json")
            if (bearer != null) conn.setRequestProperty("authorization", "Bearer $bearer")
            conn.doOutput = true
            if (contentType != null) conn.setRequestProperty("content-type", contentType)
            conn.outputStream.use { it.write((body ?: "").toByteArray()) }
            val code = conn.responseCode
            val text = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.use { it.readText() } ?: ""
            if (code == 401 || code == 403) {
                val reason = runCatching { JSONObject(text).optString("reason").ifEmpty { JSONObject(text).optString("code") } }.getOrNull()
                throw T3AuthRejected("$code ${reason ?: ""}".trim())
            }
            if (code !in 200..299) throw IOException("$code from ${URL(url).path}")
            return JSONObject(text)
        } finally {
            conn.disconnect()
        }
    }
}
