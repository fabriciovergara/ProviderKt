package com.providerkt.internal

import com.providerkt.Dispose
import com.providerkt.Listener
import com.providerkt.Provider
import com.providerkt.ProviderType

internal fun <State> Container.doListen(
    provider: Provider<State>,
    block: Listener<State>,
    origin: Container,
): Dispose {
    val providerOverride = extras.overrides.getOrNull(provider)
    if (providerOverride != null) {
        return listenInternal(providerOverride, block, origin)
    }

    if (parent == null) {
        return listenInternal(provider, block, origin)
    }

    return parent.doListen(provider, block, origin)
}

private fun <State> Container.listenInternal(
    provider: Provider<State>,
    block: Listener<State>,
    origin: Container,
): Dispose {
    val listener = { block(read(provider)) }
    synchronized(provider) {
        provider.listeners += listener
    }

    listener.invoke()
    return {
        disposeListen(provider, listener, origin)
    }
}

private fun <State> Container.disposeListen(
    provider: Provider<State>,
    listener: () -> Unit,
    origin: Container,
) {
    val listeners = synchronized(provider) {
        provider.listeners -= listener
        provider.listeners
    }

    synchronized(lock) {
        val entry = state.getOrNull(provider) ?: return@synchronized null
        if (entry.type == ProviderType.Disposable && listeners.isEmpty()) {
            state = state - provider.key
            {
                origin.extras.observers.onDisposed(provider, entry.state)
                entry.dispose()
            }
        } else {
            null
        }
    }?.invoke()
}
