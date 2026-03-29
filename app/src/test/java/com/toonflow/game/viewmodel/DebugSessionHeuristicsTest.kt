package com.toonflow.game.viewmodel

import com.toonflow.game.data.MessageItem
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugSessionHeuristicsTest {
  @Test
  fun stripKnownHistory_dropsMatchingPrefixOnly() {
    val history = listOf(
      MessageItem(
        id = 1L,
        role = "旁白",
        roleType = "narrator",
        eventType = "on_orchestrated_reply",
        content = "第一句",
        createTime = 1L,
      ),
    )
    val incoming = listOf(
      history.first(),
      MessageItem(
        id = 2L,
        role = "旁白",
        roleType = "narrator",
        eventType = "on_orchestrated_reply",
        content = "第二句",
        createTime = 2L,
      ),
    )

    val result = DebugSessionHeuristics.stripKnownHistory(history, incoming)

    assertEquals(listOf(incoming.last()), result)
  }

  @Test
  fun stripKnownHistory_keepsMessagesWhenPrefixDoesNotMatch() {
    val history = listOf(
      MessageItem(
        id = 1L,
        role = "旁白",
        roleType = "narrator",
        eventType = "on_orchestrated_reply",
        content = "第一句",
        createTime = 1L,
      ),
    )
    val incoming = listOf(
      MessageItem(
        id = 2L,
        role = "旁白",
        roleType = "narrator",
        eventType = "on_orchestrated_reply",
        content = "另一句",
        createTime = 2L,
      ),
    )

    val result = DebugSessionHeuristics.stripKnownHistory(history, incoming)

    assertEquals(incoming, result)
  }
}
