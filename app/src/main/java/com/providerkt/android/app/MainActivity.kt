package com.providerkt.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.*
import androidx.compose.runtime.getValue
import com.providerkt.*
import com.providerkt.android.ProviderScope
import com.providerkt.android.app.ui.theme.*
import com.providerkt.android.watch

val isDarkModeProvider = providerOf<Boolean>(name = "isDarkModeProvider") {
    false
}

val colorsProvider = providerOf<Colors>(name = "colorsProvider") { ref ->
    val isDarkMode = ref.watch(isDarkModeProvider)
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