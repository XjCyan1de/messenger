package com.github.xjcyan1de.messenger


interface ChannelAgent<T> : AutoCloseable {
    val channel: Channel<T>
    val listeners: Set<ChannelListener<T>>


    val hasListeners: Boolean
        get() = listeners.isNotEmpty()


    fun addListener(listener: ChannelListener<T>): Boolean


    fun removeListener(listener: ChannelListener<T>): Boolean
}