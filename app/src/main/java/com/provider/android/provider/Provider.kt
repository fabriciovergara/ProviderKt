package com.provider.android.provider

interface ProviderReader {
    fun <State> read(provider: Provider<State>): State
}

interface ProviderWatcher {
    fun <State> watch(provider: Provider<State>): State
}

interface ProviderListener {
    fun <State> listen(provider: Provider<State>, block: (State) -> Unit): Dispose
}

interface ProviderState<State> {
    var state: State
}

typealias Dispose = () -> Unit

interface Ref : ProviderReader, ProviderWatcher, ProviderListener

interface ProviderRef<State> : Ref, ProviderState<State>

interface DisposableProviderRef<State> : ProviderRef<State> {
    var onDisposed: Dispose
}

typealias ProviderKey = String

fun createProviderKey(): ProviderKey {
    return "${Any().hashCode()}"
}

fun <Argument> combineProviderKey(key: ProviderKey, argument: Argument): ProviderKey {
    return "$key+${argument.hashCode()}"
}

open class Provider<State> internal constructor(
    val name: String,
    val key: ProviderKey,
    val create: (ProviderRef<State>) -> State,
)

class DisposableProvider<State>(
    name: String,
    key: ProviderKey,
    create: (DisposableProviderRef<State>) -> State
) : Provider<State>(name, key, { ref ->
    // TODO fix this
    create(ref as DisposableProviderRef<State>)
})


class ProviderOverride<State>(
    val original: Provider<State>,
    val override: Provider<State>
)

fun <State> Provider<State>.overrideWithProvider(
    override: Provider<State>
): ProviderOverride<State> {
    return ProviderOverride(original = this, override = override)
}

fun <State> providerOf(
    name: String,
    create: (ProviderRef<State>) -> State
): Provider<State> {
    val key = createProviderKey()
    return Provider(
        name = name,
        key = key,
        create = create
    )
}

fun <State> providerDisposableOf(
    name: String,
    create: (DisposableProviderRef<State>) -> State
): Provider<State> {
    val key = createProviderKey()
    return DisposableProvider(
        name = name,
        key = key,
        create = create
    )
}

fun <State, Argument> providerFamilyOf(
    name: String,
    create: (ProviderRef<State>, Argument) -> State
): (Argument) -> Provider<State> {
    val key = createProviderKey()
    return { arg ->
        val composedKey = combineProviderKey(key, arg)
        Provider(
            name = name,
            key = composedKey,
            create = { ref -> create(ref, arg) }
        )
    }
}

fun <State, Argument> providerDisposableFamilyOf(
    name: String,
    create: (DisposableProviderRef<State>, Argument) -> State
): (Argument) -> Provider<State> {
    val key = createProviderKey()
    return { arg ->
        val composedKey = combineProviderKey(key, arg)
        DisposableProvider(
            name = name,
            key = composedKey,
            create = { ref -> create(ref, arg) }
        )
    }
}