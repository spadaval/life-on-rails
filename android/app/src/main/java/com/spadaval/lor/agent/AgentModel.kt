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
import com.spadaval.lor.net.LorClient
import com.spadaval.lor.t3.PendingRequest
import com.spadaval.lor.t3.T3Connection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The agent page's state: LoR's `/device/home`, the shared [T3Connection], and what the cards are
 * doing. [whileVisible] runs while home is on screen: it holds the connection and reads LoR every
 * minute. When home leaves the screen the hold is let go [RELEASE_AFTER_MS] later, so a quick trip
 * to another app doesn't drop and rebuild the socket. [scope] is the page's; [dispose] lets go at once.
 *
 * Each card action runs at most once at a time: its key is in [busy] until the call ends, and the
 * card's buttons are disabled meanwhile, since a command sent twice could act twice. The calls run
 * in the process's scope ([CardCalls]).
 */
class AgentModel(private val context: Context, private val scope: CoroutineScope) {
    val connection = T3Connection.get(context)
    private val prefs = Prefs(context)

    /** LoR's last good reply; kept for the process, so a recreated page starts with it (greyed until a read succeeds). */
    var lor by mutableStateOf(last.value)
        private set
    /** Why the last read of LoR failed, in plain words ([LorClient.describe]); the last good [lor] stays shown. */
    var lorError by mutableStateOf<String?>(null)
        private set
    /** A read asked for by Retry is under way. */
    var retrying by mutableStateOf(false)
        private set
    /** At least one read has ended since the page opened; until then a kept [lor] is shown greyed. */
    var lorRead by mutableStateOf(false)
        private set
    /** The cards' calls, run in the process's scope (F4). */
    private val calls = CardCalls((context.applicationContext as LorApp).scope) { T3Connection.describe(it) }
    /** Requests answered from this page, hidden before the shell catches up. */
    val answered: Set<String> get() = calls.answered
    /** Card keys with a call under way. */
    val busy: Set<String> get() = calls.busy
    /** Why a card's last action failed, by card key. */
    val failures: Map<String, String> get() = calls.failures
    /** One line about the last thing the page couldn't do outside a card, such as opening T3; the next action clears it. */
    var notice by mutableStateOf<String?>(null)

    private var holding = false
    private var release: Job? = null

    suspend fun whileVisible() {
        release?.cancel()
        if (!holding) {
            connection.start()
            holding = true
        }
        try {
            while (true) {
                readLor()
                delay(LOR_EVERY_MS)
            }
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

    private suspend fun readLor() {
        val url = prefs.serverUrl
        val device = prefs.deviceName
        try {
            lor = withContext(Dispatchers.IO) { LorClient(url).home(device) }.also { _last.value = it }
            lorError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            lorError = LorClient.describe(e)
        } finally {
            lorRead = true
        }
    }

    /** Reads LoR now instead of at the next minute. */
    fun retry() {
        if (retrying) return
        retrying = true
        scope.launch {
            try {
                readLor()
            } finally {
                retrying = false
            }
        }
    }

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
        /** The last good `/device/home` in this process (not on disk). */
        private val _last = kotlinx.coroutines.flow.MutableStateFlow<DeviceHome?>(null)
        /** The last good `/device/home` in this process (not on disk); home's header reads its events (launcher polish 6.7). */
        val last: kotlinx.coroutines.flow.StateFlow<DeviceHome?> = _last
        private const val LOR_EVERY_MS = 60_000L
        private const val RELEASE_AFTER_MS = 30_000L
    }
}

/**
 * The cards' calls (approve, answer) and what the cards show of them. Each runs at most once at a
 * time, and in [work], the process's scope ([LorApp.scope]) rather than the page's, so leaving the
 * agent page (a move two cells away, a recreated home) never cancels a call under way (F4 from T3
 * Launcher 3.3, as quick send's send already runs). [describe] puts a failure in plain words.
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
