plugins {
    kotlin("jvm") apply true
}

repositories {
    jcenter()
}

dependencies {
    api(project(":messenger-core"))
    api("redis.clients", "jedis", "3.2.0")
}