package com.providerkt.internal

import com.providerkt.*

internal class ContainerRef<State>(
    private val container: Container,
    private val origin: Provider<State>,
) : ProviderRef<State>, ProviderSelf<State> {

    private sealed class Action<State> {
        class Default<State> : Action<State>()
        class Creating<State> : Action<State>()
        class Updating<State>(val state: State) : Action<State>()
        class Disposing<State>(val state: State) : Action<State>()
    }

    private var onDisposed: VoidCallback = {}

    private var onUpdated: TypedCallback<State> = {}

    private var action: Action<State> = Action.Default()

    override val name: String = origin.name

    override val self: ProviderSelf<State> = this

    override fun <State> read(provider: Provider<State>): State {
        return container.read(provider)
    }

    override fun <WState> watch(provider: Provider<WState>): WState {
        return when (synchronized(this) { action }) {
            is Action.Default -> container.watch(origin, provider)
            is Action.Creating -> container.watch(origin, provider)
            is Action.Updating -> error("Can't do that while updating")
            is Action.Disposing -> error("Can't do that while disposing")
        }
    }

    override fun <State> listen(
        provider: Provider<State>,
        block: TypedCallback<State>,
    ): VoidCallback {
        return when (synchronized(this) { action }) {
            is Action.Default -> container.listen(provider, block)
            is Action.Creating -> container.listen(provider, block)
            is Action.Updating -> error("Can't do that while updating")
            is Action.Disposing -> error("Can't do that while disposing")
        }
    }

    override fun <State> refresh(provider: Provider<State>): State {
        return container.refresh(provider)
    }

    override fun onDisposed(block: VoidCallback) {
        synchronized(this) {
            onDisposed = block
        }
    }

    override fun onUpdated(block: TypedCallback<State>) {
        synchronized(this) {
            onUpdated = block
        }
    }

    override fun set(value: State) {
        return when (synchronized(this) { action }) {
            is Action.Default -> container.updateSelf(origin, value)
            is Action.Creating -> error("Can't do that while creating")
            is Action.Updating -> container.updateSelf(origin, value)
            is Action.Disposing -> error("Can't do that while disposing")
        }
    }

    override fun get(): State? {
        return when (val action = synchronized(this) { action }) {
            is Action.Default -> container.readSelf(origin)
            is Action.Creating -> container.readSelf(origin)
            is Action.Updating -> action.state
            is Action.Disposing -> action.state
        }
    }

    fun create(): State {
        return synchronized(this) {
            try {
                action = Action.Creating()
                origin.create(this)
            } finally {
                action = Action.Default()
            }
        }
    }

    fun update(state: State) {
        synchronized(this) {
            try {
                action = Action.Updating(state)
                onUpdated(state)
            } finally {
                action = Action.Default()
            }
        }
    }

    fun dispose(state: State) {
        synchronized(this) {
            try {
                action = Action.Disposing(state)
                onDisposed()
            } finally {
                action = Action.Default()
            }
        }
    }
}