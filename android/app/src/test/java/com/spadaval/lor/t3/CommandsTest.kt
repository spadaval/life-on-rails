package com.spadaval.lor.t3

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Exchanges recorded from the scratch T3 server (`vp run dev`, Replay threads), each a Request the
 * server accepted followed by its reply:
 * - `dispatch.ndjson`: approving a Replay `approval` request, answering a Replay `question`,
 *   a follow-up message to that idle thread, then approving the already answered question (refused).
 * - `launch.ndjson`: launching a Replay `approval` thread in `approval-required` mode.
 * - `projection-question.ndjson`: `getThreadProjection` of a thread waiting on a `question`.
 * - `server-config.ndjson`: `server.getConfig`, its reply cut to `settings`, with a server default
 *   model and one project override set.
 */
class CommandsTest {
    private fun lines(name: String): List<String> =
        javaClass.classLoader!!.getResource("t3/$name")!!.readText().lines().filter { it.isNotBlank() }

    /** (request payload, reply) pairs. */
    private fun exchanges(name: String): List<Pair<JSONObject, Rpc.Exit>> =
        lines(name).chunked(2).map { (request, reply) -> JSONObject(request).getJSONObject("payload") to Rpc.decode(reply).single() as Rpc.Exit }

    private val dispatch = exchanges("dispatch.ndjson")

    /** [ours] has the same keys and values as the payload the server took, ids aside. */
    private fun assertSameShape(sent: JSONObject, ours: JSONObject) {
        val ids = setOf("commandId", "messageId")
        fun strip(o: JSONObject): JSONObject = JSONObject(o.toString()).apply {
            ids.forEach { remove(it) }
            optJSONObject("initialMessage")?.remove("messageId")
        }
        assertEquals(plain(strip(sent)), plain(strip(ours)))
    }

    /** JSON as Kotlin maps and lists, so key order does not matter (Android's JSONObject has no toMap). */
    private fun plain(v: Any?): Any? = when (v) {
        is JSONObject -> v.keys().asSequence().associateWith { plain(v.get(it)) }
        is JSONArray -> List(v.length()) { plain(v.get(it)) }
        else -> v
    }

    private fun request(payload: JSONObject) = PendingRequest(payload.getString("threadId"), payload.getString("requestId"), "any")

    @Test
    fun approvingAndDenyingMatchWhatTheServerTook() {
        val (sent, reply) = dispatch[0]
        assertSameShape(sent, Commands.respond(request(sent), approve = true))
        assertEquals(588L, Commands.sequence(reply.value))
        val deny = Commands.respond(request(sent), approve = false)
        assertEquals("decline", deny.getString("decision"))
        assertEquals("runtime-request.respond", deny.getString("type"))
    }

    @Test
    fun answeringMatchesWhatTheServerTook() {
        val (sent, reply) = dispatch[1]
        assertSameShape(sent, Commands.answer(request(sent), mapOf("schema_vs_ui_flexibility" to "UI flexibility")))
        assertEquals(632L, Commands.sequence(reply.value))
        // A multi-select answer is a list.
        val multi = Commands.answer(request(sent), mapOf("q" to listOf("a", "b")))
        assertEquals("b", multi.getJSONObject("answers").getJSONArray("q").getString(1))
    }

    @Test
    fun aFollowUpMatchesWhatTheServerTook() {
        val (sent, reply) = dispatch[2]
        assertSameShape(sent, Commands.message(sent.getString("threadId"), "one more"))
        assertEquals(653L, Commands.sequence(reply.value))
    }

    @Test
    fun aRefusedCommandSaysWhy() {
        val (_, reply) = dispatch[3]
        assertNull(reply.value)
        assertTrue(reply.error!!, reply.error!!.endsWith("is resolved."))
    }

    @Test
    fun aLaunchMatchesWhatTheServerTook() {
        val (sent, reply) = exchanges("launch.ndjson").single()
        val ours = Commands.launch(sent.getString("projectId"), "go", ModelSelection("devReplay", "approval"), "approval-required")
        // The recorded launch set its own title; ours derives one from the text and lets the server retitle.
        assertEquals("go", ours.getString("title"))
        assertTrue(ours.getBoolean("generateTitle"))
        ours.remove("generateTitle")
        ours.put("title", sent.getString("title"))
        assertSameShape(sent, ours)
        assertEquals(reply.value.let { (it as JSONObject).getJSONObject("projection").getJSONObject("thread").getString("id") }, Commands.launchedThreadId(reply.value))
        assertTrue(Commands.launchedThreadId(reply.value).startsWith("thread:project:"))
    }

    @Test
    fun titlesFollowT3() {
        assertEquals("Fix the flaky test", Commands.title("  Fix the\n flaky   test "))
        assertEquals("New thread", Commands.title("   "))
        val long = "a".repeat(49) + "😀" + "tail"
        assertEquals("a".repeat(49) + "😀...", Commands.title(long))
    }

    @Test
    fun questionsAreReadFromTheProjection() {
        val (sent, reply) = exchanges("projection-question.ndjson").single()
        val requestId = (reply.value as JSONObject).getJSONArray("runtimeRequests").getJSONObject(0).getString("id")
        val questions = Commands.questions(reply.value, requestId)
        assertEquals(sent.getString("threadId"), (reply.value as JSONObject).getJSONObject("thread").getString("id"))
        val q = questions.single()
        assertEquals("schema_vs_ui_flexibility", q.id)
        assertEquals("Should this fixture prefer strict schemas or UI flexibility?", q.question)
        assertEquals(listOf("Strict schemas (Recommended)", "UI flexibility"), q.options.map { it.answer })
        assertEquals(false, q.multiSelect)
        assertEquals(true, q.allowCustomAnswer)
        assertTrue(Commands.questions(reply.value, "another").isEmpty())
    }

    @Test
    fun launchDefaultsComeFromTheProjectThenTheServer() {
        val (_, reply) = exchanges("server-config.ndjson").single()
        val overridden = Commands.launchDefaults(reply.value, "6914c5fe-4222-4ffe-ba69-0847ebd493b5")
        assertEquals(ModelSelection("devReplay", "multi-turn"), overridden.model)
        assertEquals("full-access", overridden.runtimeMode)
        val plain = Commands.launchDefaults(reply.value, "e80c134d-2247-4203-84dc-5c4574210d8d")
        assertEquals(ModelSelection("devReplay", "showcase"), plain.model)

        // A null project override wins and the usage-limit fallback does not fill it; a project runtime mode wins.
        val settings = JSONObject().put("defaultModelSelection", JSONObject().put("instanceId", "codex").put("model", "m"))
            .put("fallbackModelSelection", JSONObject().put("provider", "claudeAgent").put("model", "haiku"))
            .put("defaultRuntimeMode", "auto")
            .put("projectSettingsOverrides", JSONObject().put("p", JSONObject().put("defaultModelSelection", JSONObject.NULL).put("defaultRuntimeMode", "approval-required")))
        val config = JSONObject().put("settings", settings)
        assertEquals(LaunchDefaults(null, "approval-required"), Commands.launchDefaults(config, "p"))
        assertEquals(LaunchDefaults(ModelSelection("codex", "m"), "auto"), Commands.launchDefaults(config, "q"))
        settings.put("defaultModelSelection", JSONObject.NULL).put("fallbackModelSelection", JSONObject.NULL)
        assertNull(Commands.launchDefaults(config, "q").model)
    }
}
