package dev.fabriciovergara.providerkt.internal

import dev.fabriciovergara.providerkt.*

internal fun <State, WState> Container.doWatch(
    self: Provider<State>,
    provider: Provider<WState>,
    origin: Container
) {
    var dispose: VoidCallback? = null
    dispose = listen(provider) {
        dispose?.run {
            invoke()
            origin.refresh(self)
        }
    }
}
