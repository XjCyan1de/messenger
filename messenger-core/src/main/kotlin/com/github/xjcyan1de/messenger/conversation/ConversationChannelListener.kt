package com.github.xjcyan1de.messenger.conversation

@FunctionalInterface
interface ConversationChannelListener<T : ConversationMessage, R : ConversationMessage> {
    fun onMessage(agent: ConversationChannelAgent<T, R>, message: T): ConversationReply<R>
}