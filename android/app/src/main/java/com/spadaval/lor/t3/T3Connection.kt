package com.spadaval.lor.t3

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** A call T3 refused or never answered; [message] is safe to show. */
class T3CallFailed(message: String) : IOException(message)

/**
 * One authenticated WebSocket to a paired T3 server, subscribed to the shell, reconnecting with
 * backoff when it drops. Each connection takes a fresh ticket and a fresh authoritative snapshot,
 * so nothing is resumed by sequence.
 *
 * Requests are matched to replies by id through the per-socket handler table: the shell
 * subscription, and every unary [call]. The actions ([respond], [answer], [send], [launch]) are
 * unary calls on the open socket; they fail at once when no socket is open, and with the socket
 * when it drops, and are never retried, since a resent command could act twice.
 *
 * Screens share it by holding it: each [start] is one hold and each [stop] lets one go, and the
 * connection runs while anyone holds it (home while it is on screen, the debug screen while open).
 * Each run of the loop is a new generation, and halting it ends that generation. A loop writes [status] and [shell],
 * and sends a queued call, only through [ifCurrent], so a cancelled loop still blocked in a ticket request can never
 * overwrite what a newer loop, [stop] or [forget] wrote.
 */
class T3Connection(private val store: T3Store) {
    sealed interface Status {
        data object NotPaired : Status
        /** Paired, but nothing has started the connection. */
        data object Stopped : Status
        data object Connecting : Status
        data object Live : Status
        /** Dropped; trying again in [retryInMs]. */
        data class Retrying(val error: String, val retryInMs: Long) : Status
        /** The server refused the stored session: pair again. */
        data class Rejected(val error: String) : Status
    }

    private val _status = MutableStateFlow<Status>(if (store.session == null) Status.NotPaired else Status.Stopped)
    val status: StateFlow<Status> = _status.asStateFlow()
    private val _shell = MutableStateFlow(Shell())
    val shell: StateFlow<Shell> = _shell.asStateFlow()

    private val http = OkHttpClient.Builder()
        // Node's ws answers pings, so a silent drop shows as a failure within a minute.
        .pingInterval(20, TimeUnit.SECONDS)
        .build()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    /** The current loop's number; bumped when a loop starts or halts. Guarded by this object's lock. */
    private var generation = 0L
    /** How many [start]s have no [stop] yet; the connection runs while this is above 0. Guarded by this object's lock. */
    private var holds = 0
    /** The paired server's environment id, by the server it came from. */
    @Volatile private var environment: Pair<String, String>? = null
    /** The open socket's event queue, where [call] puts its requests; null while no socket is open and after [stop]. */
    private var calls: SendChannel<Event>? = null

    /** Holds the connection, starting it if no one else holds it. Pair each call with one [stop]. */
    @Synchronized
    fun start() {
        holds++
        run()
    }

    /** Lets go of one [start]'s hold; the connection stops when no one holds it. */
    @Synchronized
    fun stop() {
        if (holds > 0) holds--
        if (holds == 0) halt()
    }

    private fun run() {
        if (job?.isActive == true) return
        val gen = ++generation
        job = scope.launch { loop(gen) }
    }

    private fun halt() {
        generation++
        job?.cancel()
        job = null
        calls = null
        _status.value = if (store.session == null) Status.NotPaired else Status.Stopped
        _shell.value = _shell.value.copy(loaded = false)
    }

    /**
     * Pairs and stores the session, replacing any earlier one. If someone holds the connection, it
     * reconnects with the new session; otherwise it stays stopped, so a pairing that finishes after
     * its screen closed starts nothing. On failure nothing changes. Blocking: call from Dispatchers.IO.
     */
    fun pair(target: PairTarget) {
        val token = T3Auth.pair(target, "LoR launcher")
        synchronized(this) {
            halt()
            store.session = T3Store.Session(target.httpBase, token)
            _shell.value = Shell()
            _status.value = Status.Stopped
            if (holds > 0) run()
        }
    }

    /** The paired server's environment id, for deep links into the T3 app; read once per server. */
    suspend fun environmentId(): String = withContext(Dispatchers.IO) {
        val base = store.session?.httpBase ?: throw T3CallFailed("Not paired with T3.")
        environment?.takeIf { it.first == base }?.second
            ?: T3Auth.environmentId(base).also { environment = base to it }
    }

    /** Approves or denies a pending approval. Returns the command's sequence. */
    suspend fun respond(request: PendingRequest, approve: Boolean): Long =
        Commands.sequence(call(Commands.DISPATCH, Commands.respond(request, approve)))

    /** The questions of a pending `user_input` request, read from its thread's projection. */
    suspend fun questions(request: PendingRequest): List<Question> =
        Commands.questions(call(Commands.PROJECTION, JSONObject().put("threadId", request.threadId)), request.requestId)

    /** Answers a pending question; see [Commands.answer]. Returns the command's sequence. */
    suspend fun answer(request: PendingRequest, answers: Map<String, Any>): Long =
        Commands.sequence(call(Commands.DISPATCH, Commands.answer(request, answers)))

    /** Sends a follow-up message to a thread. Returns the command's sequence. */
    suspend fun send(threadId: String, text: String): Long =
        Commands.sequence(call(Commands.DISPATCH, Commands.message(threadId, text)))

    /** What [launch] uses for [projectId] when given no model or runtime mode. */
    suspend fun launchDefaults(projectId: String): LaunchDefaults =
        Commands.launchDefaults(call(Commands.SERVER_CONFIG, JSONObject()), projectId)

    /**
     * Starts a thread in [projectId] with [text] as its first message. A null [model] or [runtimeMode]
     * takes the project's or server's default ([launchDefaults]); with no default model it fails, and
     * it fails for a project set to start threads in a worktree. An [image] is uploaded first and
     * goes with the first message. Returns the new thread's id.
     */
    suspend fun launch(projectId: String, text: String, model: ModelSelection? = null, runtimeMode: String? = null, image: Image? = null): String {
        val defaults = launchDefaults(projectId)
        // T3 would prepare a worktree, which needs a base branch the launcher doesn't read; refuse rather than start in the root.
        if (defaults.envMode == "worktree") throw T3CallFailed("This project starts threads in a new worktree, which the launcher can't do yet.")
        val chosen = model ?: defaults.model ?: throw T3CallFailed("The server has no default model; choose one.")
        // Like T3's clients, choose the thread and message ids first, so an image can be stored under them.
        val threadId = Commands.newId()
        val messageId = Commands.newId()
        val attachments = image?.let {
            Commands.persistedAttachments(call(Commands.PERSIST_ATTACHMENTS, Commands.persistImage(threadId, messageId, it.name, it.mimeType, it.bytes)))
        } ?: org.json.JSONArray()
        val payload = Commands.launch(
            projectId, text, chosen, runtimeMode ?: defaults.runtimeMode,
            messageId = messageId, threadId = threadId, attachments = attachments,
        )
        return Commands.launchedThreadId(call(Commands.LAUNCH, payload))
    }

    /** An image for a launch's first message, such as the assistant's screen capture. */
    class Image(val name: String, val mimeType: String, val bytes: ByteArray)

    /** One unary request on the open socket; returns the Exit's value. */
    suspend fun call(tag: String, payload: JSONObject): Any? {
        val reply = CompletableDeferred<Any?>()
        val queue = synchronized(this) { calls } ?: throw T3CallFailed("Not connected to T3.")
        if (queue.trySend(Event.Call(tag, payload, reply)).isFailure) throw T3CallFailed("Not connected to T3.")
        return try {
            withTimeout(CALL_TIMEOUT_MS) { reply.await() }
        } catch (e: TimeoutCancellationException) {
            throw T3CallFailed("T3 did not answer within ${CALL_TIMEOUT_MS / 1000} s.")
        }
    }

    /** Drops the session; the holds stay, so pairing again from the same screen reconnects. */
    @Synchronized
    fun forget() {
        halt()
        store.session = null
        _shell.value = Shell()
        _status.value = Status.NotPaired
    }

    /** Runs [write] only while [gen] is the current loop; returns whether it ran. */
    @Synchronized
    private fun ifCurrent(gen: Long, write: () -> Unit): Boolean {
        if (gen != generation) return false
        write()
        return true
    }

    private fun setStatus(gen: Long, status: Status) = ifCurrent(gen) { _status.value = status }

    private suspend fun loop(gen: Long) {
        var backoff = INITIAL_BACKOFF_MS
        while (true) {
            val session = store.session ?: run { setStatus(gen, Status.NotPaired); return }
            if (!setStatus(gen, Status.Connecting)) return
            val error = try {
                connectOnce(gen, session) { backoff = INITIAL_BACKOFF_MS }
            } catch (e: T3AuthRejected) {
                setStatus(gen, Status.Rejected(e.message ?: "rejected"))
                return
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                describe(e)
            }
            if (!setStatus(gen, Status.Retrying(error, backoff))) return
            delay(backoff)
            backoff = (backoff * 2).coerceAtMost(MAX_BACKOFF_MS)
        }
    }

    private sealed interface Event {
        data object Open : Event
        data class Text(val text: String) : Event
        data class Closed(val reason: String) : Event
        class Call(val tag: String, val payload: JSONObject, val reply: CompletableDeferred<Any?>) : Event
    }

    /** Runs one socket until it ends; returns why. [onLive] fires on the first snapshot. */
    private suspend fun connectOnce(gen: Long, session: T3Store.Session, onLive: () -> Unit): String {
        // Blocking and deaf to cancellation, so the loop may be stale by the time it returns.
        val ticket = T3Auth.webSocketTicket(session.httpBase, session.accessToken)
        // Keep showing the last list, but only as live once this socket's own snapshot lands.
        if (!ifCurrent(gen) { _shell.value = _shell.value.copy(loaded = false) }) return "stopped"
        val wsBase = session.httpBase.replaceFirst("http", "ws")
        val request = Request.Builder()
            .url("$wsBase/ws?wsTicket=${URLEncoder.encode(ticket, "UTF-8")}&orchestrationProtocol=$ORCHESTRATION_PROTOCOL")
            .build()
        val events = Channel<Event>(Channel.UNLIMITED)
        val socket = http.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) { events.trySend(Event.Open) }
            override fun onMessage(webSocket: WebSocket, text: String) { events.trySend(Event.Text(text)) }
            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
                events.trySend(Event.Closed("closed by server ($code)"))
            }
            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                if (response?.code == 401) events.trySend(Event.Closed(REJECTED)) else events.trySend(Event.Closed(describe(t)))
            }
        })
        val handlers = HashMap<String, (Rpc.Message) -> Unit>()
        /** Replies still owed to callers; whatever is left fails when this socket ends. */
        val owed = mutableListOf<CompletableDeferred<Any?>>()
        var nextId = 1L
        var ended: String? = null
        try {
            for (event in events) {
                when (event) {
                    Event.Open -> {
                        val id = (nextId++).toString()
                        handlers[id] = { message ->
                            when (message) {
                                is Rpc.Chunk -> ifCurrent(gen) {
                                    var shell = _shell.value
                                    for (item in message.values) shell = shell.apply(item)
                                    _shell.value = shell
                                    if (shell.loaded && _status.value != Status.Live) {
                                        _status.value = Status.Live
                                        onLive()
                                    }
                                }
                                is Rpc.Exit -> ended = "the shell subscription ended: ${message.error ?: "done"}"
                                else -> Unit
                            }
                        }
                        socket.send(Rpc.request(id, "orchestration.subscribeShell", JSONObject()))
                        if (!ifCurrent(gen) { calls = events }) return "stopped"
                    }
                    is Event.Call -> if (!ifCurrent(gen) {}) {
                        // A stopped loop draining its queue sends nothing more.
                        event.reply.completeExceptionally(T3CallFailed("Not connected to T3."))
                    } else {
                        val id = (nextId++).toString()
                        val reply = event.reply
                        owed += reply
                        handlers[id] = { message ->
                            if (message is Rpc.Exit) {
                                if (message.error == null) reply.complete(message.value) else reply.completeExceptionally(T3CallFailed(message.error))
                                owed -= reply
                            }
                        }
                        if (!socket.send(Rpc.request(id, event.tag, event.payload))) reply.completeExceptionally(T3CallFailed("The connection is closing."))
                    }
                    is Event.Text -> for (message in Rpc.decode(event.text)) {
                        when (message) {
                            is Rpc.Chunk -> {
                                handlers[message.requestId]?.invoke(message)
                                socket.send(Rpc.ack(message.requestId))
                            }
                            is Rpc.Exit -> handlers.remove(message.requestId)?.invoke(message)
                            is Rpc.Defect -> ended = "server defect"
                            else -> Unit
                        }
                    }
                    is Event.Closed -> {
                        if (event.reason == REJECTED) throw T3AuthRejected("401 on connect")
                        return event.reason
                    }
                }
                ended?.let { return it }
            }
            return "closed"
        } finally {
            socket.cancel()
            synchronized(this) { if (calls === events) calls = null }
            events.close()
            val lost = T3CallFailed("The connection dropped before T3 answered.")
            for (reply in owed) reply.completeExceptionally(lost)
            while (true) {
                val left = events.tryReceive().getOrNull() ?: break
                if (left is Event.Call) left.reply.completeExceptionally(lost)
            }
        }
    }

    companion object {
        /** `ORCHESTRATION_PROTOCOL_VERSION` in T3's contracts; the server answers 426 to any other. */
        const val ORCHESTRATION_PROTOCOL = 2
        private const val INITIAL_BACKOFF_MS = 1_000L
        private const val MAX_BACKOFF_MS = 30_000L
        private const val REJECTED = "rejected"
        private const val CALL_TIMEOUT_MS = 30_000L

        /** Exception text without anything from a URL's query, where the ticket rides. */
        fun describe(t: Throwable): String = (t.message ?: t.javaClass.simpleName).replace(Regex("\\?\\S*"), "")

        @Volatile private var instance: T3Connection? = null

        /** One connection for the app process. */
        fun get(context: Context): T3Connection = instance ?: synchronized(this) {
            instance ?: T3Connection(T3Store(context.applicationContext)).also { instance = it }
        }
    }
}
