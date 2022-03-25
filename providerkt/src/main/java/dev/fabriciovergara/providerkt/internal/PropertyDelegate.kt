package dev.fabriciovergara.providerkt.internal

import kotlin.properties.ReadOnlyProperty
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