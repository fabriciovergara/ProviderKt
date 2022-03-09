package com.provider

class ProviderContainer internal constructor(
    internal val parent: ProviderContainer?,
    internal val overrides: Map<ProviderKey, Provider<*>>,
    internal var state: Map<ProviderKey, ProviderEntry<*>>
) : ProviderListener, ProviderReader {

    constructor(
        parent: ProviderContainer? = null,
        overrides: Set<ProviderOverride<*>> = setOf()
    ) : this(
        parent = parent,
        overrides = overrides.associate { it.original.key to it.override },
        state = mapOf()
    )

    override fun <State> read(provider: Provider<State>): State {
        return doRead(provider)
    }

    override fun <State> listen(provider: Provider<State>, block: Listener<State>): Dispose {
        return doListen(provider, block)
    }
}

internal data class ProviderEntry<State>(
    val state: State,
    val ref: ProviderRefInternal<State>,
    val listeners: Set<() -> Unit>,
)

internal class ProviderRefInternal<State>(
    private val container: ProviderContainer,
    val self: Provider<State>,
) : ProviderRef<State>, DisposableProviderRef<State> {

    var onDisposed: Dispose = {}
        set(value) = synchronized(this) {
            field = value
        }
        get() = synchronized(this) {
            field
        }

    override fun <State> read(provider: Provider<State>): State {
        return container.read(provider)
    }

    override fun <State> watch(provider: Provider<State>): State {
        container.doWatch(self, provider)
        return container.read(provider)
    }

    override fun <State> listen(provider: Provider<State>, block: (State) -> Unit): Dispose {
        return container.listen(provider, block)
    }

    override var state: State
        get() = container.read(self)
        set(value) {
            container.doUpdate(self, value)
        }


    override fun onDisposed(block: Dispose) {
        onDisposed = block
    }
}


private fun <State> ProviderContainer.doRead(
    provider: Provider<State>
): State {
    val providerOverride = overrides.getTypedOrNull(provider)
    if (providerOverride != null) {
        return doReadInternal(providerOverride)
    }

    if (parent == null) {
        return doReadInternal(provider)
    }

    return parent.read(provider)
}

private fun <State> ProviderContainer.doReadInternal(
    provider: Provider<State>
): State {
    return synchronized(state) {
        val entry = state.getTypedOrCreate(provider, this)
        state = state + (provider.key to entry)
        entry.state
    }
}

private fun <State> ProviderContainer.doListen(
    provider: Provider<State>,
    block: Listener<State>
): Dispose {
    val providerOverride = overrides.getTypedOrNull(provider)
    if (providerOverride != null) {
        return doListenInternal(providerOverride, block)
    }

    if (parent == null) {
        return doListenInternal(provider, block)
    }

    return parent.listen(provider, block)
}

private fun <State> ProviderContainer.doListenInternal(
    provider: Provider<State>,
    block: Listener<State>
): Dispose {
    val listener = { block(read(provider)) }

    synchronized(state) {
        var entry = state.getTypedOrCreate(provider, this)
        entry = entry.copy(listeners = entry.listeners + listener)
        state = state + (provider.key to entry)
    }

    listener.invoke()
    return {
        doDisposeListen(provider, listener)?.invoke()
    }
}

internal fun <State> ProviderContainer.doDisposeListen(
    provider: Provider<State>,
    listener: () -> Unit
): Dispose? {
    return synchronized(state) {
        var entry = state.getTypedOrNull(provider) ?: return@synchronized null
        entry = entry.copy(listeners = entry.listeners - listener)
        if (entry.shouldDispose) {
            state = state - provider.key
            entry.ref.onDisposed
        } else {
            state = state + (provider.key to entry)
            null
        }
    }
}

internal fun <State> ProviderContainer.doWatch(
    origin: Provider<*>,
    provider: Provider<State>
) {
    var dispose: Dispose? = null
    dispose = listen(provider) {
        dispose?.run {
            invoke()
            doReset(origin)
        }
    }
}

internal fun <State> ProviderContainer.doUpdate(
    provider: Provider<State>,
    next: State
) {
    val providerOverride = overrides.getTypedOrNull(provider)
    if (providerOverride != null) {
        return updateInternal(providerOverride, next)
    }

    if (parent == null) {
        return updateInternal(provider, next)
    }

    return parent.doUpdate(provider, next)
}

private fun <State> ProviderContainer.updateInternal(
    provider: Provider<State>,
    next: State
) {
    synchronized(state) {
        var entry = state.getTypedOrCreate(provider, this)
        entry = entry.copy(state = next)
        state = state + (provider.key to entry)
        entry.listeners
    }.forEach {
        it.invoke()
    }
}

private fun <State> ProviderContainer.doReset(
    provider: Provider<State>
) {
    val providerOverride = overrides.getTypedOrNull(provider)
    if (providerOverride != null) {
        return doResetInternal(providerOverride)
    }

    if (parent == null) {
        return doResetInternal(provider)
    }

    return parent.doReset(provider)
}

private fun <State> ProviderContainer.doResetInternal(
    provider: Provider<State>
) {
    synchronized(state) {
        var entry = state.getTypedOrNull(provider)
        if (entry != null) {
            entry = entry.copy(state = provider.create(entry.ref))
            state = state + (provider.key to entry)
            entry.listeners
        } else {
            emptyList()
        }
    }.forEach {
        it.invoke()
    }
}

private fun <State> ProviderContainer.toRef(
    provider: Provider<State>
): ProviderRefInternal<State> {
    return ProviderRefInternal(container = this, self = provider)
}

private fun <State> Provider<State>.create(
    ref: ProviderRefInternal<State>
): State {
    return when (this) {
        is AlwaysAliveProvider -> create(ref)
        is DisposableProvider -> create(ref)
    }
}

private val <State> ProviderEntry<State>.shouldDispose: Boolean
    get() = ref.self is DisposableProvider<State> && listeners.isEmpty()

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
        val ref = container.toRef(provider)
        ProviderEntry(
            state = provider.create(ref),
            ref = ref,
            listeners = emptySet()
        )
    }
}