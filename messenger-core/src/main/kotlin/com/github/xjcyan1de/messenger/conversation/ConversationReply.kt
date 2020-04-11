package com.github.xjcyan1de.messenger.conversation

class ConversationReply<R : ConversationMessage>(
        val reply: () -> R? = { null }
) {

    override fun toString(): String {
        return "ConversationReply(reply=$reply)"
    }

    companion object {
        private val NO_REPLY = ConversationReply<ConversationMessage>()

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <R : ConversationMessage> noReply(): ConversationReply<R> =
                NO_REPLY as ConversationReply<R>
    }
}