package com.spadaval.lor.quicksend

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.spadaval.lor.agent.T3Link
import com.spadaval.lor.ui.Bad
import com.spadaval.lor.ui.Edge
import com.spadaval.lor.ui.Field
import com.spadaval.lor.ui.Mute
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.PanelFace
import com.spadaval.lor.ui.Pill
import com.spadaval.lor.ui.Scrim
import com.spadaval.lor.ui.SmallText
import com.spadaval.lor.ui.Txt
import com.spadaval.lor.ui.framedPanel

/*
 * The quick-send sheet (T3 Launcher, Increment 4): where the prompt will go and why ([Destination]),
 * one tap on another project to pick it, the assistant's screenshot when there is one, the field,
 * and Send. The app's `QuickSendActivity` holds the T3 connection and sends; this draws the sheet
 * from [SheetUi] (lor-rgx4). What is typed, picked and attached is the sheet's own, kept across a
 * fold and started over by each new opening ([SheetUi.opened]).
 */

/** The assistant's screenshot: [image] once taken (null while [done] is false, or when the app refused one), the app's name, and what the browser showed. */
data class Shot(val image: ImageBitmap? = null, val done: Boolean = false, val appLabel: String? = null, val uris: List<String> = emptyList())

data class SheetUi(
    val link: T3Link = T3Link.NOT_PAIRED,
    /** The shell's first snapshot has arrived, with the projects. */
    val loaded: Boolean = false,
    /** Project id to title. */
    val projects: Map<String, String> = emptyMap(),
    /** Thread id to its project id, for what is on screen. */
    val threads: Map<String, String> = emptyMap(),
    /** A send is under way: the sheet can't be closed, and won't send again. */
    val sending: Boolean = false,
    /** The assistant's screenshot; null when the sheet wasn't opened by the assistant. */
    val shot: Shot? = null,
    /** What the field starts with. */
    val initial: String = "",
    /** Bumped by each opening, so the sheet starts over. */
    val opened: Int = 0,
)

/**
 * The sheet's actions. [send] starts a thread in a project with the text, and the screenshot when
 * [attach]; its last argument gets null on success, else why it failed.
 */
class SheetActions(
    val close: () -> Unit = {},
    val send: (projectId: String, projectTitle: String, text: String, attach: Boolean, done: (String?) -> Unit) -> Unit = { _, _, _, _, _ -> },
)

@Composable
fun QuickSendSheet(ui: SheetUi, on: SheetActions) {
    // Saved, so a fold, unfold or rotation keeps a half-typed prompt; a new opening starts over.
    var text by rememberSaveable(ui.opened) { mutableStateOf(ui.initial) }
    var picked by rememberSaveable(ui.opened) { mutableStateOf<String?>(null) }
    var attach by rememberSaveable(ui.opened) { mutableStateOf(true) }
    var failure by rememberSaveable(ui.opened) { mutableStateOf<String?>(null) }
    val live = ui.link == T3Link.LIVE
    val capture = ui.shot
    val choice = Destination.choose(text, ui.projects, ui.threads, capture?.let { Destination.Screen(it.uris) }, picked)
    val target = choice.projectId
    val dismiss = { if (!ui.sending) on.close() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = dismiss),
    ) {
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .imePadding()
                .navigationBarsPadding()
                .padding(8.dp)
                .widthIn(max = 560.dp)
                .fillMaxWidth()
                .framedPanel(PanelFace)
                // Taps on the sheet itself don't reach the scrim.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Unpaired, there are no projects to read; the connection line says what to do.
            val paired = ui.link != T3Link.NOT_PAIRED && ui.link != T3Link.REJECTED
            // Retrying, the connection line already says T3 is down; "Reading…" would contradict it (6.8).
            if (paired && !(ui.link == T3Link.RETRYING && !ui.loaded)) Text(
                when {
                    !ui.loaded -> "Reading T3's projects…"
                    target == null -> "No project named \"${Destination.INBOX}\" on this T3 server. Pick one."
                    else -> "To ${ui.projects[target]} · ${choice.why.label} · a new thread"
                },
                style = SmallText.copy(color = if (target == null && ui.loaded) Or else Mute),
            )
            Projects(ui.projects, target) { picked = it }
            if (capture != null) ShotRow(capture, attach) { attach = !attach }
            PromptField(text, ui.opened) { text = it; failure = null }
            failure?.let { Text("Not sent: $it", style = SmallText.copy(color = Bad)) }
            connectionLine(ui.link)?.let { Text(it, style = SmallText) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                Pill("Close", enabled = !ui.sending, dense = true) { on.close() }
                Pill(
                    if (ui.sending) "Sending…" else "Send",
                    primary = true,
                    enabled = live && !ui.sending && target != null && choice.text.isNotBlank(),
                    dense = true,
                ) {
                    on.send(target!!, ui.projects[target] ?: target, choice.text, attach) { error ->
                        if (error == null) on.close() else failure = error
                    }
                }
            }
        }
    }
}

private fun connectionLine(link: T3Link): String? = when (link) {
    T3Link.LIVE -> null
    T3Link.NOT_PAIRED -> "T3 isn't paired. Pair it on the agent page, left of home."
    T3Link.REJECTED -> "T3 refused this phone. Pair again on the agent page, left of home."
    T3Link.RETRYING -> "T3 is disconnected; trying again. Send is off until it's back."
    T3Link.CONNECTING -> "Connecting to T3…"
}

/** Every project as a chip, the Inbox first; the chosen one is filled. One tap picks another. */
@Composable
private fun Projects(projects: Map<String, String>, chosen: String?, pick: (String) -> Unit) {
    if (projects.isEmpty()) return
    val ordered = projects.entries.sortedWith(
        compareBy<Map.Entry<String, String>> { !it.value.equals(Destination.INBOX, ignoreCase = true) }.thenBy { it.value.lowercase() },
    )
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ordered.forEach { (id, title) -> Pill(title, primary = id == chosen, dense = true) { pick(id) } }
    }
}

/** The screenshot the assistant took, small, with a tap to leave it out or put it back. */
@Composable
private fun ShotRow(capture: Shot, attach: Boolean, toggle: () -> Unit) {
    val bitmap = capture.image
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            bitmap != null && attach -> Image(
                bitmap,
                contentDescription = "Screenshot",
                contentScale = ContentScale.Crop,
                modifier = Modifier.height(72.dp).widthIn(max = 40.dp).clip(RectangleShape).border(1.dp, Edge, RectangleShape),
            )
            bitmap != null -> {}
            !capture.done -> Text("Taking the screenshot…", style = SmallText)
            else -> Text("No screenshot: this app doesn't allow one, or it's off in the assistant's settings.", style = SmallText)
        }
        if (bitmap != null) {
            Column(Modifier.padding(end = 4.dp)) {
                Text(if (attach) "The screen goes with it" else "The screen stays here", style = SmallText.copy(color = Txt))
                capture.appLabel?.let { Text(it, style = SmallText) }
            }
            Pill(if (attach) "Remove" else "Attach", dense = true) { toggle() }
        }
    }
}

/** The prompt, focused on each opening. */
@Composable
private fun PromptField(value: String, opened: Int, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(opened) { runCatching { focus.requestFocus() } }
    Field(value, "Ask an agent… (@project to name one)", Modifier.focusRequester(focus), maxLines = 6, onChange = onChange)
}
