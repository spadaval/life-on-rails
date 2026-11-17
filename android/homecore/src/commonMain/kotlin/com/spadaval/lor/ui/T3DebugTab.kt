package com.spadaval.lor.ui

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** A thread as the debug view lists it: its [state] in words, and [error] only when it failed. */
data class T3ThreadUi(
    val id: String,
    val title: String,
    val project: String,
    val state: String,
    val error: String? = null,
    /** The first request it is blocked on, if any. */
    val request: T3RequestUi? = null,
)

/** A request a thread is blocked on: a question (`user_input`) or an approval of [kind]. */
data class T3RequestUi(val id: String, val kind: String) {
    val isQuestion: Boolean get() = kind == "user_input"
}

/** One question of a `user_input` request; each option's [T3OptionUi.answer] is what is sent. */
data class T3QuestionUi(
    val id: String,
    val question: String,
    val options: List<T3OptionUi>,
    val multiSelect: Boolean = false,
    val allowCustomAnswer: Boolean = true,
)

data class T3OptionUi(val label: String, val answer: String = label)

/**
 * The T3 debug view: the typed [server] and [code], what pairing said ([pairing]), the
 * connection's [status] in words and whether it is [live], what the last action did ([outcome]),
 * the threads and the open one ([selected]), and the projects a thread launches in, by id.
 */
data class T3DebugUi(
    val server: String = "",
    val code: String = "",
    val pairing: String? = null,
    val status: String = "Not paired.",
    val live: Boolean = false,
    val outcome: String? = null,
    val threads: List<T3ThreadUi> = emptyList(),
    val selected: String? = null,
    val projects: Map<String, String> = emptyMap(),
)

/**
 * What the debug view's taps ask for. [send] and [launch] call their last argument once the
 * server took it, to clear the field. [questions] and [defaults] read from T3: a failure's
 * message says why, and [defaults] returns its line whether it read them or not.
 */
class T3DebugActions(
    val back: () -> Unit = {},
    val server: (String) -> Unit = {},
    val code: (String) -> Unit = {},
    val scan: () -> Unit = {},
    val pair: () -> Unit = {},
    val forget: () -> Unit = {},
    val select: (String?) -> Unit = {},
    val respond: (T3RequestUi, Boolean) -> Unit = { _, _ -> },
    val send: (threadId: String, text: String, sent: () -> Unit) -> Unit = { _, _, _ -> },
    val questions: suspend (T3RequestUi) -> Result<List<T3QuestionUi>> = { Result.success(emptyList()) },
    /** By question id: an answer, or for a multi-select question a list of one. */
    val answer: (T3RequestUi, Map<String, Any>) -> Unit = { _, _ -> },
    val defaults: suspend (projectId: String) -> String = { "" },
    /** [model] is `instance` to `model`, null for the server's default; [askFirst] runs it approval-required. */
    val launch: (projectId: String, prompt: String, model: Pair<String, String>?, askFirst: Boolean, launched: () -> Unit) -> Unit = { _, _, _, _, _ -> },
)

/** Debug view of the T3 connection: pair, watch the thread list, act on a thread (tap it), and launch one. */
@Composable
fun T3DebugTab(ui: T3DebugUi, on: T3DebugActions) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = on.back) { Text("‹ Settings") }
        Section("Pair with T3 Code") {
            OutlinedTextField(value = ui.server, onValueChange = on.server, label = { Text("Server address") }, placeholder = { Text("http://10.0.2.2:16021") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = ui.code, onValueChange = on.code, label = { Text("Pairing token or link") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
            Text(
                "Scan the QR code from T3's Connections settings, or paste the link or a `t3 pair` token. When pasting, the address above, if set, wins over the link's host.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = 6.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(shape = androidx.compose.ui.graphics.RectangleShape, onClick = on.scan) { Text("Scan QR") }
                OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = on.pair) { Text("Pair") }
                OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = on.forget) { Text("Forget") }
            }
            ui.pairing?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
        }
        Section("Connection") {
            Text(ui.status)
        }
        ui.outcome?.let { Text(it, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 4.dp)) }
        Section("Threads (${ui.threads.size})") {
            if (ui.threads.isEmpty()) Text("None yet.", style = MaterialTheme.typography.bodySmall)
            for (thread in ui.threads) {
                Column(Modifier.padding(vertical = 6.dp)) {
                    // Only the heading toggles the actions, so their buttons stay separate targets.
                    Column(Modifier.fillMaxWidth().clickable { on.select(if (ui.selected == thread.id) null else thread.id) }) {
                        Text(thread.title, fontWeight = FontWeight.Medium)
                        Text("${thread.project} · ${thread.state}", style = MaterialTheme.typography.bodySmall)
                        thread.error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
                    }
                    if (ui.selected == thread.id) ThreadActions(thread, on)
                }
            }
        }
        Section("Launch a thread") {
            LaunchForm(ui.projects, ui.live, on)
        }
    }
}

@Composable
private fun ThreadActions(thread: T3ThreadUi, on: T3DebugActions) {
    val request = thread.request
    Column(Modifier.padding(top = 6.dp)) {
        when {
            request == null -> Unit
            request.isQuestion -> QuestionForm(request, on)
            else -> {
                Text("Asks for approval (${request.kind}).", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { on.respond(request, true) }) { Text("Approve") }
                    OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { on.respond(request, false) }) { Text("Deny") }
                }
            }
        }
        var message by remember(thread.id) { mutableStateOf("") }
        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Follow-up message") }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
        Button(shape = androidx.compose.ui.graphics.RectangleShape, enabled = message.isNotBlank(), onClick = {
            on.send(thread.id, message) { message = "" }
        }) { Text("Send") }
    }
}

/** A pending question's choices, read from its thread; one answer per question. */
@Composable
private fun QuestionForm(request: T3RequestUi, on: T3DebugActions) {
    var questions by remember(request.id) { mutableStateOf<Result<List<T3QuestionUi>>?>(null) }
    val answers = remember(request.id) { mutableStateMapOf<String, String>() }
    LaunchedEffect(request.id) {
        questions = on.questions(request)
    }
    val loaded = questions
    when {
        loaded == null -> Text("Reading the question…", style = MaterialTheme.typography.bodySmall)
        loaded.isFailure -> Text("Cannot read the question: ${loaded.exceptionOrNull()!!.message}", style = MaterialTheme.typography.bodySmall)
        else -> {
            val list = loaded.getOrThrow()
            if (list.isEmpty()) Text("The question is not in the thread.", style = MaterialTheme.typography.bodySmall)
            for (q in list) {
                Text(q.question, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 4.dp))
                for (option in q.options) {
                    val chosen = answers[q.id] == option.answer
                    if (chosen) Button(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { answers.remove(q.id) }) { Text(option.label) }
                    else OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { answers[q.id] = option.answer }) { Text(option.label) }
                }
                if (q.allowCustomAnswer) {
                    val typed = answers[q.id]?.takeIf { a -> q.options.none { it.answer == a } } ?: ""
                    OutlinedTextField(value = typed, onValueChange = { if (it.isBlank()) answers.remove(q.id) else answers[q.id] = it }, label = { Text("Or type an answer") }, modifier = Modifier.fillMaxWidth())
                }
            }
            if (list.isNotEmpty()) {
                Button(shape = androidx.compose.ui.graphics.RectangleShape, enabled = list.all { answers.containsKey(it.id) }, onClick = {
                    // A multi-select question takes a list; this form picks one option for it.
                    val payload: Map<String, Any> = list.associate { q -> q.id to (answers.getValue(q.id).let { if (q.multiSelect) listOf(it) else it }) }
                    on.answer(request, payload)
                }) { Text("Answer") }
            }
        }
    }
}

@Composable
private fun LaunchForm(projects: Map<String, String>, live: Boolean, on: T3DebugActions) {
    var projectId by remember { mutableStateOf<String?>(null) }
    var prompt by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var askFirst by remember { mutableStateOf(false) }
    var defaults by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(projectId, live) {
        val id = projectId ?: return@LaunchedEffect
        if (!live) return@LaunchedEffect
        defaults = on.defaults(id)
    }
    if (projects.isEmpty()) Text("No projects yet.", style = MaterialTheme.typography.bodySmall)
    for ((id, title) in projects.entries.sortedBy { it.value.lowercase() }) {
        if (projectId == id) Button(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { projectId = null; defaults = null }) { Text(title) }
        else OutlinedButton(shape = androidx.compose.ui.graphics.RectangleShape, onClick = { projectId = id; defaults = null }) { Text(title) }
    }
    defaults?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    OutlinedTextField(value = prompt, onValueChange = { prompt = it }, label = { Text("Prompt") }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model (instance/model), blank for the default") }, singleLine = true, modifier = Modifier.fillMaxWidth().padding(top = 6.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = askFirst, onCheckedChange = { askFirst = it })
        Text("Ask before acting (approval-required)", style = MaterialTheme.typography.bodySmall)
    }
    val chosen = model.trim().split("/", limit = 2).takeIf { it.size == 2 && it.all(String::isNotBlank) }?.let { it[0] to it[1] }
    Button(shape = androidx.compose.ui.graphics.RectangleShape, enabled = projectId != null && prompt.isNotBlank() && (model.isBlank() || chosen != null), onClick = {
        on.launch(projectId!!, prompt, chosen, askFirst) { prompt = "" }
    }) { Text("Launch") }
}
