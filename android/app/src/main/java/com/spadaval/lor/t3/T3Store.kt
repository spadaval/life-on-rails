package com.spadaval.lor.t3

import android.content.Context
import androidx.core.content.edit

/** The paired server and its access token, in app-private SharedPreferences. Never log [accessToken]. */
class T3Store(context: Context) {
    private val p = context.getSharedPreferences("t3", Context.MODE_PRIVATE)

    data class Session(val httpBase: String, val accessToken: String)

    var session: Session?
        get() {
            val base = p.getString("http_base", null) ?: return null
            val token = p.getString("access_token", null) ?: return null
            return Session(base, token)
        }
        set(value) = p.edit {
            if (value == null) {
                remove("access_token")
            } else {
                putString("http_base", value.httpBase)
                putString("access_token", value.accessToken)
            }
        }

    /** The last server paired with, kept after forgetting the token so it can be re-paired. */
    val lastServer: String get() = p.getString("http_base", null) ?: ""
}
