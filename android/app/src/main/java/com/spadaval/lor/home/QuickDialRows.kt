package com.spadaval.lor.home

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.spadaval.lor.ui.Bg
import com.spadaval.lor.ui.Dim
import com.spadaval.lor.ui.Mute
import com.spadaval.lor.ui.Or
import com.spadaval.lor.ui.Raised
import com.spadaval.lor.ui.Txt
import com.spadaval.lor.ui.Type
import com.spadaval.lor.ui.framedPanel

/*
 * Quick dial's own rows (lor-2imw), after the right frame of
 * docs/missions/launcher-use/mocks/home-quick-dial.html: the card for what's next from LoR and the
 * people row. The apps and New are the drawer's tiles (DrawerScreen.kt).
 */

/** `/device/home`'s one thing: its kicker over its text in a framed panel, greyed with its time when LoR can't be read now. */
@Composable
internal fun QuickCardView(card: QuickCard, onClick: () -> Unit) {
    val stale = card.stale != null
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .framedPanel(Raised)
            .clickable(role = Role.Button, onClickLabel = "Open calendar", onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(card.kicker.uppercase(), style = Type.label.copy(color = if (stale) Dim else Or, fontSize = 11.sp, letterSpacing = 0.1.em), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
            card.stale?.let { Text(" · $it", style = Type.body.copy(color = Dim, fontSize = 11.sp), maxLines = 1) }
        }
        Text(card.text, style = Type.body.copy(color = if (stale) Mute else Txt, fontSize = 14.sp), maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

/** The people row: each person's avatar (the conversation's icon) with its app's badge, and name; a tap opens the conversation. */
@Composable
internal fun PeopleRow(people: List<SearchShortcut>, at: At, home: HomeContext) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp), horizontalArrangement = Arrangement.SpaceAround) {
        people.forEach { p ->
            val spot = remember(p.hit.info.id, p.owner.key) { IconSpot() }
            Column(
                Modifier
                    .width(76.dp)
                    .clickable(role = Role.Button) { home.actions.openShortcut(p.owner, p.hit.info, at, spot) }
                    .semantics(mergeDescendants = true) { contentDescription = "${p.hit.label}, ${p.owner.label}" }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box {
                    ShortcutIcon(p, home, Modifier.clip(CircleShape).iconSpot(spot), size = 48.dp)
                    // The app the conversation is in, as the mock's badge.
                    Box(Modifier.align(Alignment.BottomEnd).offset(3.dp, 3.dp).clip(CircleShape).border(2.dp, Bg, CircleShape)) {
                        AppIcon(p.owner, 18.dp)
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(p.hit.label, style = Type.body.copy(color = Txt, fontSize = 12.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
