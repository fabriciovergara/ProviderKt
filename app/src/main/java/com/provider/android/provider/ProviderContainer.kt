package com.provider.android.provider

class ProviderContainer(
    internal val parent: ProviderContainer? = null,
    overrides: Set<ProviderOverride<*>> = setOf()
) : ProviderReader, ProviderListener {

    private val overridesMap = overrides.associate {
        it.original.key to it.override
    }

    private var stateMap = mapOf<ProviderKey, ProviderEntry<*>>()

    @Suppress("UNCHECKED_CAST")
    override fun <State> read(provider: Provider<State>): State {
        val providerOverride = overridesMap.getTypedOrNull(provider)
        if (providerOverride != null) {
            return readInternal(providerOverride)
        }

        if (parent == null) {
            return readInternal(provider)
        }

        return parent.read(provider)
    }

    private fun <State> readInternal(provider: Provider<State>): State {
        synchronized(this) {
            val entry = stateMap.getTypedOrCreate(provider, this)
            stateMap = stateMap + (provider.key to entry)
            return entry.state
        }
    }

    override fun <State> listen(provider: Provider<State>, block: (State) -> Unit): Dispose {
        val providerOverride = overridesMap.getTypedOrNull(provider)
        if (providerOverride != null) {
            return listenInternal(providerOverride, block)
        }

        if (parent == null) {
            return listenInternal(provider, block)
        }

        return parent.listen(provider, block)
    }

    private fun <State> listenInternal(
        provider: Provider<State>,
        block: (State) -> Unit
    ): Dispose {
        val listener = { block(read(provider)) }
        synchronized(this) {
            var entry = stateMap.getTypedOrCreate(provider, this)
            entry = entry.copy(listeners = entry.listeners + listener)
            stateMap = stateMap + (provider.key to entry)
        }

        listener.invoke()
        return {
            synchronized(this) {
                var onDisposed: Dispose? = null
                var entryDispose = stateMap.getTypedOrNull(provider)
                if (entryDispose != null) {
                    entryDispose = entryDispose.copy(listeners = entryDispose.listeners - listener)
                    if (entryDispose.shouldDispose) {
                        stateMap = stateMap - provider.key
                        onDisposed = entryDispose.ref.onDisposed
                    } else {
                        stateMap = stateMap + (provider.key to entryDispose)
                    }
                }

                onDisposed
            }?.invoke()
        }
    }

    internal fun <State> watch(origin: Provider<*>, provider: Provider<State>) {
        var dispose: Dispose? = null
        dispose = listen(provider) {
            dispose?.run {
                invoke()
                reset(origin)
            }
        }
    }

    internal fun <State> update(provider: Provider<State>, next: State) {
        val providerOverride = overridesMap.getTypedOrNull(provider)
        if (providerOverride != null) {
            return updateInternal(providerOverride, next)
        }

        if (parent == null) {
            return updateInternal(provider, next)
        }

        return parent.update(provider, next)
    }

    private fun <State> updateInternal(provider: Provider<State>, next: State) {
        synchronized(this) {
            var entry = stateMap.getTypedOrCreate(provider, this)
            entry = entry.copy(state = next)
            stateMap = stateMap + (provider.key to entry)
            entry.listeners
        }.forEach {
            it.invoke()
        }
    }

    private fun <State> reset(provider: Provider<State>) {
        val providerOverride = overridesMap.getTypedOrNull(provider)
        if (providerOverride != null) {
            return resetInternal(providerOverride)
        }

        if (parent == null) {
            return resetInternal(provider)
        }

        return parent.reset(provider)
    }

    private fun <State> resetInternal(provider: Provider<State>) {
        synchronized(this) {
            var entry = stateMap.getTypedOrNull(provider)
            if (entry != null) {
                entry = entry.copy(state = provider.create(entry.ref))
                stateMap = stateMap + (provider.key to entry)
                entry.listeners
            } else {
                emptyList()
            }
        }.forEach {
            it.invoke()
        }
    }
}

private class ProviderRefImpl<State>(
    private val container: ProviderContainer,
    private val self: Provider<State>
) : ProviderRef<State>, DisposableProviderRef<State> {

    override fun <State> read(provider: Provider<State>): State {
        return container.read(provider)
    }

    override fun <State> watch(provider: Provider<State>): State {
        container.watch(self, provider)
        return container.read(provider)
    }

    override fun <State> listen(provider: Provider<State>, block: (State) -> Unit): Dispose {
        return container.listen(provider, block)
    }

    override var state: State
        get() = container.read(self)
        set(value) {
            container.update(self, value)
        }

    override var onDisposed: Dispose = {}
}

private data class ProviderEntry<State>(
    val state: State,
    val provider: Provider<State>,
    val ref: ProviderRefImpl<State>,
    val listeners: Set<() -> Unit> = emptySet()
)

private val <State> ProviderEntry<State>.shouldDispose: Boolean
    get() = provider is DisposableProvider<State> && listeners.isEmpty()

private fun <State> Map<ProviderKey, Provider<*>>.getTypedOrNull(
    provider: Provider<State>
): Provider<State>? {
    val value = get(provider.key)
    return if (value != null) {
        @Suppress("UNCHECKED_CAST")
        value as Provider<State>
    } else {
        null
    }
}

private fun <State> Map<ProviderKey, ProviderEntry<*>>.getTypedOrNull(
    provider: Provider<State>,
): ProviderEntry<State>? {
    val value = get(provider.key)
    return if (value != null) {
        @Suppress("UNCHECKED_CAST")
        value as ProviderEntry<State>
    } else {
        null
    }
}

private fun <State> Map<ProviderKey, ProviderEntry<*>>.getTypedOrCreate(
    provider: Provider<State>,
    container: ProviderContainer
): ProviderEntry<State> {
    val value = getTypedOrNull(provider)
    return if (value != null) {
        value
    } else {
        val ref = ProviderRefImpl(container = container, self = provider)
        ProviderEntry(state = provider.create(ref), ref = ref, provider = provider)
    }
}