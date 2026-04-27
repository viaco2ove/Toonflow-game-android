package com.toonflow.game.data

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.toonflow.game.api.ApiClient
import com.toonflow.game.util.VueTagLogger
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
  private val avatarVideoTaskPollIntervalMs = 2000L
  private val avatarVideoTaskTimeoutMs = 30L * 60L * 1000L
  private val debugStreamIdleTimeoutMs = 15000L
  private val debugStreamWatchdogPollMs = 500L

  private fun api() = ApiClient.create(settingsStore)

  private fun <T> unwrapEnvelope(path: String, envelope: ApiEnvelope<T>): T {
    if (envelope.code != 200) {
      val message = envelope.message.trim().ifBlank { "$path 请求失败" }
      VueTagLogger.error("api", "$path failed code=${envelope.code} message=$message")
      error(message)
    }
    return envelope.data
  }

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
      foregroundExt = task.foregroundExt,
    )
  }

  /**
   * 把视频头像队列状态转换为可直接显示的进度文案。
   */
  private fun avatarVideoProgressText(task: RoleAvatarTaskResult): String {
    val status = task.status.trim().lowercase()
    val progressText = task.progress?.takeIf { it > 0 }?.let { " ${it}%" }.orEmpty()
    return when (status) {
      "queued" -> if ((task.queuePosition ?: 0) > 0) "排队中，第 ${task.queuePosition} 个$progressText" else "排队中$progressText"
      "running" -> task.message.ifBlank { "生成中" } + progressText
      "success" -> "生成完成 100%"
      "failed" -> task.errorMessage.ifBlank { task.message.ifBlank { "生成失败" } }
      else -> task.message.ifBlank { "头像任务处理中" }
    }
  }

  suspend fun login(username: String, password: String): JsonObject {
    val payload = JsonObject().apply {
      addProperty("username", username)
      addProperty("password", password)
    }
    return unwrapEnvelope("other/login", api().login(payload))
  }

  suspend fun register(username: String, password: String): JsonObject {
    val payload = JsonObject().apply {
      addProperty("username", username)
      addProperty("password", password)
    }
    return unwrapEnvelope("other/register", api().register(payload))
  }

  suspend fun getProjects(): List<ProjectItem> {
    return unwrapEnvelope("project/getProject", api().getProjects())
  }

  suspend fun getUser(): JsonObject {
    return unwrapEnvelope("user/getUser", api().getUser())
  }

  suspend fun saveUser(
    name: String? = null,
    nickname: String? = null,
    intro: String? = null,
    password: String? = null,
    avatarPath: String? = null,
    avatarBgPath: String? = null,
  ) {
    val payload = JsonObject().apply {
      if (!name.isNullOrBlank()) addProperty("name", name)
      if (nickname != null) addProperty("nickname", nickname)
      if (intro != null) addProperty("intro", intro)
      if (!password.isNullOrBlank()) addProperty("password", password)
      if (avatarPath != null) addProperty("avatarPath", avatarPath)
      if (avatarBgPath != null) addProperty("avatarBgPath", avatarBgPath)
    }
    if (payload.size() == 0) return
    unwrapEnvelope("user/saveUser", api().saveUser(payload))
  }

  suspend fun changePassword(oldPassword: String, newPassword: String) {
    val payload = JsonObject().apply {
      addProperty("oldPassword", oldPassword)
      addProperty("newPassword", newPassword)
    }
    unwrapEnvelope("user/changePassword", api().changePassword(payload))
  }

  suspend fun getWorld(projectId: Long, autoCreate: Boolean): WorldItem? {
    val payload = JsonObject().apply {
      addProperty("projectId", projectId)
      addProperty("autoCreate", autoCreate)
    }
    return runCatching { unwrapEnvelope("game/getWorld", api().getWorld(payload)) }.getOrNull()
  }

  suspend fun getWorldById(worldId: Long): WorldItem? {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
    }
    return runCatching { unwrapEnvelope("game/getWorld", api().getWorld(payload)) }.getOrNull()
  }

  suspend fun listWorlds(projectId: Long? = null, includePublicPublished: Boolean = false): List<WorldItem> {
    val payload = JsonObject().apply {
      if (projectId != null && projectId > 0L) addProperty("projectId", projectId)
      if (includePublicPublished) addProperty("includePublicPublished", true)
    }
    return runCatching { unwrapEnvelope("game/listWorlds", api().listWorlds(payload)) }.getOrElse { emptyList() }
  }

  suspend fun saveWorld(payload: JsonObject): WorldItem {
    return unwrapEnvelope("game/saveWorld", api().saveWorld(payload))
  }

  /**
   * 复制现有故事为新的草稿世界。
   * 后端会把封面、章节资源和世界配置都复制成独立副本。
   */
  suspend fun copyWorld(worldId: Long): WorldItem {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
    }
    return unwrapEnvelope("game/copyWorld", api().copyWorld(payload))
  }

  suspend fun deleteWorld(worldId: Long) {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
    }
    unwrapEnvelope("game/deleteWorld", api().deleteWorld(payload))
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
    return unwrapEnvelope("game/generateImage", api().generateImage(payload))
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
    return unwrapEnvelope("game/uploadImage", api().uploadImage(payload))
  }

  suspend fun convertAvatarVideoToGif(
    projectId: Long? = null,
    base64Data: String,
    fileName: String = "avatar.mp4",
    preferGif: Boolean = false,
    onProgress: (String) -> Unit = {},
  ): SeparatedRoleImageResult {
    val payload = JsonObject().apply {
      if (projectId != null && projectId > 0L) addProperty("projectId", projectId)
      addProperty("base64Data", base64Data)
      if (fileName.isNotBlank()) addProperty("fileName", fileName)
      if (preferGif) addProperty("preferGif", true)
    }
    val task = unwrapEnvelope("game/convertAvatarVideoToGif", api().convertAvatarVideoToGif(payload))
    val taskId = task.taskId.takeIf { it > 0L } ?: task.jobId
    if (taskId <= 0L) {
      error("MP4 转 GIF 任务创建失败")
    }
    val startedAt = System.currentTimeMillis()
    while (System.currentTimeMillis() - startedAt < avatarVideoTaskTimeoutMs) {
      val status = unwrapEnvelope("game/convertAvatarVideoToGif/status", api().convertAvatarVideoToGifStatus(JsonObject().apply {
        addProperty("taskId", taskId)
      }))
      onProgress(avatarVideoProgressText(status))
      when (status.status.trim().lowercase()) {
        "success" -> return resolveRoleAvatarResult(status)
        "failed" -> error(status.errorMessage.ifBlank { status.message.ifBlank { "MP4 转 GIF 失败" } })
      }
      delay(avatarVideoTaskPollIntervalMs)
    }
    error("MP4 转 GIF 处理超时，请稍后重试")
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
    val task = unwrapEnvelope("game/separateRoleAvatar", api().separateRoleAvatar(payload))
    val taskId = task.taskId
    if (taskId <= 0L) {
      error("头像分离任务创建失败")
    }
    val startedAt = System.currentTimeMillis()
    while (System.currentTimeMillis() - startedAt < roleAvatarTaskTimeoutMs) {
      val status = unwrapEnvelope("game/separateRoleAvatar/status", api().separateRoleAvatarStatus(JsonObject().apply {
        addProperty("taskId", taskId)
      }))
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
    return unwrapEnvelope("game/getChapter", api().getChapter(payload))
  }

  suspend fun saveChapter(payload: JsonObject): ChapterItem {
    return unwrapEnvelope("game/saveChapter", api().saveChapter(payload))
  }

  suspend fun previewRuntimeOutline(payload: JsonObject): JsonObject {
    return unwrapEnvelope("game/previewRuntimeOutline", api().previewRuntimeOutline(payload))
  }

  suspend fun startSession(worldId: Long, projectId: Long, chapterId: Long?): String {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
      addProperty("projectId", projectId)
      if (chapterId != null) addProperty("chapterId", chapterId)
    }
    return unwrapEnvelope("game/startSession", api().startSession(payload)).get("sessionId")?.asString ?: ""
  }

  suspend fun initStory(
    worldId: Long,
    projectId: Long,
    title: String,
    skipOpening: Boolean = false,
  ): StoryInitResult {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
      addProperty("projectId", projectId)
      if (title.isNotBlank()) addProperty("title", title)
      if (skipOpening) addProperty("skipOpening", true)
    }
    return unwrapEnvelope("game/initStory", api().initStory(payload))
  }

  /**
   * 正式游玩开场白独立请求，和章节调试保持同一启动顺序。
   */
  suspend fun introduceStory(sessionId: String): SessionOrchestrationResult {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
    }
    return unwrapEnvelope("game/introduction", api().introduceStory(payload))
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
    return unwrapEnvelope("game/listSession", api().listSession(payload))
  }

  suspend fun getSession(sessionId: String): SessionDetail {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
      addProperty("messageLimit", 120)
    }
    return unwrapEnvelope("game/getSession", api().getSession(payload))
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

  suspend fun revisitMessage(sessionId: String, messageId: Long) {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
      addProperty("messageId", messageId)
    }
    api().revisitMessage(payload)
  }

  suspend fun debugRevisitMessage(debugRuntimeKey: String, messageCount: Int): DebugRevisitResult {
    val payload = JsonObject().apply {
      addProperty("debugRuntimeKey", debugRuntimeKey)
      addProperty("messageCount", messageCount)
    }
    return unwrapEnvelope("game/debugRuntimeShared/revisit", api().debugRevisitMessage(payload))
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
      addProperty("orchestrate", false)
    }
    return unwrapEnvelope("game/addMessage", api().addMessage(payload))
  }

  suspend fun commitNarrativeTurn(
    sessionId: String,
    role: String,
    roleType: String,
    eventType: String,
    content: String,
    createTime: Long,
  ): SessionNarrativeResult {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
      addProperty("role", role)
      addProperty("roleType", roleType)
      addProperty("eventType", eventType)
      addProperty("content", content)
      addProperty("createTime", createTime)
      addProperty("saveSnapshot", true)
    }
    return unwrapEnvelope("game/commitNarrativeTurn", api().commitNarrativeTurn(payload))
  }

  suspend fun continueSession(sessionId: String): SessionNarrativeResult {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
    }
    return unwrapEnvelope("game/continueSession", api().continueSession(payload))
  }

  suspend fun orchestrateSession(sessionId: String): SessionOrchestrationResult {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
    }
    return normalizeSessionOrchestrationPlan(
      unwrapEnvelope("game/orchestration", api().orchestrateSession(payload)),
    )
  }

  suspend fun initChapter(sessionId: String, chapterId: Long? = null): InitChapterResult {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
      if (chapterId != null && chapterId > 0L) {
        addProperty("chapterId", chapterId)
      }
    }
    return unwrapEnvelope("game/initChapter", api().initChapter(payload))
  }

  /**
   * 将 /game/orchestration 的最小 data 响应包装成旧业务层继续消费的 plan。
   *
   * 后端现在只允许 data 返回 role/roleType/motive/awaitUser；
   * 安卓上层仍统一从 plan 读取要生成的台词目标和是否交还用户输入。
   */
  private fun normalizeSessionOrchestrationPlan(result: SessionOrchestrationResult): SessionOrchestrationResult {
    if (result.plan != null) return result
    if (result.role.isBlank() && result.motive.isBlank()) return result
    return result.copy(
      plan = DebugNarrativePlan(
        role = result.role.trim(),
        roleType = result.roleType.trim().ifBlank { "narrator" },
        motive = result.motive.trim(),
        awaitUser = result.awaitUser,
      ),
    )
  }

  /**
   * 统一读取正式会话或章节调试的故事运行信息。
   *
   * 用途：
   * - 让故事设定、当前章节事件、调试锚点都从单独接口读取；
   * - 避免继续依赖 orchestration/streamlines 的附带状态。
   */
  suspend fun storyInfo(
    sessionId: String? = null,
    worldId: Long? = null,
    chapterId: Long? = null,
    state: JsonElement? = null,
  ): StoryInfoResult {
    val payload = JsonObject().apply {
      if (!sessionId.isNullOrBlank()) addProperty("sessionId", sessionId)
      if (worldId != null && worldId > 0L) addProperty("worldId", worldId)
      if (chapterId != null && chapterId > 0L) addProperty("chapterId", chapterId)
      if (state != null && !state.isJsonNull) add("state", state)
    }
    return unwrapEnvelope("game/storyInfo", api().storyInfo(payload))
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
    return normalizeDebugOrchestrationPlan(api().orchestrateDebug(payload).data)
  }

  /**
   * 将调试编排的最小 data 响应包装成 DebugNarrativePlan。
   *
   * 这样 UI 和流式台词生成链路不需要感知后端接口已经瘦身。
   */
  private fun normalizeDebugOrchestrationPlan(result: DebugOrchestrationResult): DebugOrchestrationResult {
    if (result.plan != null) return result
    if (result.role.isBlank() && result.motive.isBlank()) return result
    return result.copy(
      plan = DebugNarrativePlan(
        role = result.role.trim(),
        roleType = result.roleType.trim().ifBlank { "narrator" },
        motive = result.motive.trim(),
        awaitUser = result.awaitUser,
      ),
    )
  }

  suspend fun debugIntroduction(
    worldId: Long,
    chapterId: Long?,
    state: JsonElement?,
    messages: List<MessageItem>,
  ): DebugOrchestrationResult {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
      if (chapterId != null && chapterId > 0L) addProperty("chapterId", chapterId)
      if (state != null && !state.isJsonNull) {
        add("state", state)
      }
      add("messages", gson.toJsonTree(messages))
    }
    return api().introduceDebug(payload).data
  }

  suspend fun initDebug(
    worldId: Long,
    chapterId: Long?,
    state: JsonElement?,
    messages: List<MessageItem>,
  ): DebugInitResult {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
      if (chapterId != null && chapterId > 0L) addProperty("chapterId", chapterId)
      if (state != null && !state.isJsonNull) {
        add("state", state)
      }
      add("messages", gson.toJsonTree(messages))
    }
    return unwrapEnvelope("game/initDebug", api().initDebug(payload))
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

  suspend fun streamSessionLines(
    sessionId: String,
    plan: DebugNarrativePlan,
    onEvent: suspend (JsonObject) -> Unit,
  ) {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
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
          error("剧情台词流空闲超时")
        }
        throw err
      } finally {
        watchdog.cancel()
      }
    }
  }

  /**
   * 开场白专用流接口。
   *
   * 用途：
   * - opening 是章节写死文案，不能再走普通 streamlines 的 speaker 改写链；
   * - 这里只消费后端 `/game/streamlines/introduction` 返回的 preset 分片事件；
   * - 安卓正式游玩和 Web 保持一致，先播完 opening，再进入第一章正文编排。
   */
  suspend fun streamSessionIntroductionLines(
    sessionId: String,
    plan: DebugNarrativePlan,
    onEvent: suspend (JsonObject) -> Unit,
  ) {
    val payload = JsonObject().apply {
      addProperty("sessionId", sessionId)
      add("plan", gson.toJsonTree(plan))
    }
    streamIntroductionPayload(payload, onEvent)
  }

  /**
   * 调试 opening 专用流接口。
   *
   * 用途：
   * - 调试首开也要直接播放章节写死的 opening 文案；
   * - 这里把 debug state 和 messages 一起带给后端，让它同步推进调试运行态；
   * - 这样 opening 播完后，后续 debug orchestration 才会基于正确状态继续跑。
   */
  suspend fun streamDebugIntroductionLines(
    worldId: Long,
    chapterId: Long?,
    state: JsonElement?,
    messages: List<MessageItem>,
    plan: DebugNarrativePlan,
    onEvent: suspend (JsonObject) -> Unit,
  ) {
    val payload = JsonObject().apply {
      addProperty("worldId", worldId)
      if (chapterId != null && chapterId > 0L) {
        addProperty("chapterId", chapterId)
      }
      if (state != null && !state.isJsonNull) {
        add("state", state)
      }
      add("messages", gson.toJsonTree(messages))
      add("plan", gson.toJsonTree(plan))
    }
    streamIntroductionPayload(payload, onEvent)
  }

  /**
   * 统一执行 opening 专用流请求。
   *
   * 用途：
   * - 正式游玩和调试 opening 共用同一条 `/game/streamlines/introduction`；
   * - 避免两端各自复制一套 NDJSON 读取、超时和事件分发逻辑。
   */
  private suspend fun streamIntroductionPayload(
    payload: JsonObject,
    onEvent: suspend (JsonObject) -> Unit,
  ) {
    val baseUrl = settingsStore.baseUrl.trim().removeSuffix("/")
    val request = Request.Builder()
      .url("$baseUrl/game/streamlines/introduction")
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
            val body = response.body ?: error("未返回开场白流正文")
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
          error("开场白流空闲超时")
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

  suspend fun addModelConfig(
    type: String,
    model: String,
    baseUrl: String,
    apiKey: String,
    modelType: String,
    manufacturer: String,
    inputPricePer1M: Double,
    outputPricePer1M: Double,
    cacheReadPricePer1M: Double,
    currency: String,
    reasoningEffort: String,
  ) {
    val payload = JsonObject().apply {
      addProperty("type", type)
      addProperty("model", model)
      addProperty("baseUrl", baseUrl)
      addProperty("apiKey", apiKey)
      addProperty("modelType", modelType)
      addProperty("manufacturer", manufacturer)
      addProperty("inputPricePer1M", inputPricePer1M)
      addProperty("outputPricePer1M", outputPricePer1M)
      addProperty("cacheReadPricePer1M", cacheReadPricePer1M)
      addProperty("currency", currency)
      addProperty("reasoningEffort", reasoningEffort)
    }
    api().addModelConfig(payload)
  }

  suspend fun updateModelConfig(
    id: Long,
    type: String,
    model: String,
    baseUrl: String,
    apiKey: String,
    modelType: String,
    manufacturer: String,
    inputPricePer1M: Double,
    outputPricePer1M: Double,
    cacheReadPricePer1M: Double,
    currency: String,
    reasoningEffort: String,
  ) {
    val payload = JsonObject().apply {
      addProperty("id", id)
      addProperty("type", type)
      addProperty("model", model)
      addProperty("baseUrl", baseUrl)
      addProperty("apiKey", apiKey)
      addProperty("modelType", modelType)
      addProperty("manufacturer", manufacturer)
      addProperty("inputPricePer1M", inputPricePer1M)
      addProperty("outputPricePer1M", outputPricePer1M)
      addProperty("cacheReadPricePer1M", cacheReadPricePer1M)
      addProperty("currency", currency)
      addProperty("reasoningEffort", reasoningEffort)
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

  suspend fun getAiModelList(type: String): Map<String, List<AiModelOptionItem>> {
    val payload = JsonObject().apply {
      addProperty("type", type)
    }
    return runCatching { api().getAiModelList(payload).data }.getOrElse { emptyMap() }
  }

  suspend fun getAiTokenUsageLog(startTime: String, endTime: String, type: String): List<AiTokenUsageLogItem> {
    val payload = JsonObject().apply {
      if (startTime.isNotBlank()) addProperty("startTime", startTime)
      if (endTime.isNotBlank()) addProperty("endTime", endTime)
      if (type.isNotBlank()) addProperty("type", type)
      addProperty("limit", 200)
    }
    return unwrapEnvelope("setting/getAiTokenUsageLog", api().getAiTokenUsageLog(payload))
  }

  suspend fun getAiTokenUsageStats(startTime: String, endTime: String, type: String, granularity: String): List<AiTokenUsageStatsItem> {
    val payload = JsonObject().apply {
      if (startTime.isNotBlank()) addProperty("startTime", startTime)
      if (endTime.isNotBlank()) addProperty("endTime", endTime)
      if (type.isNotBlank()) addProperty("type", type)
      addProperty("granularity", granularity.ifBlank { "day" })
    }
    return unwrapEnvelope("setting/getAiTokenUsageStats", api().getAiTokenUsageStats(payload))
  }

  suspend fun bindModelConfig(id: Long, configId: Long) {
    val payload = JsonObject().apply {
      addProperty("id", id)
      addProperty("configId", configId)
    }
    api().bindModelConfig(payload)
  }

  suspend fun saveStoryRuntimeConfig(mode: String): StoryRuntimeConfig {
    val payload = JsonObject().apply {
      addProperty("storyOrchestratorPayloadMode", if (mode == "advanced") "advanced" else "compact")
    }
    return unwrapEnvelope("setting/saveStoryRuntimeConfig", api().saveStoryRuntimeConfig(payload))
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
    roleId: String = "",
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
      if (roleId.isNotBlank()) addProperty("roleId", roleId)
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

  /**
   * 先按当前绑定模式生成稳定参考音频文件，供运行时统一转 clone 通道使用。
   */
  suspend fun generateVoiceBinding(
    configId: Long?,
    mode: String,
    voiceId: String = "",
    referenceAudioPath: String = "",
    referenceText: String = "",
    promptText: String = "",
    mixVoices: List<VoiceMixItem> = emptyList(),
  ): GeneratedVoiceBindingResult {
    val payload = JsonObject().apply {
      if (configId != null && configId > 0L) addProperty("configId", configId)
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
    return unwrapEnvelope("voice/generateBindingVoice", api().generateBindingVoice(payload))
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
    val data = unwrapEnvelope("game/streamvoice", api().streamVoice(payload))
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

  /**
   * 根据当前模式与语音配置，把润色请求交给后端选择更合适的策略。
   */
  suspend fun polishVoicePrompt(
    text: String,
    configId: Long? = null,
    mode: String = "",
    provider: String = "",
  ): String {
    val payload = JsonObject().apply {
      addProperty("text", text)
      if (configId != null && configId > 0L) addProperty("configId", configId)
      if (mode.isNotBlank()) addProperty("mode", mode)
      if (provider.isNotBlank()) addProperty("provider", provider)
    }
    val data = api().polishVoicePrompt(payload).data
    return data.get("prompt")?.asString?.trim().orEmpty()
  }

  suspend fun testTextModel(model: String, apiKey: String, baseUrl: String, manufacturer: String, reasoningEffort: String): String {
    val payload = JsonObject().apply {
      addProperty("modelName", model)
      addProperty("apiKey", apiKey)
      if (baseUrl.isNotBlank()) addProperty("baseURL", baseUrl)
      addProperty("manufacturer", manufacturer)
      addProperty("reasoningEffort", reasoningEffort.ifBlank { "minimal" })
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
