package icu.nullptr.hidemyapplist.service

import android.content.ComponentName
import android.content.Context.MODE_PRIVATE
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatDelegate
import icu.nullptr.hidemyapplist.hmaApp

object PrefManager {

    private const val PREF_LOCALE = "language"

    private const val PREF_DARK_THEME = "dark_theme"

    private const val PREF_HIDE_ICON = "hide_icon"
    private const val PREF_GLOBAL_HIDING_ENABLED = "global_hiding_enabled"
    private const val PREF_APP_FILTER_SHOW_SYSTEM = "app_filter_show_system"
    private const val PREF_LOG_FILTER_LEVEL = "log_filter_level"

    private val pref = hmaApp.getSharedPreferences("settings", MODE_PRIVATE)

    var locale: String
        get() = pref.getString(PREF_LOCALE, "SYSTEM")!!
        set(value) = pref.edit().putString(PREF_LOCALE, value).apply()

    var darkTheme: Int
        get() = pref.getInt(PREF_DARK_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        set(value) = pref.edit().putInt(PREF_DARK_THEME, value).apply()

    var hideIcon: Boolean
        get() = pref.getBoolean(PREF_HIDE_ICON, false)
        set(value) {
            pref.edit().putBoolean(PREF_HIDE_ICON, value).apply()
            val component = ComponentName(hmaApp, "com.tsng.hidemyapplist.MainActivityLauncher")
            val status =
                if (value) PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                else PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            hmaApp.packageManager.setComponentEnabledSetting(component, status, PackageManager.DONT_KILL_APP)
        }

    var globalHidingEnabled: Boolean
        get() = pref.getBoolean(PREF_GLOBAL_HIDING_ENABLED, true)
        set(value) = pref.edit().putBoolean(PREF_GLOBAL_HIDING_ENABLED, value).apply()

    var showSystemApps: Boolean
        get() = pref.getBoolean(PREF_APP_FILTER_SHOW_SYSTEM, false)
        set(value) = pref.edit().putBoolean(PREF_APP_FILTER_SHOW_SYSTEM, value).apply()

    var logFilterLevel: Int
        get() = pref.getInt(PREF_LOG_FILTER_LEVEL, 0)
        set(value) = pref.edit().putInt(PREF_LOG_FILTER_LEVEL, value).apply()

}
