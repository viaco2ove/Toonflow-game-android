package com.toonflow.game.viewmodel

import com.toonflow.game.data.MessageItem

internal object DebugSessionHeuristics {
  fun stripKnownHistory(historyMessages: List<MessageItem>, incomingMessages: List<MessageItem>): List<MessageItem> {
    if (historyMessages.isEmpty() || incomingMessages.isEmpty()) return incomingMessages
    if (incomingMessages.size < historyMessages.size) return incomingMessages
    val historySignatures = historyMessages.map(::messageSignature)
    val incomingPrefix = incomingMessages.take(historyMessages.size).map(::messageSignature)
    return if (incomingPrefix == historySignatures) {
      incomingMessages.drop(historyMessages.size)
    } else {
      incomingMessages
    }
  }

  private fun messageSignature(message: MessageItem): String {
    return buildString {
      append(message.roleType.trim())
      append('|').append(message.role.trim())
      append('|').append(message.eventType.trim())
      append('|').append(message.content.trim())
    }
  }
}
