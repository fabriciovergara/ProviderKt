package com.providerkt.internal

import com.providerkt.*

internal class ProviderContainerInternal private constructor(
    val parent: ProviderContainerInternal?,
    val overrides: Map<ProviderKey, Provider<*>>,
    var state: Map<ProviderKey, ProviderEntry<*>>,
    val observers: Set<ProviderObserver>
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
        return doRead(provider, this)
    }

    override fun <State> update(provider: Provider<State>, value: State) {
        doUpdate(provider, value, this)
    }

    override fun <State> listen(provider: Provider<State>, block: Listener<State>): Dispose {
        return doListen(provider, block, this)
    }

    fun <State, WatchState> watch(origin: Provider<State>, provider: Provider<WatchState>) {
        return doWatch(origin, provider, this)
    }

    fun <State> selfUpdate(provider: Provider<State>, value: State) {
        return doSelfUpdate(provider, value, this)
    }

    fun <State> selfRead(provider: Provider<State>): State? {
        return doSelfRead(provider)
    }
}

internal data class ProviderEntry<State>(
    val state: State,
    val ref: ProviderRefInternal<State>
)

internal class ProviderRefInternal<State>(
    val container: ProviderContainerInternal,
    val pSelf: Provider<State>,
) : ProviderRef<State>, ProviderSelf<State> {

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

    override fun <WatchState> watch(provider: Provider<WatchState>): WatchState {
        container.watch(pSelf, provider)
        return container.read(provider)
    }

    override fun <State> listen(provider: Provider<State>, block: Listener<State>): Dispose {
        return container.listen(provider, block)
    }

    override fun onDisposed(block: Dispose) {
        onDisposed = block
    }

    override fun set(value: State) {
        container.selfUpdate(pSelf, value)
    }

    override fun get(): State? {
        return container.selfRead(pSelf)
    }

    override val name: String = pSelf.name

    override val self: ProviderSelf<State> = this
}

private val ProviderContainerInternal.lock: Any
    get() = root.state


private fun <State> ProviderContainerInternal.doSelfRead(
    provider: Provider<State>
): State? {
    val state = synchronized(lock) {
        val entry = state.getTypedOrNull(provider)
        entry?.state
    }

    return state
}

private fun <State> ProviderContainerInternal.doRead(
    provider: Provider<State>,
    origin: ProviderContainerInternal
): State {
    val providerOverride = overrides.getTypedOrNull(provider)
    if (providerOverride != null) {
        return doReadInternal(providerOverride, origin)
    }

    if (parent == null) {
        return doReadInternal(provider, origin)
    }

    return parent.doRead(provider, origin)
}

private fun <State> ProviderContainerInternal.doReadInternal(
    provider: Provider<State>,
    origin: ProviderContainerInternal,
): State {
    val (state, onCreated) = synchronized(lock) {
        val (created, entry) = state.getTypedOrCreate(provider, origin)
        state = state + (provider.key to entry)
        entry.state to {
            if (created) {
                origin.observers.onCreated(provider, entry.state)
            }
        }
    }

    onCreated()
    return state
}

private fun <State> ProviderContainerInternal.doListen(
    provider: Provider<State>,
    block: Listener<State>,
    origin: ProviderContainerInternal
): Dispose {
    val providerOverride = overrides.getTypedOrNull(provider)
    if (providerOverride != null) {
        return doListenInternal(providerOverride, block, origin)
    }

    if (parent == null) {
        return doListenInternal(provider, block, origin)
    }

    return parent.doListen(provider, block, origin)
}

private fun <State> ProviderContainerInternal.doListenInternal(
    provider: Provider<State>,
    block: Listener<State>,
    origin: ProviderContainerInternal
): Dispose {
    val listener = { block(read(provider)) }
    provider.addListener(listener)
    listener.invoke()
    return {
        doDisposeListen(provider, listener, origin)
    }
}

internal fun <State> ProviderContainerInternal.doDisposeListen(
    provider: Provider<State>,
    listener: () -> Unit,
    origin: ProviderContainerInternal
) {
    val listeners = provider.removeListener(listener)
    synchronized(lock) {
        val entry = state.getTypedOrNull(provider) ?: return@synchronized null
        if (entry.ref.pSelf.type == ProviderType.Disposable && listeners.isEmpty()) {
            state = state - provider.key
            {
                origin.observers.onDisposed(provider, entry.state)
                entry.ref.onDisposed()
            }
        } else {
            null
        }
    }?.invoke()
}

internal fun <State, WatchState> ProviderContainerInternal.doWatch(
    self: Provider<State>,
    provider: Provider<WatchState>,
    origin: ProviderContainerInternal
) {
    var dispose: Dispose? = null
    dispose = listen(provider) {
        dispose?.run {
            invoke()
            doReset(self, origin)
        }
    }
}

internal fun <State> ProviderContainerInternal.doSelfUpdate(
    provider: Provider<State>,
    next: State,
    origin: ProviderContainerInternal
) {
    synchronized(lock) {
        val entry = state.getTypedOrNull(provider)
        if (entry != null) {
            val newEntry = entry.copy(state = next)
            state = state + (provider.key to newEntry)
            {
                origin.observers.onUpdated(provider, entry.state, newEntry.state)
                provider.notifyListeners()
            }
        } else {
            null
        }
    }?.invoke()
}

internal fun <State> ProviderContainerInternal.doUpdate(
    provider: Provider<State>,
    next: State,
    origin: ProviderContainerInternal
) {
    val providerOverride = overrides.getTypedOrNull(provider)
    if (providerOverride != null) {
        return updateInternal(providerOverride, next, origin)
    }

    if (parent == null) {
        return updateInternal(provider, next, origin)
    }

    return parent.doUpdate(provider, next, origin)
}

private fun <State> ProviderContainerInternal.updateInternal(
    provider: Provider<State>,
    next: State,
    origin: ProviderContainerInternal
) {
    synchronized(lock) {
        val (created, entry) = state.getTypedOrCreate(provider, origin)
        val newEntry = entry.copy(state = next)
        state = state + (provider.key to newEntry)
        {
            if (created) {
                origin.observers.onCreated(provider, newEntry.state)
            } else {
                origin.observers.onUpdated(provider, entry.state, newEntry.state)
            }

            provider.notifyListeners()
        }
    }.invoke()


}

private fun <State> ProviderContainerInternal.doReset(
    provider: Provider<State>,
    origin: ProviderContainerInternal
) {
    val providerOverride = overrides.getTypedOrNull(provider)
    if (providerOverride != null) {
        return doResetInternal(providerOverride, origin)
    }

    if (parent == null) {
        return doResetInternal(provider, origin)
    }

    return parent.doReset(provider, origin)
}

private fun <State> ProviderContainerInternal.doResetInternal(
    provider: Provider<State>,
    origin: ProviderContainerInternal
) {
    synchronized(lock) {
        val entry = state.getTypedOrNull(provider)
        if (entry != null) {
            val newEntry = entry.copy(state = provider.create(entry.ref))
            state = state + (provider.key to newEntry)
            {
                entry.ref.onDisposed()
                origin.observers.onDisposed(provider, entry.state)
                provider.notifyListeners()
            }
        } else {
            null
        }
    }?.invoke()
}

private fun <State> ProviderContainerInternal.toRef(
    provider: Provider<State>
): ProviderRefInternal<State> {
    return ProviderRefInternal(container = this, pSelf = provider)
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

private fun Set<ProviderObserver>.onCreated(provider: Provider<*>, value: Any?) {
    forEach { it.onCreated(provider, value) }
}

private fun Set<ProviderObserver>.onUpdated(provider: Provider<*>, old: Any?, value: Any?) {
    forEach { it.onUpdated(provider, old, value) }
}

private fun Set<ProviderObserver>.onDisposed(provider: Provider<*>, value: Any?) {
    forEach { it.onDisposed(provider, value) }
}