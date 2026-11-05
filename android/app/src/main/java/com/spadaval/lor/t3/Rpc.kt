package com.spadaval.lor.t3

import org.json.JSONArray
import org.json.JSONObject

/**
 * Effect RPC over WebSocket, JSON serialization (T3's `RpcSerialization.layerJson`).
 * Each text frame holds one message object or an array of them.
 *
 * Client to server: `Request {id, tag, payload, headers}`, `Ack {requestId}` after every
 * `Chunk` (the server sends a stream's next chunk only once the last one is acked),
 * `Interrupt {requestId}` to cancel, `Ping`. Server to client: `Chunk {requestId, values}`,
 * `Exit {requestId, exit}`, `Defect {defect}`, `Pong`.
 */
object Rpc {
    sealed interface Message
    data class Chunk(val requestId: String, val values: List<JSONObject>) : Message
    /** `value` on success; on failure `error` is the cause's message, or its tag when it has none. */
    data class Exit(val requestId: String, val value: Any?, val error: String?) : Message
    data class Defect(val defect: String) : Message
    data object Pong : Message
    data class Other(val tag: String) : Message

    fun request(id: String, tag: String, payload: JSONObject): String =
        JSONObject().put("_tag", "Request").put("id", id).put("tag", tag).put("payload", payload).put("headers", JSONArray()).toString()

    fun ack(requestId: String): String = JSONObject().put("_tag", "Ack").put("requestId", requestId).toString()

    fun decode(text: String): List<Message> {
        val trimmed = text.trim()
        val objects = if (trimmed.startsWith("[")) JSONArray(trimmed).let { a -> List(a.length()) { a.getJSONObject(it) } } else listOf(JSONObject(trimmed))
        return objects.map(::message)
    }

    private fun message(m: JSONObject): Message = when (val tag = m.optString("_tag")) {
        "Chunk" -> {
            val values = m.getJSONArray("values")
            Chunk(m.get("requestId").toString(), List(values.length()) { values.getJSONObject(it) })
        }
        "Exit" -> {
            val exit = m.getJSONObject("exit")
            if (exit.optString("_tag") == "Success") Exit(m.get("requestId").toString(), exit.opt("value"), null)
            else Exit(m.get("requestId").toString(), null, failure(exit.opt("cause")))
        }
        "Defect" -> Defect(m.opt("defect")?.toString() ?: "")
        "Pong" -> Pong
        else -> Other(tag)
    }

    /** A cause is an array of `Fail {error}`, `Die {defect}` and `Interrupt`; the first one that says something wins. */
    private fun failure(cause: Any?): String {
        val parts = (cause as? JSONArray)?.let { a -> List(a.length()) { a.opt(it) } } ?: listOfNotNull(cause)
        for (part in parts) {
            val o = part as? JSONObject ?: continue
            val error = o.optJSONObject("error") ?: o.optJSONObject("defect")
            val text = error?.optString("message")?.ifEmpty { null } ?: error?.optString("_tag")?.ifEmpty { null } ?: o.opt("defect")?.toString()
            if (!text.isNullOrEmpty()) return text
        }
        return cause?.toString() ?: "failed"
    }
}
