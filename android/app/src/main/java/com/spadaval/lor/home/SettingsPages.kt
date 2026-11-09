package com.spadaval.lor.home

/*
 * Settings pages in search (launcher polish 5.3). Lawnchair (Apache-2.0, 15-dev,
 * `search/algorithms/engine/provider/SettingsSearchProvider.kt`) finds pages by reflecting over
 * `Settings.ACTION_*` and matching the constants' names; here the same intents, in a table with
 * Android's own titles and a few words people type for them. A row whose intent nothing takes on
 * this phone is dropped when search's sources are built ([HomeStore]), so a page One UI lacks
 * goes missing instead of failing.
 */

/** A page of settings: its [title], other words for it, and the intent [action] that opens it, or one of LoR's own. */
data class SettingsPage(val title: String, val aliases: List<String>, val action: String) {
    /** Opened inside LoR, not by another app. */
    val lor: Boolean get() = action == LOR_HOME || action == LOR_APP

    companion object {
        const val LOR_HOME = "lor:home-settings"
        const val LOR_APP = "lor:app-settings"

        val ALL = listOf(
            SettingsPage("Wi-Fi", listOf("wifi", "wireless", "internet"), "android.settings.WIFI_SETTINGS"),
            SettingsPage("Bluetooth", listOf("bt"), "android.settings.BLUETOOTH_SETTINGS"),
            SettingsPage("Network & internet", listOf("mobile network", "cellular", "sim"), "android.settings.WIRELESS_SETTINGS"),
            SettingsPage("Data usage", listOf("mobile data"), "android.settings.DATA_USAGE_SETTINGS"),
            SettingsPage("Airplane mode", listOf("flight mode", "aeroplane"), "android.settings.AIRPLANE_MODE_SETTINGS"),
            SettingsPage("Display", listOf("brightness", "dark theme", "screen timeout", "font size"), "android.settings.DISPLAY_SETTINGS"),
            SettingsPage("Night Light", listOf("blue light"), "android.settings.NIGHT_DISPLAY_SETTINGS"),
            SettingsPage("Sound & vibration", listOf("volume", "ringtone", "vibrate"), "android.settings.SOUND_SETTINGS"),
            SettingsPage("Notifications", emptyList(), "android.settings.NOTIFICATION_SETTINGS"),
            SettingsPage("Do Not Disturb", listOf("dnd", "focus", "quiet"), "android.settings.ZEN_MODE_SETTINGS"),
            SettingsPage("Apps", listOf("applications", "all apps"), "android.settings.MANAGE_APPLICATIONS_SETTINGS"),
            SettingsPage("Default apps", listOf("default browser"), "android.settings.MANAGE_DEFAULT_APPS_SETTINGS"),
            SettingsPage("Default home app", listOf("launcher", "home app"), "android.settings.HOME_SETTINGS"),
            SettingsPage("Battery", listOf("power", "battery saver"), "android.intent.action.POWER_USAGE_SUMMARY"),
            SettingsPage("Storage", listOf("space", "free up"), "android.settings.INTERNAL_STORAGE_SETTINGS"),
            SettingsPage("Location", listOf("gps"), "android.settings.LOCATION_SOURCE_SETTINGS"),
            SettingsPage("Security", listOf("lock screen", "fingerprint", "screen lock"), "android.settings.SECURITY_SETTINGS"),
            SettingsPage("Privacy", listOf("permissions"), "android.settings.PRIVACY_SETTINGS"),
            SettingsPage("Accounts", listOf("passwords", "sync"), "android.settings.SYNC_SETTINGS"),
            SettingsPage("Date & time", listOf("time zone"), "android.settings.DATE_SETTINGS"),
            SettingsPage("Languages", listOf("language", "locale"), "android.settings.LOCALE_SETTINGS"),
            SettingsPage("Keyboard", listOf("input method", "on-screen keyboard"), "android.settings.INPUT_METHOD_SETTINGS"),
            SettingsPage("Accessibility", emptyList(), "android.settings.ACCESSIBILITY_SETTINGS"),
            SettingsPage("Developer options", listOf("developer", "usb debugging", "adb"), "android.settings.APPLICATION_DEVELOPMENT_SETTINGS"),
            SettingsPage("About phone", listOf("device info", "android version", "build number"), "android.settings.DEVICE_INFO_SETTINGS"),
            SettingsPage("NFC", listOf("tap to pay", "contactless"), "android.settings.NFC_SETTINGS"),
            SettingsPage("Cast", listOf("screen cast", "chromecast"), "android.settings.CAST_SETTINGS"),
            SettingsPage("VPN", emptyList(), "android.settings.VPN_SETTINGS"),
            SettingsPage("Usage access", emptyList(), "android.settings.USAGE_ACCESS_SETTINGS"),
            SettingsPage("Home settings", listOf("launcher settings", "hidden apps", "drawer tabs"), LOR_HOME),
            SettingsPage("LoR app settings", listOf("life on rails"), LOR_APP),
        )

        /** [page] as search finds it: by its title and its aliases. */
        fun <T> found(page: SettingsPage, value: T) =
            Found(Kind.SETTINGS, "set:${page.title}", (listOf(page.title) + page.aliases).map { Match.prepare(it) }, value)
    }
}
