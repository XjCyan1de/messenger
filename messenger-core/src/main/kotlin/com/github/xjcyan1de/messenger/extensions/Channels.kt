package com.github.xjcyan1de.messenger.extensions

import com.github.xjcyan1de.messenger.Channel
import com.github.xjcyan1de.messenger.ChannelAgent
import com.github.xjcyan1de.messenger.ChannelListener

inline fun <T> Channel<T>.newAgent(crossinline block: (ChannelAgent<T>, T) -> Unit) =
        newAgent(object : ChannelListener<T> {
            override fun onMessage(agent: ChannelAgent<T>, message: T) {
                block(agent, message)
            }
        })