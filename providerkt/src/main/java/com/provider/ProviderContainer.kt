package com.provider

class ProviderContainer internal constructor(
    internal val parent: ProviderContainer?,
    internal val overrides: Map<ProviderKey, Provider<*>>,
    internal var stateMap: Map<ProviderKey, ProviderEntry<*>>,
    internal var createGuard: Provider<*>? = null,
    internal val lock: Any = Any()
) : ProviderListener, ProviderReader {

    constructor(
        parent: ProviderContainer? = null,
        overrides: Set<ProviderOverride<*>> = setOf()
    ) : this(
        parent = parent,
        overrides = overrides.associate { it.original.key to it.override },
        stateMap = mapOf()
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
    val provider: Provider<State>,
    val ref: RefInternal<State>,
    val listeners: Set<() -> Unit>,
)

internal class RefInternal<State>(
    private val container: ProviderContainer,
    private val self: Provider<State>
) : ProviderRef<State>, DisposableProviderRef<State> {

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

    override var onDisposed: Dispose = {}
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
    return synchronized(lock) {
        val entry = stateMap.getTypedOrCreate(provider, this)
        stateMap = stateMap + (provider.key to entry)
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

    synchronized(lock) {
        var entry = stateMap.getTypedOrCreate(provider, this)
        entry = entry.copy(listeners = entry.listeners + listener)
        stateMap = stateMap + (provider.key to entry)
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
    return synchronized(lock) {
        var entryDispose = stateMap.getTypedOrNull(provider) ?: return@synchronized null
        entryDispose = entryDispose.copy(listeners = entryDispose.listeners - listener)
        if (entryDispose.shouldDispose) {
            stateMap = stateMap - provider.key
            entryDispose.ref.onDisposed
        } else {
            stateMap = stateMap + (provider.key to entryDispose)
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
    synchronized(lock) {
        var entry = stateMap.getTypedOrCreate(provider, this)
        entry = entry.copy(state = next)
        stateMap = stateMap + (provider.key to entry)
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
    synchronized(lock) {
        var entry = stateMap.getTypedOrNull(provider)
        if (entry != null) {
            entry = entry.copy(state = doCreate(provider, entry.ref))
            stateMap = stateMap + (provider.key to entry)
            entry.listeners
        } else {
            emptyList()
        }
    }.forEach {
        it.invoke()
    }
}

private fun <State> ProviderContainer.toRef(provider: Provider<State>): RefInternal<State> {
    return RefInternal(container = this, self = provider)
}

private fun <State> ProviderContainer.doCreate(
    provider: Provider<State>,
    ref: RefInternal<State>
): State {

    if (createGuard == null) {
        createGuard = provider
    } else if (createGuard?.key == provider.key) {
        throw Error("Cyclic dependency detected. $createGuard $provider depends on each other")
    }

    return when (provider) {
        is AlwaysAliveProvider -> provider.create(ref)
        is DisposableProvider -> provider.create(ref)
    }.also {
        if (createGuard?.key == provider.key) {
            createGuard = null
        }
    }
}

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
        val ref = container.toRef(provider)
        ProviderEntry(
            state = container.doCreate(provider, ref),
            ref = ref,
            provider = provider,
            listeners = emptySet()
        )
    }
}