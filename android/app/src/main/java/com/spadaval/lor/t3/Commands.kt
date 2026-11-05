package com.spadaval.lor.t3

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/** A provider instance and model, as T3's `ModelSelection` puts it on the wire. [options] passes through untouched. */
data class ModelSelection(val instanceId: String, val model: String, val options: JSONObject? = null) {
    fun toJson(): JSONObject = JSONObject().put("instanceId", instanceId).put("model", model).also { o -> options?.let { o.put("options", it) } }

    companion object {
        /** Older settings name the instance `provider`. Null when absent or JSON null. */
        fun from(o: JSONObject?): ModelSelection? {
            if (o == null) return null
            val instance = if (o.isNull("instanceId")) (if (o.isNull("provider")) null else o.optString("provider")) else o.optString("instanceId")
            if (instance.isNullOrEmpty() || o.isNull("model")) return null
            return ModelSelection(instance, o.getString("model"), o.optJSONObject("options"))
        }
    }
}

/** One question of a pending `user_input` request (T3's `OrchestrationV2UserInputQuestion`). */
data class Question(
    val id: String,
    val header: String,
    val question: String,
    val options: List<Option>,
    val multiSelect: Boolean,
    /** False only when the server says so; otherwise free text is accepted. */
    val allowCustomAnswer: Boolean,
) {
    /** [answer] is what the server takes: the option's `value`, or its label when it has none. */
    data class Option(val label: String, val description: String, val answer: String)
}

/** What a launch uses when the caller names no model or runtime mode: the project's settings, then the server's. */
data class LaunchDefaults(val model: ModelSelection?, val runtimeMode: String)

/**
 * The `orchestration.dispatchCommand` and `orchestration.launchThread` payloads the launcher sends,
 * and the parts of their replies it reads. Shapes follow T3's `packages/client-runtime/src/operations/commands.ts`
 * (`respondToThreadApproval`, `respondToThreadUserInput`, `startThreadTurn`), sending only the fields these calls need.
 * Pure: ids are passed in, so tests can pin them.
 */
object Commands {
    const val DISPATCH = "orchestration.dispatchCommand"
    const val LAUNCH = "orchestration.launchThread"
    const val PROJECTION = "orchestration.getThreadProjection"
    const val SERVER_CONFIG = "server.getConfig"

    /** What threads this app starts or messages say they came from. */
    private const val SOURCE = "mobile"

    fun newId(): String = UUID.randomUUID().toString()

    /** Approves (`accept`, once) or denies (`decline`) a pending approval. */
    fun respond(request: PendingRequest, approve: Boolean, commandId: String = newId()): JSONObject =
        JSONObject()
            .put("type", "runtime-request.respond")
            .put("commandId", commandId)
            .put("threadId", request.threadId)
            .put("requestId", request.requestId)
            .put("decision", if (approve) "accept" else "decline")

    /** Answers a pending question. [answers] maps question id to a string, or a list of strings for a multi-select. */
    fun answer(request: PendingRequest, answers: Map<String, Any>, commandId: String = newId()): JSONObject {
        val encoded = JSONObject()
        for ((id, value) in answers) encoded.put(id, if (value is List<*>) JSONArray(value) else value)
        return JSONObject()
            .put("type", "runtime-request.respond")
            .put("commandId", commandId)
            .put("threadId", request.threadId)
            .put("requestId", request.requestId)
            .put("answers", encoded)
    }

    /**
     * A user message to an existing thread, as T3's composer sends one by default (`deliveryIntent: auto`):
     * it starts a turn on an idle thread, and the server decides what an active one does with it.
     */
    fun message(threadId: String, text: String, commandId: String = newId(), messageId: String = newId()): JSONObject =
        JSONObject()
            .put("type", "message.dispatch")
            .put("commandId", commandId)
            .put("createdBy", "user")
            .put("creationSource", SOURCE)
            .put("threadId", threadId)
            .put("messageId", messageId)
            .put("text", text)
            .put("attachments", JSONArray())
            .put("deliveryIntent", "auto")
            .put("dispatchMode", JSONObject().put("type", "start_immediately"))

    /** A new thread in [projectId] at the project's root, starting with [text]; the server may retitle it. */
    fun launch(
        projectId: String,
        text: String,
        model: ModelSelection,
        runtimeMode: String,
        commandId: String = newId(),
        messageId: String = newId(),
    ): JSONObject =
        JSONObject()
            .put("commandId", commandId)
            .put("creationSource", SOURCE)
            .put("projectId", projectId)
            .put("title", title(text))
            .put("generateTitle", true)
            .put("modelSelection", model.toJson())
            .put("runtimeMode", runtimeMode)
            .put("interactionMode", "default")
            .put("workspaceStrategy", JSONObject().put("type", "root"))
            .put("initialMessage", JSONObject().put("messageId", messageId).put("text", text).put("attachments", JSONArray()))

    /** T3's `deriveThreadTitleSeed` for plain text: whitespace collapsed, cut at 50 characters. */
    fun title(text: String): String {
        val collapsed = text.trim().replace(Regex("\\s+"), " ")
        if (collapsed.isEmpty()) return "New thread"
        val points = collapsed.codePoints().toArray()
        return if (points.size <= 50) collapsed else String(points, 0, 50) + "..."
    }

    /** The commit sequence a dispatch reply carries. */
    fun sequence(reply: Any?): Long = (reply as JSONObject).getLong("sequence")

    /** The thread a launch created (or resumed). */
    fun launchedThreadId(reply: Any?): String = (reply as JSONObject).getString("threadId")

    /** The questions of [requestId] in a `getThreadProjection` reply; empty when it holds none. */
    fun questions(projection: Any?, requestId: String): List<Question> {
        val items = (projection as JSONObject).getJSONArray("turnItems")
        for (i in 0 until items.length()) {
            val item = items.getJSONObject(i)
            if (item.optString("type") != "user_input_request" || item.optString("requestId") != requestId) continue
            val qs = item.getJSONArray("questions")
            return List(qs.length()) { j ->
                val q = qs.getJSONObject(j)
                val opts = q.getJSONArray("options")
                Question(
                    id = q.getString("id"),
                    header = q.getString("header"),
                    question = q.getString("question"),
                    options = List(opts.length()) { k ->
                        val o = opts.getJSONObject(k)
                        val label = o.getString("label")
                        Question.Option(label, o.optString("description"), if (o.isNull("value")) label else o.getString("value"))
                    },
                    multiSelect = q.optBoolean("multiSelect"),
                    allowCustomAnswer = q.optBoolean("allowCustomAnswer", true),
                )
            }
        }
        return emptyList()
    }

    /**
     * The model and runtime mode a new thread in [projectId] gets from a `server.getConfig` reply: the
     * project's override in `projectSettingsOverrides`, else the server's `defaultModelSelection`, as T3's
     * `resolveProjectSettings` does. Not ported from T3's new-thread flow: switching to
     * `fallbackModelSelection` when the default provider's usage limit is used up, offering the first
     * available model when none is set (the launcher does not guess), skipping an override whose
     * instance is disabled, and the project's legacy `defaultModelSelection`.
     */
    fun launchDefaults(config: Any?, projectId: String): LaunchDefaults {
        val settings = (config as JSONObject).getJSONObject("settings")
        val project = settings.optJSONObject("projectSettingsOverrides")?.optJSONObject(projectId)
        // A project override replaces the server's default even when it is null.
        val default = if (project?.has("defaultModelSelection") == true) project.optJSONObject("defaultModelSelection") else settings.optJSONObject("defaultModelSelection")
        val model = ModelSelection.from(default)
        val runtimeMode = project?.optString("defaultRuntimeMode")?.ifEmpty { null }
            ?: settings.optString("defaultRuntimeMode").ifEmpty { "full-access" }
        return LaunchDefaults(model, runtimeMode)
    }
}
