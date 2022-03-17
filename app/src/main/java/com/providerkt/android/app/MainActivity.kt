package com.providerkt.android.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.getValue
import com.providerkt.*
import com.providerkt.android.ProviderScope
import com.providerkt.android.app.ui.theme.AndroidTheme
import com.providerkt.android.watch

val isDarkModeProvider = providerOf<Boolean>(name = "isDarkModeProvider") {
    false
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProviderScope {
                val isDarkMode by isDarkModeProvider.watch()
                AndroidTheme(darkTheme = isDarkMode) {
                    MainPage()
                }
            }
        }
    }
}