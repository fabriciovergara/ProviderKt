package com.provider.android.provider

interface ProviderReader {
    fun <State> read(provider: Provider<State>): State
}

interface ProviderWatcher {
    fun <State> watch(provider: Provider<State>): State
}

interface ProviderListener {
    fun <State> listen(provider: Provider<State>, block: (State) -> Unit): () -> Unit
}

interface ProviderState<State> {
    var state: State
}

interface Ref : ProviderReader, ProviderWatcher, ProviderListener

interface ProviderRef<State> : Ref, ProviderState<State>

class Provider<State>(val name: String, val create: (ProviderRef<State>) -> State)

class ProviderOverride<State>(
    val original: Provider<State>,
    val override: Provider<State>
)

fun <State> Provider<State>.overrideWithProvider(
    override: Provider<State>
): ProviderOverride<State> {
    return ProviderOverride(original = this, override = override)
}

fun <State> Provider<State>.overrideWithValue(
    state: State
): ProviderOverride<State> {
    return overrideWithProvider(Provider(name = name) { state })
}