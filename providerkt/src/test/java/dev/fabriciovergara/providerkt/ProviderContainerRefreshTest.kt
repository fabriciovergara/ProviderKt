package dev.fabriciovergara.providerkt

import org.junit.Assert.*
import org.junit.Test

internal class ProviderContainerRefreshTest {

    @Test
    fun `refresh WHEN when provider never created before THEN just create once`() {
        val container = providerContainerOf()

        var createCount = 0
        val provider by provider<String> {
            val count = ++createCount
            "A${count}"
        }

        container.refresh(provider)

        assertEquals("A1", container.read(provider))
        assertEquals(1, createCount)
    }

    @Test
    fun `refresh WHEN when provider created before THEN recreate it`() {
        val container = providerContainerOf()

        var updateCount = 0
        var disposeCount = 0
        var disposeValue = ""
        var createCount = 0
        val provider by provider<String> {
            val count = ++createCount
            val value =  "A${count}"
            self.onDisposed {
                disposeValue = value
                disposeCount++
            }

            self.onUpdated {
                updateCount++
            }

            value
        }

        assertEquals("A1", container.read(provider))

        container.refresh(provider)

        assertEquals("A2",  container.read(provider))
        assertEquals(2, createCount)
        assertEquals(1, disposeCount)
        assertEquals("A1", disposeValue)
        assertEquals(0, updateCount)
    }
}