package com.github.xjcyan1de.messenger

import com.github.xjcyan1de.messenger.codec.Codec


interface Channel<T> {
    val name: String
    var codec: Codec<T>


    fun newAgent(): ChannelAgent<T>


    fun newAgent(listener: ChannelListener<T>): ChannelAgent<T> {
        val agent = newAgent()
        agent.addListener(listener)
        return agent
    }

    fun sendMessage(message: T)
}

