package com.spadaval.lor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.spadaval.lor.t3.LaunchDefaults
import com.spadaval.lor.t3.ModelSelection
import com.spadaval.lor.t3.PairScan
import com.spadaval.lor.t3.Pairing
import com.spadaval.lor.t3.PendingRequest
import com.spadaval.lor.t3.T3Connection
import com.spadaval.lor.t3.T3Store
import com.spadaval.lor.t3.T3Thread
import com.spadaval.lor.t3.ThreadState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Debug view of the T3 connection: pair, watch the thread list, act on a thread (tap it), and launch
 * one. Connected only while open. [T3DebugTab] draws it.
 */
@Composable
fun T3DebugScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val connection = remember { T3Connection.get(context) }
    val store = remember { T3Store(context) }
    val scope = rememberCoroutineScope()
    val status by connection.status.collectAsState()
    val shell by connection.shell.collectAsState()
    var server by remember { mutableStateOf(store.lastServer) }
    var code by remember { mutableStateOf("") }
    var pairing by remember { mutableStateOf<String?>(null) }
    var selected by remember { mutableStateOf<String?>(null) }
    /** What the last action did, or why it failed. */
    var outcome by remember { mutableStateOf<String?>(null) }
    val act: (String, suspend () -> String) -> Unit = { label, block -> scope.runAction(label, { outcome = it }, block) }

    BackHandler(onBack = onBack)
    DisposableEffect(Unit) {
        connection.start()
        onDispose { connection.stop() }
    }

    val pair: (String, String) -> Unit = { address, link ->
        scope.launch {
            pairing = "Pairing…"
            // pair() reconnects only while this screen has the connection started, so a
            // pairing that finishes after the screen closed starts nothing.
            val result = withContext(Dispatchers.IO) { runCatching { connection.pair(Pairing.parse(address, link)) } }
            pairing = result.fold({ code = ""; null }, { "Cannot pair: ${T3Connection.describe(it)}" })
        }
    }
    // The requests on screen, by id, as the connection last listed them.
    val requests = shell.threads.mapNotNull { it.requests.firstOrNull() }.associateBy { it.requestId }
    fun request(r: T3RequestUi): PendingRequest = requests[r.id] ?: throw IllegalStateException("the request is no longer pending")

    T3DebugTab(
        T3DebugUi(
            server = server,
            code = code,
            pairing = pairing,
            status = when (val s = status) {
                T3Connection.Status.NotPaired -> "Not paired."
                T3Connection.Status.Stopped -> "Not connected."
                T3Connection.Status.Connecting -> "Connecting to ${store.lastServer}…"
                T3Connection.Status.Live -> "Live: ${store.lastServer}"
                is T3Connection.Status.Retrying -> "Disconnected (${s.error}). Retrying in ${s.retryInMs / 1000}s."
                is T3Connection.Status.Rejected -> "The server refused this phone (${s.error}). Pair again."
            },
            live = status == T3Connection.Status.Live,
            outcome = outcome,
            threads = shell.threads.map(::threadUi),
            selected = selected,
            projects = shell.projects,
        ),
        T3DebugActions(
            back = onBack,
            server = { server = it },
            code = { code = it },
            scan = { PairScan.scan(context, { link, target -> server = target.httpBase; pair("", link) }, { pairing = it }) },
            pair = { pair(server, code) },
            forget = { connection.forget() },
            select = { selected = it },
            respond = { r, approve ->
                if (approve) act("Approve") { "Approved (sequence ${connection.respond(request(r), approve = true)})." }
                else act("Deny") { "Denied (sequence ${connection.respond(request(r), approve = false)})." }
            },
            send = { threadId, text, sent -> act("Send") { connection.send(threadId, text).let { sent(); "Sent (sequence $it)." } } },
            questions = { r ->
                try {
                    Result.success(
                        connection.questions(request(r)).map { q ->
                            T3QuestionUi(q.id, q.question, q.options.map { T3OptionUi(it.label, it.answer) }, q.multiSelect, q.allowCustomAnswer)
                        },
                    )
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Result.failure(Exception(T3Connection.describe(e)))
                }
            },
            answer = { r, payload -> act("Answer") { "Answered (sequence ${connection.answer(request(r), payload)})." } },
            defaults = { id ->
                try { describe(connection.launchDefaults(id)) } catch (e: CancellationException) { throw e } catch (e: Exception) { "Cannot read defaults: ${T3Connection.describe(e)}" }
            },
            launch = { id, text, model, askFirst, launched ->
                act("Launch") {
                    val threadId = connection.launch(id, text, model?.let { ModelSelection(it.first, it.second) }, if (askFirst) "approval-required" else null)
                    launched()
                    "Launched ${threadId.substringAfterLast(':').take(8)} in ${shell.projects[id] ?: id}."
                }
            },
        ),
    )
}

private fun threadUi(thread: T3Thread) = T3ThreadUi(
    thread.id,
    thread.title,
    thread.projectTitle,
    thread.state.label,
    thread.lastError?.takeIf { thread.state == ThreadState.FAILED },
    thread.requests.firstOrNull()?.let { T3RequestUi(it.requestId, it.kind) },
)

/** Runs [block] as the action [label], reporting its result or failure through [report]. */
private fun CoroutineScope.runAction(label: String, report: (String) -> Unit, block: suspend () -> String) {
    launch {
        report("$label…")
        report(
            try {
                block()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                "$label failed: ${T3Connection.describe(e)}"
            },
        )
    }
}

private fun describe(d: LaunchDefaults): String =
    "Defaults: ${d.model?.let { "${it.instanceId}/${it.model}" } ?: "no model set on the server"}, ${d.runtimeMode}."
