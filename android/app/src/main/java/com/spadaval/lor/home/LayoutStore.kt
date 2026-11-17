package com.spadaval.lor.home

import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** The layout file on disk. No Android here, so the JVM tests can reach it. */
class LayoutStore(val file: File) {
    /**
     * A parsed file and the modified time it was read or written at. After an edit that wrote,
     * [undo] holds what [undo] needs to put the file back (launcher polish 4.2).
     */
    data class Read(val layout: LayoutFile, val stamp: Long, val undo: Undo? = null)

    /** The file's text before an edit, and the text the edit wrote. */
    data class Undo(val before: String, val wrote: String)

    /**
     * The text of the last file that parsed (launcher polish 3.1), so a file that is broken when
     * the process starts still has a layout to show beside its error. A cache, not part of the schema.
     */
    val good = File(file.parentFile, "layout.good.json")

    /** Where [restore] sets a broken file aside, for whoever wrote it to read. */
    val broken = File(file.parentFile, "layout.json.broken")

    /**
     * Reads and parses the file. When it doesn't exist, writes [default] first; with no default
     * (the app list hasn't loaded), throws instead, so no layout is made from a partial list.
     */
    fun read(default: (() -> LayoutFile)?): Read {
        if (!file.exists()) {
            val make = default ?: throw LayoutError("the app list didn't load, so there is no default layout yet")
            write(make())
        }
        return readText().first
    }

    /** The file parsed, and its text exactly as it is on disk. */
    private fun readText(): Pair<Read, String> {
        val stamp = file.lastModified()
        if (file.length() > LayoutFile.MAX_BYTES) throw LayoutError("the file is over ${LayoutFile.MAX_BYTES / 1024} KB")
        val text = file.readText()
        val layout = LayoutFile.parse(text)
        keepGood(text)
        return Read(layout, stamp) to text
    }

    /**
     * What home shows while the file is broken (lor-bdus's review, R3): the last good file, and the
     * old dock apps it has no room for, which [LayoutFile.parseLeavingOut] left out. With any left
     * out it is for showing only: [readGood], which a restore writes from, is null then. Null when
     * there is no copy or it doesn't parse even so.
     */
    fun readLastGood(): LastGood? = try {
        if (good.exists()) LayoutFile.parseLeavingOut(good.readText()).let { (layout, left) -> LastGood(layout, left) } else null
    } catch (e: LayoutError) {
        null
    } catch (e: IOException) {
        null
    }

    /** The last good file as shown, and the old dock apps left out of it ([readLastGood]). */
    data class LastGood(val layout: LayoutFile, val left: List<AppRef>)

    /** The last file that parsed, or null when there is none or it doesn't parse either. */
    fun readGood(): LayoutFile? = try {
        if (good.exists()) LayoutFile.parse(good.readText()) else null
    } catch (e: LayoutError) {
        null
    } catch (e: IOException) {
        null
    }

    /**
     * Rereads the file, applies [change], and writes the result if it parses back to the same
     * thing; otherwise the file is left as it was. Fails with the [LayoutError] or I/O error. When
     * [change] changes nothing, nothing is written and the result has no [Read.undo]. While the
     * file is locked, an edit whose result differs in `active` or `layouts` is refused with [LOCKED].
     */
    fun edit(change: (LayoutFile) -> LayoutFile): Result<Read> = runCatching {
        if (!file.exists()) throw LayoutError("there is no layout file yet")
        val (read, before) = readText()
        val changed = change(read.layout)
        if (changed == read.layout) return@runCatching read
        // The layout lock (decision lor-73li): one rule on what the edit changes, whatever code made it.
        if (read.layout.locked && (changed.active != read.layout.active || changed.layouts != read.layout.layouts)) throw LayoutError(LOCKED)
        val stamp = write(changed)
        Read(changed, stamp, Undo(before, changed.toJson() + "\n"))
    }

    /**
     * Puts the file back as it was before the edit [undo] came from, byte for byte: the copy of
     * its text, not an inverse edit, so a hand-formatted file comes back as it was written. Refuses
     * when the file no longer holds what that edit wrote (an agent or adb wrote since).
     */
    fun undo(undo: Undo): Read {
        if (!file.exists() || file.readText() != undo.wrote) throw LayoutError("layout.json changed since; nothing was undone")
        val layout = LayoutFile.parse(undo.before)
        writeAtomically(file, undo.before)
        keepGood(undo.before)
        return Read(layout, file.lastModified())
    }

    /**
     * Puts a broken file right: renames it to [broken] (replacing an older one), then writes
     * [fallback] in its place. Returns the new modified time.
     */
    fun restore(fallback: LayoutFile): Long {
        if (file.exists() && !file.renameTo(broken)) throw IOException("can't set ${file.path} aside")
        return write(fallback)
    }

    /**
     * Serialises [layout], checks it parses back to itself, writes a temporary file and renames it
     * over the file. Returns the new modified time.
     */
    fun write(layout: LayoutFile): Long {
        val text = layout.toJson() + "\n"
        val back = LayoutFile.parse(text)
        if (back != layout) throw LayoutError("the edited layout doesn't read back the same")
        writeAtomically(file, text)
        keepGood(text)
        return file.lastModified()
    }

    /**
     * Copies [text] to [good] unless it already holds it, so most reads write nothing. The copy is
     * a cache: failing to write it (a full disk) must not fail the read or edit it follows.
     */
    private fun keepGood(text: String) {
        try {
            if (good.exists() && good.readText() == text) return
            writeAtomically(good, text)
        } catch (e: IOException) {
            // Kept as it was; the next read or write tries again.
        }
    }
}

/**
 * Writes [text] to a temporary file beside [file] and renames it over [file]. Synced before the
 * rename, so a crash or power loss leaves the old file or the new one, never a partly written one
 * (f2fs, Samsung's /data, can otherwise rename unwritten data).
 */
fun writeAtomically(file: File, text: String) {
    val tmp = File(file.path + ".tmp")
    FileOutputStream(tmp).use { out ->
        out.write(text.toByteArray())
        out.fd.sync()
    }
    if (!tmp.renameTo(file)) throw IOException("can't replace ${file.path}")
}
