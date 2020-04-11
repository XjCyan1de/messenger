package com.github.xjcyan1de.messenger

import com.google.common.cache.CacheBuilder
import com.google.common.cache.CacheLoader
import com.google.common.cache.LoadingCache
import com.google.common.collect.Maps

abstract class AbstractMessenger : Messenger {
    private val channels: LoadingCache<Map.Entry<String, Class<Any>>, AbstractChannel<Any>> = CacheBuilder.newBuilder().build(ChannelLoader())

    abstract fun outgoingMessage(channel: String, message: ByteArray)
    abstract fun notifySubscribe(channel: String)
    abstract fun notifyUnsubscribe(channel: String)

    fun registerIncomingMessage(channel: String, message: ByteArray) {
        for ((key, value) in channels.asMap().entries) {
            if (key.key == channel) {
                value.onIncomingMessage(message)
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getChannel(name: String, type: Class<T>): Channel<T> {
        require(name.trim().isNotEmpty()) { "name cannot be empty" }
        return channels.getUnchecked(Maps.immutableEntry(name, type as Class<Any>)) as Channel<T>
    }

    private inner class ChannelLoader<T> : CacheLoader<Map.Entry<String, Class<T>>, AbstractChannel<T>>() {
        override fun load(spec: Map.Entry<String, Class<T>>): AbstractChannel<T> =
                object : AbstractChannel<T>(this@AbstractMessenger, spec.key, spec.value) {}
    }
}