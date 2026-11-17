package com.spadaval.lor.lab

import android.app.Activity
import android.content.Intent

/** A release build opens no fixture: the debug build's `lab_state` entry (lor-oech) has no path here. */
object LabStates {
    const val PROCESS = ":lab"

    @Suppress("UNUSED_PARAMETER")
    fun open(from: Activity?, intent: Intent?): Boolean = false
}
