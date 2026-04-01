package com.toonflow.game.util

import android.util.Log

object VueTagLogger {
  private const val TAG = "vue_tag"
  private const val PREFIX = "[vue_tag]"
  private const val DEFAULT_LIMIT = 1600
  private val sensitiveFieldPattern = Regex(
    """("(?:base64Data|base64|apiKey|password|token|authorization|voiceReferenceAudioPath|referenceAudioPath|audioBase64)"\s*:\s*")([^"]*)(")""",
    RegexOption.IGNORE_CASE,
  )

  fun info(scope: String, message: String) {
    Log.d(TAG, "$PREFIX [$scope] $message")
  }

  fun error(scope: String, message: String, throwable: Throwable? = null) {
    if (throwable == null) {
      Log.e(TAG, "$PREFIX [$scope] $message")
    } else {
      Log.e(TAG, "$PREFIX [$scope] $message", throwable)
    }
  }

  fun sanitize(raw: String?, limit: Int = DEFAULT_LIMIT): String {
    val compact = raw.orEmpty()
      .replace("\r", " ")
      .replace("\n", " ")
      .replace(Regex("\\s+"), " ")
      .trim()
    if (compact.isBlank()) return "-"
    val redacted = sensitiveFieldPattern.replace(compact) { match ->
      "${match.groupValues[1]}<redacted>${match.groupValues[3]}"
    }
    return if (redacted.length <= limit) redacted else "${redacted.take(limit)}...(truncated)"
  }

  fun throwableMessage(error: Throwable): String {
    val type = error::class.java.simpleName.ifBlank { "Throwable" }
    val detail = error.message?.trim().orEmpty()
    return if (detail.isBlank()) type else "$type: $detail"
  }
}
