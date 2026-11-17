package com.spadaval.lor.concepts.kit

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.spadaval.lor.lab.labAnchor
import kotlinx.coroutines.delay

/*
 * The store's notice at the foot of the screen: what just happened, with Undo when the change can
 * be taken back. It leaves after six seconds, or when another takes its place. Anchors: `toast`,
 * `toast:undo`.
 */
@Composable
fun ToastHost(store: ConceptStore, modifier: Modifier = Modifier) {
    val look = LocalKitLook.current
    val toast = store.toast
    LaunchedEffect(toast?.id) {
        val id = toast?.id ?: return@LaunchedEffect
        delay(6000)
        store.dismissToast(id)
    }
    AnimatedContent(
        toast,
        modifier,
        transitionSpec = { (slideInVertically(tween(200)) { it / 2 } + fadeIn(tween(200))) togetherWith (slideOutVertically(tween(160)) { it / 2 } + fadeOut(tween(160))) },
        contentKey = { it?.id },
    ) { t ->
        if (t == null) { Box(Modifier.fillMaxWidth()); return@AnimatedContent }
        val shape = RoundedCornerShape(look.radius * 0.75f)
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Row(
                Modifier.padding(horizontal = 16.dp).widthIn(max = 480.dp).fillMaxWidth()
                    .clip(shape).background(look.glass).border(1.dp, look.glassLine, shape)
                    .heightIn(min = 52.dp).padding(start = 18.dp, end = 6.dp).labAnchor("toast"),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                BasicText(t.text, Modifier.weight(1f).padding(vertical = 12.dp), style = look.body.copy(color = look.text), maxLines = 2, overflow = TextOverflow.Ellipsis)
                val undo = t.undo
                if (undo != null) {
                    Spacer(Modifier.padding(4.dp))
                    BasicText(
                        "Undo",
                        Modifier.clip(RoundedCornerShape(8.dp)).clickable { undo() }.padding(horizontal = 14.dp, vertical = 12.dp).labAnchor("toast:undo"),
                        style = look.body.copy(color = look.accent, fontWeight = FontWeight.SemiBold),
                    )
                }
            }
        }
    }
}
