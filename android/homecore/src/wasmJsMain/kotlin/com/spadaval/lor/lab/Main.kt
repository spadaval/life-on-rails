@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalTextApi::class, kotlin.js.ExperimentalWasmJsInterop::class)

package com.spadaval.lor.lab

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import org.w3c.fetch.Response

/*
 * The lab page (index.html beside it). The URL hash is the only state: the controls write it and
 * the frame draws what it names, so any screen is a link. Everything it draws comes from files in
 * the bundle (the fonts, the wallpapers) and the fixtures in common code; it fetches nothing else.
 *
 * When a hash has been drawn, the body's `data-drawn` attribute holds it, for shots.mjs to wait on.
 */

private suspend fun bundled(path: String): ByteArray {
    val res = window.fetch(path).await<Response>()
    if (!res.ok) error("$path: ${res.status}")
    val a = Int8Array(res.arrayBuffer().await<ArrayBuffer>())
    return ByteArray(a.length) { a[it] }
}

private fun image(bytes: ByteArray): ImageBitmap = Image.makeFromEncoded(bytes).toComposeImageBitmap()

private fun el(id: String): HTMLElement = document.getElementById(id) as HTMLElement? ?: error("index.html has no #$id")

fun main() {
    MainScope().launch {
        val saira = bundled("fonts/saira.ttf")
        WebFonts.saira = FontFamily(listOf(400, 500, 600).map { w -> Font("saira-$w", saira, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w))) })
        WebFonts.condensed = FontFamily(
            Font("saira-condensed-medium", bundled("fonts/saira_condensed_medium.ttf"), FontWeight.Medium),
            Font("saira-condensed-semibold", bundled("fonts/saira_condensed_semibold.ttf"), FontWeight.SemiBold),
        )
        val wallpapers = Wallpaper.entries.associateWith { image(bundled("wallpapers/${it.id}.png")) }

        var hash by mutableStateOf(window.location.hash)
        val controls = Controls { link -> window.location.hash = link.hash }
        fun show() {
            val parsed = parseHash(hash)
            controls.show(parsed)
            val phone = el("phone")
            phone.style.width = "${parsed.link.screen.widthDp}px"
            phone.style.height = "${parsed.link.screen.heightDp}px"
            // ComposeViewport sizes its canvas on the window's resize, not its container's.
            window.dispatchEvent(Event("resize"))
        }
        show()
        window.addEventListener("hashchange", { hash = window.location.hash; show() })
        ComposeViewport(el("phone")) { Frame(hash, parseHash(hash), wallpapers) }
    }
}

@Composable
private fun Frame(hash: String, parsed: Parsed, wallpapers: Map<Wallpaper, ImageBitmap>) {
    val link = parsed.link
    Box(Modifier.fillMaxSize()) {
        Image(wallpapers.getValue(link.wallpaper), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        key(link) {
            Lab.version(link.version)!!.draw(LabScene(Lab.state(link.state)!!, link.screen))
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

/** The toolbar in index.html: filled from the registry, and writing the hash when changed. */
private class Controls(val go: (LabLink) -> Unit) {
    private val version = el("version") as HTMLSelectElement
    private val state = el("state") as HTMLSelectElement
    private val wallpaper = el("wallpaper") as HTMLSelectElement
    private val fold = el("fold") as HTMLButtonElement
    private val problem = el("problem")
    private var current = LabLink.first()

    init {
        fill(version, Lab.versions.map { Triple(it.name, it.name, it.about) })
        fill(state, Lab.states.map { Triple(it.name, it.name, it.about) })
        fill(wallpaper, Wallpaper.entries.map { Triple(it.id, it.label, it.label) })
        version.addEventListener("change", { go(current.copy(version = version.value)) })
        state.addEventListener("change", { go(current.copy(state = state.value)) })
        wallpaper.addEventListener("change", { go(current.copy(wallpaper = Wallpaper.entries.first { it.id == wallpaper.value })) })
        fold.addEventListener("click", {
            go(current.copy(screen = if (current.screen == LabScreen.Cover) LabScreen.Inner else LabScreen.Cover))
        })
    }

    private fun fill(select: HTMLSelectElement, options: List<Triple<String, String, String>>) {
        for ((value, label, title) in options) {
            val o = document.createElement("option") as HTMLOptionElement
            o.value = value; o.text = label; o.title = title
            select.appendChild(o)
        }
    }

    fun show(parsed: Parsed) {
        current = parsed.link
        version.value = current.version
        state.value = current.state
        wallpaper.value = current.wallpaper.id
        fold.textContent = if (current.screen == LabScreen.Cover) "Unfold" else "Fold"
        fold.title = current.screen.label
        problem.textContent = parsed.problem ?: ""
        problem.hidden = parsed.problem == null
        document.title = "Lab · ${current.hash}"
    }
}
