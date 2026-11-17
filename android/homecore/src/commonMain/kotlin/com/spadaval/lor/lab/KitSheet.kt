package com.spadaval.lor.lab

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.spadaval.lor.ui.Bad
import com.spadaval.lor.ui.Bg
import com.spadaval.lor.ui.Chamfer
import com.spadaval.lor.ui.Cy
import com.spadaval.lor.ui.Dim
import com.spadaval.lor.ui.Edge
import com.spadaval.lor.ui.Frame
import com.spadaval.lor.ui.Line
import com.spadaval.lor.ui.LorTheme
import com.spadaval.lor.ui.Motion
import com.spadaval.lor.ui.Mute
import com.spadaval.lor.ui.OnOr
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.OrDeep
import com.spadaval.lor.ui.Palette
import com.spadaval.lor.ui.PanelFace
import com.spadaval.lor.ui.PressScale
import com.spadaval.lor.ui.Raised
import com.spadaval.lor.ui.Scrim
import com.spadaval.lor.ui.StateFill
import com.spadaval.lor.ui.Txt
import com.spadaval.lor.ui.Type
import com.spadaval.lor.ui.Veil
import com.spadaval.lor.ui.Well
import com.spadaval.lor.ui.framedPanel
import com.spadaval.lor.ui.selection

/*
 * The kit sheet: `current` until the screens move into this module. Everything the look is made
 * of, drawn from the same tokens the app uses (com.spadaval.lor.ui), over the lab's wallpaper:
 * every colour, every type step, the chamfered panel, the selection, the press scales and the
 * motion tokens.
 */

private class Swatch(val name: String, val color: Color, val argb: Long?)

private val solids = listOf(
    Swatch("Deep Space", Bg, Palette.BG), Swatch("Instrument Well", Well, Palette.WELL),
    Swatch("Panel Face", PanelFace, Palette.PANEL), Swatch("Raised Plate", Raised, Palette.RAISED),
    Swatch("Engraved Line", Line, Palette.LINE), Swatch("Bulkhead Edge", Edge, Palette.EDGE),
    Swatch("Frame Bronze", Frame, Palette.FRAME), Swatch("Starlight", Txt, Palette.TXT),
    Swatch("Dust", Mute, Palette.MUTE), Swatch("Far Dust", Dim, Palette.DIM),
    Swatch("Drive Amber", Or, Palette.OR), Swatch("Banked Amber", OrDeep, Palette.OR_DEEP),
    Swatch("Nav-Beacon Cyan", Cy, Palette.CY), Swatch("Hull-Breach Coral", Bad, Palette.BAD),
    Swatch("On amber", OnOr, Palette.ON_OR),
)

/** The colours drawn with alpha, over the wallpaper as they are on the phone. */
private val glazes = listOf(
    Swatch("State fill · Panel 87%", StateFill, null), Swatch("Scrim · black 60%", Scrim, null),
    Swatch("Veil · Deep Space 92%", Veil, null),
    Swatch("Folder glass · Deep Space 72%", Bg.copy(alpha = Palette.FOLDER_GLASS.toFloat()), null),
    Swatch("Folder dim · black 50%", Color.Black.copy(alpha = Palette.FOLDER_DIM.toFloat()), null),
)

private val steps: List<Pair<String, TextStyle>> = listOf(
    "display 56" to Type.display, "headline 22" to Type.headline, "title 15" to Type.title,
    "body 15" to Type.body, "bodySmall 13" to Type.bodySmall, "label 12.5" to Type.label, "button 13.5" to Type.button,
)

/** The steps set in Saira Condensed, which callers uppercase. */
private val caps = setOf("display 56", "headline 22", "title 15", "label 12.5")

@Composable
fun KitSheet(scene: LabScene) {
    LorTheme {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Section("Kit · ${scene.fixture.name} · ${scene.screen.id} ${scene.screen.widthDp}×${scene.screen.heightDp} dp") {
                Text("The look's tokens, drawn by the app's own code. Screens replace this sheet as they move into homecore.", style = Type.bodySmall)
            }
            Section("Colours") {
                Swatches(solids)
            }
            Glazes()
            Section("Type") {
                for ((name, style) in steps) {
                    Text(name.uppercase(), style = Type.label)
                    Text(if (name in caps) "DRIVE 1452 · JUMP READY" else "Drive ready in 14:52, three jumps", style = style)
                    Spacer(Modifier.height(8.dp))
                }
            }
            Section("Panel and selection") {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.weight(1f).height(72.dp).framedPanel().padding(10.dp)) {
                        Text("FRAMED PANEL", style = Type.title)
                    }
                    Box(Modifier.weight(1f).height(72.dp).framedPanel(Raised).padding(10.dp)) {
                        Text("ON RAISED", style = Type.title)
                    }
                }
                Spacer(Modifier.height(12.dp))
                Column(Modifier.fillMaxWidth().border(1.dp, Line)) {
                    Text("A row", style = Type.body, modifier = Modifier.fillMaxWidth().padding(10.dp))
                    Text("The selected row", style = Type.body, modifier = Modifier.fillMaxWidth().selection().padding(10.dp))
                    Text("Another row", style = Type.body, modifier = Modifier.fillMaxWidth().padding(10.dp))
                }
            }
            Section("Press scales") {
                Text("Held still at the pressed scale; press one to see it move.", style = Type.bodySmall)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    PressDemo("Tile 0.9", PressScale.Tile, 0.9f, Modifier.size(64.dp))
                    PressDemo("Row 0.97", PressScale.Row, 0.97f, Modifier.size(width = 120.dp, height = 44.dp))
                }
            }
            Section("Motion") {
                MotionRow("quick", "${Motion.QUICK} ms, fast out slow in: fades and feedback")
                MotionRow("move", "${Motion.MOVE} ms, fast out slow in: a thing taking its new place")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("SETTLE", style = Type.label, modifier = Modifier.width(64.dp))
                    Text("spring, damping 1 (no bounce), stiffness ${Spring.StiffnessMediumLow.toInt()}: whatever a finger let go of", style = Type.bodySmall)
                }
            }
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth().framedPanel(PanelFace).padding(14.dp)) {
        Text(title.uppercase(), style = Type.title)
        Spacer(Modifier.height(10.dp))
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Swatches(list: List<Swatch>) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (s in list) {
            Column(Modifier.width(96.dp)) {
                Box(Modifier.fillMaxWidth().height(40.dp).background(s.color).border(1.dp, Edge))
                Text(s.name, style = Type.bodySmall.copy(color = Txt))
                if (s.argb != null) Text("#" + (s.argb and 0xFFFFFF).toString(16).padStart(6, '0').uppercase(), style = Type.label)
            }
        }
    }
}

/**
 * The colours drawn with alpha, each sample straight on the wallpaper (no panel under it), so the
 * wallpaper picker shows how each one reads; only the labels sit on a panel.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Glazes() {
    Column(Modifier.fillMaxWidth()) {
        Text("WITH ALPHA, OVER THE WALLPAPER", style = Type.title, modifier = Modifier.background(StateFill).padding(horizontal = 6.dp, vertical = 2.dp))
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (s in glazes) {
                Column(Modifier.width(96.dp)) {
                    Box(Modifier.fillMaxWidth().height(56.dp).background(s.color).border(1.dp, Edge))
                    Text(s.name, style = Type.bodySmall.copy(color = Txt), modifier = Modifier.fillMaxWidth().background(PanelFace).padding(4.dp))
                }
            }
        }
    }
}

@Composable
private fun PressDemo(label: String, press: PressScale, held: Float, box: Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // At rest, and held at the pressed scale, so a still shot shows the difference.
            Box(box.background(Raised, Chamfer))
            Box(box.scale(held).background(Raised, Chamfer).border(1.dp, Or.copy(alpha = 0.55f), Chamfer))
        }
        val source = remember { MutableInteractionSource() }
        Box(
            box.clickable(interactionSource = source, indication = press) {}.background(Or, Chamfer),
            contentAlignment = Alignment.Center,
        ) { Text("PRESS", style = Type.label.copy(color = OnOr)) }
        Text(label.uppercase(), style = Type.label)
    }
}

@Composable
private fun MotionRow(name: String, about: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 8.dp)) {
        Text(name.uppercase(), style = Type.label, modifier = Modifier.width(64.dp))
        // The easing curve, time across and progress up.
        Canvas(Modifier.size(width = 56.dp, height = 36.dp).background(Well)) {
            val path = Path()
            for (i in 0..32) {
                val t = i / 32f
                val p = Offset(t * size.width, size.height * (1 - FastOutSlowInEasing.transform(t)))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, Or, style = Stroke(width = 1.5.dp.toPx()))
        }
        Spacer(Modifier.width(10.dp))
        Text(about, style = Type.bodySmall)
    }
}
