package com.spadaval.lor.quicksend

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.spadaval.lor.t3.T3Connection
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Quick send (T3 Launcher, Increment 4): opening the sheet ([QuickSendActivity]) from each entry
 * point, and the send itself. A send runs in the process's scope, not the sheet's, and holds the T3
 * connection until T3 answers, so closing or losing the sheet never drops a launch that has gone
 * out: it still ends in a toast saying where the thread started, or why it didn't.
 */
object QuickSend {
    enum class From { AGENT, HOME, TILE, ASSIST }

    private const val EXTRA_FROM = "from"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _sending = MutableStateFlow(false)
    /** True while a send is under way; the sheet can't be closed meanwhile, and won't send again. */
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    fun intent(context: Context, from: From): Intent =
        Intent(context, QuickSendActivity::class.java)
            .putExtra(EXTRA_FROM, from.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

    fun open(context: Context, from: From) = context.startActivity(intent(context, from))

    fun from(intent: Intent?): From =
        intent?.getStringExtra(EXTRA_FROM)?.let { name -> From.entries.firstOrNull { it.name == name } } ?: From.AGENT

    /**
     * Starts a new thread in [projectId] with [text] and the optional [image]. [done] gets null on
     * success, else why it failed; it runs on the main thread even if the sheet is gone, so it must
     * not hold the sheet beyond checking whether it still exists.
     */
    fun send(context: Context, projectId: String, projectTitle: String, text: String, image: T3Connection.Image?, done: (String?) -> Unit) {
        if (_sending.value) return
        _sending.value = true
        val app = context.applicationContext
        val connection = T3Connection.get(app)
        connection.start()
        scope.launch {
            val failure = try {
                connection.launch(projectId, text, image = image)
                null
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                T3Connection.describe(e)
            } finally {
                connection.stop()
                _sending.value = false
            }
            Toast.makeText(app, if (failure == null) "Started in $projectTitle." else "Not sent: $failure", Toast.LENGTH_SHORT).show()
            done(failure)
        }
    }
}
