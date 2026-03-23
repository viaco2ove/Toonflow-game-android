package com.toonflow.game.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.toonflow.game.api.ApiClient

class GameRepository(private val settingsStore: SettingsStore) {
  private val gson = Gson()

  private fun api() = ApiClient.create(settingsStore)

  suspend fun login(username: String, password: String): JsonObject {
    val payload = JsonObject().apply {
      addProperty("username", username)
      addProperty("password", password)
    }
    return api().login(payload).data
  }

  suspend fun getProjects(): List<ProjectItem> {
    return api().getProjects().data
  }

  suspend fun getUser(): JsonObject {
    return api().getUser().data
  }

  suspend fun getWorld(projectId: Long, autoCreate: Boolean): WorldItem? {
    val payload = JsonObject().apply {
      addProperty("projectId", projectId)
      addProperty("autoCreate", autoCreate)
    }
    return runCatching { api().getWorld(payload).data }.getOrNull()
  }

  suspend fun getWorldById(worldId: Long): WorldItem? {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
    }
    return runCatching { api().getWorld(payload).data }.getOrNull()
  }

  suspend fun listWorlds(projectId: Long? = null): List<WorldItem> {
    val payload = JsonObject().apply {
      if (projectId != null && projectId > 0L) addProperty("projectId", projectId)
    }
    return runCatching { api().listWorlds(payload).data }.getOrElse { emptyList() }
  }

  suspend fun saveWorld(payload: JsonObject): WorldItem {
    return api().saveWorld(payload).data
  }

  suspend fun generateImage(
    projectId: Long,
    type: String,
    prompt: String,
    name: String = "",
    base64: String? = null,
    base64List: List<String> = emptyList(),
    aspectRatio: String? = null,
    size: String = "2K",
  ): GeneratedImageResult {
    val payload = JsonObject().apply {
      addProperty("projectId", projectId)
      addProperty("type", type)
      addProperty("prompt", prompt)
      if (name.isNotBlank()) addProperty("name", name)
      if (!base64.isNullOrBlank()) addProperty("base64", base64)
      if (base64List.isNotEmpty()) {
        add("base64List", JsonArray().apply {
          base64List.filter { it.isNotBlank() }.forEach { add(it) }
        })
      }
      if (!aspectRatio.isNullOrBlank()) addProperty("aspectRatio", aspectRatio)
      addProperty("size", size)
    }
    return api().generateImage(payload).data
  }

  suspend fun getChapter(worldId: Long): List<ChapterItem> {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
    }
    return api().getChapter(payload).data
  }

  suspend fun saveChapter(payload: JsonObject): ChapterItem {
    return api().saveChapter(payload).data
  }

  suspend fun startSession(worldId: Long, projectId: Long, chapterId: Long?): String {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
      addProperty("projectId", projectId)
      if (chapterId != null) addProperty("chapterId", chapterId)
    }
    return api().startSession(payload).data.get("sessionId")?.asString ?: ""
  }

  suspend fun listSession(projectId: Long? = null): List<SessionItem> {
    val payload = JsonObject().apply {
      if (projectId != null && projectId > 0L) {
        addProperty("projectId", projectId)
      }
      addProperty("limit", 60)
    }
    return runCatching { api().listSession(payload).data }.getOrElse { emptyList() }
  }

  suspend fun getSession(sessionId: String): SessionDetail {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
      addProperty("messageLimit", 120)
    }
    return api().getSession(payload).data
  }

  suspend fun getMessages(sessionId: String): List<MessageItem> {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
      addProperty("limit", 120)
    }
    return api().getMessage(payload).data
  }

  suspend fun addPlayerMessage(sessionId: String, role: String, content: String) {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
      addProperty("roleType", "player")
      addProperty("role", role)
      addProperty("content", content)
      addProperty("eventType", "on_message")
    }
    api().addMessage(payload)
  }

  suspend fun getVoiceModels(): List<VoiceModelConfig> {
    return runCatching { api().getVoiceModelList().data }.getOrElse { emptyList() }
  }

  suspend fun getVoicePresets(configId: Long?): List<VoicePresetItem> {
    val payload = JsonObject().apply {
      if (configId != null && configId > 0L) addProperty("configId", configId)
    }
    val data = api().getVoices(payload).data
    val rawList = when {
      data.isJsonArray -> data.asJsonArray
      data.isJsonObject && data.asJsonObject.get("voices")?.isJsonArray == true -> data.asJsonObject.getAsJsonArray("voices")
      else -> JsonArray()
    }
    return rawList.mapNotNull { item ->
      if (item == null || item.isJsonNull) return@mapNotNull null
      if (item.isJsonPrimitive) {
        val voiceId = item.asString.trim()
        if (voiceId.isBlank()) return@mapNotNull null
        return@mapNotNull VoicePresetItem(voiceId = voiceId, name = voiceId)
      }
      if (!item.isJsonObject) return@mapNotNull null
      val obj = item.asJsonObject
      val voiceId = listOf("voice_id", "voiceId", "id", "key").firstNotNullOfOrNull { key ->
        obj.get(key)?.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotBlank() }
      } ?: return@mapNotNull null
      val name = listOf("name", "label", "voice_name").firstNotNullOfOrNull { key ->
        obj.get(key)?.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotBlank() }
      } ?: voiceId
      VoicePresetItem(voiceId = voiceId, name = name)
    }
  }

  suspend fun uploadVoiceAudio(projectId: Long?, base64Data: String, fileName: String): UploadedVoiceAudioResult {
    val payload = JsonObject().apply {
      addProperty("base64Data", base64Data)
      addProperty("fileName", fileName)
      if (projectId != null && projectId > 0L) addProperty("projectId", projectId)
    }
    return api().uploadVoiceAudio(payload).data
  }

  suspend fun previewVoice(
    configId: Long?,
    text: String,
    mode: String = "text",
    voiceId: String = "",
    referenceAudioPath: String = "",
    referenceText: String = "",
    promptText: String = "",
    mixVoices: List<VoiceMixItem> = emptyList(),
  ): String {
    val payload = JsonObject().apply {
      if (configId != null && configId > 0L) addProperty("configId", configId)
      addProperty("text", text)
      addProperty("mode", mode)
      when (mode) {
        "text" -> if (voiceId.isNotBlank()) {
          addProperty("voiceId", voiceId)
        }
        "clone" -> {
          if (referenceAudioPath.isNotBlank()) addProperty("referenceAudioPath", referenceAudioPath)
          if (referenceText.isNotBlank()) addProperty("referenceText", referenceText)
        }
        "mix" -> {
          add("mixVoices", JsonArray().apply {
            mixVoices
              .filter { it.voiceId.isNotBlank() }
              .forEach { item ->
                add(JsonObject().apply {
                  addProperty("voiceId", item.voiceId)
                  addProperty("weight", item.weight)
                })
              }
          })
        }
        "prompt_voice" -> if (promptText.isNotBlank()) {
          addProperty("promptText", promptText)
        }
      }
    }
    val data = api().previewVoice(payload).data
    return data.get("audioUrl")?.asString?.trim().orEmpty()
  }

  suspend fun syncMiniGame(sessionId: String, miniGameJson: JsonObject) {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
      addProperty("roleType", "system")
      addProperty("role", "系统")
      addProperty("content", "同步小游戏状态")
      addProperty("eventType", "on_mini_game_sync")
      add("meta", JsonObject().apply {
        add("miniGame", miniGameJson)
      })
      add("attrChanges", JsonArray().apply {
        add(JsonObject().apply {
          addProperty("entityType", "state")
          addProperty("field", "state.miniGame")
          add("value", miniGameJson)
          addProperty("source", "mini_game_sync")
        })
      })
    }
    api().addMessage(payload)
  }

  fun toJson(value: Any): JsonObject {
    val element = gson.toJsonTree(value)
    return if (element.isJsonObject) element.asJsonObject else JsonObject()
  }

  fun toJsonElement(value: Any): JsonElement {
    return gson.toJsonTree(value)
  }

  fun toJsonArray(values: List<String>): JsonArray {
    return JsonArray().apply {
      values.forEach { add(it) }
    }
  }
}
