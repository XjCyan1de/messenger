package com.github.xjcyan1de.messenger.extensions

import com.github.xjcyan1de.messenger.Messenger
import com.github.xjcyan1de.messenger.conversation.ConversationMessage

inline fun <reified T> Messenger.getChannel(name: String) = getChannel(name, T::class.java)
inline fun <reified T : ConversationMessage, reified R : ConversationMessage> Messenger.getConversationChannel(name: String) =
        getConversationChannel(name, T::class.java, R::class.java)