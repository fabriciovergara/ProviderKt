package dev.fabriciovergara.providerkt.internal

import dev.fabriciovergara.providerkt.*

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
        val shouldDispose = provider.shouldDispose()
        if (shouldDispose) {
            state = state - provider.key
        }
        {
            if (shouldDispose) {
                extras.observers.onDisposed(provider, entry.state)
                entry.onDisposed(entry.state)
            }
        }
    }?.invoke()
}
