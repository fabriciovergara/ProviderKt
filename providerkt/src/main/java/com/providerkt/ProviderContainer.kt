package com.providerkt

internal class ProviderContainerInternal private constructor(
    val parent: ProviderContainerInternal?,
    val overrides: Map<ProviderKey, Provider<*>>,
    var state: Map<ProviderKey, ProviderEntry<*>>,
    private val observers: Set<ProviderObserver>
) : ProviderContainer() {

    val root: ProviderContainerInternal = parent?.root ?: this

    constructor(
        parent: ProviderContainerInternal?,
        overrides: Set<ProviderOverride<*>>,
        observers: Set<ProviderObserver>
    ) : this(
        parent = parent,
        overrides = overrides.associate { it.original.key to it.override },
        observers = observers + (parent?.observers ?: emptySet()),
        state = mapOf()
    )

    override fun <State> read(provider: Provider<State>): State {
        return doRead(provider, observers)
    }

    override fun <State> update(provider: Provider<State>, value: State) {
        doUpdate(provider, value, observers)
    }

    override fun <State> listen(provider: Provider<State>, block: Listener<State>): Dispose {
        return doListen(provider, block, observers)
    }
}

internal data class ProviderEntry<State>(
    val state: State,
    val ref: ProviderRefInternal<State>
)

internal class ProviderRefInternal<State>(
    val container: ProviderContainerInternal,
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
            container.update(self, value)
        }


    override fun onDisposed(block: Dispose) {
        onDisposed = block
    }
}

private val ProviderContainerInternal.lock: Any
    get() = root.state

private fun <State> ProviderContainerInternal.doRead(
    provider: Provider<State>,
    observers: Set<ProviderObserver>
): State {
    val providerOverride = overrides.getTypedOrNull(provider)
    if (providerOverride != null) {
        return doReadInternal(providerOverride, observers)
    }

    if (parent == null) {
        return doReadInternal(provider, observers)
    }

    return parent.doRead(provider, observers)
}

private fun <State> ProviderContainerInternal.doReadInternal(
    provider: Provider<State>,
    observers: Set<ProviderObserver>,
): State {
    val (state, onCreated) = synchronized(lock) {
        val (created, entry) = state.getTypedOrCreate(provider, this)
        state = state + (provider.key to entry)
        entry.state to {
            if (created) {
                observers.forEach { it.onCreated(provider, entry.state) }
            }
        }
    }

    onCreated()
    return state
}

private fun <State> ProviderContainerInternal.doListen(
    provider: Provider<State>,
    block: Listener<State>,
    observers: Set<ProviderObserver>
): Dispose {
    val providerOverride = overrides.getTypedOrNull(provider)
    if (providerOverride != null) {
        return doListenInternal(providerOverride, block, observers)
    }

    if (parent == null) {
        return doListenInternal(provider, block, observers)
    }

    return parent.doListen(provider, block, observers)
}

private fun <State> ProviderContainerInternal.doListenInternal(
    provider: Provider<State>,
    block: Listener<State>,
    observers: Set<ProviderObserver>
): Dispose {
    val listener = { block(read(provider)) }
    provider.addListener(listener)
    listener.invoke()
    return {
        doDisposeListen(provider, listener, observers)
    }
}

internal fun <State> ProviderContainerInternal.doDisposeListen(
    provider: Provider<State>,
    listener: () -> Unit,
    observers: Set<ProviderObserver>
) {
    val listeners = provider.removeListener(listener)
    synchronized(lock) {
        val entry = state.getTypedOrNull(provider) ?: return@synchronized null
        if (entry.isDisposable && listeners.isEmpty()) {
            state = state - provider.key
            {
                observers.forEach { it.onDisposed(provider, entry.state) }
                entry.ref.onDisposed()
            }
        } else {
            null
        }
    }?.invoke()
}

internal fun <State> ProviderContainerInternal.doWatch(
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

internal fun <State> ProviderContainerInternal.doUpdate(
    provider: Provider<State>,
    next: State,
    observers: Set<ProviderObserver>
) {
    val providerOverride = overrides.getTypedOrNull(provider)
    if (providerOverride != null) {
        return updateInternal(providerOverride, next, observers)
    }

    if (parent == null) {
        return updateInternal(provider, next, observers)
    }

    return parent.doUpdate(provider, next, observers)
}

private fun <State> ProviderContainerInternal.updateInternal(
    provider: Provider<State>,
    next: State,
    observers: Set<ProviderObserver>
) {
    synchronized(lock) {
        val (created, entry) = state.getTypedOrCreate(provider, this)
        val newEntry = entry.copy(state = next)
        state = state + (provider.key to newEntry)
        {
            if (created) {
                observers.forEach { it.onCreated(provider, newEntry.state) }
            } else {
                observers.forEach { it.onUpdated(provider, entry.state, newEntry.state) }
            }

            provider.notifyListeners()
        }
    }.invoke()


}

private fun <State> ProviderContainerInternal.doReset(
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

private fun <State> ProviderContainerInternal.doResetInternal(
    provider: Provider<State>
) {
    synchronized(lock) {
        var entry = state.getTypedOrNull(provider)
        if (entry != null) {
            entry = entry.copy(state = provider.create(entry.ref))
            state = state + (provider.key to entry)
        }
    }

    synchronized(provider.listeners) {

    }
}

private fun <State> ProviderContainerInternal.toRef(
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

private fun <State> Provider<State>.addListener(listener: () -> Unit): Set<() -> Unit> {
    return synchronized(listeners) {
        listeners = listeners + listener
        listeners
    }
}

private fun <State> Provider<State>.removeListener(listener: () -> Unit): Set<() -> Unit> {
    return synchronized(listeners) {
        listeners = listeners - listener
        listeners
    }
}

private fun <State> Provider<State>.notifyListeners() {
    return synchronized(listeners) {
        listeners
    }.forEach {
        it.invoke()
    }
}

private val <State> ProviderEntry<State>.isDisposable: Boolean
    get() = ref.self is DisposableProvider<State>


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
    container: ProviderContainerInternal
): Pair<Boolean, ProviderEntry<State>> {
    val value = getTypedOrNull(provider)
    return if (value != null) {
        false to value
    } else {
        val ref = container.toRef(provider)
        true to ProviderEntry(
            state = provider.create(ref),
            ref = ref
        )
    }
}