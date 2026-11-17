package com.spadaval.lor.agent

import org.junit.Assert.assertEquals
import org.junit.Test

/** What the reply box says when a send fails (lor-iabt). */
class RepliesTest {
    @Test
    fun aReplyIsCalledNotSentOnlyWhenNoSocketWasOpen() {
        assertEquals("Not sent: Not connected to T3.", Replies.failure("Not connected to T3."))
        // A timeout or a drop: T3 may still take it (seen on the scratch server), so not "Not sent".
        val timeout = Replies.failure("T3 did not answer within 30 s.")
        assertEquals("Not confirmed: T3 did not answer within 30 s. It may still reach the thread, so check it before sending again.", timeout)
    }
}
