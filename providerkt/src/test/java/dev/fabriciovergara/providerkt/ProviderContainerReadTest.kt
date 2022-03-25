package dev.fabriciovergara.providerkt

import org.junit.Assert.*
import org.junit.Test

internal class ProviderContainerReadTest {

    @Test
    fun `read WHEN simple provider THEN return value`() {
        val container = providerContainerOf()
        val provider by provider<String> {
            "A"
        }

        assertEquals("A", container.read(provider))
    }

    @Test
    fun `read WHEN override with provider THEN return the override value `() {
        val provider by provider<String> {
            "A"
        }

        val providerOverride by provider<String> {
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
    fun `read WHEN depends on override provider which throws THEN don't throw`() {
        val provider by provider<String> {
            error("Should be overridden")
        }

        val providerOverride by provider<String> {
            "B"
        }

        val provider2 by provider<String> {
            watch(provider)
        }

        val container = providerContainerOf(
            overrides = setOf(
                provider.overrideWithProvider(providerOverride)
            )
        )

        assertEquals("B", container.read(provider2))
    }

    @Test
    fun `read WHEN depends on override provider which throws from child container THEN don't throw`() {
        val provider by provider<String> {
            error("Should be overridden")
        }

        val providerOverride by provider<String> {
            "B"
        }

        val provider2 by provider<String> {
            watch(provider)
        }

        val container = providerContainerOf()

        val container2 = providerContainerOf(
            parent = container,
            overrides = setOf(
                provider.overrideWithProvider(providerOverride)
            )
        )

        assertEquals("B", container2.read(provider2))
    }

    @Test
    fun `read WHEN override multiple in multiple containers THEN return the override value from last container`() {
        val provider by provider<String> {
            error("Should be overridden")
        }

        val provider2 by provider<String> {
            watch(provider)
        }

        val providerOverride by provider<String> {
            "B"
        }

        val providerOverride2 by provider<String> {
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

        assertEquals("C", container3.read(provider2))
    }

    @Test
    fun `read WHEN provider depends to another THEN combine value`() {
        val container = providerContainerOf()
        val provider by provider<String> {
            "A"
        }

        val provider2 by provider<String> {
            "B + ${read(provider)}"
        }

        assertEquals("B + A", container.read(provider2))
    }

    @Test
    fun `read WHEN update value THEN return updated value`() {
        val container = providerContainerOf()
        lateinit var update: (String) -> Unit
        val provider by provider<String> {
            update = { self.set(it) }
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
        val provider1 by provider<String> {
            provider1Value
        }

        val container2 = providerContainerOf()
        val provider2 by provider<String> {
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
        val provider1 by provider<String> {
            provider1Value
        }

        val container2 = providerContainerOf(
            parent = container1
        )

        val provider2 by provider<String> {
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
        val provider1 by provider<String> {
            update = { self.set("B") }
            "A"
        }

        var updateCount = 0
        val provider2 by provider<String> {
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
        val provider by familyProvider<String, String> { arg ->
            arg
        }

        assertEquals("A", container.read(provider("A")))
    }

    @Test
    fun `read WHEN family provider and called with different arguments THEN return value for each`() {
        val container = providerContainerOf()
        var callCount = 0
        val provider by familyProvider<String, String> { arg ->
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
    fun `read WHEN provider and is rebuilt due to dependency change THEN then call onDisposed`() {
        val container = providerContainerOf()
        lateinit var onChange: (String) -> Unit
        val provider1 by provider<String> {
            onChange = { self.set(it) }
            "A"
        }

        var onDisposeCallCount = 0
        var onDisposeCallValue: String? = null
        val provider2 by provider<String> {
            val value1 = watch(provider1)
            self.onDisposed {
                onDisposeCallValue = value1
                onDisposeCallCount++
            }
            value1
        }

        assertEquals("A", container.read(provider2))

        onChange("B")

        assertEquals("B", container.read(provider2))
        assertEquals("A", onDisposeCallValue)
        assertEquals(1, onDisposeCallCount)
    }

    @Test
    fun `read WHEN contains cyclical dependency THEN throw error`() {
        val container = providerContainerOf()

        lateinit var provider2: Provider<String>

        val provider1 by provider<String> {
            read(provider2)
        }

        val providerOther by provider<String> {
            read(provider1)
        }

        provider2 = providerOther

        assertThrows(Error::class.java) {
            container.read(provider2)
        }
    }

    @Test(expected = IllegalStateException::class)
    fun `read WHEN provider is not yet create and try to set self state THEN throw error`() {
        val container = providerContainerOf()
        val provider by provider<String> {
            self.set("SomethingElse")
            "A"
        }

        container.read(provider)
    }

    @Test
    fun `read WHEN provider is not yet create and try to get self state THEN return null`() {
        val container = providerContainerOf()
        var value: String? = "SomethingElse"
        val provider by provider<String> {
            value = self.get()
            "A"
        }

        assertEquals("A", container.read(provider))
        assertNull(value)
    }
}