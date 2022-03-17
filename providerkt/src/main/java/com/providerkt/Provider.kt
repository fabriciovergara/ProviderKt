package com.providerkt

interface ProviderReader {
    fun <State> read(provider: Provider<State>): State
}

interface ProviderWatcher {
    fun <State> watch(provider: Provider<State>): State
}

interface ProviderListener {
    fun <State> listen(provider: Provider<State>, block: Listener<State>): Dispose
}

interface ProviderUpdater {
    fun <State> update(provider: Provider<State>, value: State)
}

interface ProviderObserver {
    fun onCreated(provider: Provider<*>, value: Any?) = Unit
    fun onUpdated(provider: Provider<*>, old: Any?, value: Any?) = Unit
    fun onDisposed(provider: Provider<*>, value: Any?) = Unit
}

typealias Dispose = () -> Unit

typealias ProviderKey = String

typealias Listener<State> = (State) -> Unit

typealias FamilyProvider<State, Argument> = (Argument) -> Provider<State>

interface ProviderRef<State> : ProviderReader, ProviderWatcher, ProviderListener {
    var state: State
}

interface DisposableProviderRef<State> : ProviderRef<State> {
    fun onDisposed(block: Dispose)
}

sealed class Provider<State>(val key: ProviderKey, val name: String) {

    override fun toString(): String = "Provider($name)"

    internal var listeners: Set<() -> Unit> = setOf()
}

sealed class ProviderContainer : ProviderListener, ProviderReader, ProviderUpdater

internal class AlwaysAliveProvider<State>(
    key: ProviderKey,
    name: String,
    val create: (ProviderRef<State>) -> State
) : Provider<State>(key, name)

internal class DisposableProvider<State>(
    key: ProviderKey,
    name: String,
    val create: (DisposableProviderRef<State>) -> State
) : Provider<State>(key, name)

class ProviderOverride<State>(
    val original: Provider<State>,
    val override: Provider<State>
)

fun <State> Provider<State>.overrideWithProvider(
    override: Provider<State>
): ProviderOverride<State> {
    return ProviderOverride(
        original = this,
        override = override
    )
}

fun <State> Provider<State>.overrideWithValue(
    override: State
): ProviderOverride<State> {
    return ProviderOverride(
        original = this,
        override = providerOf(name = name) { override }
    )
}

fun providerContainerOf(
    parent: ProviderContainer? = null,
    overrides: Set<ProviderOverride<*>> = setOf(),
    observers: Set<ProviderObserver> = setOf()
): ProviderContainer {
    return ProviderContainerInternal(
        parent = when (parent) {
            is ProviderContainerInternal -> parent
            null -> parent
        },
        overrides = overrides,
        observers = observers
    )
}

internal fun providerKeyOf(
    base: ProviderKey = "${Any().hashCode()}",
    extra: Any? = null
): ProviderKey {
    return extra?.let { "$base+${it.hashCode()}" } ?: base
}

fun <State> providerOf(
    name: String,
    create: (ProviderRef<State>) -> State
): Provider<State> {
    val key = providerKeyOf()
    return AlwaysAliveProvider(
        name = name,
        key = key,
        create = create
    )
}

fun <State> disposableProviderOf(
    name: String,
    create: (DisposableProviderRef<State>) -> State
): Provider<State> {
    val key = providerKeyOf()
    return DisposableProvider(
        name = name,
        key = key,
        create = create
    )
}

fun <State, Argument> familyProviderOf(
    name: String,
    create: (ProviderRef<State>, Argument) -> State
): FamilyProvider<State, Argument> {
    val key = providerKeyOf()
    return { arg ->
        AlwaysAliveProvider(
            name = name,
            key = providerKeyOf(key, arg),
            create = { ref -> create(ref, arg) }
        )
    }
}

fun <State, Argument> disposableFamilyProviderOf(
    name: String,
    create: (DisposableProviderRef<State>, Argument) -> State
): FamilyProvider<State, Argument> {
    val key = providerKeyOf()
    return { arg ->
        DisposableProvider(
            name = name,
            key = providerKeyOf(key, arg),
            create = { ref -> create(ref, arg) }
        )
    }
}