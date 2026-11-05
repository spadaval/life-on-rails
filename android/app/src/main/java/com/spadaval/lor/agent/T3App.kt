package com.spadaval.lor.agent

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.spadaval.lor.collect.Launch
import com.spadaval.lor.collect.LaunchFrom
import com.spadaval.lor.collect.Launches
import com.spadaval.lor.t3.T3Links

/** Opening the installed T3 app at a thread ([T3Links]). */
object T3App {
    /** The first T3 build in [T3Links.APPS] installed that takes thread links; null when there is none. */
    fun installed(context: Context): T3Links.App? = T3Links.APPS.firstOrNull { app ->
        context.packageManager.resolveActivity(intent(app, "e", "t"), 0) != null
    }

    /**
     * Opens [threadId] on the server [environmentId] names, in a new task and, on a large screen,
     * in a pane beside what's open ([flags]). Returns why it couldn't, or null.
     */
    fun open(context: Context, environmentId: String, threadId: String, from: LaunchFrom): String? {
        val app = installed(context) ?: return "The T3 app isn't installed."
        val intent = intent(app, environmentId, threadId)
        return try {
            context.startActivity(intent.addFlags(flags(context)))
            Launches.record(
                context,
                Launch(from, app.packageName, label = Launches.label(context, app.packageName), thread = threadId, url = intent.dataString),
            )
            null
        } catch (e: ActivityNotFoundException) {
            "The T3 app can't open threads."
        }
    }

    /**
     * A new task, and `FLAG_ACTIVITY_LAUNCH_ADJACENT` only on a large screen (600 dp wide or more):
     * on a phone the flag still splits the screen, pairing the app with the last one used, since home
     * itself can't be in a split.
     */
    fun flags(context: Context): Int =
        Intent.FLAG_ACTIVITY_NEW_TASK or
            if (context.resources.configuration.screenWidthDp >= 600) Intent.FLAG_ACTIVITY_LAUNCH_ADJACENT else 0

    private fun intent(app: T3Links.App, environmentId: String, threadId: String) =
        Intent(Intent.ACTION_VIEW, Uri.parse(T3Links.thread(app, environmentId, threadId))).setPackage(app.packageName)
}
