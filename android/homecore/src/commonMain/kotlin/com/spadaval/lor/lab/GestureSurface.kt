package com.spadaval.lor.lab

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.NavigationEventTransitionState
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import com.spadaval.lor.ui.Bg
import com.spadaval.lor.ui.Chamfer
import com.spadaval.lor.ui.Cy
import com.spadaval.lor.ui.Edge
import com.spadaval.lor.ui.Line
import com.spadaval.lor.ui.LorTheme
import com.spadaval.lor.ui.OnOr
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.Raised
import com.spadaval.lor.ui.Type
import com.spadaval.lor.ui.Well
import com.spadaval.lor.ui.framedPanel
import com.spadaval.lor.ui.selection
import kotlin.math.roundToInt

/*
 * The lab's gesture test surface (version `gestures`): not a launcher, but one of each thing a
 * gesture script drives, so scripts/lab/run.mjs and every helper in gesture.mjs are proven on it.
 * A pager that follows the finger, a list that scrolls and flings, a tile that opens a menu on a
 * long press and moves when the press drifts into a drag, and two panes on the inner screen that
 * become one column on the cover. Its state sits above both layouts, so a fold keeps the page, the
 * scroll and the tile. The readout at the top says what the last gesture did, for the final shot.
 *
 * Anchors (Modifier.labAnchor): `pager`, `desk`, `tile`, `menu` (while open), `list`, `readout`.
 */

private const val PAGES = 5
private const val ROWS = 60
private val TileSize = 88.dp
private val MenuItems = listOf("Open", "Pin", "Remove")

private class GestureState {
    var tapped by mutableStateOf<Int?>(null)
    var picked by mutableStateOf<String?>(null)
    /** The tile's top left in the desk, in dp. */
    var tile by mutableStateOf(Offset(16f, 16f))
    var menu by mutableStateOf(false)
    var dragging by mutableStateOf(false)
    var doubleTaps by mutableIntStateOf(0)
    var backs by mutableIntStateOf(0)
}

@Composable
fun GestureSurface(scene: LabScene) {
    val s = remember { GestureState() }
    val pager = rememberPagerState { PAGES }
    val list = rememberLazyListState()
    val wide = scene.widthDp >= 600

    val back = rememberNavigationEventState(NavigationEventInfo.None)
    // How far a predictive back has come, 0 when none is under way.
    val backProgress = (back.transitionState as? NavigationEventTransitionState.InProgress)?.latestEvent?.progress ?: 0f
    // Back closes the menu first, then clears the tapped row; with neither, it is the system's.
    NavigationBackHandler(back, isBackEnabled = s.menu || s.tapped != null, onBackCompleted = {
        s.backs++
        if (s.menu) s.menu = false else s.tapped = null
    })

    LorTheme {
        Column(Modifier.fillMaxSize().background(Bg.copy(alpha = 0.55f)).padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Readout(s, pager, list, scene, backProgress)
            if (wide) {
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Pager(pager, Modifier.fillMaxWidth().height(220.dp))
                        Desk(s, backProgress, Modifier.fillMaxWidth().weight(1f))
                    }
                    Rows(s, list, Modifier.weight(1f).fillMaxHeight())
                }
            } else {
                Pager(pager, Modifier.fillMaxWidth().height(200.dp))
                Desk(s, backProgress, Modifier.fillMaxWidth().height(300.dp))
                Rows(s, list, Modifier.fillMaxWidth().weight(1f))
            }
        }
    }
}

@Composable
private fun Readout(s: GestureState, pager: PagerState, list: LazyListState, scene: LabScene, backProgress: Float) {
    val lines = listOf(
        "page ${pager.currentPage + 1}/$PAGES · row ${s.tapped ?: "none"} · top row ${list.firstVisibleItemIndex + 1}",
        "tile ${s.tile.x.roundToInt()},${s.tile.y.roundToInt()} · menu ${if (s.menu) "open" else "shut"}${s.picked?.let { " · picked $it" } ?: ""} · double taps ${s.doubleTaps}",
        "back ${s.backs}${if (backProgress > 0f) " · back at ${(backProgress * 100).roundToInt()}%" else ""} · ${if (scene.widthDp >= 600) "two panes" else "one column"} ${scene.widthDp}×${scene.heightDp}",
    )
    Column(Modifier.fillMaxWidth().framedPanel().padding(horizontal = 10.dp, vertical = 6.dp).labAnchor("readout")) {
        for (l in lines) Text(l.uppercase(), style = Type.label.copy(color = Cy))
    }
}

@Composable
private fun Pager(pager: PagerState, modifier: Modifier) {
    HorizontalPager(pager, modifier.labAnchor("pager"), pageSpacing = 10.dp) { page ->
        Box(Modifier.fillMaxSize().framedPanel(if (page % 2 == 0) Raised else Well), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("${page + 1}", style = Type.display.copy(color = Or))
                Text("PAGE ${page + 1} OF $PAGES · SWIPE", style = Type.label)
            }
        }
    }
}

@Composable
private fun Desk(s: GestureState, backProgress: Float, modifier: Modifier) {
    BoxWithConstraints(modifier.border(1.dp, Line).labAnchor("desk")) {
        val maxX = (maxWidth - TileSize).value.coerceAtLeast(0f)
        val maxY = (maxHeight - TileSize).value.coerceAtLeast(0f)
        val at = Offset(s.tile.x.coerceIn(0f, maxX), s.tile.y.coerceIn(0f, maxY))
        Text("DESK · DOUBLE TAP, LONG PRESS OR LONG PRESS AND DRAG THE TILE", style = Type.label, modifier = Modifier.align(Alignment.BottomStart).padding(8.dp))
        val density = LocalDensity.current.density
        Box(
            Modifier.offset(at.x.dp, at.y.dp).size(TileSize).labAnchor("tile")
                .pointerInput(Unit) { detectTapGestures(onDoubleTap = { s.doubleTaps++ }) }
                .pointerInput(maxX, maxY) {
                    var moved = 0f
                    detectDragGesturesAfterLongPress(
                        onDragStart = { moved = 0f; s.menu = true; s.picked = null; s.dragging = true },
                        onDragEnd = { s.dragging = false },
                        onDragCancel = { s.dragging = false },
                        onDrag = { change, delta ->
                            change.consume()
                            moved += delta.getDistance() / density
                            // A press that drifts past 8 dp is a drag: the menu gives way to it.
                            if (moved > 8f) s.menu = false
                            val t = s.tile.copy(x = s.tile.x.coerceIn(0f, maxX), y = s.tile.y.coerceIn(0f, maxY))
                            s.tile = Offset((t.x + delta.x / density).coerceIn(0f, maxX), (t.y + delta.y / density).coerceIn(0f, maxY))
                        },
                    )
                }
                // Drawn larger while held, after the pointer input so the finger's moves stay 1:1.
                .scale(if (s.dragging) 1.12f else 1f)
                .background(if (s.dragging) Or else Raised, Chamfer).border(1.dp, if (s.menu) Or else Edge, Chamfer),
            contentAlignment = Alignment.Center,
        ) { Text("TILE", style = Type.title.copy(color = if (s.dragging) OnOr else Or)) }
        if (s.menu) Menu(s, backProgress, Modifier.offset(at.x.dp, (at.y.dp + TileSize + 6.dp).coerceAtMost(maxHeight - 132.dp)))
    }
}

@Composable
private fun Menu(s: GestureState, backProgress: Float, modifier: Modifier) {
    // A predictive back shrinks the menu as it comes, the way the system's back preview does.
    Column(modifier.width(160.dp).scale(1f - 0.1f * backProgress).framedPanel(Raised).labAnchor("menu")) {
        for (item in MenuItems) {
            Text(
                item, style = Type.body,
                modifier = Modifier.fillMaxWidth().clickable { s.picked = item; s.menu = false }.padding(horizontal = 12.dp, vertical = 10.dp),
            )
        }
    }
}

@Composable
private fun Rows(s: GestureState, list: LazyListState, modifier: Modifier) {
    LazyColumn(modifier.border(1.dp, Line).labAnchor("list"), state = list) {
        items(ROWS) { i ->
            val n = i + 1
            val row = Modifier.fillMaxWidth().height(RowHeight).clickable { s.tapped = n }
            Box(if (s.tapped == n) row.selection() else row, contentAlignment = Alignment.CenterStart) {
                Text("Row $n", style = Type.body, modifier = Modifier.padding(horizontal = 12.dp))
            }
        }
    }
}

private val RowHeight: Dp = 48.dp
