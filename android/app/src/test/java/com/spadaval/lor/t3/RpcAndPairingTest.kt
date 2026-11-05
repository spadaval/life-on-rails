package com.spadaval.lor.t3

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RpcAndPairingTest {
    @Test
    fun requestsAndAcksAreEffectRpcJson() {
        val request = JSONObject(Rpc.request("1", "orchestration.subscribeShell", JSONObject()))
        assertEquals("Request", request.getString("_tag"))
        assertEquals("1", request.getString("id"))
        assertEquals("orchestration.subscribeShell", request.getString("tag"))
        assertEquals(0, request.getJSONArray("headers").length())
        val ack = JSONObject(Rpc.ack("1"))
        assertEquals("Ack", ack.getString("_tag"))
        assertEquals("1", ack.getString("requestId"))
    }

    @Test
    fun framesDecodeOneMessageOrAnArray() {
        assertEquals(listOf(Rpc.Pong), Rpc.decode("""{"_tag":"Pong"}"""))
        val batch = Rpc.decode("""[{"_tag":"Chunk","requestId":"3","values":[{"kind":"synchronized"}]},{"_tag":"Exit","requestId":"3","exit":{"_tag":"Success","value":null}}]""")
        assertEquals("3", (batch[0] as Rpc.Chunk).requestId)
        assertEquals(Rpc.Exit("3", JSONObject.NULL, null), batch[1])
        val failed = Rpc.decode("""{"_tag":"Exit","requestId":4,"exit":{"_tag":"Failure","cause":[{"_tag":"Fail","error":{"_tag":"EnvironmentAuthorizationError"}}]}}""").single() as Rpc.Exit
        assertEquals("4", failed.requestId)
        assertTrue(failed.error!!.contains("EnvironmentAuthorizationError"))
    }

    @Test
    fun pairingReadsLinksAppLinksAndBareTokens() {
        // `t3 pair` prints all three forms.
        assertEquals(PairTarget("http://localhost:7981", "UGHPG2WQPQPJ"), Pairing.parse("", "http://localhost:7981/pair#token=UGHPG2WQPQPJ"))
        assertEquals(PairTarget("http://localhost:7981", "UGHPG2WQPQPJ"), Pairing.parse("", "t3code://pair?pairingUrl=http%3A%2F%2Flocalhost%3A7981%2Fpair%23token%3DUGHPG2WQPQPJ"))
        assertEquals(PairTarget("http://10.0.2.2:16021", "UGHPG2WQPQPJ"), Pairing.parse("http://10.0.2.2:16021/", "UGHPG2WQPQPJ"))
        // The typed address wins over the link's host.
        assertEquals(PairTarget("http://10.0.2.2:16021", "ABC"), Pairing.parse("http://10.0.2.2:16021", "http://localhost:7981/pair#token=ABC"))
        // A hosted link names the backend in `host`.
        assertEquals(PairTarget("https://buildbot.example.ts.net", "ABC"), Pairing.parse("", "https://app.t3.codes/pair?host=https%3A%2F%2Fbuildbot.example.ts.net#token=ABC"))
        // Like T3, a bare host means https.
        assertEquals("https://buildbot.example.ts.net", Pairing.base("buildbot.example.ts.net"))
        assertThrows(IllegalArgumentException::class.java) { Pairing.parse("", "ABC") }
        assertThrows(IllegalArgumentException::class.java) { Pairing.parse("", "http://localhost:7981/pair") }
    }

    @Test
    fun describeDropsTheTicket() {
        assertEquals("Failed to reach ws://h/ws", T3Connection.describe(RuntimeException("Failed to reach ws://h/ws?wsTicket=secret&orchestrationProtocol=2")))
    }
}
