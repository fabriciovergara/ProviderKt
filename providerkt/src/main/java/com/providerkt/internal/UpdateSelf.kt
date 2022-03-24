package com.providerkt.internal

import com.providerkt.Provider

internal fun <State> Container.doUpdateSelf(
    provider: Provider<State>,
    next: State,
    origin: Container
) {
    synchronized(lock) {
        val entry = state.getOrNull(provider)
        if (entry != null) {
            val newEntry = entry.copy(state = next)
            state = state + (provider.key to newEntry)
            {
                origin.extras.observers.onUpdated(provider, entry.state, newEntry.state)
                synchronized(provider) { provider.listeners }.forEach { it() }
            }
        } else {
            null
        }
    }?.invoke()
}
