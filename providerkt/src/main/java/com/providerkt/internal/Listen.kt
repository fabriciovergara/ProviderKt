package com.providerkt.internal

import com.providerkt.VoidCallback
import com.providerkt.TypedCallback
import com.providerkt.Provider

internal fun <State> Container.doListen(
    provider: Provider<State>,
    block: TypedCallback<State>,
    origin: Container,
    extras: ContainerExtras,
): VoidCallback {
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
    block: TypedCallback<State>,
): VoidCallback {
    val listener = { block(read(provider)) }
    provider.addListener(listener)
    listener.invoke()
    return { dispose(provider, listener) }
}
