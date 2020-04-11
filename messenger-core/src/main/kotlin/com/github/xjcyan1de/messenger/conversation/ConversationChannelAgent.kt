package com.github.xjcyan1de.messenger.conversation


interface ConversationChannelAgent<T : ConversationMessage, R : ConversationMessage> : AutoCloseable {
    val channel: ConversationChannel<T, R>
    val listeners: Set<ConversationChannelListener<T, R>>

    val hasListeners get() = listeners.isNotEmpty()

    fun addListener(listener: ConversationChannelListener<T, R>): Boolean

    fun removeListener(listener: ConversationChannelListener<T, R>): Boolean
}