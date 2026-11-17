package com.spadaval.lor.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.net.LorClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant

/** The Timer tab: reads and drives LoR's timer; [TimerTab] draws it. */
@Composable
fun TimerScreen() {
    val context = LocalContext.current
    val prefs = remember { Prefs(context) }
    val scope = rememberCoroutineScope()
    var state by remember { mutableStateOf<TimerReply?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var title by remember { mutableStateOf("") }
    var showAll by remember { mutableStateOf(false) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

    fun act(block: (LorClient) -> JSONObject) {
        scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) { runCatching { timerReply(block(LorClient(prefs.serverUrl))) } }
            result.onSuccess { state = it; error = null }.onFailure { error = "Cannot reach LoR: ${it.message}" }
            busy = false
        }
    }

    fun start(intentId: String?) {
        val what = title // read now: the request runs after the field is cleared
        title = ""
        showAll = false
        act { it.timer("start", prefs.deviceName, intentId, what) }
    }

    LaunchedEffect(Unit) {
        while (true) {
            act { it.state(prefs.deviceName) }
            delay(30_000)
        }
    }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000)
        }
    }

    TimerTab(
        TimerUi(state, error, busy, title, showAll, now),
        TimerActions(
            title = { title = it },
            start = ::start,
            stop = { act { it.timer("stop", prefs.deviceName) } },
            pause = { pause -> act { it.timer(if (pause) "pause" else "resume", prefs.deviceName) } },
            showAll = { showAll = !showAll },
        ),
    )
}

/** `/device/state`'s reply as the tab reads it; JSON's null and blank strings are none. */
internal fun timerReply(s: JSONObject): TimerReply {
    fun JSONObject.text(key: String) = optString(key).takeUnless { it.isBlank() || it == "null" }
    fun intents(a: JSONArray?, typed: Boolean) = (0 until (a?.length() ?: 0)).map { i ->
        val o = a!!.getJSONObject(i)
        TimerIntent(o.getString("id"), o.getString("title"), if (typed) o.getString("type") else "")
    }
    val running = s.optJSONObject("running")?.let { r ->
        RunningTimer(
            intent = r.text("intent"),
            title = r.text("title"),
            startMs = Instant.parse(r.getString("t_start")).toEpochMilli(),
            pausedAtMs = r.text("paused_at")?.let { Instant.parse(it).toEpochMilli() },
        )
    }
    return TimerReply(s.optString("login"), running, intents(s.optJSONArray("recent"), typed = false), intents(s.optJSONArray("intents"), typed = true))
}
