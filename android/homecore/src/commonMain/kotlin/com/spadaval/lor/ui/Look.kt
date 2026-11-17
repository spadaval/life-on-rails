package com.spadaval.lor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextMotion
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/*
 * The look (launcher polish 6.1): DESIGN.md ("Elite: Mostly Harmless") as Compose colours, type
 * and Material theme, so every screen of the app draws from one place. The colours' values are
 * in [Palette]; their jobs are DESIGN.md's.
 *
 * Fonts (OFL-1.1, licence in the app's assets/fonts/OFL.txt), from github.com/google/fonts, in
 * this module's androidMain/res/font:
 * - saira.ttf: ofl/saira/Saira[wdth,wght].ttf, the one file upstream has, drawn at 400,
 *   500 and 600 by its weight axis;
 * - saira_condensed_medium.ttf and saira_condensed_semibold.ttf:
 *   ofl/sairacondensed/SairaCondensed-Medium.ttf and -SemiBold.ttf.
 */

val Bg = Color(Palette.BG)
val Well = Color(Palette.WELL)
val PanelFace = Color(Palette.PANEL)
val Raised = Color(Palette.RAISED)
val Line = Color(Palette.LINE)
val Edge = Color(Palette.EDGE)
val Frame = Color(Palette.FRAME)
val Txt = Color(Palette.TXT)
val Mute = Color(Palette.MUTE)
val Dim = Color(Palette.DIM)
val Or = Color(Palette.OR)
val OrDeep = Color(Palette.OR_DEEP)
val Cy = Color(Palette.CY)
val Bad = Color(Palette.BAD)
val OnOr = Color(Palette.ON_OR)

/** The backing of a card drawn over the wallpaper or a page (a state on home, an agent card). */
val StateFill = PanelFace.copy(alpha = 0xDD / 255f)
/** The scrim behind a sheet (quick send). An open folder has its own glass (lor-gnk0). */
val Scrim = Color(0x99000000)
/**
 * The agent page's veil over the wallpaper (launcher polish 6.3): strong enough that Far Dust on it
 * reads at 4.5:1 over white, the worst wallpaper, so it needn't follow the wallpaper.
 */
val Veil = Bg.copy(alpha = Palette.VEIL_ALPHA.toFloat())

/** DESIGN.md's one silhouette: the top-left and bottom-right corners clipped at 6 dp, for outer panels. */
val Chamfer: Shape = CutCornerShape(topStart = 6.dp, topEnd = 0.dp, bottomEnd = 6.dp, bottomStart = 0.dp)

/** An outer panel (DESIGN.md, Cards): [face] in the chamfer, held in a 1 dp Frame Bronze edge. */
fun Modifier.framedPanel(face: Color = PanelFace): Modifier = background(face, Chamfer).border(1.dp, Frame, Chamfer)

/** A dialog's dismiss action: DESIGN.md's ordinary button text (Starlight), so only the confirming action is amber. */
val DismissColors: androidx.compose.material3.ButtonColors
    @Composable get() = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = Txt)

/**
 * DESIGN.md's motion (launcher polish 7.1): one-shot, and it follows the finger. Nothing loops.
 * Android's animator duration scale (and "Remove animations") reaches these through Compose's own
 * MotionDurationScale, so at 0 each ends at once.
 */
object Motion {
    /** Fades and feedback (DESIGN.md's 150 ms colour transition). */
    const val QUICK = 150

    /** A thing taking its new place: a tile, a folder, a settling icon. */
    const val MOVE = 220

    fun <T> quick(): androidx.compose.animation.core.TweenSpec<T> = androidx.compose.animation.core.tween(QUICK, easing = androidx.compose.animation.core.FastOutSlowInEasing)
    fun <T> move(): androidx.compose.animation.core.TweenSpec<T> = androidx.compose.animation.core.tween(MOVE, easing = androidx.compose.animation.core.FastOutSlowInEasing)

    /** Whatever a finger let go of: no bounce, started from the finger's velocity by its caller. */
    fun <T> settle(): androidx.compose.animation.core.SpringSpec<T> =
        androidx.compose.animation.core.spring(dampingRatio = 1f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
}

/** DESIGN.md's selection: an 8% amber fill with a 1 dp amber edge at 55%. */
fun Modifier.selection(shape: Shape = RectangleShape): Modifier =
    background(Or.copy(alpha = 0.08f), shape).border(1.dp, Or.copy(alpha = 0.55f), shape)

/**
 * Saira at 400, 500 and 600 by its weight axis, and Saira Condensed Medium and SemiBold. Each
 * platform builds them from the same files: Android from this module's res/font, the lab from
 * those files copied into its bundle and loaded before anything draws.
 */
expect val Saira: FontFamily
expect val SairaCondensed: FontFamily

private const val TNUM = "tnum"

/** DESIGN.md's type scale; px become sp. Condensed caps are for titles, labels and readouts: callers uppercase the text. */
object Type {
    val display = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.SemiBold, fontSize = 56.sp, lineHeight = 56.sp, letterSpacing = 0.03.em, fontFeatureSettings = TNUM, color = Txt, textMotion = TextMotion.Animated)
    val headline = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.08.em, fontFeatureSettings = TNUM, color = Txt, textMotion = TextMotion.Animated)
    val title = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.07.em, fontFeatureSettings = TNUM, color = Mute, textMotion = TextMotion.Animated)
    val body = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal, fontSize = 15.sp, fontFeatureSettings = TNUM, color = Txt, textMotion = TextMotion.Animated)
    val bodySmall = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal, fontSize = 13.sp, fontFeatureSettings = TNUM, color = Mute, textMotion = TextMotion.Animated)
    val label = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, letterSpacing = 0.07.em, fontFeatureSettings = TNUM, color = Dim, textMotion = TextMotion.Animated)
    val button = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, fontFeatureSettings = TNUM, color = Txt, textMotion = TextMotion.Animated)
}

private val Square = RoundedCornerShape(0.dp)

/**
 * DESIGN.md's selection tint over Panel Face, as an opaque colour. Material's primary container:
 * it must differ from every surface colour, or `contentColorFor` takes a Raised card for a primary
 * container and draws its text amber.
 */
private val SelectedFill = Color(Palette.over(Palette.OR, 0.08, Palette.PANEL))

private val Scheme = darkColorScheme(
    primary = Or, onPrimary = OnOr, primaryContainer = SelectedFill, onPrimaryContainer = Txt, inversePrimary = OrDeep,
    secondary = Mute, onSecondary = Bg, secondaryContainer = Raised, onSecondaryContainer = Txt,
    tertiary = Cy, onTertiary = Bg, tertiaryContainer = Raised, onTertiaryContainer = Cy,
    background = Bg, onBackground = Txt,
    surface = PanelFace, onSurface = Txt, surfaceVariant = Raised, onSurfaceVariant = Mute, surfaceTint = Color.Transparent,
    inverseSurface = Txt, inverseOnSurface = Bg,
    error = Bad, onError = Bg, errorContainer = Raised, onErrorContainer = Bad,
    outline = Edge, outlineVariant = Line, scrim = Color.Black,
    surfaceBright = Raised, surfaceDim = Bg,
    surfaceContainerLowest = Well, surfaceContainerLow = PanelFace, surfaceContainer = PanelFace,
    surfaceContainerHigh = Raised, surfaceContainerHighest = Raised,
)

// Lazy, so a test that reads only a colour of this file reads no font (the browser's are loaded by the lab).
private val Types by lazy {
    Typography(
    displayLarge = Type.display, displayMedium = Type.display.copy(fontSize = 45.sp, lineHeight = 45.sp), displaySmall = Type.display.copy(fontSize = 36.sp, lineHeight = 36.sp),
    headlineLarge = Type.headline.copy(fontSize = 28.sp), headlineMedium = Type.headline.copy(fontSize = 25.sp), headlineSmall = Type.headline,
    titleLarge = Type.headline.copy(fontSize = 19.sp), titleMedium = Type.title.copy(color = Color.Unspecified, fontSize = 16.sp), titleSmall = Type.title.copy(color = Color.Unspecified),
    bodyLarge = Type.body.copy(color = Color.Unspecified), bodyMedium = Type.body.copy(color = Color.Unspecified, fontSize = 14.sp), bodySmall = Type.bodySmall.copy(color = Color.Unspecified),
    labelLarge = Type.button.copy(color = Color.Unspecified), labelMedium = Type.label.copy(color = Color.Unspecified), labelSmall = Type.label.copy(color = Color.Unspecified, fontSize = 12.sp),
).let { t -> t.copy(headlineSmall = t.headlineSmall.copy(color = Color.Unspecified), headlineMedium = t.headlineMedium.copy(color = Color.Unspecified), headlineLarge = t.headlineLarge.copy(color = Color.Unspecified), titleLarge = t.titleLarge.copy(color = Color.Unspecified), displayLarge = t.displayLarge.copy(color = Color.Unspecified), displayMedium = t.displayMedium.copy(color = Color.Unspecified), displaySmall = t.displaySmall.copy(color = Color.Unspecified)) }
}

/** The app's one theme: DESIGN.md's dark scheme, square shapes and Saira, whatever the system's light or dark. */
@Composable
fun LorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, shapes = Shapes(Square, Square, Square, Square, Square), typography = Types, content = content)
}
