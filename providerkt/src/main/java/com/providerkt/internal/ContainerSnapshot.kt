package com.providerkt.internal

import com.providerkt.*
import com.providerkt.ProviderOverrideWithProvider
import com.providerkt.ProviderOverrideWithValue

internal typealias OverrideMap = Map<ProviderKey, Provider<*>>
internal typealias ObserverSet = Set<ProviderObserver>
internal typealias OverrideSet = Set<ProviderOverride<*>>

internal data class ContainerExtras(
    val overrides: OverrideMap = emptyMap(),
    val observers: ObserverSet = emptySet(),
)

internal operator fun ContainerExtras.plus(value: ContainerExtras): ContainerExtras = copy(
    overrides = overrides + value.overrides,
    observers = observers + value.observers
)

internal fun <State> OverrideMap.getOrNull(
    provider: Provider<State>,
): Provider<State>? {
    val value = get(provider.key)
    return if (value != null) {
        @Suppress("UNCHECKED_CAST")
        value as Provider<State>
    } else {
        null
    }
}

internal fun <State> ProviderOverride<State>.toProvider(): Provider<State> = when (this) {
    is ProviderOverrideWithProvider -> Provider(
        name = original.name,
        key = original.key,
        type = original.type,
        create = override.create
    )
    is ProviderOverrideWithValue -> Provider(
        name = original.name,
        key = original.key,
        type = original.type,
        create = { override }
    )
}

internal fun OverrideSet.toOverrideMap(from: OverrideMap = emptyMap()): OverrideMap {
    return associate {
        it.original.key to it.toProvider()
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
