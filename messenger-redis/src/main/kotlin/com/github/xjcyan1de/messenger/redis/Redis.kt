package com.github.xjcyan1de.cyanlibz.redis

import com.github.xjcyan1de.messenger.Messenger
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool

interface Redis : AutoCloseable, Messenger {
    val jedisPool: JedisPool
    val jedis: Jedis
}