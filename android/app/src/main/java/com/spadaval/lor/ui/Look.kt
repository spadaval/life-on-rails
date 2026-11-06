package com.spadaval.lor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.spadaval.lor.R

/*
 * The look (launcher polish 6.1): DESIGN.md ("Elite: Mostly Harmless") as Compose colours, type
 * and Material theme, so every screen of the app draws from one place. The colours' values are
 * in [Palette]; their jobs are DESIGN.md's.
 *
 * Fonts (OFL-1.1, licence in assets/fonts/OFL.txt), from github.com/google/fonts:
 * - res/font/saira.ttf: ofl/saira/Saira[wdth,wght].ttf, the one file upstream has, drawn at 400,
 *   500 and 600 by its weight axis;
 * - res/font/saira_condensed_medium.ttf and saira_condensed_semibold.ttf:
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
/** The scrim behind an open folder or sheet. */
val Scrim = Color(0x99000000)
/** The agent page's veil over the wallpaper. */
val Veil = Bg.copy(alpha = 0xD9 / 255f)

// Colours the agent page uses that have no job in DESIGN.md; 6.2 replaces them.
val TempGreen = Color(0xFF84B98F)
val TempBlue = Color(0xFF82A7E2)
val TempT3Tile = Color(0xFF3B3F8F)
val TempLorTile = Color(0xFF2C4A3A)

/** DESIGN.md's selection: an 8% amber fill with a 1 dp amber edge at 55%. */
fun Modifier.selection(shape: Shape = RectangleShape): Modifier =
    background(Or.copy(alpha = 0.08f), shape).border(1.dp, Or.copy(alpha = 0.55f), shape)

@OptIn(ExperimentalTextApi::class)
private fun saira(weight: Int) = Font(R.font.saira, FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))

@OptIn(ExperimentalTextApi::class)
val Saira = FontFamily(saira(400), saira(500), saira(600))
val SairaCondensed = FontFamily(
    Font(R.font.saira_condensed_medium, FontWeight.Medium),
    Font(R.font.saira_condensed_semibold, FontWeight.SemiBold),
)

private const val TNUM = "tnum"

/** DESIGN.md's type scale; px become sp. Condensed caps are for titles, labels and readouts: callers uppercase the text. */
object Type {
    val display = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.SemiBold, fontSize = 56.sp, lineHeight = 56.sp, letterSpacing = 0.03.em, fontFeatureSettings = TNUM, color = Txt)
    val headline = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = 0.08.em, fontFeatureSettings = TNUM, color = Txt)
    val title = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, letterSpacing = 0.07.em, fontFeatureSettings = TNUM, color = Mute)
    val body = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal, fontSize = 15.sp, fontFeatureSettings = TNUM, color = Txt)
    val bodySmall = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Normal, fontSize = 13.sp, fontFeatureSettings = TNUM, color = Mute)
    val label = TextStyle(fontFamily = SairaCondensed, fontWeight = FontWeight.Medium, fontSize = 12.5.sp, letterSpacing = 0.07.em, fontFeatureSettings = TNUM, color = Dim)
    val button = TextStyle(fontFamily = Saira, fontWeight = FontWeight.Medium, fontSize = 13.5.sp, fontFeatureSettings = TNUM, color = Txt)
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

private val Types = Typography(
    displayLarge = Type.display, displayMedium = Type.display.copy(fontSize = 45.sp, lineHeight = 45.sp), displaySmall = Type.display.copy(fontSize = 36.sp, lineHeight = 36.sp),
    headlineLarge = Type.headline.copy(fontSize = 28.sp), headlineMedium = Type.headline.copy(fontSize = 25.sp), headlineSmall = Type.headline,
    titleLarge = Type.headline.copy(fontSize = 19.sp), titleMedium = Type.title.copy(color = Color.Unspecified, fontSize = 16.sp), titleSmall = Type.title.copy(color = Color.Unspecified),
    bodyLarge = Type.body.copy(color = Color.Unspecified), bodyMedium = Type.body.copy(color = Color.Unspecified, fontSize = 14.sp), bodySmall = Type.bodySmall.copy(color = Color.Unspecified),
    labelLarge = Type.button.copy(color = Color.Unspecified), labelMedium = Type.label.copy(color = Color.Unspecified), labelSmall = Type.label.copy(color = Color.Unspecified, fontSize = 12.sp),
).let { t -> t.copy(headlineSmall = t.headlineSmall.copy(color = Color.Unspecified), headlineMedium = t.headlineMedium.copy(color = Color.Unspecified), headlineLarge = t.headlineLarge.copy(color = Color.Unspecified), titleLarge = t.titleLarge.copy(color = Color.Unspecified), displayLarge = t.displayLarge.copy(color = Color.Unspecified), displayMedium = t.displayMedium.copy(color = Color.Unspecified), displaySmall = t.displaySmall.copy(color = Color.Unspecified)) }

/** The app's one theme: DESIGN.md's dark scheme, square shapes and Saira, whatever the system's light or dark. */
@Composable
fun LorTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, shapes = Shapes(Square, Square, Square, Square, Square), typography = Types, content = content)
}
