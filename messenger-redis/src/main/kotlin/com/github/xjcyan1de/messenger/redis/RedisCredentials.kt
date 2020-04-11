package com.github.xjcyan1de.messenger.redis

data class RedisCredentials
@JvmOverloads
constructor(
        val address: String = "localhost",
        val port: Int = 6379,
        val password: String = ""
)