package com.providerkt.internal

import com.providerkt.Dispose
import com.providerkt.Provider
import com.providerkt.ProviderKey
import com.providerkt.ProviderType

internal typealias ContainerState = Map<ProviderKey, ContainerEntry<*>>

internal data class ContainerEntry<State>(
    val state: State,
    val type: ProviderType,
    val dispose: Dispose,
)

internal fun <State> ContainerState.getOrNull(
    provider: Provider<State>,
): ContainerEntry<State>? {
    val value = get(provider.key)
    return if (value != null) {
        @Suppress("UNCHECKED_CAST")
        value as ContainerEntry<State>
    } else {
        null
    }
}