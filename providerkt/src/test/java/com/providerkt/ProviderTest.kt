package com.providerkt

import org.junit.Assert.*
import org.junit.Test

internal class ProviderTest {

    @Test
    fun `name WHEN name not provided THEN use property name`() {
        val provider by provider<String> {
            "A"
        }

        assertEquals("provider", provider.name)
    }

    @Test
    fun `name WHEN name provided THEN use given name`() {
        val provider by provider<String>("Name") {
            "A"
        }

        assertEquals("Name", provider.name)
    }

    @Test
    fun `name WHEN family and name provided THEN use given name`() {
        val provider by familyProvider<String, String>(FamilyName("Name")) {
            "A"
        }

        assertEquals("Name", provider("B").name)
    }

    @Test
    fun `name WHEN family and factory name provided THEN use created name`() {
        val provider by familyProvider<String, String>(FamilyName { "Name_$it" }) {
            "A"
        }

        assertEquals("Name_B", provider("B").name)
    }

    @Test
    fun `name WHEN called from provider lambda THEN return self name`() {
        val container = providerContainerOf()
        val provider by provider<String> {
            "A ${self.name}"
        }

        assertEquals("A provider", container.read(provider))
    }
}