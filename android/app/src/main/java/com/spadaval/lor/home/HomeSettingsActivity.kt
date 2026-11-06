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
                        HomeSettings(apps, layout, ::unhide, ::open)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        (application as LorApp).home.resume()
    }

    private fun unhide(ref: AppRef, label: String) {
        lifecycleScope.launch {
            (application as LorApp).home.edit { it.unhide(ref.packageName) }
                .onSuccess { toast("$label is back in the drawer.") }
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
private fun HomeSettings(apps: List<HomeApp>, layout: LayoutState, unhide: (AppRef, String) -> Unit, open: (Intent) -> Unit) {
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
                    TextButton(onClick = { unhide(h.ref, h.label) }) { Text("Unhide") }
                }
            }
        }
        item { HorizontalDivider(Modifier.padding(top = 12.dp)) }
        item {
            Link("Default home app", "Choose which app is home") { open(Intent(Settings.ACTION_HOME_SETTINGS)) }
        }
        item {
            val context = LocalContext.current
            Link("LoR app settings", "Timer, sensors, server and T3") { open(Intent(context, MainActivity::class.java)) }
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
