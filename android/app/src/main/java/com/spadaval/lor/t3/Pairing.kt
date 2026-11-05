package com.spadaval.lor.t3

import java.net.URI
import java.net.URLDecoder

/** Where to pair and with what, read from what the owner typed (T3's `resolveRemotePairingTarget`). */
data class PairTarget(val httpBase: String, val credential: String)

object Pairing {
    /**
     * [code] is a pairing token, a pairing URL (`http://host:port/pair#token=…`, or a hosted one with
     * `?host=`), or a `t3code://pair?pairingUrl=…` app link. A non-blank [server] wins over the link's
     * host, so a link minted for `localhost` works from the emulator at `10.0.2.2`.
     */
    fun parse(server: String, code: String): PairTarget {
        var link = code.trim()
        require(link.isNotEmpty()) { "Enter a pairing token or link." }
        if (link.startsWith("t3code") && link.contains("://pair")) {
            link = param(URI(link).rawQuery, "pairingUrl") ?: throw IllegalArgumentException("The app link has no pairing URL.")
        }
        if (!link.contains("://")) return PairTarget(base(server.ifBlank { throw IllegalArgumentException("Enter the server address.") }), link)
        val uri = URI(link)
        val token = param(uri.rawFragment, "token") ?: param(uri.rawQuery, "token") ?: throw IllegalArgumentException("The link has no token.")
        val linkHost = param(uri.rawQuery, "host") ?: "${uri.scheme}://${uri.rawAuthority}"
        return PairTarget(base(server.ifBlank { linkHost }), token)
    }

    /** `host:port` or a URL, as an http(s) origin with no trailing slash. */
    fun base(server: String): String {
        val s = server.trim().trimEnd('/')
        val withScheme = if (s.contains("://")) s else "https://$s"
        val uri = URI(withScheme)
        val scheme = when (uri.scheme) { "ws" -> "http"; "wss" -> "https"; "http", "https" -> uri.scheme; else -> throw IllegalArgumentException("Unsupported address: ${uri.scheme}") }
        return "$scheme://${uri.rawAuthority}"
    }

    private fun param(raw: String?, name: String): String? = raw?.split('&')
        ?.map { it.split('=', limit = 2) }
        ?.firstOrNull { URLDecoder.decode(it[0], "UTF-8") == name && it.size == 2 }
        ?.let { URLDecoder.decode(it[1], "UTF-8").trim() }
        ?.ifEmpty { null }
}
