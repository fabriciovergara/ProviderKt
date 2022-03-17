package com.providerkt.android

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.providerkt.*

private val LocalProviderContainer = compositionLocalOf<ProviderContainer?> {
    null
}

@Composable
fun ProviderScope(
    overrides: Set<ProviderOverride<*>> = emptySet(),
    content: @Composable () -> Unit
) {
    val parent = LocalProviderContainer.current
    val container = providerContainerOf(overrides = overrides, parent = parent)
    UncontrolledProviderScope(
        container,
        content = content
    )
}

@Composable
fun UncontrolledProviderScope(
    container: ProviderContainer,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalProviderContainer provides container,
        content = content
    )
}

@Composable
fun <T> Provider<T>.read(): T {
    val container = LocalProviderContainer.current ?: error("ProviderContainer not found")
    return container.read(this)
}

@Composable
fun <T> Provider<T>.watch(): MutableState<T> {
    val container = LocalProviderContainer.current ?: error("ProviderContainer not found")
    val state = remember(container) { mutableStateOf(container.read(this)) }
    val mutableState = remember(state) {
        object : MutableState<T> by state {
            override var value: T
                get() = state.value
                set(value) {
                    container.update(this@watch, value)
                }
        }
    }

    listen { next ->
        state.value = next
    }

    return mutableState
}

@Composable
fun <T> Provider<T>.listen(block: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val container = LocalProviderContainer.current ?: error("ProviderContainer not found")
    DisposableEffect(key, container, lifecycleOwner) {
        val dispose = container.listen(this@listen, block)
        onDispose { dispose() }
    }
}