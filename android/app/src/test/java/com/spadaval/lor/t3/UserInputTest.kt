package com.spadaval.lor.t3

import com.spadaval.lor.t3.UserInput.Draft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Cases from T3's `pendingUserInput.test.ts` and `resolvePendingUserInputAnswer`'s rules. */
class UserInputTest {
    private fun q(
        multi: Boolean = false,
        custom: Boolean = true,
        initial: String? = null,
        options: List<Question.Option> = listOf(Question.Option("Yes", "d"), Question.Option("No", "d", value = "no-value")),
    ) = Question("q", "h", "Q?", options, multi, custom, initial)

    @Test
    fun typedTextWinsOverOptionsAndIsTrimmed() {
        assertEquals("typed", UserInput.resolve(q(), Draft(listOf("Yes"), "  typed  ")))
        assertEquals("typed", UserInput.resolve(q(multi = true), Draft(listOf("Yes"), "typed")))
        assertEquals("Yes", UserInput.resolve(q(), Draft(listOf("Yes"), "   ")))
        assertNull(UserInput.resolve(q(), Draft(custom = "  ")))
    }

    @Test
    fun withAnInitialAnswerTypedTextIsSentAsIs() {
        assertEquals("  keep  ", UserInput.resolve(q(initial = "x"), Draft(custom = "  keep  ")))
        // An emptied initial answer still counts as an answer.
        assertEquals("", UserInput.resolve(q(initial = "x"), Draft(custom = "")))
    }

    @Test
    fun typedTextIsIgnoredWhenNotAllowed() {
        assertNull(UserInput.resolve(q(custom = false), Draft(custom = "typed")))
        assertEquals("Yes", UserInput.resolve(q(custom = false), Draft(listOf("Yes"), "typed")))
        assertEquals(Draft(listOf("Yes")), UserInput.setCustom(q(custom = false), Draft(listOf("Yes")), "typed"))
    }

    @Test
    fun optionsMatchByValueOrByLabelWhenTheyHaveNoValue() {
        assertEquals("no-value", UserInput.resolve(q(), Draft(listOf("no-value"))))
        // An option with a value doesn't match by its label.
        assertNull(UserInput.resolve(q(), Draft(listOf("No"))))
        assertEquals("Yes", UserInput.resolve(q(), Draft(listOf(" Yes "))))
        assertEquals(listOf("Yes", "no-value"), UserInput.resolve(q(multi = true), Draft(listOf("Yes", "no-value", "Yes", "unknown"))))
    }

    @Test
    fun togglingPicksOneOrTogglesForMultiSelect() {
        val single = UserInput.toggle(q(), Draft(listOf("Yes"), "typed"), "no-value")
        assertEquals(Draft(listOf("no-value"), ""), single)
        var multi = UserInput.toggle(q(multi = true), null, "Yes")
        multi = UserInput.toggle(q(multi = true), multi, "no-value")
        assertEquals(listOf("Yes", "no-value"), multi.selected)
        assertEquals(listOf("no-value"), UserInput.toggle(q(multi = true), multi, "Yes").selected)
        assertEquals(Draft(), UserInput.toggle(q(), null, "unknown"))
    }

    @Test
    fun typedTextHidesTheChosenOption() {
        val q = q()
        assertTrue(UserInput.isSelected(q, Draft(listOf("Yes")), "Yes"))
        assertFalse(UserInput.isSelected(q, Draft(listOf("Yes"), "typed"), "Yes"))
        // Typing clears the options; blank typing keeps them.
        assertEquals(Draft(emptyList(), "t"), UserInput.setCustom(q, Draft(listOf("Yes")), "t"))
        assertEquals(Draft(listOf("Yes"), " "), UserInput.setCustom(q, Draft(listOf("Yes")), " "))
    }

    @Test
    fun theInitialAnswerSeedsTheDraftAndEveryQuestionMustBeAnswered() {
        val a = q(initial = "draft").copy(id = "a")
        val b = q().copy(id = "b")
        val seeded = UserInput.seed(listOf(a, b), emptyMap())
        assertEquals(mapOf("a" to Draft(custom = "draft")), seeded)
        // An existing draft is kept.
        assertEquals(Draft(custom = "mine"), UserInput.seed(listOf(a), mapOf("a" to Draft(custom = "mine")))["a"])
        assertNull(UserInput.build(listOf(a, b), seeded))
        assertEquals(mapOf("a" to "draft", "b" to "Yes"), UserInput.build(listOf(a, b), seeded + ("b" to Draft(listOf("Yes")))))
    }
}
