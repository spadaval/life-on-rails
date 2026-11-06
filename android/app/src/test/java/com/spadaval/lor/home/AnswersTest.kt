package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AnswersTest {
    private fun v(q: String) = Answers.of(q, '.')?.value

    @Test
    fun calculates() {
        assertEquals("4", v("2+2"))
        assertEquals("14", v("2 + 3 * 4"))
        assertEquals("20", v("(2+3)*4"))
        assertEquals("1024", v("2^10"))
        assertEquals("2", v("-3+5"))
        assertEquals("3", v("1,5*2"))
        assertEquals("2.5", v("10/4"))
        assertEquals("0.3333333333", v("1/3"))
        assertEquals("3000", v("1,000*3"))
        assertEquals("84", v("12×7"))
        assertEquals("-4", v("-2^2"))
        assertEquals("1.2676506E30", v("2^100"))
    }

    @Test
    fun noAnswerForWhatIsNotASum() {
        for (q in listOf("1/0", "2+", "42", "-42", "(42)", "maps", "(1", "555-1234", "2026-11-06", "555 1234")) assertNull(q, v(q))
    }

    @Test
    fun theLocalesDecimalComma() {
        assertEquals("2,5", Answers.of("10/4", ',')?.value)
        assertEquals("3000", Answers.of("1.000*3", ',')?.value)
        assertEquals("3", Answers.of("1,5*2", ',')?.value)
    }

    @Test
    fun converts() {
        assertEquals("3.107 mi", v("5 km"))
        assertEquals("5000 m", v("5 km to m"))
        assertEquals("37.78 °C", v("100 f"))
        assertEquals("154.3 lb", v("70 kg in lb"))
        assertEquals("0.001 GB", v("1 mb"))
        assertEquals("12.7 cm", v("5 in"))
        assertEquals("2 in", v("5.08 cm in in"))
        assertEquals("-40 °F", v("-40 c"))
        assertEquals("62.14 mph", v("100 km/h"))
        assertEquals("5 km", v("3.107 mi to km"))
    }

    @Test
    fun noAnswerForWhatIsNotAConversion() {
        for (q in listOf("5 km to kg", "5 foo", "5 km to km", "km", "5")) assertNull(q, v(q))
    }

    @Test
    fun anAnswerKeepsItsQuery() {
        assertEquals(Answer("84", "12*7"), Answers.of(" 12*7 ", '.'))
    }

    @Test
    fun theAnswerComesFirstAndIsNeverGo() {
        val apps = listOf(Found(Kind.APPS, "a", listOf(Match.prepare("12 Minute Workout")), "app"))
        val r = Search.run("12*7", apps, answer = { Answers.of(it, '.') })
        assertEquals("84", r.answer?.value)
        assertNull(r.go)
        assertEquals(true, r.nothing)
    }
}
