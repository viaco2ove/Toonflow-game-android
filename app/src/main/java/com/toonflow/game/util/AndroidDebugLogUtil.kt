package com.toonflow.game.util

/**
 * 安卓端调试日志工具。
 */
object AndroidDebugLogUtil {
  /**
   * 输出调试日志，格式: [vue_tag] [scope] message
   */
  fun log(scope: String, message: String) {
    android.util.Log.d("vue_tag", "[vue_tag] [$scope] $message")
  }
}
