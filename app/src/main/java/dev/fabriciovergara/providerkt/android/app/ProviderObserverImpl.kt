package dev.fabriciovergara.providerkt.android.app

import android.util.Log
import dev.fabriciovergara.providerkt.Provider
import dev.fabriciovergara.providerkt.ProviderObserver

class ProviderObserverImpl : ProviderObserver {

    override fun onCreated(provider: Provider<*>, value: Any?) {
        Log.d("ProviderKt", "onCreated(${provider.name}, $value)")
    }

    override fun onUpdated(provider: Provider<*>, old: Any?, value: Any?) {
        Log.d("ProviderKt", "onUpdated(${provider.name}, $old $value)")
    }

    override fun onDisposed(provider: Provider<*>, value: Any?) {
        Log.d("ProviderKt", "onDisposed(${provider.name}, $value)")
    }
}