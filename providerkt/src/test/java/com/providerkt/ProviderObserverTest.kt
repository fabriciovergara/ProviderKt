package com.providerkt

import org.junit.Assert.*
import org.junit.Test

internal class ProviderObserverTest {

    @Test
    fun `onCreated WHEN observer is read for the first time THEN notify create`() {
        var notifiedCount = 0
        var notifiedProvider: Provider<*>? = null
        var notifiedValue: Any? = null
        val observer = object : ProviderObserver {
            override fun onCreated(provider: Provider<*>, value: Any?) {
                notifiedProvider = provider
                notifiedValue = value
                notifiedCount++
            }
        }

        val container = providerContainerOf(
            observers = setOf(observer)
        )

        val provider by provider<String> {
            "A"
        }

        container.read(provider)
        container.read(provider)
        container.read(provider)
        container.read(provider)

        assertSame(provider, notifiedProvider)
        assertEquals("A", notifiedValue)
        assertEquals(1, notifiedCount)
    }

    @Test
    fun `onUpdated WHEN observer is updated THEN notify update`() {
        var notifiedCount = 0
        var notifiedProvider: Provider<*>? = null
        var notifiedValue: Any? = null
        var notifiedOldValue: Any? = null
        val observer = object : ProviderObserver {
            override fun onUpdated(provider: Provider<*>, old: Any?, value: Any?) {
                notifiedProvider = provider
                notifiedValue = value
                notifiedOldValue = old
                notifiedCount++
            }
        }

        val container = providerContainerOf(
            observers = setOf(observer)
        )

        val provider by provider<String> {
            "A"
        }

        container.read(provider)
        container.update(provider, "B")

        assertSame(provider, notifiedProvider)
        assertEquals("B", notifiedValue)
        assertEquals("A", notifiedOldValue)
        assertEquals(1, notifiedCount)
    }

    @Test
    fun `onDisposed WHEN observer is disposed THEN notify dispose`() {
        var notifiedCount = 0
        var notifiedProvider: Provider<*>? = null
        var notifiedValue: Any? = null
        val observer = object : ProviderObserver {
            override fun onDisposed(provider: Provider<*>, value: Any?) {
                notifiedProvider = provider
                notifiedValue = value
                notifiedCount++
            }
        }

        val container = providerContainerOf(
            observers = setOf(observer)
        )

        val provider by provider<String>( type = ProviderType.Disposable) {
            "A"
        }

        val dispose = container.listen(provider) { }
        dispose()

        assertSame(provider, notifiedProvider)
        assertEquals("A", notifiedValue)
        assertEquals(1, notifiedCount)
    }

    @Test
    fun `onCreated WHEN parent of container also has observer THEN notify create from parent and child`() {
        var notifiedParentCount = 0
        var notifiedParentProvider: Provider<*>? = null
        var notifiedParentValue: Any? = null
        val parentObserver = object : ProviderObserver {
            override fun onCreated(provider: Provider<*>, value: Any?) {
                notifiedParentProvider = provider
                notifiedParentValue = value
                notifiedParentCount++
            }
        }

        var notifiedCount = 0
        var notifiedProvider: Provider<*>? = null
        var notifiedValue: Any? = null
        val observer = object : ProviderObserver {
            override fun onCreated(provider: Provider<*>, value: Any?) {
                notifiedProvider = provider
                notifiedValue = value
                notifiedCount++
            }
        }

        val parent = providerContainerOf(
            observers = setOf(parentObserver)
        )

        val container = providerContainerOf(
            parent = parent,
            observers = setOf(observer),
        )

        val provider by provider<String> {
            "A"
        }

        container.read(provider)
        container.read(provider)
        container.read(provider)
        container.read(provider)

        assertSame(provider, notifiedParentProvider)
        assertEquals("A", notifiedParentValue)
        assertEquals(1, notifiedParentCount)

        assertSame(provider, notifiedProvider)
        assertEquals("A", notifiedValue)
        assertEquals(1, notifiedCount)
    }
}