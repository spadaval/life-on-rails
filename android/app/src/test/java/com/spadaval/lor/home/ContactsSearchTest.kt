package com.spadaval.lor.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContactsSearchTest {
    private fun contact(name: String, vararg numbers: String) =
        Found(Kind.CONTACTS, "c:$name", listOf(Match.prepare(name)), name, numbers.map(::digitsOf))

    private val contacts = listOf(
        contact("Alice Smith", "+1 (555) 123-4567"),
        contact("Bob Jones", "020 7946 0018"),
        contact("Carol", ),
    )

    private fun found(q: String, sources: List<Found<String>> = contacts) =
        Search.run(q, sources).blocks.flatMap { b -> b.entries.map { it.value } }

    @Test
    fun aNameByWordStartAndByTypo() {
        assertEquals(listOf("Alice Smith"), found("ali"))
        assertEquals(listOf("Alice Smith"), found("smi"))
        assertEquals(listOf("Alice Smith"), found("alcie"))
    }

    @Test
    fun aNumberByItsDigitsFromThreeDigits() {
        assertEquals(listOf("Alice Smith"), found("555"))
        assertEquals(listOf("Alice Smith"), found("555-1234"))
        assertEquals(listOf("Bob Jones"), found("7946"))
        assertEquals(emptyList<String>(), found("55"))
        assertEquals("15551234567", digitsOf("+1 (555) 123-4567"))
    }

    @Test
    fun contactsComeAfterAppsOfTheSameTier() {
        val app = Found(Kind.APPS, "a", listOf(Match.prepare("Alarms")), "Alarms")
        val r = Search.run("al", listOf(contacts[0], app))
        assertEquals(listOf(Kind.APPS, Kind.CONTACTS), r.blocks.map { it.kind })
        assertEquals("Alarms", r.go?.value)
        // One letter is apps only.
        assertEquals(listOf(Kind.APPS), Search.run("a", listOf(contacts[0], app)).blocks.map { it.kind })
    }

    @Test
    fun aBetterTierPutsContactsFirst() {
        // "car" is inside Vocalcar's name (contains) but starts Carol's (prefix).
        val app = Found(Kind.APPS, "a", listOf(Match.prepare("Vocalcar")), "Vocalcar")
        val r = Search.run("car", listOf(app, contacts[2]))
        assertEquals(listOf(Kind.CONTACTS, Kind.APPS), r.blocks.map { it.kind })
        assertEquals("Carol", r.go?.value)
        assertNull(Search.run("zzz", contacts).go)
    }

    @Test
    fun aLooseMatchOfAnotherKindDoesNotHideAnAppTypo() {
        val chrome = Found(Kind.APPS, "a", listOf(Match.prepare("Chrome")), "Chrome")
        val christina = contact("Christina Ramey")
        // c-h-r…a-m-e is in Christina Ramey in order (tier 5); Chrome is one edit away (tier 6).
        val r = Search.run("chrame", listOf(chrome, christina))
        assertEquals(listOf("Christina Ramey", "Chrome"), r.blocks.flatMap { b -> b.entries.map { it.value } })
        // The blocks keep the planned order, best tier first, so Go is the contact.
        assertEquals(listOf(Kind.CONTACTS, Kind.APPS), r.blocks.map { it.kind })
        // An app that matched better still hides the app typos ("chromec" is one edit from Chrome)…
        val chromecast = Found(Kind.APPS, "b", listOf(Match.prepare("Chromecast")), "Chromecast")
        assertEquals(listOf("Chromecast"), found("chromec", listOf(chrome, chromecast)))
        // …and a contact's typo still waits for nothing of any kind to match (a-l-c…i-e is in Alchemie in order).
        val alchemie = Found(Kind.APPS, "c", listOf(Match.prepare("Alchemie")), "Alchemie")
        assertEquals(listOf("Alchemie"), found("alcie", listOf(contacts[0], alchemie)))
    }

    @Test
    fun theAppTypoPassLeavesOtherKindsAlone() {
        // "alcie" matches the shortcut in order (a-l-c…i-e), so no other kind gets the typo pass:
        // not Alice Smith (one edit away), and not the shortcut again (also one edit from "alche").
        val shortcut = Found(Kind.SHORTCUTS, "s", listOf(Match.prepare("Alchemie")), "Alchemie")
        val r = Search.run("alcie", listOf(shortcut, contacts[0]))
        assertEquals(listOf("Alchemie"), r.blocks.flatMap { b -> b.entries.map { it.value } })
    }
}
