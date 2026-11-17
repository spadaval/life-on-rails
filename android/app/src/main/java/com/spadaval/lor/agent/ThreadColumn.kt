package com.spadaval.lor.agent

import android.os.SystemClock
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spadaval.lor.t3.Commands
import com.spadaval.lor.t3.T3Connection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/*
 * The T3 panel's wide form (lor-iabt), the app's half: the thread chosen, the replies typed and
 * sent, and the thread's reads. The thread is read with `getThreadProjection` when it is shown and
 * again when its shell row's updatedAt changes (decision note on lor-iabt); messages are plain text.
 * The column's drawing is homecore's (`ThreadView.kt`, lor-rgx4).
 */

/** The thread chosen in the wide form, kept for the process, so a fold and an unfold keep it. */
object ChosenThread {
    var id by mutableStateOf<String?>(null)
}

/**
 * Replies typed in the thread column, by thread, held for the process so a fold or a trip away
 * keeps a draft. A send runs in the process's scope, so leaving the panel doesn't cancel it; at
 * most one runs per thread. On success the draft clears (unless it was edited meanwhile); on
 * failure it stays, with the reason ([failure]).
 */
object Replies {
    var drafts by mutableStateOf(emptyMap<String, String>())
        private set
    var sending by mutableStateOf(emptySet<String>())
        private set
    var failures by mutableStateOf(emptyMap<String, String>())
        private set

    /**
     * What the box says when a send fails. Only with no open socket is it known not to have gone;
     * after a timeout or a dropped connection T3 may still take it (seen on the scratch server: a
     * reply timed out while the server was paused and arrived when it resumed), so the box says
     * so rather than inviting a second copy.
     */
    fun failure(reason: String): String =
        if (reason == NOT_CONNECTED) "Not sent: $reason"
        else "Not confirmed: $reason It may still reach the thread, so check it before sending again."

    /** [T3Connection.call]'s refusal when no socket is open: nothing was sent. */
    private const val NOT_CONNECTED = "Not connected to T3."

    fun edit(threadId: String, text: String) {
        drafts = drafts + (threadId to text)
    }

    fun send(work: CoroutineScope, connection: T3Connection, threadId: String) {
        val draft = drafts[threadId].orEmpty()
        val text = draft.trim()
        if (text.isEmpty() || threadId in sending) return
        sending = sending + threadId
        failures = failures - threadId
        work.launch {
            try {
                connection.send(threadId, text)
                if (drafts[threadId] == draft) drafts = drafts - threadId
                Log.i(TAG, "reply sent to $threadId")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val reason = T3Connection.describe(e)
                failures = failures + (threadId to failure(reason))
                Log.i(TAG, "reply to $threadId failed: $reason")
            } finally {
                sending = sending - threadId
            }
        }
    }
}

private const val TAG = "T3Thread"

/** What one read of the thread column is for: the thread and the updatedAt its shell row had. */
internal data class ReadKey(val threadId: String, val updatedAt: String)

/** The thread the column shows, and whether the connection is live. */
internal data class Shown(val key: ReadKey, val live: Boolean)

/**
 * The reads the thread column makes, from what it shows: one per thread and updatedAt, made only
 * while T3 is live, one at a time, and a change during a read gives one more read after it
 * (conflate keeps only the latest). A read waits for the connection, since without a socket it
 * fails at once; and the thread is read again when the connection comes back, because the
 * reconnect's snapshot carries the same updatedAt and a read that failed while it was down would
 * otherwise stay the column's last word (finding lor-wc8z).
 */
internal fun threadReads(shown: Flow<Shown>): Flow<ReadKey> =
    shown.map { if (it.live) it.key else null }.distinctUntilChanged().filterNotNull().conflate()

/** Reads [threadId]'s projection and decodes its last messages; logs one `T3Thread` line per read. */
internal suspend fun readThread(connection: T3Connection, threadId: String, updatedAt: String, last: ThreadRead?): ThreadRead {
    val started = SystemClock.elapsedRealtime()
    return try {
        val reply = connection.call(Commands.PROJECTION, JSONObject().put("threadId", threadId))
        val (messages, size) = withContext(Dispatchers.Default) { Commands.messages(reply) to reply.toString().length }
        Log.i(TAG, "read $threadId (updatedAt $updatedAt): ${messages.size} messages, reply $size chars, ${SystemClock.elapsedRealtime() - started} ms")
        ThreadRead(threadId, messages, null)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.i(TAG, "read $threadId (updatedAt $updatedAt) failed: ${T3Connection.describe(e)}")
        // The last good messages stay, with the reason.
        ThreadRead(threadId, last?.takeIf { it.threadId == threadId }?.messages, "Can't read the thread: ${T3Connection.describe(e)}")
    }
}
