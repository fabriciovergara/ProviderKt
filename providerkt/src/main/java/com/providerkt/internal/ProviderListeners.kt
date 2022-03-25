package com.providerkt.internal

import com.providerkt.Provider
import com.providerkt.ProviderType

internal fun <State> Provider<State>.addListener(listener: () -> Unit) {
    synchronized(this) {
        listeners = listeners + listener
    }
}

internal fun <State> Provider<State>.removeListener(listener: () -> Unit) {
    synchronized(this) {
        listeners = listeners - listener
    }
}

internal fun <State> Provider<State>.shouldDispose(): Boolean {
    return synchronized(this) {
        listeners.isEmpty() && type == ProviderType.Disposable
    }
}

internal fun <State> Provider<State>.notifyListeners() {
    return synchronized(this) {
        listeners.toList()
    }.forEach {
        it()
    }
}
