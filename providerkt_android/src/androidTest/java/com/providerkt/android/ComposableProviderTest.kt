package com.providerkt.android

import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.providerkt.*
import org.junit.Assert.assertEquals

import org.junit.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ComposableProviderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun watch_WHEN_just_watch_THEN_display_value() {
        val valueProvider = providerOf<String>(name = "") {
            "valueTest"
        }

        composeTestRule.setContent {
            ProviderScope {
                val value by valueProvider.watch()
                Text(
                    text = value
                )
            }
        }

        composeTestRule.onNodeWithText("valueTest")
            .assertIsDisplayed()
    }

    @Test
    fun watch_WHEN_watch_family_THEN_display_value() {
        val valueProvider = familyProviderOf<String, String>(name = "") { arg ->
            arg
        }

        composeTestRule.setContent {
            ProviderScope {
                val value by valueProvider("valueTest").watch()
                Text(
                    text = value
                )
            }
        }

        composeTestRule.onNodeWithText("valueTest")
            .assertIsDisplayed()
    }

    @Test
    fun watch_WHEN_watch_disposable_family_and_change_value_THEN_display_value_and_dispose_old() {
        var disposedCount = 0
        var disposedValue: String? = null
        val valueProvider =
            familyProviderOf<String, String>(name = "", type = ProviderType.Disposable) { arg ->
                onDisposed {
                    disposedCount++
                    disposedValue = arg
                }
                arg
            }

        composeTestRule.setContent {
            ProviderScope {
                val input = remember { mutableStateOf("valueTest") }
                val value by valueProvider(input.value).watch()
                Text(
                    text = value,
                    modifier = Modifier.testTag("text1")
                )

                Button(
                    onClick = {
                        input.value = "valueTest2"
                    },
                    modifier = Modifier.testTag("button")
                ) {
                    Text(text = "Click")
                }
            }
        }

        composeTestRule.onNodeWithTag("text1")
            .assert(hasText("valueTest"))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("button")
            .performClick()
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("text1")
            .assert(hasText("valueTest2"))
            .assertIsDisplayed()

        assertEquals("valueTest", disposedValue)
        assertEquals(1, disposedCount)
    }

    @Test
    fun watch_WHEN_value_changed_THEN_update_displayed_value() {
        lateinit var updateValue: (String) -> Unit
        val valueProvider = providerOf<String>(name = "") {
            updateValue = { set(it) }
            "valueTest"
        }

        composeTestRule.setContent {
            ProviderScope {
                val value by valueProvider.watch()
                Text(
                    text = value
                )
            }
        }

        composeTestRule.onNodeWithText("valueTest")
            .assertIsDisplayed()

        updateValue("valueTest2")

        composeTestRule.onNodeWithText("valueTest2")
            .assertIsDisplayed()
    }

    @Test
    fun watch_WHEN_override_by_provider_THEN_display_overridden_value() {
        val valueProvider = providerOf<String>(name = "") {
            "valueTest"
        }

        val overrideProvider = providerOf<String>(name = "") {
            "valueTest2"
        }

        composeTestRule.setContent {
            ProviderScope(
                overrides = setOf(
                    valueProvider.overrideWithProvider(overrideProvider)
                )
            ) {
                val value by valueProvider.watch()
                Text(
                    text = value
                )
            }
        }

        composeTestRule.onNodeWithText("valueTest2")
            .assertIsDisplayed()
    }

    @Test
    fun watch_WHEN_override_by_value_THEN_display_overridden_value() {
        val valueProvider = providerOf<String>(name = "") {
            "valueTest"
        }

        composeTestRule.setContent {
            ProviderScope(
                overrides = setOf(
                    valueProvider.overrideWithValue("valueTest2")
                )
            ) {
                val value by valueProvider.watch()
                Text(
                    text = value
                )
            }
        }

        composeTestRule.onNodeWithText("valueTest2")
            .assertIsDisplayed()
    }

    @Test
    fun watch_WHEN_overridden_multiple_times_THEN_display_last_overridden_value() {
        val valueProvider = providerOf<String>(name = "") {
            "valueTest"
        }

        composeTestRule.setContent {
            ProviderScope(
                overrides = setOf(
                    valueProvider.overrideWithValue("valueTest2")
                )
            ) {
                ProviderScope(
                    overrides = setOf(
                        valueProvider.overrideWithValue("valueTest3")
                    )
                ) {
                    ProviderScope(
                        overrides = setOf(
                            valueProvider.overrideWithValue("valueTest4")
                        )
                    ) {
                        val value by valueProvider.watch()
                        Text(
                            text = value
                        )
                    }
                }
            }
        }

        composeTestRule.onNodeWithText("valueTest4")
            .assertIsDisplayed()
    }

    @Test
    fun watch_WHEN_override_changes_THEN_display_new_overridden_value() {
        val valueProvider = providerOf<String>(name = "") {
            "valueTest"
        }

        composeTestRule.setContent {
            val overrideValue = remember { mutableStateOf("valueTest1") }
            ProviderScope(
                overrides = setOf(
                    valueProvider.overrideWithValue(overrideValue.value)
                )
            ) {
                val value1 by valueProvider.watch()
                Text(
                    text = value1,
                    modifier = Modifier.testTag("text1")
                )

                ProviderScope(
                    overrides = setOf(
                        valueProvider.overrideWithValue("valueTest2")
                    )
                ) {
                    val value2 by valueProvider.watch()
                    Text(
                        text = value2,
                        modifier = Modifier.testTag("text2")
                    )
                    Button(
                        onClick = {
                            overrideValue.value = "valueTest3"
                        },
                        modifier = Modifier.testTag("button")
                    ) {
                        Text(text = "Click")
                    }
                }
            }
        }

        composeTestRule.onNodeWithTag("text1")
            .assert(hasText("valueTest1"))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("text2")
            .assert(hasText("valueTest2"))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("button")
            .performClick()

        composeTestRule.onNodeWithTag("text1")
            .assert(hasText("valueTest3"))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("text2")
            .assert(hasText("valueTest2"))
            .assertIsDisplayed()
    }

    @Test
    fun watch_WHEN_override_changes_THEN_reset_child_providers() {
        val valueProvider = providerOf<String>(name = "") {
            "valueTest"
        }

        lateinit var updateValue2: (String) -> Unit
        val value2Provider = providerOf<String>(name = "") {
            updateValue2 = { set(it) }
            "value2Test"
        }

        composeTestRule.setContent {
            val overrideValue = remember { mutableStateOf("valueTest1") }
            ProviderScope(
                overrides = setOf(
                    valueProvider.overrideWithValue(overrideValue.value)
                )
            ) {
                val value1 by valueProvider.watch()
                val value2 by value2Provider.watch()
                Text(
                    text = value1,
                    modifier = Modifier.testTag("text1")
                )

                Text(
                    text = value2,
                    modifier = Modifier.testTag("text2")
                )

                Button(
                    onClick = {
                        overrideValue.value = "valueTest3"
                    },
                    modifier = Modifier.testTag("button")
                ) {
                    Text(text = "Click")
                }
            }
        }

        composeTestRule.onNodeWithTag("text1")
            .assert(hasText("valueTest1"))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("text2")
            .assert(hasText("value2Test"))
            .assertIsDisplayed()

        updateValue2("value2Test2")

        composeTestRule.onNodeWithTag("text2")
            .assert(hasText("value2Test2"))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("button")
            .performClick()

        composeTestRule.onNodeWithTag("text1")
            .assert(hasText("valueTest3"))
            .assertIsDisplayed()

        composeTestRule.onNodeWithTag("text2")
            .assert(hasText("value2Test"))
            .assertIsDisplayed()
    }


    @Test
    fun listen_WHEN_just_listen_THEN_receive_value() {
        val valueProvider = providerOf<String>(name = "") {
            "valueTest"
        }

        composeTestRule.setContent {
            ProviderScope {
                val state = remember { mutableStateOf("") }
                valueProvider.listen { next ->
                    state.value = next
                }

                Text(
                    text = state.value
                )
            }
        }

        composeTestRule.onNodeWithText("valueTest")
            .assertIsDisplayed()
    }


}