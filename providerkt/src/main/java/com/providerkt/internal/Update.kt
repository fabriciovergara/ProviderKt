package com.providerkt.internal

import com.providerkt.Provider

internal fun <State> Container.doUpdate(
    provider: Provider<State>,
    next: State,
    origin: Container,
    extras: ContainerExtras,
) {
    val providerOverride = extras.overrides.getOrNull(provider)
    if (providerOverride != null) {
        return updateInternal(providerOverride, next, origin, extras)
    }

    if (parent == null) {
        return updateInternal(provider, next, origin, extras)
    }

    return parent.doUpdate(provider, next, origin, extras)
}

private fun <State> Container.updateInternal(
    provider: Provider<State>,
    next: State,
    origin: Container,
    extras: ContainerExtras,
) {
    synchronized(root) {
        val cachedEntry = state.getOrNull(provider)
        val entry = if (cachedEntry != null) {
            cachedEntry
        } else {
            val ref = ContainerRef(container = origin, origin = provider)
            ContainerEntry(state = provider.create(ref), type = provider.type) {
                ref.onDisposed()
            }
        }

        val newEntry = entry.copy(state = next)
        state = state + (provider.key to newEntry)
        {
            if (cachedEntry != null) {
                extras.observers.onUpdated(provider, entry.state, newEntry.state)
            } else {
                extras.observers.onCreated(provider, newEntry.state)
            }

            provider.notifyListeners()
        }
    }.invoke()
}