package com.providerkt.internal

import com.providerkt.Provider

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

internal fun <State> Provider<State>.isEmpty(): Boolean {
    return synchronized(this) {
        listeners.isEmpty()
    }
}

internal fun <State> Provider<State>.notifyListeners() {
    return synchronized(this) {
        listeners.toList()
    }.forEach {
        it()
    }
}
