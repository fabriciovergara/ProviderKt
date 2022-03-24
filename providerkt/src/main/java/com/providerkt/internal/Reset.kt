package com.providerkt.internal

import com.providerkt.Provider

internal fun <State> Container.doReset(
    provider: Provider<State>,
    origin: Container,
    extras: ContainerExtras,
) {
    val providerOverride = extras.overrides.getOrNull(provider)
    if (providerOverride != null) {
        return resetInternal(providerOverride, origin, extras)
    }

    if (parent == null) {
        return resetInternal(provider, origin, extras)
    }

    return parent.doReset(provider, origin, extras)
}

private fun <State> Container.resetInternal(
    provider: Provider<State>,
    origin: Container,
    extras: ContainerExtras,
) {
    synchronized(root) {
        val entry = state.getOrNull(provider)
        if (entry != null) {
            val ref = ContainerRef(container = origin, origin = provider)
            val newEntry = ContainerEntry(state = provider.create(ref), type = provider.type) {
                ref.onDisposed()
            }

            state = state + (provider.key to newEntry)
            {
                entry.dispose()
                extras.observers.onDisposed(provider, entry.state)
                provider.notifyListeners()
            }
        } else {
            null
        }
    }?.invoke()
}
