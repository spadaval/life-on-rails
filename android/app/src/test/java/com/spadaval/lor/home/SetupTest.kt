package com.spadaval.lor.home

import com.spadaval.lor.t3.T3Connection.Status
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Home's set-up card (launcher polish 3.5): what counts as paired. The steps are SetupStepsTest's, in homecore. */
class SetupTest {
    @Test
    fun pairedMeansASessionTheServerHasNotRefused() {
        assertFalse(Setup.paired(Status.NotPaired))
        assertFalse(Setup.paired(Status.Rejected("revoked")))
        assertTrue(Setup.paired(Status.Stopped))
        assertTrue(Setup.paired(Status.Connecting))
        assertTrue(Setup.paired(Status.Live))
        assertTrue(Setup.paired(Status.Retrying("dropped", 1000)))
    }
}
