package com.providerkt

import org.junit.Assert.*
import org.junit.Test

internal class ProviderContainerUpdateTest {

    @Test
    fun `update WHEN simple provider THEN update value`() {
        val container = providerContainerOf()
        val provider by provider<String> {
            "A"
        }

        container.update(provider, "B")

        assertEquals("B", container.read(provider))
    }

    @Test
    fun `update WHEN family provider THEN update value`() {
        val container = providerContainerOf()
        val provider by familyProvider<String, String> { arg ->
            arg
        }

        container.update(provider("A"), "B")

        assertEquals("B", container.read(provider("A")))
    }

    @Test
    fun `update WHEN provider was already created and try to get self state THEN return previous value`() {
        val container = providerContainerOf()
        var value: String? = "SomethingElse"
        val provider1 by provider<String> {
            "A"
        }

        val provider2 by provider<String> {
            value = self.get()
            val value1 = watch(provider1)
            value1
        }

        assertEquals("A", container.read(provider2))
        assertNull(value)

        container.update(provider1, "B")

        assertEquals("B", container.read(provider2))
        assertEquals("A", value)
    }

    @Test
    fun `update WHEN provider was not created and value updated THEN don't notify onUpdated`() {
        val container = providerContainerOf()
        var onUpdatedCount = 0

        val provider by provider<String> {
            self.onUpdated {
                onUpdatedCount++
            }

            "A"
        }

        container.update(provider, "B")
        assertEquals("B", container.read(provider))
        assertEquals(0, onUpdatedCount)
    }

    @Test
    fun `update WHEN provider was created and value updated THEN notify onUpdated`() {
        val container = providerContainerOf()
        var onUpdatedCount = 0
        var onUpdatedValue0: String? = ""
        var onUpdatedValue1 = ""
        var onUpdatedValue2 = ""

        val provider by provider<String> {
            onUpdatedValue0 = self.get()
            self.onUpdated { next ->
                onUpdatedCount++
                onUpdatedValue1 = self.get()!!
                onUpdatedValue2 = next
            }

            "A"
        }

        assertEquals("A", container.read(provider))

        container.update(provider, "B")

        assertEquals("B", container.read(provider))
        assertEquals(1, onUpdatedCount)
        assertNull(onUpdatedValue0)
        assertEquals("B", onUpdatedValue1)
        assertEquals("B", onUpdatedValue2)
    }

    @Test(expected = StackOverflowError::class)
    fun `update WHEN called set without exit condition when updated THEN throw error`() {
        val container = providerContainerOf()

        val provider by provider<String> {
            self.onUpdated { next ->
                self.set(next)
            }

            "A"
        }

        assertEquals("A", container.read(provider))
        container.update(provider, "B")
    }

    @Test
    fun `update WHEN called set with exit condition when updated THEN update value`() {
        val container = providerContainerOf()
        var onUpdatedCount = 0
        val provider by provider<String> {
            self.onUpdated { next ->
                onUpdatedCount++
                if (onUpdatedCount < 5) {
                    self.set("$next$onUpdatedCount")
                }
            }

            "A"
        }

        assertEquals("A", container.read(provider))

        container.update(provider, "B")

        assertEquals("B1234", container.read(provider))
        assertEquals(5, onUpdatedCount)
    }
}