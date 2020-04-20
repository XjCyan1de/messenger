plugins {
    kotlin("jvm") version "1.3.71"
}

repositories {
    jcenter()
}

dependencies {
    api(project(":messenger-core"))
    api("redis.clients", "jedis", "3.2.0")
}