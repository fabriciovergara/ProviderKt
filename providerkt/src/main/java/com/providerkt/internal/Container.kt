package com.providerkt.internal

import com.providerkt.*

internal class Container private constructor(
    val parent: Container?,
    private var extras: ContainerExtras,
) : ProviderContainer() {

    constructor(
        parent: Container?,
        overrides: OverrideSet,
        observers: ObserverSet,
    ) : this(
        parent = parent,
        extras = ContainerExtras(
            overrides = overrides.associate { it.original.key to it.toProvider() },
            observers = observers,
        )
    )

    val root: Container = parent?.root ?: this
    var state: ContainerState = mapOf()

    override fun setOverrides(overrides: OverrideSet) {
        synchronized(this) {
            val old = extras
            extras = extras.copy(
                overrides = overrides.associate {
                    val oldOverride = extras.overrides[it.original.key]
                    val provider = it.toProvider()
                    if (oldOverride != null) {
                        synchronized(oldOverride) {
                            provider.listeners = oldOverride.listeners
                        }
                    }
                    it.original.key to provider
                }
            )
            old.overrides.values
        }.forEach { provider ->
            reset(provider)
        }
    }

    override fun setObservers(observers: ObserverSet) {
        synchronized(this) {
            extras = extras.copy(observers = observers)
        }
    }

    override fun <State> listen(provider: Provider<State>, block: Listener<State>): Dispose {
        return doListen(provider = provider, block = block, origin = this, extras = collectExtras())
    }

    override fun <State> read(provider: Provider<State>): State {
        return doRead(provider = provider, origin = this, extras = collectExtras())
    }

    override fun <State> update(provider: Provider<State>, value: State) {
        return doUpdate(provider = provider, next = value, origin = this, extras = collectExtras())
    }

    fun <State, WState> watch(origin: Provider<State>, provider: Provider<WState>): WState {
        doWatch(self = origin, provider = provider, origin = this)
        return doRead(provider = provider, origin = this, extras = collectExtras())
    }

    fun <State> updateSelf(provider: Provider<State>, value: State) {
        doUpdateSelf(provider = provider, next = value, extras = collectExtras())
    }

    fun <State> readSelf(provider: Provider<State>): State? {
        return doReadSelf(provider = provider, origin = this)
    }

    fun <State> reset(provider: Provider<State>) {
        doReset(provider = provider, origin = this, extras = collectExtras())
    }

    fun <State> dispose(provider: Provider<State>, listener: () -> Unit) {
        doListenDispose(provider = provider, listener = listener, extras = collectExtras())
    }

    private fun collectExtras(): ContainerExtras {
        val extrasList = mutableListOf<ContainerExtras>()
        var container: Container? = this
        while (container != null) {
            val value: Container = container
            val extras = synchronized(value) { value.extras }
            extrasList += extras
            container = value.parent
        }

        return extrasList.foldRight(ContainerExtras()) { next, acc ->
            acc + next
        }
    }
}
