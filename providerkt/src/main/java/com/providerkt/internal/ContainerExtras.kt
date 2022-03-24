package com.providerkt.internal

import com.providerkt.Provider
import com.providerkt.ProviderKey
import com.providerkt.ProviderObserver

internal typealias OverrideMap = Map<ProviderKey, Provider<*>>
internal typealias ObserverSet = Set<ProviderObserver>

internal data class ContainerExtras(
    val overrides: OverrideMap = emptyMap(),
    val observers: ObserverSet = emptySet(),
)

internal operator fun ContainerExtras.plus(extras: ContainerExtras): ContainerExtras = copy(
    overrides = overrides + extras.overrides,
    observers = observers + extras.observers
)

internal fun <State> OverrideMap.getOrNull(
    provider: Provider<State>
): Provider<State>? {
    val value = get(provider.key)
    return if (value != null) {
        @Suppress("UNCHECKED_CAST")
        value as Provider<State>
    } else {
        null
    }
}

internal fun ObserverSet.onCreated(provider: Provider<*>, value: Any?) {
    forEach { it.onCreated(provider, value) }
}

internal fun ObserverSet.onUpdated(provider: Provider<*>, old: Any?, value: Any?) {
    forEach { it.onUpdated(provider, old, value) }
}

internal fun ObserverSet.onDisposed(provider: Provider<*>, value: Any?) {
    forEach { it.onDisposed(provider, value) }
}