package com.github.xjcyan1de.messenger

import com.github.xjcyan1de.messenger.conversation.ConversationMessage
import com.github.xjcyan1de.messenger.conversation.SimpleConversationChannel

interface Messenger {
    fun <T> getChannel(name: String, type: Class<T>): Channel<T>

    fun <T : ConversationMessage, R : ConversationMessage> getConversationChannel(
            name: String,
            clazz: Class<T>,
            replyClazz: Class<R>
    ) = SimpleConversationChannel(this, name, clazz, replyClazz)
}

