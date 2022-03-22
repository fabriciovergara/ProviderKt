package com.providerkt

import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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

interface ProviderDispose {
    fun onDisposed(block: Dispose)
}

interface ProviderObserver {
    fun onCreated(provider: Provider<*>, value: Any?) = Unit
    fun onUpdated(provider: Provider<*>, old: Any?, value: Any?) = Unit
    fun onDisposed(provider: Provider<*>, value: Any?) = Unit
}

typealias Dispose = () -> Unit
typealias ProviderKey = String
typealias Listener<State> = (state: State) -> Unit
typealias Create<State> = ProviderRef<State>.() -> State
typealias CreateFamily<State, Argument> = ProviderRef<State>.(arg: Argument) -> State
typealias FamilyProvider<State, Argument> = (arg: Argument) -> Provider<State>

interface ProviderRef<State> : ProviderReader, ProviderWatcher, ProviderListener, ProviderDispose {
    fun set(value: State)
    fun get(): State?
}

sealed class ProviderContainer : ProviderListener, ProviderReader, ProviderUpdater

enum class ProviderType {
    AlwaysAlive,
    Disposable
}

class Provider<State>(
    val key: ProviderKey,
    val name: String,
    val type: ProviderType,
    internal val create: Create<State>
) {
    internal var listeners: Set<() -> Unit> = setOf()
    override fun toString(): String = "Provider($key, $name, $type)"
}

class ProviderOverride<State>(
    val original: Provider<State>,
    val override: Provider<State>
)

fun providerContainerOf(
    parent: ProviderContainer? = null,
    overrides: Set<ProviderOverride<*>> = setOf(),
    observers: Set<ProviderObserver> = setOf()
): ProviderContainer = ProviderContainerInternal(
    parent = when (parent) {
        is ProviderContainerInternal -> parent
        null -> parent
    },
    overrides = overrides,
    observers = observers
)

fun <State> Provider<State>.overrideWithProvider(
    override: Provider<State>
): ProviderOverride<State> = ProviderOverride(
    original = this,
    override = override
)

fun <State> Provider<State>.overrideWithValue(
    override: State
): ProviderOverride<State> = ProviderOverride(
    original = this,
    override = providerOf(name = name) { override }
)


fun <State> providerOf(
    name: String,
    type: ProviderType = ProviderType.AlwaysAlive,
    create: Create<State>,
): Provider<State> {
    val key = providerKeyOf()
    return Provider(
        name = name,
        key = key,
        type = type,
        create = create
    )
}

fun <State> provider(
    name: String? = null,
    type: ProviderType = ProviderType.AlwaysAlive,
    create: Create<State>
): ReadOnlyProperty<Any?, Provider<State>> = ReadOnlyProperty { _, property ->
    providerOf(name = name ?: property.name, type = type, create = create)
}


fun <State, Argument> familyProviderOf(
    name: String,
    type: ProviderType = ProviderType.AlwaysAlive,
    create: CreateFamily<State, Argument>
): FamilyProvider<State, Argument> {
    val key = providerKeyOf()
    return { arg ->
        Provider(
            name = name,
            key = key,
            type = type,
            create = { create(this, arg) }
        )
    }
}

fun <State, Argument> familyProvider(
    name: String? = null,
    type: ProviderType = ProviderType.AlwaysAlive,
    create: CreateFamily<State, Argument>
): ReadOnlyProperty<Any?, FamilyProvider<State, Argument>> = ReadOnlyProperty { _, property ->
    familyProviderOf(name = name ?: property.name, type = type, create = create)
}

private fun providerKeyOf(
    base: ProviderKey = "${Any().hashCode()}",
    extra: Any? = null
): ProviderKey {
    return extra?.let { "$base+${it.hashCode()}" } ?: base
}

