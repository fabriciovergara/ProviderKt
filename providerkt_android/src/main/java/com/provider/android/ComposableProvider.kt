package com.provider.android

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.provider.*

private val LocalProviderContainer = compositionLocalOf<ProviderContainer?> {
    null
}

@Composable
fun ProviderScope(
    overrides: Set<ProviderOverride<*>> = emptySet(),
    content: @Composable () -> Unit
) {
    val parent = LocalProviderContainer.current
    val container = ProviderContainer(overrides = overrides, parent = parent)
    CompositionLocalProvider(
        LocalProviderContainer provides container,
        content = content
    )
}

@Composable
fun <T> Provider<T>.observeAsState(): State<T> {
    val lifecycleOwner = LocalLifecycleOwner.current
    val container = LocalProviderContainer.current ?: error("ProviderContainer not found")
    val state = remember { mutableStateOf(container.read(this)) }
    DisposableEffect(key, lifecycleOwner) {
        val dispose = container.listen(this@observeAsState) { next ->
            state.value = next
        }
        onDispose { dispose() }
    }

    return state
}
