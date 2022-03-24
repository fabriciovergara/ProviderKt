package com.providerkt.internal

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

private val UNINITIALIZED_VALUE = Any()

internal fun <T> cached(block: (KProperty<*>) -> T) = object : ReadOnlyProperty<Any?, T> {

    private var value: Any? = UNINITIALIZED_VALUE

    override fun getValue(thisRef: Any?, property: KProperty<*>): T {
        if (value === UNINITIALIZED_VALUE) {
            value = block(property)
        }

        @Suppress("UNCHECKED_CAST")
        return value as T
    }
}

internal fun <T> sync(lock: Any, initial: T) = object : ReadWriteProperty<Any?, T> {

    private var value: T = initial

    override fun getValue(thisRef: Any?, property: KProperty<*>): T =
        synchronized(lock) {
            value
        }

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) =
        synchronized(lock) {
            this.value = value
        }
}