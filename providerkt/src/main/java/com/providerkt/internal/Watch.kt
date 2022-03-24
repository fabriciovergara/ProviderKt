package com.providerkt.internal

import com.providerkt.Dispose
import com.providerkt.Provider

internal fun <State, WState> Container.doWatch(
    self: Provider<State>,
    provider: Provider<WState>,
    origin: Container
) {
    var dispose: Dispose? = null
    dispose = listen(provider) {
        dispose?.run {
            invoke()
            origin.reset(self)
        }
    }
}
