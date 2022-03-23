package com.providerkt.android.app

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.providerkt.*
import com.providerkt.android.watch

val toDoListProvider by provider<List<String>>(type = ProviderType.Disposable) {
    listOf()
}

@Composable
fun MainPageContent() {
    val list = toDoListProvider.watch()
    LazyColumn {
        items(list.value.size + 1) { i ->
            if (i < list.value.size) {
                Text(
                    text = list.value[i]
                )
            } else {
                val input = remember { mutableStateOf("") }
                TextField(
                    value = input.value,
                    modifier = Modifier.fillMaxWidth(),
                    onValueChange = { newText ->
                        input.value = newText
                    }
                )
                Button(
                    onClick = {
                        list.value += input.value
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Add")
                }
            }
        }
    }
}