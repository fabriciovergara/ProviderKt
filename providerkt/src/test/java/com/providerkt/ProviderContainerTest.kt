package com.providerkt

import org.junit.Assert.*
import org.junit.Test

class ProviderContainerTest {

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
            "B + ${it.read(provider)}"
        }

        assertEquals("B + A", container.read(provider2))
    }

    @Test
    fun `read WHEN update value THEN return updated value`() {
        val container = providerContainerOf()
        lateinit var update: () -> Unit
        val provider = providerOf<String>(name = "name") {
            update = { it.state = "B" }
            "A"
        }

        assertEquals("A", container.read(provider))
        update()
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
            "B + ${it.read(provider1)}"
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
            "B + ${it.read(provider1)}"
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
        lateinit var update: () -> Unit
        val provider1 = providerOf<String>(name = "name1") {
            update = { it.state = "B" }
            "A"
        }

        var updateCount = 0
        val provider2 = providerOf<String>(name = "name2") {
            updateCount++
            "B + ${it.watch(provider1)}"
        }

        assertEquals("A", container.read(provider1))
        assertEquals("B + A", container.read(provider2))
        assertEquals(1, updateCount)

        update()

        assertEquals("B", container.read(provider1))
        assertEquals("B + B", container.read(provider2))
        assertEquals(2, updateCount)
    }

    @Test
    fun `read WHEN family provider THEN return provided argument`() {
        val container = providerContainerOf()
        val provider = familyProviderOf<String, String>(name = "name1") { _, arg ->
            arg
        }

        assertEquals("A", container.read(provider("A")))
    }

    @Test
    fun `read WHEN family provider and called with different arguments THEN return value for each`() {
        val container = providerContainerOf()
        var callCount = 0
        val provider = familyProviderOf<String, String>(name = "name1") { _, arg ->
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
        lateinit var update: () -> Unit
        val provider1 = providerOf<String>(name = "name1") {
            update = { it.state = "B" }
            "A"
        }

        lateinit var value: String
        container.listen(provider1) { next ->
            value = next
        }

        assertEquals("A", value)

        update()

        assertEquals("B", value)
    }

    @Test
    fun `listen WHEN disposed THEN notify don't change`() {
        val container = providerContainerOf()
        lateinit var update: () -> Unit
        val provider1 = providerOf<String>(name = "name1") {
            update = { it.state = "B" }
            "A"
        }

        lateinit var value: String
        val dispose = container.listen(provider1) { next ->
            value = next
        }

        assertEquals("A", value)

        dispose()
        update()

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
        val provider1 = disposableProviderOf<String>(name = "name1") {
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
    fun `listen WHEN disposable provider and disposed THEN then call onDisposed`() {
        val container = providerContainerOf()
        var onDisposeCallCount = 0
        val provider1 = disposableProviderOf<String>(name = "name1") {
            it.onDisposed {
                onDisposeCallCount++
            }
            "A"
        }

        val dispose = container.listen(provider1) {}

        dispose()

        assertEquals(1, onDisposeCallCount)
    }


    @Test
    fun `read WHEN contains cyclical dependency THEN throw error`() {
        val container = providerContainerOf()

        lateinit var provider1: Provider<String>
        lateinit var provider2: Provider<String>

        provider1 = providerOf(name = "name1") {
            it.read(provider2)
        }

        provider2 = providerOf(name = "name2") {
            it.read(provider1)
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
            it.read(provider2)
        }

        provider2 = providerOf(name = "name2") {
            it.read(provider1)
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
        val provider = familyProviderOf<String, String>(name = "name") { ref, arg ->
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

        val provider = providerOf<String>(name = "name") { ref ->
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
}