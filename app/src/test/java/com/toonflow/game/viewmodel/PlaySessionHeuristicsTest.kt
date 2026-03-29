package com.toonflow.game.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaySessionHeuristicsTest {
  @Test
  fun `placeholder narrative should be recognized`() {
    assertTrue(PlaySessionHeuristics.isPlaceholderNarrative("..."))
    assertTrue(PlaySessionHeuristics.isPlaceholderNarrative("……"))
    assertTrue(PlaySessionHeuristics.isPlaceholderNarrative(" · · "))
    assertFalse(PlaySessionHeuristics.isPlaceholderNarrative("旁白开口了"))
  }

  @Test
  fun `stuck npc placeholder should allow fallback input`() {
    assertTrue(
      PlaySessionHeuristics.shouldOfferInputFallback(
        canPlayerSpeak = false,
        sessionStatus = "active",
        latestRoleType = "narrator",
        latestContent = "...",
      ),
    )
  }

  @Test
  fun `finished session should not allow fallback input`() {
    assertFalse(
      PlaySessionHeuristics.shouldOfferInputFallback(
        canPlayerSpeak = false,
        sessionStatus = "chapter_completed",
        latestRoleType = "narrator",
        latestContent = "...",
      ),
    )
  }

  @Test
  fun `turn hint should prefer finished status message`() {
    assertEquals(
      "当前章节已完成，可刷新或返回历史继续查看。",
      PlaySessionHeuristics.buildTurnHint(
        canPlayerSpeak = false,
        expectedSpeaker = "旁白",
        sessionStatus = "chapter_completed",
        allowFallback = true,
      ),
    )
  }

  @Test
  fun `fallback placeholder should guide direct input`() {
    assertEquals(
      "台词可能卡住了，直接输入继续",
      PlaySessionHeuristics.buildInputPlaceholder(
        textMode = true,
        canPlayerSpeak = false,
        expectedSpeaker = "旁白",
        sessionStatus = "active",
        allowFallback = true,
      ),
    )
  }
}
