package com.github.xjcyan1de.messenger.conversation

import java.util.*

/**
 * Представляет собой сообщение отправленное через [ConversationChannel]
 *
 * *ID должен быть серелизирован*
 */
interface ConversationMessage {
    val uuid: UUID
}