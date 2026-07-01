package com.example.data.pref

import android.content.Context
import android.content.SharedPreferences

class UserPrefs(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("lab_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_PASSWORD = "lab_password"
        private const val KEY_LOGGED_IN = "is_logged_in"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_LOGGED_MOBILE = "logged_mobile"
        
        // Lab Profile
        private const val KEY_LAB_NAME = "lab_name"
        private const val KEY_LAB_ADDRESS = "lab_address"
        private const val KEY_LAB_PHONE = "lab_phone"
        private const val KEY_LAB_EMAIL = "lab_email"
        private const val KEY_LAB_LOGO_URI = "lab_logo_uri"

        // App Lock
        private const val KEY_APP_LOCK_PIN = "app_lock_pin"
        private const val KEY_APP_LOCK_ENABLED = "app_lock_enabled"

        // Monthly Lock
        private const val KEY_LOCKED_MONTHS = "locked_months"
    }

    var labName: String
        get() = prefs.getString(KEY_LAB_NAME, "Micro Pathology Lab") ?: "Micro Pathology Lab"
        set(value) = prefs.edit().putString(KEY_LAB_NAME, value).apply()

    var labAddress: String
        get() = prefs.getString(KEY_LAB_ADDRESS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAB_ADDRESS, value).apply()

    var labPhone: String
        get() = prefs.getString(KEY_LAB_PHONE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAB_PHONE, value).apply()

    var labEmail: String
        get() = prefs.getString(KEY_LAB_EMAIL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAB_EMAIL, value).apply()

    var labLogoUri: String
        get() = prefs.getString(KEY_LAB_LOGO_URI, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LAB_LOGO_URI, value).apply()

    var appLockPin: String
        get() = prefs.getString(KEY_APP_LOCK_PIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_APP_LOCK_PIN, value).apply()

    var appLockEnabled: Boolean
        get() = prefs.getBoolean(KEY_APP_LOCK_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_APP_LOCK_ENABLED, value).apply()

    var lockedMonthsString: String
        get() = prefs.getString(KEY_LOCKED_MONTHS, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LOCKED_MONTHS, value).apply()

    fun isMonthLocked(monthYear: String): Boolean {
        val list = lockedMonthsString.split(",").map { it.trim() }
        return list.contains(monthYear)
    }

    fun lockMonth(monthYear: String) {
        val set = lockedMonthsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        set.add(monthYear)
        lockedMonthsString = set.joinToString(",")
    }

    fun unlockMonth(monthYear: String) {
        val set = lockedMonthsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }.toMutableSet()
        set.remove(monthYear)
        lockedMonthsString = set.joinToString(",")
    }

    var password: String
        get() = prefs.getString(KEY_PASSWORD, "admin") ?: "admin"
        set(value) = prefs.edit().putString(KEY_PASSWORD, value).apply()

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(KEY_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(KEY_LOGGED_IN, value).apply()

    var isDarkMode: Boolean?
        get() {
            if (!prefs.contains(KEY_DARK_MODE)) return null
            return prefs.getBoolean(KEY_DARK_MODE, false)
        }
        set(value) {
            if (value == null) {
                prefs.edit().remove(KEY_DARK_MODE).apply()
            } else {
                prefs.edit().putBoolean(KEY_DARK_MODE, value).apply()
            }
        }

    var loggedInMobile: String
        get() = prefs.getString(KEY_LOGGED_MOBILE, "") ?: ""
        set(value) = prefs.edit().putString(KEY_LOGGED_MOBILE, value).apply()

    fun logout() {
        isLoggedIn = false
        loggedInMobile = ""
    }
}
