package dev.fabriciovergara.providerkt

import org.junit.Assert.*
import org.junit.Test

internal class ProviderContainerListenTest {

    @Test
    fun `listen WHEN update value THEN notify change`() {
        val container = providerContainerOf()
        lateinit var update: (String) -> Unit
        val provider1 by provider<String> {
            update = { self.set(it) }
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
        val provider1 by provider<String> {
            update = { self.set(it) }
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
        val provider1 by provider<String>(type = ProviderType.Disposable) {
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
        val provider1 by provider<String>(type = ProviderType.Disposable) {
            self.onDisposed {
                onDisposeCallCount++
            }
            "A"
        }

        val dispose = container.listen(provider1) {}

        dispose()

        assertEquals(1, onDisposeCallCount)
    }

    @Test
    fun `listen WHEN watch another that updates THEN rebuild the provider`() {
        val container = providerContainerOf()
        lateinit var update: (String) -> Unit
        val provider1 by provider<String> {
            update = { self.set(it) }
            "A"
        }

        val provider2 by provider<String> {
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
}