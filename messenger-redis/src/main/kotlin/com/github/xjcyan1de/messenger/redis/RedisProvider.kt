package com.github.xjcyan1de.messenger.redis

import com.github.xjcyan1de.cyanlibz.redis.Redis

interface RedisProvider {
    val redis: Redis
    val globalCredentials: RedisCredentials

    fun getRedis(credentials: RedisCredentials): Redis
}