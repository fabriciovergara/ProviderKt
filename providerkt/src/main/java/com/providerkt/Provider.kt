package com.providerkt

import kotlin.properties.ReadOnlyProperty

public interface ProviderReader {
    public fun <State> read(provider: Provider<State>): State
}

public interface ProviderWatcher {
    public fun <State> watch(provider: Provider<State>): State
}

public interface ProviderListener {
    public fun <State> listen(provider: Provider<State>, block: Listener<State>): Dispose
}

public interface ProviderUpdater {
    public fun <State> update(provider: Provider<State>, value: State)
}

public interface ProviderDispose {
    public fun onDisposed(block: Dispose)
}

public interface ProviderObserver {
    public fun onCreated(provider: Provider<*>, value: Any?): Unit = Unit
    public fun onUpdated(provider: Provider<*>, old: Any?, value: Any?): Unit = Unit
    public fun onDisposed(provider: Provider<*>, value: Any?): Unit = Unit
}

public typealias Dispose = () -> Unit
public typealias ProviderKey = String
public typealias Listener<State> = (state: State) -> Unit
public typealias Create<State> = ProviderRef<State>.() -> State
public typealias CreateFamily<State, Argument> = ProviderRef<State>.(arg: Argument) -> State
public typealias FamilyProvider<State, Argument> = (arg: Argument) -> Provider<State>

public interface ProviderRef<State> : ProviderReader, ProviderWatcher, ProviderListener,
    ProviderDispose {
    public fun set(value: State)
    public fun get(): State?
}

public sealed class ProviderContainer : ProviderListener, ProviderReader, ProviderUpdater

public enum class ProviderType {
    AlwaysAlive,
    Disposable
}

public class Provider<State>(
    public val key: ProviderKey,
    public val name: String,
    public val type: ProviderType,
    internal val create: Create<State>
) {
    internal var listeners: Set<() -> Unit> = setOf()
    override fun toString(): String = "Provider($key, $name, $type)"
}

public class ProviderOverride<State>(
    public val original: Provider<State>,
    public val override: Provider<State>
)

public fun providerContainerOf(
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

public fun <State> Provider<State>.overrideWithProvider(
    override: Provider<State>
): ProviderOverride<State> = ProviderOverride(
    original = this,
    override = override
)

public fun <State> Provider<State>.overrideWithValue(
    override: State
): ProviderOverride<State> = ProviderOverride(
    original = this,
    override = providerOf(name = name) { override }
)


public fun <State> providerOf(
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

public fun <State> provider(
    name: String? = null,
    type: ProviderType = ProviderType.AlwaysAlive,
    create: Create<State>
): ReadOnlyProperty<Any?, Provider<State>> = ReadOnlyProperty { _, property ->
    providerOf(name = name ?: property.name, type = type, create = create)
}

public fun <State, Argument> familyProviderOf(
    name: String,
    type: ProviderType = ProviderType.AlwaysAlive,
    create: CreateFamily<State, Argument>
): FamilyProvider<State, Argument> {
    val key = providerKeyOf()
    return { arg ->
        Provider(
            name = name,
            key = providerKeyOf(key, arg),
            type = type,
            create = { create(this, arg) }
        )
    }
}

public fun <State, Argument> familyProvider(
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

