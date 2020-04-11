package com.github.xjcyan1de.messenger.conversation

import com.github.xjcyan1de.messenger.ChannelAgent
import com.github.xjcyan1de.messenger.ChannelListener
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class SimpleConversationChannelAgent<T : ConversationMessage, R : ConversationMessage>(
        channel: ConversationChannel<T, R>
) : ConversationChannelAgent<T, R> {
    override val channel: ConversationChannel<T, R> = channel
        get() {
            delegateAgent.channel // убеждаемся что агент всё еще жив
            return field
        }
    val delegateAgent = channel.outgoingChannel.newAgent()
    override val listeners: Set<ConversationChannelListener<T, R>>
        get() = delegateAgent.listeners.map {
            @Suppress("RemoveRedundantQualifierName", "UNCHECKED_CAST")
            (it as SimpleConversationChannelAgent<T, R>.WrappedListener).delegate
        }.toSet()

    override val hasListeners: Boolean
        get() = delegateAgent.hasListeners

    override fun addListener(listener: ConversationChannelListener<T, R>): Boolean =
            delegateAgent.addListener(WrappedListener(listener))

    @Suppress("RemoveRedundantQualifierName", "UNCHECKED_CAST")
    override fun removeListener(listener: ConversationChannelListener<T, R>): Boolean {
        val listeners: Set<ChannelListener<T>> = delegateAgent.listeners
        for (other in listeners) {
            val wrapped: WrappedListener = other as SimpleConversationChannelAgent<T, R>.WrappedListener
            if (wrapped.delegate === listener) {
                return delegateAgent.removeListener(other)
            }
        }
        return false
    }

    override fun close() = delegateAgent.close()

    inner class WrappedListener(
            val delegate: ConversationChannelListener<T, R>
    ) : ChannelListener<T> {
        override fun onMessage(agent: ChannelAgent<T>, message: T) {
            GlobalScope.launch {
                val reply = delegate.onMessage(this@SimpleConversationChannelAgent, message).reply()
                if (reply != null) {
                    this@SimpleConversationChannelAgent.channel.replyChannel.sendMessage(reply)
                }
            }
        }
    }
}