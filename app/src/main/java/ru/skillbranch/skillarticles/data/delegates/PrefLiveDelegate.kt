package ru.skillbranch.skillarticles.data.delegates

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefLiveDelegate<T>(
    private val fieldKey: String,
    private val defaultValue: T,
    private val preferences: SharedPreferences
): ReadWriteProperty<Any?, LiveData<T>> {
    private var storedValue: LiveData<T>? = null

    override fun getValue(thisRef: Any?, property: KProperty<*>): LiveData<T> {
        if (storedValue == null){
            storedValue = SharedPreferenceLiveData(preferences, fieldKey, defaultValue)
        }

        return storedValue!!
    }


}

internal class SharedPreferenceLiveData<T>(
    var sharedPrefs: SharedPreferences,
    var key: String,
    var defValue: T
) : LiveData<T>() {
    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener {_, shKey ->
            if (shKey == key) {
                value = readValue(defValue)
            }
        }

    override fun onActive() {
        sharedPrefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
        super.onActive()
    }

    override fun onInactive() {
        sharedPrefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
        super.onInactive()
    }

    @Suppress("UNCHECKED_CAST")
    private fun readValue(defValue: T): T? {
        return when(defValue) {
            is Int -> sharedPrefs.getInt(key, defValue as Int) as T
            is Long -> sharedPrefs.getLong(key, defValue as Long) as T
            is Float -> sharedPrefs.getFloat(key, defValue as Float) as T
            is String -> sharedPrefs.getString(key, defValue as String) as T
            is Boolean -> sharedPrefs.getBoolean(key, defValue as Boolean) as T
            else -> error("This type $defValue can not to stored into Preferences")
        }
    }
}