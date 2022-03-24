package com.providerkt.internal

import com.providerkt.*

internal class ContainerRef<State>(
    private val container: Container,
    private val origin: Provider<State>,
) : ProviderRef<State>, ProviderSelf<State> {

    var onDisposed: Dispose by sync(this) {}

    override val name: String = origin.name

    override val self: ProviderSelf<State> = this

    override fun <State> read(provider: Provider<State>): State {
        return container.read(provider)
    }

    override fun <WState> watch(provider: Provider<WState>): WState {
        return container.watch(origin, provider)
    }

    override fun <State> listen(provider: Provider<State>, block: Listener<State>): Dispose {
        return container.listen(provider, block)
    }

    override fun onDisposed(block: Dispose) {
        onDisposed = block
    }

    override fun set(value: State) {
        container.updateSelf(origin, value)
    }

    override fun get(): State? {
        return container.readSelf(origin)
    }


}