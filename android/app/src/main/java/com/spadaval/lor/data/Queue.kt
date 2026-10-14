package com.spadaval.lor.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import org.json.JSONObject

/**
 * Observations waiting to be uploaded. Keyed by observation id, so collecting
 * the same thing twice (a session that grew longer) replaces the waiting copy.
 * Rows leave only when the server has accepted them.
 */
class Queue(context: Context) : SQLiteOpenHelper(context, "queue.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE pending (id TEXT PRIMARY KEY, json TEXT NOT NULL, added INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    fun put(items: List<JSONObject>) {
        if (items.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            val now = System.currentTimeMillis()
            for (item in items) {
                val values = ContentValues().apply {
                    put("id", item.getString("id"))
                    put("json", item.toString())
                    put("added", now)
                }
                db.insertWithOnConflict("pending", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    /** The oldest waiting observations, as (id, json). */
    fun take(limit: Int): List<Pair<String, String>> {
        val out = mutableListOf<Pair<String, String>>()
        readableDatabase.rawQuery("SELECT id, json FROM pending ORDER BY added, id LIMIT ?", arrayOf(limit.toString())).use { c ->
            while (c.moveToNext()) out += c.getString(0) to c.getString(1)
        }
        return out
    }

    fun remove(ids: Collection<String>) {
        if (ids.isEmpty()) return
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (id in ids) db.delete("pending", "id = ?", arrayOf(id))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun count(): Long = readableDatabase.rawQuery("SELECT count(*) FROM pending", null).use { c -> if (c.moveToFirst()) c.getLong(0) else 0L }
}
