package com.spadaval.lor.home

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.util.SizeF
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.AbsListView
import androidx.activity.compose.BackHandler
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
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.drawable.toBitmap
import com.spadaval.lor.LorApp
import com.spadaval.lor.ui.Chamfer
import com.spadaval.lor.ui.DismissColors
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.PanelFace
import com.spadaval.lor.ui.Raised
import com.spadaval.lor.ui.Scrim
import com.spadaval.lor.ui.StateFill
import com.spadaval.lor.ui.Txt
import com.spadaval.lor.ui.Type
import com.spadaval.lor.ui.Well
import com.spadaval.lor.ui.framedPanel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

private const val TAG = "LorWidgets"

/**
 * The process's widget host (lor-r0f9), held by [HomeStore]: the ids LoR bound, their views, and
 * the sweep of ids no layout names, once per process when `layout.json` first parses.
 */
class WidgetHost(context: Context) {
    private val app = context.applicationContext
    val manager: AppWidgetManager = AppWidgetManager.getInstance(app)
    val host: AppWidgetHost = object : AppWidgetHost(app, HOST_ID) {
        override fun onCreateView(context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?): AppWidgetHostView = LorWidgetView(context)
    }
    private var swept = false

    /**
     * The widget on its way to home (lor-r0f9): bound, waiting for Android's bind dialog or its
     * configure screen. Held here, for the process, so a home recreated behind those screens (a
     * configuration change home doesn't handle itself) still places it. A process that dies there
     * loses it; the id it bound is in no layout, so the next start's sweep deletes it.
     */
    var adding: Adding? = null

    /** A widget on its way to home: its bound [id], the stack [at] it goes to, and its provider. */
    class Adding(val id: Int, val at: Cell, val info: AppWidgetProviderInfo)

    /** From [HomeActivity.onStart]: widgets get their updates while home is started. */
    fun startListening() = try {
        host.startListening()
    } catch (e: RuntimeException) {
        Log.w(TAG, "the host can't start listening", e)
    }

    /** From [HomeActivity.onStop]. */
    fun stopListening() = try {
        host.stopListening()
    } catch (e: RuntimeException) {
        Log.w(TAG, "the host can't stop listening", e)
    }

    fun allocate(): Int = host.allocateAppWidgetId()

    fun delete(id: Int, why: String) {
        host.deleteAppWidgetId(id)
        Log.i(TAG, "deleted $id: $why")
    }

    /** The provider bound to [id] when it is [provider]; null when the widget is gone (decision lor-9wlx). */
    fun info(id: Int, provider: String): AppWidgetProviderInfo? =
        manager.getAppWidgetInfo(id)?.takeIf { it.provider == ComponentName.unflattenFromString(provider) }

    /** A new view of the widget [id], made with the application context as Kvaesitso does. */
    fun view(id: Int, info: AppWidgetProviderInfo): LorWidgetView = host.createView(app, id, info) as LorWidgetView

    /**
     * Deletes every id the host holds that no layout of [files] names, the first time it is called
     * in the process; the caller passes `layout.json` and `layout.good.json` as they parsed, and
     * doesn't call it while `layout.json` doesn't parse (decision lor-9wlx).
     */
    fun sweep(files: List<LayoutFile>) {
        if (swept || files.isEmpty()) return
        swept = true
        // A failure here must not fail the read it follows; the next process tries again.
        try {
            val held = host.appWidgetIds.toList()
            val gone = widgetIdsToDelete(held, files)
            gone.forEach { host.deleteAppWidgetId(it) }
            Log.i(TAG, "sweep: the host holds $held; deleted $gone")
        } catch (e: RuntimeException) {
            Log.w(TAG, "sweep failed", e)
        }
    }

    companion object {
        /** Launcher3's host id. */
        const val HOST_ID = 1024
    }
}

/**
 * A widget's view: Android's, with a long press found without taking the widget's taps, as
 * Launcher3's `CheckLongPressHelper` does (Apache-2.0): it watches every touch on its way to the
 * widget, and only once the press has been held still does it take the rest of the gesture, so
 * the widget gets a cancel instead of a click.
 */
class LorWidgetView(context: Context) : AppWidgetHostView(context) {
    /** Opens the widget's menu; null when the menu would be empty. */
    var onLongPress: (() -> Unit)? = null
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var check: Runnable? = null
    private var longPressed = false
    private var downX = 0f
    private var downY = 0f
    private var told: SizeF? = null

    init {
        // TalkBack's long press reaches the menu the same way.
        setOnLongClickListener { onLongPress?.let { it(); true } ?: false }
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        watch(ev)
        return longPressed
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        watch(ev)
        // Keep the gesture, so a press on a part that takes no touch can still be held for the menu.
        return true
    }

    private fun watch(ev: MotionEvent) {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                cancel()
                longPressed = false
                downX = ev.x
                downY = ev.y
                val r = Runnable {
                    check = null
                    val open = onLongPress
                    if (open != null && parent != null && hasWindowFocus()) {
                        longPressed = true
                        performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        open()
                    }
                }
                check = r
                postDelayed(r, (ViewConfiguration.getLongPressTimeout() * LONG_PRESS_FACTOR).toLong())
            }
            MotionEvent.ACTION_MOVE -> if (abs(ev.x - downX) > slop || abs(ev.y - downY) > slop) cancel()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> cancel()
        }
    }

    private fun cancel() {
        check?.let(::removeCallbacks)
        check = null
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        cancel()
    }

    /**
     * Whether a list the widget holds, under ([x], [y]) in this view's px, takes a swipe that would
     * move the grid [way]: it can still scroll that way ([listTakes]).
     */
    fun scrolls(x: Float, y: Float, way: Way): Boolean {
        val r = android.graphics.Rect()
        fun under(v: View): Boolean {
            if (v.visibility != View.VISIBLE) return false
            if (v is AbsListView) {
                v.getDrawingRect(r)
                offsetDescendantRectToMyCoords(v, r)
                if (r.contains(x.toInt(), y.toInt()) && listTakes(way, v.canScrollVertically(1), v.canScrollVertically(-1), reversed = false)) return true
            }
            if (v is ViewGroup) for (i in 0 until v.childCount) if (under(v.getChildAt(i))) return true
            return false
        }
        return under(this)
    }

    /** Tells the widget [id] it is [width] by [height] dp, when that changed (decision lor-9wlx). */
    fun tellSize(id: Int, width: Float, height: Float) {
        val size = SizeF(width, height)
        if (size == told) return
        told = size
        updateAppWidgetSize(Bundle(), listOf(size))
        Log.i(TAG, "size $id: ${width.roundToInt()}x${height.roundToInt()} dp")
    }

    private companion object {
        /** Launcher3's `DEFAULT_LONG_PRESS_TIMEOUT_FACTOR`. */
        const val LONG_PRESS_FACTOR = 0.75f
    }
}

/** The process's widget host. */
@Composable
internal fun rememberWidgets(): WidgetHost = (LocalContext.current.applicationContext as LorApp).home.widgets

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
    val widgets = rememberWidgets()
    // Looked up again when the app list changes, so an app turned off or on shows at once.
    val info = remember(block.id, block.provider, home.apps) { widgets.info(block.id, block.provider) }
    val locked = home.file?.locked == true
    if (info == null) return GoneWidget(block, home, locked)
    val context = LocalContext.current
    val density = LocalDensity.current.density
    fun dp(px: Int) = px / density
    val label = remember(info) { info.loadLabel(context.packageManager) ?: block.provider }
    val reconfigurable = info.configure != null && info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE != 0
    val resizing = ui.resizing == block.id
    val range = WidgetSize.resizeRange(dp(info.minHeight), dp(info.minResizeHeight), dp(info.maxResizeHeight), panelHeight)
    // The frame's height while a finger has its handle; the file's otherwise.
    var dragged by remember { mutableFloatStateOf(block.height.toFloat()) }
    val height = if (resizing) dragged else block.height.toFloat()
    val menu = rememberFloatingMenu(ui)
    val hasMenu = !locked || reconfigurable
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp)) {
        val width = WidgetSize.width(maxWidth.value, dp(info.maxResizeWidth))
        Box(Modifier.align(Alignment.TopCenter).size(width.dp, height.dp)) {
            val view = remember { arrayOfNulls<LorWidgetView>(1) }
            AndroidView(
                factory = { widgets.view(block.id, info).also { v -> view[0] = v; v.contentDescription = label } },
                modifier = Modifier.fillMaxSize().scrollsInGridAt { at, way -> view[0]?.scrolls(at.x, at.y, way) == true },
                update = { v ->
                    v.onLongPress = if (hasMenu) ({ menu.open = true }) else null
                    v.tellSize(block.id, width, block.height.toFloat())
                },
            )
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
                DropdownMenu(true, { menu.open = false }, containerColor = Raised, shape = RectangleShape) {
                    if (!locked) Item("Resize") {
                        menu.open = false
                        dragged = block.height.toFloat()
                        ui.resizing = block.id
                    }
                    if (reconfigurable) Item("Settings") {
                        menu.open = false
                        home.actions.widgetSettings(block.id)
                    }
                    if (!locked) Item("Remove") {
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
    val context = LocalContext.current
    val app = remember(block.provider) { appLabel(context, block.provider) }
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

/** The label of [provider]'s app, a disabled one too; its package name when it isn't installed. */
private fun appLabel(context: Context, provider: String): String {
    val pkg = ComponentName.unflattenFromString(provider)?.packageName ?: return provider
    val pm = context.packageManager
    return try {
        pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS).loadLabel(pm).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }
}

/** A widget the picker offers: its provider, label, preview (else its app's icon) and minimum size in dp. */
private class PickerWidget(val info: AppWidgetProviderInfo, val label: String, val image: ImageBitmap?, val minWidth: Int, val minHeight: Int)

/** An app's widgets in the picker. */
private class PickerApp(val label: String, val icon: ImageBitmap?, val widgets: List<PickerWidget>)

/**
 * The widget picker (lor-r0f9): a sheet over home (DESIGN.md's Elevation & Depth: a framed face
 * over a dim, with the float shadow) of the main profile's widget providers, grouped by app. A
 * choice closes it and adds the widget to the stack at [at] ([HomeActions.addWidget]).
 */
@Composable
internal fun WidgetPicker(at: Cell, home: HomeContext, close: () -> Unit) {
    BackHandler(onBack = close)
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val apps by produceState<List<PickerApp>?>(null, context) { value = withContext(Dispatchers.IO) { providers(context, density) } }
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
                            item(key = "app:${app.widgets.first().info.provider.packageName}") {
                                Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    app.icon?.let { Image(it, contentDescription = null, modifier = Modifier.size(22.dp)) }
                                    Text(app.label.uppercase(), style = Type.label, modifier = Modifier.padding(start = 10.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                            items(app.widgets, key = { it.info.provider.flattenToString() }) { w ->
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

/** The main profile's widget providers, by app label, each app's by widget label. Off the main thread. */
private fun providers(context: Context, density: Float): List<PickerApp> {
    val pm = context.packageManager
    val manager = AppWidgetManager.getInstance(context)
    val w = (PREVIEW_W * density).roundToInt()
    val h = (PREVIEW_H * density).roundToInt()
    return manager.getInstalledProvidersForProfile(Process.myUserHandle()).groupBy { it.provider.packageName }.mapNotNull { (pkg, infos) ->
        val app = try {
            pm.getApplicationInfo(pkg, 0)
        } catch (e: PackageManager.NameNotFoundException) {
            return@mapNotNull null
        }
        val icon = runCatching { fit(pm.getApplicationIcon(app), w, h) }.getOrNull()
        val widgets = infos.map { info ->
            val preview = runCatching { info.loadPreviewImage(context, 0)?.let { fit(it, w, h) } }.getOrNull()
            PickerWidget(info, info.loadLabel(pm) ?: info.provider.className, preview ?: icon, (info.minWidth / density).roundToInt(), (info.minHeight / density).roundToInt())
        }.sortedBy { it.label.lowercase() }
        PickerApp(app.loadLabel(pm).toString(), icon, widgets)
    }.sortedBy { it.label.lowercase() }
}

/** [d] drawn to fit [w] by [h] px, keeping its shape. */
private fun fit(d: Drawable, w: Int, h: Int): ImageBitmap {
    val iw = d.intrinsicWidth.takeIf { it > 0 } ?: w
    val ih = d.intrinsicHeight.takeIf { it > 0 } ?: h
    val scale = minOf(w.toFloat() / iw, h.toFloat() / ih)
    return d.toBitmap((iw * scale).roundToInt().coerceAtLeast(1), (ih * scale).roundToInt().coerceAtLeast(1)).asImageBitmap()
}
