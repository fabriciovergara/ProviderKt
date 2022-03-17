# ProviderKt

Solution inspired by [Recoil](https://github.com/facebookexperimental/Recoil), [Jotai](https://github.com/pmndrs/jotai) and [Riverpod](https://github.com/rrousselGit/river_pod)

#### Compose

```kotlin 
// Hold a boolean, false by default
val isDarkModeProvider = providerOf<Boolean>(name = "isDarkModeProvider") {
    false
}

// AppColorsProvider will rebuild every time isDarkModeProvider value changes
val appColorsProvider = providerOf<Colors>(name = "appColorsProvider") { ref-> 
  val isDarkMode = ref.watch(isDarkModeProvider)
  if (isDarkMode) darkColors() else lightColors()
}


@Composable
fun AppMain() {
  ProviderScope {
    // watch() returns a MutableState<Colors>, so this composable will rebuild everytime appColorsProvider changes
    val colors by appColorsProvider.watch()
    MaterialTheme(
      colors = colors
    ) {
      MainPage()
    }
  }
}

@Composable
fun MainPage() {
  // watch() returns a MutableState<Boolean> and by using '=' instead of 'by' you can have access to state setter
  val isDarkModeState = isDarkModeProvider.watch()
  Button(
    onClick = { isDarkModeState.value = !isDarkModeState.value },
  ) {
    Text(text = "Toggle (${isDarkModeState.value})")
  }
}


```
