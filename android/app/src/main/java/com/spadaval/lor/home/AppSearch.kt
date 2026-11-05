package com.spadaval.lor.home

import java.text.Normalizer
import java.util.Locale

/**
 * Finding an app by name in the drawer.
 *
 * Matching follows Launcher3 / Lawnchair (`search/StringMatcherUtility.java`, Apache-2.0): the
 * query must be a prefix of the label at a word break, where a break is the start, a character
 * after a space or symbol, a digit after a non-digit, or a capital after a lower-case letter, so
 * "tube" finds YouTube. Normalisation follows Kvaesitso
 * (`CompatStringNormalizer`, GPL-3.0): lower case with accents stripped, so "cafe" finds Café.
 * Ranking is ours: a label that starts with the query first, then a later word, then (for queries
 * of three or more characters) anywhere in the label; ties keep the input's alphabetical order.
 */
object AppSearch {
    fun <T> search(query: String, items: List<T>, label: (T) -> String): List<T> {
        val q = normalize(query.trim())
        if (q.isEmpty()) return items
        return items
            .mapNotNull { item -> rank(q, label(item))?.let { it to item } }
            .sortedBy { it.first }
            .map { it.second }
    }

    /** 0: the label starts with [q]; 1: a later word does; 2: it contains [q] (3+ characters); null: no match. */
    fun rank(q: String, label: String): Int? {
        if (normalize(label).startsWith(q)) return 0
        for (i in 1 until label.length) {
            if (isBreak(label, i) && normalize(label.substring(i)).startsWith(q)) return 1
        }
        if (q.length >= 3 && normalize(label).contains(q)) return 2
        return null
    }

    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(Locale.ROOT), Normalizer.Form.NFD).replace(MARKS, "")

    private val MARKS = Regex("\\p{Mn}+")

    private fun isBreak(s: String, i: Int): Boolean {
        val prev = s[i - 1]
        val cur = s[i]
        if (!cur.isLetterOrDigit()) return false
        return !prev.isLetterOrDigit() ||
            (cur.isDigit() && !prev.isDigit()) ||
            (cur.isUpperCase() && (prev.isLowerCase() || prev.isDigit()))
    }
}
