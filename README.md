# ProviderKt

Solution inspired by [Recoil](https://github.com/facebookexperimental/Recoil), [Jotai](https://github.com/pmndrs/jotai) and [Riverpod](https://github.com/rrousselGit/river_pod)

## Usage

### Basic

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

### Family

```kotlin
// AppColorsProvider will rebuild every time isDarkModeProvider value changes and will also receive a extra arg sent by the callers
val appColorsProvider = familyProviderOf<Colors, String?>(name = "appColorsProvider") { ref, arg -> 
  if (arg == "dark") darkColors() else lightColors()
}


@Composable
fun AppMain() {
  ProviderScope {
    // Create provider with input value, watch() returns darkColors
    val colors by appColorsProvider("dark").watch()
    
    // It will just return the previous created value, without new computation
    val colorsAgain by appColorsProvider("dark").watch()

    MaterialTheme(
      colors = colors
    ) {
      MainPage()
    }
  }
}
```


### Disposable and State

```kotlin

val userRepositoryProvider = providerOf<UserRepository>(name = "userRepositoryProvider") { ref -> 
    val something = ref.watch(somethingProvider)
    UserRepository(someInfo)
}

val userIdProvider = disposableProviderOf<String?>(name = "userIdProvider") { ref -> 
    // This provider listen userRepositoryProvider and register a listener, if userRepositoryProvider changes because 
    // somethingProvider was modified, then this provider will be disposed, on Disposed will be called
    // and listener will be unregistered. After that this provider will be recreated
    val repository = ref.watch(userRepositoryProvider)
    
    val listener = { userId -> 
        // If a new value is received in this listener, this provider state will be updated without recreating it
        ref.state = userId
    }
    
    ref.onDisposed { 
        userIdProvider.unregisterListener(listener)
    }
    
    userIdProvider.registerListener(listener)
    repository.currentUser
}

@Composable
fun AppMain() {
  ProviderScope {
    // Create provider with input value, watch() returns darkColors
    val userId by userIdProvider.watch()
    Text(text = "${userId}")
  }
}
```


### Override

```kotlin
val appColorsProvider = providerOf<Colors>(name = "appColorsProvider") { ref-> 
    lightColors()
}

@Composable
fun AppMain() {
  val overrideColors by remeber { darkColors(primary = Purple200) }
  ProviderScope(
    overrides = setOf(isDarkModeProvider.overrideWithValue(overrideColors)
  ) {
    // watch() on all childs from here will returns overridden value from overrideColors
    val colors by appColorsProvider.watch()
    MaterialTheme(
      colors = colors
    ) {
      MainPage()
    }
  }
}
```
