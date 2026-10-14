package com.spadaval.lor.collect

import android.content.Context
import android.net.ConnectivityManager
import android.os.Environment
import com.spadaval.lor.data.Prefs
import com.spadaval.lor.net.LorClient
import java.io.File

/**
 * WhatsApp's nightly local backup, uploaded as it is (still encrypted) for the
 * server to decrypt with the owner's 64-digit key. Not an observation sensor:
 * the server turns the backup into observations. Needs "All files access",
 * which reaches Android/media but not WhatsApp's private folder.
 */
class WhatsAppBackup(private val context: Context, private val prefs: Prefs) {

    fun permitted(): Boolean = Environment.isExternalStorageManager()

    fun file(): File = File(Environment.getExternalStorageDirectory(), PATH)

    /** Uploads the backup if it changed since the last upload and the network is unmetered. Returns a line for the sync summary, or null. */
    fun uploadIfChanged(client: LorClient): String? {
        val f = file()
        if (!f.isFile) return "no backup found at $PATH: turn on WhatsApp's end-to-end encrypted backup with the 64-digit key"
        val stamp = "${f.lastModified()}:${f.length()}"
        if (stamp == prefs.whatsappUploaded) return null
        val cm = context.getSystemService(ConnectivityManager::class.java)
        if (cm.isActiveNetworkMetered) return "WhatsApp backup waiting for Wi-Fi"
        client.uploadWhatsappBackup(prefs.deviceName, f)
        prefs.whatsappUploaded = stamp
        return "WhatsApp backup uploaded (${f.length() / 1_000_000} MB)"
    }

    companion object {
        const val PATH = "Android/media/com.whatsapp/WhatsApp/Databases/msgstore.db.crypt15"
    }
}
