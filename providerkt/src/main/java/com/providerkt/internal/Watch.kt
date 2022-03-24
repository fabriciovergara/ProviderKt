package com.providerkt.internal

import com.providerkt.Dispose
import com.providerkt.Provider

internal fun <State, WState> Container.doWatch(
    self: Provider<State>,
    provider: Provider<WState>,
    origin: Container,
) {
    var dispose: Dispose? = null
    dispose = listen(provider) {
        dispose?.run {
            invoke()
            reset(self, origin)
        }
    }
}

private fun <State> Container.reset(
    provider: Provider<State>,
    origin: Container,
) {
    val providerOverride = extras.overrides.getOrNull(provider)
    if (providerOverride != null) {
        return resetInternal(providerOverride, origin)
    }

    if (parent == null) {
        return resetInternal(provider, origin)
    }

    return parent.reset(provider, origin)
}

private fun <State> Container.resetInternal(
    provider: Provider<State>,
    origin: Container,
) {
    synchronized(lock) {
        val entry = state.getOrNull(provider)
        if (entry != null) {
            val ref = ContainerRef(container = origin, origin = provider)
            val newEntry = ContainerEntry(state = provider.create(ref), type = provider.type) {
                ref.onDisposed()
            }

            state = state + (provider.key to newEntry)
            {
                entry.dispose()
                origin.extras.observers.onDisposed(provider, entry.state)
                synchronized(provider) { provider.listeners }.forEach { it() }
            }
        } else {
            null
        }
    }?.invoke()
}
