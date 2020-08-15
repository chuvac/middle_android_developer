package ru.skillbranch.skillarticles.data.delegates

import ru.skillbranch.skillarticles.data.local.PrefManager
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

class PrefDelegate<T>(private val defaultValue: T) : ReadWriteProperty<PrefManager, T?> {
    private var value = defaultValue

    override fun getValue(thisRef: PrefManager, property: KProperty<*>): T? {
        return value
    }

    override fun setValue(thisRef: PrefManager, property: KProperty<*>, value: T?) {
        if (value != null) this.value = value
    }

}