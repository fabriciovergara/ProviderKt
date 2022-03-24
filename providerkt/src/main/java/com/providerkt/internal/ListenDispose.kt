package com.providerkt.internal

import com.providerkt.Provider
import com.providerkt.ProviderType

internal fun <State> Container.doListenDispose(
    provider: Provider<State>,
    listener: () -> Unit,
    extras: ContainerExtras,
) {
    val providerOverride = extras.overrides.getOrNull(provider)
    if (providerOverride != null) {
        return listenDisposeInternal(providerOverride, listener, extras)
    }

    if (parent == null) {
        return listenDisposeInternal(provider, listener, extras)
    }

    return parent.doListenDispose(provider, listener, extras)
}

private fun <State> Container.listenDisposeInternal(
    provider: Provider<State>,
    listener: () -> Unit,
    extras: ContainerExtras,
) {
    provider.removeListener(listener)
    synchronized(root) {
        val entry = state.getOrNull(provider) ?: return@synchronized null
        if (entry.type == ProviderType.Disposable && provider.isEmpty()) {
            state = state - provider.key
            {
                extras.observers.onDisposed(provider, entry.state)
                entry.dispose()
            }
        } else {
            null
        }
    }?.invoke()
}
