package com.spadaval.lor.quicksend

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.t3.T3Connection
import com.spadaval.lor.ui.LorTheme

/**
 * The quick-send sheet (T3 Launcher, Increment 4), in its own translucent task so it opens over
 * whatever is on screen: home, another app (the tile), or the app the assistant was called over.
 * It shows where the prompt will go and why ([Destination]); one tap on another project picks it.
 * Each send starts a new thread ([QuickSend.send]). It holds the T3 connection while it's started,
 * and closes when it leaves the screen; a send under way still finishes and says where it went.
 */
class QuickSendActivity : ComponentActivity() {
    private lateinit var connection: T3Connection
    private var from by mutableStateOf(QuickSend.From.AGENT)
    /** Bumped by a new intent, so the sheet starts over. */
    private var opened by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(AndroidColor.TRANSPARENT), SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        connection = T3Connection.get(applicationContext)
        from = QuickSend.from(intent)
        setContent { LorTheme { Sheet(connection, from, opened) { finish() } } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        from = QuickSend.from(intent)
        opened++
    }

    override fun onStart() {
        super.onStart()
        connection.start()
    }

    override fun onStop() {
        connection.stop()
        // A sheet left behind would come back stale; a send under way finishes on its own.
        if (!isChangingConfigurations) finish()
        super.onStop()
    }

    override fun onDestroy() {
        if (from == QuickSend.From.ASSIST && !isChangingConfigurations) Capture.clear()
        super.onDestroy()
    }
}

private val Ink = Color(0xFFECEBE6)
private val Muted = Color(0xFF9097A1)
private val Dim = Color(0xFF616871)
private val SheetFill = Color(0xFF111418)
private val Edge = Color(0xFF2C3139)
private val Scrim = Color(0x99000000)
private val Amber = Color(0xFFE3A857)
private val Red = Color(0xFFE2806F)
private val Small = TextStyle(color = Muted, fontSize = 12.sp)

@Composable
private fun Sheet(connection: T3Connection, from: QuickSend.From, opened: Int, close: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val status by connection.status.collectAsState()
    val shell by connection.shell.collectAsState()
    val sending by QuickSend.sending.collectAsState()
    val captured by Capture.current.collectAsState()
    val capture = if (from == QuickSend.From.ASSIST) captured else null
    var text by remember(opened) { mutableStateOf("") }
    var picked by remember(opened) { mutableStateOf<String?>(null) }
    var attach by remember(opened) { mutableStateOf(true) }
    var failure by remember(opened) { mutableStateOf<String?>(null) }
    val live = status == T3Connection.Status.Live

    val threads = remember(shell) { shell.threads.associate { it.id to it.projectId } }
    val choice = Destination.choose(text, shell.projects, threads, capture?.screen, picked)
    val target = choice.projectId
    val jpeg = capture?.jpeg?.takeIf { attach }
    val dismiss = { if (!sending) close() }
    BackHandler { dismiss() }

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
                .background(SheetFill, RoundedCornerShape(22.dp))
                .border(1.dp, Edge, RoundedCornerShape(22.dp))
                // Taps on the sheet itself don't reach the scrim.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                when {
                    !shell.loaded -> "Reading T3's projects…"
                    target == null -> "No project named \"${Destination.INBOX}\" on this T3 server. Pick one."
                    else -> "To ${shell.projects[target]} · ${choice.why.label} · a new thread"
                },
                style = Small.copy(color = if (target == null && shell.loaded) Amber else Muted),
            )
            Projects(shell.projects, target) { picked = it }
            if (capture != null) Shot(capture, attach) { attach = !attach }
            Field(text, opened) { text = it; failure = null }
            failure?.let { Text("Not sent: $it", style = Small.copy(color = Red)) }
            connectionLine(status)?.let { Text(it, style = Small) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                Pill("Close", enabled = !sending) { close() }
                Pill(
                    if (sending) "Sending…" else "Send",
                    primary = true,
                    enabled = live && !sending && target != null && choice.text.isNotBlank(),
                ) {
                    val image = jpeg?.let { T3Connection.Image(imageName(capture?.appLabel), "image/jpeg", it) }
                    QuickSend.send(context, target!!, shell.projects[target] ?: target, choice.text, image) { error ->
                        if (error == null) close() else failure = error
                    }
                }
            }
        }
    }
}

private fun imageName(appLabel: String?): String =
    ("Screen" + (appLabel?.replace(Regex("[^A-Za-z0-9 _-]"), "")?.trim()?.takeIf { it.isNotEmpty() }?.let { " - $it" } ?: "") + ".jpg").take(120)

private fun connectionLine(status: T3Connection.Status): String? = when (status) {
    T3Connection.Status.Live -> null
    T3Connection.Status.NotPaired -> "T3 isn't paired. Pair it in LoR's Settings, under T3 Code."
    is T3Connection.Status.Rejected -> "T3 refused this phone. Pair again in LoR's Settings."
    is T3Connection.Status.Retrying -> "T3 is disconnected; trying again. Send is off until it's back."
    T3Connection.Status.Connecting, T3Connection.Status.Stopped -> "Connecting to T3…"
}

/** Every project as a chip, the Inbox first; the chosen one is filled. One tap picks another. */
@Composable
private fun Projects(projects: Map<String, String>, chosen: String?, pick: (String) -> Unit) {
    if (projects.isEmpty()) return
    val ordered = projects.entries.sortedWith(
        compareBy<Map.Entry<String, String>> { !it.value.equals(Destination.INBOX, ignoreCase = true) }.thenBy { it.value.lowercase() },
    )
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        ordered.forEach { (id, title) -> Pill(title, primary = id == chosen) { pick(id) } }
    }
}

/** The screenshot the assistant took, small, with a tap to leave it out or put it back. */
@Composable
private fun Shot(capture: Capture, attach: Boolean, toggle: () -> Unit) {
    val jpeg = capture.jpeg
    val bitmap = remember(jpeg) { jpeg?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        when {
            bitmap != null && attach -> Image(
                bitmap,
                contentDescription = "Screenshot",
                contentScale = ContentScale.Crop,
                modifier = Modifier.height(72.dp).widthIn(max = 40.dp).clip(RoundedCornerShape(6.dp)).border(1.dp, Edge, RoundedCornerShape(6.dp)),
            )
            bitmap != null -> {}
            !capture.shotDone -> Text("Taking the screenshot…", style = Small)
            else -> Text("No screenshot: this app doesn't allow one, or it's off in the assistant's settings.", style = Small)
        }
        if (bitmap != null) {
            Column(Modifier.padding(end = 4.dp)) {
                Text(if (attach) "The screen goes with it" else "The screen stays here", style = Small.copy(color = Ink))
                capture.appLabel?.let { Text(it, style = Small) }
            }
            Pill(if (attach) "Remove" else "Attach") { toggle() }
        }
    }
}

@Composable
private fun Field(value: String, opened: Int, onChange: (String) -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(opened) { runCatching { focus.requestFocus() } }
    OutlinedTextField(
        value,
        onChange,
        Modifier.fillMaxWidth().focusRequester(focus),
        placeholder = { Text("Ask an agent… (@project to name one)", color = Dim) },
        textStyle = TextStyle(color = Ink, fontSize = 14.sp),
        shape = RoundedCornerShape(12.dp),
        maxLines = 6,
        colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = Ink, unfocusedTextColor = Ink, cursorColor = Amber,
            focusedBorderColor = Muted, unfocusedBorderColor = Color(0x2AFFFFFF),
        ),
    )
}

@Composable
private fun Pill(text: String, primary: Boolean = false, enabled: Boolean = true, onClick: () -> Unit) {
    val shape = RoundedCornerShape(9.dp)
    val base = if (primary) Modifier.background(if (enabled) Ink else Muted, shape) else Modifier.border(1.dp, if (enabled) Color(0x33FFFFFF) else Color(0x14FFFFFF), shape)
    Text(
        text,
        style = TextStyle(
            color = when {
                primary -> Color(0xFF0B0D10)
                enabled -> Ink
                else -> Dim
            },
            fontSize = 12.sp,
            fontWeight = if (primary) FontWeight.SemiBold else FontWeight.Normal,
        ),
        maxLines = 1,
        modifier = base
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 11.dp, vertical = 6.dp),
    )
}
