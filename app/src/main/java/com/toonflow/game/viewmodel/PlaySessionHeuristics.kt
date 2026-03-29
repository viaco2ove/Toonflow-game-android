package com.toonflow.game.viewmodel

internal object PlaySessionHeuristics {
  private val placeholderNarrativeRegex = Regex("^[.。…·•・⋯\\s]{1,12}$")
  private val finishedStatuses = setOf("chapter_completed", "completed", "success", "finished")
  private val failedStatuses = setOf("failed", "dead", "lose", "loss")

  fun isPlaceholderNarrative(content: String): Boolean {
    val normalized = content.trim()
    if (normalized.isBlank()) return true
    return placeholderNarrativeRegex.matches(normalized)
  }

  fun shouldOfferInputFallback(
    canPlayerSpeak: Boolean,
    sessionStatus: String,
    latestRoleType: String,
    latestContent: String,
  ): Boolean {
    if (canPlayerSpeak) return true
    val status = sessionStatus.trim().lowercase()
    if (status.isNotBlank() && status !in setOf("active", "running", "playing")) return false
    if (latestRoleType.isBlank() && latestContent.isBlank()) return false
    if (latestRoleType.trim().lowercase() == "player") return false
    return isPlaceholderNarrative(latestContent)
  }

  fun buildTurnHint(
    canPlayerSpeak: Boolean,
    expectedSpeaker: String,
    sessionStatus: String,
    allowFallback: Boolean,
  ): String {
    val status = sessionStatus.trim().lowercase()
    if (status in finishedStatuses) {
      return "当前章节已完成，可刷新或返回历史继续查看。"
    }
    if (status in failedStatuses) {
      return "当前故事已失败，可返回历史重新开始。"
    }
    if (canPlayerSpeak) return ""
    if (allowFallback) {
      return "当前台词可能卡住了，你也可以直接输入继续。"
    }
    return "当前还没轮到用户发言，等待${expectedSpeaker.ifBlank { "当前角色" }}继续。"
  }

  fun buildInputPlaceholder(
    textMode: Boolean,
    canPlayerSpeak: Boolean,
    expectedSpeaker: String,
    sessionStatus: String,
    allowFallback: Boolean,
  ): String {
    val status = sessionStatus.trim().lowercase()
    if (canPlayerSpeak) {
      return if (textMode) "输入一句话继续故事" else "按住说话"
    }
    if (status in finishedStatuses) {
      return "当前章节已完成"
    }
    if (status in failedStatuses) {
      return "当前故事已失败"
    }
    if (allowFallback) {
      return if (textMode) "台词可能卡住了，直接输入继续" else "台词可能卡住了，点按继续说话"
    }
    return "当前轮到${expectedSpeaker.ifBlank { "当前角色" }}发言"
  }
}
