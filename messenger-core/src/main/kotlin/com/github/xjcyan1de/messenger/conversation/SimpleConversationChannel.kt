package com.github.xjcyan1de.messenger.conversation

import com.github.xjcyan1de.messenger.ChannelAgent
import com.github.xjcyan1de.messenger.ChannelListener
import com.github.xjcyan1de.messenger.Messenger
import com.google.common.collect.Multimaps
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class SimpleConversationChannel<T : ConversationMessage, R : ConversationMessage>(
        val messenger: Messenger,
        override val name: String,
        val outgoingType: Class<T>,
        val replyType: Class<R>
) : ConversationChannel<T, R> {
    override val outgoingChannel = messenger.getChannel("$name-o", outgoingType)
    override val replyChannel = messenger.getChannel("$name-r", replyType)

    private val agents = ConcurrentHashMap.newKeySet<SimpleConversationChannelAgent<T, R>>()

    private val replyTimeoutExecutor = Executors.newSingleThreadScheduledExecutor()
    private val replyListeners = Multimaps.newSetMultimap(ConcurrentHashMap<UUID, Collection<ReplyListenerRegistration<R>>>()) { ConcurrentHashMap.newKeySet() }
    private val replyAgent = replyChannel.newAgent(object : ChannelListener<R> {
        override fun onMessage(agent: ChannelAgent<R>, message: R) {
            replyListeners.get(message.uuid).removeIf { it.onReply(message) }
        }
    })

    override fun newAgent(): ConversationChannelAgent<T, R> {
        val agent = SimpleConversationChannelAgent(this)
        agents.add(agent)
        return agent
    }

    override fun close() {
        replyAgent.close()
        replyTimeoutExecutor.shutdown()
        agents.forEach { it.close() }
    }

    override fun sendMessage(
            message: T,
            timeoutDuration: Long,
            unit: TimeUnit,
            replyListener: ConversationReplyListener<R>
    ) {
        val listenerRegistration = ReplyListenerRegistration(replyListener, timeoutDuration, unit)
        replyListeners.put(message.uuid, listenerRegistration)
        outgoingChannel.sendMessage(message)
    }

    private inner class ReplyListenerRegistration<R : ConversationMessage>(
            private val listener: ConversationReplyListener<R>,
            private val timeoutDuration: Long,
            private val unit: TimeUnit
    ) {
        private val timeoutFuture = replyTimeoutExecutor.schedule({
            timeout()
        }, timeoutDuration, unit)
        private val replies: MutableList<R> = ArrayList()
        private var active = true

        fun onReply(message: R): Boolean {
            synchronized(this) {
                if (!active) {
                    return true
                }
                replies.add(message)
                val action: ConversationReplyListener.RegistrationAction = listener.onReply(message)
                return if (action === ConversationReplyListener.RegistrationAction.STOP_LISTENING) { // unregister
                    active = false
                    timeoutFuture.cancel(false)
                    true
                } else {
                    false
                }
            }
        }

        fun timeout() {
            synchronized(this) {
                if (!active) {
                    return
                }
                listener.onTimeout(replies)
                active = false
            }
        }
    }
}