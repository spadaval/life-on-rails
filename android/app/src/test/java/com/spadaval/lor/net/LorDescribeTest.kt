package com.spadaval.lor.net

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/** Why LoR can't be read, in plain words (launcher polish 3.3). */
class LorDescribeTest {
    @Test
    fun noAnswerAsksAboutTailscale() {
        listOf(UnknownHostException("buildbot"), ConnectException("refused"), NoRouteToHostException("x"), SocketTimeoutException("timeout"))
            .forEach { assertEquals("LoR didn't answer. Is Tailscale on?", LorClient.describe(it)) }
    }

    @Test
    fun anAnswerSaysWhatTheServerSaid() {
        assertEquals("403: the server did not recognise you: is Tailscale on?", LorClient.describe(IOException("403: the server did not recognise you: is Tailscale on?")))
        assertEquals("500: boom.", LorClient.describe(IOException("500: boom")))
        assertEquals("IllegalStateException.", LorClient.describe(IllegalStateException()))
    }
}
