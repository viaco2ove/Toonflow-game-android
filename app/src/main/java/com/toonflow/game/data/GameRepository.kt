package com.toonflow.game.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.toonflow.game.api.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

class GameRepository(private val settingsStore: SettingsStore) {
  private val gson = Gson()
  private val roleAvatarTaskPollIntervalMs = 1000L
  private val roleAvatarTaskTimeoutMs = 180000L
  private val debugStreamIdleTimeoutMs = 15000L
  private val debugStreamWatchdogPollMs = 500L

  private fun api() = ApiClient.create(settingsStore)

  private fun resolveRoleAvatarResult(task: RoleAvatarTaskResult): SeparatedRoleImageResult {
    val foregroundPath = task.foregroundFilePath.ifBlank { task.foregroundPath }
    val backgroundPath = task.backgroundFilePath.ifBlank { task.backgroundPath }
    if (foregroundPath.isBlank() || backgroundPath.isBlank()) {
      error("图像模型分离失败，未返回主体或背景图片")
    }
    return SeparatedRoleImageResult(
      foregroundPath = task.foregroundPath,
      foregroundFilePath = task.foregroundFilePath,
      backgroundPath = task.backgroundPath,
      backgroundFilePath = task.backgroundFilePath,
    )
  }

  suspend fun login(username: String, password: String): JsonObject {
    val payload = JsonObject().apply {
      addProperty("username", username)
      addProperty("password", password)
    }
    return api().login(payload).data
  }

  suspend fun register(username: String, password: String): JsonObject {
    val payload = JsonObject().apply {
      addProperty("username", username)
      addProperty("password", password)
    }
    return api().register(payload).data
  }

  suspend fun getProjects(): List<ProjectItem> {
    return api().getProjects().data
  }

  suspend fun getUser(): JsonObject {
    return api().getUser().data
  }

  suspend fun saveUser(
    name: String? = null,
    password: String? = null,
    avatarPath: String? = null,
    avatarBgPath: String? = null,
  ) {
    val payload = JsonObject().apply {
      if (!name.isNullOrBlank()) addProperty("name", name)
      if (!password.isNullOrBlank()) addProperty("password", password)
      if (avatarPath != null) addProperty("avatarPath", avatarPath)
      if (avatarBgPath != null) addProperty("avatarBgPath", avatarBgPath)
    }
    if (payload.size() == 0) return
    api().saveUser(payload)
  }

  suspend fun changePassword(oldPassword: String, newPassword: String) {
    val payload = JsonObject().apply {
      addProperty("oldPassword", oldPassword)
      addProperty("newPassword", newPassword)
    }
    api().changePassword(payload)
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

  suspend fun listWorlds(projectId: Long? = null, includePublicPublished: Boolean = false): List<WorldItem> {
    val payload = JsonObject().apply {
      if (projectId != null && projectId > 0L) addProperty("projectId", projectId)
      if (includePublicPublished) addProperty("includePublicPublished", true)
    }
    return runCatching { api().listWorlds(payload).data }.getOrElse { emptyList() }
  }

  suspend fun saveWorld(payload: JsonObject): WorldItem {
    return api().saveWorld(payload).data
  }

  suspend fun deleteWorld(worldId: Long) {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
    }
    api().deleteWorld(payload)
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

  suspend fun uploadImage(
    projectId: Long? = null,
    type: String,
    base64Data: String,
    fileName: String = "image.png",
  ): GeneratedImageResult {
    val payload = JsonObject().apply {
      if (projectId != null && projectId > 0L) addProperty("projectId", projectId)
      addProperty("type", type)
      addProperty("base64Data", base64Data)
      if (fileName.isNotBlank()) addProperty("fileName", fileName)
    }
    return api().uploadImage(payload).data
  }

  suspend fun convertAvatarVideoToGif(
    projectId: Long? = null,
    base64Data: String,
    fileName: String = "avatar.mp4",
  ): SeparatedRoleImageResult {
    val payload = JsonObject().apply {
      if (projectId != null && projectId > 0L) addProperty("projectId", projectId)
      addProperty("base64Data", base64Data)
      if (fileName.isNotBlank()) addProperty("fileName", fileName)
    }
    return api().convertAvatarVideoToGif(payload).data
  }

  suspend fun separateRoleAvatar(
    projectId: Long? = null,
    base64Data: String,
    fileName: String = "avatar.png",
    name: String = "角色",
  ): SeparatedRoleImageResult {
    val payload = JsonObject().apply {
      if (projectId != null && projectId > 0L) addProperty("projectId", projectId)
      addProperty("base64Data", base64Data)
      if (fileName.isNotBlank()) addProperty("fileName", fileName)
      if (name.isNotBlank()) addProperty("name", name)
      addProperty("asyncTask", true)
    }
    val task = api().separateRoleAvatar(payload).data
    val taskId = task.taskId
    if (taskId <= 0L) {
      error("头像分离任务创建失败")
    }
    val startedAt = System.currentTimeMillis()
    while (System.currentTimeMillis() - startedAt < roleAvatarTaskTimeoutMs) {
      val status = api().separateRoleAvatarStatus(JsonObject().apply {
        addProperty("taskId", taskId)
      }).data
      when (status.status.trim().lowercase()) {
        "success" -> return resolveRoleAvatarResult(status)
        "failed" -> error(status.errorMessage.ifBlank { status.message.ifBlank { "头像分离失败" } })
      }
      delay(roleAvatarTaskPollIntervalMs)
    }
    error("头像分离处理超时，请稍后重试")
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

  suspend fun listSession(projectId: Long? = null, worldId: Long? = null): List<SessionItem> {
    val payload = JsonObject().apply {
      if (projectId != null && projectId > 0L) {
        addProperty("projectId", projectId)
      }
      if (worldId != null && worldId > 0L) {
        addProperty("worldId", worldId)
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

  suspend fun deleteSession(sessionId: String) {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
    }
    api().deleteSession(payload)
  }

  suspend fun deleteMessage(sessionId: String, messageId: Long) {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
      addProperty("messageId", messageId)
    }
    api().deleteMessage(payload)
  }

  suspend fun getMessages(sessionId: String): List<MessageItem> {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
      addProperty("limit", 120)
    }
    return api().getMessage(payload).data
  }

  suspend fun addPlayerMessage(sessionId: String, role: String, content: String): SessionNarrativeResult {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
      addProperty("roleType", "player")
      addProperty("role", role)
      addProperty("content", content)
      addProperty("eventType", "on_message")
    }
    return api().addMessage(payload).data
  }

  suspend fun continueSession(sessionId: String): SessionNarrativeResult {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
    }
    return api().continueSession(payload).data
  }

  suspend fun debugStep(
    worldId: Long,
    chapterId: Long?,
    state: JsonElement?,
    messages: List<MessageItem>,
    playerContent: String? = null,
  ): DebugStepResult {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
      if (chapterId != null && chapterId > 0L) addProperty("chapterId", chapterId)
      if (playerContent != null) addProperty("playerContent", playerContent)
      if (state != null && !state.isJsonNull) {
        add("state", state)
      }
      add("messages", gson.toJsonTree(messages))
    }
    return api().debugStep(payload).data
  }

  suspend fun debugOrchestration(
    worldId: Long,
    chapterId: Long?,
    state: JsonElement?,
    messages: List<MessageItem>,
    playerContent: String? = null,
  ): DebugOrchestrationResult {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
      if (chapterId != null && chapterId > 0L) addProperty("chapterId", chapterId)
      if (playerContent != null) addProperty("playerContent", playerContent)
      if (state != null && !state.isJsonNull) {
        add("state", state)
      }
      add("messages", gson.toJsonTree(messages))
    }
    return api().orchestrateDebug(payload).data
  }

  suspend fun streamDebugLines(
    worldId: Long,
    chapterId: Long?,
    state: JsonElement?,
    messages: List<MessageItem>,
    playerContent: String? = null,
    plan: DebugNarrativePlan,
    onEvent: suspend (JsonObject) -> Unit,
  ) {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
      if (chapterId != null && chapterId > 0L) addProperty("chapterId", chapterId)
      if (playerContent != null) addProperty("playerContent", playerContent)
      if (state != null && !state.isJsonNull) {
        add("state", state)
      }
      add("messages", gson.toJsonTree(messages))
      add("plan", gson.toJsonTree(plan))
    }
    val baseUrl = settingsStore.baseUrl.trim().removeSuffix("/")
    val request = Request.Builder()
      .url("$baseUrl/game/streamlines")
      .post(gson.toJson(payload).toRequestBody("application/json; charset=utf-8".toMediaType()))
      .apply {
        val token = settingsStore.token.trim()
        if (token.isNotEmpty()) {
          header("Authorization", token)
        }
    }
      .build()
    val logger = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    }
    val client = OkHttpClient.Builder()
      .addInterceptor(logger)
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(0, TimeUnit.SECONDS)
      .writeTimeout(30, TimeUnit.SECONDS)
      .build()
    coroutineScope {
      val call = client.newCall(request)
      val timedOut = AtomicBoolean(false)
      val lastEventAt = AtomicLong(System.currentTimeMillis())
      val watchdog = launch(Dispatchers.IO) {
        while (isActive) {
          delay(debugStreamWatchdogPollMs)
          if (System.currentTimeMillis() - lastEventAt.get() < debugStreamIdleTimeoutMs) continue
          timedOut.set(true)
          call.cancel()
          break
        }
      }
      try {
        withContext(Dispatchers.IO) {
          call.execute().use { response ->
            if (!response.isSuccessful) {
              error("HTTP ${response.code}")
            }
            val body = response.body ?: error("未返回流式正文")
            val source = body.source()
            source.timeout().timeout(debugStreamIdleTimeoutMs, TimeUnit.MILLISECONDS)
            while (true) {
              val raw = source.readUtf8Line() ?: break
              val line = raw.trim()
              if (line.isBlank()) continue
              lastEventAt.set(System.currentTimeMillis())
              val event = gson.fromJson(line, JsonObject::class.java)
              withContext(Dispatchers.Main.immediate) {
                onEvent(event)
              }
            }
          }
        }
      } catch (err: Throwable) {
        if (timedOut.get() || err is InterruptedIOException || err is SocketTimeoutException) {
          error("调试台词流空闲超时")
        }
        throw err
      } finally {
        watchdog.cancel()
      }
    }
  }

  suspend fun getVoiceModels(): List<VoiceModelConfig> {
    return runCatching { api().getVoiceModelList().data }.getOrElse { emptyList() }
  }

  suspend fun getModelConfigs(): List<ModelConfigItem> {
    return runCatching { api().getModelConfigs().data }.getOrElse { emptyList() }
  }

  suspend fun addModelConfig(type: String, model: String, baseUrl: String, apiKey: String, modelType: String, manufacturer: String) {
    val payload = JsonObject().apply {
      addProperty("type", type)
      addProperty("model", model)
      addProperty("baseUrl", baseUrl)
      addProperty("apiKey", apiKey)
      addProperty("modelType", modelType)
      addProperty("manufacturer", manufacturer)
    }
    api().addModelConfig(payload)
  }

  suspend fun updateModelConfig(id: Long, type: String, model: String, baseUrl: String, apiKey: String, modelType: String, manufacturer: String) {
    val payload = JsonObject().apply {
      addProperty("id", id)
      addProperty("type", type)
      addProperty("model", model)
      addProperty("baseUrl", baseUrl)
      addProperty("apiKey", apiKey)
      addProperty("modelType", modelType)
      addProperty("manufacturer", manufacturer)
    }
    api().updateModelConfig(payload)
  }

  suspend fun getLocalAvatarMattingStatus(manufacturer: String, model: String): LocalAvatarMattingStatus {
    val payload = JsonObject().apply {
      addProperty("manufacturer", manufacturer)
      addProperty("model", model)
    }
    return api().getLocalAvatarMattingStatus(payload).data
  }

  suspend fun installLocalAvatarMatting(manufacturer: String, model: String): LocalAvatarMattingStatus {
    val payload = JsonObject().apply {
      addProperty("manufacturer", manufacturer)
      addProperty("model", model)
    }
    return api().installLocalAvatarMatting(payload).data
  }

  suspend fun deleteModelConfig(id: Long) {
    val payload = JsonObject().apply {
      addProperty("id", id)
    }
    api().deleteModelConfig(payload)
  }

  suspend fun getAiModelMap(): List<AiModelMapItem> {
    return runCatching { api().getAiModelMap().data }.getOrElse { emptyList() }
  }

  suspend fun bindModelConfig(id: Long, configId: Long) {
    val payload = JsonObject().apply {
      addProperty("id", id)
      addProperty("configId", configId)
    }
    api().bindModelConfig(payload)
  }

  suspend fun getPrompts(): List<PromptItem> {
    return runCatching { api().getPrompts().data }.getOrElse { emptyList() }
  }

  suspend fun updatePrompt(id: Long, code: String, customValue: String) {
    val payload = JsonObject().apply {
      addProperty("id", id)
      addProperty("code", code)
      addProperty("customValue", customValue)
    }
    api().updatePrompt(payload)
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
      val provider = listOf("provider", "provider_id").firstNotNullOfOrNull { key ->
        obj.get(key)?.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotBlank() }
      }
      val modes = obj.getAsJsonArray("modes")
        ?.mapNotNull { mode -> mode?.asString?.trim()?.takeIf { it.isNotBlank() } }
        ?: emptyList()
      val description = listOf("description", "desc").firstNotNullOfOrNull { key ->
        obj.get(key)?.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotBlank() }
      }
      VoicePresetItem(
        voiceId = voiceId,
        name = name,
        provider = provider,
        modes = modes,
        description = description,
      )
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
    format: String = "",
    sampleRate: Int? = null,
  ): String {
    val payload = JsonObject().apply {
      if (configId != null && configId > 0L) addProperty("configId", configId)
      addProperty("text", text)
      addProperty("mode", mode)
      if (format.isNotBlank()) addProperty("format", format)
      if (sampleRate != null && sampleRate > 0) addProperty("sampleRate", sampleRate)
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

  suspend fun streamVoice(
    configId: Long?,
    text: String,
    mode: String = "text",
    voiceId: String = "",
    referenceAudioPath: String = "",
    referenceText: String = "",
    promptText: String = "",
    mixVoices: List<VoiceMixItem> = emptyList(),
    format: String = "",
    sampleRate: Int? = null,
  ): String {
    val payload = JsonObject().apply {
      if (configId != null && configId > 0L) addProperty("configId", configId)
      addProperty("text", text)
      addProperty("mode", mode)
      if (format.isNotBlank()) addProperty("format", format)
      if (sampleRate != null && sampleRate > 0) addProperty("sampleRate", sampleRate)
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
    val data = api().streamVoice(payload).data
    return data.get("audioUrl")?.asString?.trim().orEmpty()
  }

  suspend fun transcribeVoice(
    configId: Long?,
    audioBase64: String,
    lang: String = "zh",
    sessionId: String = "",
  ): String {
    val payload = JsonObject().apply {
      if (configId != null && configId > 0L) addProperty("configId", configId)
      addProperty("audioBase64", audioBase64)
      if (lang.isNotBlank()) addProperty("lang", lang)
      if (sessionId.isNotBlank()) addProperty("sessionId", sessionId)
      addProperty("withSegments", false)
    }
    val data = api().transcribeVoice(payload).data
    return data.get("text")?.asString?.trim().orEmpty()
  }

  suspend fun polishVoicePrompt(text: String, style: String = ""): String {
    val payload = JsonObject().apply {
      addProperty("text", text)
      if (style.isNotBlank()) addProperty("style", style)
    }
    val data = api().polishVoicePrompt(payload).data
    return data.get("prompt")?.asString?.trim().orEmpty()
  }

  suspend fun testTextModel(model: String, apiKey: String, baseUrl: String, manufacturer: String): String {
    val payload = JsonObject().apply {
      addProperty("modelName", model)
      addProperty("apiKey", apiKey)
      if (baseUrl.isNotBlank()) addProperty("baseURL", baseUrl)
      addProperty("manufacturer", manufacturer)
    }
    return api().testTextModel(payload).data
  }

  suspend fun testImageModel(model: String, apiKey: String, baseUrl: String, manufacturer: String): String {
    val payload = JsonObject().apply {
      if (model.isNotBlank()) addProperty("modelName", model)
      addProperty("apiKey", apiKey)
      if (baseUrl.isNotBlank()) addProperty("baseURL", baseUrl)
      addProperty("manufacturer", manufacturer)
    }
    return api().testImageModel(payload).data
  }

  suspend fun testVoiceDesignModel(model: String, apiKey: String, baseUrl: String, manufacturer: String): String {
    val payload = JsonObject().apply {
      addProperty("modelName", model)
      addProperty("apiKey", apiKey)
      if (baseUrl.isNotBlank()) addProperty("baseURL", baseUrl)
      addProperty("manufacturer", manufacturer)
    }
    return api().testVoiceDesignModel(payload).data
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
