package com.github.xjcyan1de.messenger.conversation

import java.util.function.Function

interface ConversationReplyListener<R : ConversationMessage> {
    fun onReply(reply: R): RegistrationAction

    fun onTimeout(replies: List<R>)

    enum class RegistrationAction {
        CONTINUE_LISTENING,
        STOP_LISTENING
    }

    companion object {
        fun <R : ConversationMessage> of(onReply: Function<in R, RegistrationAction>): ConversationReplyListener<R> {
            return object : ConversationReplyListener<R> {
                override fun onReply(reply: R): RegistrationAction = onReply.apply(reply)

                override fun onTimeout(replies: List<R>) {}
            }
        }
    }
}
