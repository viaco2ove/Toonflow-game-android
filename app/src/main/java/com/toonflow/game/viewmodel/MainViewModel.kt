package com.toonflow.game.viewmodel

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.toonflow.game.data.ChapterItem
import com.toonflow.game.data.ChapterExtra
import com.toonflow.game.data.GameRepository
import com.toonflow.game.data.MessageItem
import com.toonflow.game.data.ModelConfigItem
import com.toonflow.game.data.ProjectItem
import com.toonflow.game.data.PromptItem
import com.toonflow.game.data.RoleParameterCard
import com.toonflow.game.data.SessionDetail
import com.toonflow.game.data.SessionItem
import com.toonflow.game.data.SettingsStore
import com.toonflow.game.data.StoryRole
import com.toonflow.game.data.UploadedVoiceAudioResult
import com.toonflow.game.data.AiModelMapItem
import com.toonflow.game.data.DebugNarrativePlan
import com.toonflow.game.data.VoiceBindingDraft
import com.toonflow.game.data.VoiceModelConfig
import com.toonflow.game.data.VoiceMixItem
import com.toonflow.game.data.VoicePresetItem
import com.toonflow.game.data.WorldItem
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private val settingsStore = SettingsStore(application)
  private val repository = GameRepository(settingsStore)
  private val avatarStdSize = 512
  private val avatarBgSize = 768
  private val coverStdWidth = 1280
  private val coverStdHeight = 720
  private val coverBgWidth = 1536
  private val coverBgHeight = 864

  data class SettingsModelSlot(
    val key: String,
    val label: String,
    val configType: String,
  )

  data class SettingsModelTestResult(
    val kind: String,
    val content: String,
  )

  data class RuntimeMiniGameAction(
    val actionId: String,
    val label: String,
    val desc: String,
  )

  data class RuntimeMiniGameStateItem(
    val key: String,
    val value: String,
  )

  data class RuntimeMiniGameView(
    val gameType: String,
    val displayName: String,
    val status: String,
    val phase: String,
    val round: Int,
    val ruleSummary: String,
    val narration: String,
    val pendingExit: Boolean,
    val stateItems: List<RuntimeMiniGameStateItem>,
    val playerOptions: List<RuntimeMiniGameAction>,
    val controlOptions: List<String>,
  )

  val baseTabs = listOf("主页", "创建", "聊过", "我的")
  val settingsModelSlots = listOf(
    SettingsModelSlot("storyOrchestratorModel", "编排师", "text"),
    SettingsModelSlot("storySpeakerModel", "角色发言", "text"),
    SettingsModelSlot("storyMemoryModel", "记忆管理", "text"),
    SettingsModelSlot("storyImageModel", "AI生图", "image"),
    SettingsModelSlot("storyAvatarMattingModel", "头像分离", "image"),
    SettingsModelSlot("storyVoiceDesignModel", "语音设计", "voice_design"),
    SettingsModelSlot("storyVoiceModel", "语音生成", "voice"),
    SettingsModelSlot("storyAsrModel", "语音识别", "voice"),
  )
  val storyPromptCodes = listOf(
    "story-main",
    "story-orchestrator",
    "story-speaker",
    "story-memory",
    "story-chapter",
    "story-mini-game",
    "story-safety",
  )

  var baseUrl by mutableStateOf(settingsStore.baseUrl)
  var token by mutableStateOf(settingsStore.token)
  var loginUsername by mutableStateOf("admin")
  var loginPassword by mutableStateOf("admin123")
  var notice by mutableStateOf("")
  var activeTab by mutableStateOf("主页")
  var loading by mutableStateOf(false)

  var userName by mutableStateOf("")
  var userId by mutableStateOf(0L)
  var accountAvatarPath by mutableStateOf("")
  var accountAvatarBgPath by mutableStateOf("")
  var userAvatarPath by mutableStateOf("")
  var userAvatarBgPath by mutableStateOf("")
  var accountAvatarProcessing by mutableStateOf(false)
  var storyPlayerAvatarProcessing by mutableStateOf(false)
  var roleAvatarProcessing by mutableStateOf(false)

  val projects = mutableStateListOf<ProjectItem>()
  var selectedProjectId by mutableStateOf(-1L)

  val worlds = mutableStateListOf<WorldItem>()
  var homeRecommendWorldId by mutableStateOf<Long?>(null)
  var hallKeyword by mutableStateOf("")
  var hallCategory by mutableStateOf("all")

  var worldId by mutableStateOf(0L)
  var worldName by mutableStateOf("")
  var worldIntro by mutableStateOf("")
  var worldCoverPath by mutableStateOf("")
  var worldCoverBgPath by mutableStateOf("")
  var playerName by mutableStateOf("用户")
  var playerDesc by mutableStateOf("")
  var playerVoice by mutableStateOf("")
  var playerVoicePresetId by mutableStateOf("")
  var playerVoiceMode by mutableStateOf("text")
  var playerVoiceReferenceAudioPath by mutableStateOf("")
  var playerVoiceReferenceAudioName by mutableStateOf("")
  var playerVoiceReferenceText by mutableStateOf("")
  var playerVoicePromptText by mutableStateOf("")
  var playerVoiceMixVoices by mutableStateOf(defaultMixVoices())
  var narratorName by mutableStateOf("旁白")
  var narratorVoice by mutableStateOf("默认旁白")
  var narratorVoicePresetId by mutableStateOf("")
  var narratorVoiceMode by mutableStateOf("text")
  var narratorVoiceReferenceAudioPath by mutableStateOf("")
  var narratorVoiceReferenceAudioName by mutableStateOf("")
  var narratorVoiceReferenceText by mutableStateOf("")
  var narratorVoicePromptText by mutableStateOf("")
  var narratorVoiceMixVoices by mutableStateOf(defaultMixVoices())
  var globalBackground by mutableStateOf("")
  var allowRoleView by mutableStateOf(true)
  var allowChatShare by mutableStateOf(true)
  var worldPublishStatus by mutableStateOf("draft")
  val npcRoles = mutableStateListOf<StoryRole>()
  val chapterExtras = mutableStateListOf<ChapterExtra>()
  val voiceModels = mutableStateListOf<VoiceModelConfig>()
  val settingsTextConfigs = mutableStateListOf<ModelConfigItem>()
  val settingsImageConfigs = mutableStateListOf<ModelConfigItem>()
  val settingsVoiceDesignConfigs = mutableStateListOf<ModelConfigItem>()
  val settingsVoiceConfigs = mutableStateListOf<VoiceModelConfig>()
  val settingsAiModelMap = mutableStateListOf<AiModelMapItem>()
  val storyPrompts = mutableStateListOf<PromptItem>()
  private val voicePresetsCache = mutableStateMapOf<Long, List<VoicePresetItem>>()
  var voiceLoading by mutableStateOf(false)
  var settingsPanelLoading by mutableStateOf(false)
  var settingsPanelLoaded by mutableStateOf(false)
  var aiGenerating by mutableStateOf(false)

  var chapterTitle by mutableStateOf("")
  var chapterContent by mutableStateOf("")
  var chapterEntryCondition by mutableStateOf("")
  var chapterCondition by mutableStateOf("")
  var chapterOpeningRole by mutableStateOf("旁白")
  var chapterOpeningLine by mutableStateOf("")
  var chapterBackground by mutableStateOf("")
  var chapterMusic by mutableStateOf("")
  var chapterConditionVisible by mutableStateOf(true)
  val chapters = mutableStateListOf<ChapterItem>()
  var selectedChapterId by mutableStateOf<Long?>(null)


  val sessions = mutableStateListOf<SessionItem>()
  var quickInput by mutableStateOf("")
  var currentSessionId by mutableStateOf("")
  var sessionDetail by mutableStateOf<SessionDetail?>(null)
  val messages = mutableStateListOf<MessageItem>()
  private val messageReactions = mutableStateMapOf<String, String>()
  var sendText by mutableStateOf("")

  var debugMode by mutableStateOf(false)
  var debugSessionTitle by mutableStateOf("")
  var debugWorldName by mutableStateOf("")
  var debugWorldIntro by mutableStateOf("")
  var debugChapterId by mutableStateOf<Long?>(null)
  var debugChapterTitle by mutableStateOf("")
  var debugRuntimeState by mutableStateOf<JsonElement?>(null)
  var debugStatePreview by mutableStateOf("{}")
  var debugEndDialog by mutableStateOf<String?>(null)
  var debugLoading by mutableStateOf(false)
  var debugLoadingStage by mutableStateOf("")
  private var debugChapterSequence: List<ChapterItem> = emptyList()
  private var debugMessageSeed: Long = 1L
  private var runtimeRetryTask: (suspend () -> Unit)? = null
  private var runtimeRetryMessageText: String = ""
  private var runtimeRetrying = false
  private data class StoryEditorPersistSnapshot(
    val worldId: Long,
    val worldName: String,
    val worldIntro: String,
    val worldCoverPath: String,
    val worldCoverBgPath: String,
    val playerName: String,
    val playerDesc: String,
    val playerVoice: String,
    val playerVoicePresetId: String,
    val playerVoiceMode: String,
    val playerVoiceReferenceAudioPath: String,
    val playerVoiceReferenceAudioName: String,
    val playerVoiceReferenceText: String,
    val playerVoicePromptText: String,
    val playerVoiceMixVoices: List<VoiceMixItem>,
    val narratorName: String,
    val narratorVoice: String,
    val narratorVoicePresetId: String,
    val narratorVoiceMode: String,
    val narratorVoiceReferenceAudioPath: String,
    val narratorVoiceReferenceAudioName: String,
    val narratorVoiceReferenceText: String,
    val narratorVoicePromptText: String,
    val narratorVoiceMixVoices: List<VoiceMixItem>,
    val globalBackground: String,
    val allowRoleView: Boolean,
    val allowChatShare: Boolean,
    val worldPublishStatus: String,
    val npcRoles: List<StoryRole>,
    val chapters: List<ChapterItem>,
    val chapterExtras: List<ChapterExtra>,
    val selectedChapterId: Long?,
    val chapterTitle: String,
    val chapterContent: String,
    val chapterEntryCondition: String,
    val chapterCondition: String,
    val chapterOpeningRole: String,
    val chapterOpeningLine: String,
    val chapterBackground: String,
    val chapterMusic: String,
    val chapterConditionVisible: Boolean,
  )
  private var storyEditorAutoPersistJob: Job? = null
  private var storyEditorPersistMuted = false
  private var lastPersistedStoryEditorSnapshot: StoryEditorPersistSnapshot? = null
  private var undoStoryEditorSnapshot: StoryEditorPersistSnapshot? = null

  private fun StoryRole.withoutVoiceConfigId(): StoryRole {
    return copy(
      voiceConfigId = null,
      voiceMixVoices = voiceMixVoices.map { it.copy() },
    )
  }

  init {
    if (token.isBlank()) {
      resetRuntimeData()
      notice = "请先登录账号"
      activeTab = "设置"
    } else {
      reloadAll()
    }
  }

  fun setTab(tab: String) {
    activeTab = tab
    if (tab == "设置" && token.isNotBlank()) {
      ensureSettingsPanelData()
    }
  }

  fun openHall() {
    activeTab = "故事大厅"
  }

  fun openSettings() {
    activeTab = "设置"
    if (token.isNotBlank()) {
      ensureSettingsPanelData()
    }
  }

  fun backToMy() {
    activeTab = "我的"
  }

  fun voicePresets(configId: Long?): List<VoicePresetItem> {
    val key = configId ?: return emptyList()
    return voicePresetsCache[key] ?: emptyList()
  }

  fun isAdminAccount(): Boolean {
    return userId == 1L || userName.trim().equals("admin", ignoreCase = true)
  }

  fun settingsConfigOptions(type: String): List<ModelConfigItem> {
    return when (type) {
      "text" -> settingsTextConfigs.toList()
      "image" -> settingsImageConfigs.toList()
      "voice_design" -> settingsVoiceDesignConfigs.toList()
      "voice" -> settingsVoiceConfigs.map {
        ModelConfigItem(
          id = it.id,
          type = "voice",
          model = it.model,
          modelType = it.modelType,
          manufacturer = it.manufacturer,
          baseUrl = it.baseUrl,
          apiKey = it.apiKey,
          createTime = it.createTime,
        )
      }
      else -> emptyList()
    }
  }

  fun settingsModelBinding(key: String): AiModelMapItem? {
    return settingsAiModelMap.firstOrNull { it.key == key }
  }

  private fun normalizeSettingsModelHint(value: String?): String {
    return value.orEmpty().trim().lowercase(Locale.getDefault())
  }

  private fun storySpeakerRecommendationScore(item: ModelConfigItem, orchestratorConfigId: Long?): Double {
    if (item.id <= 0L) return Double.NEGATIVE_INFINITY
    if (orchestratorConfigId != null && item.id == orchestratorConfigId) return Double.NEGATIVE_INFINITY
    val model = normalizeSettingsModelHint(item.model)
    val modelType = normalizeSettingsModelHint(item.modelType)
    var score = 0.0
    if (modelType.isBlank() || modelType == "text") score += 180.0
    if (modelType == "deepthinkingtext") score -= 420.0
    if (model.contains("flash")) score += 320.0
    if (model.contains("lite")) score += 220.0
    if (model.contains("mini")) score += 180.0
    if (model.contains("turbo") || model.contains("instant") || model.contains("speed")) score += 120.0
    if (model.contains("reason") || model.contains("think") || model.contains("deep")) score -= 260.0
    if (model.contains("pro") || model.contains("max") || model.contains("ultra")) score -= 120.0
    score += minOf((item.createTime / 1_000_000_000_000.0), 50.0)
    return score
  }

  private fun isAvatarMattingManufacturer(manufacturer: String): Boolean {
    return manufacturer.trim().equals("bria", ignoreCase = true)
      || manufacturer.trim().equals("aliyun_imageseg", ignoreCase = true)
      || manufacturer.trim().equals("tencent_ci", ignoreCase = true)
      || manufacturer.trim().equals("local_birefnet", ignoreCase = true)
  }

  private fun avatarMattingRecommendationScore(item: ModelConfigItem): Double {
    val manufacturer = item.manufacturer.trim().lowercase(Locale.ROOT)
    var score = 0.0
    if (manufacturer == "bria") score += 1000.0
    if (manufacturer == "local_birefnet") score += 960.0
    if (manufacturer == "tencent_ci") score += 850.0
    if (manufacturer == "aliyun_imageseg") score += 700.0
    if (item.model.trim().equals("SegmentCommonImage", ignoreCase = true)) score += 80.0
    if (item.model.trim().equals("AIPortraitMatting", ignoreCase = true)) score += 120.0
    if (item.model.trim().equals("birefnet-portrait", ignoreCase = true)) score += 160.0
    score += minOf((item.createTime / 1_000_000_000_000.0), 50.0)
    return score
  }

  fun settingsRecommendedModel(key: String): ModelConfigItem? {
    if (key == "storyAvatarMattingModel") {
      return settingsImageConfigs
        .filter { item -> isAvatarMattingManufacturer(item.manufacturer) }
        .sortedWith(compareByDescending<ModelConfigItem> { avatarMattingRecommendationScore(it) }.thenByDescending { it.id })
        .firstOrNull()
    }
    if (key != "storySpeakerModel") return null
    val orchestratorConfigId = settingsModelBinding("storyOrchestratorModel")?.configId
    return settingsTextConfigs
      .asSequence()
      .map { item -> item to storySpeakerRecommendationScore(item, orchestratorConfigId) }
      .filter { (_, score) -> score.isFinite() }
      .sortedWith(compareByDescending<Pair<ModelConfigItem, Double>> { it.second }.thenByDescending { it.first.id })
      .map { it.first }
      .firstOrNull()
  }

  fun settingsModelAdvisory(key: String): String? {
    if (key == "storyAvatarMattingModel") {
      val binding = settingsModelBinding("storyAvatarMattingModel")
      val recommendation = settingsRecommendedModel("storyAvatarMattingModel")
      val recommendationText = recommendation?.let {
        listOf(it.manufacturer.takeIf { value -> value.isNotBlank() }, it.model.takeIf { value -> value.isNotBlank() })
          .filterNotNull()
          .joinToString(" / ")
      }.orEmpty().ifBlank { "Bria / RMBG-2.0" }
      val credentialHint = "Bria 的 API Key 直接填 token；阿里云视觉请填 AccessKeyId|AccessKeySecret 或 JSON；腾讯云数据万象请填 SecretId|SecretKey，Base URL 填标准 COS 桶域名；BiRefNet 本地无需 Key，但首次选择会提示安装本地依赖和模型文件。"
      return if (binding?.configId == null || binding.configId <= 0L) {
        "用于角色头像的主体/背景分离。建议绑定：$recommendationText。未配置时会回退旧的图像大模型分离链路，效果通常更差。$credentialHint"
      } else {
        "这个槽位专门负责角色头像主体/背景分离，不参与普通生图。$credentialHint"
      }
    }
    if (key != "storySpeakerModel") return null
    val speaker = settingsModelBinding("storySpeakerModel")
    val orchestrator = settingsModelBinding("storyOrchestratorModel")
    val recommendation = settingsRecommendedModel("storySpeakerModel")
    val recommendationText = recommendation?.let {
      listOf(it.manufacturer.takeIf { value -> value.isNotBlank() }, it.model.takeIf { value -> value.isNotBlank() })
        .filterNotNull()
        .joinToString(" / ")
    }.orEmpty()
    if (speaker?.configId == null || speaker.configId <= 0L) {
      return if (recommendation != null) {
        "未单独配置。建议绑定：$recommendationText。未配置时运行时会直接提示缺少角色发言模型。"
      } else {
        "未单独配置，且当前没有可直接推荐的独立文本模型。建议先新增一个更快的文本模型后再绑定。"
      }
    }
    if (orchestrator?.configId != null && orchestrator.configId > 0L && orchestrator.configId == speaker.configId) {
      return if (recommendation != null && recommendation.id != speaker.configId) {
        "当前与编排师共用同一模型，单次调试会串行调用两次。建议改绑：$recommendationText。"
      } else {
        "当前与编排师共用同一模型，单次调试会串行调用两次，容易明显变慢。"
      }
    }
    return null
  }

  private suspend fun runtimeVoiceConfigId(key: String): Long? {
    val current = settingsModelBinding(key)?.configId
    if (current != null && current > 0L) return current
    return runCatching { repository.getAiModelMap() }
      .getOrElse { emptyList() }
      .firstOrNull { it.key == key }
      ?.configId
      ?.takeIf { it > 0L }
  }

  private fun runtimeStoryVoiceConfigId(): Long? {
    return settingsModelBinding("storyVoiceModel")?.configId?.takeIf { it > 0L }
  }

  private fun inferFallbackVoicePreset(roleType: String, name: String = "", description: String = ""): String {
    if (roleType == "narrator") return "story_narrator"
    val text = "$name $description".lowercase()
    return if (Regex("[女姐妈妹娘妃后妻她]|female|woman|girl|lady").containsMatchIn(text)) {
      "story_std_female"
    } else {
      "story_std_male"
    }
  }

  fun currentStoryPromptValue(code: String): String {
    val row = storyPrompts.firstOrNull { it.code == code } ?: return ""
    return row.customValue?.takeIf { it.isNotBlank() } ?: row.defaultValue.orEmpty()
  }

  fun canUndoStoryAutoPersist(): Boolean = undoStoryEditorSnapshot != null

  fun primeStoryEditorPersistState(clearUndo: Boolean = true) {
    storyEditorAutoPersistJob?.cancel()
    lastPersistedStoryEditorSnapshot = captureStoryEditorSnapshot()
    if (clearUndo) {
      undoStoryEditorSnapshot = null
    }
  }

  fun scheduleStoryEditorAutoPersist() {
    if (storyEditorPersistMuted || activeTab != "创建" || selectedProjectId <= 0L) return
    val current = captureStoryEditorSnapshot()
    if (!hasPersistableStoryEditorContent(current)) return
    if (lastPersistedStoryEditorSnapshot == current) return
    storyEditorAutoPersistJob?.cancel()
    storyEditorAutoPersistJob = viewModelScope.launch {
      delay(900)
      if (storyEditorPersistMuted || activeTab != "创建") return@launch
      val latest = captureStoryEditorSnapshot()
      if (!hasPersistableStoryEditorContent(latest) || lastPersistedStoryEditorSnapshot == latest) return@launch
      val previous = lastPersistedStoryEditorSnapshot?.copyDeep()
      storyEditorPersistMuted = true
      runCatching {
        saveWorldInternal(false)
        val savedChapter = saveEditorChapterInternal("draft")
        if (savedChapter != null) {
          saveWorldInternal(false)
        }
        refreshStoryData()
        lastPersistedStoryEditorSnapshot = captureStoryEditorSnapshot()
        undoStoryEditorSnapshot = previous
      }.onFailure {
        notice = "自动保存失败: ${it.message ?: "未知错误"}"
      }
      storyEditorPersistMuted = false
    }
  }

  fun undoStoryAutoPersist() {
    val snapshot = undoStoryEditorSnapshot?.copyDeep() ?: return
    storyEditorAutoPersistJob?.cancel()
    undoStoryEditorSnapshot = null
    viewModelScope.launch {
      storyEditorPersistMuted = true
      runCatching {
        applyStoryEditorSnapshot(snapshot)
        saveWorldInternal(false)
        val savedChapter = saveEditorChapterInternal("draft")
        if (savedChapter != null) {
          saveWorldInternal(false)
        }
        refreshStoryData()
        notice = "已撤回到上一次自动保存前"
        lastPersistedStoryEditorSnapshot = captureStoryEditorSnapshot()
      }.onFailure {
        notice = "撤回失败: ${it.message ?: "未知错误"}"
      }
      storyEditorPersistMuted = false
    }
  }

  private fun StoryEditorPersistSnapshot.copyDeep(): StoryEditorPersistSnapshot =
    copy(
      playerVoiceMixVoices = playerVoiceMixVoices.map { it.copy() },
      narratorVoiceMixVoices = narratorVoiceMixVoices.map { it.copy() },
      npcRoles = npcRoles.map { it.copy(voiceMixVoices = it.voiceMixVoices.map { voice -> voice.copy() }) },
      chapters = chapters.map { it.copy() },
      chapterExtras = chapterExtras.map { it.copy() },
    )

  private fun captureStoryEditorSnapshot(): StoryEditorPersistSnapshot {
    return StoryEditorPersistSnapshot(
      worldId = worldId,
      worldName = worldName,
      worldIntro = worldIntro,
      worldCoverPath = worldCoverPath,
      worldCoverBgPath = worldCoverBgPath,
      playerName = playerName,
      playerDesc = playerDesc,
      playerVoice = playerVoice,
      playerVoicePresetId = playerVoicePresetId,
      playerVoiceMode = playerVoiceMode,
      playerVoiceReferenceAudioPath = playerVoiceReferenceAudioPath,
      playerVoiceReferenceAudioName = playerVoiceReferenceAudioName,
      playerVoiceReferenceText = playerVoiceReferenceText,
      playerVoicePromptText = playerVoicePromptText,
      playerVoiceMixVoices = playerVoiceMixVoices.map { it.copy() },
      narratorName = narratorName,
      narratorVoice = narratorVoice,
      narratorVoicePresetId = narratorVoicePresetId,
      narratorVoiceMode = narratorVoiceMode,
      narratorVoiceReferenceAudioPath = narratorVoiceReferenceAudioPath,
      narratorVoiceReferenceAudioName = narratorVoiceReferenceAudioName,
      narratorVoiceReferenceText = narratorVoiceReferenceText,
      narratorVoicePromptText = narratorVoicePromptText,
      narratorVoiceMixVoices = narratorVoiceMixVoices.map { it.copy() },
      globalBackground = globalBackground,
      allowRoleView = allowRoleView,
      allowChatShare = allowChatShare,
      worldPublishStatus = worldPublishStatus,
      npcRoles = npcRoles.map { it.withoutVoiceConfigId() },
      chapters = chapters.map { it.copy() },
      chapterExtras = chapterExtras.map { it.copy() },
      selectedChapterId = selectedChapterId,
      chapterTitle = chapterTitle,
      chapterContent = chapterContent,
      chapterEntryCondition = chapterEntryCondition,
      chapterCondition = chapterCondition,
      chapterOpeningRole = chapterOpeningRole,
      chapterOpeningLine = chapterOpeningLine,
      chapterBackground = chapterBackground,
      chapterMusic = chapterMusic,
      chapterConditionVisible = chapterConditionVisible,
    )
  }

  private fun applyStoryEditorSnapshot(snapshot: StoryEditorPersistSnapshot) {
    val extractedOpening = extractOpeningContentParts(snapshot.chapterContent)
    val snapshotOpeningRole = normalizeScalarEditorText(snapshot.chapterOpeningRole).trim().ifBlank {
      extractedOpening?.role ?: snapshot.narratorName.ifBlank { "旁白" }
    }
    val snapshotOpeningLine = normalizeScalarEditorText(snapshot.chapterOpeningLine).trim().ifBlank {
      extractedOpening?.line.orEmpty()
    }
    worldId = snapshot.worldId
    worldName = snapshot.worldName
    worldIntro = snapshot.worldIntro
    worldCoverPath = snapshot.worldCoverPath
    worldCoverBgPath = snapshot.worldCoverBgPath
    playerName = snapshot.playerName
    playerDesc = snapshot.playerDesc
    playerVoice = snapshot.playerVoice
    playerVoicePresetId = snapshot.playerVoicePresetId
    playerVoiceMode = snapshot.playerVoiceMode
    playerVoiceReferenceAudioPath = snapshot.playerVoiceReferenceAudioPath
    playerVoiceReferenceAudioName = snapshot.playerVoiceReferenceAudioName
    playerVoiceReferenceText = snapshot.playerVoiceReferenceText
    playerVoicePromptText = snapshot.playerVoicePromptText
    playerVoiceMixVoices = snapshot.playerVoiceMixVoices.map { it.copy() }
    narratorName = snapshot.narratorName
    narratorVoice = snapshot.narratorVoice
    narratorVoicePresetId = snapshot.narratorVoicePresetId
    narratorVoiceMode = snapshot.narratorVoiceMode
    narratorVoiceReferenceAudioPath = snapshot.narratorVoiceReferenceAudioPath
    narratorVoiceReferenceAudioName = snapshot.narratorVoiceReferenceAudioName
    narratorVoiceReferenceText = snapshot.narratorVoiceReferenceText
    narratorVoicePromptText = snapshot.narratorVoicePromptText
    narratorVoiceMixVoices = snapshot.narratorVoiceMixVoices.map { it.copy() }
    globalBackground = snapshot.globalBackground
    allowRoleView = snapshot.allowRoleView
    allowChatShare = snapshot.allowChatShare
    worldPublishStatus = snapshot.worldPublishStatus
    npcRoles.clear()
    npcRoles.addAll(snapshot.npcRoles.map { it.withoutVoiceConfigId() })
    chapters.clear()
    chapters.addAll(snapshot.chapters.map { it.copy() })
    chapterExtras.clear()
    chapterExtras.addAll(snapshot.chapterExtras.map { it.copy() })
    selectedChapterId = snapshot.selectedChapterId
    chapterTitle = snapshot.chapterTitle
    chapterContent = stripOpeningPrefix(snapshot.chapterContent, snapshotOpeningRole, snapshotOpeningLine)
    chapterEntryCondition = normalizeConditionEditorText(snapshot.chapterEntryCondition)
    chapterCondition = normalizeConditionEditorText(snapshot.chapterCondition)
    chapterOpeningRole = snapshotOpeningRole
    chapterOpeningLine = snapshotOpeningLine
    chapterBackground = snapshot.chapterBackground
    chapterMusic = snapshot.chapterMusic
    chapterConditionVisible = snapshot.chapterConditionVisible
  }

  private fun hasPersistableStoryEditorContent(snapshot: StoryEditorPersistSnapshot = captureStoryEditorSnapshot()): Boolean {
    if (snapshot.worldId > 0L) return true
    if (snapshot.worldName.isNotBlank() || snapshot.worldIntro.isNotBlank()) return true
    if (snapshot.worldCoverPath.isNotBlank() || snapshot.playerDesc.isNotBlank() || snapshot.globalBackground.isNotBlank()) return true
    if (snapshot.chapterTitle.isNotBlank() || snapshot.chapterContent.isNotBlank() || snapshot.chapterOpeningLine.isNotBlank()) return true
    if (snapshot.chapterEntryCondition.isNotBlank() || snapshot.chapterCondition.isNotBlank()) return true
    if (snapshot.chapterBackground.isNotBlank() || snapshot.chapterMusic.isNotBlank()) return true
    return snapshot.npcRoles.any { role ->
      role.name.isNotBlank() ||
        role.description.isNotBlank() ||
        role.avatarPath.isNotBlank() ||
        role.avatarBgPath.isNotBlank() ||
        role.voice.isNotBlank() ||
        role.sample.isNotBlank()
    }
  }

  fun ensureSettingsPanelData(force: Boolean = false) {
    if (token.isBlank()) return
    if (settingsPanelLoading) return
    if (settingsPanelLoaded && !force) return
    settingsPanelLoading = true
    viewModelScope.launch {
      runCatching {
        val configs = repository.getModelConfigs()
        val voiceConfigs = repository.getVoiceModels()
        val bindings = repository.getAiModelMap()
        val prompts = repository.getPrompts()
        settingsTextConfigs.clear()
        settingsTextConfigs.addAll(configs.filter { it.type == "text" }.sortedBy { it.id })
        settingsImageConfigs.clear()
        settingsImageConfigs.addAll(configs.filter { it.type == "image" }.sortedBy { it.id })
        settingsVoiceDesignConfigs.clear()
        settingsVoiceDesignConfigs.addAll(configs.filter { it.type == "voice_design" }.sortedBy { it.id })
        settingsVoiceConfigs.clear()
        settingsVoiceConfigs.addAll(voiceConfigs.filter { (it.type.ifBlank { "voice" }) == "voice" }.sortedBy { it.id })
        settingsAiModelMap.clear()
        settingsAiModelMap.addAll(bindings.filter { item -> settingsModelSlots.any { it.key == item.key } }.sortedBy { it.id })
        storyPrompts.clear()
        storyPrompts.addAll(prompts.filter { it.code in storyPromptCodes }.sortedBy { it.id })
        settingsPanelLoaded = true
      }.onFailure {
        notice = "加载设置失败: ${it.message ?: "未知错误"}"
      }
      settingsPanelLoading = false
    }
  }

  fun bindGameModel(key: String, configId: Long) {
    val row = settingsModelBinding(key)
    if (row == null) {
      notice = "模型槽位不存在"
      return
    }
    viewModelScope.launch {
      runCatching {
        repository.bindModelConfig(row.id, configId)
        ensureSettingsPanelData(true)
      }.onSuccess {
        notice = "模型配置已保存"
      }.onFailure {
        notice = "保存模型配置失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun bindRecommendedGameModel(key: String) {
    val recommendation = settingsRecommendedModel(key)
    if (recommendation == null) {
      notice = if (key == "storySpeakerModel") {
        "当前没有可直接推荐的独立角色发言模型，请先新增一个文本模型"
      } else {
        "当前没有可直接推荐的模型"
      }
      return
    }
    bindGameModel(key, recommendation.id)
  }

  suspend fun addManagedModelConfig(
    type: String,
    model: String,
    baseUrl: String,
    apiKey: String,
    modelType: String,
    manufacturer: String,
  ) {
    repository.addModelConfig(type, model, baseUrl, apiKey, modelType, manufacturer)
    ensureSettingsPanelData(true)
  }

  suspend fun updateManagedModelConfig(
    id: Long,
    type: String,
    model: String,
    baseUrl: String,
    apiKey: String,
    modelType: String,
    manufacturer: String,
  ) {
    repository.updateModelConfig(id, type, model, baseUrl, apiKey, modelType, manufacturer)
    ensureSettingsPanelData(true)
  }

  suspend fun getLocalAvatarMattingStatus(manufacturer: String, model: String): com.toonflow.game.data.LocalAvatarMattingStatus {
    return repository.getLocalAvatarMattingStatus(manufacturer, model)
  }

  suspend fun installLocalAvatarMattingModel(manufacturer: String, model: String): com.toonflow.game.data.LocalAvatarMattingStatus {
    return repository.installLocalAvatarMatting(manufacturer, model)
  }

  suspend fun deleteManagedModelConfig(id: Long) {
    repository.deleteModelConfig(id)
    ensureSettingsPanelData(true)
  }

  suspend fun testManagedModelConfig(config: ModelConfigItem): SettingsModelTestResult {
    return when (config.type.ifBlank { "text" }) {
      "voice_design" -> SettingsModelTestResult(
        kind = "audio",
        content = resolveMediaPath(repository.testVoiceDesignModel(config.model, config.apiKey, config.baseUrl, config.manufacturer)),
      )
      "text" -> SettingsModelTestResult(
        kind = "text",
        content = repository.testTextModel(config.model, config.apiKey, config.baseUrl, config.manufacturer),
      )
      "image" -> {
        if (isAvatarMattingManufacturer(config.manufacturer)) {
          SettingsModelTestResult(
            kind = "text",
            content = "当前是头像分离专用模型，不走普通生图测试。请在角色头像上传或 AI 生成后直接验证主体/背景分离效果。",
          )
        } else {
          SettingsModelTestResult(
            kind = "image",
            content = resolveMediaPath(repository.testImageModel(config.model, config.apiKey, config.baseUrl, config.manufacturer)),
          )
        }
      }
      else -> {
        if (config.modelType == "asr") {
          return SettingsModelTestResult(
            kind = "text",
            content = "当前为语音识别模型。设置页暂不内置样本音频测试，请在录音入口验证。",
          )
        }
        val presets = repository.getVoicePresets(config.id)
        val firstVoice = presets.firstOrNull { it.voiceId.isNotBlank() }?.voiceId
          ?: error("当前语音模型没有可用音色，无法测试")
        SettingsModelTestResult(
          kind = "audio",
          content = resolveMediaPath(
            repository.previewVoice(
              configId = config.id,
              text = "这是 AI 故事设置页的语音模型测试。",
              mode = "text",
              voiceId = firstVoice,
            ),
          ),
        )
      }
    }
  }

  fun saveStoryPrompt(code: String, value: String) {
    val row = storyPrompts.firstOrNull { it.code == code }
    if (row == null) {
      notice = "提示词不存在"
      return
    }
    viewModelScope.launch {
      runCatching {
        repository.updatePrompt(row.id, code, value)
        val index = storyPrompts.indexOfFirst { it.code == code }
        if (index >= 0) {
          storyPrompts[index] = storyPrompts[index].copy(customValue = value)
        }
      }.onSuccess {
        notice = "提示词已保存"
      }.onFailure {
        notice = "保存提示词失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun resetStoryPrompt(code: String) {
    saveStoryPrompt(code, "")
    notice = "提示词已重置为默认值"
  }

  companion object {
    private const val RUNTIME_RETRY_EVENT = "on_runtime_retry_error"
    private fun defaultMixVoices(): List<VoiceMixItem> = listOf(VoiceMixItem(weight = 0.7))
  }

  private fun normalizedMixVoices(mixVoices: List<VoiceMixItem>): List<VoiceMixItem> {
    return mixVoices
      .filter { it.voiceId.isNotBlank() }
      .map { item -> item.copy(weight = item.weight.coerceIn(0.1, 1.0)) }
  }

  fun ensureVoiceModels() {
    if (voiceLoading || voiceModels.isNotEmpty()) return
    voiceLoading = true
    viewModelScope.launch {
      runCatching { repository.getVoiceModels() }
        .onSuccess { rows ->
          val ttsRows = rows.filter {
            val modelType = it.modelType.trim()
            modelType.isBlank() || modelType == "tts"
          }
          voiceModels.clear()
          voiceModels.addAll(ttsRows.sortedBy { it.id })
        }
        .onFailure {
          notice = "加载音色模型失败: ${it.message ?: "未知错误"}"
        }
      voiceLoading = false
    }
  }

  fun ensureVoicePresets(configId: Long?) {
    val key = configId ?: return
    if (voicePresetsCache.containsKey(key)) return
    viewModelScope.launch {
      runCatching { repository.getVoicePresets(key) }
        .onSuccess { rows ->
          voicePresetsCache[key] = rows
        }
        .onFailure {
          notice = "加载音色列表失败: ${it.message ?: "未知错误"}"
        }
    }
  }

  fun saveConnection() {
    settingsStore.baseUrl = baseUrl
    if (token.isNotBlank()) {
      settingsStore.token = token
      notice = "连接设置已保存"
      settingsPanelLoaded = false
      reloadAll()
    } else {
      notice = "连接设置已保存，请先登录账号"
    }
  }

  fun loginAndSaveToken() {
    if (loginUsername.isBlank() || loginPassword.isBlank()) {
      notice = "请输入用户名和密码"
      return
    }
    viewModelScope.launch {
      runCatching {
        settingsStore.baseUrl = baseUrl
        val loginResult = repository.login(loginUsername.trim(), loginPassword)
        val fetchedToken = loginResult.get("token")?.asString?.trim().orEmpty()
        if (fetchedToken.isBlank()) error("登录成功但未返回 token")
        token = fetchedToken
        settingsStore.token = fetchedToken
        notice = "登录成功"
        reloadAll()
        ensureSettingsPanelData(true)
        activeTab = "我的"
      }.onFailure {
        notice = "登录失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun registerAndLogin(username: String, password: String) {
    if (username.isBlank() || password.isBlank()) {
      notice = "请输入用户名和密码"
      return
    }
    viewModelScope.launch {
      runCatching {
        settingsStore.baseUrl = baseUrl
        val result = repository.register(username.trim(), password)
        val fetchedToken = result.get("token")?.asString?.trim().orEmpty()
        if (fetchedToken.isBlank()) error("注册成功但未返回 token")
        token = fetchedToken
        settingsStore.token = fetchedToken
        loginUsername = username.trim()
        loginPassword = password
        notice = "注册成功"
        reloadAll()
        ensureSettingsPanelData(true)
        activeTab = "我的"
      }.onFailure {
        notice = "注册失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun changePassword(oldPassword: String, newPassword: String) {
    if (token.isBlank()) {
      notice = "请先登录账号"
      return
    }
    viewModelScope.launch {
      runCatching {
        repository.changePassword(oldPassword, newPassword)
        loginPassword = newPassword
      }.onSuccess {
        notice = "密码已更新"
      }.onFailure {
        notice = "修改密码失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  suspend fun previewVoice(
    configId: Long?,
    text: String,
    mode: String = "text",
    presetId: String = "",
    referenceAudioPath: String = "",
    referenceText: String = "",
    promptText: String = "",
    mixVoices: List<VoiceMixItem> = emptyList(),
    format: String = "",
    sampleRate: Int? = null,
  ): String {
    return repository.previewVoice(
      configId = configId,
      text = text,
      mode = mode,
      voiceId = presetId,
      referenceAudioPath = referenceAudioPath,
      referenceText = referenceText,
      promptText = promptText,
      mixVoices = normalizedMixVoices(mixVoices),
      format = format,
      sampleRate = sampleRate,
    )
  }

  suspend fun streamVoice(
    configId: Long?,
    text: String,
    mode: String = "text",
    presetId: String = "",
    referenceAudioPath: String = "",
    referenceText: String = "",
    promptText: String = "",
    mixVoices: List<VoiceMixItem> = emptyList(),
    format: String = "",
    sampleRate: Int? = null,
  ): String {
    return repository.streamVoice(
      configId = configId,
      text = text,
      mode = mode,
      voiceId = presetId,
      referenceAudioPath = referenceAudioPath,
      referenceText = referenceText,
      promptText = promptText,
      mixVoices = normalizedMixVoices(mixVoices),
      format = format,
      sampleRate = sampleRate,
    )
  }

  suspend fun polishVoicePrompt(text: String, style: String = ""): String {
    return repository.polishVoicePrompt(text, style)
  }

  suspend fun transcribeRuntimeVoice(audioBase64: String, sessionId: String = ""): String {
    val configId = runtimeVoiceConfigId("storyAsrModel")
    return repository.transcribeVoice(
      configId = configId,
      audioBase64 = audioBase64,
      lang = "zh",
      sessionId = sessionId,
    )
  }

  suspend fun uploadVoiceReferenceAudio(rawUri: String): UploadedVoiceAudioResult {
    if (selectedProjectId <= 0L) error("请先选择项目后再上传参考音频")
    val uri = Uri.parse(rawUri)
    val (base64Data, fileName) = audioUriToBase64(uri)
    return repository.uploadVoiceAudio(selectedProjectId, base64Data, fileName)
  }

  fun clearToken() {
    token = ""
    settingsStore.token = ""
    settingsPanelLoaded = false
    settingsTextConfigs.clear()
    settingsImageConfigs.clear()
    settingsVoiceDesignConfigs.clear()
    settingsVoiceConfigs.clear()
    settingsAiModelMap.clear()
    storyPrompts.clear()
    resetRuntimeData()
    activeTab = "设置"
    notice = "已退出登录"
  }

  fun playSessionTitle(): String {
    if (debugMode) {
      return debugSessionTitle.ifBlank { "章节调试" }
    }
    return sessionDetail?.title?.takeIf { it.isNotBlank() }
      ?: sessionDetail?.world?.name?.takeIf { it.isNotBlank() }
      ?: "游玩故事"
  }

  fun playWorldName(): String {
    if (debugMode) return debugWorldName.ifBlank { "调试世界" }
    return sessionDetail?.world?.name?.ifBlank { playSessionTitle() } ?: playSessionTitle()
  }

  fun playWorldIntro(): String {
    if (debugMode) return debugWorldIntro.ifBlank { "暂无简介" }
    return sessionDetail?.world?.intro?.ifBlank { "暂无简介" } ?: "暂无简介"
  }

  fun playCurrentChapter(): ChapterItem? {
    if (debugMode) {
      val chapterId = debugChapterId ?: return null
      return debugChapterSequence.firstOrNull { it.id == chapterId }
    }
    return sessionDetail?.chapter
  }

  fun playChapterTitle(): String {
    if (debugMode) {
      val chapter = playCurrentChapter()
      return normalizeChapterTitleLabel(debugChapterTitle.ifBlank { chapter?.title.orEmpty() }, chapter?.sort ?: 0).ifBlank { "未进入章节" }
    }
    val chapter = sessionDetail?.chapter
    return normalizeChapterTitleLabel(chapter?.title.orEmpty(), chapter?.sort ?: 0).ifBlank { "未进入章节" }
  }

  fun playStatePreview(): String {
    if (debugMode) return debugStatePreview.ifBlank { "{}" }
    return sessionDetail?.state?.toString().orEmpty().ifBlank { "{}" }.take(320)
  }

  private fun runtimeStateRoot(): JsonObject? {
    val source = if (debugMode) debugRuntimeState else sessionDetail?.state
    if (source == null || source.isJsonNull || !source.isJsonObject) return null
    return source.asJsonObject
  }

  private fun runtimeTurnStateRoot(): JsonObject? {
    return runtimeStateRoot()?.getAsJsonObject("turnState")
  }

  private fun scalarRuntimeText(input: JsonElement?): String {
    if (input == null || input.isJsonNull) return ""
    return runCatching { input.asString }.getOrElse { input.toString() }
      .trim()
      .takeIf { it.isNotBlank() && it != "null" && it != "undefined" }
      .orEmpty()
  }

  private fun runtimeStringify(input: JsonElement?): String {
    if (input == null || input.isJsonNull) return ""
    return when {
      input.isJsonPrimitive -> scalarRuntimeText(input)
      input.isJsonArray -> input.asJsonArray.mapNotNull { item ->
        runtimeStringify(item).takeIf { it.isNotBlank() }
      }.joinToString("、")
      input.isJsonObject -> runCatching { input.toString() }.getOrElse { "" }
      else -> input.toString()
    }
  }

  fun playCanPlayerSpeak(): Boolean {
    return runtimeTurnStateRoot()?.get("canPlayerSpeak")?.asBoolean ?: true
  }

  fun playExpectedSpeaker(): String {
    return scalarRuntimeText(runtimeTurnStateRoot()?.get("expectedRole")).ifBlank { "当前角色" }
  }

  fun playInputPlaceholder(textMode: Boolean): String {
    if (playCanPlayerSpeak()) {
      return if (textMode) "输入一句话开始故事" else "按住说话"
    }
    return "当前轮到${playExpectedSpeaker()}发言"
  }

  fun playTurnHint(): String {
    if (playCanPlayerSpeak()) return ""
    return "当前还没轮到用户发言，等待${playExpectedSpeaker()}继续。"
  }

  fun playRuntimeMiniGame(): RuntimeMiniGameView? {
    val root = runtimeStateRoot()?.getAsJsonObject("miniGame") ?: return null
    val session = root.getAsJsonObject("session") ?: JsonObject()
    val ui = root.getAsJsonObject("ui") ?: JsonObject()
    val status = scalarRuntimeText(session.get("status"))
    val gameType = scalarRuntimeText(session.get("game_type")).ifBlank { scalarRuntimeText(session.get("gameType")) }
    if (gameType.isBlank()) return null
    val playerOptionsSource = when {
      ui.get("player_options")?.isJsonArray == true -> ui.getAsJsonArray("player_options")
      session.get("player_options")?.isJsonArray == true -> session.getAsJsonArray("player_options")
      else -> JsonArray()
    }
    val playerOptions = playerOptionsSource.mapNotNull { item ->
      if (!item.isJsonObject) return@mapNotNull null
      val obj = item.asJsonObject
      val actionId = scalarRuntimeText(obj.get("action_id"))
      val label = scalarRuntimeText(obj.get("label")).ifBlank { actionId }
      if (label.isBlank()) return@mapNotNull null
      RuntimeMiniGameAction(
        actionId = actionId.ifBlank { label },
        label = label,
        desc = scalarRuntimeText(obj.get("desc")),
      )
    }
    val pendingExit = session.get("pending_exit")?.asBoolean ?: false
    val visibleStatuses = setOf("preparing", "active", "settling", "suspended")
    if (!visibleStatuses.contains(status) && playerOptions.isEmpty() && !pendingExit) return null
    val publicState = session.getAsJsonObject("public_state") ?: JsonObject()
    val stateItems = publicState.entrySet()
      .mapNotNull { entry ->
        val value = runtimeStringify(entry.value).trim()
        if (value.isBlank()) null else RuntimeMiniGameStateItem(entry.key, value)
      }
      .take(10)
    val controlOptions = when {
      status == "suspended" -> listOf("恢复小游戏", "查看状态", "查看规则", "申请退出")
      pendingExit -> listOf("确认退出", "继续", "查看状态")
      else -> listOf("查看状态", "查看规则", "暂停", "申请退出")
    }
    return RuntimeMiniGameView(
      gameType = gameType,
      displayName = scalarRuntimeText(root.getAsJsonObject("rulebook")?.get("displayName")).ifBlank { gameType },
      status = status.ifBlank { "active" },
      phase = scalarRuntimeText(session.get("phase")),
      round = runCatching { session.get("round")?.asInt ?: 0 }.getOrDefault(0),
      ruleSummary = scalarRuntimeText(ui.get("rule_summary")),
      narration = scalarRuntimeText(ui.get("narration")),
      pendingExit = pendingExit,
      stateItems = stateItems,
      playerOptions = playerOptions,
      controlOptions = controlOptions,
    )
  }

  fun playChapterConditionText(): String {
    return normalizeConditionEditorText(playCurrentChapter()?.completionCondition).ifBlank { "无" }
  }

  fun playVisibleChapterObjective(): String {
    val chapter = playCurrentChapter() ?: return ""
    if (chapter.showCompletionCondition == false) return ""
    return normalizeConditionEditorText(chapter.completionCondition)
  }

  fun playGlobalBackground(): String {
    if (debugMode) return globalBackground
    return sessionDetail?.world?.settings?.globalBackground.orEmpty()
  }

  fun playAllowRoleView(): Boolean {
    if (debugMode) return allowRoleView
    return sessionDetail?.world?.settings?.allowRoleView ?: true
  }

  fun playStoryRoles(): List<StoryRole> {
    if (debugMode) {
      val player = buildRole(
        id = "player",
        roleType = "player",
        name = playerName.ifBlank { "用户" },
        avatarPath = userAvatarPath,
        avatarBgPath = userAvatarBgPath,
        description = playerDesc,
        voice = playerVoice,
        voiceMode = playerVoiceMode,
        voicePresetId = playerVoicePresetId,
        voiceReferenceAudioPath = playerVoiceReferenceAudioPath,
        voiceReferenceAudioName = playerVoiceReferenceAudioName,
        voiceReferenceText = playerVoiceReferenceText,
        voicePromptText = playerVoicePromptText,
        voiceMixVoices = playerVoiceMixVoices,
        sample = "",
      )
      val narrator = buildRole(
        id = "narrator",
        roleType = "narrator",
        name = narratorName.ifBlank { "旁白" },
        avatarPath = "",
        avatarBgPath = "",
        description = "负责叙事推进",
        voice = narratorVoice,
        voiceMode = narratorVoiceMode,
        voicePresetId = narratorVoicePresetId,
        voiceReferenceAudioPath = narratorVoiceReferenceAudioPath,
        voiceReferenceAudioName = narratorVoiceReferenceAudioName,
        voiceReferenceText = narratorVoiceReferenceText,
        voicePromptText = narratorVoicePromptText,
        voiceMixVoices = narratorVoiceMixVoices,
        sample = "",
      )
      return (listOf(player, narrator) + npcRoles.toList()).map(::resolveRoleMedia)
    }
    val world = sessionDetail?.world ?: return emptyList()
    val roles = mutableListOf<StoryRole>()
    world.playerRole?.let { roles.add(it) }
    world.narratorRole?.let { roles.add(it) }
    world.settings?.roles?.filter { it.roleType == "npc" }?.forEach { roles.add(it) }
    return roles.map(::resolveRoleMedia).distinctBy { it.id.ifBlank { "${it.roleType}:${it.name}" } }
  }

  fun closeDebugDialog(backToCreate: Boolean) {
    debugEndDialog = null
    if (backToCreate) {
      leaveDebugMode()
      activeTab = "创建"
    }
  }

  fun leaveDebugMode() {
    clearRuntimeRetryState()
    debugMode = false
    debugLoading = false
    debugLoadingStage = ""
    debugSessionTitle = ""
    debugWorldName = ""
    debugWorldIntro = ""
    debugChapterId = null
    debugChapterTitle = ""
    debugRuntimeState = null
    debugStatePreview = "{}"
    debugEndDialog = null
    debugChapterSequence = emptyList()
    debugMessageSeed = 1L
    currentSessionId = ""
    sessionDetail = null
    messages.clear()
    sendText = ""
  }

  fun updateAccountAvatarFromUri(rawUri: String) {
    if (userId <= 0L) {
      notice = "请先登录后再设置头像"
      return
    }
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
    if (uri == null) {
      notice = "头像选择失败"
      return
    }

    viewModelScope.launch {
      accountAvatarProcessing = true
      try {
        runCatching {
          val saved = saveAvatarFiles(uri, "user_${userId}", projectScoped = false)
          accountAvatarPath = saved.foregroundPath
          accountAvatarBgPath = saved.backgroundPath
          val storedAvatarPath = normalizeStoredMediaPath(accountAvatarPath)
          val storedAvatarBgPath = normalizeStoredMediaPath(accountAvatarBgPath)
          settingsStore.setAvatarPath(userId, storedAvatarPath)
          settingsStore.setAvatarBgPath(userId, storedAvatarBgPath)
          repository.saveUser(
            avatarPath = storedAvatarPath,
            avatarBgPath = storedAvatarBgPath,
          )
          notice = "账号头像已更新"
        }.onFailure {
          notice = "账号头像更新失败: ${it.message ?: "未知错误"}"
        }
      } finally {
        accountAvatarProcessing = false
      }
    }
  }

  fun updateStoryPlayerAvatarFromUri(rawUri: String) {
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
    if (uri == null) {
      notice = "头像选择失败"
      return
    }

    viewModelScope.launch {
      storyPlayerAvatarProcessing = true
      try {
        runCatching {
          val key = if (worldId > 0L) "world_${worldId}_player" else "project_${selectedProjectId}_player"
          val saved = saveAvatarFiles(uri, key, projectScoped = true)
          userAvatarPath = saved.foregroundPath
          userAvatarBgPath = saved.backgroundPath
          notice = "头像已更新"
        }.onFailure {
          notice = "头像更新失败: ${it.message ?: "未知错误"}"
        }
      } finally {
        storyPlayerAvatarProcessing = false
      }
    }
  }

  fun importRoleAvatar(rawUri: String, storageKey: String, onSaved: (String, String) -> Unit) {
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
    if (uri == null) {
      notice = "头像选择失败"
      return
    }
    viewModelScope.launch {
      roleAvatarProcessing = true
      try {
        runCatching {
          val saved = saveAvatarFiles(uri, storageKey, projectScoped = true)
          onSaved(saved.foregroundPath, saved.backgroundPath)
          notice = "头像已更新"
        }.onFailure {
          notice = "头像更新失败: ${it.message ?: "未知错误"}"
        }
      } finally {
        roleAvatarProcessing = false
      }
    }
  }

  fun generateUserAvatar(prompt: String, referenceUris: List<String> = emptyList()) {
    generateAndStoreImage(
      type = "role",
      prompt = prompt,
      name = playerName.ifBlank { userName.ifBlank { "用户" } },
      referenceUris = referenceUris,
      aspectRatio = "1:1",
      storageKey = if (worldId > 0L) "world_${worldId}_player" else "project_${selectedProjectId}_player",
      wide = false,
    ) { fg, bg ->
      userAvatarPath = fg
      userAvatarBgPath = bg
    }
  }

  fun generateAccountAvatar(prompt: String, referenceUris: List<String> = emptyList()) {
    if (userId <= 0L) {
      notice = "请先登录后再设置头像"
      return
    }
    generateAndStoreImage(
      type = "role",
      prompt = prompt,
      name = userName.ifBlank { "用户" },
      referenceUris = referenceUris,
      aspectRatio = "1:1",
      storageKey = "user_${userId}",
      wide = false,
    ) { fg, bg ->
      accountAvatarPath = fg
      accountAvatarBgPath = bg
      viewModelScope.launch {
        runCatching {
          val storedAvatarPath = normalizeStoredMediaPath(accountAvatarPath)
          val storedAvatarBgPath = normalizeStoredMediaPath(accountAvatarBgPath)
          settingsStore.setAvatarPath(userId, storedAvatarPath)
          settingsStore.setAvatarBgPath(userId, storedAvatarBgPath)
          repository.saveUser(
            avatarPath = storedAvatarPath,
            avatarBgPath = storedAvatarBgPath,
          )
        }.onFailure {
          notice = "账号头像同步失败: ${it.message ?: "未知错误"}"
        }
      }
    }
  }

  fun generateRoleAvatar(prompt: String, referenceUris: List<String> = emptyList(), storageKey: String, roleName: String, onSaved: (String, String) -> Unit) {
    generateAndStoreImage(
      type = "role",
      prompt = prompt,
      name = roleName,
      referenceUris = referenceUris,
      aspectRatio = "1:1",
      storageKey = storageKey,
      wide = false,
      onSaved = onSaved,
    )
  }

  fun importWorldCover(rawUri: String) {
    val key = "world_${selectedProjectId.takeIf { it > 0 } ?: "draft"}_cover"
    importSceneImage(rawUri, key) { fg, bg ->
      worldCoverPath = fg
      worldCoverBgPath = bg
    }
  }

  fun generateWorldCover(prompt: String, referenceUris: List<String> = emptyList()) {
    val key = "world_${selectedProjectId.takeIf { it > 0 } ?: "draft"}_cover"
    generateAndStoreImage(
      type = "scene",
      prompt = prompt,
      name = worldName.ifBlank { "故事封面" },
      referenceUris = referenceUris,
      aspectRatio = "16:9",
      storageKey = key,
      wide = true,
    ) { fg, bg ->
      worldCoverPath = fg
      worldCoverBgPath = bg
    }
  }

  fun importChapterBackground(rawUri: String) {
    val chapterKey = selectedChapterId?.takeIf { it > 0L }?.toString()
      ?: "draft_${currentChapterSort()}"
    val key = "world_${selectedProjectId.takeIf { it > 0 } ?: "draft"}_chapter_$chapterKey"
    importSceneImage(rawUri, key) { fg, _ ->
      chapterBackground = fg
    }
  }

  fun importChapterMusic(rawUri: String) {
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
    if (uri == null) {
      notice = "背景音乐选择失败"
      return
    }
    viewModelScope.launch {
      runCatching {
        if (selectedProjectId <= 0L) error("请先选择项目后再上传背景音乐")
        val (base64Data, fileName) = audioUriToBase64(uri)
        val uploaded = repository.uploadVoiceAudio(selectedProjectId, base64Data, fileName)
        chapterMusic = uploaded.filePath.ifBlank { uploaded.url }
        notice = "背景音乐已上传"
      }.onFailure {
        notice = "背景音乐上传失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun generateChapterBackground(prompt: String, referenceUris: List<String> = emptyList()) {
    val chapterKey = selectedChapterId?.takeIf { it > 0L }?.toString()
      ?: "draft_${currentChapterSort()}"
    val key = "world_${selectedProjectId.takeIf { it > 0 } ?: "draft"}_chapter_$chapterKey"
    generateAndStoreImage(
      type = "scene",
      prompt = prompt,
      name = chapterTitle.ifBlank { "章节背景" },
      referenceUris = referenceUris,
      aspectRatio = "16:9",
      storageKey = key,
      wide = true,
    ) { fg, _ ->
      chapterBackground = fg
    }
  }

  private data class SavedImagePaths(
    val foregroundPath: String,
    val backgroundPath: String,
  )

  private data class BinaryImageSource(
    val bytes: ByteArray,
    val mime: String,
  )

  private fun importSceneImage(rawUri: String, storageKey: String, onSaved: (String, String) -> Unit) {
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
    if (uri == null) {
      notice = "图片选择失败"
      return
    }
    viewModelScope.launch {
      runCatching {
        val source = decodeBitmapFromUri(uri)
        val saved = uploadBitmapPair(
          source = source,
          storageKey = storageKey,
          fgWidth = coverStdWidth,
          fgHeight = coverStdHeight,
          bgWidth = coverBgWidth,
          bgHeight = coverBgHeight,
          type = "scene",
          projectScoped = true,
        )
        onSaved(saved.foregroundPath, saved.backgroundPath)
        notice = "图片已更新"
      }.onFailure {
        notice = "图片更新失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  private fun generateAndStoreImage(
    type: String,
    prompt: String,
    name: String,
    referenceUris: List<String>,
    aspectRatio: String,
    storageKey: String,
    wide: Boolean,
    onSaved: (String, String) -> Unit,
  ) {
    if (selectedProjectId <= 0L) {
      notice = "请先选择项目后再生成图片"
      return
    }
    val safePrompt = prompt.trim()
    if (safePrompt.isBlank()) {
      notice = "请输入生图提示词"
      return
    }
    val normalizedRefs = referenceUris.map { it.trim() }.filter { it.isNotBlank() }
    aiGenerating = true
    notice = if (normalizedRefs.isEmpty()) "AI 文生图生成中..." else "AI 图生图生成中..."
    viewModelScope.launch {
      runCatching {
        val base64List = normalizedRefs.map { uriToBase64(Uri.parse(it)) }
        val result = repository.generateImage(
          projectId = selectedProjectId,
          type = type,
          prompt = safePrompt,
          name = name,
          base64 = base64List.firstOrNull(),
          base64List = base64List,
          aspectRatio = aspectRatio,
        )
        val remotePath = result.filePath.ifBlank { result.path }.ifBlank { error("生成成功但未返回图片地址") }
        val resolvedRemotePath = resolveMediaPath(remotePath)
        val saved = if (wide) {
          val source = decodeBitmapFromUrl(resolvedRemotePath)
          uploadBitmapPair(source, storageKey, coverStdWidth, coverStdHeight, coverBgWidth, coverBgHeight, "scene", true)
        } else {
          val imageSource = loadRemoteImageSource(resolvedRemotePath)
          separateAvatarWithImageModel(imageSource, storageKey, true)
        }
        onSaved(saved.foregroundPath, saved.backgroundPath)
        notice = "AI 图片已生成并应用"
      }.onFailure {
        notice = "AI 生图失败: ${it.message ?: "未知错误"}"
      }
      aiGenerating = false
    }
  }

  private suspend fun saveAvatarFiles(uri: Uri, storageKey: String, projectScoped: Boolean): SavedImagePaths {
    val imageSource = readImageSourceFromUri(uri)
    return separateAvatarWithImageModel(imageSource, storageKey, projectScoped)
  }

  private suspend fun separateAvatarWithImageModel(
    source: BinaryImageSource,
    storageKey: String,
    projectScoped: Boolean,
  ): SavedImagePaths {
    val safeKey = storageKey.replace(Regex("[^A-Za-z0-9._-]"), "_")
    val result = repository.separateRoleAvatar(
      projectId = if (projectScoped) selectedProjectId.takeIf { it > 0L } ?: error("请先选择项目后再上传图片") else null,
      base64Data = bytesToBase64Payload(source.bytes, source.mime),
      fileName = "${safeKey}.${imageMimeToExtension(source.mime)}",
      name = safeKey,
    )
    val foregroundPath = resolveMediaPath(result.foregroundFilePath.ifBlank { result.foregroundPath })
    val backgroundPath = resolveMediaPath(result.backgroundFilePath.ifBlank { result.backgroundPath })
    if (foregroundPath.isBlank() || backgroundPath.isBlank()) {
      error("图像模型分离失败，未返回主体或背景图片")
    }
    return SavedImagePaths(
      foregroundPath = foregroundPath,
      backgroundPath = backgroundPath,
    )
  }

  private suspend fun uploadAnimatedAvatarPair(
    source: BinaryImageSource,
    storageKey: String,
    projectScoped: Boolean,
  ): SavedImagePaths {
    val backgroundSource = decodeBitmapFromBytes(source.bytes)
    val background = centerCropBitmap(backgroundSource, avatarBgSize, avatarBgSize)
    backgroundSource.recycle()

    val safeKey = storageKey.replace(Regex("[^A-Za-z0-9._-]"), "_")
    val version = System.currentTimeMillis()
    val targetProjectId = if (projectScoped) selectedProjectId.takeIf { it > 0L } ?: error("请先选择项目后再上传图片") else null
    return try {
      val fgResult = repository.uploadImage(
        projectId = targetProjectId,
        type = "role",
        base64Data = bytesToBase64Payload(source.bytes, source.mime),
        fileName = "${safeKey}_${version}_fg.${imageMimeToExtension(source.mime)}",
      )
      val bgResult = repository.uploadImage(
        projectId = targetProjectId,
        type = "role",
        base64Data = bitmapToBase64Payload(background),
        fileName = "${safeKey}_${version}_bg.png",
      )
      SavedImagePaths(
        foregroundPath = resolveMediaPath(fgResult.filePath.ifBlank { fgResult.path }),
        backgroundPath = resolveMediaPath(bgResult.filePath.ifBlank { bgResult.path }),
      )
    } finally {
      background.recycle()
    }
  }

  private suspend fun uploadBitmapPair(
    source: Bitmap,
    storageKey: String,
    fgWidth: Int,
    fgHeight: Int,
    bgWidth: Int,
    bgHeight: Int,
    type: String,
    projectScoped: Boolean,
  ): SavedImagePaths {
    val foreground = centerCropBitmap(source, fgWidth, fgHeight)
    val background = centerCropBitmap(source, bgWidth, bgHeight)
    source.recycle()

    val safeKey = storageKey.replace(Regex("[^A-Za-z0-9._-]"), "_")
    val version = System.currentTimeMillis()
    val targetProjectId = if (projectScoped) selectedProjectId.takeIf { it > 0L } ?: error("请先选择项目后再上传图片") else null
    return try {
      val fgResult = repository.uploadImage(
        projectId = targetProjectId,
        type = type,
        base64Data = bitmapToBase64Payload(foreground),
        fileName = "${safeKey}_${version}_fg.png",
      )
      val bgResult = repository.uploadImage(
        projectId = targetProjectId,
        type = type,
        base64Data = bitmapToBase64Payload(background),
        fileName = "${safeKey}_${version}_bg.png",
      )
      SavedImagePaths(
        foregroundPath = resolveMediaPath(fgResult.filePath.ifBlank { fgResult.path }),
        backgroundPath = resolveMediaPath(bgResult.filePath.ifBlank { bgResult.path }),
      )
    } finally {
      foreground.recycle()
      background.recycle()
    }
  }

  private fun decodeBitmapFromUri(uri: Uri): Bitmap {
    val app = getApplication<Application>()
    val resolver = app.contentResolver
    return resolver.openInputStream(uri).use { input ->
      if (input == null) error("无法读取所选图片")
      BitmapFactory.decodeStream(input) ?: error("图片解码失败")
    }
  }

  private fun decodeBitmapFromBytes(bytes: ByteArray): Bitmap {
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: error("图片解码失败")
  }

  private fun decodeBitmapFromUrl(url: String): Bitmap {
    return URL(url).openStream().use { input ->
      BitmapFactory.decodeStream(input) ?: error("图片解码失败")
    }
  }

  private fun bitmapToBase64Payload(bitmap: Bitmap): String {
    val output = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    val bytes = output.toByteArray()
    return "data:image/png;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
  }

  private fun fileBytesToBase64Payload(file: File): String {
    val ext = file.extension.trim().lowercase()
    val mime = when (ext) {
      "jpg", "jpeg" -> "image/jpeg"
      "webp" -> "image/webp"
      "gif" -> "image/gif"
      else -> "image/png"
    }
    return bytesToBase64Payload(file.readBytes(), mime)
  }

  private fun bytesToBase64Payload(bytes: ByteArray, mime: String): String {
    return "data:$mime;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
  }

  private fun imageMimeToExtension(mime: String): String {
    return when (mime.trim().lowercase()) {
      "image/jpeg" -> "jpg"
      "image/webp" -> "webp"
      "image/gif" -> "gif"
      else -> "png"
    }
  }

  private fun normalizeImageMime(displayName: String, rawMime: String): String {
    val mime = rawMime.trim().lowercase()
    if (mime.startsWith("image/")) {
      return when (mime) {
        "image/jpg" -> "image/jpeg"
        "image/x-png" -> "image/png"
        else -> mime
      }
    }
    return when (displayName.substringAfterLast('.', "").trim().lowercase()) {
      "jpg", "jpeg" -> "image/jpeg"
      "webp" -> "image/webp"
      "gif" -> "image/gif"
      else -> "image/png"
    }
  }

  private fun readImageSourceFromUri(uri: Uri): BinaryImageSource {
    val app = getApplication<Application>()
    val resolver = app.contentResolver
    val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (nameIdx >= 0 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
    }?.trim().orEmpty()
    val mime = normalizeImageMime(displayName, resolver.getType(uri).orEmpty())
    val bytes = resolver.openInputStream(uri).use { input ->
      if (input == null) error("无法读取所选图片")
      input.readBytes()
    }
    return BinaryImageSource(bytes = bytes, mime = mime)
  }

  private fun loadRemoteImageSource(url: String): BinaryImageSource {
    val connection = URL(url).openConnection().apply {
      connectTimeout = 10000
      readTimeout = 10000
    }
    val displayName = url.substringAfterLast('/').substringBefore('?').substringBefore('#').trim()
    val mime = normalizeImageMime(displayName, connection.contentType.orEmpty())
    val bytes = connection.getInputStream().use { input -> input.readBytes() }
    return BinaryImageSource(bytes = bytes, mime = mime)
  }

  private fun uriToBase64(uri: Uri): String {
    val app = getApplication<Application>()
    val resolver = app.contentResolver
    val bytes = resolver.openInputStream(uri).use { input ->
      if (input == null) error("无法读取参考图")
      input.readBytes()
    }
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
  }

  private fun audioUriToBase64(uri: Uri): Pair<String, String> {
    val app = getApplication<Application>()
    val resolver = app.contentResolver
    val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (nameIdx >= 0 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
    }?.trim().orEmpty()
    val mime = resolver.getType(uri).orEmpty().trim()
    val bytes = resolver.openInputStream(uri).use { input ->
      if (input == null) error("无法读取参考音频")
      input.readBytes()
    }
    val ext = displayName.substringAfterLast('.', "").takeIf { it.isNotBlank() }
      ?: when (mime.lowercase()) {
        "audio/mpeg", "audio/mp3" -> "mp3"
        "audio/ogg" -> "ogg"
        "audio/webm" -> "webm"
        "audio/wav", "audio/x-wav" -> "wav"
        else -> "wav"
      }
    val fileName = displayName.takeIf { it.isNotBlank() } ?: "voice_${System.currentTimeMillis()}.$ext"
    return Base64.encodeToString(bytes, Base64.NO_WRAP) to fileName
  }

  private fun centerCropBitmap(source: Bitmap, width: Int, height: Int): Bitmap {
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val scale = maxOf(width.toFloat() / source.width.toFloat(), height.toFloat() / source.height.toFloat())
    val scaledW = source.width * scale
    val scaledH = source.height * scale
    val dx = (width - scaledW) / 2f
    val dy = (height - scaledH) / 2f
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(source, null, android.graphics.RectF(dx, dy, dx + scaledW, dy + scaledH), paint)
    return output
  }

  private fun worldsForRecommendation(): List<WorldItem> {
    return worldsForSelectedProject().filter { (it.chapterCount ?: 0) > 0 && isWorldPublished(it) }
  }

  fun refreshRecommendation() {
    val pool = worldsForRecommendation()
    homeRecommendWorldId = if (pool.isEmpty()) null else pool.random().id
  }

  fun onProjectChanged(projectId: Long) {
    if (projectId <= 0 || projectId == selectedProjectId) return
    selectedProjectId = projectId
    viewModelScope.launch {
      loadWorlds()
      beginNewStoryDraft()
      loadSessions()
      refreshRecommendation()
    }
  }

  fun selectedProjectName(): String {
    return projects.firstOrNull { it.id == selectedProjectId }?.name?.ifBlank { "未命名项目" } ?: "未选择项目"
  }

  fun reloadAll() {
    viewModelScope.launch {
      if (token.isBlank()) {
        resetRuntimeData()
        notice = "请先登录账号"
        activeTab = "设置"
        return@launch
      }
      loading = true
      runCatching {
        val projectRows = repository.getProjects()
        projects.clear()
        projects.addAll(projectRows)
        if (projects.isNotEmpty() && selectedProjectId <= 0) {
          selectedProjectId = projects.first().id
        }
        loadUser()
        loadWorlds()
        if (selectedProjectId > 0) {
          beginNewStoryDraft()
          loadSessions()
          refreshRecommendation()
        }
      }.onFailure {
        resetRuntimeData()
        notice = if ((it.message ?: "").contains("401")) "登录已失效，请重新登录" else "加载失败: ${it.message ?: "未知错误"}"
        activeTab = "设置"
      }
      loading = false
    }
  }

  fun worldsForSelectedProject(): List<WorldItem> {
    if (selectedProjectId <= 0) return worlds.toList()
    return worlds.filter { it.projectId == selectedProjectId }
  }

  fun recommendedWorld(): WorldItem? {
    val pool = worldsForRecommendation()
    if (pool.isEmpty()) return null
    val picked = pool.firstOrNull { it.id == homeRecommendWorldId }
    return picked ?: pool.first().also { homeRecommendWorldId = it.id }
  }

  fun filteredHallWorlds(): List<WorldItem> {
    val key = hallKeyword.trim().lowercase()
    return worldsForSelectedProject().filter { item ->
      if (!isWorldPublished(item)) return@filter false
      val matchedKeyword = key.isBlank() || item.name.lowercase().contains(key) || item.intro.lowercase().contains(key)
      if (!matchedKeyword) return@filter false
      when (hallCategory) {
        "hasChapter" -> (item.chapterCount ?: 0) > 0
        "noChapter" -> (item.sessionCount ?: 0) > 0
        else -> true
      }
    }.sortedWith(
      compareByDescending<WorldItem> { it.sessionCount ?: 0 }
        .thenByDescending { it.chapterCount ?: 0 }
        .thenBy { it.name },
    )
  }

  private fun safeText(value: String?): String = value.orEmpty()

  private fun normalizeBaseUrlValue(value: String): String {
    return value.trim().trimEnd('/')
  }

  fun resolveMediaPath(value: String?): String {
    val raw = safeText(value).trim()
    if (raw.isBlank()) return ""
    if (raw.startsWith("/data/")) return raw
    if (raw.startsWith("content://", ignoreCase = true)) return raw
    if (raw.startsWith("file://", ignoreCase = true)) return raw
    if (raw.startsWith("data:", ignoreCase = true)) return raw
    if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) return raw
    val base = normalizeBaseUrlValue(baseUrl.ifBlank { settingsStore.baseUrl })
    if (base.isBlank()) return raw
    return if (raw.startsWith("/")) "$base$raw" else "$base/$raw"
  }

  private fun normalizeStoredMediaPath(value: String?): String {
    val raw = safeText(value).trim()
    if (raw.isBlank()) return ""
    val base = normalizeBaseUrlValue(baseUrl.ifBlank { settingsStore.baseUrl })
    if (base.isNotBlank() && raw.startsWith(base, ignoreCase = true)) {
      val suffix = raw.substring(base.length).trim()
      return if (suffix.startsWith("/")) suffix else "/$suffix"
    }
    return raw
  }

  private suspend fun ensureRemoteStoredMediaPath(
    value: String?,
    type: String,
    fileName: String,
    projectScoped: Boolean,
  ): String {
    val raw = safeText(value).trim()
    if (raw.isBlank()) return ""
    val normalized = normalizeStoredMediaPath(raw)
    if (!normalized.startsWith("/data/")) {
      return normalized
    }
    val file = File(normalized)
    if (!file.exists() || !file.isFile) {
      return normalized
    }
    val ext = file.extension.trim().ifBlank { "png" }
    val targetProjectId = if (projectScoped) selectedProjectId.takeIf { it > 0L } ?: error("请先选择项目后再上传图片") else null
    val uploaded = repository.uploadImage(
      projectId = targetProjectId,
      type = type,
      base64Data = fileBytesToBase64Payload(file),
      fileName = "$fileName.$ext",
    )
    return uploaded.filePath.ifBlank { uploaded.path }
  }

  private fun resolveRoleMedia(role: StoryRole): StoryRole {
    return role.copy(
      avatarPath = resolveMediaPath(role.avatarPath),
      avatarBgPath = resolveMediaPath(role.avatarBgPath),
    )
  }

  fun isWorldPublished(world: WorldItem): Boolean {
    val topLevelStatus = safeText(world.publishStatus).trim().lowercase()
    if (topLevelStatus.isNotBlank()) {
      return topLevelStatus == "published"
    }
    return safeText(world.settings?.publishStatus).trim().lowercase() == "published"
  }

  fun worldCoverPath(world: WorldItem?): String {
    if (world == null) return ""
    return resolveMediaPath(
      safeText(world.coverPath)
      .ifBlank { safeText(world.settings?.coverPath) }
      .ifBlank { safeText(world.coverBgPath) }
      .ifBlank { safeText(world.settings?.coverBgPath) },
    )
  }

  fun playChapterBackgroundPath(): String {
    if (debugMode) {
      val chapter = playCurrentChapter() ?: return ""
      val useEditorState = (selectedChapterId != null && chapter.id == selectedChapterId) || (selectedChapterId == null && chapter.id < 0L)
      if (useEditorState && chapterBackground.isNotBlank()) return resolveMediaPath(chapterBackground)
      return resolveMediaPath(
        normalizeScalarEditorText(chapterExtraFor(chapter.id.takeIf { it > 0L }, chapter.sort)?.background).ifBlank {
          normalizeScalarEditorText(chapter.backgroundPath)
        },
      )
    }
    val chapter = sessionDetail?.chapter ?: return ""
    val extra = sessionDetail?.world?.settings?.chapterExtras?.firstOrNull {
      (chapter.id > 0L && it.chapterId == chapter.id) || (chapter.sort > 0 && it.sort == chapter.sort)
    }
    return resolveMediaPath(
      normalizeScalarEditorText(extra?.background).ifBlank {
        normalizeScalarEditorText(chapter.backgroundPath)
      },
    )
  }

  fun mentionRoleNames(): List<String> {
    return listOf(playerName, narratorName) + npcRoles.map { it.name }
  }

  fun messageUiKey(message: MessageItem, sessionId: String = currentSessionId): String {
    return "${sessionId.trim()}_${message.id}_${message.createTime}"
  }

  fun isRuntimeRetryMessage(message: MessageItem): Boolean {
    return message.eventType == RUNTIME_RETRY_EVENT
  }

  fun isStreamingRuntimeMessage(message: MessageItem): Boolean {
    val meta = message.meta?.takeIf { it.isJsonObject }?.asJsonObject ?: return false
    return meta.get("kind")?.asString == "runtime_stream" && (meta.get("streaming")?.asBoolean == true)
  }

  fun streamingSentenceTexts(message: MessageItem): List<String> {
    val meta = message.meta?.takeIf { it.isJsonObject }?.asJsonObject ?: return emptyList()
    val sentences = meta.getAsJsonArray("sentences") ?: return emptyList()
    return sentences.mapNotNull { item ->
      item?.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotBlank() }
    }
  }

  fun reactionForMessage(message: MessageItem, sessionId: String = currentSessionId): String {
    val key = messageUiKey(message, sessionId)
    val cached = messageReactions[key]
    if (cached != null) return cached
    val loaded = settingsStore.getMessageReaction(sessionId, message.id, message.createTime)
    if (loaded.isNotBlank()) {
      messageReactions[key] = loaded
    }
    return loaded
  }

  fun setReactionForMessage(message: MessageItem, reaction: String, sessionId: String = currentSessionId) {
    val normalized = reaction.trim().lowercase()
    val current = reactionForMessage(message, sessionId)
    val next = if (current == normalized) "" else normalized
    val key = messageUiKey(message, sessionId)
    if (next.isBlank()) {
      messageReactions.remove(key)
    } else {
      messageReactions[key] = next
    }
    settingsStore.setMessageReaction(sessionId, message.id, message.createTime, next)
    notice = when (next) {
      "like" -> "已点赞该条对话"
      "dislike" -> "已点踩该条对话"
      else -> "已取消评价"
    }
  }

  fun applyRewritePrompt(message: MessageItem) {
    val speaker = displayNameForMessage(message)
    sendText = "请将“${message.content.trim()}”改写成更自然、符合${speaker}当前处境的一句话。"
    notice = "已把改写指令填入输入框"
  }

  fun avatarPathForMessage(message: MessageItem): String {
    val roleName = message.role.trim()
    if (message.roleType == "player") {
      return resolveMediaPath(playStoryRoles().firstOrNull { it.roleType == "player" }?.avatarPath)
    }
    val matched = playStoryRoles().firstOrNull { role ->
      (role.roleType == message.roleType && roleName.isNotBlank() && role.name == roleName) ||
        (roleName.isNotBlank() && role.name == roleName)
    } ?: playStoryRoles().firstOrNull { it.roleType == message.roleType }
    return resolveMediaPath(matched?.avatarPath)
  }

  fun avatarBgPathForMessage(message: MessageItem): String {
    val roleName = message.role.trim()
    if (message.roleType == "player") {
      return resolveMediaPath(playStoryRoles().firstOrNull { it.roleType == "player" }?.avatarBgPath)
    }
    val matched = playStoryRoles().firstOrNull { role ->
      (role.roleType == message.roleType && roleName.isNotBlank() && role.name == roleName) ||
        (roleName.isNotBlank() && role.name == roleName)
    } ?: playStoryRoles().firstOrNull { it.roleType == message.roleType }
    return resolveMediaPath(matched?.avatarBgPath)
  }

  fun displayNameForMessage(message: MessageItem): String {
    if (message.role.isNotBlank()) return message.role
    return when (message.roleType) {
      "system" -> "系统"
      "player" -> playerName.ifBlank { "用户" }
      "narrator" -> narratorName.ifBlank { "旁白" }
      else -> "角色"
    }
  }

  private fun conversationMessages(source: List<MessageItem> = messages): List<MessageItem> {
    return source.filterNot(::isRuntimeRetryMessage)
  }

  private fun updateMessageById(messageId: Long, updater: (MessageItem) -> MessageItem?) {
    val index = messages.indexOfFirst { it.id == messageId }
    if (index < 0) return
    val next = updater(messages[index])
    if (next == null) {
      messages.removeAt(index)
      return
    }
    messages[index] = next
  }

  private fun createStreamingMessage(plan: DebugNarrativePlan): MessageItem {
    val now = System.currentTimeMillis()
    return MessageItem(
      id = now,
      role = plan.role.ifBlank { "旁白" },
      roleType = plan.roleType.ifBlank { "narrator" },
      eventType = plan.eventType.ifBlank { "on_streaming_reply" },
      content = "",
      createTime = now,
      meta = JsonObject().apply {
        addProperty("kind", "runtime_stream")
        addProperty("streaming", true)
        add("sentences", JsonArray())
      },
    )
  }

  private fun clearRuntimeRetryMessage() {
    val nextMessages = conversationMessages()
    if (nextMessages.size == messages.size) return
    messages.clear()
    messages.addAll(nextMessages)
  }

  private fun clearRuntimeRetryState() {
    runtimeRetryTask = null
    runtimeRetryMessageText = ""
    clearRuntimeRetryMessage()
  }

  private fun showRuntimeRetryMessage(message: String, task: suspend () -> Unit) {
    val now = System.currentTimeMillis()
    runtimeRetryTask = task
    runtimeRetryMessageText = message
    notice = ""
    val nextMessages = conversationMessages(messages.toList())
    messages.clear()
    messages.addAll(nextMessages)
    messages.add(
      MessageItem(
        id = -now,
        role = "系统",
        roleType = "system",
        eventType = RUNTIME_RETRY_EVENT,
        content = message,
        createTime = now,
      ),
    )
  }

  fun retryRuntimeFailure() {
    val task = runtimeRetryTask ?: return
    val retryMessage = runtimeRetryMessageText
    if (runtimeRetrying) return
    runtimeRetrying = true
    clearRuntimeRetryState()
    viewModelScope.launch {
      try {
        task()
      } catch (err: Throwable) {
        val nextMessage = retryMessage.ifBlank {
          "重试失败: ${err.message ?: "未知错误"}"
        }
        showRuntimeRetryMessage(nextMessage) {
          task()
        }
      } finally {
        runtimeRetrying = false
      }
    }
  }

  private fun createPlayableVoiceBinding(
    label: String,
    configId: Long?,
    presetId: String,
    mode: String,
    referenceAudioPath: String,
    referenceAudioName: String,
    referenceText: String,
    promptText: String,
    mixVoices: List<VoiceMixItem>,
  ): VoiceBindingDraft? {
    val normalizedMode = mode.ifBlank { "text" }
    val normalized = VoiceBindingDraft(
      label = label.trim(),
      configId = configId,
      presetId = presetId.trim(),
      mode = normalizedMode,
      referenceAudioPath = referenceAudioPath.trim(),
      referenceAudioName = referenceAudioName.trim(),
      referenceText = referenceText.trim(),
      promptText = promptText.trim(),
      mixVoices = normalizedMixVoices(mixVoices),
    )
    return when (normalizedMode) {
      "clone" -> normalized.takeIf { it.referenceAudioPath.isNotBlank() }
      "mix" -> normalized.takeIf { it.mixVoices.any { item -> item.voiceId.isNotBlank() } }
      "prompt_voice" -> normalized.takeIf { it.promptText.isNotBlank() }
      else -> normalized.takeIf { it.presetId.isNotBlank() }
    }
  }

  fun playVoiceBindingForMessage(message: MessageItem): VoiceBindingDraft? {
    if (message.roleType == "player") return null
    val world = sessionDetail?.world
    if (message.roleType == "narrator") {
      val settings = world?.settings
      val narratorRole = world?.narratorRole
      val debugConfigId = if (debugMode && world == null) runtimeStoryVoiceConfigId() else null
      val configId = settings?.narratorVoiceConfigId ?: narratorRole?.voiceConfigId ?: debugConfigId
      val mode = settings?.narratorVoiceMode ?: narratorRole?.voiceMode.orEmpty().ifBlank { narratorVoiceMode.ifBlank { "text" } }
      val presetId = settings?.narratorVoicePresetId ?: narratorRole?.voicePresetId.orEmpty().ifBlank { narratorVoicePresetId }
      return createPlayableVoiceBinding(
        label = settings?.narratorVoice ?: narratorRole?.voice ?: narratorVoice.ifBlank { narratorName.ifBlank { "旁白" } },
        configId = configId,
        presetId = if (presetId.isBlank() && (mode.ifBlank { "text" } == "text")) "story_narrator" else presetId,
        mode = mode,
        referenceAudioPath = settings?.narratorVoiceReferenceAudioPath ?: narratorRole?.voiceReferenceAudioPath.orEmpty().ifBlank { narratorVoiceReferenceAudioPath },
        referenceAudioName = settings?.narratorVoiceReferenceAudioName ?: narratorRole?.voiceReferenceAudioName.orEmpty().ifBlank { narratorVoiceReferenceAudioName },
        referenceText = settings?.narratorVoiceReferenceText ?: narratorRole?.voiceReferenceText.orEmpty().ifBlank { narratorVoiceReferenceText },
        promptText = settings?.narratorVoicePromptText ?: narratorRole?.voicePromptText.orEmpty().ifBlank { narratorVoicePromptText },
        mixVoices = settings?.narratorVoiceMixVoices ?: narratorRole?.voiceMixVoices ?: narratorVoiceMixVoices,
      )
    }
    val roleName = message.role.trim()
    val matchedRole = playStoryRoles().firstOrNull { role ->
      (roleName.isNotBlank() && (role.name == roleName || role.id == roleName)) || (roleName.isBlank() && role.roleType == message.roleType)
    } ?: playStoryRoles().firstOrNull { it.roleType == message.roleType }
    val configId = matchedRole?.voiceConfigId ?: if (debugMode && world == null) runtimeStoryVoiceConfigId() else null
    val mode = matchedRole?.voiceMode.orEmpty().ifBlank { "text" }
    val presetId = matchedRole?.voicePresetId.orEmpty().ifBlank {
      if (mode == "text" && matchedRole != null) {
        inferFallbackVoicePreset(matchedRole.roleType, matchedRole.name, matchedRole.description)
      } else {
        ""
      }
    }
    return createPlayableVoiceBinding(
      label = matchedRole?.voice ?: matchedRole?.name.orEmpty(),
      configId = configId,
      presetId = presetId,
      mode = mode,
      referenceAudioPath = matchedRole?.voiceReferenceAudioPath.orEmpty(),
      referenceAudioName = matchedRole?.voiceReferenceAudioName.orEmpty(),
      referenceText = matchedRole?.voiceReferenceText.orEmpty(),
      promptText = matchedRole?.voicePromptText.orEmpty(),
      mixVoices = matchedRole?.voiceMixVoices ?: emptyList(),
    )
  }

  fun playNarratorVoiceBinding(): VoiceBindingDraft? {
    return playVoiceBindingForMessage(
      MessageItem(
        id = -1L,
        role = narratorName.ifBlank { "旁白" },
        roleType = "narrator",
        content = "",
        eventType = "on_preview",
        createTime = System.currentTimeMillis(),
      ),
    )
  }

  fun buildAiTipOptions(): List<String> {
    val chapter = playCurrentChapter()
    val roles = playStoryRoles()
    val narrator = roles.firstOrNull { it.roleType == "narrator" }?.name?.ifBlank { narratorName } ?: narratorName.ifBlank { "旁白" }
    val npc = roles.firstOrNull { it.roleType == "npc" && it.name.isNotBlank() }
    val latestNpc = messages.asReversed().firstOrNull { it.roleType != "player" }?.role?.ifBlank { narrator } ?: narrator
    val conditionText = playChapterConditionText()
      .takeIf { it.isNotBlank() && it != "无" }
      ?.replace("\n", " ")
      ?.take(42)
    val chapterName = playChapterTitle().ifBlank { chapter?.title?.ifBlank { "当前章节" } ?: "当前章节" }
    val options = mutableListOf<String>()
    options += "请${latestNpc}总结一下《${chapterName}》当前局势，并给我一个下一步行动。"
    if (npc != null) {
      options += "我想和${npc.name}继续互动，请直接推进剧情并制造新的冲突。"
    } else {
      options += "请${narrator}直接推进主线，给我一个立刻可执行的动作。"
    }
    if (!conditionText.isNullOrBlank()) {
      options += "当前章节目标是：${conditionText}。请给我一条最稳妥的完成方案。"
    } else {
      val contentHint = chapter?.content?.replace("\n", " ")?.take(40)
      options += if (contentHint.isNullOrBlank()) {
        "保持当前节奏推进故事，并补一个新的选择分支给我。"
      } else {
        "围绕“${contentHint}”继续推进，并给我一个明确选择。"
      }
    }
    return options.distinct().take(3)
  }

  private fun currentChapterSort(): Int {
    return chapters.firstOrNull { it.id == selectedChapterId }?.sort ?: (chapters.maxOfOrNull { it.sort } ?: 0) + 1
  }

  private fun hasCurrentChapterDraft(): Boolean {
    return chapterTitle.isNotBlank() ||
      chapterContent.isNotBlank() ||
      chapterEntryCondition.isNotBlank() ||
      chapterCondition.isNotBlank() ||
      chapterOpeningLine.isNotBlank() ||
      chapterBackground.isNotBlank() ||
      chapterMusic.isNotBlank()
  }

  private data class OpeningContentParts(
    val role: String,
    val line: String,
    val body: String,
  )

  private fun normalizeScalarEditorText(raw: Any?): String {
    if (raw == null) return ""
    val text = when (raw) {
      is JsonElement -> {
        if (raw.isJsonNull) {
          ""
        } else if (raw.isJsonPrimitive && raw.asJsonPrimitive.isString) {
          raw.asString
        } else {
          raw.toString()
        }
      }

      else -> raw.toString()
    }
    val trimmed = text.trim()
    return if (
      trimmed.isEmpty() ||
      trimmed == "null" ||
      trimmed == "undefined" ||
      trimmed == "\"\"" ||
      trimmed == "''" ||
      trimmed == "\"null\"" ||
      trimmed == "\"undefined\""
    ) {
      ""
    } else {
      text
    }
  }

  private fun normalizeConditionEditorText(raw: JsonElement?): String {
    if (raw == null || raw.isJsonNull) return ""
    if (raw.isJsonPrimitive) {
      val primitive = raw.asJsonPrimitive
      if (primitive.isString) return normalizeScalarEditorText(primitive.asString).trim()
    }
    extractSimpleConditionText(raw)?.let { return it }
    return raw.toString()
  }

  private fun normalizeConditionEditorText(raw: String): String {
    val text = normalizeScalarEditorText(raw).trim()
    if (text.isBlank()) return ""
    return runCatching {
      val parsed = JsonParser.parseString(text)
      normalizeConditionEditorText(parsed)
    }.getOrElse { text }
  }

  private fun extractSimpleConditionText(raw: JsonElement?): String? {
    if (raw == null || raw.isJsonNull || !raw.isJsonObject) return null
    val obj = raw.asJsonObject
    val allowedKeys = setOf("type", "op", "field", "left", "value", "right")
    if (obj.keySet().any { it !in allowedKeys }) return null
    val op = (obj.get("type") ?: obj.get("op"))?.let { normalizeScalarEditorText(it.toString()).trim().trim('"').lowercase() }
      ?: "contains"
    val field = (obj.get("field") ?: obj.get("left"))?.let { normalizeScalarEditorText(it.toString()).trim().trim('"').lowercase() }
      ?: "message"
    val valueElement = obj.get("value") ?: obj.get("right")
    val value = if (valueElement == null || valueElement.isJsonNull) "" else when {
      valueElement.isJsonPrimitive && valueElement.asJsonPrimitive.isString -> normalizeScalarEditorText(valueElement.asString).trim()
      else -> normalizeScalarEditorText(valueElement.toString()).trim().trim('"')
    }
    if (value.isBlank()) return null
    if (op !in setOf("contains", "equals", "eq")) return null
    if (field !in setOf("message", "message.content", "latest", "latest_message")) return null
    return value
  }

  private fun extractOpeningContentParts(content: String): OpeningContentParts? {
    val text = normalizeScalarEditorText(content)
    if (text.isBlank()) return null
    val match = Regex("^开场白(?:\\[(.+?)\\]|([^\\[\\]:：\\r\\n]+))\\s*[:：]\\s*([^\\r\\n]*)(?:\\r?\\n)*").find(text) ?: return null
    val role = (match.groupValues.getOrNull(1).orEmpty().ifBlank { match.groupValues.getOrNull(2).orEmpty() }).trim()
    val line = match.groupValues.getOrNull(3).orEmpty().trim()
    val body = text.removeRange(0, match.range.last + 1).replace(Regex("^[\\s\\r\\n]+"), "")
    if (role.isBlank() && line.isBlank()) return null
    return OpeningContentParts(role = role, line = line, body = body)
  }

  private fun stripLeadingOpeningBlocks(content: String): String {
    var text = normalizeScalarEditorText(content)
    if (text.isBlank()) return ""
    repeat(8) {
      val extracted = extractOpeningContentParts(text) ?: return@repeat
      text = extracted.body.replace(Regex("^[\\s\\r\\n]+"), "")
    }
    return text
  }

  private fun splitParagraphs(content: String): List<String> {
    return content
      .replace("\r\n", "\n")
      .split(Regex("\n\\s*\n+"))
      .map { it.trim() }
      .filter { it.isNotBlank() }
  }

  private fun normalizeChapterTitleLabel(title: String, sort: Int): String {
    val raw = normalizeScalarEditorText(title).trim()
    if (raw.isNotBlank() && !Regex("^章节\\s*\\d{10,}$").matches(raw)) {
      return raw
    }
    return if (sort > 0) "第 $sort 章" else raw
  }

  private fun normalizeOpeningEditorFields(openingRole: String, openingLine: String, content: String): Triple<String, String, String> {
    val role = normalizeScalarEditorText(openingRole).trim().ifBlank { "旁白" }
    var line = normalizeScalarEditorText(openingLine).trim()
    var body = normalizeScalarEditorText(content).trim()
    val openingParagraphs = splitParagraphs(line)
    if (openingParagraphs.size > 1) {
      line = openingParagraphs.first()
      val remainder = openingParagraphs.drop(1).joinToString("\n\n").trim()
      if (remainder.isNotBlank()) {
        val remainderParagraphs = splitParagraphs(remainder)
        val contentParagraphs = splitParagraphs(body)
        val alreadyPrefixed = remainderParagraphs.withIndex().all { (index, item) ->
          contentParagraphs.getOrNull(index) == item
        }
        if (!alreadyPrefixed) {
          body = listOf(remainder, body).filter { it.isNotBlank() }.joinToString("\n\n").trim()
        }
      }
    }
    return Triple(role, line, body)
  }

  private fun escapeRegExp(input: String): String {
    return Regex("""[.*+?^${'$'}()|\[\]\\]""").replace(input) { "\\${it.value}" }
  }

  private fun stripOpeningHeader(content: String, openingRole: String): String {
    val text = normalizeScalarEditorText(content).trimStart()
    if (text.isBlank()) return ""
    val role = normalizeScalarEditorText(openingRole).trim()
    val header = if (role.isNotBlank()) {
      Regex("^开场白(?:\\[${escapeRegExp(role)}\\]|${escapeRegExp(role)})?\\s*[:：]\\s*")
    } else {
      Regex("^开场白(?:\\[(.+?)\\]|([^\\[\\]:：\\r\\n]+))?\\s*[:：]\\s*")
    }
    return text.replaceFirst(header, "").replace(Regex("^[\\s\\r\\n]+"), "")
  }

  private fun stripLeadingOpeningParagraphs(content: String, openingLine: String): String {
    val openingParagraphs = splitParagraphs(openingLine)
    if (openingParagraphs.isEmpty()) return content.trim()
    val openingSet = openingParagraphs.toSet()
    val contentParagraphs = splitParagraphs(content).toMutableList()
    while (contentParagraphs.isNotEmpty() && openingSet.contains(contentParagraphs.first())) {
      contentParagraphs.removeAt(0)
    }
    return contentParagraphs.joinToString("\n\n").trim()
  }

  private fun stripLeadingOpeningArtifacts(content: String, openingRole: String, openingLine: String): String {
    var text = normalizeScalarEditorText(content)
    if (text.isBlank()) return ""
    val role = normalizeScalarEditorText(openingRole).trim()
    val line = normalizeScalarEditorText(openingLine).trim()
    val openingParagraphs = splitParagraphs(line).sortedByDescending { it.length }
    repeat(64) {
      val before = text
      text = stripOpeningHeader(text, role)
      val extracted = extractOpeningContentParts(text)
      if (extracted != null) {
        val roleMatches = role.isBlank() || extracted.role.isBlank() || extracted.role == role
        val lineMatches = line.isBlank() || extracted.line.isBlank() || line.startsWith(extracted.line) || extracted.line == line
        if (roleMatches && lineMatches) {
          text = extracted.body.replace(Regex("^[\\s\\r\\n]+"), "")
        }
      }
      if (line.isNotBlank() && text.startsWith(line)) {
        text = text.removePrefix(line).replace(Regex("^[\\s\\r\\n]+"), "")
      }
      val paragraphMatch = openingParagraphs.firstOrNull { it.isNotBlank() && text.startsWith(it) }
      if (paragraphMatch != null) {
        text = text.removePrefix(paragraphMatch).replace(Regex("^[\\s\\r\\n]+"), "")
      }
      if (text == before) return@repeat
    }
    if (line.isNotBlank()) {
      text = stripLeadingOpeningParagraphs(text, line)
    }
    return text.trim()
  }

  private fun stripOpeningPrefix(content: String, openingRole: String, openingLine: String): String {
    return stripLeadingOpeningArtifacts(content, openingRole, openingLine)
  }

  private fun buildPersistedChapterContent(): String {
    val role = chapterOpeningRole.ifBlank { narratorName.ifBlank { "旁白" } }.trim()
    val opening = chapterOpeningLine.trim()
    return stripLeadingOpeningArtifacts(chapterContent.trim(), role, opening)
  }

  private fun currentChapterDebugOpening(chapter: ChapterItem): Pair<String, String> {
    val useEditorState = (selectedChapterId != null && chapter.id == selectedChapterId) || (selectedChapterId == null && chapter.id < 0L)
    if (useEditorState) {
      val normalized = normalizeOpeningEditorFields(
        chapterOpeningRole.ifBlank { narratorName.ifBlank { "旁白" } },
        chapterOpeningLine,
        "",
      )
      return normalized.first to normalized.second
    }
    val extra = chapterExtraFor(chapter.id.takeIf { it > 0L }, chapter.sort)
    val role = normalizeScalarEditorText(extra?.openingRole).trim().ifBlank {
      normalizeScalarEditorText(chapter.openingRole).trim().ifBlank { narratorName.ifBlank { "旁白" } }
    }
    val line = normalizeScalarEditorText(extra?.openingLine).trim().ifBlank {
      normalizeScalarEditorText(chapter.openingText).trim()
    }
    val normalized = normalizeOpeningEditorFields(role, line, "")
    return normalized.first to normalized.second
  }

  private fun buildDebugChapterSummary(chapter: ChapterItem): String {
    val (openingRole, openingLine) = currentChapterDebugOpening(chapter)
    if (openingLine.isNotBlank()) {
      return openingLine
    }
    val chapterBody = stripLeadingOpeningArtifacts(chapter.content, openingRole, openingLine).trim()
    if (chapterBody.isNotBlank()) {
      return chapterBody
        .split(Regex("\\r?\\n+"))
        .map { it.trim() }
        .firstOrNull { it.isNotBlank() }
        ?.take(80)
        ?: chapterBody.take(80)
    }
    return "进入章节《${chapter.title.ifBlank { "当前章节" }}》"
  }

  private fun buildEditorChapterSnapshot(): ChapterItem? {
    if (!hasCurrentChapterDraft()) return null
    val sort = currentChapterSort()
    val title = normalizeChapterTitleLabel(chapterTitle, sort).ifBlank { "第 $sort 章" }
    return ChapterItem(
      id = selectedChapterId ?: -sort.toLong(),
      title = title,
      content = buildPersistedChapterContent(),
      entryCondition = parseChapterCondition(chapterEntryCondition),
      sort = sort,
      status = if (worldPublishStatus == "published") "published" else "draft",
      completionCondition = parseChapterCondition(chapterCondition),
      chapterKey = title,
      backgroundPath = chapterBackground,
      openingRole = chapterOpeningRole.ifBlank { narratorName.ifBlank { "旁白" } },
      openingText = chapterOpeningLine,
      bgmPath = chapterMusic,
      showCompletionCondition = chapterConditionVisible,
    )
  }

  fun selectChapter(chapterId: Long?) {
    selectedChapterId = chapterId
    val chapter = chapters.firstOrNull { it.id == chapterId }
    if (chapter == null) {
      chapterTitle = ""
      chapterContent = ""
      chapterEntryCondition = ""
      chapterCondition = ""
      chapterOpeningRole = narratorName.ifBlank { "旁白" }
      chapterOpeningLine = ""
      chapterBackground = ""
      chapterMusic = ""
      chapterConditionVisible = true
      primeStoryEditorPersistState()
      return
    }
    val extra = chapterExtraFor(chapter.id, chapter.sort)
    val extractedOpening = extractOpeningContentParts(chapter.content)
    val openingRole = normalizeScalarEditorText(extra?.openingRole).trim().ifBlank {
      normalizeScalarEditorText(chapter.openingRole).trim().ifBlank {
        extractedOpening?.role ?: narratorName.ifBlank { "旁白" }
      }
    }
    val openingLine = normalizeScalarEditorText(extra?.openingLine).trim().ifBlank {
      normalizeScalarEditorText(chapter.openingText).trim().ifBlank {
        extractedOpening?.line.orEmpty()
      }
    }
    val normalizedChapter = normalizeOpeningEditorFields(
      openingRole,
      openingLine,
      stripOpeningPrefix(chapter.content, openingRole, openingLine),
    )
    chapterTitle = normalizeChapterTitleLabel(chapter.title, chapter.sort)
    chapterContent = normalizedChapter.third
    chapterEntryCondition = normalizeConditionEditorText(chapter.entryCondition)
    chapterCondition = normalizeConditionEditorText(chapter.completionCondition)
    chapterOpeningRole = normalizedChapter.first
    chapterOpeningLine = normalizedChapter.second
    chapterBackground = resolveMediaPath(
      normalizeScalarEditorText(extra?.background).ifBlank {
        normalizeScalarEditorText(chapter.backgroundPath)
      },
    )
    chapterMusic = normalizeScalarEditorText(extra?.music).ifBlank {
      normalizeScalarEditorText(chapter.bgmPath)
    }
    chapterConditionVisible = extra?.conditionVisible ?: chapter.showCompletionCondition
    primeStoryEditorPersistState()
  }

  fun appendGlobalMention(roleName: String) {
    val trimmed = roleName.trim()
    if (trimmed.isEmpty()) return
    globalBackground = "$globalBackground${if (globalBackground.isBlank()) "" else " "}@$trimmed "
  }

  fun appendChapterMention(roleName: String) {
    val trimmed = roleName.trim()
    if (trimmed.isEmpty()) return
    chapterContent = "$chapterContent${if (chapterContent.isBlank()) "" else " "}@$trimmed "
  }

  fun addNpcRole() {
    npcRoles.add(
      StoryRole(
        id = "npc_${System.currentTimeMillis()}",
        roleType = "npc",
        name = "新角色",
        description = "",
        voice = "",
        sample = "",
      ),
    )
  }

  fun removeNpcRole(index: Int) {
    if (index in npcRoles.indices) {
      npcRoles.removeAt(index)
    }
  }

  fun setNpcRoleName(index: Int, value: String) {
    if (index in npcRoles.indices) {
      npcRoles[index] = npcRoles[index].copy(name = value)
    }
  }

  fun setNpcRoleAvatar(index: Int, avatarPath: String, avatarBgPath: String) {
    if (index in npcRoles.indices) {
      npcRoles[index] = npcRoles[index].copy(
        avatarPath = avatarPath.trim(),
        avatarBgPath = avatarBgPath.trim(),
      )
    }
  }

  fun setNpcRoleDescription(index: Int, value: String) {
    if (index in npcRoles.indices) {
      npcRoles[index] = npcRoles[index].copy(description = value)
    }
  }

  fun setPlayerVoiceBinding(label: String, configId: Long?, presetId: String) {
    setPlayerVoiceBinding(
      VoiceBindingDraft(
        label = label,
        configId = configId,
        presetId = presetId,
      ),
    )
  }

  fun setPlayerVoiceBinding(binding: VoiceBindingDraft) {
    playerVoice = binding.label.trim()
    playerVoicePresetId = binding.presetId.trim()
    playerVoiceMode = binding.mode.ifBlank { "text" }
    playerVoiceReferenceAudioPath = binding.referenceAudioPath.trim()
    playerVoiceReferenceAudioName = binding.referenceAudioName.trim()
    playerVoiceReferenceText = binding.referenceText.trim()
    playerVoicePromptText = binding.promptText.trim()
    playerVoiceMixVoices = normalizedMixVoices(binding.mixVoices)
  }

  fun setNarratorVoiceBinding(label: String, configId: Long?, presetId: String) {
    setNarratorVoiceBinding(
      VoiceBindingDraft(
        label = label,
        configId = configId,
        presetId = presetId,
      ),
    )
  }

  fun setNarratorVoiceBinding(binding: VoiceBindingDraft) {
    narratorVoice = binding.label.trim()
    narratorVoicePresetId = binding.presetId.trim()
    narratorVoiceMode = binding.mode.ifBlank { "text" }
    narratorVoiceReferenceAudioPath = binding.referenceAudioPath.trim()
    narratorVoiceReferenceAudioName = binding.referenceAudioName.trim()
    narratorVoiceReferenceText = binding.referenceText.trim()
    narratorVoicePromptText = binding.promptText.trim()
    narratorVoiceMixVoices = normalizedMixVoices(binding.mixVoices)
  }

  fun setNpcRoleVoice(index: Int, value: String, configId: Long? = null, presetId: String = "") {
    setNpcRoleVoice(
      index,
      VoiceBindingDraft(
        label = value,
        configId = configId,
        presetId = presetId,
      ),
    )
  }

  fun setNpcRoleVoice(index: Int, binding: VoiceBindingDraft) {
    if (index in npcRoles.indices) {
      npcRoles[index] = npcRoles[index].copy(
        voice = binding.label.trim(),
        voiceMode = binding.mode.ifBlank { "text" },
        voicePresetId = binding.presetId.trim(),
        voiceReferenceAudioPath = binding.referenceAudioPath.trim(),
        voiceReferenceAudioName = binding.referenceAudioName.trim(),
        voiceReferenceText = binding.referenceText.trim(),
        voicePromptText = binding.promptText.trim(),
        voiceMixVoices = normalizedMixVoices(binding.mixVoices),
      )
    }
  }

  fun setNpcRoleSample(index: Int, value: String) {
    if (index in npcRoles.indices) {
      npcRoles[index] = npcRoles[index].copy(sample = value)
    }
  }

  fun beginNewChapterDraft() {
    selectedChapterId = null
    chapterTitle = "第 ${chapters.size + 1} 章"
    chapterContent = ""
    chapterEntryCondition = ""
    chapterCondition = ""
    chapterOpeningRole = narratorName.ifBlank { "旁白" }
    chapterOpeningLine = ""
    chapterBackground = ""
    chapterMusic = ""
    chapterConditionVisible = true
    primeStoryEditorPersistState()
  }

  private fun chapterExtraFor(chapterId: Long?, sort: Int): ChapterExtra? {
    return chapterExtras.firstOrNull {
      (chapterId != null && chapterId > 0L && it.chapterId == chapterId) || (sort > 0 && it.sort == sort)
    }
  }

  private fun upsertChapterExtra(chapterId: Long?, sort: Int) {
    val next = ChapterExtra(
      chapterId = chapterId,
      sort = sort,
      openingRole = chapterOpeningRole.ifBlank { narratorName.ifBlank { "旁白" } },
      openingLine = chapterOpeningLine,
      background = normalizeStoredMediaPath(chapterBackground),
      music = chapterMusic,
      conditionVisible = chapterConditionVisible,
    )
    val idx = chapterExtras.indexOfFirst {
      (chapterId != null && chapterId > 0L && it.chapterId == chapterId) || (sort > 0 && it.sort == sort)
    }
    if (idx >= 0) {
      chapterExtras[idx] = next
    } else {
      chapterExtras.add(next)
    }
  }

  private fun prepareWorldNameForSave(publish: Boolean?): String {
    val trimmed = worldName.trim()
    if (trimmed.isNotBlank()) return trimmed
    if (publish == true) error("故事名称不能为空")
    val fallback = selectedProjectName().takeIf { it.isNotBlank() && it != "未选择项目" } ?: "未命名故事"
    worldName = fallback
    return fallback
  }

  private suspend fun saveWorldInternal(publish: Boolean? = null): WorldItem {
    val safeWorldName = prepareWorldNameForSave(publish)
    val worldKey = if (worldId > 0L) "world_$worldId" else "project_${selectedProjectId}"
    val persistedUserAvatarPath = ensureRemoteStoredMediaPath(userAvatarPath, "role", "${worldKey}_player_fg", true)
    val persistedUserAvatarBgPath = ensureRemoteStoredMediaPath(userAvatarBgPath, "role", "${worldKey}_player_bg", true)
    val persistedWorldCoverPath = ensureRemoteStoredMediaPath(worldCoverPath, "scene", "${worldKey}_cover_fg", true)
    val persistedWorldCoverBgPath = ensureRemoteStoredMediaPath(worldCoverBgPath, "scene", "${worldKey}_cover_bg", true)
    val playerRole = buildRole(
      id = "player",
      roleType = "player",
      name = playerName.ifBlank { "用户" },
      avatarPath = persistedUserAvatarPath,
      avatarBgPath = persistedUserAvatarBgPath,
      description = playerDesc,
      voice = playerVoice,
      voiceMode = playerVoiceMode,
      voicePresetId = playerVoicePresetId,
      voiceReferenceAudioPath = playerVoiceReferenceAudioPath,
      voiceReferenceAudioName = playerVoiceReferenceAudioName,
      voiceReferenceText = playerVoiceReferenceText,
      voicePromptText = playerVoicePromptText,
      voiceMixVoices = playerVoiceMixVoices,
      sample = "",
    )
    val narratorRole = buildRole(
      id = "narrator",
      roleType = "narrator",
      name = narratorName.ifBlank { "旁白" },
      avatarPath = "",
      avatarBgPath = "",
      description = "负责叙事推进",
      voice = narratorVoice,
      voiceMode = narratorVoiceMode,
      voicePresetId = narratorVoicePresetId,
      voiceReferenceAudioPath = narratorVoiceReferenceAudioPath,
      voiceReferenceAudioName = narratorVoiceReferenceAudioName,
      voiceReferenceText = narratorVoiceReferenceText,
      voicePromptText = narratorVoicePromptText,
      voiceMixVoices = narratorVoiceMixVoices,
      sample = "",
    )
    val npc = mutableListOf<StoryRole>()
    npcRoles.forEachIndexed { index, role ->
      npc.add(
        buildRole(
          id = role.id,
          roleType = "npc",
          name = role.name,
          avatarPath = ensureRemoteStoredMediaPath(role.avatarPath, "role", "${worldKey}_npc_${index}_fg", true),
          avatarBgPath = ensureRemoteStoredMediaPath(role.avatarBgPath, "role", "${worldKey}_npc_${index}_bg", true),
          description = role.description,
          voice = role.voice,
          voiceMode = role.voiceMode,
          voicePresetId = role.voicePresetId,
          voiceReferenceAudioPath = role.voiceReferenceAudioPath,
          voiceReferenceAudioName = role.voiceReferenceAudioName,
          voiceReferenceText = role.voiceReferenceText,
          voicePromptText = role.voicePromptText,
          voiceMixVoices = role.voiceMixVoices,
          sample = role.sample,
        ),
      )
    }
    val targetStatus = when (publish) {
      true -> "published"
      false -> "draft"
      else -> worldPublishStatus.ifBlank { "draft" }
    }
    val persistedChapterExtras = mutableListOf<ChapterExtra>()
    chapterExtras.forEachIndexed { index, extra ->
      persistedChapterExtras.add(
        extra.copy(
          background = ensureRemoteStoredMediaPath(
            extra.background,
            "scene",
            "${worldKey}_chapter_extra_${extra.chapterId ?: extra.sort.takeIf { it > 0 } ?: index}_bg",
            true,
          ),
        ),
      )
    }
    val settings = linkedMapOf<String, Any?>(
      "roles" to (listOf(playerRole) + npc),
      "narratorVoice" to narratorVoice,
      "narratorVoiceMode" to narratorVoiceMode,
      "narratorVoicePresetId" to narratorVoicePresetId,
      "narratorVoiceReferenceAudioPath" to narratorVoiceReferenceAudioPath,
      "narratorVoiceReferenceAudioName" to narratorVoiceReferenceAudioName,
      "narratorVoiceReferenceText" to narratorVoiceReferenceText,
      "narratorVoicePromptText" to narratorVoicePromptText,
      "narratorVoiceMixVoices" to narratorVoiceMixVoices,
      "globalBackground" to globalBackground,
      "coverPath" to persistedWorldCoverPath,
      "coverBgPath" to persistedWorldCoverBgPath,
      "allowRoleView" to allowRoleView,
      "allowChatShare" to allowChatShare,
      "publishStatus" to targetStatus,
      "chapterExtras" to persistedChapterExtras,
    )
    val payload = JsonObject().apply {
      if (worldId > 0) addProperty("worldId", worldId)
      addProperty("projectId", selectedProjectId)
      addProperty("name", safeWorldName)
      addProperty("intro", worldIntro.trim())
      add("settings", repository.toJson(settings))
      add("playerRole", repository.toJson(playerRole))
      add("narratorRole", repository.toJson(narratorRole))
    }
    val saved = repository.saveWorld(payload)
    worldId = saved.id
    worldPublishStatus = targetStatus
    return saved
  }

  private suspend fun saveEditorChapterInternal(targetStatus: String): ChapterItem? {
    if (worldId <= 0L || !hasCurrentChapterDraft()) return null
    val currentId = selectedChapterId
    val currentSort = currentChapterSort()
    val normalizedOpeningRole = chapterOpeningRole.ifBlank { narratorName.ifBlank { "旁白" } }.trim()
    val normalizedOpeningLine = normalizeScalarEditorText(chapterOpeningLine).trim()
    val normalizedChapterBackground = normalizeStoredMediaPath(chapterBackground)
    val normalizedChapterMusic = normalizeScalarEditorText(chapterMusic).trim()
    val payload = JsonObject().apply {
      if (currentId != null && currentId > 0L) addProperty("chapterId", currentId)
      addProperty("worldId", worldId)
      addProperty("title", chapterTitle.ifBlank { "第 $currentSort 章" }.trim())
      addProperty("chapterKey", chapterTitle.ifBlank { "第 $currentSort 章" }.trim())
      addProperty("content", buildPersistedChapterContent())
      addProperty("backgroundPath", normalizedChapterBackground)
      addProperty("openingRole", normalizedOpeningRole)
      addProperty("openingText", normalizedOpeningLine)
      addProperty("bgmPath", normalizedChapterMusic)
      addProperty("showCompletionCondition", chapterConditionVisible)
      parseChapterCondition(chapterEntryCondition)?.let { add("entryCondition", it) }
      parseChapterCondition(chapterCondition)?.let { add("completionCondition", it) }
      addProperty("sort", currentSort)
      addProperty("status", if (targetStatus == "published") "published" else "draft")
    }
    val saved = repository.saveChapter(payload)
    upsertChapterExtra(saved.id, saved.sort)
    selectedChapterId = saved.id
    return saved
  }

  private suspend fun refreshStoryData() {
    loadWorlds()
    loadSessions()
    refreshRecommendation()
  }

  fun openWorldForEdit(world: WorldItem) {
    if (selectedProjectId != world.projectId) {
      selectedProjectId = world.projectId
    }
    viewModelScope.launch {
      runCatching {
        loadWorldEditor(world.id) ?: error("未找到世界观")
        activeTab = "创建"
        notice = "已载入故事编辑"
      }.onFailure {
        notice = "载入故事失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun reopenPublishedWorldAsDraft(world: WorldItem) {
    if (selectedProjectId != world.projectId) {
      selectedProjectId = world.projectId
    }
    viewModelScope.launch {
      runCatching {
        val loaded = loadWorldEditor(world.id) ?: error("未找到世界观")
        if (!isWorldPublished(loaded)) {
          activeTab = "创建"
          notice = "该故事已在草稿箱"
          return@runCatching
        }
        saveWorldInternal(false)
        refreshStoryData()
        loadWorldEditor(world.id)
        activeTab = "创建"
        notice = "已转回草稿箱"
      }.onFailure {
        notice = "转回草稿失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun deleteWorld(world: WorldItem) {
    viewModelScope.launch {
      runCatching {
        repository.deleteWorld(world.id)
        if (world.id == worldId) {
          beginNewStoryDraft()
        }
        refreshStoryData()
        notice = "已删除故事"
      }.onFailure {
        notice = "删除故事失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun saveWorldConfig(publish: Boolean? = null, successNotice: String = "世界观保存成功") {
    if (selectedProjectId <= 0) {
      notice = "请先选择项目"
      return
    }
    if (publish == true && worldName.isBlank()) {
      notice = "世界名称不能为空"
      return
    }

    viewModelScope.launch {
      runCatching {
        saveWorldInternal(publish)
        refreshStoryData()
        primeStoryEditorPersistState()
        notice = successNotice
      }.onFailure {
        notice = "保存失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun saveStoryEditor(publish: Boolean? = null, startNextDraft: Boolean = false, successNotice: String? = "故事设定已保存") {
    if (selectedProjectId <= 0) {
      notice = "请先选择项目"
      return
    }
    if (publish == true && worldName.isBlank()) {
      notice = "故事名称不能为空"
      return
    }

    viewModelScope.launch {
      runCatching {
        val targetStatus = when (publish) {
          true -> "published"
          false -> "draft"
          else -> worldPublishStatus.ifBlank { "draft" }
        }
        saveWorldInternal(publish)
        val savedChapter = saveEditorChapterInternal(targetStatus)
        if (savedChapter != null) {
          saveWorldInternal(publish)
        }
        refreshStoryData()
        when {
          startNextDraft -> beginNewChapterDraft()
          savedChapter != null -> selectChapter(savedChapter.id)
        }
        primeStoryEditorPersistState()
        if (!successNotice.isNullOrBlank()) {
          notice = successNotice
        }
      }.onFailure {
        notice = "保存失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun addOrUpdateChapter(startNextDraft: Boolean = false) {
    if (worldId <= 0L) {
      notice = "请先保存世界观"
      return
    }
    if (chapterTitle.isBlank()) {
      notice = "章节标题不能为空"
      return
    }

    viewModelScope.launch {
      runCatching {
        val wasUpdating = selectedChapterId != null && selectedChapterId!! > 0L
        val saved = saveEditorChapterInternal(worldPublishStatus.ifBlank { "draft" }) ?: error("当前没有可保存的章节内容")
        saveWorldInternal(null)
        loadChapters(worldId)
        if (startNextDraft) {
          beginNewChapterDraft()
        } else {
          selectedChapterId = saved.id
          selectChapter(saved.id)
        }
        primeStoryEditorPersistState()
        notice = if (wasUpdating) "章节更新成功" else "章节创建成功"
      }.onFailure {
        notice = "章节保存失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun saveCurrentChapterAndSelect(targetChapterId: Long?) {
    viewModelScope.launch {
      runCatching {
        if (hasCurrentChapterDraft()) {
          saveWorldInternal(null)
          val savedChapter = saveEditorChapterInternal(worldPublishStatus.ifBlank { "draft" })
          if (savedChapter != null) {
            saveWorldInternal(null)
          }
        }
        refreshStoryData()
        if (targetChapterId == null) {
          beginNewChapterDraft()
        } else {
          selectChapter(targetChapterId)
        }
        primeStoryEditorPersistState()
      }.onFailure {
        notice = "章节切换失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun debugCurrentChapter() {
    if (selectedProjectId <= 0) {
      notice = "请先选择项目"
      return
    }
    clearRuntimeRetryState()
    viewModelScope.launch {
      runCatching {
        performDebugCurrentChapter()
      }.onFailure {
        debugLoading = false
        debugLoadingStage = ""
        activeTab = "游玩"
        showRuntimeRetryMessage("进入调试失败: ${it.message ?: "未知错误"}") {
          performDebugCurrentChapter()
        }
      }
    }
  }

  private suspend fun performDebugCurrentChapter() {
    debugMode = true
    debugLoading = true
    debugLoadingStage = "进入调试界面"
    debugEndDialog = null
    currentSessionId = "debug_${System.currentTimeMillis()}"
    debugSessionTitle = "调试：${worldName.ifBlank { "未命名故事" }}"
    debugWorldName = worldName
    debugWorldIntro = worldIntro
    debugRuntimeState = JsonObject()
    messages.clear()
    sessionDetail = null
    sendText = ""
    activeTab = "游玩"
    notice = "进入调试中..."
    try {
      debugLoadingStage = "保存草稿"
      saveWorldInternal(false)
      val savedChapter = saveEditorChapterInternal(worldPublishStatus.ifBlank { "draft" })
      if (savedChapter != null) {
        saveWorldInternal(false)
      }
      debugLoadingStage = "创建这次会话环境"
      if (worldId > 0L) {
        loadChapters(worldId)
      }
      refreshStoryData()
      primeStoryEditorPersistState()

      val currentWorld = worlds.firstOrNull { it.id == worldId } ?: WorldItem(
        id = worldId,
        projectId = selectedProjectId,
        name = worldName.ifBlank { "未命名故事" },
        intro = worldIntro,
        chapterCount = chapters.size,
      )
      val sorted = chapters.toList().sortedWith(compareBy<ChapterItem> { it.sort }.thenBy { it.id })
      val preferredId = savedChapter?.id ?: selectedChapterId
      val startChapter = when {
        preferredId != null -> sorted.firstOrNull { it.id == preferredId }
        else -> sorted.firstOrNull()
      } ?: error("请先填写当前章节")

      debugChapterSequence = sorted
      debugChapterId = startChapter.id
      debugChapterTitle = normalizeChapterTitleLabel(startChapter.title, startChapter.sort)
      debugWorldName = currentWorld.name
      debugWorldIntro = currentWorld.intro
      debugSessionTitle = "调试：${currentWorld.name.ifBlank { "未命名故事" }}"
      debugRuntimeState = JsonObject()
      debugEndDialog = null
      debugMessageSeed = 1L
      currentSessionId = "debug_${System.currentTimeMillis()}"
      sessionDetail = null
      messages.clear()
      sendText = ""

      debugLoadingStage = "读取记忆"
      val result = repository.debugOrchestration(
        worldId = worldId,
        chapterId = startChapter.id,
        state = debugRuntimeState,
        messages = emptyList(),
        playerContent = null,
      )
      debugLoadingStage = "准备剧情编排完毕"
      clearRuntimeRetryState()
      applyDebugOrchestrationResult(result, startChapter)
      debugLoading = false
      debugLoadingStage = ""
      if (result.plan != null) {
        streamDebugPlan(result.plan, emptyList(), null)
      } else {
        messages.clear()
      }
      updateDebugStatePreview()
      activeTab = "游玩"
      notice = "已进入章节调试模式（仅调试缓存，不会持久化）"
    } finally {
      debugLoading = false
      debugLoadingStage = ""
    }
  }

  fun quickStart() {
    val world = recommendedWorld()
    if (world == null) {
      notice = "暂无可推荐故事"
      return
    }
    startFromWorld(world, quickInput)
    quickInput = ""
  }

  fun startFromWorld(world: WorldItem, firstMessage: String = "") {
    if (debugMode) leaveDebugMode()
    if ((world.chapterCount ?: 0) <= 0) {
      notice = "该故事还没有章节，暂时无法游玩"
      return
    }
    viewModelScope.launch {
      runCatching {
        val sid = repository.startSession(world.id, world.projectId, null)
        if (sid.isBlank()) error("未返回 sessionId")
        currentSessionId = sid
        if (firstMessage.isNotBlank()) {
          repository.addPlayerMessage(sid, playerName.ifBlank { "用户" }, firstMessage)
        }
        refreshCurrentSession()
        loadSessions()
        activeTab = "游玩"
      }.onFailure {
        notice = "开始会话失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun createAndPlay() {
    if (debugMode) leaveDebugMode()
    val world = worlds.firstOrNull { it.id == worldId }
    if (world == null) {
      notice = "请先保存世界观"
      return
    }
    if (chapters.isEmpty()) {
      notice = "请先创建章节后再开始游玩"
      return
    }
    startFromWorld(world)
  }

  fun openSession(sessionId: String) {
    if (debugMode) leaveDebugMode()
    clearRuntimeRetryState()
    currentSessionId = sessionId
    viewModelScope.launch {
      runCatching {
        refreshCurrentSession()
        activeTab = "游玩"
      }.onFailure {
        notice = "打开会话失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun refreshPlaySession() {
    if (debugMode) {
      updateDebugStatePreview()
      return
    }
    viewModelScope.launch {
      runCatching {
        refreshCurrentSession()
      }.onFailure {
        notice = "刷新会话失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun sendMessage() {
    val sid = currentSessionId
    val text = sendText.trim()
    if (sid.isBlank() || text.isBlank()) return
    if (!playCanPlayerSpeak()) {
      notice = playTurnHint()
      return
    }

    if (debugMode) {
      sendDebugMessage(text)
      return
    }

    clearRuntimeRetryState()
    viewModelScope.launch {
      runCatching {
        performSessionPlayerMessage(sid, text)
      }.onFailure {
        showRuntimeRetryMessage("发送失败: ${it.message ?: "未知错误"}") {
          performSessionPlayerMessage(sid, text)
        }
      }
    }
  }

  private suspend fun loadUser() {
    runCatching {
      val user = repository.getUser()
      userName = user.get("name")?.asString ?: ""
      userId = user.get("id")?.asLong ?: 0L
      val remoteAvatarPath = user.get("avatarPath")?.asString?.trim().orEmpty()
      val remoteAvatarBgPath = user.get("avatarBgPath")?.asString?.trim().orEmpty()
      val storedAvatarPath = remoteAvatarPath.ifBlank { settingsStore.getAvatarPath(userId) }
      val storedAvatarBgPath = remoteAvatarBgPath.ifBlank { settingsStore.getAvatarBgPath(userId) }
      if (remoteAvatarPath.isNotBlank()) settingsStore.setAvatarPath(userId, remoteAvatarPath)
      if (remoteAvatarBgPath.isNotBlank()) settingsStore.setAvatarBgPath(userId, remoteAvatarBgPath)
      accountAvatarPath = resolveMediaPath(storedAvatarPath)
      accountAvatarBgPath = resolveMediaPath(storedAvatarBgPath)
    }.onFailure {
      userName = ""
      userId = 0L
      accountAvatarPath = ""
      accountAvatarBgPath = ""
      userAvatarPath = ""
      userAvatarBgPath = ""
    }
  }

  private suspend fun loadWorlds() {
    val rows = repository.listWorlds()
    worlds.clear()
    worlds.addAll(rows)
  }

  private suspend fun loadCurrentWorld(autoCreate: Boolean) {
    if (selectedProjectId <= 0) return
    val world = if (autoCreate) repository.getWorld(selectedProjectId, true) else repository.getWorld(selectedProjectId, false) ?: return
    if (world != null) {
      applyWorldToEditor(world)
      loadChapters(world.id)
    }
  }

  private suspend fun loadChapters(currentWorldId: Long) {
    val rows = repository.getChapter(currentWorldId)
    chapters.clear()
    chapters.addAll(rows)
    if (chapters.isNotEmpty()) {
      if (selectedChapterId == null || chapters.none { it.id == selectedChapterId }) {
        selectedChapterId = chapters.first().id
      }
      selectChapter(selectedChapterId)
    } else {
      selectedChapterId = null
      selectChapter(null)
    }
  }

  private suspend fun loadSessions() {
    val rows = repository.listSession(null)
      .groupBy { it.worldId }
      .mapNotNull { (_, group) -> group.maxByOrNull { item -> item.updateTime } }
      .sortedByDescending { it.updateTime }
    sessions.clear()
    sessions.addAll(rows)
  }

  private suspend fun refreshCurrentSession() {
    if (currentSessionId.isBlank()) return
    val detail = repository.getSession(currentSessionId)
    clearRuntimeRetryState()
    sessionDetail = detail
    messages.clear()
    if (detail.messages.isNotEmpty()) {
      messages.addAll(detail.messages)
    } else {
      messages.addAll(repository.getMessages(currentSessionId))
    }
  }

  private enum class DebugChapterResult {
    Continue,
    Success,
    Failed,
  }

  private suspend fun performSessionPlayerMessage(sessionId: String, text: String) {
    repository.addPlayerMessage(sessionId, playerName.ifBlank { "用户" }, text)
    sendText = ""
    clearRuntimeRetryState()
    refreshCurrentSession()
    loadSessions()
  }

  private fun sendDebugMessage(text: String) {
    val chapter = playCurrentChapter()
    if (chapter == null) {
      notice = "调试章节不存在，请返回重新选择。"
      updateDebugStatePreview()
      return
    }
    clearRuntimeRetryState()
    viewModelScope.launch {
      runCatching {
        performDebugPlayerMessage(text, appendPlayerMessage = true)
      }.onFailure {
        showRuntimeRetryMessage("调试发送失败: ${it.message ?: "未知错误"}") {
          performDebugPlayerMessage(text, appendPlayerMessage = false)
        }
        updateDebugStatePreview()
      }
    }
  }

  private suspend fun performDebugPlayerMessage(text: String, appendPlayerMessage: Boolean) {
    if (appendPlayerMessage) {
      messages.add(
        MessageItem(
          id = debugMessageSeed++,
          role = playerName.ifBlank { "用户" },
          roleType = "player",
          eventType = "on_message",
          content = text,
          createTime = System.currentTimeMillis(),
        ),
      )
      sendText = ""
    }
    val history = conversationMessages()
      .let { list ->
        val last = list.lastOrNull()
        if (last?.roleType == "player" && last.content == text) list.dropLast(1) else list
      }
    val result = repository.debugOrchestration(
      worldId = worldId,
      chapterId = debugChapterId,
      state = debugRuntimeState,
      messages = history,
      playerContent = text,
    )
    clearRuntimeRetryState()
    applyDebugOrchestrationResult(result, playCurrentChapter())
    if (result.plan != null) {
      streamDebugPlan(result.plan, conversationMessages(), text)
    }
    updateDebugStatePreview()
  }

  suspend fun continueDebugNarrative(): Boolean {
    if (!debugMode || worldId <= 0L) return false
    clearRuntimeRetryState()
    return runCatching {
      performContinueDebugNarrative()
      true
    }.getOrElse {
      showRuntimeRetryMessage("调试推进失败: ${it.message ?: "未知错误"}") {
        performContinueDebugNarrative()
      }
      updateDebugStatePreview()
      false
    }
  }

  private suspend fun performContinueDebugNarrative() {
    var advanced = false
    for (attempt in 0 until 3) {
      val beforeCount = conversationMessages().size
      val history = conversationMessages()
      val result = repository.debugOrchestration(
        worldId = worldId,
        chapterId = debugChapterId,
        state = debugRuntimeState,
        messages = history,
        playerContent = null,
      )
      clearRuntimeRetryState()
      applyDebugOrchestrationResult(result, playCurrentChapter())
      if (result.plan != null) {
        streamDebugPlan(result.plan, history, null)
      }
      updateDebugStatePreview()
      val canSpeak = try {
        val turnState = debugRuntimeState?.asJsonObject?.getAsJsonObject("turnState")
        turnState?.get("canPlayerSpeak")?.asBoolean ?: true
      } catch (_: Throwable) {
        true
      }
      if (conversationMessages().size > beforeCount || debugEndDialog != null || canSpeak) {
        advanced = true
        break
      }
    }
    if (!advanced) {
      error("自动推进没有产出新内容")
    }
  }

  private fun applyDebugOrchestrationResult(result: com.toonflow.game.data.DebugOrchestrationResult, fallbackChapter: ChapterItem?) {
    debugRuntimeState = result.state
    if (result.chapterId != null && result.chapterId > 0L) {
      debugChapterId = result.chapterId
    }
    val activeChapter = chapters.firstOrNull { it.id == (result.chapterId ?: debugChapterId) } ?: fallbackChapter
    debugChapterTitle = normalizeChapterTitleLabel(result.chapterTitle.ifBlank { activeChapter?.title.orEmpty() }, activeChapter?.sort ?: 0)
    debugEndDialog = result.endDialog
    updateDebugStatePreview()
  }

  private suspend fun streamDebugPlan(plan: DebugNarrativePlan, historyMessages: List<MessageItem>, playerContent: String?) {
    val placeholder = createStreamingMessage(plan)
    messages.clear()
    messages.addAll(historyMessages)
    messages.add(placeholder)
    var done = false
    repository.streamDebugLines(
      worldId = worldId,
      chapterId = debugChapterId,
      state = debugRuntimeState,
      messages = historyMessages,
      playerContent = playerContent,
      plan = plan,
    ) { event ->
      when (event.get("type")?.asString.orEmpty()) {
        "delta" -> {
          val text = event.getAsJsonObject("data")?.get("text")?.asString.orEmpty()
          if (text.isBlank()) return@streamDebugLines
          updateMessageById(placeholder.id) { current ->
            current.copy(content = current.content + text)
          }
        }

        "sentence" -> {
          val text = event.getAsJsonObject("data")?.get("text")?.asString.orEmpty().trim()
          if (text.isBlank()) return@streamDebugLines
          updateMessageById(placeholder.id) { current ->
            val meta = current.meta?.deepCopy()?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject().apply {
              addProperty("kind", "runtime_stream")
              addProperty("streaming", true)
            }
            val sentences = meta.getAsJsonArray("sentences") ?: JsonArray().also { meta.add("sentences", it) }
            val exists = sentences.any { item -> item?.takeIf { !it.isJsonNull }?.asString == text }
            if (!exists) {
              sentences.add(text)
            }
            current.copy(meta = meta)
          }
        }

        "done" -> {
          done = true
          val data = event.getAsJsonObject("data")
          val message = data?.getAsJsonObject("message")
          val finalContent = message?.get("content")?.asString ?: data?.get("content")?.asString.orEmpty()
          updateMessageById(placeholder.id) { current ->
            current.copy(
              role = message?.get("role")?.asString ?: current.role,
              roleType = message?.get("roleType")?.asString ?: current.roleType,
              eventType = message?.get("eventType")?.asString ?: current.eventType,
              content = finalContent,
              meta = JsonObject(),
            )
          }
        }

        "error" -> {
          val message = event.getAsJsonObject("data")?.get("message")?.asString.orEmpty().ifBlank { "台词流生成失败" }
          updateMessageById(placeholder.id) { null }
          error(message)
        }
      }
    }
    if (!done) {
      updateMessageById(placeholder.id) { null }
      error("台词流未正常结束")
    }
  }

  private fun appendDebugPlayer(content: String) {
    messages.add(
      MessageItem(
        id = debugMessageSeed++,
        role = playerName.ifBlank { "用户" },
        roleType = "player",
        eventType = "on_message",
        content = content,
        createTime = System.currentTimeMillis(),
      ),
    )
  }

  private fun appendDebugNarrator(content: String) {
    messages.add(
      MessageItem(
        id = debugMessageSeed++,
        role = narratorName.ifBlank { "旁白" },
        roleType = "narrator",
        eventType = "on_debug",
        content = content,
        createTime = System.currentTimeMillis(),
      ),
    )
  }

  private fun resolveNextDebugChapter(currentChapterId: Long, explicitNextId: Long?): ChapterItem? {
    val seq = debugChapterSequence.sortedWith(compareBy<ChapterItem> { it.sort }.thenBy { it.id })
    if (explicitNextId != null && explicitNextId > 0) {
      val explicit = seq.firstOrNull { it.id == explicitNextId }
      if (explicit != null) return explicit
    }
    val index = seq.indexOfFirst { it.id == currentChapterId }
    if (index < 0) return null
    return seq.getOrNull(index + 1)
  }

  private fun evaluateDebugChapterResult(chapter: ChapterItem, latestMessage: String): Pair<DebugChapterResult, Long?> {
    val cond = chapter.completionCondition ?: return DebugChapterResult.Continue to null
    if (cond.isJsonNull) return DebugChapterResult.Continue to null
    val condText = cond.toString().trim()
    if (condText.isBlank() || condText == "{}" || condText == "[]") {
      return DebugChapterResult.Continue to null
    }

    if (cond.isJsonObject) {
      val obj = cond.asJsonObject
      val failureNode = obj.get("failure") ?: obj.get("failed") ?: obj.get("fail")
      if (failureNode != null && evaluateConditionNode(failureNode, latestMessage, chapter)) {
        return DebugChapterResult.Failed to extractNextChapterId(obj)
      }
      val successNode = obj.get("success") ?: obj.get("pass")
      if (successNode != null && evaluateConditionNode(successNode, latestMessage, chapter)) {
        return DebugChapterResult.Success to extractNextChapterId(obj)
      }
    }

    val matched = evaluateConditionNode(cond, latestMessage, chapter)
    if (!matched) return DebugChapterResult.Continue to null

    val outcome = extractOutcome(cond)
    return if (outcome == "failed") {
      DebugChapterResult.Failed to extractNextChapterId(cond)
    } else {
      DebugChapterResult.Success to extractNextChapterId(cond)
    }
  }

  private fun evaluateConditionNode(node: JsonElement, latestMessage: String, chapter: ChapterItem): Boolean {
    if (node.isJsonNull) return false
    if (node.isJsonArray) {
      val array = node.asJsonArray
      if (array.size() == 0) return false
      return array.any { evaluateConditionNode(it, latestMessage, chapter) }
    }
    if (node.isJsonPrimitive) {
      val token = node.asString.trim()
      if (token.isBlank()) return false
      return latestMessage.contains(token, ignoreCase = true)
    }
    if (!node.isJsonObject) return false

    val obj = node.asJsonObject
    val allNode = obj.get("all")
    if (allNode is JsonArray) {
      if (allNode.size() == 0) return false
      return allNode.all { evaluateConditionNode(it, latestMessage, chapter) }
    }
    val anyNode = obj.get("any")
    if (anyNode is JsonArray) {
      if (anyNode.size() == 0) return false
      return anyNode.any { evaluateConditionNode(it, latestMessage, chapter) }
    }
    obj.get("not")?.let { return !evaluateConditionNode(it, latestMessage, chapter) }

    val type = obj.get("type")?.asString?.trim()?.lowercase() ?: "contains"
    val field = obj.get("field")?.asString?.trim()?.lowercase() ?: "message"
    val value = obj.get("value")?.asString?.trim().orEmpty()

    val fullText = messages.joinToString("\n") { it.content }
    val target = when (field) {
      "message", "latest", "latest_message" -> latestMessage
      "messages", "history", "full", "all" -> fullText
      "chapter", "chapter_title" -> chapter.title
      "chapter_content" -> chapter.content
      else -> latestMessage
    }

    return when (type) {
      "contains" -> value.isNotBlank() && target.contains(value, ignoreCase = true)
      "not_contains", "notcontains" -> value.isNotBlank() && !target.contains(value, ignoreCase = true)
      "equals", "eq" -> target.trim().equals(value, ignoreCase = true)
      "not_equals", "neq" -> !target.trim().equals(value, ignoreCase = true)
      "regex" -> runCatching { Regex(value, RegexOption.IGNORE_CASE).containsMatchIn(target) }.getOrDefault(false)
      "length_gte", "lengthgte" -> target.length >= (value.toIntOrNull() ?: Int.MAX_VALUE)
      "length_lte", "lengthlte" -> target.length <= (value.toIntOrNull() ?: Int.MIN_VALUE)
      else -> value.isNotBlank() && target.contains(value, ignoreCase = true)
    }
  }

  private fun extractOutcome(node: JsonElement): String {
    if (!node.isJsonObject) return "success"
    val obj = node.asJsonObject
    val raw = obj.get("result")?.asString
      ?: obj.get("status")?.asString
      ?: obj.get("outcome")?.asString
      ?: obj.get("onMatched")?.asString
      ?: "success"
    return when (raw.trim().lowercase()) {
      "failed", "fail", "failure", "lose", "dead" -> "failed"
      else -> "success"
    }
  }

  private fun extractNextChapterId(node: JsonElement): Long? {
    if (!node.isJsonObject) return null
    val obj = node.asJsonObject
    val raw = obj.get("nextChapterId") ?: obj.get("nextChapter") ?: return null
    return runCatching { raw.asLong }.getOrNull()
  }

  private fun updateDebugStatePreview() {
    val runtimePreview = debugRuntimeState?.takeIf { !it.isJsonNull }?.toString()?.trim()
    if (!runtimePreview.isNullOrBlank() && runtimePreview != "null") {
      debugStatePreview = runtimePreview
      return
    }
    val chapter = playCurrentChapter()
    debugStatePreview = JsonObject().apply {
      addProperty("mode", "debug")
      addProperty("worldName", debugWorldName)
      addProperty("chapterId", chapter?.id ?: 0L)
      addProperty("chapterTitle", chapter?.title ?: "")
      addProperty("messageCount", messages.size)
    }.toString()
  }

  private fun parseChapterCondition(raw: String): JsonElement? {
    val text = normalizeScalarEditorText(raw).trim()
    if (text.isEmpty()) return null
    return runCatching { JsonParser.parseString(text) }.getOrElse { JsonPrimitive(text) }
  }

  private fun buildRole(
    id: String,
    roleType: String,
    name: String,
    avatarPath: String,
    avatarBgPath: String,
    description: String,
    voice: String,
    voiceMode: String,
    voicePresetId: String,
    voiceReferenceAudioPath: String,
    voiceReferenceAudioName: String,
    voiceReferenceText: String,
    voicePromptText: String,
    voiceMixVoices: List<VoiceMixItem>,
    sample: String,
  ): StoryRole {
    val card = RoleParameterCard(
      name = name,
      rawSetting = description,
      voice = voice,
    )
    return StoryRole(
      id = id,
      roleType = roleType,
      name = name,
      avatarPath = avatarPath,
      avatarBgPath = avatarBgPath,
      description = description,
      voice = voice,
      voiceMode = voiceMode.ifBlank { "text" },
      voicePresetId = voicePresetId,
      voiceReferenceAudioPath = voiceReferenceAudioPath,
      voiceReferenceAudioName = voiceReferenceAudioName,
      voiceReferenceText = voiceReferenceText,
      voicePromptText = voicePromptText,
      voiceMixVoices = normalizedMixVoices(voiceMixVoices),
      sample = sample,
      parameterCardJson = card,
    )
  }

  private fun resetRuntimeData() {
    loading = false
    userName = ""
    userId = 0L
    accountAvatarPath = ""
    accountAvatarBgPath = ""
    userAvatarPath = ""
    userAvatarBgPath = ""
    selectedProjectId = -1L
    projects.clear()
    worlds.clear()
    sessions.clear()
    chapters.clear()
    npcRoles.clear()
    worldId = 0L
    worldName = ""
    worldIntro = ""
    worldCoverPath = ""
    worldCoverBgPath = ""
    playerName = "用户"
    playerDesc = ""
    playerVoice = ""
    playerVoicePresetId = ""
    playerVoiceMode = "text"
    playerVoiceReferenceAudioPath = ""
    playerVoiceReferenceAudioName = ""
    playerVoiceReferenceText = ""
    playerVoicePromptText = ""
    playerVoiceMixVoices = defaultMixVoices()
    narratorName = "旁白"
    narratorVoice = "默认旁白"
    narratorVoicePresetId = ""
    narratorVoiceMode = "text"
    narratorVoiceReferenceAudioPath = ""
    narratorVoiceReferenceAudioName = ""
    narratorVoiceReferenceText = ""
    narratorVoicePromptText = ""
    narratorVoiceMixVoices = defaultMixVoices()
    globalBackground = ""
    allowRoleView = true
    allowChatShare = true
    worldPublishStatus = "draft"
    chapterExtras.clear()
    chapterTitle = ""
    chapterContent = ""
    chapterEntryCondition = ""
    chapterCondition = ""
    chapterOpeningRole = "旁白"
    chapterOpeningLine = ""
    chapterBackground = ""
    chapterMusic = ""
    chapterConditionVisible = true
    selectedChapterId = null
    homeRecommendWorldId = null
    quickInput = ""
    currentSessionId = ""
    sessionDetail = null
    messages.clear()
    messageReactions.clear()
    sendText = ""
    debugMode = false
    debugSessionTitle = ""
    debugWorldName = ""
    debugWorldIntro = ""
    debugChapterId = null
    debugChapterTitle = ""
    debugRuntimeState = null
    debugStatePreview = "{}"
    debugEndDialog = null
    debugChapterSequence = emptyList()
    debugMessageSeed = 1L
    voiceModels.clear()
    settingsTextConfigs.clear()
    settingsImageConfigs.clear()
    settingsVoiceDesignConfigs.clear()
    settingsVoiceConfigs.clear()
    settingsAiModelMap.clear()
    storyPrompts.clear()
    voicePresetsCache.clear()
    voiceLoading = false
    settingsPanelLoaded = false
    settingsPanelLoading = false
    aiGenerating = false
  }

  private fun beginNewStoryDraft() {
    worldId = 0L
    worldName = ""
    worldIntro = ""
    worldCoverPath = ""
    worldCoverBgPath = ""
    playerName = "用户"
    playerDesc = ""
    playerVoice = ""
    playerVoicePresetId = ""
    playerVoiceMode = "text"
    playerVoiceReferenceAudioPath = ""
    playerVoiceReferenceAudioName = ""
    playerVoiceReferenceText = ""
    playerVoicePromptText = ""
    playerVoiceMixVoices = defaultMixVoices()
    narratorName = "旁白"
    narratorVoice = "默认旁白"
    narratorVoicePresetId = ""
    narratorVoiceMode = "text"
    narratorVoiceReferenceAudioPath = ""
    narratorVoiceReferenceAudioName = ""
    narratorVoiceReferenceText = ""
    narratorVoicePromptText = ""
    narratorVoiceMixVoices = defaultMixVoices()
    globalBackground = ""
    allowRoleView = true
    allowChatShare = true
    worldPublishStatus = "draft"
    chapterExtras.clear()
    npcRoles.clear()
    chapters.clear()
    selectedChapterId = null
    chapterTitle = ""
    chapterContent = ""
    chapterEntryCondition = ""
    chapterCondition = ""
    chapterOpeningRole = "旁白"
    chapterOpeningLine = ""
    chapterBackground = ""
    chapterMusic = ""
    chapterConditionVisible = true
  }

  fun startNewStoryDraft() {
    beginNewStoryDraft()
    primeStoryEditorPersistState()
    activeTab = "创建"
    notice = "已切换到新故事草稿"
  }

  private fun applyWorldToEditor(world: WorldItem) {
    worldId = world.id
    worldName = safeText(world.name)
    worldIntro = safeText(world.intro)
    worldCoverPath = resolveMediaPath(safeText(world.coverPath).ifBlank { safeText(world.settings?.coverPath) })
    worldCoverBgPath = resolveMediaPath(safeText(world.coverBgPath).ifBlank { safeText(world.settings?.coverBgPath) })
    playerName = world.playerRole?.name ?: "用户"
    userAvatarPath = resolveMediaPath(world.playerRole?.avatarPath)
    userAvatarBgPath = resolveMediaPath(world.playerRole?.avatarBgPath)
    playerDesc = world.playerRole?.description ?: ""
    playerVoice = world.playerRole?.voice ?: ""
    playerVoiceMode = world.playerRole?.voiceMode ?: "text"
    playerVoicePresetId = world.playerRole?.voicePresetId ?: ""
    playerVoiceReferenceAudioPath = world.playerRole?.voiceReferenceAudioPath.orEmpty()
    playerVoiceReferenceAudioName = world.playerRole?.voiceReferenceAudioName.orEmpty()
    playerVoiceReferenceText = world.playerRole?.voiceReferenceText.orEmpty()
    playerVoicePromptText = world.playerRole?.voicePromptText.orEmpty()
    playerVoiceMixVoices = normalizedMixVoices(world.playerRole?.voiceMixVoices ?: emptyList())
    narratorName = world.narratorRole?.name ?: "旁白"
    narratorVoice = world.settings?.narratorVoice ?: world.narratorRole?.voice ?: "默认旁白"
    narratorVoiceMode = world.settings?.narratorVoiceMode ?: world.narratorRole?.voiceMode ?: "text"
    narratorVoicePresetId = world.settings?.narratorVoicePresetId ?: world.narratorRole?.voicePresetId.orEmpty()
    narratorVoiceReferenceAudioPath = world.settings?.narratorVoiceReferenceAudioPath ?: world.narratorRole?.voiceReferenceAudioPath.orEmpty()
    narratorVoiceReferenceAudioName = world.settings?.narratorVoiceReferenceAudioName ?: world.narratorRole?.voiceReferenceAudioName.orEmpty()
    narratorVoiceReferenceText = world.settings?.narratorVoiceReferenceText ?: world.narratorRole?.voiceReferenceText.orEmpty()
    narratorVoicePromptText = world.settings?.narratorVoicePromptText ?: world.narratorRole?.voicePromptText.orEmpty()
    narratorVoiceMixVoices = normalizedMixVoices(world.settings?.narratorVoiceMixVoices ?: world.narratorRole?.voiceMixVoices ?: emptyList())
    globalBackground = world.settings?.globalBackground ?: ""
    allowRoleView = world.settings?.allowRoleView ?: true
    allowChatShare = world.settings?.allowChatShare ?: true
    worldPublishStatus = safeText(world.publishStatus).ifBlank {
      safeText(world.settings?.publishStatus).ifBlank { "draft" }
    }
    chapterExtras.clear()
    chapterExtras.addAll(world.settings?.chapterExtras ?: emptyList())
    npcRoles.clear()
    npcRoles.addAll((world.settings?.roles ?: emptyList()).filter { it.roleType == "npc" }.map(::resolveRoleMedia).map { it.withoutVoiceConfigId() })

    primeStoryEditorPersistState()
  }

  private suspend fun loadWorldEditor(worldId: Long): WorldItem? {
    if (worldId <= 0L) return null
    val world = repository.getWorldById(worldId) ?: return null
    applyWorldToEditor(world)
    loadChapters(world.id)
    return world
  }
}
