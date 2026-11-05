package com.spadaval.lor.t3

/**
 * Deep links into the T3 app. It opens a thread at `<scheme>://threads/<environmentId>/<threadId>`
 * (`THREAD_LINKING_PREFIX` in `apps/mobile/src/Stack.tsx`), each segment percent-encoded as
 * `encodeURIComponent` does. The scheme and package differ by build variant (`VARIANT_CONFIG` in
 * `apps/mobile/app.config.ts`). The environment id names the paired server: [T3Connection.environmentId].
 * Pure.
 */
object T3Links {
    /** A build of the T3 app: its package and URL scheme. */
    data class App(val packageName: String, val scheme: String)

    /** In the order the launcher prefers them: production (Sumeet's fork builds), then preview, then dev. */
    val APPS = listOf(
        App("com.t3tools.t3code", "t3code"),
        App("com.t3tools.t3code.preview", "t3code-preview"),
        App("com.t3tools.t3code.dev", "t3code-dev"),
    )

    fun thread(app: App, environmentId: String, threadId: String): String =
        "${app.scheme}://threads/${component(environmentId)}/${component(threadId)}"

    /** JavaScript's `encodeURIComponent`: everything but `A-Z a-z 0-9 - _ . ! ~ * ' ( )` as UTF-8 escapes. */
    fun component(s: String): String = buildString {
        for (b in s.toByteArray(Charsets.UTF_8)) {
            val c = b.toInt() and 0xFF
            if (c < 0x80 && (c.toChar().isLetterOrDigit() || c.toChar() in "-_.!~*'()")) append(c.toChar())
            else append('%').append("0123456789ABCDEF"[c shr 4]).append("0123456789ABCDEF"[c and 15])
        }
    }
}
