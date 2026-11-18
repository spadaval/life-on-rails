@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalTextApi::class, kotlin.js.ExperimentalWasmJsInterop::class)

package com.spadaval.lor.lab

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.window.ComposeViewport
import com.spadaval.lor.ui.WebFonts
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.w3c.dom.HTMLButtonElement
import org.w3c.dom.events.Event
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.DragEvent
import org.w3c.dom.HTMLTextAreaElement
import kotlin.js.Promise
import org.w3c.files.FileReader
import org.w3c.files.get
import kotlinx.serialization.json.Json
import androidx.compose.runtime.remember
import org.w3c.fetch.Response
import org.w3c.dom.events.KeyboardEvent
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.add

/*
 * The lab page (index.html beside it). The URL hash is the only state: the controls write it and
 * the frame draws what it names, so any screen is a link. Everything it draws comes from files in
 * the bundle (the fonts, the wallpapers) and the fixtures in common code; it fetches nothing else.
 *
 * When a hash has been drawn, the body's `data-drawn` attribute holds it, for shots.mjs to wait on.
 * The body's `data-anchors` holds every [labAnchor] on screen as JSON, `{"name": [left, top,
 * right, bottom]}` in dp from the screen's top left, for scripts/lab/gesture.mjs.
 *
 * What is not a link: Home (draws the state afresh), Back (sent to Compose's back handlers), and
 * the speed switch (index.html's clock). Each is a button and a key; Fold and Rotate change the link.
 */

/** The page fills the viewport (index.html's `fill`, on a phone): the screen then follows the viewport's width. */
private val fill: Boolean get() = document.body?.classList?.contains("fill") == true

/** In fill, whether the viewport is wide enough for the inner screen; a resize (a real fold) changes it. */
private val viewportWide = mutableStateOf(false)

/** The screen to draw: the link's, or in fill the one the viewport is (cover under 600 px wide). */
private fun screenOf(link: LabLink): LabScreen = if (!fill) link.screen else if (viewportWide.value) LabScreen.Inner else LabScreen.Cover

/** Home presses: the scene is keyed on them, so Home draws the state afresh. */
private val homes = mutableIntStateOf(0)

/** Where the lab's Back reaches Compose: added to the page's navigation-event dispatcher by [Frame]. */
private val backInput = DirectNavigationEventInput()

private suspend fun bundled(path: String): ByteArray {
    val res = window.fetch(path).await<Response>()
    if (!res.ok) error("$path: ${res.status}")
    val a = Int8Array(res.arrayBuffer().await<ArrayBuffer>())
    return ByteArray(a.length) { a[it] }
}

private fun image(bytes: ByteArray): ImageBitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()

private fun el(id: String): HTMLElement = document.getElementById(id) as HTMLElement? ?: error("index.html has no #$id")

fun main() {
    LabAnchors.on = true
    Lab.concepts = labConcepts()
    MainScope().launch {
        val saira = bundled("fonts/saira.ttf")
        WebFonts.saira = FontFamily(listOf(400, 500, 600).map { w -> Font("saira-$w", saira, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) })
        WebFonts.condensed = FontFamily(
            Font("saira-condensed-medium", bundled("fonts/saira_condensed_medium.ttf"), FontWeight.Medium),
            Font("saira-condensed-semibold", bundled("fonts/saira_condensed_semibold.ttf"), FontWeight.SemiBold),
        )
        ConceptFonts.hanken = FontFamily(listOf(400, 500, 600).map { w -> Font("hanken-grotesk-$w", bundled("fonts/hanken-grotesk-$w.ttf"), FontWeight(w)) })
        val wallpapers = Wallpaper.entries.associateWith { image(bundled("wallpapers/${it.id}.png")) }

        var hash by mutableStateOf(window.location.hash)
        val controls = Controls { link -> window.location.hash = link.hash }
        fun show() {
            val parsed = parseHash(hash)
            controls.show(parsed)
            val phone = el("phone")
            val scene = LabScene(Fixtures.all.first(), screenOf(parsed.link), parsed.link.landscape)
            phone.style.width = "${scene.widthDp}px"
            phone.style.height = "${scene.heightDp}px"
            // ComposeViewport sizes its canvas on the window's resize, not its container's.
            window.dispatchEvent(Event("resize"))
        }
        show()
        window.addEventListener("hashchange", { hash = window.location.hash; show() })
        viewportWide.value = window.innerWidth >= 600
        window.addEventListener("resize", { viewportWide.value = window.innerWidth >= 600 })
        dropOneOffs(controls)
        predictiveBack()
        ComposeViewport(el("phone")) { Frame(hash, parseHash(hash), wallpapers) }
    }
}

/**
 * A JSON file dropped anywhere on the page opens as a one-off state, written into the hash so the
 * link is the state. One that doesn't read is named in the problem line and changes nothing.
 */
private fun dropOneOffs(controls: Controls) {
    document.addEventListener("dragover", { e -> e.preventDefault(); document.body?.classList?.add("dropping") })
    document.addEventListener("dragleave", { document.body?.classList?.remove("dropping") })
    document.addEventListener("drop", { e ->
        e.preventDefault()
        document.body?.classList?.remove("dropping")
        val file = (e as DragEvent).dataTransfer?.files?.get(0) ?: return@addEventListener
        val reader = FileReader()
        reader.onload = {
            val text = reader.result.toString()
            Fixtures.fromJson(text).fold(
                { controls.oneOff(Json.parseToJsonElement(text).toString()) },
                { controls.say("${file.name}: ${it.message}") },
            )
        }
        reader.readAsText(file)
    })
}

@Composable
private fun Frame(hash: String, parsed: Parsed, wallpapers: Map<Wallpaper, ImageBitmap>) {
    val link = parsed.link
    val fixture = remember(link.state) { Lab.fixture(link.state).first }
    Box(Modifier.fillMaxSize()) {
        Image(wallpapers.getValue(link.wallpaper), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        // The fixture's font size (Android's setting, `phone.fontScale`) for every version, and the
        // wallpaper for a concept to blur behind what floats.
        val density = LocalDensity.current
        CompositionLocalProvider(
            LocalDensity provides Density(density.density, density.fontScale * fixture.phone.fontScale),
            LocalWallpaper provides wallpapers.getValue(link.wallpaper),
            LocalWallpaperKind provides link.wallpaper,
        ) {
            // Keyed on what the scene is, not where it is drawn: a fold, a turn or a new wallpaper keeps
            // the surface's place, as the phone does. Home starts it again.
            key(link.version, link.state, homes.intValue) {
                Lab.version(link.version)!!.draw(LabScene(fixture, screenOf(link), link.landscape, link.wallpaper))
            }
        }
    }
    val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
    DisposableEffect(dispatcher) {
        dispatcher?.addInput(backInput)
        onDispose { dispatcher?.removeInput(backInput) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { LabAnchors.all.toMap() }.collect { anchors ->
            val json = buildJsonObject {
                for ((name, r) in anchors) putJsonArray(name) { add(r.left); add(r.top); add(r.right); add(r.bottom) }
            }
            document.body?.setAttribute("data-anchors", json.toString())
        }
    }
    // Keyed on the hash as written, not the link: two hashes can name one link, and the marker
    // must name the one now in the address bar.
    LaunchedEffect(hash) {
        // Two frames: the first lays the tree out, the second has drawn it.
        withFrameNanos {}
        withFrameNanos {}
        document.body?.setAttribute("data-drawn", hash)
    }
}

/** Sets the speed of index.html's animation clock: 1, 0.25, or 0 for none. */
private fun setSpeed(speed: Double): Unit = js("window.labSetSpeed(speed)")

private fun detailOf(e: Event): String = js("String(e.detail)")

private fun typedInto(e: Event): String = js("String(e.composedPath()[0]?.tagName ?? '')")

/** A back with no gesture to it, as a phone's button sends it. */
private fun backNow() {
    backInput.backStarted(NavigationEvent())
    backInput.backCompleted()
}

/**
 * A predictive back, step by step, from scripts/lab/gesture.mjs: a `lab-back` event on the document
 * whose detail is `start <x> <y> <edge>`, `progress <p> <x> <y> <edge>`, `cancel` or `complete`,
 * with x and y in dp and edge `left` or `right`.
 */
private fun predictiveBack() {
    document.addEventListener("lab-back", { e ->
        val w = detailOf(e).split(" ")
        fun edge(i: Int) = if (w.getOrNull(i) == "right") NavigationEvent.EDGE_RIGHT else NavigationEvent.EDGE_LEFT
        when (w[0]) {
            "start" -> backInput.backStarted(NavigationEvent(touchX = w[1].toFloat(), touchY = w[2].toFloat(), progress = 0f, swipeEdge = edge(3)))
            "progress" -> backInput.backProgressed(NavigationEvent(touchX = w[2].toFloat(), touchY = w[3].toFloat(), progress = w[1].toFloat(), swipeEdge = edge(4)))
            "cancel" -> backInput.backCancelled()
            "complete" -> backInput.backCompleted()
        }
    })
}

/** Whether this page may write the clipboard: only in a secure context (https or localhost). */
private fun clipboardAvailable(): Boolean = js("window.isSecureContext && typeof navigator.clipboard !== 'undefined'")

private fun writeClipboard(text: String): Promise<JsAny?> = js("navigator.clipboard.writeText(text)")

private fun showModal(dialog: HTMLElement): Unit = js("dialog.showModal()")

private fun closeDialog(dialog: HTMLElement): Unit = js("dialog.close()")

/** The toolbar in index.html: filled from the registry, and writing the hash when changed. */
private class Controls(val go: (LabLink) -> Unit) {
    private val version = el("version") as HTMLSelectElement
    private val state = el("state") as HTMLSelectElement
    private val wallpaper = el("wallpaper") as HTMLSelectElement
    private val fold = el("fold") as HTMLButtonElement
    private val rotate = el("rotate") as HTMLButtonElement
    private val speed = el("speed") as HTMLButtonElement
    private var speedAt = 0
    private val json = el("json") as HTMLButtonElement
    private val problem = el("problem")
    /** The State dropdown's entry for a one-off, shown while the hash holds one. */
    private val oneOffOption = (document.createElement("option") as HTMLOptionElement).apply {
        value = ONE_OFF; text = "one-off (JSON)"; title = "A state from JSON in the link or a dropped file"
    }
    private var current = LabLink.first()

    init {
        fill(version, Lab.versions.map { Triple(it.name, it.name, it.about) })
        fill(state, Lab.states.map { Triple(it.name, it.name, it.about) })
        fill(wallpaper, Wallpaper.entries.map { Triple(it.id, it.label, it.label) })
        version.addEventListener("change", { go(current.copy(version = version.value)) })
        state.addEventListener("change", { if (state.value != ONE_OFF) go(current.copy(state = state.value)) })
        json.addEventListener("click", { showJson(Fixtures.toJson(Lab.fixture(current.state).first)) })
        wallpaper.addEventListener("change", { go(current.copy(wallpaper = Wallpaper.entries.first { it.id == wallpaper.value })) })
        fold.addEventListener("click", { toggleFold() })
        rotate.addEventListener("click", { go(current.copy(landscape = !current.landscape)) })
        el("home").addEventListener("click", { homes.intValue++ })
        el("back").addEventListener("click", { backNow() })
        speed.addEventListener("click", { nextSpeed() })
        document.addEventListener("keydown", { e -> key(e as KeyboardEvent) })
        showSpeed()
    }

    private fun toggleFold() = go(current.copy(screen = if (current.screen == LabScreen.Cover) LabScreen.Inner else LabScreen.Cover))

    private fun nextSpeed() {
        speedAt = (speedAt + 1) % SPEEDS.size
        setSpeed(SPEEDS[speedAt].first)
        showSpeed()
    }

    private fun showSpeed() {
        speed.textContent = SPEEDS[speedAt].second
        speed.setAttribute("data-speed", SPEEDS[speedAt].first.toString())
    }

    /**
     * The toolbar's keys, the letters its buttons are titled with: H Home, Escape or B Back, F Fold
     * and Unfold, R Rotate, S the speed. Not while typing in a field or with a modifier held.
     */
    private fun key(e: KeyboardEvent) {
        if (e.ctrlKey || e.metaKey || e.altKey) return
        // The element typed into, inside Compose's shadow root too (e.target is retargeted to its host).
        val tag = typedInto(e)
        if (tag == "INPUT" || tag == "TEXTAREA" || tag == "SELECT") return
        when (e.key) {
            "h", "H" -> homes.intValue++
            // Escape typed into Compose's canvas is already a back there (Compose's own input);
            // sending one here as well would go back twice.
            "Escape" -> if (tag == "CANVAS") return else backNow()
            "b", "B" -> backNow()
            "f", "F" -> toggleFold()
            "r", "R" -> go(current.copy(landscape = !current.landscape))
            "s", "S" -> nextSpeed()
            else -> return
        }
        e.preventDefault()
    }

    private fun fill(select: HTMLSelectElement, options: List<Triple<String, String, String>>) {
        for ((value, label, title) in options) {
            val o = document.createElement("option") as HTMLOptionElement
            o.value = value; o.text = label; o.title = title
            select.appendChild(o)
        }
    }

    fun oneOff(json: String) = go(current.copy(state = json))

    fun say(text: String) {
        problem.textContent = text
        problem.hidden = false
    }

    fun show(parsed: Parsed) {
        current = parsed.link
        version.value = current.version
        val oneOff = Lab.isOneOff(current.state)
        if (oneOff && oneOffOption.parentNode == null) state.appendChild(oneOffOption)
        if (!oneOff) oneOffOption.remove()
        state.value = if (oneOff) ONE_OFF else current.state
        val problemText = listOfNotNull(parsed.problem, Lab.fixture(current.state).second).joinToString("; ").ifEmpty { null }
        wallpaper.value = current.wallpaper.id
        fold.textContent = if (current.screen == LabScreen.Cover) "Unfold" else "Fold"
        fold.title = "${current.screen.label} (F)"
        rotate.textContent = if (current.landscape) "Upright" else "Rotate"
        problem.textContent = problemText ?: ""
        problem.hidden = problemText == null
        document.title = "Lab · " + if (oneOff) "${current.version}/one-off" else current.hash
    }

    private val jsonBox = el("json-box")
    private val jsonText = el("json-text") as HTMLTextAreaElement
    private val jsonCopy = el("json-copy") as HTMLButtonElement
    private val jsonSaid = el("json-said")

    init {
        el("json-close").addEventListener("click", { closeDialog(jsonBox) })
        jsonCopy.addEventListener("click", {
            jsonSaid.textContent = ""
            MainScope().launch {
                jsonSaid.textContent = try {
                    writeClipboard(jsonText.value).await<JsAny?>()
                    "Copied."
                } catch (e: Throwable) {
                    "Not copied: ${(e.message ?: "the browser refused").trimEnd('.')}. Select the text and copy it by hand."
                }
            }
        })
    }

    /**
     * The state's JSON in a dialog, selected, to copy by hand: the published lab is plain HTTP on
     * the tailnet, where the browser has no clipboard API. Where it has one, Copy uses it and says
     * whether it worked.
     */
    private fun showJson(text: String) {
        jsonText.value = text
        jsonSaid.textContent = ""
        jsonCopy.hidden = !clipboardAvailable()
        showModal(jsonBox)
        jsonText.focus()
        jsonText.select()
    }

    private companion object {
        const val ONE_OFF = "(one-off)"
        /** The speed switch's steps: animations at full speed, at a quarter, and none (each ends at once). */
        val SPEEDS = listOf(1.0 to "Speed 1×", 0.25 to "Speed ¼×", 0.0 to "No motion")
    }
}
