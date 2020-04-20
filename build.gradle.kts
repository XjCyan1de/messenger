plugins {
    `java-library`
    java
}

repositories {
    jcenter()
}

dependencies {
    api(project(":messenger-core"))
}

allprojects {
    buildDir = File("$rootDir/build")
}