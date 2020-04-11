plugins {
    kotlin("jvm") apply true
}

repositories {
    jcenter()
    maven { setUrl("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") }
    maven { setUrl("https://oss.sonatype.org/content/groups/public/") }
}

dependencies {
    api(project(":messenger-core"))
    compileOnly("org.spigotmc", "spigot-api", "1.15.2-R0.1-SNAPSHOT")
}