package com.providerkt

import org.junit.Assert.*
import org.junit.Test

internal class ProviderTest {

    @Test
    fun `read WHEN simple provider THEN return value`() {
        val container = providerContainerOf()
        val provider = providerOf<String>(name = "name") {
            "A"
        }

        assertEquals("A", container.read(provider))
    }

    @Test
    fun `read WHEN override with provider THEN return the override value `() {
        val provider = providerOf<String>(name = "name") {
            "A"
        }

        val providerOverride = providerOf<String>(name = "name") {
            "B"
        }

        val container = providerContainerOf(
            overrides = setOf(
                provider.overrideWithProvider(providerOverride)
            )
        )

        assertEquals("B", container.read(provider))
    }

    @Test
    fun `read WHEN override multiple in multiple containers THEN return the override value from last container`() {
        val provider = providerOf<String>(name = "name") {
            "A"
        }

        val providerOverride = providerOf<String>(name = "name") {
            "B"
        }

        val providerOverride2 = providerOf<String>(name = "name") {
            "C"
        }

        val container1 = providerContainerOf(
            overrides = setOf(
                provider.overrideWithProvider(providerOverride)
            )
        )

        val container2 = providerContainerOf(
            parent = container1,
            overrides = setOf(
                provider.overrideWithProvider(providerOverride2)
            )
        )

        val container3 = providerContainerOf(
            parent = container2
        )

        assertEquals("C", container3.read(provider))
    }

    @Test
    fun `read WHEN provider depends to another THEN combine value`() {
        val container = providerContainerOf()
        val provider = providerOf<String>(name = "name") {
            "A"
        }

        val provider2 = providerOf<String>(name = "name2") {
            "B + ${read(provider)}"
        }

        assertEquals("B + A", container.read(provider2))
    }

    @Test
    fun `read WHEN update value THEN return updated value`() {
        val container = providerContainerOf()
        lateinit var update: (String) -> Unit
        val provider = providerOf<String>(name = "name") {
            update = { set(it) }
            "A"
        }

        assertEquals("A", container.read(provider))
        update("B")
        assertEquals("B", container.read(provider))
    }

    @Test
    fun `read WHEN container has no parent THEN hold state isolated`() {
        val container1 = providerContainerOf()
        var provider1Value = "A"
        val provider1 = providerOf<String>(name = "name1") {
            provider1Value
        }

        val container2 = providerContainerOf()
        val provider2 = providerOf<String>(name = "name2") {
            "B + ${read(provider1)}"
        }

        assertEquals("A", container2.read(provider1))
        assertEquals("B + A", container2.read(provider2))

        provider1Value = "B"

        assertEquals("B", container1.read(provider1))
        assertEquals("B + B", container1.read(provider2))
    }

    @Test
    fun `read WHEN container has parent THEN lookup parent for already initialized provider`() {
        val container1 = providerContainerOf()
        var provider1Value = "A"
        val provider1 = providerOf<String>(name = "name1") {
            provider1Value
        }

        val container2 = providerContainerOf(parent = container1)
        val provider2 = providerOf<String>(name = "name2") {
            "B + ${read(provider1)}"
        }

        assertEquals("A", container1.read(provider1))
        assertEquals("B + A", container2.read(provider2))

        provider1Value = "B"

        assertEquals("A", container1.read(provider1))
        assertEquals("B + A", container2.read(provider2))
    }

    @Test
    fun `read WHEN update value of watch dependency THEN return updated value`() {
        val container = providerContainerOf()
        lateinit var update: (String) -> Unit
        val provider1 = providerOf<String>(name = "name1") {
            update = { set("B") }
            "A"
        }

        var updateCount = 0
        val provider2 = providerOf<String>(name = "name2") {
            updateCount++
            "B + ${watch(provider1)}"
        }

        assertEquals("A", container.read(provider1))
        assertEquals("B + A", container.read(provider2))
        assertEquals(1, updateCount)

        update("B")

        assertEquals("B", container.read(provider1))
        assertEquals("B + B", container.read(provider2))
        assertEquals(2, updateCount)
    }

    @Test
    fun `read WHEN family provider THEN return provided argument`() {
        val container = providerContainerOf()
        val provider = familyProviderOf<String, String>(name = "name1") { arg ->
            arg
        }

        assertEquals("A", container.read(provider("A")))
    }

    @Test
    fun `read WHEN family provider and called with different arguments THEN return value for each`() {
        val container = providerContainerOf()
        var callCount = 0
        val provider = familyProviderOf<String, String>(name = "name1") { arg ->
            "$arg $callCount".also {
                callCount++
            }
        }

        assertEquals("A 0", container.read(provider("A")))
        assertEquals("A 0", container.read(provider("A")))

        assertEquals("B 1", container.read(provider("B")))
        assertEquals("B 1", container.read(provider("B")))

        assertEquals("C 2", container.read(provider("C")))
        assertEquals("C 2", container.read(provider("C")))
    }

    @Test
    fun `listen WHEN update value THEN notify change`() {
        val container = providerContainerOf()
        lateinit var update: (String) -> Unit
        val provider1 = providerOf<String>(name = "name1") {
            update = { set(it) }
            "A"
        }

        lateinit var value: String
        container.listen(provider1) { next ->
            value = next
        }

        assertEquals("A", value)

        update("B")

        assertEquals("B", value)
    }

    @Test
    fun `listen WHEN disposed THEN notify don't change`() {
        val container = providerContainerOf()
        lateinit var update: (String) -> Unit
        val provider1 = providerOf<String>(name = "name1") {
            update = { set(it) }
            "A"
        }

        lateinit var value: String
        val dispose = container.listen(provider1) { next ->
            value = next
        }

        assertEquals("A", value)

        dispose()
        update("B")

        assertEquals("A", value)

        container.listen(provider1) { next ->
            value = next
        }

        assertEquals("B", value)
    }

    @Test
    fun `listen WHEN disposable provider and there is no more listener THEN remove entry`() {
        val container = providerContainerOf()
        var providerValue = "A"
        val provider1 = providerOf<String>(name = "name1", type = ProviderType.Disposable) {
            providerValue
        }

        lateinit var value: String
        val dispose = container.listen(provider1) { next ->
            value = next
        }

        assertEquals("A", value)

        dispose()
        providerValue = "B"

        container.listen(provider1) { next ->
            value = next
        }

        assertEquals("B", value)
    }

    @Test
    fun `listen WHEN disposable provider and all listeners are disposed THEN then call onDisposed`() {
        val container = providerContainerOf()
        var onDisposeCallCount = 0
        val provider1 = providerOf<String>(name = "name1", type = ProviderType.Disposable) {
            onDisposed {
                onDisposeCallCount++
            }
            "A"
        }

        val dispose = container.listen(provider1) {}

        dispose()

        assertEquals(1, onDisposeCallCount)
    }

    @Test
    fun `listen WHEN provider and is rebuilt due to dependency change THEN then call onDisposed`() {
        val container = providerContainerOf()
        lateinit var onChange: (String) -> Unit
        val provider1 = providerOf<String>(name = "name1") {
            onChange = { set(it) }
            "A"
        }

        var onDisposeCallCount = 0
        var onDisposeCallValue: String? = null
        val provider2 = providerOf<String>(name = "name2") {
            val value1 = watch(provider1)
            onDisposed {
                onDisposeCallValue = value1
                onDisposeCallCount++
            }
            value1
        }

        assertEquals("A", container.read(provider2))

        onChange("B")

        assertEquals("B", container.read(provider2))
        assertEquals("B", onDisposeCallValue)
        assertEquals(1, onDisposeCallCount)
    }

    @Test
    fun `listen WHEN watch another that updates THEN rebuild the provider`() {
        val container = providerContainerOf()
        lateinit var update: (String) -> Unit
        val provider1 = providerOf<String>(name = "name1") {
            update = { set(it) }
            "A"
        }

        val provider2 = providerOf<String>(name = "name1") {
            val p1Value = watch(provider1)
            "B + $p1Value"
        }

        val updates = mutableMapOf<String, Int>()
        container.listen(provider2) {
            val current = updates[it] ?: 0
            updates[it] = current + 1
        }

        update("C")
        update("D")

        val entries = updates.entries.toList()
        assertEquals(3, entries.size)
        assertEquals("B + A", entries[0].key)
        assertEquals("B + C", entries[1].key)
        assertEquals("B + D", entries[2].key)
    }

    @Test
    fun `read WHEN contains cyclical dependency THEN throw error`() {
        val container = providerContainerOf()

        lateinit var provider1: Provider<String>
        lateinit var provider2: Provider<String>

        provider1 = providerOf(name = "name1") {
            read(provider2)
        }

        provider2 = providerOf(name = "name2") {
            read(provider1)
        }

        assertThrows(Error::class.java) {
            container.read(provider2)
        }
    }

    @Test
    fun `listen WHEN contains cyclical dependency THEN throw error`() {
        val container = providerContainerOf()

        lateinit var provider1: Provider<String>
        lateinit var provider2: Provider<String>

        provider1 = providerOf(name = "name1") {
            read(provider2)
        }

        provider2 = providerOf(name = "name2") {
            read(provider1)
        }

        assertThrows(Error::class.java) {
            container.listen(provider2) { }
        }
    }

    @Test
    fun `update WHEN simple provider THEN update value`() {
        val container = providerContainerOf()
        val provider = providerOf<String>(name = "name") {
            "A"
        }

        container.update(provider, "B")

        assertEquals("B", container.read(provider))
    }

    @Test
    fun `update WHEN family provider THEN update value`() {
        val container = providerContainerOf()
        val provider = familyProviderOf<String, String>(name = "name") { arg ->
            arg
        }

        container.update(provider("A"), "B")

        assertEquals("B", container.read(provider("A")))
    }


    @Test
    fun `read WHEN observer is set and first call THEN notify create`() {
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

        val provider = providerOf<String>(name = "name") {
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
    fun `read WHEN provider is not yet create and try to get self state THEN return null`() {
        val container = providerContainerOf()
        var value: String? = "SomethingElse"
        val provider = providerOf<String>(name = "name") {
            value = get()
            "A"
        }

        assertEquals("A", container.read(provider))
        assertNull(value)
    }

    @Test
    fun `update WHEN provider was already created and try to get self state THEN return previous value`() {
        val container = providerContainerOf()
        var value: String? = "SomethingElse"
        val provider1 = providerOf<String>(name = "name") {
            "A"
        }

        val provider2 = providerOf<String>(name = "name") {
            value = get()
            val value1 = watch(provider1)
            value1
        }

        assertEquals("A", container.read(provider2))
        assertNull(value)

        container.update(provider1, "B")

        assertEquals("B", container.read(provider2))
        assertEquals("A", value)
    }
}