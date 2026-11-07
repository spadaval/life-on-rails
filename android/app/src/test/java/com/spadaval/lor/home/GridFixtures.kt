package com.spadaval.lor.home

/*
 * The default grid's places, for tests written against version 1's pages (lor-euec): page 0 is
 * home's `apps` block, the second block of the stack at [1, 0]; page n after it is the `apps`
 * block of the stack at [1 + n, 0].
 */

val HOME_APPS = BlockAt(Cell(1, 0), 1)

/** The `apps` block version 1's page [n] converts to. */
fun page(n: Int): BlockAt = if (n == 0) HOME_APPS else BlockAt(Cell(1 + n, 0), 0)

/** Home's items: what version 1 called `home`. */
val Layout.home: List<LayoutItem> get() = items(HOME_APPS)!!

/** A version 2 layout file of one layout "a" with one stack at [0, 0] holding [blocks] (JSON). */
fun v2Stack(blocks: String, extra: String = ""): String =
    """{"version": 2, "active": "a", $extra "layouts": [{"name": "a", "start": {"narrow": [0, 0]}, "panels": [{"at": [0, 0], "type": "stack", "blocks": $blocks}]}]}"""

/** A version 2 layout file of one layout "a" whose home holds nothing, with [extra] top-level fields. */
fun v2Empty(extra: String = ""): String = v2Stack("""[{"type": "apps"}]""", extra)
