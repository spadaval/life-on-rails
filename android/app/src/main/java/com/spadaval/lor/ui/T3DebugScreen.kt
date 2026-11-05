package com.spadaval.lor.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.spadaval.lor.t3.LaunchDefaults
import com.spadaval.lor.t3.ModelSelection
import com.spadaval.lor.t3.Pairing
import com.spadaval.lor.t3.PendingRequest
import com.spadaval.lor.t3.Question
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
 * one. Connected only while open.
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

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(onClick = onBack) { Text("‹ Settings") }
        Section("Pair with T3 Code") {
            OutlinedTextField(value = server, onValueChange = { server = it }, label = { Text("Server address") }, placeholder = { Text("http://10.0.2.2:16021") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Pairing token or link") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            Text(
                "Scan the QR code from T3's Connections settings, or paste the link or a `t3 pair` token. When pasting, the address above, if set, wins over the link's host.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            val pair: (String, String) -> Unit = { address, link ->
                scope.launch {
                    pairing = "Pairing…"
                    // pair() reconnects only while this screen has the connection started, so a
                    // pairing that finishes after the screen closed starts nothing.
                    val result = withContext(Dispatchers.IO) { runCatching { connection.pair(Pairing.parse(address, link)) } }
                    pairing = result.fold({ code = ""; null }, { "Cannot pair: ${T3Connection.describe(it)}" })
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
                    GmsBarcodeScanning.getClient(context, options).startScan()
                        .addOnSuccessListener { barcode ->
                            val link = barcode.rawValue.orEmpty()
                            // A scanned link names its own server, so the typed address doesn't override it.
                            runCatching { Pairing.parse("", link) }.fold(
                                { server = it.httpBase; pair("", link) },
                                { pairing = "That QR code isn't a T3 pairing link." },
                            )
                        }
                        .addOnFailureListener { pairing = "Cannot scan: ${it.message}" }
                }) { Text("Scan QR") }
                OutlinedButton(onClick = { pair(server, code) }) { Text("Pair") }
                OutlinedButton(onClick = { connection.forget() }) { Text("Forget") }
            }
            pairing?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        }
        Section("Connection") {
            Text(
                when (val s = status) {
                    T3Connection.Status.NotPaired -> "Not paired."
                    T3Connection.Status.Stopped -> "Not connected."
                    T3Connection.Status.Connecting -> "Connecting to ${store.lastServer}…"
                    T3Connection.Status.Live -> "Live: ${store.lastServer}"
                    is T3Connection.Status.Retrying -> "Disconnected (${s.error}). Retrying in ${s.retryInMs / 1000}s."
                    is T3Connection.Status.Rejected -> "The server refused this phone (${s.error}). Pair again."
                },
            )
        }
        outcome?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp)) }
        Section("Threads (${shell.threads.size})") {
            if (shell.threads.isEmpty()) Text("None yet.", style = MaterialTheme.typography.bodySmall)
            for (thread in shell.threads) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    // Only the heading toggles the actions, so their buttons stay separate targets.
                    Column(Modifier.fillMaxWidth().clickable { selected = if (selected == thread.id) null else thread.id }) {
                        Text(thread.title, fontWeight = FontWeight.Medium)
                        Text("${thread.projectTitle} · ${thread.state.label}", style = MaterialTheme.typography.bodySmall)
                        thread.lastError?.takeIf { thread.state == ThreadState.FAILED }?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    }
                    if (selected == thread.id) ThreadActions(connection, thread, act)
                }
            }
        }
        Section("Launch a thread") {
            LaunchForm(connection, shell.projects, status == T3Connection.Status.Live, act)
        }
    }
}

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

@Composable
private fun ThreadActions(connection: T3Connection, thread: T3Thread, act: (String, suspend () -> String) -> Unit) {
    val request = thread.requests.firstOrNull()
    Column(Modifier.padding(top = 6.dp)) {
        when {
            request == null -> Unit
            request.isQuestion -> QuestionForm(connection, request, act)
            else -> {
                Text("Asks for approval (${request.kind}).", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { act("Approve") { "Approved (sequence ${connection.respond(request, approve = true)})." } }) { Text("Approve") }
                    OutlinedButton(onClick = { act("Deny") { "Denied (sequence ${connection.respond(request, approve = false)})." } }) { Text("Deny") }
                }
            }
        }
        var message by remember(thread.id) { mutableStateOf("") }
        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Follow-up message") }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Button(enabled = message.isNotBlank(), onClick = {
            val text = message
            act("Send") { connection.send(thread.id, text).let { message = ""; "Sent (sequence $it)." } }
        }) { Text("Send") }
    }
}

/** A pending question's choices, read from its thread; one answer per question. */
@Composable
private fun QuestionForm(connection: T3Connection, request: PendingRequest, act: (String, suspend () -> String) -> Unit) {
    var questions by remember(request.requestId) { mutableStateOf<Result<List<Question>>?>(null) }
    val answers = remember(request.requestId) { mutableStateMapOf<String, String>() }
    LaunchedEffect(request.requestId) {
        questions = try { Result.success(connection.questions(request)) } catch (e: CancellationException) { throw e } catch (e: Exception) { Result.failure(e) }
    }
    val loaded = questions
    when {
        loaded == null -> Text("Reading the question…", style = MaterialTheme.typography.bodySmall)
        loaded.isFailure -> Text("Cannot read the question: ${T3Connection.describe(loaded.exceptionOrNull()!!)}", style = MaterialTheme.typography.bodySmall)
        else -> {
            val list = loaded.getOrThrow()
            if (list.isEmpty()) Text("The question is not in the thread.", style = MaterialTheme.typography.bodySmall)
            for (q in list) {
                Text(q.question, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                for (option in q.options) {
                    val chosen = answers[q.id] == option.answer
                    if (chosen) Button(onClick = { answers.remove(q.id) }) { Text(option.label) }
                    else OutlinedButton(onClick = { answers[q.id] = option.answer }) { Text(option.label) }
                }
                if (q.allowCustomAnswer) {
                    val typed = answers[q.id]?.takeIf { a -> q.options.none { it.answer == a } } ?: ""
                    OutlinedTextField(value = typed, onValueChange = { if (it.isBlank()) answers.remove(q.id) else answers[q.id] = it }, label = { Text("Or type an answer") }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (list.isNotEmpty()) {
                Button(enabled = list.all { answers.containsKey(it.id) }, onClick = {
                    // A multi-select question takes a list; this form picks one option for it.
                    val payload: Map<String, Any> = list.associate { q -> q.id to (answers.getValue(q.id).let { if (q.multiSelect) listOf(it) else it }) }
                    act("Answer") { "Answered (sequence ${connection.answer(request, payload)})." }
                }) { Text("Answer") }
            }
        }
    }
}

@Composable
private fun LaunchForm(connection: T3Connection, projects: Map<String, String>, live: Boolean, act: (String, suspend () -> String) -> Unit) {
    var projectId by remember { mutableStateOf<String?>(null) }
    var prompt by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var askFirst by remember { mutableStateOf(false) }
    var defaults by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(projectId, live) {
        val id = projectId ?: return@LaunchedEffect
        if (!live) return@LaunchedEffect
        defaults = try { describe(connection.launchDefaults(id)) } catch (e: CancellationException) { throw e } catch (e: Exception) { "Cannot read defaults: ${T3Connection.describe(e)}" }
    }
    if (projects.isEmpty()) Text("No projects yet.", style = MaterialTheme.typography.bodySmall)
    for ((id, title) in projects.entries.sortedBy { it.value.lowercase() }) {
        if (projectId == id) Button(onClick = { projectId = null; defaults = null }) { Text(title) }
        else OutlinedButton(onClick = { projectId = id; defaults = null }) { Text(title) }
    }
    defaults?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Prompt") }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model (instance/model), blank for the default") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = askFirst, onCheckedChange = { askFirst = it })
        Text("Ask before acting (approval-required)", style = MaterialTheme.typography.bodySmall)
    }
    val chosen = model.trim().split("/", limit = 2).takeIf { it.size == 2 && it.all(String::isNotBlank) }?.let { ModelSelection(it[0], it[1]) }
    Button(enabled = projectId != null && prompt.isNotBlank() && (model.isBlank() || chosen != null), onClick = {
        val id = projectId!!
        val text = prompt
        act("Launch") {
            val threadId = connection.launch(id, text, chosen, if (askFirst) "approval-required" else null)
            prompt = ""
            "Launched ${threadId.substringAfterLast(':').take(8)} in ${projects[id] ?: id}."
        }
    }) { Text("Launch") }
}

private fun describe(d: LaunchDefaults): String =
    "Defaults: ${d.model?.let { "${it.instanceId}/${it.model}" } ?: "no model set on the server"}, ${d.runtimeMode}."
