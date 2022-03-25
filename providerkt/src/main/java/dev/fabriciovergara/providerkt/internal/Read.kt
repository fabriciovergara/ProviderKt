package dev.fabriciovergara.providerkt.internal

import dev.fabriciovergara.providerkt.*

internal fun <State> Container.doRead(
    provider: Provider<State>,
    origin: Container,
    extras: ContainerExtras,
): State {
    val providerOverride = extras.overrides.getOrNull(provider)
    if (providerOverride != null) {
        return readInternal(providerOverride, origin, extras)
    }

    if (parent == null) {
        return readInternal(provider, origin, extras)
    }

    return parent.doRead(provider, origin, extras)
}

private fun <State> Container.readInternal(
    provider: Provider<State>,
    origin: Container,
    extras: ContainerExtras,
): State {
    val (state, onCreated) = synchronized(root) {
        val cachedEntry = state.getOrNull(provider)
        val entry = if (cachedEntry != null) {
            cachedEntry
        } else {
            val ref = ContainerRef(container = origin, origin = provider)
            ContainerEntry(ref = ref)
        }

        val shouldDispose = provider.shouldDispose()
        state = if (shouldDispose) {
            state - provider.key
        } else {
            state + (provider.key to entry)
        }

        entry.state to {
            if (cachedEntry == null) {
                extras.observers.onCreated(provider, entry.state)
            }

            if (shouldDispose) {
                extras.observers.onDisposed(provider, entry.state)
                entry.onDisposed(entry.state)
            }

        }
    }

    onCreated()
    return state
}