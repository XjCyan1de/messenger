package com.github.xjcyan1de.messenger


@FunctionalInterface
interface ChannelListener<T> {

    fun onMessage(agent: ChannelAgent<T>, message: T)
}