package com.providerkt.internal

import com.providerkt.Dispose
import com.providerkt.Listener
import com.providerkt.Provider
import com.providerkt.ProviderType

internal fun <State> Container.doListen(
    provider: Provider<State>,
    block: Listener<State>,
    origin: Container,
    extras: ContainerExtras,
): Dispose {
    val providerOverride = extras.overrides.getOrNull(provider)
    if (providerOverride != null) {
        return listenInternal(providerOverride, block)
    }

    if (parent == null) {
        return listenInternal(provider, block)
    }

    return parent.doListen(provider, block, origin, extras)
}

private fun <State> Container.listenInternal(
    provider: Provider<State>,
    block: Listener<State>,
): Dispose {
    val listener = { block(read(provider)) }
    provider.addListener(listener)
    listener.invoke()
    return { dispose(provider, listener) }
}
