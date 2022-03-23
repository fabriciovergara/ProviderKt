# ProviderKt

Solution inspired by [Recoil](https://github.com/facebookexperimental/Recoil), [Jotai](https://github.com/pmndrs/jotai) and [Riverpod](https://github.com/rrousselGit/river_pod)

## Usage

### Basic

```kotlin 
// Hold a boolean, false by default
val isDarkModeProvider by provider<Boolean> {
    false
}

// AppColorsProvider will rebuild every time isDarkModeProvider value changes
val appColorsProvider by provider<Colors> {
  val isDarkMode = watch(isDarkModeProvider)
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
val appColorsProvider by familyProvider<Colors, String?> { arg -> 
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

### onDisposed and state

```kotlin

val userRepositoryProvider by provider<UserRepository> { 
    val something = watch(somethingProvider)
    UserRepository(someInfo)
}

val userIdProvider by provider<String> {
    // This provider listen userRepositoryProvider and register a listener, if userRepositoryProvider changes because 
    // somethingProvider was modified, then this provider will be disposed, on Disposed will be called
    // and listener will be unregistered. After that this provider will be recreated
    val repository = watch(userRepositoryProvider)
    
    // If this provider is being recreated, then by calling get() you will receive the previous value
    // If it's the first time this provider is being created, then get() will return null
    val previousValue: String? = self.get()
    
    val listener = { userId -> 
       // Provider was already created, so get will return the current value
       val currentValue: String? = self.get()
        // If a new value is received in this listener, this provider state will be updated without recreating it
       self.set(userId)
    }
    
    self.onDisposed {
        userIdProvider.unregisterListener(listener)
    }
    
    userIdProvider.registerListener(listener)
    repository.currentUser
}

@Composable
fun AppMain() {
  ProviderScope {
    // Create provider with input value, watch() returns userId
    val userId by userIdProvider.watch()
    Text(text = "${userId}")
  }
}
```


### Disposable

```kotlin

val userIdProvider by provider<String?>(type = ProviderType.Disposable) {
    val repository = FooRepository()
    
    // By defining provider of type Disposable when no one is listening this provider, 
    // then onDispose will be called, it's state will not be cached for later queries
    // and next it being listened it will recreate from scratch
    self.onDisposed {
        repository.clear()
    }
    
    repository.currentUser
}

@Composable
fun AppMain() {
  ProviderScope {

    val listen = remember { mutableStateOf(true) }
   
    Button(
        onClick = { listen.value = !listen.value}
    ) {
        Text(text = "Toggle")
    } 
    
    // When not watching userIdProvider, the current value will be disposed
    // and the provider will be recreated from scratch
    val value = if (listen.value) {
        val userId by userIdProvider.watch()
        userId
    } else {
        "Not listening"
    }
    
    Text(text = "${value}")
}
```


### Override

```kotlin
val appColorsProvider by provider<Colors> { 
    lightColors()
}

@Composable
fun AppMain() {
  val overrideColors by remember { darkColors(primary = Purple200) }
  ProviderScope(
    overrides = setOf(isDarkModeProvider.overrideWithValue(overrideColors)
  ) {
    // watch() on all children from here will returns overridden value from overrideColors
    val colors by appColorsProvider.watch()
    MaterialTheme(
      colors = colors
    ) {
      MainPage()
    }
  }
}
```
