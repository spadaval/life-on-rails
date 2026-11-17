package com.spadaval.lor.home

import android.app.SearchManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract

/*
 * The intents search's rows open on the phone (launcher polish 5.5, 5.6): the web and the store
 * for a search no app matched (NoResults.kt), and a contact's. The rows are drawn in homecore.
 */

/** The intent for [way] and [query]; null for the agent, which opens quick send. */
internal fun intentFor(way: Elsewhere, query: String): Intent? = when (way.kind) {
    Elsewhere.Kind.WEB -> webSearch(query.trim())
    Elsewhere.Kind.STORE -> storeSearch(query.trim())
    Elsewhere.Kind.AGENT -> null
}

private fun webSearch(q: String) = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, q)

private fun storeSearch(q: String) = Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + Uri.encode(q)))

/** Resolves [SearchTargets]; asks the package manager, so it runs off the main thread. */
internal fun searchTargets(pm: PackageManager): SearchTargets {
    val web = webSearch("").takeIf { takes(pm, it) }?.let { takenBy(pm, it) ?: "" }
    val store = storeSearch("").takeIf { takes(pm, it) }?.let { intent ->
        val pkg = takenBy(pm, intent) ?: return@let "app stores"
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrDefault("app stores")
    }
    return SearchTargets(web, store)
}

private fun takes(pm: PackageManager, intent: Intent) = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isNotEmpty()

/** The package that opens [intent] without asking; null when Android asks (its chooser is `android`). */
internal fun takenBy(pm: PackageManager, intent: Intent): String? =
    pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)?.activityInfo?.packageName?.takeIf { it != "android" }

/** The intent for [way] on [c]. */
internal fun contactIntent(c: SearchContact, way: ContactWay): Intent = when (way) {
    ContactWay.OPEN -> Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.getLookupUri(c.id, c.lookupKey))
    ContactWay.CALL -> Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", c.numbers.first(), null))
    ContactWay.MESSAGE -> Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", c.numbers.first(), null))
}
