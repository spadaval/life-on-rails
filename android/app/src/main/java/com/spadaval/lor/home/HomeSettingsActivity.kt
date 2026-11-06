package com.spadaval.lor.home

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.spadaval.lor.LorApp
import com.spadaval.lor.MainActivity
import com.spadaval.lor.ui.LorTheme
import kotlinx.coroutines.launch

/**
 * Home settings (launcher polish 1.3): the launcher's own settings, opened from the home menu and
 * the drawer's "Hidden apps" row. It starts in home's task, so back returns to home. It reads and
 * writes through [HomeStore], as home does. Later increments add their settings here, not to the
 * LoR tabs.
 */
class HomeSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val store = (application as LorApp).home
        val icons = Icons((40 * resources.displayMetrics.density).toInt())
        setContent {
            val apps by store.apps.collectAsStateWithLifecycle()
            val layout by store.layout.collectAsStateWithLifecycle()
            LorTheme {
                CompositionLocalProvider(LocalIcons provides icons) {
                    Surface(Modifier.fillMaxSize()) {
                        HomeSettings(apps, layout, ::edit, ::open)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as LorApp).home.resume()
    }

    /** Applies [change] to `layout.json`; [done] is said on success, a refusal says why. */
    private fun edit(done: String?, change: (LayoutFile) -> LayoutFile) {
        lifecycleScope.launch {
            (application as LorApp).home.edit(change)
                .onSuccess { if (done != null) toast(done) }
                .onFailure { toast("Can't: ${it.message}.") }
        }
    }

    private fun open(intent: Intent) {
        try {
            startActivity(intent)
        } catch (e: RuntimeException) {
            toast("That screen can't be opened.")
        }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_SHORT).show()

    companion object {
        fun open(context: Context) = context.startActivity(Intent(context, HomeSettingsActivity::class.java))
    }
}

/** A hidden entry: the installed app it names, if any (the first activity, for a package name). */
private class Hidden(val ref: AppRef, val app: HomeApp?) {
    val label: String get() = app?.label ?: ref.toString()
}

@Composable
private fun HomeSettings(apps: List<HomeApp>, layout: LayoutState, edit: (String?, (LayoutFile) -> LayoutFile) -> Unit, open: (Intent) -> Unit) {
    val file = (layout as? LayoutState.Ready)?.file
    val hidden = remember(apps, file) {
        file?.hidden.orEmpty().map { ref ->
            Hidden(ref, apps.firstOrNull { it.mainProfile && ref.matches(it.packageName, it.id.className) })
        }
    }
    LazyColumn(Modifier.fillMaxSize().safeDrawingPadding()) {
        item {
            Text("Home settings", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(20.dp, 24.dp, 20.dp, 8.dp))
        }
        item { Section("Hidden apps") }
        when {
            file == null -> item {
                Note(if (layout is LayoutState.Broken) "layout.json can't be used: ${layout.message}" else "Loading…")
            }
            hidden.isEmpty() -> item { Note("None. Hide an app from its long-press menu.") }
            else -> items(hidden, key = { it.ref.toString() }) { h ->
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (h.app != null) AppIcon(h.app, 40.dp) else Spacer(Modifier.size(40.dp))
                    Spacer(Modifier.width(16.dp))
                    Text(h.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    TextButton(onClick = { edit("${h.label} is back in the drawer.") { it.unhide(h.ref.packageName) } }) { Text("Unhide") }
                }
            }
        }
        if (file != null) {
            item { Section("Drawer sections") }
            val all = file.drawer.allSections()
            // Keyed by place: names are unique, but a crash here would lock the edits away.
            items(all.size, key = { "section:$it" }) { i ->
                val s = all[i]
                SectionRow(file, s, first = i == 0, last = i == all.lastIndex, edit)
            }
            item {
                var naming by remember { mutableStateOf(false) }
                TextButton(onClick = { naming = true }, modifier = Modifier.padding(horizontal = 8.dp)) { Text("New section") }
                if (naming) NameDialog("New section", "") { name ->
                    naming = false
                    if (name != null) edit("$name is the last section.") { it.newSection(name) }
                }
            }
        }
        item { HorizontalDivider(Modifier.padding(top = 12.dp)) }
        item {
            Link("Default home app", "Choose which app is home") { open(Intent(Settings.ACTION_HOME_SETTINGS)) }
        }
        item {
            val context = LocalContext.current
            Link("LoR app settings", "Timer, sensors, server and T3") { open(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(20.dp, 16.dp, 20.dp, 8.dp),
    )
}

@Composable
private fun Note(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
    )
}

@Composable
private fun Link(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** One drawer section, with its edits behind a menu (launcher polish 2.3). */
@Composable
private fun SectionRow(file: LayoutFile, s: DrawerSection, first: Boolean, last: Boolean, edit: (String?, (LayoutFile) -> LayoutFile) -> Unit) {
    val inFile = file.drawer.sections.any { it.name == s.name }
    var menu by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    val about = listOfNotNull(
        s.category?.let { "Takes ${it.title}" } ?: "No category",
        s.apps.size.takeIf { it > 0 }?.let { "$it put here" },
        "hidden".takeIf { s.hidden },
    ).joinToString(" · ")
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 2.dp, bottom = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(s.name, style = MaterialTheme.typography.bodyLarge)
            Text(about, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box {
            TextButton(onClick = { menu = true }) { Text("Edit") }
            DropdownMenu(menu, { menu = false }) {
                DropdownMenuItem(text = { Text("Rename…") }, onClick = { menu = false; renaming = true })
                DropdownMenuItem(text = { Text("Category…") }, onClick = { menu = false; picking = true })
                if (!first) DropdownMenuItem(text = { Text("Move up") }, onClick = { menu = false; edit(null) { it.moveSection(s.name, -1) } })
                if (!last) DropdownMenuItem(text = { Text("Move down") }, onClick = { menu = false; edit(null) { it.moveSection(s.name, 1) } })
                DropdownMenuItem(
                    text = { Text(if (s.hidden) "Show in the drawer" else "Hide from the drawer") },
                    onClick = { menu = false; edit(null) { it.setSectionHidden(s.name, !s.hidden) } },
                )
                if (inFile) DropdownMenuItem(text = { Text("Delete") }, onClick = { menu = false; edit("${s.name} is deleted.") { it.deleteSection(s.name) } })
            }
        }
    }
    if (renaming) NameDialog("Rename ${s.name}", s.name) { name ->
        renaming = false
        if (name != null && name != s.name) edit(null) { it.renameSection(s.name, name) }
    }
    if (picking) {
        // The categories it may take: none, its own, or one no other section in the file takes
        // (for a section not in the file yet, one no other section in the drawer takes).
        val others = (if (inFile) file.drawer.sections else file.drawer.allSections()).filter { it.name != s.name }
        val free = listOf<Category?>(null) + Category.entries.filter { c -> others.none { it.category == c } }
        AlertDialog(
            onDismissRequest = { picking = false },
            confirmButton = { TextButton(onClick = { picking = false }) { Text("Cancel") } },
            title = { Text("${s.name} takes") },
            text = {
                Column {
                    free.forEach { c ->
                        Row(
                            Modifier.fillMaxWidth().clickable {
                                picking = false
                                if (c != s.category) edit(null) { it.setSectionCategory(s.name, c) }
                            }.padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = c == s.category, onClick = null)
                            Spacer(Modifier.width(12.dp))
                            Text(c?.title ?: "No category (only the apps put here)")
                        }
                    }
                }
            },
        )
    }
}

/** Asks for a section's name; [done] gets it, or null when cancelled. */
@Composable
private fun NameDialog(title: String, initial: String, done: (String?) -> Unit) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = { done(null) },
        confirmButton = { TextButton(onClick = { done(text.trim()) }, enabled = text.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = { done(null) }) { Text("Cancel") } },
        title = { Text(title) },
        text = { OutlinedTextField(text, { text = it }, singleLine = true, label = { Text("Name") }) },
    )
}
