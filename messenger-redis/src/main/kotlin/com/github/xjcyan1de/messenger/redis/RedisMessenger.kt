package com.github.xjcyan1de.messenger.redis

import com.github.xjcyan1de.messenger.AbstractMessenger
import com.github.xjcyan1de.messenger.Channel
import kotlinx.coroutines.*
import redis.clients.jedis.BinaryJedisPubSub
import redis.clients.jedis.Jedis
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Logger

fun RedisMessenger(builder: RedisMessenger.Builder.() -> Unit) = RedisMessenger.builder().apply(builder).build()

class RedisMessenger
private constructor(
        private var hostname: String,
        private var port: Int,
        private var password: ByteArray
) : Redis, CoroutineScope by GlobalScope {
    constructor(builder: Builder) : this(builder.hostname, builder.port, builder.password.toByteArray())

    private val log = Logger.getLogger("Redis")
    override lateinit var jedisPool: JedisPool
    override val jedis: Jedis
        get() = jedisPool.resource
    private val messenger = object : AbstractMessenger() {
        override fun outgoingMessage(channel: String, message: ByteArray) {
            jedis.use {
                it.publish(channel.toByteArray(), message)
            }
        }

        override fun notifySubscribe(channel: String) {
            channels.add(channel)
            listener!!.subscribe(channel.toByteArray())
        }

        override fun notifyUnsubscribe(channel: String) {
            channels.remove(channel)
            listener!!.unsubscribe(channel.toByteArray())
        }
    }
    private val channels = HashSet<String>()
    private var listener: PubSubListener? = null
    private var listenerFixerJob: Job? = null
    private var subscribeJob: Job? = null

    fun connect() = apply {
        jedisPool = JedisPoolConfig().let { config ->
            config.maxTotal = 16
            if (password.isEmpty()) {
                JedisPool(config, hostname, port)
            } else {
                JedisPool(config, hostname, port, 2000, password.toString(Charsets.UTF_8))
            }
        }
        jedis.use {
            it.ping()
        }

        listenerFixerJob = launch {
            var broken = false
            while (true) {
                if (broken) {
                    log.info("[messenger-redis] Retrying subscription...")
                    broken = false
                }
                jedis.use {
                    try {
                        listener = PubSubListener()
                        it.psubscribe(listener, "messenger-redis-dummy".toByteArray())
                    } catch (e: Exception) {
                        RuntimeException("Error subscribing to listener", e).printStackTrace()
                        try {
                            listener?.unsubscribe()
                        } catch (ignored: Exception) {
                        }
                        listener = null
                        broken = true
                    }
                }
                if (broken) {
                    delay(1000)
                }
            }
        }

        subscribeJob = launch {
            while (true) {
                listener?.let {
                    if (it.isSubscribed) {
                        for (channel in channels) {
                            it.subscribe(channel.toByteArray())
                        }
                    }
                }
                delay(1000)
            }
        }
    }

    override fun close() {
        listener?.let {
            it.unsubscribe()
            listener = null
        }
        listenerFixerJob?.let {
            it.cancel()
            listenerFixerJob = null
        }
        subscribeJob?.let {
            it.cancel()
            subscribeJob = null
        }
        jedisPool.close()
    }

    override fun <T> getChannel(name: String, type: Class<T>): Channel<T> = messenger.getChannel(name, type)

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }

    inner class PubSubListener : BinaryJedisPubSub() {
        private val subscribed: MutableSet<String> = ConcurrentHashMap.newKeySet()

        override fun subscribe(vararg channels: ByteArray) {
            for (channel in channels) {
                val channelName = String(channel, StandardCharsets.UTF_8).intern()
                if (subscribed.add(channelName)) {
                    super.subscribe(channel)
                }
            }
        }

        override fun onSubscribe(channel: ByteArray, subscribedChannels: Int) {
            log.info("Subscribed to channel: ${channel.toString(Charsets.UTF_8)}")
        }

        override fun onUnsubscribe(channel: ByteArray?, subscribedChannels: Int) {
            if (channel != null) {
                val channelName = channel.toString(Charsets.UTF_8)
                log.info("Unsubscribed from channel: $channelName")
                subscribed.remove(channelName)
            }
        }

        override fun onMessage(channel: ByteArray, message: ByteArray) {
            val channelName = channel.toString(Charsets.UTF_8)
            try {
                messenger.registerIncomingMessage(channelName, message)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    class Builder {
        var hostname: String = "localhost"
        var port: Int = 6379
        var password: String = "password"

        fun hostname(hostname: String) = apply {
            this.hostname = hostname
        }

        fun port(port: Int) = apply {
            this.port = port
        }

        fun password(password: String) = apply {
            this.password = password
        }

        fun build(): RedisMessenger = RedisMessenger(this)
    }
}

