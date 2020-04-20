plugins {
    kotlin("jvm") version "1.3.71"
}

repositories {
    jcenter()
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.5")
    api("com.google.guava", "guava", "29.0-jre")
    api("com.google.code.gson", "gson", "2.8.6")
}