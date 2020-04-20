# messenger
[![](https://jitpack.io/v/XjCyan1de/Messenger.svg)](https://jitpack.io/#XjCyan1de/Messenger)

Kotlin Gradle DSL:
```kotlin
repositories {
    maven { setUrl("https://jitpack.io") }
}

dependencies {
    implementation("com.github.xjcyan1de.messenger", "messenger-core", "1.0.1")
}
```

### Example

```kotlin
val channel = messenger.getChannel<String>("my_channel")

// send channel
channel.sendMessage("Hello World!")

// listen channel
channel.newAgent { channelAgent, message -> 
    println("New Message: $message") // New Message: Hello World!
}
```