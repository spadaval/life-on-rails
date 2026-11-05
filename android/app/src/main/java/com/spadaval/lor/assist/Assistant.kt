package com.spadaval.lor.assist

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.voice.VoiceInteractionService
import android.service.voice.VoiceInteractionSession
import android.service.voice.VoiceInteractionSessionService
import android.speech.RecognitionService
import com.spadaval.lor.quicksend.Capture
import com.spadaval.lor.quicksend.Destination
import com.spadaval.lor.quicksend.QuickSend
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

/*
 * LoR's app as the default digital assistant (T3 Launcher, Increment 4.3). Only a voice
 * interaction session is handed the screen: its assist data (the foreground app's view structure
 * and AssistContent) and a screenshot. Only AssistContent's URIs are read, for the project on screen
 * ([Destination.onScreen]); the view text is not. An activity answering ACTION_ASSIST gets neither. Settings lists
 * a VoiceInteractionService as an assistant only when its metadata also names a RecognitionService,
 * so [NoRecognition] is an empty one. There is no voice: the session opens the quick-send sheet.
 */

/** The service Settings picks as the digital assistant; everything happens in [AssistSession]. */
class AssistService : VoiceInteractionService()

class AssistSessionService : VoiceInteractionSessionService() {
    override fun onNewSession(args: Bundle?): VoiceInteractionSession = AssistSession(this)
}

/**
 * Collects the assist data and the screenshot into [Capture] and opens the sheet over the app. The
 * sheet starts after the screenshot is in (or [OPEN_AFTER_MS] at most), so it is never in the
 * picture. The session hides once both callbacks have run, or after [HIDE_AFTER_MS].
 */
class AssistSession(service: VoiceInteractionSessionService) : VoiceInteractionSession(service) {
    private val main = Handler(Looper.getMainLooper())
    private var opened = false
    private var gotAssist = false
    private var gotShot = false

    override fun onShow(args: Bundle?, showFlags: Int) {
        super.onShow(args, showFlags)
        opened = false
        gotAssist = false
        gotShot = false
        Capture.begin()
        if (showFlags and SHOW_WITH_SCREENSHOT == 0) {
            Capture.update { it.copy(shotDone = true) }
            gotShot = true
            open()
        } else {
            main.postDelayed({ open() }, OPEN_AFTER_MS)
        }
        if (showFlags and SHOW_WITH_ASSIST == 0) gotAssist = true
        main.postDelayed({ hide() }, HIDE_AFTER_MS)
    }

    override fun onHide() {
        main.removeCallbacksAndMessages(null)
        super.onHide()
    }

    override fun onHandleAssist(state: AssistState) {
        // Several states come when the screen has more than one activity (a split); the focused one
        // is first. Only the first is kept.
        if (state.index == 0) {
            val structure = state.assistStructure
            val content = state.assistContent
            val pkg = structure?.activityComponent?.packageName
            // Only the page a browser shows: an app's intent is the one that started its activity, which
            // in a single-activity app such as T3's is stale after the first navigation.
            val uris = listOfNotNull(content?.webUri?.toString())
            val label = pkg?.let { p ->
                runCatching { context.packageManager.run { getApplicationLabel(getApplicationInfo(p, 0)).toString() } }.getOrNull()
            }
            Capture.update { it.copy(packageName = pkg, appLabel = label, screen = Destination.Screen(uris)) }
        }
        if (state.index == state.count - 1) {
            gotAssist = true
            maybeHide()
        }
    }

    override fun onHandleScreenshot(screenshot: Bitmap?) {
        if (screenshot == null) {
            Capture.update { it.copy(shotDone = true) }
            gotShot = true
            open()
            maybeHide()
            return
        }
        // Compress off the main thread; the copy keeps the bitmap ours after the callback returns.
        val bitmap = screenshot.copy(Bitmap.Config.ARGB_8888, false)
        encoder.execute {
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            bitmap.recycle()
            val jpeg = out.toByteArray()
            main.post {
                Capture.update { it.copy(jpeg = jpeg, shotDone = true) }
                gotShot = true
                open()
                maybeHide()
            }
        }
    }

    private fun open() {
        if (opened) return
        opened = true
        startAssistantActivity(QuickSend.intent(context, QuickSend.From.ASSIST))
    }

    private fun maybeHide() {
        if (opened && gotAssist && gotShot) hide()
    }

    companion object {
        private const val OPEN_AFTER_MS = 1_500L
        private const val HIDE_AFTER_MS = 5_000L
        private const val JPEG_QUALITY = 85
        private val encoder = Executors.newSingleThreadExecutor()
    }
}

/** Required for Settings to offer [AssistService]; LoR's assistant takes no voice. */
class NoRecognition : RecognitionService() {
    override fun onStartListening(recognizerIntent: Intent?, listener: Callback?) {
        listener?.error(android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY)
    }
    override fun onCancel(listener: Callback?) {}
    override fun onStopListening(listener: Callback?) {}
}
