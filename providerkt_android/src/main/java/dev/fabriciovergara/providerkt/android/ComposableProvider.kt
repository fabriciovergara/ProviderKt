package dev.fabriciovergara.providerkt.android

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.fabriciovergara.providerkt.*
import java.util.concurrent.atomic.AtomicBoolean

private val LocalProviderContainer = compositionLocalOf<ProviderContainer?> {
    null
}

private val savedStateHandleProvider by provider<SavedStateHandle> {
    error("Must override")
}

internal class SavedStateHandlerViewModel(
    val savedStateHandle: SavedStateHandle,
) : ViewModel()

@Composable
public fun ProviderScope(
    overrides: Set<ProviderOverride<*>> = emptySet(),
    observers: Set<ProviderObserver> = setOf(),
    content: @Composable () -> Unit,
) {
    val parent = LocalProviderContainer.current

    val viewModel = viewModel<SavedStateHandlerViewModel>()
    val viewModelOverride = remember(viewModel) {
        savedStateHandleProvider.overrideWithValue(viewModel.savedStateHandle)
    }

    val container = remember(parent) {
        providerContainerOf(parent, overrides + viewModelOverride, observers)
    }

    val isFirstComposition = remember {
        AtomicBoolean(true)
    }

    if (isFirstComposition.getAndSet(false).not()) {
        remember(overrides) {
            container.setOverrides(overrides + viewModelOverride)
            overrides
        }

        remember(observers) {
            container.setObservers(observers)
            observers
        }
    }

    UncontrolledProviderScope(
        container,
        content = content
    )
}

@Composable
public fun UncontrolledProviderScope(
    container: ProviderContainer,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalProviderContainer provides container,
        content = content
    )
}

@Composable
public fun <T> Provider<T>.read(): T {
    val container = LocalProviderContainer.current ?: error("ProviderContainer not found")
    return container.read(this)
}

@Composable
public fun <T> Provider<T>.watch(): MutableState<T> {
    val container = LocalProviderContainer.current ?: error("ProviderContainer not found")
    val lifecycleOwner = LocalLifecycleOwner.current

    var initialValue: Any? = null
    var state: MutableState<T>? = null

    val dispose = remember(key, container, lifecycleOwner) {
        container.listen(this) { next ->
            val localState = state
            if (localState != null) {
                localState.value = next
            } else {
                initialValue = next
            }
        }
    }

    DisposableEffect(dispose) {
        onDispose { dispose() }
    }

    state = remember(dispose) {
        @Suppress("UNCHECKED_CAST")
        mutableStateOf(initialValue as T)
    }

    val mutableState = remember(state) {
        object : MutableState<T> by state {
            override var value: T
                get() = state.value
                set(value) {
                    container.update(this@watch, value)
                }
        }
    }

    return mutableState
}

@SuppressLint("ComposableNaming")
@Composable
public fun <T> Provider<T>.listen(block: (T) -> Unit) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val container = LocalProviderContainer.current ?: error("ProviderContainer not found")
    DisposableEffect(key, container, lifecycleOwner) {
        val dispose = container.listen(this@listen, block)
        onDispose { dispose() }
    }
}


@Composable
public fun <T> Provider<T>.refresh(): () -> Unit {
    val container = LocalProviderContainer.current ?: error("ProviderContainer not found")
    return { container.refresh(this) }
}

public val <T> ProviderRef<T>.savedStateHandler: SavedStateHandle
    get() = read(savedStateHandleProvider)