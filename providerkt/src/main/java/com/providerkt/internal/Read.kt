package com.providerkt.internal

import com.providerkt.Provider

internal fun <State> Container.doRead(
    provider: Provider<State>,
    origin: Container,
): State {
    val providerOverride = extras.overrides.getOrNull(provider)
    if (providerOverride != null) {
        return readInternal(providerOverride, origin)
    }

    if (parent == null) {
        return readInternal(provider, origin)
    }

    return parent.doRead(provider, origin)
}


private fun <State> Container.readInternal(
    provider: Provider<State>,
    origin: Container,
): State {
    val (state, onCreated) = synchronized(lock) {
        val cachedEntry = state.getOrNull(provider)
        val entry = if (cachedEntry != null) {
            cachedEntry
        } else {
            val ref = ContainerRef(container = origin, origin = provider)
            ContainerEntry(state = provider.create(ref), type = provider.type) {
                ref.onDisposed()
            }
        }

        state = state + (provider.key to entry)
        entry.state to {
            if (cachedEntry == null) {
                origin.extras.observers.onCreated(provider, entry.state)
            }
        }
    }

    onCreated()
    return state
}