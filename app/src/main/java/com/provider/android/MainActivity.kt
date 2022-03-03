package com.provider.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.provider.android.provider.*
import com.provider.android.ui.theme.ProviderKt_AndroidTheme
import kotlin.concurrent.thread

var i = 0;
val A_provider = providerOf<String>(name = "A_provider") { ref ->
    thread {
        while (true) {
            Thread.sleep(5000)
            ref.state = "A${i++}"
        }
    }
    "A"
}

val B_provider = providerOf<String>(name = "B_provider") { ref ->
    val aValue = ref.watch(A_provider)
    "B ${aValue}"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProviderContainerComposable {
                ProviderKt_AndroidTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        Column {
                            Greeting("Id=${LocalProviderContainer.current?.hashCode()}")
                            ProviderContainerComposable {
                                val value by B_provider.observeAsState()
                                Greeting(
                                    "Id=${LocalProviderContainer.current?.hashCode()} Value=$value"
                                )

                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun Greeting(name: String) {
    Text(text = "$name")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ProviderKt_AndroidTheme {
        Greeting("Android")
    }
}