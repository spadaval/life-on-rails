package com.spadaval.lor.home

import kotlin.test.Test
import kotlin.test.assertEquals

/** Home's set-up card (launcher polish 3.5): which steps are left, and which one leads. */
class SetupStepsTest {
    @Test
    fun theStepsLeftAreInOrderAndNoneMeansDone() {
        assertEquals(listOf(SetupStep.HOME, SetupStep.T3, SetupStep.LOR), Setup.left(home = false, paired = false, lor = LorReach.Checking))
        assertEquals(listOf(SetupStep.T3), Setup.left(home = true, paired = false, lor = LorReach.Reached))
        assertEquals(listOf(SetupStep.LOR), Setup.left(home = true, paired = true, lor = LorReach.Failed("LoR didn't answer. Is Tailscale on?")))
        assertEquals(listOf(SetupStep.HOME), Setup.left(home = false, paired = true, lor = LorReach.Reached))
        assertEquals(emptyList<SetupStep>(), Setup.left(home = true, paired = true, lor = LorReach.Reached))
    }
}
