package com.toonflow.game.util

import com.toonflow.game.data.SettingsStore

/**
 * 安卓端调试日志工具。
 *
 * 用途：
 * - 统一控制故事游玩 / 调试 / 小游戏相关日志的开关；
 * - 只在显式开启 `debug=true` 时输出，避免正式游玩污染 Logcat；
 * - 复用现有 `VueTagLogger` 的输出格式，保持浏览器端与安卓端标签一致。
 */
object AndroidDebugLogUtil {
  @Volatile
  private var enabled: Boolean = false

  /**
   * 根据设置仓库同步当前调试日志开关。
   */
  fun sync(settingsStore: SettingsStore) {
    enabled = settingsStore.isDebugLoggingEnabled()
  }

  /**
   * 判断当前安卓端调试日志是否开启。
   */
  fun isEnabled(): Boolean = enabled

  /**
   * 输出受调试开关控制的普通日志。
   */
  fun log(scope: String, message: String) {
    if (!enabled) {
      return
    }
    VueTagLogger.info(scope, message)
  }
}
