package com.providerkt.android

import android.annotation.SuppressLint
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import com.providerkt.*
import java.lang.IllegalStateException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

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
    val state = remember(key, container) {
        mutableStateOf(container.read(this))
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

    listen { next ->
        state.value = next
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

public val <T> ProviderRef<T>.savedStateHandler: SavedStateHandle
    get() = read(savedStateHandleProvider)