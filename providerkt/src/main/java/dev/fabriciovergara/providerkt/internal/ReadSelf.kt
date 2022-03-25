package dev.fabriciovergara.providerkt.internal

import dev.fabriciovergara.providerkt.*

internal fun <State> Container.doReadSelf(
    provider: Provider<State>,
    origin: Container,
): State? {
    if (this !== origin) {
        error("Should never happen")
    }

    val state = synchronized(root) {
        val entry = state.getOrNull(provider)
        entry?.state
    }

    return state
}