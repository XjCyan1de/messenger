package com.github.xjcyan1de.messenger.conversation

import com.github.xjcyan1de.messenger.Channel
import java.util.concurrent.TimeUnit

interface ConversationChannel<T : ConversationMessage, R : ConversationMessage> : AutoCloseable {
    val name: String
    val outgoingChannel: Channel<T>
    val replyChannel: Channel<R>

    fun newAgent(): ConversationChannelAgent<T, R>

    fun newAgent(listener: ConversationChannelListener<T, R>): ConversationChannelAgent<T, R> {
        val agent = newAgent()
        agent.addListener(listener)
        return agent
    }

    fun sendMessage(message: T, timeoutDuration: Long, unit: TimeUnit, replyListener: ConversationReplyListener<R>)
}