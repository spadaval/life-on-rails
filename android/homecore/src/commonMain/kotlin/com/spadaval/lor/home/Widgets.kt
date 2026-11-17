package com.spadaval.lor.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import com.spadaval.lor.ui.Chamfer
import com.spadaval.lor.ui.DismissColors
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.PanelFace
import com.spadaval.lor.ui.Scrim
import com.spadaval.lor.ui.StateFill
import com.spadaval.lor.ui.Txt
import com.spadaval.lor.ui.Type
import com.spadaval.lor.ui.Well
import com.spadaval.lor.ui.framedPanel
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Android widgets on home (lor-r0f9): the process's widget host, the `widget` block (decision
 * lor-9wlx) with its menu and resize frame, the note for a widget that is gone, and the picker
 * sheet. The bind and configure flow runs in [HomeActivity], which gets the activity results.
 *
 * Borrowed, as lor-sbth's decision note records (read 2026-11-07, ideas only, no code copied):
 * - Kvaesitso (github.com/MM2-0/Kvaesitso, GPL-3.0): one AppWidgetHost for the process, listening
 *   while home is on screen (`ui/base/AppWidgetHost.kt`); each widget view made with the
 *   application context in an AndroidView and told its size with `updateAppWidgetSize(SizeF)`
 *   (`launcher/widgets/external/AppWidgetHost.kt`); the bind and configure flow
 *   (`sheets/WidgetPickerSheet.kt`); the height in dp at the column's width, resized within the
 *   provider's limits (`sheets/ConfigureWidgetSheet.kt`); the note with Remove for a widget that
 *   can't be drawn (`launcher/widgets/external/AppWidget.kt`).
 * - Launcher3 as Lawnchair carries it (Apache-2.0): host id 1024, listening with the activity
 *   (`widget/LauncherWidgetHolder.java`); the long press found without taking the widget's taps
 *   (`CheckLongPressHelper.java`, `LauncherAppWidgetHostView.onInterceptTouchEvent`); the sweep
 *   of ids no layout names (`model/DatabaseHelper.java`'s `removeGhostWidgets`); Settings only
 *   for a reconfigurable widget (`WIDGET_FEATURE_RECONFIGURABLE`).
 * Widgets keep their own colours (DESIGN.md has no Material You palette to map them to).
 */

/** While a resize frame is up, a press outside it closes it, and does nothing else. */
internal fun Modifier.closeResizeOutside(ui: HomeUi): Modifier = pointerInput(ui) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
        if (ui.resizing != null && ui.resizeFrame?.contains(down.position) != true) {
            ui.resizing = null
            down.consume()
        }
    }
}

/**
 * A `widget` block (decision lor-9wlx): the widget at its column's width, or at its provider's
 * `maxResizeWidth` centred when that is narrower, and at the block's height, told its size when
 * either changes. A long press opens its menu; while locked (decision lor-73li) the menu has no
 * Resize or Remove. [panelHeight] (dp) caps a resize. A widget whose provider is gone draws a note.
 */
@Composable
internal fun WidgetBlock(block: StackBlock.Widget, panelHeight: Float, ui: HomeUi, home: HomeContext) {
    val platform = home.actions
    // Looked up again when the app list changes, so an app turned off or on shows at once.
    val info = remember(block.id, block.provider, home.apps) { platform.widget(block.id, block.provider) }
    val locked = home.file?.locked == true
    if (info == null) return GoneWidget(block, home, locked)
    val density = LocalDensity.current.density
    fun dp(px: Int) = px / density
    val label = info.label
    val reconfigurable = info.reconfigurable
    val resizing = ui.resizing == block.id
    val range = WidgetSize.resizeRange(dp(info.minHeight), dp(info.minResizeHeight), dp(info.maxResizeHeight), panelHeight)
    // The frame's height while a finger has its handle; the file's otherwise.
    var dragged by remember { mutableFloatStateOf(block.height.toFloat()) }
    val height = if (resizing) dragged else block.height.toFloat()
    val menu = rememberFloatingMenu(ui)
    val hasMenu = !locked || reconfigurable
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        val width = WidgetSize.width(maxWidth.value, dp(info.maxResizeWidth))
        // Where the widget is, which its menu points at (lor-fhk5).
        val spot = remember { IconSpot() }
        Box(Modifier.align(Alignment.TopCenter).size(width.dp, height.dp).iconSpot(spot)) {
            val press = remember { WidgetPress() }
            platform.WidgetView(block, info, width, if (hasMenu) ({ menu.open = true }) else null, press, Modifier.fillMaxSize())
            if (resizing) {
                BackHandler { ui.resizing = null }
                Box(
                    Modifier.matchParentSize().border(1.dp, Or.copy(alpha = 0.85f))
                        .onGloballyPositioned { ui.resizeFrame = it.boundsInRoot() },
                )
                // The handle, on the frame's bottom edge: it follows the finger within the provider's
                // limits and writes the height when let go, with Undo.
                Box(
                    Modifier.align(Alignment.BottomCenter).size(72.dp, 36.dp)
                        .semantics { contentDescription = "Resize $label" }
                        .pointerInput(block.id) {
                            detectVerticalDragGestures(
                                onDragStart = { dragged = block.height.toFloat() },
                                onDragEnd = { resize(block, dragged.roundToInt(), ui, home) },
                                onDragCancel = { ui.resizing = null },
                            ) { change, dy ->
                                change.consume()
                                dragged = (dragged + dy / density).coerceIn(range.first.toFloat(), range.last.toFloat())
                            }
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Box(Modifier.padding(bottom = 6.dp).size(40.dp, 4.dp).background(Or))
                }
            }
            if (menu.open) {
                // It points at the finger and rings the widget: a tall widget leaves no room beside its box.
                val box = { spot.bounds()?.toBounds() }
                Popover(ui.popovers, { press.at() ?: box() }, { menu.open = false }, PopoverRing.BOX, source = box) {
                    if (!locked) PopoverItem("Resize") {
                        menu.open = false
                        dragged = block.height.toFloat()
                        ui.resizing = block.id
                    }
                    if (reconfigurable) PopoverItem("Settings") {
                        menu.open = false
                        home.actions.widgetSettings(block.id)
                    }
                    if (!locked) PopoverItem("Remove") {
                        menu.open = false
                        home.actions.edit("$label is off home.") { it.removeWidget(block.id) }
                    }
                }
            }
        }
    }
}

/** Writes the widget's new [height], drawn at once while the write lands; a height unchanged writes nothing. */
private fun resize(block: StackBlock.Widget, height: Int, ui: HomeUi, home: HomeContext) {
    ui.resizing = null
    val now = home.file ?: return
    val next = try {
        now.setWidgetHeight(block.id, height)
    } catch (e: LayoutError) {
        return home.actions.refused(e.message ?: "it can't be that tall")
    }
    if (next == now) return
    home.drag.landing = next
    home.actions.edit("Widget resized.", after = { home.drag.landing = null }) { file ->
        if (file != now) throw LayoutError(HOME_CHANGED)
        next
    }
}

/**
 * A widget whose provider is gone (decision lor-9wlx): a note at its height, naming its app, with
 * Remove, which takes the block out of the file (refused, as any Remove is, when it is the stack's
 * only block). Not while locked.
 */
@Composable
private fun GoneWidget(block: StackBlock.Widget, home: HomeContext, locked: Boolean) {
    val platform = home.actions
    val app = remember(block.provider) { platform.appLabel(block.provider) }
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp).height(block.height.dp)
            .framedPanel(StateFill).padding(start = 14.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text("$app's widget isn't available.", style = Type.body.copy(color = Txt), maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("Its app is gone or turned off.", style = Type.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (!locked) TextButton(
            onClick = { home.actions.edit("$app's widget is off home.") { it.removeWidget(block.id) } },
            shape = RectangleShape,
            colors = DismissColors,
        ) { Text("Remove") }
    }
}

/**
 * The widget picker (lor-r0f9): a sheet over home (DESIGN.md's Elevation & Depth: a framed face
 * over a dim, with the float shadow) of the main profile's widget providers, grouped by app. A
 * choice closes it and adds the widget to the stack at [at] ([HomeActions.addWidget]).
 */
@Composable
internal fun WidgetPicker(at: Cell, home: HomeContext, close: () -> Unit) {
    BackHandler(onBack = close)
    val platform = home.actions
    val density = LocalDensity.current.density
    val apps by produceState<List<PickerApp>?>(null, platform) {
        value = platform.widgetProviders((PREVIEW_W * density).roundToInt(), (PREVIEW_H * density).roundToInt(), density)
    }
    Box(
        Modifier.fillMaxSize().background(Scrim)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close),
    ) {
        Column(
            Modifier.align(Alignment.BottomCenter).safeDrawingPadding().padding(8.dp).widthIn(max = 560.dp).fillMaxWidth().fillMaxHeight(0.8f)
                .shadow(8.dp, Chamfer, ambientColor = Color.Black, spotColor = Color.Black)
                .framedPanel(PanelFace)
                .swallowTaps()
                .padding(vertical = 16.dp),
        ) {
            Text("ADD A WIDGET", style = Type.title, modifier = Modifier.padding(horizontal = 20.dp))
            val list = apps
            Box(Modifier.weight(1f).fillMaxWidth().padding(top = 12.dp)) {
                when {
                    list == null -> Text("Reading the widgets…", style = Type.bodySmall, modifier = Modifier.padding(horizontal = 20.dp))
                    list.isEmpty() -> Text("No app offers a widget.", style = Type.bodySmall, modifier = Modifier.padding(horizontal = 20.dp))
                    else -> LazyColumn(Modifier.fillMaxSize()) {
                        list.forEach { app ->
                            item(key = "app:${app.widgets.first().info.packageName}") {
                                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    app.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(22.dp)) }
                                    Text(app.label.uppercase(), style = Type.label, modifier = Modifier.padding(start = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            items(app.widgets, key = { it.info.provider }) { w ->
                                Row(
                                    Modifier.fillMaxWidth()
                                        .clickable {
                                            close()
                                            home.actions.addWidget(at, w.info)
                                        }
                                        .padding(horizontal = 20.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(Modifier.size(PREVIEW_W.dp, PREVIEW_H.dp).background(Well), contentAlignment = Alignment.Center) {
                                        w.image?.let { Image(it, contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(4.dp)) }
                                    }
                                    Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                        Text(w.label, style = Type.body, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text("${w.minWidth} × ${w.minHeight} dp", style = Type.bodySmall.copy(fontSize = 13.sp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 12.dp), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = close, shape = RectangleShape, colors = DismissColors) { Text("Cancel") }
            }
        }
    }
}


private const val PREVIEW_W = 96
private const val PREVIEW_H = 72
