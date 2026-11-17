package com.spadaval.lor.lab

import android.app.Activity
import android.content.Intent

/** A release build opens no fixture: the debug build's `lab_state` entry (lor-oech) has no path here. */
object LabStates {
    /** The release build has no entry, so the app starts its services as it always has. */
    const val ENABLED = false

    const val PROCESS = ":lab"

    @Suppress("UNUSED_PARAMETER")
    fun handsOff(intent: Intent?): Boolean = false

    @Suppress("UNUSED_PARAMETER")
    fun open(from: Activity?, intent: Intent?): Boolean = false
}
