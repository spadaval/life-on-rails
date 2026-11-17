package com.spadaval.lor.lab

import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FixturesTest {
    @Test
    fun namesAreUniqueAndTheDemoDayIsFirst() {
        val names = Fixtures.all.map { it.name }
        assertEquals(names.distinct(), names)
        assertEquals("demo", names.first())
        for (f in Fixtures.all) assertTrue(f.about.isNotBlank(), "${f.name} says what it stands for")
    }

    @Test
    fun everyFixtureReadsBackFromItsJson() {
        for (f in Fixtures.all) assertEquals(f, Fixtures.fromJson(Fixtures.toJson(f)).getOrThrow(), f.name)
    }

    @Test
    fun theDefaultIsTheDemoSetsDay() {
        val home = assertNotNull(Fixtures.demo.lor.home.reply)
        assertEquals(Fixtures.demo.clock.now, home["now"]!!.jsonPrimitive.content)
        assertEquals(Fixtures.demo.clock.zone, home["timezone"]!!.jsonPrimitive.content)
        assertEquals(3, home["needs_you"]!!.jsonArray.size)
        assertEquals("Partly cloudy", home["weather"]!!.jsonObject["condition"]!!.jsonPrimitive.content)
    }

    @Test
    fun aOneOffChangesOnlyWhatItNames() {
        val f = Fixtures.fromJson("""{"base":"demo","t3":{"status":"NOT_PAIRED"},"where":{"open":"DRAWER","target":"Work"}}""").getOrThrow()
        assertEquals("one-off", f.name)
        assertEquals(T3Status.NOT_PAIRED, f.t3.status)
        assertEquals(Fixtures.demo.t3.threads, f.t3.threads)
        assertEquals(Opened.DRAWER, f.where.open)
        assertEquals("Work", f.where.target)
        assertEquals(Fixtures.demo.copy(name = "one-off", t3 = f.t3, where = f.where), f)
    }

    @Test
    fun aOneOffPatchesAReplyKeyByKey() {
        val f = Fixtures.fromJson("""{"base":"demo","name":"no-flags","lor":{"home":{"reply":{"needs_you":[]}}}}""").getOrThrow()
        assertEquals("no-flags", f.name)
        val home = f.lor.home.reply!!
        assertEquals(0, home["needs_you"]!!.jsonArray.size)
        assertEquals(Fixtures.demo.lor.home.reply!!["day"], home["day"])
    }

    @Test
    fun withoutABaseTheObjectIsTheWholeFixture() {
        val f = Fixtures.fromJson("""{"name":"bare","t3":{"status":"LIVE"}}""").getOrThrow()
        assertEquals(Fixture("bare", t3 = T3(T3Status.LIVE)), f)
    }

    @Test
    fun aOneOffSaysWhatIsWrong() {
        assertEquals("not JSON", Fixtures.fromJson("{nope").exceptionOrNull()?.message?.substringBefore(":"))
        assertEquals("no state \"gone\" to start from", Fixtures.fromJson("""{"base":"gone"}""").exceptionOrNull()?.message)
        val typo = Fixtures.fromJson("""{"t3":{"stauts":"LIVE"}}""").exceptionOrNull()?.message
        assertTrue(typo != null && "stauts" in typo, "a typo is named: $typo")
        assertTrue(Fixtures.fromJson("""{"t3":{"status":"SLEEPING"}}""").isFailure)
    }

    @Test
    fun theLabResolvesNamesAndOneOffs() {
        assertEquals(Fixtures.overfull to null, Lab.fixture("overfull"))
        assertEquals(T3Status.REJECTED, Lab.fixture("""{"base":"failed"}""").first.t3.status)
        val (f, problem) = Lab.fixture("""{"base":"demo""")
        assertEquals("demo", f.name)
        assertTrue(problem!!.startsWith("The one-off state: not JSON"), problem)
        assertNull(Lab.fixture("demo").second)
    }

    @Test
    fun theConditionsAreWhatTheirNamesSay() {
        assertNull(Fixtures.empty.layout)
        assertEquals(T3Status.NOT_PAIRED, Fixtures.empty.t3.status)
        assertEquals(Load.LOADING, Fixtures.loading.apps.load)
        assertTrue(listOf(Fixtures.loading.lor.home, Fixtures.loading.lor.tasks, Fixtures.loading.lor.people).all { it.load == Load.LOADING })
        assertTrue(listOf(Fixtures.stale.lor.home, Fixtures.stale.lor.tasks).all { it.load == Load.STALE && it.reply != null })
        assertEquals(T3Status.RETRYING, Fixtures.stale.t3.status)
        assertEquals(Load.FAILED, Fixtures.failed.lor.home.load)
        assertEquals(2, Fixtures.failed.phone.sync.errors.size)
        assertEquals(2, Fixtures.failed.phone.permissions.count { !it.value })
        assertEquals(12, Fixtures.overfull.t3.threads.count { it.requests.isNotEmpty() || it.state == ThreadState.FAILED })
        assertEquals(9, Fixtures.overfull.lor.home.reply!!["needs_you"]!!.jsonArray.size)
        assertEquals(200, Fixtures.overfull.apps.list.size)
    }

    @Test
    fun everyAppOnALayoutIsInstalledAndOnHomeOnce() {
        for (f in Fixtures.all) {
            val layout = f.layout ?: continue
            val installed = f.apps.list.map { it.ref }.toSet().ifEmpty { Parts.manyApps.map { it.ref }.toSet() }
            val onHome = Regex("\"((?:work:)?[a-zA-Z][\\w.]*/[\\w.]+)\"").findAll(layout["layouts"].toString()).map { it.groupValues[1] }.toList()
            assertTrue(onHome.isNotEmpty(), f.name)
            assertEquals(emptyList(), onHome.filter { it !in installed }, "${f.name}: apps on home that aren't installed")
            assertEquals(onHome.distinct(), onHome, "${f.name}: an app on home twice")
        }
    }
}
