package dev.fabriciovergara.providerkt.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.runtime.getValue
import dev.fabriciovergara.providerkt.android.ProviderScope
import dev.fabriciovergara.providerkt.android.app.ui.theme.DarkColorPalette
import dev.fabriciovergara.providerkt.android.app.ui.theme.LightColorPalette
import dev.fabriciovergara.providerkt.android.app.ui.theme.Shapes
import dev.fabriciovergara.providerkt.android.app.ui.theme.Typography
import dev.fabriciovergara.providerkt.android.watch
import dev.fabriciovergara.providerkt.provider

val isDarkModeProvider by provider<Boolean> {
    false
}

val colorsProvider by provider<Colors> {
    val isDarkMode = watch(isDarkModeProvider)
    if (isDarkMode) DarkColorPalette else LightColorPalette
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProviderScope(
                observers = setOf(
                    ProviderObserverImpl()
                )
            ) {
                val colors by colorsProvider.watch()
                MaterialTheme(
                    colors = colors,
                    typography = Typography,
                    shapes = Shapes,
                ) {
                    MainPage()
                }
            }
        }
    }
}