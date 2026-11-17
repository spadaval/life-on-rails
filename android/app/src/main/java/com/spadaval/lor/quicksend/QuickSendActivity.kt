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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.spadaval.lor.agent.link
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
    /** What the field starts with. */
    private var initial by mutableStateOf("")
    /** Bumped by a new intent, so the sheet starts over. */
    private var opened by mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(SystemBarStyle.dark(AndroidColor.TRANSPARENT), SystemBarStyle.dark(AndroidColor.TRANSPARENT))
        connection = T3Connection.get(applicationContext)
        from = QuickSend.from(intent)
        initial = QuickSend.text(intent)
        opened = savedInstanceState?.getInt(OPENED) ?: 0
        setContent { LorTheme { Sheet(connection, from, initial, opened) { finish() } } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // so a recreated sheet reads the latest opening
        from = QuickSend.from(intent)
        initial = QuickSend.text(intent)
        opened++
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(OPENED, opened)
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

    private companion object {
        const val OPENED = "opened"
    }
}

/** The sheet's state from the connection, the send under way and the assistant's capture; its drawing is homecore's (`SheetView.kt`). */
@Composable
private fun Sheet(connection: T3Connection, from: QuickSend.From, initial: String, opened: Int, close: () -> Unit) {
    val context = LocalContext.current.applicationContext
    val status by connection.status.collectAsState()
    val shell by connection.shell.collectAsState()
    val sending by QuickSend.sending.collectAsState()
    val captured by Capture.current.collectAsState()
    val capture = if (from == QuickSend.From.ASSIST) captured else null
    val jpeg = capture?.jpeg
    val bitmap = remember(jpeg) { jpeg?.let { BitmapFactory.decodeByteArray(it, 0, it.size)?.asImageBitmap() } }
    val threads = remember(shell) { shell.threads.associate { it.id to it.projectId } }
    BackHandler { if (!sending) close() }
    QuickSendSheet(
        SheetUi(
            link = status.link(),
            loaded = shell.loaded,
            projects = shell.projects,
            threads = threads,
            sending = sending,
            shot = capture?.let { Shot(bitmap, it.shotDone, it.appLabel, it.screen.uris) },
            initial = initial,
            opened = opened,
        ),
        SheetActions(
            close = close,
            send = { projectId, title, text, attach, done ->
                val image = capture?.jpeg?.takeIf { attach }?.let { T3Connection.Image(imageName(capture.appLabel), "image/jpeg", it) }
                QuickSend.send(context, projectId, title, text, image, done)
            },
        ),
    )
}

private fun imageName(appLabel: String?): String =
    ("Screen" + (appLabel?.replace(Regex("[^A-Za-z0-9 _-]"), "")?.trim()?.takeIf { it.isNotEmpty() }?.let { " - $it" } ?: "") + ".jpg").take(120)
