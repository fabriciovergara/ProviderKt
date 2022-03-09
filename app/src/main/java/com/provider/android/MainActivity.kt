package com.provider.android

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.provider.android.provider.*
import com.provider.android.ui.theme.ProviderKt_AndroidTheme
import com.provider.*
import kotlin.concurrent.thread

var i = 0;
val A_provider = providerOf<Int>(name = "A_provider") { ref ->
    thread {
        while (true) {
            Thread.sleep(5000)
            ref.state = i++
        }
    }
    i
}

val B_provider = providerOf<String>(name = "B_provider") { ref ->
    val aValue = ref.watch(A_provider)
    "B ${aValue}"
}

val C_provider = familyProviderOf<String, String>(name = "C_provider") { ref, arg ->
    arg
}

val D_provider = disposableFamilyProviderOf<String, String>(name = "D_provider") { ref, arg ->
    ref.onDisposed = {
        Log.d("ProviderKt", "D_provider disposed $arg")
    }
    arg
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ProviderScope {
                ProviderKt_AndroidTheme {
                    // A surface container using the 'background' color from the theme
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colors.background
                    ) {
                        Column {
                            ProviderScope {
                                val valueA by A_provider.observeAsState()
                                val valueB by B_provider.observeAsState()
                                val valueC by C_provider("Yolo").observeAsState()
                                val valueD by D_provider("D ${if (valueA % 2 != 0) valueA - 1 else valueA}").observeAsState()
                                Greeting(
                                    "valueA=$valueA\nvalueB=$valueB\nvalueC=$valueC\nvalueD=$valueD"
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
    viewModelProviderOf()
    ProviderKt_AndroidTheme {
        Greeting("Android")
    }
}