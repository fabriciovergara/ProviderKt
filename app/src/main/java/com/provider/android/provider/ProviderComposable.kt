package com.provider.android.provider

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.provider.Provider
import com.provider.ProviderContainer
import com.provider.ProviderOverride

val LocalProviderContainer = compositionLocalOf<ProviderContainer?> {
    null
}

@Composable
fun ProviderContainerComposable(
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
    val container = LocalProviderContainer.current ?: error("")
    val state = remember { mutableStateOf(container.read(this)) }
    DisposableEffect(this.key, lifecycleOwner) {
        val dispose = container.listen(this@observeAsState) { next ->
            state.value = next
        }
        onDispose { dispose() }
    }
    return state
}
