package com.providerkt.internal

import com.providerkt.Provider

internal fun <State> Container.doUpdateSelf(
    provider: Provider<State>,
    next: State,
    extras: ContainerExtras,
) {
    synchronized(root) {
        val entry = state.getOrNull(provider)
        if (entry != null) {
            val newEntry = entry.copy(state = next)
            state = state + (provider.key to newEntry)
            {
                extras.observers.onUpdated(provider, entry.state, newEntry.state)
                newEntry.onUpdated(newEntry.state)
                provider.notifyListeners()
            }
        } else {
            null
        }
    }?.invoke()
}
