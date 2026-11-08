package com.spadaval.lor.agent

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.spadaval.lor.LorApp
import com.spadaval.lor.collect.Launch
import com.spadaval.lor.collect.LaunchFrom
import com.spadaval.lor.collect.Launches
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.net.DeviceHome
import com.spadaval.lor.net.DevicePeople
import com.spadaval.lor.net.DeviceTasks
import com.spadaval.lor.net.LorClient
import com.spadaval.lor.net.LorReads
import com.spadaval.lor.t3.PendingRequest
import com.spadaval.lor.t3.T3Connection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The agent page's state, and that of LoR's blocks and the `lor` panel (lor-lz97), one per screen
 * that shows it: LoR's replies from the process's reader ([LorApp.reads]), the shared
 * [T3Connection], and what the cards are doing. [whileVisible] runs while home is on screen:
 * it holds the connection and the reader, which reads LoR every minute. When home leaves the screen
 * the connection is let go [RELEASE_AFTER_MS] later, so a quick trip to another app doesn't drop
 * and rebuild the socket. [scope] is the page's; [dispose] lets go at once.
 *
 * Each card action runs at most once at a time: its key is in [busy] until the call ends, and the
 * card's buttons are disabled meanwhile, since a command sent twice could act twice. The calls run
 * in the process's scope ([CardCalls]).
 */
class AgentModel(private val context: Context, private val scope: CoroutineScope) {
    val connection = T3Connection.get(context)
    private val prefs = Prefs(context)

    private val reads = (context.applicationContext as LorApp).reads
    /** LoR's last good reply; kept for the process, so a recreated page starts with it (greyed until it is current). */
    val lor: DeviceHome? get() = reads.home
    /** Why the last read of LoR failed, in plain words ([LorClient.describe]); the last good [lor] stays shown. */
    val lorError: String? get() = reads.error
    /** A read asked for by Retry is under way. */
    val retrying: Boolean get() = reads.retrying
    /** [lor] is current ([LorReads.current]); until then a kept [lor] is shown greyed. */
    val lorRead: Boolean get() = reads.current
    /** The cards' calls, one for the process (F4, lor-0fbs): a page built again sees a call still under way. */
    private val calls = (context.applicationContext as LorApp).cardCalls
    /** Requests answered from this page, hidden before the shell catches up. */
    val answered: Set<String> get() = calls.answered
    /** Card keys with a call under way. */
    val busy: Set<String> get() = calls.busy
    /** Why a card's last action failed, by card key. */
    val failures: Map<String, String> get() = calls.failures
    /** One line about the last thing the page couldn't do outside a card, such as opening T3; the next action clears it. */
    var notice by mutableStateOf<String?>(null)

    /** The open tasks and those done today, while a holder asks for the lists ([whileVisible]'s `lists`). */
    val tasks: LorReads.Kept<DeviceTasks> get() = reads.tasks
    /** Who to be in touch with, read with [tasks]. */
    val people: LorReads.Kept<DevicePeople> get() = reads.people

    private var holding = false
    private var release: Job? = null

    /**
     * Holds what this screen shows while it is started: LoR's reader, with `/device/tasks` and
     * `/device/people` too when [lists] (the `lor` panel), and the T3 connection when [t3].
     */
    suspend fun whileVisible(t3: Boolean = true, lists: Boolean = false) {
        release?.cancel()
        if (t3 && !holding) {
            connection.start()
            holding = true
        }
        try {
            reads.hold(lists)
        } finally {
            release = scope.launch {
                delay(RELEASE_AFTER_MS)
                letGo()
            }
        }
    }

    fun dispose() {
        release?.cancel()
        letGo()
    }

    private fun letGo() {
        if (holding) connection.stop()
        holding = false
    }

    /** Reads LoR now instead of at the next minute. */
    fun retry() = reads.retry()

    /** Runs a card's call once; on success the request is hidden, on failure the card says why. */
    fun act(key: String, request: PendingRequest?, call: suspend () -> Unit) {
        if (key in busy) return
        notice = null
        calls.act(key, request?.requestId, call)
    }

    fun approve(card: T3Card, approve: Boolean) {
        val request = card.request ?: return
        act(card.key, request) { connection.respond(request, approve) }
    }

    fun answer(card: T3Card, answers: Map<String, Any>) {
        val request = card.request ?: return
        act(card.key, request) { connection.answer(request, answers) }
    }

    /** Opens the T3 app at [threadId] on the paired server. */
    fun openThread(threadId: String, from: LaunchFrom) {
        notice = null
        scope.launch {
            notice = try {
                T3App.open(context, connection.environmentId(), threadId, from)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "Can't open T3: ${T3Connection.describe(e)}"
            }
        }
    }

    /** Opens LoR's dashboard at [path] in the browser, beside home on a large screen. */
    fun openLor(path: String, from: LaunchFrom) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(prefs.serverUrl + path))
            .addFlags(T3App.flags(context))
        notice = try {
            context.startActivity(intent)
            // The app that takes the link: the browser, or null when Android asks which one.
            val pkg = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName?.takeIf { it != "android" }
            Launches.record(context, Launch(from, pkg, label = pkg?.let { Launches.label(context, it) }, url = intent.dataString))
            null
        } catch (e: ActivityNotFoundException) {
            "No browser can open LoR."
        }
    }

    companion object {
        private const val RELEASE_AFTER_MS = 30_000L
    }
}

/**
 * The cards' calls (approve, answer) and what the cards show of them. Each runs at most once at a
 * time, and in [work], the process's scope ([LorApp.scope]) rather than the page's, so leaving the
 * agent page (a move two cells away, a recreated home) never cancels a call under way (F4 from T3
 * Launcher 3.3, as quick send's send already runs). One is held for the process
 * ([LorApp.cardCalls]), so an agent page built again while a call runs shows it busy and won't
 * send it twice (lor-0fbs). [describe] puts a failure in plain words.
 */
class CardCalls(private val work: CoroutineScope, private val describe: (Exception) -> String) {
    /** Requests answered, hidden before the shell catches up. */
    var answered by mutableStateOf(emptySet<String>())
        private set
    /** Card keys with a call under way. */
    var busy by mutableStateOf(emptySet<String>())
        private set
    /** Why a card's last call failed, by card key. */
    var failures by mutableStateOf(emptyMap<String, String>())
        private set

    /** Runs [call] for the card [key] unless one is under way; on success [requestId], if any, is answered. */
    fun act(key: String, requestId: String?, call: suspend () -> Unit): Job? {
        if (key in busy) return null
        busy = busy + key
        failures = failures - key
        return work.launch {
            try {
                call()
                if (requestId != null) answered = answered + requestId
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failures = failures + (key to describe(e))
            } finally {
                busy = busy - key
            }
        }
    }
}
