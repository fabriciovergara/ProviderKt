package com.providerkt.android.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.providerkt.android.watch

@Composable
fun MainPage() {
    Scaffold(

        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(id = R.string.app_name))
                },
                actions = {
                    val isDarkMode = isDarkModeProvider.watch()
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Localized description",
                        modifier = Modifier.clickable {
                            isDarkMode.value = !isDarkMode.value
                        }
                    )
                }
            )
        }
    ) {
        Box(modifier = Modifier.padding(16.dp)) {
            MainPageContent()
        }
    }
}