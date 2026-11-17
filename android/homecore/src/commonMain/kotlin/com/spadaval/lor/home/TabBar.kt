package com.spadaval.lor.home

import com.spadaval.lor.lab.labAnchor
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spadaval.lor.ui.Chamfer
import com.spadaval.lor.ui.Dim
import com.spadaval.lor.ui.Edge
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.Raised
import com.spadaval.lor.ui.Txt
import com.spadaval.lor.ui.Type
import com.spadaval.lor.ui.Well

/*
 * The drawer's floating controls (lor-iyc3), after the launcher-use mocks: a chamfered bar of tab
 * icons, the current one amber with its underline, and beside it a search button that grows into
 * the field. Both float over the list, which scrolls under them.
 */

/** The height of the bar, the search button and the field. */
internal val BarHeight = BarSize.SEARCH.dp

/** Floating things' face: Raised, a little see-through, as the mock's 86% (DESIGN.md: menus float). */
private val Float = Raised.copy(alpha = 0.92f)

/** The bar of one icon per tab, each [tabWidth] wide ([tabWidth] in Drawer.kt fits them beside search). */
@Composable
internal fun TabBar(tabs: List<DrawerTab>, current: Int, tabWidth: Dp, choose: (Int) -> Unit) {
    Row(
        Modifier
            .shadow(10.dp, Chamfer, ambientColor = Color.Black, spotColor = Color.Black)
            .background(Float, Chamfer)
            .border(1.dp, Edge, Chamfer)
            .padding((BarSize.PADDING / 2).dp),
    ) {
        tabs.forEachIndexed { i, t ->
            val on = i == current
            val color = if (on) Or else Dim
            Box(
                Modifier
                    .width(tabWidth)
                    .height(BarHeight - 6.dp)
                    .background(if (on) Or.copy(alpha = 0.1f) else Color.Transparent)
                    .semantics { role = Role.Tab; selected = on; contentDescription = t.name }
                    .clickable { choose(i) },
                contentAlignment = Alignment.Center,
            ) {
                TabGlyph(t, color)
                if (on) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 3.dp).width(20.dp).height(2.dp).background(Or))
            }
        }
    }
}

/** A tab's icon, or its name's first letter when the file names an icon the phone doesn't draw. */
@Composable
internal fun TabGlyph(tab: DrawerTab, color: Color) = TabGlyph(tab.name, tab.icon, color)

/** The search button: the bar's look, a magnifier in Starlight. */
@Composable
internal fun SearchButton(open: () -> Unit) {
    Box(
        Modifier
            .size(BarHeight)
            .shadow(10.dp, RectangleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(Float)
            .border(1.dp, Edge)
            .semantics { contentDescription = "Search" }
            .labAnchor("drawer-search")
            .clickable(role = Role.Button, onClick = open),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) { drawSearch(Txt) }
    }
}

/**
 * The search field the button grows into, [width] wide: a magnifier, the text, and ✕, which folds
 * it back ([close]). DESIGN.md's field: amber at 70% while it has focus.
 */
@Composable
internal fun SearchField(
    width: Dp,
    value: TextFieldValue,
    change: (TextFieldValue) -> Unit,
    focus: FocusRequester,
    focused: (Boolean) -> Unit,
    go: () -> Unit,
    close: () -> Unit,
) {
    Row(
        Modifier
            .width(width)
            .height(BarHeight)
            .shadow(10.dp, RectangleShape, ambientColor = Color.Black, spotColor = Color.Black)
            .background(Well.copy(alpha = 0.95f))
            .border(1.dp, Or.copy(alpha = 0.7f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(12.dp))
        Canvas(Modifier.size(18.dp)) { drawSearch(Dim) }
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.text.isEmpty()) Text("Search", style = Type.body.copy(color = Dim), maxLines = 1)
            BasicTextField(
                value,
                change,
                Modifier.fillMaxWidth().labAnchor("search-field").focusRequester(focus).onFocusChanged { focused(it.isFocused) },
                textStyle = Type.body.copy(color = Txt),
                singleLine = true,
                cursorBrush = SolidColor(Txt),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { go() }),
            )
        }
        Box(
            Modifier
                .fillMaxHeight()
                .width(BarHeight)
                .semantics { contentDescription = "Close search" }
                .clickable(role = Role.Button, onClick = close),
            contentAlignment = Alignment.Center,
        ) {
            Text("✕", style = Type.body.copy(color = Dim, fontSize = 15.sp))
        }
    }
}

/** The magnifier, in a 24-unit square scaled to this canvas. */
private fun DrawScope.drawSearch(color: Color) {
    val u = size.minDimension / 24f
    drawCircle(color, 6.5f * u, Offset(10.5f * u, 10.5f * u), style = Stroke(2f * u))
    drawLine(color, Offset(15.5f * u, 15.5f * u), Offset(21f * u, 21f * u), 2f * u)
}
