package com.spadaval.lor.quicksend

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * What the assistant session ([com.spadaval.lor.assist.AssistSession]) was handed about the screen,
 * passed to the sheet it opens. The session's callbacks come one by one and in no fixed order, so
 * each fills in its part and the sheet redraws as they arrive. One at a time: each invocation
 * starts a new one. In memory only.
 */
data class Capture(
    /** The foreground app's package and label, when the assist data named it. */
    val packageName: String? = null,
    val appLabel: String? = null,
    val screen: Destination.Screen = Destination.Screen(),
    /** The screenshot as JPEG; null until it arrives, or when the system gave none. */
    val jpeg: ByteArray? = null,
    /** True once the screenshot callback has run, with or without an image. */
    val shotDone: Boolean = false,
) {
    companion object {
        private val _current = MutableStateFlow<Capture?>(null)
        val current: StateFlow<Capture?> = _current.asStateFlow()

        fun begin() { _current.value = Capture() }
        fun update(change: (Capture) -> Capture) = _current.update { it?.let(change) }
        fun clear() { _current.value = null }
    }

    override fun equals(other: Any?): Boolean = this === other
    override fun hashCode(): Int = System.identityHashCode(this)
}
