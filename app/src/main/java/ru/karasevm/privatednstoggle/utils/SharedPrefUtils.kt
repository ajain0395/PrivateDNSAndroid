package ru.karasevm.privatednstoggle.utils

import android.content.Context
import android.content.SharedPreferences

object PreferenceHelper {

    private const val DNS_SERVERS = "dns_servers"
    private const val AUTO_MODE = "auto_enabled"

    fun defaultPreference(context: Context): SharedPreferences =
        context.getSharedPreferences("app_prefs", 0)

    private inline fun SharedPreferences.editMe(operation: (SharedPreferences.Editor) -> Unit) {
        val editMe = edit()
        operation(editMe)
        editMe.apply()
    }

    private fun SharedPreferences.Editor.put(pair: Pair<String, Any>) {
        val key = pair.first
        when (val value = pair.second) {
            is String -> putString(key, value)
            is Int -> putInt(key, value)
            is Boolean -> putBoolean(key, value)
            is Long -> putLong(key, value)
            is Float -> putFloat(key, value)
            else -> error("Only primitive types can be stored in SharedPreferences")
        }
    }

    var SharedPreferences.dns_servers
        get() = getString(DNS_SERVERS, "")!!.split(",").toMutableList()
        set(items) {
            editMe {
                it.put(DNS_SERVERS to items.joinToString(separator = ","))
            }
        }

    var SharedPreferences.autoMode
        get() = getBoolean(AUTO_MODE, false)
        set(value) {
            editMe {
                it.put(AUTO_MODE to value)
            }
        }
}
