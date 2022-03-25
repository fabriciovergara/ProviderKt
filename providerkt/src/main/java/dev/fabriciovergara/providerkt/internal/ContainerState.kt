package dev.fabriciovergara.providerkt.internal

import dev.fabriciovergara.providerkt.*

internal typealias ContainerState = Map<ProviderKey, ContainerEntry<*>>

internal data class ContainerEntry<State>(
    val state: State,
    val onDisposed: TypedCallback<State>,
    val onUpdated: TypedCallback<State>,
) {
    constructor(ref: ContainerRef<State>) : this(
        state = ref.create(),
        onDisposed = ref::dispose,
        onUpdated = ref::update
    )
}

internal fun <State> ContainerState.getOrNull(
    provider: Provider<State>,
): ContainerEntry<State>? {
    val value = get(provider.key)
    return if (value != null) {
        @Suppress("UNCHECKED_CAST")
        value as ContainerEntry<State>
    } else {
        null
    }
}