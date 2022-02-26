import com.provider.android.provider.Provider
import com.provider.android.provider.ProviderContainer
import com.provider.android.provider.overrideWithProvider
import com.provider.android.provider.overrideWithValue
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderTest {

    @Test
    fun `read WHEN simple provider THEN return value`() {
        val container = ProviderContainer()
        val provider = Provider<String>(name = "name") {
            "A"
        }

        assertEquals("A", container.read(provider))
    }

    @Test
    fun `read WHEN override with provider THEN return the override value `() {
        val provider = Provider<String>(name = "name") {
            "A"
        }

        val providerOverride = Provider<String>(name = "name") {
            "B"
        }

        val container = ProviderContainer(
            overrides = setOf(
                provider.overrideWithProvider(providerOverride)
            )
        )

        assertEquals("B", container.read(provider))
    }

    @Test
    fun `read WHEN override with value THEN return the override value `() {
        val provider = Provider<String>(name = "name") {
            "A"
        }

        val container = ProviderContainer(
            overrides = setOf(
                provider.overrideWithValue("B")
            )
        )

        assertEquals("B", container.read(provider))
    }

    @Test
    fun `read WHEN override multiple in multiple containers THEN return the override value from last container`() {
        val provider = Provider<String>(name = "name") {
            "A"
        }

        val providerOverride = Provider<String>(name = "name") {
            "B"
        }

        val providerOverride2 = Provider<String>(name = "name") {
            "C"
        }

        val container1 = ProviderContainer(
            overrides = setOf(
                provider.overrideWithProvider(providerOverride)
            )
        )

        val container2 = ProviderContainer(
            parent = container1,
            overrides = setOf(
                provider.overrideWithProvider(providerOverride2)
            )
        )

        val container3 = ProviderContainer(
            parent = container2
        )

        assertEquals("C", container3.read(provider))
    }

    @Test
    fun `read WHEN provider depends to another THEN combine value`() {
        val container = ProviderContainer()
        val provider = Provider<String>(name = "name") {
            "A"
        }

        val provider2 = Provider<String>(name = "name2") {
            "B + ${it.read(provider)}"
        }

        assertEquals("B + A", container.read(provider2))
    }

    @Test
    fun `read WHEN update value THEN return updated value`() {
        val container = ProviderContainer()
        lateinit var update: () -> Unit
        val provider = Provider<String>(name = "name") {
            update = { it.state = "B" }
            "A"
        }

        assertEquals("A", container.read(provider))
        update()
        assertEquals("B", container.read(provider))
    }

    @Test
    fun `read WHEN container has no parent THEN hold state isolated`() {
        val container1 = ProviderContainer()
        var provider1Value = "A"
        val provider1 = Provider<String>(name = "name1") {
            provider1Value
        }

        val container2 = ProviderContainer()
        val provider2 = Provider<String>(name = "name2") {
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
        val container1 = ProviderContainer()
        var provider1Value = "A"
        val provider1 = Provider<String>(name = "name1") {
            provider1Value
        }

        val container2 = ProviderContainer(parent = container1)
        val provider2 = Provider<String>(name = "name2") {
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
        val container = ProviderContainer()
        lateinit var update: () -> Unit
        val provider1 = Provider<String>(name = "name1") {
            update = { it.state = "B" }
            "A"
        }

        var updateCount = 0
        val provider2 = Provider<String>(name = "name2") {
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
    fun `listen WHEN update value THEN notify change`() {
        val container = ProviderContainer()
        lateinit var update: () -> Unit
        val provider1 = Provider<String>(name = "name1") {
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
        val container = ProviderContainer()
        lateinit var update: () -> Unit
        val provider1 = Provider<String>(name = "name1") {
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
    }
}