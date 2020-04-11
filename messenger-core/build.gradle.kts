plugins {
    kotlin("jvm") apply true
}

repositories {
    jcenter()
}

dependencies {
    api(kotlin("stdlib-jdk8"))
    api("org.jetbrains.kotlinx", "kotlinx-coroutines-core", "1.3.5")
    api("com.google.guava", "guava", "28.2-jre")
    api("com.google.code.gson", "gson", "2.8.6")
}