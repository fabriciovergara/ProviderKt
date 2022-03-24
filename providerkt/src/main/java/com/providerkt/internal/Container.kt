package com.providerkt.internal

import com.providerkt.*

internal class Container private constructor(
    val parent: Container?,
    val extras: ContainerExtras,
) : ProviderContainer() {

    constructor(
        overrides: Set<ProviderOverride<*>>,
        observers: Set<ProviderObserver>,
    ) : this(
        parent = null,
        extras = ContainerExtras(
            overrides = overrides.associate { it.original.key to it.override },
            observers = observers,
        )
    )

    private val root: Container = parent?.root ?: this
    val lock: Any = root
    var state: ContainerState = mapOf()


    override fun <State> listen(provider: Provider<State>, block: Listener<State>): Dispose {
        return doListen(provider = provider, block = block, origin = this)
    }

    override fun <State> read(provider: Provider<State>): State {
        return doRead(provider = provider, origin = this)
    }

    override fun <State> update(provider: Provider<State>, value: State) {
        return doUpdate(provider = provider, next = value, origin = this)
    }

    fun <State, WState> watch(origin: Provider<State>, provider: Provider<WState>): WState {
        doWatch(self = origin, provider = provider, origin = this)
        return doRead(provider = provider, origin = this)
    }

    fun <State> updateSelf(provider: Provider<State>, value: State) {
        doUpdateSelf(provider = provider, next = value, origin = this)
    }

    fun <State> readSelf(provider: Provider<State>): State? {
        return doReadSelf(provider = provider, origin = this)
    }

    override fun extends(
        overrides: Set<ProviderOverride<*>>,
        observers: Set<ProviderObserver>,
    ): ProviderContainer {
        return Container(
            parent = this,
            extras = extras + ContainerExtras(
                overrides = overrides.associate { it.original.key to it.override },
                observers = observers,
            )
        )
    }
}