package com.github.xjcyan1de.messenger

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

abstract class AbstractChannelAgent<T>(
        override val channel: AbstractChannel<T>
) : ChannelAgent<T> {
    private val _listeners = ConcurrentHashMap.newKeySet<ChannelListener<T>>()
    override val listeners: Set<ChannelListener<T>> get() = _listeners

    fun onIncomingMessage(message: T) {
        for (listener in listeners) {
            GlobalScope.launch {
                try {
                    listener.onMessage(this@AbstractChannelAgent, message)
                } catch (e: Exception) {
                    RuntimeException("Unable to pass decoded message to listener: $listener", e).printStackTrace()
                }
            }
        }
    }

    override fun addListener(listener: ChannelListener<T>): Boolean = try {
        _listeners.add(listener)
    } finally {
        channel.checkSubscription()
    }

    override fun removeListener(listener: ChannelListener<T>): Boolean = try {
        _listeners.remove(listener)
    } finally {
        channel.checkSubscription()
    }

    override fun close() {
        _listeners.clear()
        channel.agents.remove(this)
        channel.checkSubscription()
    }
}