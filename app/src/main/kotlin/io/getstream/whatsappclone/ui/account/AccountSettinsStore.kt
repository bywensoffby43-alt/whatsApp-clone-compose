package io.getstream.whatsappclone.account

import android.content.Context

/** Local settings are usable immediately; remote sync can be added when Supabase/API is configured. */
class AccountSettingsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("wenschat_account", Context.MODE_PRIVATE)

    fun profileName(): String = prefs.getString("profile_name", "") ?: ""
    fun profileAbout(): String = prefs.getString("profile_about", "") ?: ""
    fun saveProfile(name: String, about: String) {
        prefs.edit().putString("profile_name", name.trim()).putString("profile_about", about.trim()).apply()
    }

    fun privacy(key: String, default: Boolean = true): Boolean = prefs.getBoolean("privacy_$key", default)
    fun setPrivacy(key: String, enabled: Boolean) { prefs.edit().putBoolean("privacy_$key", enabled).apply() }

    fun theme(): String = prefs.getString("app_theme", "SYSTEM") ?: "SYSTEM"
    fun setTheme(value: String) { prefs.edit().putString("app_theme", value).apply() }
}
