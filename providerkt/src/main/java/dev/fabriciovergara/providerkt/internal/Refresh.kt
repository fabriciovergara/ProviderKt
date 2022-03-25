package dev.fabriciovergara.providerkt.internal

import dev.fabriciovergara.providerkt.*

internal fun <State> Container.doRefresh(
    provider: Provider<State>,
    origin: Container,
    extras: ContainerExtras,
) {
    val providerOverride = extras.overrides.getOrNull(provider)
    if (providerOverride != null) {
        return refreshInternal(providerOverride, origin, extras)
    }

    if (parent == null) {
        return refreshInternal(provider, origin, extras)
    }

    return parent.doRefresh(provider, origin, extras)
}

private fun <State> Container.refreshInternal(
    provider: Provider<State>,
    origin: Container,
    extras: ContainerExtras,
) {
    synchronized(root) {
        val entry = state.getOrNull(provider)
        if (entry != null) {
            val ref = ContainerRef(container = origin, origin = provider)
            val newEntry = ContainerEntry(ref = ref)
            val shouldDispose = provider.shouldDispose()
            state = if (shouldDispose) {
                state - provider.key
            } else {
                state + (provider.key to newEntry)
            }
            {
                extras.observers.onDisposed(provider, newEntry.state)
                entry.onDisposed(newEntry.state)
                provider.notifyListeners()
            }
        } else {
            null
        }
    }?.invoke()
}
