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
 * The Android half of Widgets.kt (lor-r0f9): the process's widget host, a widget's own view, and
 * what the picker and the block read from Android. The block, its menu and resize frame, the note
 * for a gone widget and the picker sheet are drawn in homecore. Borrowed as Widgets.kt says.
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
    /** Where in the window the finger held for the menu; null when TalkBack opened it. */
    var pressedAt: Bounds? = null
        private set
    private val slop = ViewConfiguration.get(context).scaledTouchSlop
    private var check: Runnable? = null
    private var longPressed = false
    private var downX = 0f
    private var downY = 0f
    private var told: SizeF? = null

    init {
        // TalkBack's long press reaches the menu the same way.
        setOnLongClickListener { onLongPress?.let { pressedAt = null; it(); true } ?: false }
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
                        val at = IntArray(2).also(::getLocationInWindow)
                        pressedAt = Bounds(at[0] + downX, at[1] + downY, at[0] + downX, at[1] + downY)
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


/** A bound widget's facts, with Android's record of its provider, which its view needs. */
class LauncherWidgetFacts(val info: AppWidgetProviderInfo, label: String) : WidgetFacts(
    label, info.minHeight, info.minResizeHeight, info.maxResizeHeight, info.maxResizeWidth,
    reconfigurable = info.configure != null && info.widgetFeatures and AppWidgetProviderInfo.WIDGET_FEATURE_RECONFIGURABLE != 0,
)

/** A [WidgetProvider] on the phone, with Android's record of it. */
class LauncherWidgetProvider(val info: AppWidgetProviderInfo) : WidgetProvider(info.provider.flattenToString(), info.provider.packageName)

/** The widget's own view, as [WidgetBlock] used to draw it in place. */
@Composable
internal fun LauncherWidgetView(widgets: WidgetHost, block: StackBlock.Widget, facts: WidgetFacts, widthDp: Float, onLongPress: (() -> Unit)?, press: WidgetPress, modifier: Modifier) {
    val info = (facts as LauncherWidgetFacts).info
    val view = remember { arrayOfNulls<LorWidgetView>(1) }
    press.at = { view[0]?.pressedAt }
    AndroidView(
        factory = { widgets.view(block.id, info).also { v -> view[0] = v; v.contentDescription = facts.label } },
        modifier = modifier.scrollsInGridAt { at, way -> view[0]?.scrolls(at.x, at.y, way) == true },
        update = { v ->
            v.onLongPress = onLongPress
            v.tellSize(block.id, widthDp, block.height.toFloat())
        },
    )
}

/** The label of [provider]'s app, a disabled one too; its package name when it isn't installed. */
internal fun appLabel(context: Context, provider: String): String {
    val pkg = ComponentName.unflattenFromString(provider)?.packageName ?: return provider
    val pm = context.packageManager
    return try {
        pm.getApplicationInfo(pkg, PackageManager.MATCH_DISABLED_COMPONENTS).loadLabel(pm).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    }
}


private const val PREVIEW_W = 96
private const val PREVIEW_H = 72

/** The main profile's widget providers, by app label, each app's by widget label. Off the main thread. */
internal fun providers(context: Context, density: Float): List<PickerApp> {
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
            PickerWidget(LauncherWidgetProvider(info), info.loadLabel(pm) ?: info.provider.className, preview ?: icon, (info.minWidth / density).roundToInt(), (info.minHeight / density).roundToInt())
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
