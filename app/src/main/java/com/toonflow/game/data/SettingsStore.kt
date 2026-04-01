package com.toonflow.game.data

import android.content.Context

class SettingsStore(context: Context) {
  private val prefs = context.getSharedPreferences("toonflow_game_settings", Context.MODE_PRIVATE)

  var baseUrl: String
    get() = prefs.getString("base_url", "http://10.0.2.2:60002") ?: "http://10.0.2.2:60002"
    set(value) {
      prefs.edit().putString("base_url", value.trim()).apply()
    }

  var token: String
    get() = prefs.getString("token", "") ?: ""
    set(value) {
      prefs.edit().putString("token", value.trim()).apply()
    }

  var autoVoiceEnabled: Boolean
    get() = prefs.getBoolean("auto_voice_enabled", true)
    set(value) {
      prefs.edit().putBoolean("auto_voice_enabled", value).apply()
    }

  fun getRuntimeChatTraceJson(): String {
    return prefs.getString("toonflow.chat", "[]") ?: "[]"
  }

  fun setRuntimeChatTraceJson(value: String) {
    prefs.edit().putString("toonflow.chat", value).apply()
  }

  fun clearRuntimeChatTrace() {
    prefs.edit().remove("toonflow.chat").apply()
  }

  fun getAvatarPath(userId: Long): String {
    if (userId <= 0) return ""
    return prefs.getString("avatar_path_user_$userId", "") ?: ""
  }

  fun setAvatarPath(userId: Long, path: String) {
    if (userId <= 0) return
    prefs.edit().putString("avatar_path_user_$userId", path.trim()).apply()
  }

  fun getAvatarBgPath(userId: Long): String {
    if (userId <= 0) return ""
    return prefs.getString("avatar_bg_path_user_$userId", "") ?: ""
  }

  fun setAvatarBgPath(userId: Long, path: String) {
    if (userId <= 0) return
    prefs.edit().putString("avatar_bg_path_user_$userId", path.trim()).apply()
  }

  fun getProfileNickname(userId: Long): String {
    if (userId <= 0) return ""
    return prefs.getString("profile_nickname_user_$userId", "") ?: ""
  }

  fun setProfileNickname(userId: Long, nickname: String) {
    if (userId <= 0) return
    prefs.edit().putString("profile_nickname_user_$userId", nickname.trim()).apply()
  }

  fun getProfileIntro(userId: Long): String {
    if (userId <= 0) return ""
    return prefs.getString("profile_intro_user_$userId", "") ?: ""
  }

  fun setProfileIntro(userId: Long, intro: String) {
    if (userId <= 0) return
    prefs.edit().putString("profile_intro_user_$userId", intro.trim()).apply()
  }

  fun getMessageReaction(sessionId: String, messageId: Long, createTime: Long): String {
    if (sessionId.isBlank() || messageId <= 0L || createTime <= 0L) return ""
    return prefs.getString(messageReactionKey(sessionId, messageId, createTime), "") ?: ""
  }

  fun setMessageReaction(sessionId: String, messageId: Long, createTime: Long, reaction: String) {
    if (sessionId.isBlank() || messageId <= 0L || createTime <= 0L) return
    val key = messageReactionKey(sessionId, messageId, createTime)
    val value = reaction.trim()
    if (value.isBlank()) {
      prefs.edit().remove(key).apply()
    } else {
      prefs.edit().putString(key, value).apply()
    }
  }

  private fun messageReactionKey(sessionId: String, messageId: Long, createTime: Long): String {
    return "message_reaction_${sessionId}_${messageId}_${createTime}"
  }
}
