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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.JsonPrimitive
import com.google.gson.reflect.TypeToken
import com.toonflow.game.data.ChapterItem
import com.toonflow.game.data.ChapterExtra
import com.toonflow.game.data.GameRepository
import com.toonflow.game.data.MessageItem
import com.toonflow.game.data.ModelConfigItem
import com.toonflow.game.data.ProjectItem
import com.toonflow.game.data.PromptItem
import com.toonflow.game.data.RoleParameterCard
import com.toonflow.game.data.RuntimeEventDigestItem
import com.toonflow.game.data.SessionDetail
import com.toonflow.game.data.SessionChapterCommand
import com.toonflow.game.data.SessionItem
import com.toonflow.game.data.SessionNarrativeResult
import com.toonflow.game.data.SessionOrchestrationResult
import com.toonflow.game.data.SessionSnapshot
import com.toonflow.game.data.SettingsStore
import com.toonflow.game.data.StoryInfoResult
import com.toonflow.game.data.StoryInitResult
import com.toonflow.game.data.StoryRole
import com.toonflow.game.data.UploadedVoiceAudioResult
import com.toonflow.game.data.AiModelMapItem
import com.toonflow.game.data.AiModelOptionItem
import com.toonflow.game.data.AiTokenUsageLogItem
import com.toonflow.game.data.AiTokenUsageStatsItem
import com.toonflow.game.data.DebugNarrativePlan
import com.toonflow.game.data.DebugOrchestrationResult
import com.toonflow.game.data.DebugStepResult
import com.toonflow.game.data.GeneratedVoiceBindingResult
import com.toonflow.game.data.VoiceBindingDraft
import com.toonflow.game.data.VoiceModelConfig
import com.toonflow.game.data.VoiceMixItem
import com.toonflow.game.data.VoicePresetItem
import com.toonflow.game.data.WorldItem
import com.toonflow.game.util.VueTagLogger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import java.util.Locale

class MainViewModel(application: Application) : AndroidViewModel(application) {
  private companion object {
    const val RUNTIME_STREAM_PLACEHOLDER_TEXT = "获取台词中"
    private const val RUNTIME_RETRY_EVENT = "on_runtime_retry_error"

    /**
     * 为混音配置提供默认占位项，保证编辑器初始状态始终可编辑。
     */
    private fun defaultMixVoices(): List<VoiceMixItem> = listOf(VoiceMixItem(weight = 0.7))
  }

  private val settingsStore = SettingsStore(application)
  private val repository = GameRepository(settingsStore)
  private val prettyGson = GsonBuilder().setPrettyPrinting().create()
  private val runtimeChatStorageLimit = 24
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

  data class StoryRuntimeOption(
    val label: String,
    val value: String,
  )

  data class SettingsModelTestResult(
    val kind: String,
    val content: String,
  )

  data class ChapterPhasePreview(
    val id: String,
    val label: String,
    val kind: String,
    val allowedSpeakers: String,
    val nextPhaseIds: String,
    val defaultNextPhaseId: String,
    val requiredEventIds: String,
    val completionEventIds: String,
    val advanceSignals: String,
    val relatedFixedEventIds: String,
    val flowSummary: String,
  )

  data class ChapterUserNodePreview(
    val id: String,
    val goal: String,
    val promptRole: String,
  )

  data class ChapterFixedEventPreview(
    val id: String,
    val label: String,
  )

  data class ChapterEndingRulesPreview(
    val success: String,
    val failure: String,
    val nextChapterId: String,
  )

  data class ChapterRuntimeOutlinePreview(
    val phases: List<ChapterPhasePreview>,
    val userNodes: List<ChapterUserNodePreview>,
    val fixedEvents: List<ChapterFixedEventPreview>,
    val endingRules: ChapterEndingRulesPreview?,
  )

  data class RuntimeChapterProgressDebugItem(
    val phaseLabel: String,
    val phaseId: String,
    val pendingGoal: String,
    val userNodeLabel: String,
    val completedEvents: String,
  )

  data class RuntimeChapterEventItem(
    val eventIndex: Int,
    val eventKind: String,
    val eventFlowType: String,
    val eventSummary: String,
    val eventStatus: String,
    val eventFacts: String,
    val memorySummary: String,
    val memoryFacts: String,
  )

  data class RuntimeMiniGameStateItem(
    val key: String,
    val value: String,
  )

  data class RuntimeBattleEnemy(
    val enemyId: String,
    val name: String,
    val description: String,
    val level: Int,
    val hp: Int,
    val maxHp: Int,
    val mp: Int,
    val maxMp: Int,
    val avatarPath: String,
    val avatarBgPath: String,
    val isRoleEnemy: Boolean,
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
    val acceptsTextInput: Boolean,
    val inputHint: String,
    val stateItems: List<RuntimeMiniGameStateItem>,
    val battleEnemies: List<RuntimeBattleEnemy>,
  )

  data class RuntimeChatDebugItem(
    val conversationId: String,
    val messageId: Long,
    val lineIndex: Int,
    val currentRole: String,
    val currentRoleType: String,
    val currentStatus: String,
    val nextRole: String,
    val nextRoleType: String,
  )

  private data class ParsedPlayerProfile(
    val name: String = "",
    val gender: String = "",
    val age: Int? = null,
  )

  private data class DebugRevisitSnapshot(
    val conversationId: String,
    val messageId: Long,
    val messageCount: Int,
    val chapterId: Long?,
    val chapterTitle: String,
    val endDialog: String?,
    val endDialogDetail: String,
    val capturedAt: Long,
    val state: JsonElement?,
    val messages: List<MessageItem>,
  )

  private enum class SaveWorldStatusMode {
    PRESERVE,
    DRAFT,
    PUBLISHED,
  }

  val baseTabs = listOf("主页", "创建", "聊过", "我的")
  val settingsModelSlots = listOf(
    SettingsModelSlot("storyOrchestratorModel", "编排师", "text"),
    SettingsModelSlot("storyChapterJudgeModel", "章节判定", "text"),
    SettingsModelSlot("storyEventProgressModel", "事件进度检测", "text"),
    SettingsModelSlot("storyMiniGameModel", "小游戏动作解析", "text"),
    SettingsModelSlot("storyFastSpeakerModel", "快速角色发言", "text"),
    SettingsModelSlot("storySpeakerModel", "角色发言", "text"),
    SettingsModelSlot("storyMemoryModel", "记忆管理", "text"),
    SettingsModelSlot("storyImageModel", "AI生图", "image"),
    SettingsModelSlot("storyAvatarMattingModel", "头像分离", "image"),
    SettingsModelSlot("storyVoiceDesignModel", "语音设计", "voice_design"),
    SettingsModelSlot("storyVoiceModel", "语音生成", "voice"),
    SettingsModelSlot("storyAsrModel", "语音识别", "voice"),
  )
  val storyOrchestratorPayloadOptions = listOf(
    StoryRuntimeOption("精简版", "compact"),
    StoryRuntimeOption("高级版", "advanced"),
  )
  val reasoningEffortOptions = listOf(
    StoryRuntimeOption("minimal", "minimal"),
    StoryRuntimeOption("low", "low"),
    StoryRuntimeOption("medium", "medium"),
    StoryRuntimeOption("high", "high"),
  )
  val storyPromptCodes = listOf(
    "story-orchestrator-compact",
    "story-orchestrator-advanced",
    "story-speaker",
    "story-memory",
    "story-chapter",
    "story-event-progress",
    "story-mini-game",
    "story-mini-game-battle",
    "story-mini-game-fishing",
    "story-mini-game-werewolf",
    "story-mini-game-cultivation",
    "story-mini-game-mining",
    "story-mini-game-research-skill",
    "story-mini-game-alchemy",
    "story-mini-game-upgrade-equipment",
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
  var userNickname by mutableStateOf("")
  var userIntro by mutableStateOf("")
  var userId by mutableStateOf(0L)
  var settingsPageMode by mutableStateOf("settings")
  var profileNicknameDraft by mutableStateOf("")
  var profileIntroDraft by mutableStateOf("")
  var profileSaving by mutableStateOf(false)
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
  var storyPublishPending by mutableStateOf(false)
  val npcRoles = mutableStateListOf<StoryRole>()
  val chapterExtras = mutableStateListOf<ChapterExtra>()
  val voiceModels = mutableStateListOf<VoiceModelConfig>()
  val settingsTextConfigs = mutableStateListOf<ModelConfigItem>()
  val settingsImageConfigs = mutableStateListOf<ModelConfigItem>()
  val settingsVoiceDesignConfigs = mutableStateListOf<ModelConfigItem>()
  val settingsVoiceConfigs = mutableStateListOf<VoiceModelConfig>()
  val settingsAiModelMap = mutableStateListOf<AiModelMapItem>()
  val settingsTextModelList = mutableStateMapOf<String, List<AiModelOptionItem>>()
  val settingsTokenUsageLogs = mutableStateListOf<AiTokenUsageLogItem>()
  val settingsTokenUsageStats = mutableStateListOf<AiTokenUsageStatsItem>()
  val storyPrompts = mutableStateListOf<PromptItem>()
  private val voicePresetsCache = mutableStateMapOf<Long, List<VoicePresetItem>>()
  var voiceLoading by mutableStateOf(false)
  var settingsPanelLoading by mutableStateOf(false)
  var settingsPanelLoaded by mutableStateOf(false)
  var settingsTokenUsageLoading by mutableStateOf(false)
  var aiGenerating by mutableStateOf(false)

  var chapterTitle by mutableStateOf("")
  var chapterContent by mutableStateOf("")
  var chapterEntryCondition by mutableStateOf("")
  var chapterCondition by mutableStateOf("")
  var chapterOpeningRole by mutableStateOf("旁白")
  var chapterOpeningLine by mutableStateOf("")
  var chapterBackground by mutableStateOf("")
  var chapterMusic by mutableStateOf("")
  var chapterMusicAutoPlay by mutableStateOf(true)
  var chapterConditionVisible by mutableStateOf(true)
  var chapterRuntimeOutlineAutoGenerate by mutableStateOf(true)
  var chapterRuntimeOutlineText by mutableStateOf("")
  val chapters = mutableStateListOf<ChapterItem>()
  var selectedChapterId by mutableStateOf<Long?>(null)


  val sessions = mutableStateListOf<SessionItem>()
  var sessionListError by mutableStateOf("")
  var quickInput by mutableStateOf("")
  var currentSessionId by mutableStateOf("")
  var sessionDetail by mutableStateOf<SessionDetail?>(null)
  var sessionOpening by mutableStateOf(false)
  var sessionOpeningStage by mutableStateOf("")
  var sessionOpenError by mutableStateOf("")
  var sessionRuntimeStage by mutableStateOf("")
  private val playChapters = mutableStateListOf<ChapterItem>()
  private var playChapterWorldId by mutableStateOf(0L)
  val messages = mutableStateListOf<MessageItem>()
  var sessionViewMode by mutableStateOf("live")
  var sessionPlaybackStartIndex by mutableIntStateOf(0)
  var sessionResumeLatestOnOpen by mutableStateOf(false)
  private val messageReactions = mutableStateMapOf<String, String>()
  var sendText by mutableStateOf("")
  var sendPending by mutableStateOf(false)
  var runtimeProcessingPending by mutableStateOf(false)

  var debugMode by mutableStateOf(false)
  var debugSessionTitle by mutableStateOf("")
  var debugWorldName by mutableStateOf("")
  var debugWorldIntro by mutableStateOf("")
  var debugChapterId by mutableStateOf<Long?>(null)
  var debugChapterTitle by mutableStateOf("")
  var debugRuntimeState by mutableStateOf<JsonElement?>(null)
  var debugLatestPlan by mutableStateOf<DebugNarrativePlan?>(null)
  var debugStatePreview by mutableStateOf("{}")
  var debugEndDialog by mutableStateOf<String?>(null)
  var debugEndDialogDetail by mutableStateOf("")
  var sessionEndDialog by mutableStateOf<String?>(null)
  var sessionEndDialogDetail by mutableStateOf("")
  var debugLoading by mutableStateOf(false)
  var debugLoadingStage by mutableStateOf("")
  private val debugRevisitSnapshots = mutableStateListOf<DebugRevisitSnapshot>()
  private var debugChapterSequence: List<ChapterItem> = emptyList()
  private var debugMessageSeed: Long = 1L
  private var continueDebugNarrativeRunning = false
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
    val chapterMusicAutoPlay: Boolean,
    val chapterConditionVisible: Boolean,
    val chapterRuntimeOutlineAutoGenerate: Boolean,
    val chapterRuntimeOutlineText: String,
  )
  private var storyEditorAutoPersistJob: Job? = null
  private var storyEditorPersistMuted = false
  private var lastPersistedStoryEditorSnapshot: StoryEditorPersistSnapshot? = null
  private var undoStoryEditorSnapshot: StoryEditorPersistSnapshot? = null

  private fun StoryRole.withoutVoiceConfigId(): StoryRole {
    return copy(
      voiceConfigId = null,
      voiceMixVoices = voiceMixVoices.orEmpty().map { it.copy() },
    )
  }

  init {
    if (token.isBlank()) {
      resetRuntimeData()
      notice = "请先登录账号"
      openSettingsPanel()
    } else {
      reloadAll()
    }
  }

  fun autoVoiceEnabled(): Boolean = settingsStore.autoVoiceEnabled

  fun setAutoVoiceEnabled(enabled: Boolean) {
    settingsStore.autoVoiceEnabled = enabled
  }

  fun setTab(tab: String) {
    if (tab != "设置") {
      settingsPageMode = "settings"
    }
    activeTab = tab
    if (tab == "主页") {
      refreshRecommendation()
    }
    if (tab == "聊过" && token.isNotBlank()) {
      viewModelScope.launch {
        runCatching {
          loadSessions()
        }.onFailure {
          sessionListError = it.message ?: "未知错误"
          notice = "加载会话列表失败: ${sessionListError}"
        }
      }
    }
    if (tab == "设置" && token.isNotBlank()) {
      settingsPageMode = "settings"
      ensureSettingsPanelData()
    }
  }

  fun openHall() {
    activeTab = "故事大厅"
  }

  fun openSettings() {
    openSettingsPanel()
    if (token.isNotBlank()) ensureSettingsPanelData()
  }

  fun backToMy() {
    closeSettingsPanel()
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

  fun storyOrchestratorPayloadMode(): String {
    return if (settingsModelBinding("storyOrchestratorModel")?.payloadMode.equals("advanced", ignoreCase = true)) {
      "advanced"
    } else {
      "compact"
    }
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
      || manufacturer.trim().equals("local_modnet", ignoreCase = true)
  }

  private fun avatarMattingRecommendationScore(item: ModelConfigItem): Double {
    val manufacturer = item.manufacturer.trim().lowercase(Locale.ROOT)
    val model = item.model.trim().lowercase(Locale.ROOT)
    var score = 0.0
    if (manufacturer == "bria") score += 1000.0
    if (manufacturer == "local_birefnet") score += 960.0
    if (manufacturer == "local_modnet") score += 940.0
    if (manufacturer == "tencent_ci") score += 850.0
    if (manufacturer == "aliyun_imageseg") score += 700.0
    if (model == "segmentcommonimage") score += 80.0
    if (model == "aiportraitmatting") score += 120.0
    if (model == "birefnet-portrait") score += 160.0
    if (model == "modnet-photographic-portrait") score += 150.0
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
      val credentialHint = "Bria 的 API Key 直接填 token；阿里云视觉请填 AccessKeyId|AccessKeySecret 或 JSON；腾讯云数据万象请填 SecretId|SecretKey，Base URL 填标准 COS 桶域名；本地头像分离支持独立的 BiRefNet / MODNet 配置，无需 Key，但首次选择会提示安装本地依赖和模型文件。"
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
        saveWorldInternal(SaveWorldStatusMode.PRESERVE)
        val savedChapter = saveEditorChapterInternal("draft")
        if (savedChapter != null) {
          saveWorldInternal(SaveWorldStatusMode.PRESERVE)
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
        saveWorldInternal(SaveWorldStatusMode.PRESERVE)
        val savedChapter = saveEditorChapterInternal("draft")
        if (savedChapter != null) {
          saveWorldInternal(SaveWorldStatusMode.PRESERVE)
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
      npcRoles = npcRoles.map { it.copy(voiceMixVoices = it.voiceMixVoices.orEmpty().map { voice -> voice.copy() }) },
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
      chapterMusicAutoPlay = chapterMusicAutoPlay,
      chapterConditionVisible = chapterConditionVisible,
      chapterRuntimeOutlineAutoGenerate = chapterRuntimeOutlineAutoGenerate,
      chapterRuntimeOutlineText = chapterRuntimeOutlineText,
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
    chapterMusicAutoPlay = snapshot.chapterMusicAutoPlay
    chapterConditionVisible = snapshot.chapterConditionVisible
    chapterRuntimeOutlineAutoGenerate = snapshot.chapterRuntimeOutlineAutoGenerate
    chapterRuntimeOutlineText = normalizeRuntimeOutlineEditorText(snapshot.chapterRuntimeOutlineText)
  }

  private fun hasPersistableStoryEditorContent(snapshot: StoryEditorPersistSnapshot = captureStoryEditorSnapshot()): Boolean {
    if (snapshot.worldId > 0L) return true
    if (snapshot.worldName.isNotBlank() || snapshot.worldIntro.isNotBlank()) return true
    if (snapshot.worldCoverPath.isNotBlank() || snapshot.playerDesc.isNotBlank() || snapshot.globalBackground.isNotBlank()) return true
    if (snapshot.chapterTitle.isNotBlank() || snapshot.chapterContent.isNotBlank() || snapshot.chapterOpeningLine.isNotBlank()) return true
    if (snapshot.chapterEntryCondition.isNotBlank() || snapshot.chapterCondition.isNotBlank()) return true
    if (!snapshot.chapterRuntimeOutlineAutoGenerate) return true
    if (snapshot.chapterRuntimeOutlineText.isNotBlank()) return true
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
        val textModelList = repository.getAiModelList("text")
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
        settingsTextModelList.clear()
        settingsTextModelList.putAll(textModelList)
        storyPrompts.clear()
        storyPrompts.addAll(prompts.filter { it.code in storyPromptCodes }.sortedBy { it.id })
        settingsPanelLoaded = true
      }.onFailure {
        notice = "加载设置失败: ${it.message ?: "未知错误"}"
      }
      settingsPanelLoading = false
    }
  }

  fun loadAiTokenUsagePanel(
    startTime: String = "",
    endTime: String = "",
    type: String = "",
    granularity: String = "day",
  ) {
    if (token.isBlank()) return
    if (settingsTokenUsageLoading) return
    settingsTokenUsageLoading = true
    viewModelScope.launch {
      runCatching {
        val logs = repository.getAiTokenUsageLog(startTime.trim(), endTime.trim(), type.trim())
        val stats = repository.getAiTokenUsageStats(startTime.trim(), endTime.trim(), type.trim(), granularity.trim().ifBlank { "day" })
        settingsTokenUsageLogs.clear()
        settingsTokenUsageLogs.addAll(logs)
        settingsTokenUsageStats.clear()
        settingsTokenUsageStats.addAll(stats)
      }.onFailure {
        notice = "加载 token 消耗失败: ${it.message ?: "未知错误"}"
      }
      settingsTokenUsageLoading = false
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

  fun saveStoryOrchestratorPayloadMode(mode: String) {
    viewModelScope.launch {
      runCatching {
        repository.saveStoryRuntimeConfig(mode)
        ensureSettingsPanelData(true)
      }.onSuccess {
        notice = "编排师运行模式已保存"
      }.onFailure {
        notice = "保存编排师运行模式失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  suspend fun addManagedModelConfig(
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
    repository.addModelConfig(
      type,
      model,
      baseUrl,
      apiKey,
      modelType,
      manufacturer,
      inputPricePer1M,
      outputPricePer1M,
      cacheReadPricePer1M,
      currency,
      reasoningEffort,
    )
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
    inputPricePer1M: Double,
    outputPricePer1M: Double,
    cacheReadPricePer1M: Double,
    currency: String,
    reasoningEffort: String,
  ) {
    repository.updateModelConfig(
      id,
      type,
      model,
      baseUrl,
      apiKey,
      modelType,
      manufacturer,
      inputPricePer1M,
      outputPricePer1M,
      cacheReadPricePer1M,
      currency,
      reasoningEffort,
    )
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
        content = repository.testTextModel(
          config.model,
          config.apiKey,
          config.baseUrl,
          config.manufacturer,
          config.reasoningEffort,
        ),
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

  private fun normalizedMixVoices(mixVoices: List<VoiceMixItem>): List<VoiceMixItem> {
    return mixVoices
      .filter { it.voiceId.isNotBlank() }
      .map { item -> item.copy(weight = item.weight.coerceIn(0.1, 1.0)) }
  }

  private fun safeRoleMixVoices(role: StoryRole?): List<VoiceMixItem> {
    if (role == null) return emptyList()
    // 反序列化后的对象可能绕过构造器，把字段直接塞成 null；这里直接读底层字段，不信任 getter。
    val rawMixVoices: Any? = runCatching {
      val field = role.javaClass.getDeclaredField("voiceMixVoices")
      field.isAccessible = true
      field.get(role)
    }.getOrNull()
    @Suppress("UNCHECKED_CAST")
    return (rawMixVoices as? List<VoiceMixItem>).orEmpty()
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
    roleId: String = "",
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
      roleId = roleId,
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

  /**
   * 按当前绑定模式生成可复用参考音频，供运行时统一改走 clone 通道。
   */
  suspend fun generateVoiceBinding(
    configId: Long?,
    mode: String,
    presetId: String = "",
    referenceAudioPath: String = "",
    referenceText: String = "",
    promptText: String = "",
    mixVoices: List<VoiceMixItem> = emptyList(),
  ): GeneratedVoiceBindingResult {
    return repository.generateVoiceBinding(
      configId = configId,
      mode = mode,
      voiceId = presetId,
      referenceAudioPath = referenceAudioPath,
      referenceText = referenceText,
      promptText = promptText,
      mixVoices = normalizedMixVoices(mixVoices),
    )
  }

  /**
   * 把当前语音模式和配置 id 一起传给后端，让后端按目标语音接口选择润色策略。
   */
  suspend fun polishVoicePrompt(
    text: String,
    configId: Long? = null,
    mode: String = "",
    provider: String = "",
  ): String {
    return repository.polishVoicePrompt(text, configId, mode, provider)
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
    openSettingsPanel()
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
    val activeChapterId = playRuntimeChapterId()
    val sessionChapter = sessionDetail?.chapter
    if (activeChapterId == null) return sessionChapter
    if (sessionChapter?.id == activeChapterId) return sessionChapter
    return playChapters.firstOrNull { it.id == activeChapterId }
      ?: chapters.firstOrNull { it.id == activeChapterId }
      ?: sessionChapter
  }

  fun playChapterTitle(): String {
    if (debugMode) {
      val chapter = playCurrentChapter()
      return normalizeChapterTitleLabel(debugChapterTitle.ifBlank { chapter?.title.orEmpty() }, chapter?.sort ?: 0).ifBlank { "未进入章节" }
    }
    val chapter = playCurrentChapter()
    return normalizeChapterTitleLabel(chapter?.title.orEmpty(), chapter?.sort ?: 0).ifBlank { "未进入章节" }
  }

  fun playStatePreview(): String {
    if (debugMode) return debugStatePreview.ifBlank { "{}" }
    val source = sessionDetail?.state
    return source?.toString().orEmpty().ifBlank { "{}" }.take(320)
  }

  private fun runtimeStateRoot(): JsonObject? {
    if (debugMode) {
      val source = debugRuntimeState
      if (source == null || source.isJsonNull || !source.isJsonObject) return null
      return source.asJsonObject
    }
    val currentState = sessionDetail?.state?.takeIf { it.isJsonObject }?.asJsonObject
    val latestState = sessionDetail?.latestSnapshot?.state?.takeIf { it.isJsonObject }?.asJsonObject
    return when {
      currentState?.get("turnState")?.isJsonObject == true -> currentState
      latestState?.get("turnState")?.isJsonObject == true -> latestState
      currentState != null -> currentState
      else -> latestState
    }
  }

  private fun runtimeTurnStateRoot(): JsonObject? {
    return runtimeStateRoot()?.getAsJsonObject("turnState")
  }

  private fun sessionTurnStateRoot(detail: SessionDetail?): JsonObject? {
    val latestState = detail?.latestSnapshot?.state?.takeIf { it.isJsonObject }?.asJsonObject
    val currentState = detail?.state?.takeIf { it.isJsonObject }?.asJsonObject
    return latestState?.getAsJsonObject("turnState") ?: currentState?.getAsJsonObject("turnState")
  }

  private fun playRuntimeChapterId(): Long? {
    if (debugMode) return debugChapterId
    val fromState = scalarRuntimeText(runtimeStateRoot()?.get("chapterId")).toLongOrNull()
    if (fromState != null && fromState > 0L) return fromState
    return sessionDetail?.chapterId?.takeIf { it > 0L }
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

  private fun runtimeIntValue(input: JsonElement?): Int? {
    val text = scalarRuntimeText(input)
    if (text.isBlank() || !text.matches(Regex("^\\d{1,6}$"))) return null
    return text.toIntOrNull()
  }

  private fun runtimeMixVoices(input: JsonElement?): List<VoiceMixItem> {
    if (input == null || input.isJsonNull || !input.isJsonArray) return emptyList()
    return input.asJsonArray.mapNotNull { item ->
      if (!item.isJsonObject) return@mapNotNull null
      val obj = item.asJsonObject
      val voiceId = scalarRuntimeText(obj.get("voiceId"))
      if (voiceId.isBlank()) return@mapNotNull null
      VoiceMixItem(
        voiceId = voiceId,
        weight = scalarRuntimeText(obj.get("weight")).toDoubleOrNull() ?: 0.7,
      )
    }
  }

  private fun runtimeParameterCard(input: JsonElement?): RoleParameterCard? {
    if (input == null || input.isJsonNull || !input.isJsonObject) return null
    val obj = input.asJsonObject
    fun arrayValues(key: String): List<String> {
      val raw = obj.get(key)
      if (raw == null || raw.isJsonNull || !raw.isJsonArray) return emptyList()
      return raw.asJsonArray.mapNotNull { scalarRuntimeText(it).ifBlank { null } }
    }
    val card = RoleParameterCard(
      name = scalarRuntimeText(obj.get("name")),
      rawSetting = scalarRuntimeText(obj.get("raw_setting")).ifBlank { scalarRuntimeText(obj.get("rawSetting")) },
      gender = scalarRuntimeText(obj.get("gender")),
      age = runtimeIntValue(obj.get("age")),
      level = runtimeIntValue(obj.get("level")) ?: 1,
      levelDesc = scalarRuntimeText(obj.get("level_desc")).ifBlank { scalarRuntimeText(obj.get("levelDesc")) }.ifBlank { "初入此界" },
      personality = scalarRuntimeText(obj.get("personality")),
      appearance = scalarRuntimeText(obj.get("appearance")),
      voice = scalarRuntimeText(obj.get("voice")),
      skills = arrayValues("skills"),
      items = arrayValues("items"),
      equipment = arrayValues("equipment"),
      // 经验值和升级阈值来自正式 storyInfo / getSession 的运行态参数卡。
      // 这里如果不显式解析，后面的角色详情会退回到数据类默认值 0/100，
      // 看起来就像“后端没写经验”，实际是安卓运行态转换层把字段吃掉了。
      exp = runtimeIntValue(obj.get("exp")) ?: 0,
      nextLevelExp = runtimeIntValue(obj.get("next_level_exp")) ?: runtimeIntValue(obj.get("nextLevelExp")) ?: 100,
      hp = runtimeIntValue(obj.get("hp")) ?: 100,
      mp = runtimeIntValue(obj.get("mp")) ?: 0,
      money = runtimeIntValue(obj.get("money")) ?: 0,
      other = arrayValues("other"),
    )
    val hasContent = card.name.isNotBlank()
      || card.rawSetting.isNotBlank()
      || card.gender.isNotBlank()
      || card.age != null
      || card.level > 1
      || card.levelDesc.isNotBlank()
      || card.personality.isNotBlank()
      || card.appearance.isNotBlank()
      || card.voice.isNotBlank()
      || card.skills.isNotEmpty()
      || card.items.isNotEmpty()
      || card.equipment.isNotEmpty()
      || card.exp > 0
      || card.nextLevelExp > 100
      || card.hp > 100
      || card.mp > 0
      || card.money > 0
      || card.other.isNotEmpty()
    return if (hasContent) card else null
  }

  private fun runtimeRoleSnapshot(roleKey: String, fallbackRoleType: String): StoryRole? {
    val raw = runtimeStateRoot()?.get(roleKey) ?: return null
    if (raw.isJsonNull || !raw.isJsonObject) return null
    val obj = raw.asJsonObject
    return StoryRole(
      id = scalarRuntimeText(obj.get("id")).ifBlank { roleKey },
      roleType = scalarRuntimeText(obj.get("roleType")).ifBlank { fallbackRoleType },
      name = scalarRuntimeText(obj.get("name")).ifBlank { if (fallbackRoleType == "player") "用户" else "旁白" },
      avatarPath = scalarRuntimeText(obj.get("avatarPath")),
      avatarBgPath = scalarRuntimeText(obj.get("avatarBgPath")),
      description = scalarRuntimeText(obj.get("description")),
      voice = scalarRuntimeText(obj.get("voice")),
      voiceMode = scalarRuntimeText(obj.get("voiceMode")).ifBlank { "text" },
      voiceConfigId = null,
      voicePresetId = scalarRuntimeText(obj.get("voicePresetId")),
      voiceReferenceAudioPath = scalarRuntimeText(obj.get("voiceReferenceAudioPath")),
      voiceReferenceAudioName = scalarRuntimeText(obj.get("voiceReferenceAudioName")),
      voiceReferenceText = scalarRuntimeText(obj.get("voiceReferenceText")),
      voicePromptText = scalarRuntimeText(obj.get("voicePromptText")),
      voiceMixVoices = runtimeMixVoices(obj.get("voiceMixVoices")),
      sample = scalarRuntimeText(obj.get("sample")),
      parameterCardJson = runtimeParameterCard(obj.get("parameterCardJson")),
    )
  }

  private fun runtimeNpcSnapshot(base: StoryRole): StoryRole? {
    val npcs = runtimeStateRoot()?.getAsJsonObject("npcs") ?: return null
    val matched = npcs.entrySet()
      .mapNotNull { (_, value) -> value.takeIf { it.isJsonObject }?.asJsonObject }
      .firstOrNull { obj ->
        val itemId = scalarRuntimeText(obj.get("id"))
        val itemName = scalarRuntimeText(obj.get("name"))
        (base.id.isNotBlank() && itemId.isNotBlank() && itemId == base.id) ||
          (base.name.isNotBlank() && itemName.isNotBlank() && itemName == base.name)
      } ?: return null
    return StoryRole(
      id = scalarRuntimeText(matched.get("id")).ifBlank { base.id },
      roleType = scalarRuntimeText(matched.get("roleType")).ifBlank { base.roleType.ifBlank { "npc" } },
      name = scalarRuntimeText(matched.get("name")).ifBlank { base.name },
      avatarPath = scalarRuntimeText(matched.get("avatarPath")),
      avatarBgPath = scalarRuntimeText(matched.get("avatarBgPath")),
      description = scalarRuntimeText(matched.get("description")),
      voice = scalarRuntimeText(matched.get("voice")),
      voiceMode = scalarRuntimeText(matched.get("voiceMode")).ifBlank { "text" },
      voiceConfigId = null,
      voicePresetId = scalarRuntimeText(matched.get("voicePresetId")),
      voiceReferenceAudioPath = scalarRuntimeText(matched.get("voiceReferenceAudioPath")),
      voiceReferenceAudioName = scalarRuntimeText(matched.get("voiceReferenceAudioName")),
      voiceReferenceText = scalarRuntimeText(matched.get("voiceReferenceText")),
      voicePromptText = scalarRuntimeText(matched.get("voicePromptText")),
      voiceMixVoices = runtimeMixVoices(matched.get("voiceMixVoices")),
      sample = scalarRuntimeText(matched.get("sample")),
      parameterCardJson = runtimeParameterCard(matched.get("parameterCardJson")),
    )
  }

  private fun mergeRoleSnapshot(base: StoryRole, runtime: StoryRole?): StoryRole {
    if (runtime == null) return base
    // 后端或本地快照里可能混入空值；这里强制收敛为非空字符串，避免 Compose 重组时直接抛 NPE。
    val baseMixVoices = safeRoleMixVoices(base)
    val runtimeMixVoices = safeRoleMixVoices(runtime)
    return StoryRole(
      id = safeText(runtime.id).ifBlank { safeText(base.id) },
      roleType = safeText(runtime.roleType).ifBlank { safeText(base.roleType) },
      name = safeText(runtime.name).ifBlank { safeText(base.name) },
      avatarPath = safeText(runtime.avatarPath).ifBlank { safeText(base.avatarPath) },
      avatarBgPath = safeText(runtime.avatarBgPath).ifBlank { safeText(base.avatarBgPath) },
      description = safeText(runtime.description).ifBlank { safeText(base.description) },
      voice = safeText(runtime.voice).ifBlank { safeText(base.voice) },
      voiceMode = safeText(runtime.voiceMode).ifBlank { safeText(base.voiceMode).ifBlank { "text" } },
      voiceConfigId = runtime.voiceConfigId ?: base.voiceConfigId,
      voicePresetId = safeText(runtime.voicePresetId).ifBlank { safeText(base.voicePresetId) },
      voiceReferenceAudioPath = safeText(runtime.voiceReferenceAudioPath).ifBlank { safeText(base.voiceReferenceAudioPath) },
      voiceReferenceAudioName = safeText(runtime.voiceReferenceAudioName).ifBlank { safeText(base.voiceReferenceAudioName) },
      voiceReferenceText = safeText(runtime.voiceReferenceText).ifBlank { safeText(base.voiceReferenceText) },
      voicePromptText = safeText(runtime.voicePromptText).ifBlank { safeText(base.voicePromptText) },
      voiceMixVoices = if (runtimeMixVoices.isNotEmpty()) runtimeMixVoices else baseMixVoices,
      sample = safeText(runtime.sample).ifBlank { safeText(base.sample) },
      parameterCardJson = runtime.parameterCardJson ?: base.parameterCardJson,
    )
  }

  fun playCanPlayerSpeak(): Boolean {
    return runtimeTurnStateRoot()?.get("canPlayerSpeak")?.asBoolean ?: true
  }

  /**
   * 正式会话恢复后按运行态决定是否继续自动编排。
   *
   * 用途：
   * - 二次进入或回溯后，如果当前仍不是用户回合，就继续 orchestration -> streamlines；
   * - 不能只在“消息为空”时推进，因为已有旁白但等待下一句生成也是常态。
   */
  private fun scheduleSessionNarrativeIfSystemTurn() {
    if (debugMode || currentSessionId.isBlank() || sessionViewMode == "playback") return
    if (runtimeProcessingPending || sessionRuntimeStage.isNotBlank()) return
    val hasStreamingMessage = conversationMessages(messages.toList()).any(::isStreamingRuntimeMessage)
    if (!playCanPlayerSpeak() && !hasStreamingMessage) {
      viewModelScope.launch {
        continueSessionNarrative()
      }
    }
  }

  fun playSessionStatus(): String {
    if (debugMode) return "debug"
    return sessionDetail?.status.orEmpty()
  }

  fun playExpectedSpeaker(): String {
    return scalarRuntimeText(runtimeTurnStateRoot()?.get("expectedRole")).ifBlank {
      if (playCanPlayerSpeak()) "用户" else "当前角色"
    }
  }

  private fun playCurrentRuntimeStatus(): String {
    if (sessionOpening) return "session_opening"
    if (sessionOpenError.isNotBlank()) return "session_error"
    if (sendPending || runtimeProcessingPending) return "sending"
    if (playRuntimeMiniGame() != null) return "waiting_player"
    val latest = conversationMessages().lastOrNull()
    val status = latest?.let { runtimeMessageStatus(it) }.orEmpty()
    if (status == "sending") return "sending"
    if (status == "orchestrated") return if (playCanPlayerSpeak()) "waiting_player" else "waiting_next"
    if (
      playCanPlayerSpeak() &&
      status.isNotBlank() &&
      status !in setOf("streaming", "generated", "revealing", "voicing", "auto_advancing", "sending", "orchestrated")
    ) {
      return "waiting_player"
    }
    if (status.isNotBlank()) return status
    return if (playCanPlayerSpeak()) "waiting_player" else "waiting_next"
  }

  /**
   * 返回最后一条消息的原始运行态状态。
   *
   * 用途：
   * - 输入栏需要区分“真正还在请求后端”和“只是正在自动朗读”；
   * - `playCurrentRuntimeStatus()` 会把 `runtimeProcessingPending` 折叠成 `sending`，
   *   适合整体流程判断，但不适合给底部交互区做更细的提示文案。
   */
  fun playLatestRuntimeMessageStatus(): String {
    return conversationMessages().lastOrNull()?.let(::runtimeMessageStatus).orEmpty()
  }

  fun playCanPlayerInput(): Boolean {
    if (sessionOpening) return false
    if (sessionOpenError.isNotBlank()) return false
    if (sendPending || runtimeProcessingPending) return false
    if (sessionRuntimeStage.isNotBlank()) return false
    if (playRuntimeMiniGame() != null) return true
    return playCanPlayerSpeak() && playCurrentRuntimeStatus() == "waiting_player"
  }

  fun playShouldAutoRefreshWhileWaiting(): Boolean {
    return false
  }

  fun playInputPlaceholder(textMode: Boolean): String {
    if (sessionOpening) return sessionOpeningStage.ifBlank { "正在进入故事..." }
    if (sessionOpenError.isNotBlank()) return "打开会话失败，请重试"
    playRuntimeMiniGame()?.let {
      // 小游戏统一改成聊天流后，输入框不再重复显示动作提示，避免和状态面板文字叠加。
      return ""
    }
    val runtimeStatus = playCurrentRuntimeStatus()
    val status = playSessionStatus().trim().lowercase()
    if (runtimeStatus == "sending") {
      return "处理中..."
    }
    if (sessionRuntimeStage.isNotBlank()) return sessionRuntimeStage
    if (runtimeStatus == "waiting_player" && playCanPlayerSpeak()) {
      return if (textMode) "输入一句话继续故事" else "按住说话"
    }
    if (status in setOf("chapter_completed", "completed", "success", "finished")) {
      return "当前章节已完成"
    }
    if (status in setOf("failed", "dead", "lose", "loss")) {
      return "当前故事已失败"
    }
    return "当前轮到${playExpectedSpeaker()}发言"
  }

  fun playTurnHint(): String {
    if (sessionOpening) return sessionOpeningStage.ifBlank { "正在进入故事..." }
    if (sessionOpenError.isNotBlank()) return "打开会话失败：$sessionOpenError"
    if (playRuntimeMiniGame() != null) {
      return ""
    }
    val runtimeStatus = playCurrentRuntimeStatus()
    val status = playSessionStatus().trim().lowercase()
    if (runtimeStatus == "sending") {
      return "正在处理中..."
    }
    if (runtimeStatus == "error") {
      return "发送失败，可重试或重新输入。"
    }
    if (sessionRuntimeStage.isNotBlank()) return sessionRuntimeStage
    if (status in setOf("chapter_completed", "completed", "success", "finished")) {
      return "当前章节已完成，可刷新或返回历史继续查看。"
    }
    if (status in setOf("failed", "dead", "lose", "loss")) {
      return "当前故事已失败，可返回历史重新开始。"
    }
    val latest = conversationMessages().lastOrNull()
    if (latest != null && isLocalPendingPlayerMessage(latest) && runtimeMessageStatus(latest) == "error") {
      return "发送失败，可重试或重新输入。"
    }
    if (runtimeStatus == "waiting_player" && playCanPlayerSpeak()) {
      return ""
    }
    if (runtimeStatus == "voicing") {
      return "正在朗读当前台词，稍后继续。"
    }
    if (runtimeStatus in setOf("streaming", "generated", "revealing", "auto_advancing", "orchestrated")) {
      return "正在生成下一句内容..."
    }
    return "当前还没轮到用户发言，等待${playExpectedSpeaker()}继续。"
  }

  fun playLatestRuntimeChatDebug(): RuntimeChatDebugItem? {
    val latest = conversationMessages().lastOrNull() ?: return null
    val meta = latest.meta?.takeIf { it.isJsonObject }?.asJsonObject
    val turnState = runtimeTurnStateRoot()
    val canPlayerSpeakNow = turnState?.get("canPlayerSpeak")?.asBoolean ?: true
    val currentStatus = runtimeMessageStatus(latest).ifBlank { playCurrentRuntimeStatus() }
    return RuntimeChatDebugItem(
      conversationId = currentSessionId,
      messageId = latest.id,
      lineIndex = meta?.get("lineIndex")?.takeIf { !it.isJsonNull }?.asInt ?: conversationMessages().size,
      currentRole = displayNameForMessage(latest),
      currentRoleType = latest.roleType,
      currentStatus = currentStatus,
      nextRole =
        scalarRuntimeText(meta?.get("nextRole")).ifBlank {
          if (playCanPlayerSpeak() || canPlayerSpeakNow || currentStatus == "waiting_player") "用户" else scalarRuntimeText(turnState?.get("expectedRole")).ifBlank { "当前角色" }
        },
      nextRoleType =
        scalarRuntimeText(meta?.get("nextRoleType")).ifBlank {
          if (playCanPlayerSpeak() || canPlayerSpeakNow || currentStatus == "waiting_player") "player" else scalarRuntimeText(turnState?.get("expectedRoleType")).ifBlank { "npc" }
        },
    )
  }

  private fun runtimeConversationId(): String {
    val sessionId = currentSessionId.trim()
    if (sessionId.isNotBlank()) {
      return "session:$sessionId"
    }
    if (debugMode) {
      return "debug:world:${worldId}:chapter:${debugChapterId ?: 0L}"
    }
    return "world:${worldId}:chapter:${playRuntimeChapterId() ?: 0L}"
  }

  private fun debugRevisitConversationId(): String {
    val debugKey = debugRuntimeState
      ?.takeIf { it.isJsonObject }
      ?.asJsonObject
      ?.get("debugRuntimeKey")
      ?.takeIf { !it.isJsonNull }
      ?.asString
      ?.trim()
      .orEmpty()
    if (debugKey.isNotBlank()) {
      return debugKey
    }
    return if (debugMode) {
      "debug:world:${worldId}:chapter:${debugChapterId ?: 0L}"
    } else {
      ""
    }
  }

  private fun loadDebugRevisitSnapshots() {
    val conversationId = debugRevisitConversationId()
    debugRevisitSnapshots.clear()
    if (conversationId.isBlank()) return
    val type = object : TypeToken<List<DebugRevisitSnapshot>>() {}.type
    val restored = runCatching {
      prettyGson.fromJson<List<DebugRevisitSnapshot>>(
        settingsStore.getDebugRevisitSnapshotsJson(conversationId),
        type,
      ).orEmpty()
    }.getOrElse { emptyList() }
    debugRevisitSnapshots.addAll(
      restored
        .filter { it.messageId > 0L && it.conversationId.isNotBlank() && it.messages.isNotEmpty() }
        .sortedBy { it.capturedAt },
        )
  }

  private fun persistDebugRevisitSnapshots() {
    val conversationId = debugRevisitConversationId()
    if (conversationId.isBlank()) return
    if (debugRevisitSnapshots.isEmpty()) {
      settingsStore.clearDebugRevisitSnapshots(conversationId)
      return
    }
    settingsStore.setDebugRevisitSnapshotsJson(
      conversationId,
      prettyGson.toJson(debugRevisitSnapshots.takeLast(5)),
    )
  }

  private fun clearDebugRevisitSnapshots() {
    val conversationId = debugRevisitConversationId()
    debugRevisitSnapshots.clear()
    if (conversationId.isNotBlank()) {
      settingsStore.clearDebugRevisitSnapshots(conversationId)
    }
  }

  private fun syncRuntimeChatTraceLog() {
    val conversationId = runtimeConversationId()
    if (conversationId.isBlank()) return
    val history = conversationMessages(messages.toList()).filterNot(::isRuntimeRetryMessage)
    val previousJson = settingsStore.getRuntimeChatTraceJson().ifBlank { "[]" }
    val previousRows = runCatching {
      JsonParser.parseString(previousJson).takeIf { it.isJsonArray }?.asJsonArray ?: JsonArray()
    }.getOrElse { JsonArray() }
    val nextRows = JsonArray()
    previousRows.forEach { item ->
      val row = item.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
      if (scalarRuntimeText(row.get("conversationId")) != conversationId) {
        nextRows.add(row)
      }
    }
    val latest = history.lastOrNull()
    if (latest != null) {
      val meta = latest.meta?.takeIf { it.isJsonObject }?.asJsonObject
      val turnState = runtimeTurnStateRoot()
      val canPlayerSpeakNow = turnState?.get("canPlayerSpeak")?.asBoolean ?: true
      val currentStatus = runtimeMessageStatus(latest).ifBlank {
        if (canPlayerSpeakNow) "waiting_player" else "waiting_next"
      }
      val row = JsonObject().apply {
        addProperty("conversationId", conversationId)
        addProperty("messageId", latest.id)
        addProperty("lineIndex", meta?.get("lineIndex")?.takeIf { !it.isJsonNull }?.asInt ?: history.size)
        addProperty("currentRole", displayNameForMessage(latest))
        addProperty("currentRoleType", latest.roleType)
        addProperty("currentStatus", currentStatus)
        addProperty(
          "nextRole",
          scalarRuntimeText(meta?.get("nextRole")).ifBlank {
            if (playCanPlayerSpeak() || canPlayerSpeakNow || currentStatus == "waiting_player") "用户" else scalarRuntimeText(turnState?.get("expectedRole")).ifBlank { "当前角色" }
          },
        )
        addProperty(
          "nextRoleType",
          scalarRuntimeText(meta?.get("nextRoleType")).ifBlank {
            if (playCanPlayerSpeak() || canPlayerSpeakNow || currentStatus == "waiting_player") "player" else scalarRuntimeText(turnState?.get("expectedRoleType")).ifBlank { "npc" }
          },
        )
        addProperty("updateTime", System.currentTimeMillis())
      }
      nextRows.add(row)
      while (nextRows.size() > runtimeChatStorageLimit) {
        nextRows.remove(0)
      }
    }
    val nextJson = nextRows.toString()
    if (nextJson == previousJson) return
    if (nextRows.size() == 0) {
      settingsStore.clearRuntimeChatTrace()
      VueTagLogger.info("runtime_chat", "toonflow.chat=[]")
      return
    }
    settingsStore.setRuntimeChatTraceJson(nextJson)
    VueTagLogger.info("runtime_chat", "toonflow.chat=${VueTagLogger.sanitize(nextJson, 4000)}")
  }

  private fun runtimeMiniGamePhaseLabel(gameType: String, phase: String, uiPhaseLabel: String): String {
    if (uiPhaseLabel.isNotBlank()) return uiPhaseLabel
    if (setOf("research_skill", "alchemy", "upgrade_equipment", "battle").contains(gameType)) {
      return when (phase) {
        "await_input" -> "等待方案"
        "result" -> "已评估"
        "settling" -> "已结束"
        else -> phase.ifBlank { "进行中" }
      }
    }
    if (gameType == "cultivation") {
      return when (phase) {
        "choose_practice" -> "选择修炼"
        "gather_qi" -> "修炼中"
        "breakthrough" -> "冲关"
        "settling" -> "已结束"
        else -> phase.ifBlank { "进行中" }
      }
    }
    return if (gameType == "fishing") {
      when (phase) {
        "prepare" -> "准备中"
        "waiting" -> "等待结果"
        "result" -> "本轮结束"
        "settling" -> "已结束"
        else -> phase.ifBlank { "进行中" }
      }
    } else {
      phase.ifBlank { "进行中" }
    }
  }

  private fun runtimeMiniGameStateItems(gameType: String, ui: JsonObject, publicState: JsonObject): List<RuntimeMiniGameStateItem> {
    val uiStateItems = ui.get("state_items")?.takeIf { it.isJsonArray }?.asJsonArray?.mapNotNull { item ->
      if (!item.isJsonObject) return@mapNotNull null
      val obj = item.asJsonObject
      val key = scalarRuntimeText(obj.get("key"))
      val value = scalarRuntimeText(obj.get("value"))
      if (key.isBlank() || value.isBlank()) return@mapNotNull null
      RuntimeMiniGameStateItem(key, value)
    }.orEmpty()
    if (uiStateItems.isNotEmpty()) return uiStateItems
    if (gameType == "fishing") {
      return listOf(
        RuntimeMiniGameStateItem("当前水域", scalarRuntimeText(publicState.get("site_name")).ifBlank { "当前水域" }),
        RuntimeMiniGameStateItem("当前状态", scalarRuntimeText(publicState.get("current_status")).ifBlank { "准备抛竿" }),
        RuntimeMiniGameStateItem("本轮结果", scalarRuntimeText(publicState.get("last_result")).ifBlank { "暂无" }),
        RuntimeMiniGameStateItem("最近收获", scalarRuntimeText(publicState.get("last_reward")).ifBlank { "暂无" }),
      )
    }
    if (gameType == "cultivation") {
      return listOf(
        RuntimeMiniGameStateItem("修炼目标", scalarRuntimeText(publicState.get("current_method")).ifBlank { "未选择" }),
        RuntimeMiniGameStateItem("陪练", scalarRuntimeText(publicState.get("mentor")).ifBlank { "未选择" }),
        RuntimeMiniGameStateItem("可修炼", runtimeStringify(publicState.get("available_practices")).ifBlank { "基础功法 / 基础体术 / 基础冥想" }),
        RuntimeMiniGameStateItem("最近奖励", scalarRuntimeText(publicState.get("last_reward")).ifBlank { "暂无" }),
      )
    }
    return publicState.entrySet()
      .mapNotNull { entry ->
        val value = runtimeStringify(entry.value).trim()
        if (value.isBlank()) null else RuntimeMiniGameStateItem(entry.key, value)
      }
      .take(10)
  }

  /**
   * 把小游戏 public_state 中的 enemy_list 映射成设置面板需要的敌人资料。
   * battle 规则本会把临时敌人的数值和头像一并写进 public_state，这里只做安全解析。
   */
  private fun runtimeMiniGameEnemies(gameType: String, publicState: JsonObject): List<RuntimeBattleEnemy> {
    if (gameType != "battle") return emptyList()
    val enemyArray = publicState.get("enemy_list")?.takeIf { it.isJsonArray }?.asJsonArray ?: return emptyList()
    return enemyArray.mapIndexedNotNull { index, item ->
      if (!item.isJsonObject) return@mapIndexedNotNull null
      val obj = item.asJsonObject
      val hp = runCatching { obj.get("hp")?.asInt ?: 0 }.getOrDefault(0)
      val maxHp = maxOf(runCatching { obj.get("maxHp")?.asInt ?: obj.get("max_hp")?.asInt ?: hp }.getOrDefault(hp), hp)
      val mp = runCatching { obj.get("mp")?.asInt ?: 0 }.getOrDefault(0)
      val maxMp = maxOf(runCatching { obj.get("maxMp")?.asInt ?: obj.get("max_mp")?.asInt ?: mp }.getOrDefault(mp), mp)
      val name = scalarRuntimeText(obj.get("name")).ifBlank { "敌人${index + 1}" }
      RuntimeBattleEnemy(
        enemyId = scalarRuntimeText(obj.get("enemyId")).ifBlank {
          scalarRuntimeText(obj.get("enemy_id")).ifBlank { "enemy_$index" }
        },
        name = name,
        description = scalarRuntimeText(obj.get("description")).ifBlank { "临时敌人" },
        level = runCatching { obj.get("level")?.asInt ?: 1 }.getOrDefault(1),
        hp = hp,
        maxHp = maxHp,
        mp = mp,
        maxMp = maxMp,
        avatarPath = scalarRuntimeText(obj.get("avatarPath")).ifBlank { scalarRuntimeText(obj.get("avatar_path")) },
        avatarBgPath = scalarRuntimeText(obj.get("avatarBgPath")).ifBlank { scalarRuntimeText(obj.get("avatar_bg_path")) },
        isRoleEnemy = runCatching { obj.get("isRoleEnemy")?.asBoolean ?: obj.get("is_role_enemy")?.asBoolean ?: false }.getOrDefault(false),
      )
    }
  }

  fun playRuntimeMiniGame(): RuntimeMiniGameView? {
    val root = runtimeStateRoot()?.getAsJsonObject("miniGame") ?: return null
    val session = root.getAsJsonObject("session") ?: JsonObject()
    val ui = root.getAsJsonObject("ui") ?: JsonObject()
    val status = scalarRuntimeText(session.get("status"))
    val gameType = scalarRuntimeText(session.get("game_type")).ifBlank { scalarRuntimeText(session.get("gameType")) }
    if (gameType.isBlank()) return null
    val pendingExit = session.get("pending_exit")?.asBoolean ?: false
    val acceptsTextInput = (ui.get("accepts_text_input")?.asBoolean == true) || setOf("research_skill", "alchemy", "upgrade_equipment", "battle").contains(gameType)
    val inputHint = scalarRuntimeText(ui.get("input_hint"))
    val visibleStatuses = setOf("preparing", "active", "settling", "suspended")
    if (!visibleStatuses.contains(status) && !pendingExit) return null
    val publicState = session.getAsJsonObject("public_state") ?: JsonObject()
    val stateItems = runtimeMiniGameStateItems(gameType, ui, publicState)
    val battleEnemies = runtimeMiniGameEnemies(gameType, publicState)
    return RuntimeMiniGameView(
      gameType = gameType,
      displayName = scalarRuntimeText(root.getAsJsonObject("rulebook")?.get("displayName")).ifBlank { gameType },
      status = status.ifBlank { "active" },
      phase = runtimeMiniGamePhaseLabel(gameType, scalarRuntimeText(session.get("phase")), scalarRuntimeText(ui.get("phase_label"))),
      round = runCatching { session.get("round")?.asInt ?: 0 }.getOrDefault(0),
      ruleSummary = scalarRuntimeText(ui.get("rule_summary")),
      narration = scalarRuntimeText(ui.get("narration")),
      pendingExit = pendingExit,
      acceptsTextInput = acceptsTextInput,
      inputHint = inputHint,
      stateItems = stateItems,
      battleEnemies = battleEnemies,
    )
  }

  fun playChapterConditionText(): String {
    return resolveVisibleChapterGoalText().ifBlank { "自由剧情" }
  }

  /**
   * 获取当前章节可展示目标。
   *
   * 章节列表接口有时不会返回 completionCondition，因此这里用运行时 chapterProgress 兜底，
   * 保证底部“当前目标”和 Web 端在恢复会话、listSession 场景下保持一致。
   */
  private fun resolveVisibleChapterGoalText(): String {
    val configuredGoal = normalizeConditionEditorText(playCurrentChapter()?.completionCondition)
    if (configuredGoal.isNotBlank()) return configuredGoal
    val progress = runtimeStateRoot()?.getAsJsonObject("chapterProgress")
    return scalarRuntimeText(progress?.get("pendingGoal"))
      .ifBlank { scalarRuntimeText(progress?.get("eventSummary")) }
  }

  /**
   * 底部“当前目标”只展示章节结束条件。
   *
   * 这样可以避免把当前事件摘要和章节目标混成一条，和 Web 的最新展示口径保持一致。
   */
  fun playVisibleChapterObjective(): String {
    val chapter = playCurrentChapter() ?: return ""
    if (chapter.showCompletionCondition == false) return ""
    // 结束条件为空时仍展示稳定目标，避免底部“当前目标”在自由剧情章节消失。
    return resolveVisibleChapterGoalText().ifBlank { "自由剧情" }
  }

  /**
   * 返回编排信息面板顶部要展示的“当前事件目标”。
   *
   * 规则：
   * - 优先显示当前事件摘要；
   * - 当前事件还没摘要时，回退到事件窗口里下一条可用摘要；
   * - 再没有时，最后回退到章节结束条件。
   */
  fun playCurrentEventTargetText(): String {
    val eventSummary = playCurrentRuntimeEventDigest()?.eventSummary.orEmpty()
      .takeIf { it.isNotBlank() && it != "当前事件摘要待生成" }
      .orEmpty()
    if (eventSummary.isNotBlank()) {
      return eventSummary
    }
    val nextSummary = playVisibleChapterEvents()
      .map { it.eventSummary.trim() }
      .firstOrNull { it.isNotBlank() && it != "当前事件摘要待生成" }
      .orEmpty()
    if (nextSummary.isNotBlank()) {
      return nextSummary
    }
    return playVisibleChapterObjective()
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
    val runtimePlayer = runtimeRoleSnapshot("player", "player")
    val runtimeNarrator = runtimeRoleSnapshot("narrator", "narrator")
    val roles = mutableListOf<StoryRole>()
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
      roles += mergeRoleSnapshot(player, runtimePlayer)
      roles += mergeRoleSnapshot(narrator, runtimeNarrator)
      roles += npcRoles.map { mergeRoleSnapshot(it, runtimeNpcSnapshot(it)) }
    } else {
      val world = sessionDetail?.world ?: return emptyList()
      world.playerRole?.let { roles.add(mergeRoleSnapshot(it, runtimePlayer)) }
      world.narratorRole?.let { roles.add(mergeRoleSnapshot(it, runtimeNarrator)) }
      world.settings?.roles?.filter { it.roleType == "npc" }?.forEach { roles.add(mergeRoleSnapshot(it, runtimeNpcSnapshot(it))) }
      if (roles.none { it.roleType == "player" } && runtimePlayer != null) {
        roles += runtimePlayer
      }
      if (roles.none { it.roleType == "narrator" } && runtimeNarrator != null) {
        roles += runtimeNarrator
      }
    }
    return roles
      .map(::resolveRoleMedia)
      .distinctBy { it.id.ifBlank { "${it.roleType}:${it.name}" } }
  }

  fun closeDebugDialog(backToCreate: Boolean) {
    debugEndDialog = null
    debugEndDialogDetail = ""
    if (backToCreate) {
      leaveDebugMode()
      activeTab = "创建"
    }
  }

  /**
   * 关闭正式游玩的章节失败弹窗。
   *
   * 用途：
   * - 只清理当前前端展示态，不改服务端会话状态；
   * - 这样再次进入同一会话时，storyInfo 仍会把失败弹窗重新带回来。
   */
  fun closeSessionEndDialog() {
    sessionEndDialog = null
    sessionEndDialogDetail = ""
  }

  fun leaveDebugMode() {
    clearRuntimeRetryState()
    clearDebugRevisitSnapshots()
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
    debugEndDialogDetail = ""
    debugChapterSequence = emptyList()
    debugMessageSeed = 1L
    currentSessionId = ""
    sessionDetail = null
    sessionEndDialog = null
    sessionEndDialogDetail = ""
    clearPlayChapterCache()
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

  fun updateAccountAvatarFromVideoUri(rawUri: String) {
    if (userId <= 0L) {
      notice = "请先登录后再设置头像"
      return
    }
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
    if (uri == null) {
      notice = "视频选择失败"
      return
    }

    viewModelScope.launch {
      accountAvatarProcessing = true
      try {
        runCatching {
          val saved = convertAvatarVideoToGif(uri, projectScoped = false)
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
          notice = "账号 GIF 头像已更新"
        }.onFailure {
          notice = "GIF 头像转换失败: ${it.message ?: "未知错误"}"
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

  fun updateStoryPlayerAvatarFromVideoUri(rawUri: String) {
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
    if (uri == null) {
      notice = "视频选择失败"
      return
    }

    viewModelScope.launch {
      storyPlayerAvatarProcessing = true
      try {
        runCatching {
          val saved = convertAvatarVideoToGif(uri, projectScoped = true)
          userAvatarPath = saved.foregroundPath
          userAvatarBgPath = saved.backgroundPath
          notice = "GIF 头像已更新"
        }.onFailure {
          notice = "GIF 头像转换失败: ${it.message ?: "未知错误"}"
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

  fun importRoleAvatarVideoGif(rawUri: String, onSaved: (String, String) -> Unit) {
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
    if (uri == null) {
      notice = "视频选择失败"
      return
    }
    viewModelScope.launch {
      roleAvatarProcessing = true
      try {
        runCatching {
          val saved = convertAvatarVideoToGif(uri, projectScoped = true)
          onSaved(saved.foregroundPath, saved.backgroundPath)
          notice = "GIF 头像已更新"
        }.onFailure {
          notice = "GIF 头像转换失败: ${it.message ?: "未知错误"}"
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

  private data class BinaryVideoSource(
    val bytes: ByteArray,
    val mime: String,
    val fileName: String,
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

  private suspend fun convertAvatarVideoToGif(uri: Uri, projectScoped: Boolean): SavedImagePaths {
    val source = readVideoSourceFromUri(uri)
    val result = repository.convertAvatarVideoToGif(
      projectId = if (projectScoped) selectedProjectId.takeIf { it > 0L } ?: error("请先选择项目后再上传 MP4") else null,
      base64Data = bytesToBase64Payload(source.bytes, source.mime),
      fileName = source.fileName.ifBlank { "avatar.mp4" },
      // 安卓端与 web 端保持一致，默认请求高质量 animated WebP。
      preferGif = false,
      onProgress = { notice = it },
    )
    val foregroundPath = resolveMediaPath(result.foregroundFilePath.ifBlank { result.foregroundPath })
    val backgroundPath = resolveMediaPath(result.backgroundFilePath.ifBlank { result.backgroundPath })
    if (foregroundPath.isBlank() || backgroundPath.isBlank()) {
      error("MP4 转 GIF 失败，未返回主体或背景图片")
    }
    return SavedImagePaths(
      foregroundPath = foregroundPath,
      backgroundPath = backgroundPath,
    )
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

  private fun normalizeVideoMime(displayName: String, rawMime: String): String {
    val mime = rawMime.trim().lowercase()
    if (mime == "video/mp4" || mime == "video/x-m4v" || mime == "video/quicktime") {
      return mime
    }
    return when (displayName.substringAfterLast('.', "").trim().lowercase()) {
      "m4v" -> "video/x-m4v"
      "mov" -> "video/quicktime"
      else -> "video/mp4"
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

  private fun readVideoSourceFromUri(uri: Uri): BinaryVideoSource {
    val app = getApplication<Application>()
    val resolver = app.contentResolver
    val displayName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
      if (nameIdx >= 0 && cursor.moveToFirst()) cursor.getString(nameIdx) else null
    }?.trim().orEmpty()
    val mime = normalizeVideoMime(displayName, resolver.getType(uri).orEmpty())
    if (mime != "video/mp4" && mime != "video/x-m4v" && mime != "video/quicktime") {
      error("仅支持上传 MP4 视频转换 GIF")
    }
    val bytes = resolver.openInputStream(uri).use { input ->
      if (input == null) error("无法读取所选视频")
      input.readBytes()
    }
    return BinaryVideoSource(
      bytes = bytes,
      mime = mime,
      fileName = displayName.ifBlank { "avatar.mp4" },
    )
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
    return worlds.filter { (it.chapterCount ?: 0) > 0 && isWorldPublished(it) }
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

  fun profileDisplayName(): String {
    return userNickname.ifBlank { userName }.ifBlank { "未登录" }
  }

  fun openSettingsPanel() {
    settingsPageMode = "settings"
    activeTab = "设置"
  }

  fun openProfileEditor() {
    if (token.isBlank()) {
      notice = "请先登录账号"
      openSettingsPanel()
      return
    }
    settingsPageMode = "profile"
    syncProfileEditorDrafts()
    activeTab = "设置"
  }

  fun closeSettingsPanel() {
    settingsPageMode = "settings"
    activeTab = "我的"
  }

  fun saveProfile() {
    if (token.isBlank()) {
      notice = "请先登录账号"
      openSettingsPanel()
      return
    }
    if (profileSaving) return
    viewModelScope.launch {
      profileSaving = true
      val nextNickname = profileNicknameDraft.trim()
      val nextIntro = profileIntroDraft.trim()
      runCatching {
        repository.saveUser(
          nickname = nextNickname,
          intro = nextIntro,
        )
        userNickname = nextNickname
        userIntro = nextIntro
        settingsStore.setProfileNickname(userId, nextNickname)
        settingsStore.setProfileIntro(userId, nextIntro)
        syncProfileEditorDrafts()
      }.onSuccess {
        notice = "资料已保存"
        closeSettingsPanel()
      }.onFailure {
        notice = "保存资料失败: ${it.message ?: "未知错误"}"
      }
      profileSaving = false
    }
  }

  fun reloadAll() {
    viewModelScope.launch {
      if (token.isBlank()) {
        resetRuntimeData()
        notice = "请先登录账号"
        openSettingsPanel()
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
        openSettingsPanel()
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
    return worlds.filter { item ->
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

  fun canEditWorld(world: WorldItem?): Boolean {
    if (world == null) return false
    return projects.any { it.id == world.projectId }
  }

  private fun safeText(value: String?): String = value.orEmpty()

  private fun readUserText(user: JsonObject, vararg keys: String): String {
    keys.forEach { key ->
      val value = runCatching { user.get(key)?.asString?.trim().orEmpty() }.getOrDefault("")
      if (value.isNotBlank()) return value
    }
    return ""
  }

  private fun syncProfileEditorDrafts() {
    profileNicknameDraft = userNickname
    profileIntroDraft = userIntro
  }

  private fun normalizeBaseUrlValue(value: String): String {
    return value.trim().trimEnd('/')
  }

  private fun isLocalOrPrivateHost(host: String): Boolean {
    val normalized = host.trim().trim('[', ']').lowercase(Locale.getDefault())
    if (normalized.isBlank()) return true
    if (normalized == "localhost" || normalized == "::1" || normalized.endsWith(".local")) return true
    val parts = normalized.split('.')
    if (parts.size != 4) return normalized == "0.0.0.0"
    val octets = parts.mapNotNull { it.toIntOrNull() }
    if (octets.size != 4) return false
    val first = octets[0]
    val second = octets[1]
    if (first == 0 || first == 10 || first == 127) return true
    if (first == 169 && second == 254) return true
    if (first == 192 && second == 168) return true
    if (first == 172 && second in 16..31) return true
    return false
  }

  private fun rewriteLoopbackMediaUrl(raw: String, base: String): String {
    if (base.isBlank()) return raw
    return runCatching {
      val source = URL(raw)
      if (!isLocalOrPrivateHost(source.host)) return raw
      val target = URL(if (base.endsWith("/")) base else "$base/")
      val targetPort = if (target.port >= 0) ":${target.port}" else ""
      buildString {
        append(target.protocol)
        append("://")
        append(target.host)
        append(targetPort)
        append(source.path)
        if (!source.query.isNullOrBlank()) {
          append('?')
          append(source.query)
        }
        if (!source.ref.isNullOrBlank()) {
          append('#')
          append(source.ref)
        }
      }
    }.getOrDefault(raw)
  }

  fun resolveMediaPath(value: String?): String {
    val raw = safeText(value).trim()
    if (raw.isBlank()) return ""
    if (raw.startsWith("/data/")) return raw
    if (raw.startsWith("content://", ignoreCase = true)) return raw
    if (raw.startsWith("file://", ignoreCase = true)) return raw
    if (raw.startsWith("data:", ignoreCase = true)) return raw
    val base = normalizeBaseUrlValue(baseUrl.ifBlank { settingsStore.baseUrl })
    if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
      return rewriteLoopbackMediaUrl(raw, base)
    }
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
      // 角色头像路径也要兜底成空字符串，避免上游反序列化出脏值后继续传染到 UI。
      avatarPath = resolveMediaPath(safeText(role.avatarPath)),
      avatarBgPath = resolveMediaPath(safeText(role.avatarBgPath)),
    )
  }

  fun worldPublishStatus(world: WorldItem): String {
    val topLevelStatus = safeText(world.publishStatus).trim().lowercase()
    if (topLevelStatus.isNotBlank()) {
      return topLevelStatus
    }
    return safeText(world.settings?.publishStatus).trim().lowercase().ifBlank { "draft" }
  }

  fun isWorldPublished(world: WorldItem): Boolean {
    return worldPublishStatus(world) == "published"
  }

  // “我的”页面要把发布中的故事归到已发布分区，但公共推荐和大厅只认真正 published。
  fun isWorldInPublishedLane(world: WorldItem): Boolean {
    return worldPublishStatus(world) in setOf("published", "publishing", "publish_failed")
  }

  fun worldPublishStatusLabel(world: WorldItem): String {
    return when (worldPublishStatus(world)) {
      "publishing" -> "发布中"
      "publish_failed" -> "发布失败"
      "published" -> "已发布"
      else -> "草稿"
    }
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
    val chapter = playCurrentChapter() ?: return ""
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

  fun runtimeMessageStatus(message: MessageItem): String {
    val meta = message.meta?.takeIf { it.isJsonObject }?.asJsonObject ?: return ""
    return meta.get("status")?.asString?.trim().orEmpty()
  }

  fun streamingSentenceTexts(message: MessageItem): List<String> {
    val meta = message.meta?.takeIf { it.isJsonObject }?.asJsonObject ?: return emptyList()
    val sentences = meta.getAsJsonArray("sentences") ?: return emptyList()
    return sentences.mapNotNull { item ->
      item?.takeIf { !it.isJsonNull }?.asString?.trim()?.takeIf { it.isNotBlank() }
    }
  }

  fun displayContentForMessage(message: MessageItem): String {
    return message.content.ifBlank {
      streamingSentenceTexts(message).joinToString("")
    }
  }

  fun loadingHintForMessage(message: MessageItem): String {
    if (!isStreamingRuntimeMessage(message)) return ""
    if (displayContentForMessage(message).isNotBlank()) return ""
    val speaker = displayNameForMessage(message)
    return when (runtimeMessageStatus(message)) {
      "orchestrated", "auto_advancing", "waiting_next" -> "$speaker 正在准备下一句..."
      "streaming", "revealing", "voicing" -> "$speaker 正在生成台词..."
      else -> "$speaker 正在生成内容..."
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
    val content = displayContentForMessage(message).trim()
    if (content.isBlank()) {
      notice = "这条对话没有可改写内容"
      return
    }
    if (message.roleType == "player") {
      sendText = content
      notice = "已填回原台词，可修改后重新发送"
      return
    }
    val speaker = displayNameForMessage(message)
    sendText = "请将“${content}”改写成更自然、符合${speaker}当前处境的一句话。"
    notice = "已把改写指令填入输入框"
  }

  fun canDeleteMessage(message: MessageItem): Boolean {
    if (isRuntimeRetryMessage(message) || isStreamingRuntimeMessage(message)) return false
    if (message.roleType != "player") return false
    return conversationMessages().lastOrNull()?.id == message.id
  }

  private fun applyAwaitUserTurnFromPlan(plan: DebugNarrativePlan?) {
    val currentPlan = plan ?: return
    val shouldYieldToUser = currentPlan.awaitUser
    if (!shouldYieldToUser || shouldStreamSessionPlanFromPlan(currentPlan)) return
    val detail = sessionDetail ?: return
    val root = detail.state?.takeIf { it.isJsonObject }?.asJsonObject?.deepCopy() ?: return
    val turnState = (root.getAsJsonObject("turnState") ?: JsonObject()).also { root.add("turnState", it) }
    val displayName = playerName.ifBlank { scalarRuntimeText(root.getAsJsonObject("player")?.get("name")).ifBlank { "用户" } }
    turnState.addProperty("canPlayerSpeak", true)
    turnState.addProperty("expectedRoleType", "player")
    turnState.addProperty("expectedRole", displayName)
    turnState.addProperty("lastSpeakerRoleType", currentPlan.roleType.ifBlank { turnState.get("lastSpeakerRoleType")?.asString.orEmpty() })
    turnState.addProperty("lastSpeaker", currentPlan.role.ifBlank { turnState.get("lastSpeaker")?.asString.orEmpty() })
    sessionDetail = detail.copy(state = root)
    val latest = messages.lastOrNull()
    if (latest != null && !isRuntimeRetryMessage(latest) && !isStreamingRuntimeMessage(latest)) {
      updateMessageById(latest.id) { current ->
        current.copy(
          meta = buildRuntimeStreamMeta(
            current = current.meta,
            status = "waiting_player",
            streaming = false,
            nextRole = displayName,
            nextRoleType = "player",
          ),
        )
      }
    }
    syncRuntimeChatTraceLog()
  }

  private fun restoreDebugPlayerTurnAfterDeletion() {
    val root = debugRuntimeState?.takeIf { it.isJsonObject }?.asJsonObject?.deepCopy() ?: JsonObject()
    val turnState = (root.getAsJsonObject("turnState") ?: JsonObject()).also { root.add("turnState", it) }
    val previous = conversationMessages().lastOrNull()
    val playerNode = root.getAsJsonObject("player")
    val currentPlayerName = playerNode?.get("name")?.asString?.trim().orEmpty().ifBlank { playerName.ifBlank { "用户" } }
    turnState.addProperty("canPlayerSpeak", true)
    turnState.addProperty("expectedRoleType", "player")
    turnState.addProperty("expectedRole", currentPlayerName)
    turnState.addProperty("lastSpeakerRoleType", previous?.roleType.orEmpty())
    turnState.addProperty("lastSpeaker", previous?.role.orEmpty())
    val round = runCatching { root.get("round")?.asLong ?: 0L }.getOrDefault(0L)
    root.addProperty("round", maxOf(0L, round - 1L))
    val recentEvents = root.getAsJsonArray("recentEvents")
    if (recentEvents != null && recentEvents.size() > 0) {
      recentEvents.remove(recentEvents.size() - 1)
    }
    debugRuntimeState = root
    updateDebugStatePreview()
  }

  fun deleteMessage(message: MessageItem) {
    if (!canDeleteMessage(message)) {
      notice = "当前只支持删除最后一条用户台词"
      return
    }
    viewModelScope.launch {
      runCatching {
        if (debugMode) {
          messages.removeAll { it.id == message.id }
          messageReactions.remove(messageUiKey(message))
          restoreDebugPlayerTurnAfterDeletion()
          loadDebugRevisitSnapshots()
          val validIds = messages.map { it.id }.toSet()
          val nextSnapshots = debugRevisitSnapshots.filter { validIds.contains(it.messageId) }.sortedBy { it.capturedAt }
          debugRevisitSnapshots.clear()
          debugRevisitSnapshots.addAll(nextSnapshots)
          persistDebugRevisitSnapshots()
        } else {
          if (currentSessionId.isBlank()) error("当前没有可删除的会话")
          repository.deleteMessage(currentSessionId, message.id)
          messageReactions.remove(messageUiKey(message))
          refreshCurrentSession()
        }
      }.onSuccess {
        notice = "已删除上一句用户台词"
      }.onFailure {
        throwIfCancellation(it)
        notice = "删除台词失败: ${it.message ?: "未知错误"}"
      }
    }
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

  private fun normalizeDebugIncomingMessages(incoming: List<MessageItem>, existing: List<MessageItem> = emptyList()): List<MessageItem> {
    val usedIds = existing.map { it.id }.filter { it > 0L }.toMutableSet()
    val existingStableCount = existing.size
    return incoming.mapIndexed { index, message ->
      var messageId = message.id
      if (messageId <= 0L || usedIds.contains(messageId)) {
        messageId = debugMessageSeed++
      }
      usedIds += messageId
      val revisitData = when {
        message.revisitData != null && !message.revisitData.isJsonNull -> message.revisitData
        else -> JsonObject().apply {
          addProperty("messageCount", existingStableCount + index + 1)
        }
      }
      message.copy(
        id = messageId,
        createTime = if (message.createTime > 0L) message.createTime else System.currentTimeMillis(),
        revisitData = revisitData,
      )
    }
  }

  fun isLocalPendingPlayerMessage(message: MessageItem): Boolean {
    if (message.roleType != "player" || message.id >= 0) return false
    val meta = message.meta?.takeIf { it.isJsonObject }?.asJsonObject ?: return false
    val status = meta.get("status")?.asString?.trim().orEmpty()
    return meta.get("kind")?.asString == "runtime_stream" && (status == "sending" || status == "error")
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
    return createStreamingMessage(plan, conversationMessages().size + 1)
  }

  private fun createStreamingMessage(plan: DebugNarrativePlan, lineIndex: Int): MessageItem {
    val now = System.currentTimeMillis()
    return MessageItem(
      id = now,
      role = plan.role.ifBlank { "旁白" },
      roleType = plan.roleType.ifBlank { "narrator" },
      eventType = plan.eventType.ifBlank { "on_streaming_reply" },
      // 编排接口一返回就先插入可见台词框，避免游玩界面在流式首包前完全空白。
      // 后续真正的 delta/done 到来时会覆盖这段占位文案。
      content = RUNTIME_STREAM_PLACEHOLDER_TEXT,
      createTime = now,
      meta = JsonObject().apply {
        addProperty("kind", "runtime_stream")
        addProperty("streaming", true)
        add("sentences", JsonArray())
        addProperty("lineIndex", lineIndex)
        addProperty("status", "orchestrated")
        addProperty("nextRole", plan.nextRole)
        addProperty("nextRoleType", plan.nextRoleType)
      },
    )
  }

  private fun recordDebugRevisitSnapshot(message: MessageItem?) {
    if (!debugMode || message == null || message.id <= 0L) return
    loadDebugRevisitSnapshots()
    val conversationId = debugRevisitConversationId()
    if (conversationId.isBlank()) return
    val nextSnapshot = DebugRevisitSnapshot(
      conversationId = conversationId,
      messageId = message.id,
      messageCount = normalizeDebugIncomingMessages(
        conversationMessages(messages.toList())
          .filterNot(::isStreamingRuntimeMessage)
          .map { it.copy(meta = it.meta?.deepCopy()) },
      ).size,
      chapterId = debugChapterId,
      chapterTitle = debugChapterTitle,
      endDialog = debugEndDialog,
      endDialogDetail = debugEndDialogDetail,
      capturedAt = System.currentTimeMillis(),
      state = debugRuntimeState?.deepCopy(),
      messages = normalizeDebugIncomingMessages(
        conversationMessages(messages.toList())
          .filterNot(::isStreamingRuntimeMessage)
          .map { it.copy(meta = it.meta?.deepCopy()) },
      ),
    )
    val nextSnapshots = debugRevisitSnapshots
      .filter { it.messageId != message.id }
      .toMutableList()
      .apply { add(nextSnapshot) }
      .sortedBy { it.capturedAt }
      .takeLast(5)
    debugRevisitSnapshots.clear()
    debugRevisitSnapshots.addAll(nextSnapshots)
    persistDebugRevisitSnapshots()
  }

  fun canRevisitDebugMessage(message: MessageItem): Boolean {
    if (!debugMode || message.id <= 0L || isRuntimeRetryMessage(message) || isStreamingRuntimeMessage(message)) return false
    return true
  }

  fun canRevisitSessionMessage(message: MessageItem): Boolean {
    if (debugMode || sessionViewMode == "playback" || currentSessionId.isBlank()) return false
    if (message.id <= 0L || isRuntimeRetryMessage(message) || isStreamingRuntimeMessage(message)) return false
    val revisitData = message.revisitData
    return revisitData != null && !revisitData.isJsonNull
  }

  fun revisitDebugMessage(messageId: Long) {
    if (!debugMode) {
      notice = "当前不是章节调试模式"
      return
    }
    val debugRuntimeKey = debugRuntimeState
      ?.takeIf { it.isJsonObject }
      ?.asJsonObject
      ?.get("debugRuntimeKey")
      ?.takeIf { !it.isJsonNull }
      ?.asString
      ?.trim()
      .orEmpty()
    if (debugRuntimeKey.isBlank()) {
      notice = "当前调试环境缺少回溯标识"
      return
    }
    val stableMessages = conversationMessages(messages.toList())
      .filterNot(::isRuntimeRetryMessage)
      .filterNot(::isStreamingRuntimeMessage)
    val targetMessage = stableMessages.firstOrNull { it.id == messageId }
    if (targetMessage == null) {
      notice = "没有找到这句台词的回溯位置"
      return
    }
    val revisitData = targetMessage.revisitData?.takeIf { it.isJsonObject }?.asJsonObject
    val targetIndex = stableMessages.indexOfFirst { it.id == messageId }
    val messageCount = revisitData?.get("messageCount")?.asLong?.toInt()
      ?: if (targetIndex >= 0) targetIndex + 1 else 0
    if (messageCount <= 0) {
      notice = "当前台词缺少回溯锚点"
      return
    }
    viewModelScope.launch {
      runCatching {
        repository.debugRevisitMessage(debugRuntimeKey, messageCount)
      }.onSuccess { result ->
        val backendMessages = normalizeDebugIncomingMessages(result.messages.map { it.copy(meta = it.meta?.deepCopy()) })
        if (backendMessages.isEmpty()) {
          error("后端未返回可恢复的调试消息")
        }
        if (result.chapterId != null && result.chapterId > 0L) {
          debugChapterId = result.chapterId
        }
        debugRuntimeState = result.state?.deepCopy()
        debugStatePreview = prettyGson.toJson(debugRuntimeState)
        refreshDebugStoryInfo(playCurrentChapter())
        debugLatestPlan = null
        debugEndDialog = null
        debugEndDialogDetail = ""
        messages.clear()
        messages.addAll(backendMessages)
        updateDebugStatePreview()
        syncRuntimeChatTraceLog()
        notice = "已回溯到这句台词，可继续调试编排"
      }.onFailure {
        throwIfCancellation(it)
        notice = "回溯失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun revisitSessionMessage(messageId: Long) {
    if (currentSessionId.isBlank()) {
      notice = "当前没有可回溯的会话"
      return
    }
    viewModelScope.launch {
      runCatching {
        repository.revisitMessage(currentSessionId, messageId)
        refreshCurrentSession()
      }.onSuccess {
        scheduleSessionNarrativeIfSystemTurn()
        notice = "已回溯到这句台词，可继续编排"
      }.onFailure {
        throwIfCancellation(it)
        notice = "回溯失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  private fun createLocalPendingPlayerMessage(content: String): MessageItem {
    val now = System.currentTimeMillis()
    return MessageItem(
      id = -now,
      role = playerName.ifBlank { "用户" },
      roleType = "player",
      eventType = "on_message",
      content = content,
      createTime = now,
      meta = buildRuntimeStreamMeta(
        current = null,
        status = "sending",
        streaming = false,
        lineIndex = conversationMessages().size + 1,
        nextRole = "",
        nextRoleType = "",
      ),
    )
  }

  private fun appendLocalPendingPlayerMessage(content: String): MessageItem {
    val nextMessages = conversationMessages(messages.toList()).toMutableList()
    val message = createLocalPendingPlayerMessage(content)
    nextMessages.add(message)
    messages.clear()
    messages.addAll(nextMessages)
    syncRuntimeChatTraceLog()
    return message
  }

  private fun removeLocalPendingPlayerMessage(messageId: Long) {
    val index = messages.indexOfFirst { it.id == messageId }
    if (index >= 0) {
      messages.removeAt(index)
      syncRuntimeChatTraceLog()
    }
  }

  private fun markLocalPendingPlayerMessageFailed(messageId: Long) {
    updateMessageById(messageId) { message ->
      message.copy(meta = buildRuntimeStreamMeta(message.meta, status = "error", streaming = false))
    }
    syncRuntimeChatTraceLog()
  }

  private fun commitLocalPendingPlayerMessage(messageId: Long) {
    updateMessageById(messageId) { message ->
      message.copy(meta = buildRuntimeStreamMeta(message.meta, status = "generated", streaming = false))
    }
    syncRuntimeChatTraceLog()
  }

  fun playerPendingStatusText(message: MessageItem): String {
    if (!isLocalPendingPlayerMessage(message)) return ""
    val status = runtimeMessageStatus(message)
    return if (status == "error") "发送失败" else "处理中..."
  }

  private fun buildRuntimeStreamMeta(
    current: JsonElement?,
    status: String? = null,
    streaming: Boolean? = null,
    lineIndex: Int? = null,
    nextRole: String? = null,
    nextRoleType: String? = null,
  ): JsonObject {
    val meta = current?.deepCopy()?.takeIf { it.isJsonObject }?.asJsonObject ?: JsonObject().apply {
      addProperty("kind", "runtime_stream")
      addProperty("streaming", false)
    }
    if (!meta.has("kind")) meta.addProperty("kind", "runtime_stream")
    if (!meta.has("streaming")) meta.addProperty("streaming", false)
    if (lineIndex != null && lineIndex > 0) meta.addProperty("lineIndex", lineIndex)
    if (status != null) meta.addProperty("status", status)
    if (streaming != null) meta.addProperty("streaming", streaming)
    if (nextRole != null) meta.addProperty("nextRole", nextRole)
    if (nextRoleType != null) meta.addProperty("nextRoleType", nextRoleType)
    return meta
  }

  fun setRuntimeMessageStatus(messageId: Long, status: String) {
    val turnState = runtimeTurnStateRoot()
    val canPlayerSpeakNow = turnState?.get("canPlayerSpeak")?.asBoolean ?: true
    updateMessageById(messageId) { current ->
      current.copy(
        meta = buildRuntimeStreamMeta(
          current.meta,
          status = status,
          nextRole = if (status == "waiting_player" || canPlayerSpeakNow) "用户" else turnState?.get("expectedRole")?.asString.orEmpty(),
          nextRoleType = if (status == "waiting_player" || canPlayerSpeakNow) "player" else turnState?.get("expectedRoleType")?.asString.orEmpty(),
        ),
      )
    }
    syncRuntimeChatTraceLog()
  }

  private fun messageIdentity(message: MessageItem): String {
    return "${message.id}_${message.createTime}"
  }

  private fun mergeConversationMessages(base: List<MessageItem>, incoming: List<MessageItem>): List<MessageItem> {
    val next = base.toMutableList()
    incoming.forEach { message ->
      val identity = messageIdentity(message)
      val index = next.indexOfFirst { messageIdentity(it) == identity }
      if (index >= 0) {
        next[index] = message
      } else {
        next += message
      }
    }
    return next
  }

  private fun normalizeSessionRuntimeMessage(
    message: MessageItem,
    lineIndex: Int,
    turnState: JsonObject?,
  ): MessageItem {
    if (message.roleType == "player" || isRuntimeRetryMessage(message)) return message
    val canPlayerSpeakNow = turnState?.get("canPlayerSpeak")?.asBoolean ?: true
    return message.copy(
      meta = buildRuntimeStreamMeta(
        current = message.meta,
        status = "generated",
        streaming = false,
        lineIndex = lineIndex,
        nextRole = if (canPlayerSpeakNow) "用户" else scalarRuntimeText(turnState?.get("expectedRole")),
        nextRoleType = if (canPlayerSpeakNow) "player" else scalarRuntimeText(turnState?.get("expectedRoleType")),
      ),
    )
  }

  private fun normalizeLoadedSessionDetail(detail: SessionDetail): SessionDetail {
    if (detail.messages.isEmpty()) return detail
    val turnState = sessionTurnStateRoot(detail)
    if (turnState == null || turnState.entrySet().isEmpty()) return detail
    val normalized = detail.messages.mapIndexed { index, message ->
      normalizeSessionRuntimeMessage(message, index + 1, turnState)
    }.toMutableList()
    val latestIndex = normalized.indexOfLast { !isRuntimeRetryMessage(it) }
    if (latestIndex >= 0) {
      val latest = normalized[latestIndex]
      val canPlayerSpeakNow = turnState.get("canPlayerSpeak")?.asBoolean ?: true
      normalized[latestIndex] =
        latest.copy(
          meta =
            buildRuntimeStreamMeta(
              current = latest.meta,
              status = if (canPlayerSpeakNow) "waiting_player" else "waiting_next",
              streaming = false,
              lineIndex = latestIndex + 1,
              nextRole = if (canPlayerSpeakNow) "用户" else scalarRuntimeText(turnState.get("expectedRole")).ifBlank { "当前角色" },
              nextRoleType = if (canPlayerSpeakNow) "player" else scalarRuntimeText(turnState.get("expectedRoleType")).ifBlank { "npc" },
            ),
        )
    }
    return detail.copy(messages = normalized)
  }

  private fun applySessionNarrativeResult(result: SessionNarrativeResult) {
    val existingDetail = sessionDetail
    val nextState = result.state ?: existingDetail?.state
    val turnState = nextState?.takeIf { it.isJsonObject }?.asJsonObject?.getAsJsonObject("turnState")
    val baseMessages = conversationMessages(messages.toList())
    val incomingMessages = buildList {
      result.message?.let(::add)
      addAll(result.generatedMessages)
    }
    val lineStart = baseMessages.size
    val normalizedIncoming = incomingMessages.mapIndexed { index, message ->
      normalizeSessionRuntimeMessage(message, lineStart + index + 1, turnState)
    }
    val mergedMessages = mergeConversationMessages(baseMessages, normalizedIncoming)
    sessionDetail = SessionDetail(
      sessionId = result.sessionId.ifBlank { existingDetail?.sessionId.orEmpty() },
      title = existingDetail?.title.orEmpty(),
      status = result.status.ifBlank { existingDetail?.status.orEmpty() },
      chapterId = result.chapterId ?: existingDetail?.chapterId,
      state = nextState,
      latestSnapshot = com.toonflow.game.data.SessionSnapshot(state = nextState),
      world = existingDetail?.world,
      chapter = result.chapter ?: existingDetail?.chapter,
      messages = mergedMessages,
    )
    messages.clear()
    messages.addAll(mergedMessages)
    syncRuntimeChatTraceLog()
    result.chapter?.let { chapter ->
      val index = playChapters.indexOfFirst { it.id == chapter.id }
      if (index >= 0) {
        playChapters[index] = chapter
      } else {
        playChapters.add(chapter)
      }
    }
    sessionRuntimeStage = ""
  }

  /**
   * 把独立 storyInfo 接口返回的正式会话运行信息覆盖到当前会话详情。
   *
   * 用途：
   * - 让故事设定、当前章节事件、事件窗口只依赖 `/game/storyInfo`；
   * - 避免继续吃 orchestration/streamlines 里附带的大杂烩状态。
   */
  private fun applySessionStoryInfoResult(result: StoryInfoResult) {
    val existingDetail = sessionDetail
    val mergedState = result.state?.deepCopy() ?: existingDetail?.state?.deepCopy()
    val mergedWorld = result.world ?: existingDetail?.world
    val mergedChapter = result.chapter ?: existingDetail?.chapter
    val mergedMessages = existingDetail?.messages ?: messages.toList()
    val nextEndDialog = result.endDialog?.trim()?.ifBlank { null }
    val nextEndDialogDetail = result.endDialogDetail.orEmpty().trim()
    sessionDetail = SessionDetail(
      sessionId = currentSessionId.ifBlank { existingDetail?.sessionId.orEmpty() },
      title = existingDetail?.title?.ifBlank { mergedWorld?.name.orEmpty() }.orEmpty(),
      status = result.status.ifBlank { existingDetail?.status.orEmpty() },
      endDialog = nextEndDialog,
      endDialogDetail = nextEndDialogDetail,
      chapterId = result.chapterId ?: mergedChapter?.id ?: existingDetail?.chapterId,
      state = mergedState,
      latestSnapshot = SessionSnapshot(state = mergedState?.deepCopy()),
      world = mergedWorld,
      chapter = mergedChapter,
      messages = mergedMessages,
      currentEventDigest = result.currentEventDigest,
      eventDigestWindow = result.eventDigestWindow,
      eventDigestWindowText = result.eventDigestWindowText,
    )
    sessionEndDialog = nextEndDialog
    sessionEndDialogDetail = nextEndDialogDetail
    mergedChapter?.let { chapter ->
      val index = playChapters.indexOfFirst { it.id == chapter.id }
      if (index >= 0) {
        playChapters[index] = chapter
      } else {
        playChapters.add(chapter)
      }
    }
  }

  /**
   * 主动刷新正式会话的故事运行信息。
   *
   * 用途：
   * - 在 initStory、orchestration、streamlines 完成后补齐最新 state；
   * - 统一让 UI 从 storyInfo 读取当前章节事件和故事设定。
   */
  private suspend fun refreshSessionStoryInfo(): StoryInfoResult? {
    val sessionId = currentSessionId.trim()
    if (sessionId.isBlank()) return null
    return runCatching {
      repository.storyInfo(sessionId = sessionId)
    }.onSuccess { result ->
      applySessionStoryInfoResult(result)
    }.getOrNull()
  }

  private fun applySessionOrchestrationResult(result: SessionOrchestrationResult) {
    val existingDetail = sessionDetail
    sessionDetail = SessionDetail(
      sessionId = result.sessionId.ifBlank { existingDetail?.sessionId.orEmpty() },
      title = existingDetail?.title.orEmpty(),
      status = result.status.ifBlank { existingDetail?.status.orEmpty() },
      chapterId = result.chapterId ?: existingDetail?.chapterId,
      state = existingDetail?.state,
      latestSnapshot = existingDetail?.latestSnapshot,
      world = existingDetail?.world,
      chapter = existingDetail?.chapter,
      messages = existingDetail?.messages ?: messages.toList(),
    )
    applyAwaitUserTurnFromPlan(result.plan)
    syncRuntimeChatTraceLog()
    sessionRuntimeStage = ""
  }

  /**
   * 把独立 storyInfo 接口返回的调试运行信息覆盖到当前调试状态。
   *
   * 用途：
   * - 让调试态的故事设定、当前章节事件、运行态预览都从 `/game/storyInfo` 刷新；
   * - 避免依赖 orchestration/streamlines 旧的大杂烩返回。
   */
  private fun applyDebugStoryInfoResult(result: StoryInfoResult, fallbackChapter: ChapterItem?) {
    result.state?.takeIf { !it.isJsonNull }?.deepCopy()?.let { nextState ->
      debugRuntimeState = nextState
    }
    if (result.chapterId != null && result.chapterId > 0L) {
      debugChapterId = result.chapterId
    }
    result.world?.let { world ->
      debugWorldName = world.name
      debugWorldIntro = world.intro
    }
    result.chapter?.let { storyChapter ->
      val index = chapters.indexOfFirst { it.id == storyChapter.id }
      if (index >= 0) {
        chapters[index] = storyChapter
      }
    }
    val activeChapter = chapters.firstOrNull { it.id == (result.chapterId ?: debugChapterId) } ?: result.chapter ?: fallbackChapter
    debugChapterTitle = normalizeChapterTitleLabel(
      result.chapterTitle.ifBlank { activeChapter?.title.orEmpty() },
      activeChapter?.sort ?: 0,
    )
    loadDebugRevisitSnapshots()
    updateDebugStatePreview()
  }

  /**
   * 主动刷新章节调试的故事运行信息。
   *
   * 用途：
   * - 在调试 introduction、orchestration、streamlines 完成后补齐最新 state；
   * - 让调试 UI 不再依赖旧接口附带的状态字段。
   */
  private suspend fun refreshDebugStoryInfo(fallbackChapter: ChapterItem? = null): StoryInfoResult? {
    if (worldId <= 0L) return null
    return runCatching {
      repository.storyInfo(
        worldId = worldId,
        chapterId = debugChapterId,
        state = debugRuntimeState,
      )
    }.onSuccess { result ->
      applyDebugStoryInfoResult(result, fallbackChapter)
    }.getOrNull()
  }

  private fun applyInitializedSessionDetail(world: WorldItem, result: StoryInitResult) {
    val chapterId = result.chapterId
    val chapterTitle = result.chapterTitle.trim()
    currentSessionId = result.sessionId.trim()
    sessionDetail = SessionDetail(
      sessionId = currentSessionId,
      title = world.name.ifBlank { "会话" },
      status = "active",
      chapterId = chapterId,
      state = result.state,
      latestSnapshot = SessionSnapshot(state = result.state),
      world = world,
      chapter = if (chapterId != null && chapterId > 0L) {
        ChapterItem(
          id = chapterId,
          title = chapterTitle.ifBlank { "第 ${chapterId} 章" },
        )
      } else null,
      messages = emptyList(),
      currentEventDigest = result.currentEventDigest,
      eventDigestWindow = result.eventDigestWindow,
      eventDigestWindowText = result.eventDigestWindowText,
    )
    messages.clear()
    sessionRuntimeStage = ""
    sessionOpenError = ""
    syncRuntimeChatTraceLog()
  }

  private fun shouldStreamSessionPlanFromPlan(plan: DebugNarrativePlan?): Boolean {
    if (plan == null) return false
    return plan.role.isNotBlank() && !plan.roleType.trim().equals("player", ignoreCase = true)
  }

  /**
   * 判断编排结果是否要求客户端先显式初始化下一章节。
   */
  private fun isInitChapterCommand(command: SessionChapterCommand?): Boolean {
    if (command == null) return false
    return command.type.trim() == "init_chapter" && command.chapterId > 0L
  }

  /**
   * 调用显式章节初始化接口，并刷新当前会话的故事运行信息。
   */
  private suspend fun initCurrentSessionChapter(command: SessionChapterCommand) {
    val sessionId = currentSessionId.trim()
    if (sessionId.isBlank()) return
    repository.initChapter(sessionId, command.chapterId)
    refreshSessionStoryInfo()
  }

  private fun clearRuntimeRetryMessage() {
    val nextMessages = conversationMessages()
    if (nextMessages.size == messages.size) return
    messages.clear()
    messages.addAll(nextMessages)
    syncRuntimeChatTraceLog()
  }

  private fun clearRuntimeRetryState() {
    runtimeRetryTask = null
    runtimeRetryMessageText = ""
    clearRuntimeRetryMessage()
  }

  private fun throwIfCancellation(error: Throwable) {
    if (error is CancellationException) {
      throw error
    }
  }

  private fun runtimeUiErrorMessage(error: Throwable?, fallback: String = "未知错误"): String {
    val text = (error?.message ?: "").trim()
    if (text.isBlank()) return fallback
    if (Regex("^(编排师|角色发言|记忆管理)对接的模型异常[:：]").containsMatchIn(text)) {
      return text
    }
    val normalized = text.lowercase(Locale.getDefault())
    return if (
      normalized.contains("insufficient account balance")
      || normalized.contains("insufficient_balance")
      || normalized.contains("insufficient_user_quota")
      || normalized.contains("quota exceeded")
      || normalized.contains("余额不足")
      || normalized.contains("额度不足")
      || normalized.contains("配额不足")
      || normalized.contains("剩余额度")
    ) {
      "当前模型余额不足，请充值或切换模型。"
    } else {
      text
    }
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
    syncRuntimeChatTraceLog()
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
        throwIfCancellation(err)
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
        mixVoices = settings?.narratorVoiceMixVoices ?: safeRoleMixVoices(narratorRole).ifEmpty { narratorVoiceMixVoices },
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
    return ""
  }

  private fun buildEditorChapterSnapshot(): ChapterItem? {
    if (!hasCurrentChapterDraft()) return null
    val sort = currentChapterSort()
    val title = normalizeChapterTitleLabel(chapterTitle, sort).ifBlank { "第 $sort 章" }
    // 自动生成开启时由后端重建运行模板，不把旧的手写 JSON 再送回保存接口。
    val runtimeOutline = if (chapterRuntimeOutlineAutoGenerate) null else parseRuntimeOutlineEditorText(chapterRuntimeOutlineText)
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
      bgmAutoPlay = chapterMusicAutoPlay,
      showCompletionCondition = chapterConditionVisible,
      runtimeOutline = runtimeOutline,
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
      chapterMusicAutoPlay = true
      chapterConditionVisible = true
      chapterRuntimeOutlineAutoGenerate = true
      chapterRuntimeOutlineText = ""
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
    chapterMusicAutoPlay = extra?.musicAutoPlay ?: chapter.bgmAutoPlay
    chapterConditionVisible = extra?.conditionVisible ?: chapter.showCompletionCondition
    chapterRuntimeOutlineAutoGenerate = true
    chapterRuntimeOutlineText = normalizeRuntimeOutlineEditorText(chapter.runtimeOutline)
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
    // 新章节默认允许在调试和正式游玩时自动播放章节背景音乐。
    chapterMusicAutoPlay = true
    chapterConditionVisible = true
    chapterRuntimeOutlineAutoGenerate = true
    chapterRuntimeOutlineText = ""
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
      // 把章节背景音乐自动播放配置和其他章节附加配置一起持久化，避免切换章节后丢失。
      musicAutoPlay = chapterMusicAutoPlay,
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

  private fun worldStatusModeForPublish(publish: Boolean?): SaveWorldStatusMode {
    return when (publish) {
      true -> SaveWorldStatusMode.PUBLISHED
      false -> SaveWorldStatusMode.DRAFT
      else -> SaveWorldStatusMode.PRESERVE
    }
  }

  private fun resolveTargetWorldStatus(statusMode: SaveWorldStatusMode): String {
    return when (statusMode) {
      SaveWorldStatusMode.PUBLISHED -> "published"
      SaveWorldStatusMode.DRAFT -> "draft"
      SaveWorldStatusMode.PRESERVE -> worldPublishStatus.ifBlank { "draft" }
    }
  }

  private fun logStoryFlow(message: String) {
    VueTagLogger.info("story_flow", message)
  }

  private suspend fun saveWorldInternal(statusMode: SaveWorldStatusMode = SaveWorldStatusMode.PRESERVE): WorldItem {
    val safeWorldName = prepareWorldNameForSave(statusMode == SaveWorldStatusMode.PUBLISHED)
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
          voiceMixVoices = role.voiceMixVoices.orEmpty(),
          sample = role.sample,
        ),
      )
    }
    val targetStatus = resolveTargetWorldStatus(statusMode)
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
    payload.addProperty("publishStatus", targetStatus)
    logStoryFlow("saveWorld request worldId=${worldId.takeIf { it > 0L } ?: 0L} projectId=$selectedProjectId targetStatus=$targetStatus")
    val saved = repository.saveWorld(payload)
    worldId = saved.id
    worldPublishStatus = safeText(saved.publishStatus).ifBlank {
      safeText(saved.settings?.publishStatus).ifBlank { targetStatus }
    }
    logStoryFlow("saveWorld success worldId=${saved.id} status=$worldPublishStatus")
    return saved
  }

  private suspend fun saveEditorChapterInternal(targetStatus: String): ChapterItem? {
    if (worldId <= 0L || !hasCurrentChapterDraft()) {
      logStoryFlow("saveChapter skipped worldId=$worldId hasDraft=${hasCurrentChapterDraft()}")
      return null
    }
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
      // 明确把章节背景音乐自动播放开关发给后端，旧章节缺字段时后端会按默认开启处理。
      addProperty("bgmAutoPlay", chapterMusicAutoPlay)
      addProperty("showCompletionCondition", chapterConditionVisible)
      parseChapterCondition(chapterEntryCondition)?.let { add("entryCondition", it) }
      parseChapterCondition(chapterCondition)?.let { add("completionCondition", it) }
      parseRuntimeOutlineEditorText(chapterRuntimeOutlineText)?.let { add("runtimeOutline", it) }
      addProperty("sort", currentSort)
      addProperty("status", if (targetStatus == "published") "published" else "draft")
    }
    logStoryFlow("saveChapter request worldId=$worldId chapterId=${currentId ?: 0L} sort=$currentSort status=${if (targetStatus == "published") "published" else "draft"}")
    val saved = repository.saveChapter(payload)
    upsertChapterExtra(saved.id, saved.sort)
    selectedChapterId = saved.id
    logStoryFlow("saveChapter success chapterId=${saved.id} sort=${saved.sort} status=${saved.status}")
    return saved
  }

  private fun applySavedChapterToEditor(saved: ChapterItem?) {
    if (saved == null) return
    val index = chapters.indexOfFirst { it.id == saved.id }
    if (index >= 0) {
      chapters[index] = saved
    } else {
      chapters.add(saved)
    }
  }

  private suspend fun refreshStoryData() {
    logStoryFlow("refreshStoryData start")
    loadWorlds()
    loadSessions()
    refreshRecommendation()
    logStoryFlow("refreshStoryData done worlds=${worlds.size} sessions=${sessions.size}")
  }

  fun openWorldForEdit(world: WorldItem) {
    if (!canEditWorld(world)) {
      notice = "只能编辑自己的故事"
      return
    }
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
    if (!canEditWorld(world)) {
      notice = "只能编辑自己的故事"
      return
    }
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
        saveWorldInternal(SaveWorldStatusMode.DRAFT)
        refreshStoryData()
        loadWorldEditor(world.id)
        activeTab = "创建"
        notice = "已转回草稿箱"
      }.onFailure {
        notice = "转回草稿失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  /**
   * 复制发布故事为全新的草稿副本，并直接进入副本编辑页。
   * 副本和原故事完全独立，后续修改不会影响原故事资源和发布态。
   */
  fun copyPublishedWorldAsDraft(world: WorldItem) {
    if (!canEditWorld(world)) {
      notice = "只能复制自己的故事"
      return
    }
    if (selectedProjectId != world.projectId) {
      selectedProjectId = world.projectId
    }
    viewModelScope.launch {
      runCatching {
        val copied = repository.copyWorld(world.id)
        refreshStoryData()
        loadWorldEditor(copied.id) ?: error("未找到复制后的故事")
        activeTab = "创建"
        notice = "已复制为草稿"
      }.onFailure {
        notice = "复制故事失败: ${it.message ?: "未知错误"}"
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
        saveWorldInternal(worldStatusModeForPublish(publish))
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
      if (publish == true) {
        storyPublishPending = true
        notice = "发布中，正在生成角色参数和章节快照..."
      }
      runCatching {
        val worldStatusMode = worldStatusModeForPublish(publish)
        val targetWorldStatus = resolveTargetWorldStatus(worldStatusMode)
        val targetChapterStatus = if (targetWorldStatus == "published") "published" else "draft"
        logStoryFlow(
          "saveStoryEditor start publish=$publish mode=$worldStatusMode worldId=$worldId selectedChapterId=${selectedChapterId ?: 0L} worldStatus=$worldPublishStatus hasDraft=${hasCurrentChapterDraft()}",
        )
        if (worldId <= 0L) {
          saveWorldInternal(SaveWorldStatusMode.DRAFT)
        }
        val savedChapter = saveEditorChapterInternal(targetChapterStatus)
        saveWorldInternal(worldStatusMode)
        refreshStoryData()
        when {
          startNextDraft -> beginNewChapterDraft()
          savedChapter != null -> selectChapter(savedChapter.id)
        }
        primeStoryEditorPersistState()
        if (!successNotice.isNullOrBlank()) {
          notice = successNotice
        }
        logStoryFlow("saveStoryEditor success worldId=$worldId worldStatus=$worldPublishStatus savedChapterId=${savedChapter?.id ?: 0L}")
      }.onFailure {
        if (publish == true) {
          runCatching {
            if (worldId > 0L) {
              loadWorldEditor(worldId)
            }
            refreshStoryData()
          }
        }
        VueTagLogger.error("story_flow", "saveStoryEditor failed publish=$publish error=${VueTagLogger.throwableMessage(it)}", it)
        notice = "保存失败: ${it.message ?: "未知错误"}"
      }.also {
        if (publish == true) {
          storyPublishPending = false
        }
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
        saveWorldInternal(SaveWorldStatusMode.PRESERVE)
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
          saveWorldInternal(SaveWorldStatusMode.PRESERVE)
          val savedChapter = saveEditorChapterInternal(worldPublishStatus.ifBlank { "draft" })
          if (savedChapter != null) {
            saveWorldInternal(SaveWorldStatusMode.PRESERVE)
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
        throwIfCancellation(it)
        debugLoading = false
        debugLoadingStage = ""
        activeTab = "游玩"
        showRuntimeRetryMessage("进入调试失败: ${runtimeUiErrorMessage(it)}") {
          performDebugCurrentChapter()
        }
      }
    }
  }

  private suspend fun performDebugCurrentChapter() {
    debugMode = true
    debugLoading = true
    debugLoadingStage = "正在进入调试界面..."
    debugEndDialog = null
    currentSessionId = "debug_${System.currentTimeMillis()}"
    debugSessionTitle = "调试：${worldName.ifBlank { "未命名故事" }}"
    debugWorldName = worldName
    debugWorldIntro = worldIntro
    debugRuntimeState = JsonObject()
    debugLatestPlan = null
    debugMessageSeed = System.currentTimeMillis()
    clearDebugRevisitSnapshots()
    messages.clear()
    sessionRuntimeStage = ""
    sessionDetail = null
    sendText = ""
    activeTab = "游玩"
    notice = "进入调试中..."
    runtimeProcessingPending = true
    var debugOverlayReleased = false
    fun releaseDebugLoading() {
      if (debugOverlayReleased) return
      debugLoading = false
      debugLoadingStage = ""
      debugOverlayReleased = true
    }
    try {
      debugLoadingStage = "正在保存草稿..."
      saveWorldInternal(SaveWorldStatusMode.PRESERVE)
      val savedChapter = saveEditorChapterInternal(worldPublishStatus.ifBlank { "draft" })
      applySavedChapterToEditor(savedChapter)
      primeStoryEditorPersistState()
      debugLoadingStage = "正在初始化调试环境..."

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
      debugLatestPlan = null
      debugEndDialog = null
      debugMessageSeed = 1L
      currentSessionId = "debug_${System.currentTimeMillis()}"
      sessionDetail = null
      messages.clear()
      sendText = ""
      sessionRuntimeStage = "初始化章节"
      val initResult = repository.initDebug(
        worldId = worldId,
        chapterId = startChapter.id,
        state = debugRuntimeState,
        messages = emptyList(),
      )

      releaseDebugLoading()
      debugRuntimeState = initResult.state?.deepCopy() ?: JsonObject()
      debugChapterId = initResult.chapterId
      debugChapterTitle = initResult.chapterTitle.ifBlank { normalizeChapterTitleLabel(startChapter.title, startChapter.sort) }
      refreshDebugStoryInfo(startChapter)

      sessionRuntimeStage = "生成开场白"
      val introResult = repository.debugIntroduction(
        worldId = worldId,
        chapterId = initResult.chapterId ?: startChapter.id,
        state = debugRuntimeState,
        messages = emptyList(),
      )
      applyDebugOrchestrationResult(introResult, startChapter)
      refreshDebugStoryInfo(startChapter)
      if (introResult.plan != null && shouldStreamDebugPlan(introResult.plan)) {
        streamDebugPlan(introResult.plan, emptyList(), null)
      }
      if (sessionRuntimeStage == "生成开场白") {
        sessionRuntimeStage = ""
      }

      clearRuntimeRetryState()
      sessionRuntimeStage = "准备剧情编排"
      val result = repository.debugOrchestration(
        worldId = worldId,
        chapterId = initResult.chapterId ?: startChapter.id,
        state = debugRuntimeState,
        messages = conversationMessages(messages.toList()),
      )
      if (result.plan != null) {
        applyDebugOrchestrationResult(result, startChapter)
        refreshDebugStoryInfo(startChapter)
        if (shouldStreamDebugPlan(result.plan)) {
          sessionRuntimeStage = "生成首轮内容"
          streamDebugPlan(result.plan, emptyList(), null)
          if (sessionRuntimeStage == "生成首轮内容") {
            sessionRuntimeStage = ""
          }
        }
        if (shouldAutoContinueDebugAfterStart(result)) {
          sessionRuntimeStage = "推进到用户回合"
          continueDebugNarrative()
          if (sessionRuntimeStage == "推进到用户回合") {
            sessionRuntimeStage = ""
          }
        }
      } else {
        messages.clear()
      }
      updateDebugStatePreview()
      activeTab = "游玩"
      notice = "已进入章节调试模式（仅调试缓存，不会持久化）"
    } finally {
      debugLoading = false
      debugLoadingStage = ""
      runtimeProcessingPending = false
      if (
        sessionRuntimeStage == "初始化章节"
        || sessionRuntimeStage == "生成开场白"
        || sessionRuntimeStage == "准备剧情编排"
        || sessionRuntimeStage == "生成首轮内容"
        || sessionRuntimeStage == "推进到用户回合"
      ) {
        sessionRuntimeStage = ""
      }
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

  private suspend fun openResolvedSession(
    sessionId: String,
    playback: Boolean = false,
    playbackIndex: Int = 0,
    firstMessage: String = "",
    resumeLatest: Boolean = false,
  ) {
    sessionViewMode = if (playback) "playback" else "live"
    sessionPlaybackStartIndex = playbackIndex.coerceAtLeast(0)
    sessionResumeLatestOnOpen = resumeLatest && !playback
    currentSessionId = sessionId
    sessionOpenError = ""
    sessionOpeningStage = if (playback) "同步回放进度" else "同步游戏进度"
    notice = if (playback) "正在同步回放进度..." else "正在同步游戏进度..."
    refreshCurrentSession()
    if (!playback) {
      scheduleSessionNarrativeIfSystemTurn()
    }
    if (firstMessage.isNotBlank()) {
      performSessionPlayerMessage(sessionId, firstMessage)
    }
    runCatching {
      loadSessions()
    }.onFailure {
      throwIfCancellation(it)
      sessionListError = it.message ?: "会话列表刷新失败"
    }
  }

  fun startFromWorld(world: WorldItem, firstMessage: String = "") {
    if (debugMode) leaveDebugMode()
    activeTab = "游玩"
    sessionOpening = true
    sessionOpeningStage = "正在初始化故事..."
    sessionOpenError = ""
    sessionRuntimeStage = ""
    sessionResumeLatestOnOpen = false
    sessionDetail = null
    messages.clear()
    viewModelScope.launch {
      notice = "正在进入故事..."
      runCatching {
        val existingSessionId = sessions.firstOrNull { it.worldId == world.id }?.sessionId?.trim().orEmpty()
          .ifBlank {
            runCatching { repository.listSession(worldId = world.id) }
              .getOrElse {
                throwIfCancellation(it)
                sessionListError = it.message ?: "会话列表加载失败"
                emptyList()
              }
              .firstOrNull()
              ?.sessionId
              ?.trim()
              .orEmpty()
          }
        if (existingSessionId.isNotBlank()) {
          sessionOpeningStage = "正在继续上次故事..."
          notice = "正在继续上次故事..."
          openResolvedSession(existingSessionId, playback = false, playbackIndex = 0, firstMessage = firstMessage, resumeLatest = true)
          return@runCatching
        }
        if ((world.chapterCount ?: 0) <= 0) {
          error("该故事还没有章节，暂时无法游玩")
        }
        sessionOpeningStage = "正在初始化故事..."
        notice = "正在初始化故事..."
        val initResult = repository.initStory(
          worldId = world.id,
          projectId = world.projectId,
          title = world.name.ifBlank { "会话" },
          skipOpening = false,
        )
        applyInitializedSessionDetail(world, initResult)
        refreshSessionStoryInfo()
        sessionOpening = false
        sessionOpeningStage = ""

        // 正式游玩也拆成独立开场白请求，再进入第一章编排，避免继续把计划塞回 initStory。
        val openingResult = repository.introduceStory(currentSessionId)
        if (openingResult.plan != null) {
          applySessionOrchestrationResult(openingResult)
          refreshSessionStoryInfo()
          if (shouldStreamSessionPlanFromPlan(openingResult.plan)) {
            sessionRuntimeStage = "播放开场白"
            streamSessionIntroductionPlan(openingResult, emptyList())
            if (sessionRuntimeStage == "播放开场白") {
              sessionRuntimeStage = ""
            }
          }
        }

        val firstChapterResult = repository.orchestrateSession(currentSessionId)
        if (firstChapterResult.plan != null) {
          val history = conversationMessages(messages.toList())
          applySessionOrchestrationResult(
            firstChapterResult.copy(
              currentEventDigest = sessionDetail?.currentEventDigest ?: firstChapterResult.currentEventDigest,
              eventDigestWindow = sessionDetail?.eventDigestWindow ?: firstChapterResult.eventDigestWindow,
              eventDigestWindowText = sessionDetail?.eventDigestWindowText ?: firstChapterResult.eventDigestWindowText,
            ),
          )
          refreshSessionStoryInfo()
          if (shouldStreamSessionPlanFromPlan(firstChapterResult.plan)) {
            sessionRuntimeStage = "生成第一章内容"
            streamSessionPlan(firstChapterResult, history)
            if (sessionRuntimeStage == "生成第一章内容") {
              sessionRuntimeStage = ""
            }
          }
        }

        runCatching {
          loadSessions()
        }.onFailure {
          throwIfCancellation(it)
          sessionListError = it.message ?: "会话列表刷新失败"
        }
        if (firstMessage.isNotBlank()) {
          performSessionPlayerMessage(currentSessionId, firstMessage)
        }
      }.onFailure {
        val message = it.message ?: "未知错误"
        sessionOpenError = message
        notice = "进入游玩失败: $message"
      }.also {
        sessionOpening = false
        sessionOpeningStage = ""
      }
    }
  }

  fun continueSessionForWorld(worldId: Long, fallbackSessionId: String = "", playback: Boolean = false, playbackIndex: Int = 0) {
    if (debugMode) leaveDebugMode()
    clearRuntimeRetryState()
    activeTab = "游玩"
    sessionOpening = true
    sessionOpeningStage = if (playback) "正在加载回放..." else "正在继续上次故事..."
    sessionOpenError = ""
    sessionRuntimeStage = ""
    sessionDetail = null
    messages.clear()
    viewModelScope.launch {
      notice = if (playback) "正在打开剧情回放..." else "正在进入故事..."
      runCatching {
        val resolvedSessionId = sessions.firstOrNull { it.worldId == worldId }?.sessionId?.trim().orEmpty()
          .ifBlank {
            runCatching { repository.listSession(worldId = worldId) }
              .getOrElse {
                throwIfCancellation(it)
                sessionListError = it.message ?: "会话列表加载失败"
                emptyList()
              }
              .firstOrNull()
              ?.sessionId
              ?.trim()
              .orEmpty()
          }
          .ifBlank { fallbackSessionId.trim() }
        if (resolvedSessionId.isBlank()) error("未找到会话")
        openResolvedSession(
          resolvedSessionId,
          playback = playback,
          playbackIndex = playbackIndex,
          resumeLatest = !playback,
        )
      }.onFailure {
        val message = it.message ?: "未知错误"
        sessionOpenError = message
        notice = "打开会话失败: $message"
      }.also {
        sessionOpening = false
        sessionOpeningStage = ""
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

  fun openSession(sessionId: String, playback: Boolean = false, playbackIndex: Int = 0) {
    if (debugMode) leaveDebugMode()
    clearRuntimeRetryState()
    notice = if (playback) "正在打开剧情回放..." else "正在进入故事..."
    sessionViewMode = if (playback) "playback" else "live"
    sessionPlaybackStartIndex = playbackIndex.coerceAtLeast(0)
    sessionResumeLatestOnOpen = !playback
    currentSessionId = sessionId
    activeTab = "游玩"
    sessionOpening = true
    sessionOpeningStage = if (playback) "读取回放数据" else "读取记忆与角色参数"
    sessionOpenError = ""
    sessionEndDialog = null
    sessionEndDialogDetail = ""
    notice = if (playback) "正在读取回放数据..." else "正在读取记忆与角色参数..."
    sessionRuntimeStage = ""
    sessionDetail = null
    messages.clear()
    viewModelScope.launch {
      runCatching {
        openResolvedSession(sessionId, playback = playback, playbackIndex = playbackIndex, resumeLatest = !playback)
      }.onFailure {
        val message = it.message ?: "未知错误"
        sessionOpenError = message
        notice = "打开会话失败: $message"
      }.also {
        sessionOpening = false
        sessionOpeningStage = ""
      }
    }
  }

  fun retryOpenCurrentSession() {
    val sessionId = currentSessionId.trim()
    if (sessionId.isBlank()) {
      notice = "当前没有可重试的会话"
      return
    }
    openSession(
      sessionId = sessionId,
      playback = sessionViewMode == "playback",
      playbackIndex = sessionPlaybackStartIndex,
    )
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

  fun deleteSession(sessionId: String) {
    val normalized = sessionId.trim()
    if (normalized.isBlank()) return
    viewModelScope.launch {
      runCatching {
        repository.deleteSession(normalized)
        sessions.removeAll { it.sessionId == normalized }
        if (currentSessionId == normalized) {
          clearRuntimeRetryState()
          currentSessionId = ""
          sessionDetail = null
          messages.clear()
          sessionViewMode = "live"
          sessionPlaybackStartIndex = 0
          activeTab = "聊过"
        }
      }.onFailure {
        notice = "删除会话失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  suspend fun refreshPlaySessionNow(showNoticeOnFailure: Boolean = true): Boolean {
    if (debugMode) {
      updateDebugStatePreview()
      return true
    }
    return runCatching {
      val before = playSessionRefreshFingerprint()
      refreshCurrentSession()
      before != playSessionRefreshFingerprint()
    }.getOrElse {
      throwIfCancellation(it)
      if (showNoticeOnFailure) {
        notice = "刷新会话失败: ${it.message ?: "未知错误"}"
      }
      false
    }
  }

  fun sendMessage() {
    val sid = currentSessionId
    val text = sendText.trim()
    if (sid.isBlank() || text.isBlank() || sendPending || runtimeProcessingPending) {
      logStoryFlow("sendMessage skipped sessionId=${sid.trim()} textBlank=${text.isBlank()} pending=$sendPending runtimePending=$runtimeProcessingPending")
      return
    }

    if (debugMode) {
      sendDebugMessage(text)
      return
    }

    clearRuntimeRetryState()
    logStoryFlow("sendMessage request sessionId=$sid text=${VueTagLogger.sanitize(text, 240)}")
    val optimistic = appendLocalPendingPlayerMessage(text)
    sendText = ""
    sendPending = true
    viewModelScope.launch {
      runCatching {
        performSessionPlayerMessage(sid, text, optimistic.id)
      }.onFailure {
        val message = it.message ?: "未知错误"
        if (message.contains("当前还没轮到用户发言") || message.contains("HTTP 409")) {
          removeLocalPendingPlayerMessage(optimistic.id)
          sendText = text
          notice = message
          refreshPlaySessionNow(showNoticeOnFailure = false)
        } else {
          markLocalPendingPlayerMessageFailed(optimistic.id)
          sendText = text
          notice = "发送失败: $message"
        }
      }.also {
        sendPending = false
      }
    }
  }

  private suspend fun loadUser() {
    runCatching {
      val user = repository.getUser()
      userName = readUserText(user, "name", "username", "userName")
      userId = user.get("id")?.asLong ?: 0L
      val remoteNickname = readUserText(user, "nickname", "nickName", "displayName")
      val remoteIntro = readUserText(user, "intro", "description", "bio", "profile")
      // 资料字段优先取服务端，服务端暂未回传时退回本地缓存，避免编辑后立即丢失。
      userNickname = remoteNickname.ifBlank { settingsStore.getProfileNickname(userId) }
      userIntro = remoteIntro.ifBlank { settingsStore.getProfileIntro(userId) }
      settingsStore.setProfileNickname(userId, userNickname)
      settingsStore.setProfileIntro(userId, userIntro)
      syncProfileEditorDrafts()
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
      userNickname = ""
      userIntro = ""
      userId = 0L
      syncProfileEditorDrafts()
      accountAvatarPath = ""
      accountAvatarBgPath = ""
      userAvatarPath = ""
      userAvatarBgPath = ""
    }
  }

  private suspend fun loadWorlds() {
    val rows = repository.listWorlds(includePublicPublished = true)
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
    val rows = runCatching { repository.listSession(null) }
      .onFailure {
        throwIfCancellation(it)
        sessionListError = it.message ?: "会话列表加载失败"
      }
      .getOrElse { return }
      .groupBy { it.worldId }
      .mapNotNull { (_, group) -> group.maxByOrNull { item -> item.updateTime } }
      .sortedByDescending { it.updateTime }
    sessionListError = ""
    sessions.clear()
    sessions.addAll(rows)
  }

  private suspend fun refreshCurrentSession() {
    if (currentSessionId.isBlank()) return
    val detail = normalizeLoadedSessionDetail(repository.getSession(currentSessionId))
    clearRuntimeRetryState()
    sessionDetail = detail
    refreshPlayChapterCache(detail)
    messages.clear()
    if (detail.messages.isNotEmpty()) {
      messages.addAll(detail.messages)
    } else {
      val turnState = detail.state?.takeIf { it.isJsonObject }?.asJsonObject?.getAsJsonObject("turnState")
      val loadedMessages = repository.getMessages(currentSessionId).mapIndexed { index, message ->
        normalizeSessionRuntimeMessage(message, index + 1, turnState)
      }.toMutableList()
      val latestIndex = loadedMessages.indexOfLast { !isRuntimeRetryMessage(it) }
      if (latestIndex >= 0 && turnState != null) {
        val latest = loadedMessages[latestIndex]
        val canPlayerSpeakNow = turnState.get("canPlayerSpeak")?.asBoolean ?: true
        loadedMessages[latestIndex] =
          latest.copy(
            meta =
              buildRuntimeStreamMeta(
                current = latest.meta,
                status = if (canPlayerSpeakNow) "waiting_player" else "waiting_next",
                streaming = false,
                lineIndex = latestIndex + 1,
                nextRole = if (canPlayerSpeakNow) "用户" else scalarRuntimeText(turnState.get("expectedRole")).ifBlank { "当前角色" },
                nextRoleType = if (canPlayerSpeakNow) "player" else scalarRuntimeText(turnState.get("expectedRoleType")).ifBlank { "npc" },
              ),
          )
      }
      sessionDetail = detail.copy(messages = loadedMessages)
      messages.addAll(loadedMessages)
    }
    refreshSessionStoryInfo()
    syncRuntimeChatTraceLog()
  }

  fun playSessionRefreshFingerprint(): String {
    val latest = conversationMessages().lastOrNull()
    val latestDisplayContent = latest?.let(::displayContentForMessage).orEmpty()
    val turnState = runtimeTurnStateRoot()
    return buildString {
      append(currentSessionId.trim())
      append('|').append(playSessionStatus())
      append('|').append(playRuntimeChapterId() ?: 0L)
      append('|').append(turnState?.get("canPlayerSpeak")?.asBoolean ?: true)
      append('|').append(scalarRuntimeText(turnState?.get("expectedRole")))
      append('|').append(messages.size)
      append('|').append(latest?.id ?: 0L)
      append('|').append(latest?.createTime ?: 0L)
      append('|').append(latest?.roleType.orEmpty())
      append('|').append(latestDisplayContent.trim())
    }
  }

  private enum class DebugChapterResult {
    Continue,
    Success,
    Failed,
  }

  private suspend fun performSessionPlayerMessage(sessionId: String, text: String, optimisticMessageId: Long? = null) {
    sessionRuntimeStage = "提交用户发言"
    try {
      val result = repository.addPlayerMessage(sessionId, playerName.ifBlank { "用户" }, text)
      if (optimisticMessageId != null) {
        removeLocalPendingPlayerMessage(optimisticMessageId)
      }
      sendText = ""
      clearRuntimeRetryState()
      applySessionNarrativeResult(result)
      refreshSessionStoryInfo()
      continueSessionNarrative()
      viewModelScope.launch {
        runCatching { loadSessions() }.onFailure {
          throwIfCancellation(it)
          sessionListError = it.message ?: "会话列表刷新失败"
        }
      }
    } finally {
      if (sessionRuntimeStage == "提交用户发言") {
        sessionRuntimeStage = ""
      }
    }
  }

  private suspend fun streamSessionPlan(orchestration: SessionOrchestrationResult, historyMessages: List<MessageItem>) {
    val sessionId = currentSessionId.trim()
    val plan = orchestration.plan ?: return
    if (sessionId.isBlank()) return
    val placeholder = createStreamingMessage(plan, historyMessages.size + 1)
    messages.clear()
    messages.addAll(historyMessages)
    messages.add(placeholder)
    syncRuntimeChatTraceLog()
    var done = false
    var accumulated = ""
    var finalMessage: JsonObject? = null
    logStoryFlow("streamSessionPlan start sessionId=$sessionId role=${plan.role} roleType=${plan.roleType}")
    repository.streamSessionLines(
      sessionId = sessionId,
      plan = plan,
    ) { event ->
      when (event.get("type")?.asString.orEmpty()) {
        "delta" -> {
          val text = event.getAsJsonObject("data")?.get("text")?.asString.orEmpty()
          if (text.isBlank()) return@streamSessionLines
          accumulated += text
          updateMessageById(placeholder.id) { current ->
            // 直接覆盖累计正文，避免把“获取台词中”占位文案拼进真实台词。
            current.copy(content = accumulated)
          }
        }

        "sentence" -> {
          val text = event.getAsJsonObject("data")?.get("text")?.asString.orEmpty().trim()
          if (text.isBlank()) return@streamSessionLines
          updateMessageById(placeholder.id) { current ->
            val meta = buildRuntimeStreamMeta(current.meta, status = "streaming", streaming = true)
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
          finalMessage = data?.getAsJsonObject("message")
          val finalContent = finalMessage?.get("content")?.asString ?: data?.get("content")?.asString.orEmpty()
          updateMessageById(placeholder.id) { current ->
            current.copy(
              role = finalMessage?.get("role")?.asString ?: current.role,
              roleType = finalMessage?.get("roleType")?.asString ?: current.roleType,
              eventType = finalMessage?.get("eventType")?.asString ?: current.eventType,
              content = finalContent,
              meta = buildRuntimeStreamMeta(current.meta, status = "generated", streaming = false),
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
    val committedContent = finalMessage?.get("content")?.asString ?: accumulated
    val committedCreateTime = finalMessage?.get("createTime")?.asLong ?: System.currentTimeMillis()
    val committedRole = finalMessage?.get("role")?.asString ?: placeholder.role.ifBlank { "旁白" }
    val committedRoleType = finalMessage?.get("roleType")?.asString ?: placeholder.roleType.ifBlank { "narrator" }
    val committedEventType = finalMessage?.get("eventType")?.asString ?: placeholder.eventType.ifBlank { "on_streaming_reply" }
    val committed = repository.commitNarrativeTurn(
      sessionId = sessionId,
      role = committedRole,
      roleType = committedRoleType,
      eventType = committedEventType,
      content = committedContent,
      createTime = committedCreateTime,
    )
    logStoryFlow("streamSessionPlan committed sessionId=$sessionId content=${VueTagLogger.sanitize(committedContent, 240)}")
    messages.clear()
    messages.addAll(historyMessages)
    syncRuntimeChatTraceLog()
    applySessionNarrativeResult(committed)
    refreshSessionStoryInfo()
  }

  /**
   * 估算 opening 至少应停留的展示时长。
   *
   * 用途：
   * - 静音时至少停 2 秒，避免开场白刚出现就被第一章正文顶掉；
   * - 自动语音开启时，按字数估算一段接近朗读时长的等待窗口，让 opening 先完整播完再进正文。
   */
  private fun estimateOpeningPresentationMs(text: String): Long {
    val normalized = text.trim()
    if (normalized.isBlank()) return 2000L
    if (!autoVoiceEnabled()) return 2000L
    val estimated = normalized.length * 180L + 1200L
    return estimated.coerceIn(2000L, 12000L)
  }

  /**
   * 开场白专用流。
   *
   * 用途：
   * - opening 必须直接播放章节写死文案，不能再复用普通台词流的 speaker 改写链；
   * - 提交成功后额外等待一个 opening 展示窗口，避免一闪而过。
   */
  private suspend fun streamSessionIntroductionPlan(orchestration: SessionOrchestrationResult, historyMessages: List<MessageItem>) {
    val sessionId = currentSessionId.trim()
    val plan = orchestration.plan ?: return
    if (sessionId.isBlank()) return
    val placeholder = createStreamingMessage(plan, historyMessages.size + 1)
    messages.clear()
    messages.addAll(historyMessages)
    messages.add(placeholder)
    syncRuntimeChatTraceLog()
    var done = false
    var accumulated = ""
    var finalMessage: JsonObject? = null
    logStoryFlow("streamSessionIntroductionPlan start sessionId=$sessionId role=${plan.role} roleType=${plan.roleType}")
    repository.streamSessionIntroductionLines(
      sessionId = sessionId,
      plan = plan,
    ) { event ->
      when (event.get("type")?.asString.orEmpty()) {
        "delta" -> {
          val text = event.getAsJsonObject("data")?.get("text")?.asString.orEmpty()
          if (text.isBlank()) return@streamSessionIntroductionLines
          accumulated += text
          updateMessageById(placeholder.id) { current ->
            current.copy(content = accumulated)
          }
        }

        "sentence" -> {
          val text = event.getAsJsonObject("data")?.get("text")?.asString.orEmpty().trim()
          if (text.isBlank()) return@streamSessionIntroductionLines
          updateMessageById(placeholder.id) { current ->
            val meta = buildRuntimeStreamMeta(current.meta, status = "streaming", streaming = true)
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
          finalMessage = data?.getAsJsonObject("message")
          val finalContent = finalMessage?.get("content")?.asString ?: data?.get("content")?.asString.orEmpty()
          updateMessageById(placeholder.id) { current ->
            current.copy(
              role = finalMessage?.get("role")?.asString ?: current.role,
              roleType = finalMessage?.get("roleType")?.asString ?: current.roleType,
              eventType = finalMessage?.get("eventType")?.asString ?: current.eventType,
              content = finalContent,
              meta = buildRuntimeStreamMeta(current.meta, status = "generated", streaming = false),
            )
          }
        }

        "error" -> {
          val message = event.getAsJsonObject("data")?.get("message")?.asString.orEmpty().ifBlank { "开场白流播放失败" }
          updateMessageById(placeholder.id) { null }
          error(message)
        }
      }
    }
    if (!done) {
      updateMessageById(placeholder.id) { null }
      error("开场白流未正常结束")
    }
    val committedContent = finalMessage?.get("content")?.asString ?: accumulated
    val committedCreateTime = finalMessage?.get("createTime")?.asLong ?: System.currentTimeMillis()
    val committedRole = finalMessage?.get("role")?.asString ?: placeholder.role.ifBlank { "旁白" }
    val committedRoleType = finalMessage?.get("roleType")?.asString ?: placeholder.roleType.ifBlank { "narrator" }
    val committedEventType = finalMessage?.get("eventType")?.asString ?: placeholder.eventType.ifBlank { "on_opening" }
    val committed = repository.commitNarrativeTurn(
      sessionId = sessionId,
      role = committedRole,
      roleType = committedRoleType,
      eventType = committedEventType,
      content = committedContent,
      createTime = committedCreateTime,
    )
    logStoryFlow("streamSessionIntroductionPlan committed sessionId=$sessionId content=${VueTagLogger.sanitize(committedContent, 240)}")
    messages.clear()
    messages.addAll(historyMessages)
    syncRuntimeChatTraceLog()
    applySessionNarrativeResult(committed)
    refreshSessionStoryInfo()
    delay(estimateOpeningPresentationMs(committedContent))
  }

  fun retryFailedPlayerMessage(messageId: Long) {
    val target = messages.firstOrNull { it.id == messageId } ?: return
    if (!isLocalPendingPlayerMessage(target) || runtimeMessageStatus(target) != "error") return
    if (sendPending || runtimeProcessingPending) return
    val content = target.content.trim()
    if (content.isBlank()) return
    updateMessageById(messageId) { message ->
      message.copy(meta = buildRuntimeStreamMeta(message.meta, status = "sending", streaming = false))
    }
    clearRuntimeRetryState()
    sendText = ""
    sendPending = true
    viewModelScope.launch {
      runCatching {
        if (debugMode) {
          performDebugPlayerMessage(content, appendPlayerMessage = false, optimisticMessageId = messageId)
        } else {
          performSessionPlayerMessage(currentSessionId, content, messageId)
        }
      }.onFailure {
        throwIfCancellation(it)
        markLocalPendingPlayerMessageFailed(messageId)
        sendText = content
        notice = "发送失败: ${runtimeUiErrorMessage(it)}"
      }.also {
        sendPending = false
      }
    }
  }

  fun rewriteFailedPlayerMessage(messageId: Long) {
    val target = messages.firstOrNull { it.id == messageId } ?: return
    if (!isLocalPendingPlayerMessage(target)) return
    sendText = target.content.trim()
    removeLocalPendingPlayerMessage(messageId)
  }

  private suspend fun performContinueSessionNarrative() {
    if (currentSessionId.isBlank()) return
    sessionRuntimeStage = "继续编排下一轮剧情"
    try {
      var advanced = false
      for (attempt in 0 until 3) {
        val beforeCount = conversationMessages().size
        val history = conversationMessages()
        val orchestration = repository.orchestrateSession(currentSessionId)
        clearRuntimeRetryState()
        applySessionOrchestrationResult(orchestration)
        logStoryFlow(
          "continueSession orchestration sessionId=$currentSessionId planRole=${orchestration.plan?.role.orEmpty()} nextRole=${orchestration.plan?.nextRole.orEmpty()} status=${orchestration.status}",
        )
        val plan = orchestration.plan
        val shouldInitNextChapter = isInitChapterCommand(orchestration.command)
        val shouldYieldToUser = plan?.awaitUser == true
        val shouldStreamPlan = shouldStreamSessionPlanFromPlan(plan)
        refreshSessionStoryInfo()
        if (!shouldStreamPlan) {
          // storyInfo 可能还没同步到最新 turnState；刷新后再补一次本地 awaitUser，
          // 避免界面和调试面板仍停在 NPC/旁白回合。
          applyAwaitUserTurnFromPlan(plan)
        }
        if (shouldStreamPlan) {
          streamSessionPlan(orchestration, history)
        }
        if (shouldInitNextChapter) {
          orchestration.command?.let { initCurrentSessionChapter(it) }
          advanced = true
          continue
        }
        val afterCount = conversationMessages().size
        val latest = conversationMessages().lastOrNull()
        val latestStatus = latest?.let(::runtimeMessageStatus).orEmpty()
        val canPlayerSpeakNow = playCanPlayerSpeak()
        if (afterCount > beforeCount || canPlayerSpeakNow || latestStatus == "waiting_player" || plan == null || shouldYieldToUser) {
          advanced = true
          break
        }
      }
      if (!advanced) {
        error("自动推进没有产出新内容")
      }
      viewModelScope.launch {
        runCatching { loadSessions() }.onFailure {
          throwIfCancellation(it)
          sessionListError = it.message ?: "会话列表刷新失败"
        }
      }
    } finally {
      if (sessionRuntimeStage == "继续编排下一轮剧情") {
        sessionRuntimeStage = ""
      }
    }
  }

  suspend fun continueSessionNarrative(): Boolean {
    if (currentSessionId.isBlank()) return false
    clearRuntimeRetryState()
    runtimeProcessingPending = true
    return runCatching {
      performContinueSessionNarrative()
      true
    }.getOrElse {
      throwIfCancellation(it)
      showRuntimeRetryMessage("继续剧情失败: ${runtimeUiErrorMessage(it)}") {
        performContinueSessionNarrative()
      }
      false
    }.also {
      runtimeProcessingPending = false
    }
  }

  private fun sendDebugMessage(text: String) {
    val chapter = playCurrentChapter()
    if (chapter == null) {
      notice = "调试章节不存在，请返回重新选择。"
      updateDebugStatePreview()
      return
    }
    if (sendPending || runtimeProcessingPending) return
    clearRuntimeRetryState()
    val optimistic = appendLocalPendingPlayerMessage(text)
    sendText = ""
    sendPending = true
    viewModelScope.launch {
      runCatching {
        performDebugPlayerMessage(text, appendPlayerMessage = false, optimisticMessageId = optimistic.id)
      }.onFailure {
        throwIfCancellation(it)
        markLocalPendingPlayerMessageFailed(optimistic.id)
        sendText = text
        notice = "调试发送失败: ${runtimeUiErrorMessage(it)}"
        updateDebugStatePreview()
      }.also {
        sendPending = false
      }
    }
  }

  private suspend fun performDebugPlayerMessage(text: String, appendPlayerMessage: Boolean, optimisticMessageId: Long? = null) {
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
    applyLocalDebugPlayerProfile(text)
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
    refreshDebugStoryInfo(playCurrentChapter())
    if (optimisticMessageId != null) {
      commitLocalPendingPlayerMessage(optimisticMessageId)
    }
    if (result.plan != null && shouldStreamDebugPlan(result.plan)) {
      sessionRuntimeStage = "继续编排下一轮剧情"
      streamDebugPlanOrFallback(result.plan, conversationMessages(), text)
      refreshDebugStoryInfo(playCurrentChapter())
      if (sessionRuntimeStage == "继续编排下一轮剧情") {
        sessionRuntimeStage = ""
      }
    }
    updateDebugStatePreview()
  }

  suspend fun continueDebugNarrative(): Boolean {
    if (!debugMode || worldId <= 0L) return false
    if (continueDebugNarrativeRunning) return false
    continueDebugNarrativeRunning = true
    clearRuntimeRetryState()
    runtimeProcessingPending = true
    return runCatching {
      performContinueDebugNarrative()
      true
    }.getOrElse {
      throwIfCancellation(it)
      showRuntimeRetryMessage("调试推进失败: ${runtimeUiErrorMessage(it)}") {
        performContinueDebugNarrative()
      }
      updateDebugStatePreview()
      false
    }.also {
      runtimeProcessingPending = false
      continueDebugNarrativeRunning = false
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
      refreshDebugStoryInfo(playCurrentChapter())
      val shouldYieldToUser = shouldYieldToUserFromDebugPlan(result.plan)
      if (result.plan != null && shouldStreamDebugPlan(result.plan)) {
        streamDebugPlanOrFallback(result.plan, history, null)
        refreshDebugStoryInfo(playCurrentChapter())
      }
      updateDebugStatePreview()
      val canSpeak = debugCanPlayerSpeakFromState()
      if (conversationMessages().size > beforeCount || debugEndDialog != null || canSpeak || shouldYieldToUser) {
        advanced = true
        break
      }
    }
    if (!advanced) {
      error("自动推进没有产出新内容")
    }
  }

  private fun applyDebugOrchestrationResult(result: com.toonflow.game.data.DebugOrchestrationResult, fallbackChapter: ChapterItem?) {
    result.state?.takeIf { !it.isJsonNull }?.deepCopy()?.let { nextState ->
      debugRuntimeState = nextState
    }
    debugLatestPlan = result.plan
    if (result.chapterId != null && result.chapterId > 0L) {
      debugChapterId = result.chapterId
    }
    val activeChapter = chapters.firstOrNull { it.id == (result.chapterId ?: debugChapterId) } ?: fallbackChapter
    debugChapterTitle = normalizeChapterTitleLabel(result.chapterTitle.ifBlank { activeChapter?.title.orEmpty() }, activeChapter?.sort ?: 0)
    debugEndDialog = result.endDialog
    debugEndDialogDetail = result.endDialogDetail.orEmpty()
    loadDebugRevisitSnapshots()
    updateDebugStatePreview()
  }

  private fun applyDebugStepResult(result: DebugStepResult, historyMessages: List<MessageItem>, fallbackChapter: ChapterItem?) {
    result.state?.takeIf { !it.isJsonNull }?.deepCopy()?.let { nextState ->
      debugRuntimeState = nextState
    }
    debugLatestPlan = null
    if (result.chapterId != null && result.chapterId > 0L) {
      debugChapterId = result.chapterId
    }
    val activeChapter = chapters.firstOrNull { it.id == (result.chapterId ?: debugChapterId) } ?: fallbackChapter
    debugChapterTitle = normalizeChapterTitleLabel(result.chapterTitle.ifBlank { activeChapter?.title.orEmpty() }, activeChapter?.sort ?: 0)
    debugEndDialog = result.endDialog
    debugEndDialogDetail = result.endDialogDetail.orEmpty()
    val appendedMessages = normalizeDebugIncomingMessages(
      DebugSessionHeuristics.stripKnownHistory(historyMessages, result.messages),
      historyMessages,
    )
    messages.clear()
    messages.addAll(historyMessages)
    messages.addAll(appendedMessages)
    loadDebugRevisitSnapshots()
    appendedMessages.forEach(::recordDebugRevisitSnapshot)
    updateDebugStatePreview()
  }

  private fun applyDebugStreamState(data: JsonObject?, fallbackChapter: ChapterItem?) {
    if (data == null) return
    val nextState = data.get("state")
    if (nextState == null || nextState.isJsonNull) return
    debugRuntimeState = nextState
    val nextChapterId = data.get("chapterId")?.takeIf { !it.isJsonNull }?.asLong
    if (nextChapterId != null && nextChapterId > 0L) {
      debugChapterId = nextChapterId
    }
    val activeChapter = chapters.firstOrNull { it.id == (nextChapterId ?: debugChapterId) } ?: fallbackChapter
    val nextChapterTitle = data.get("chapterTitle")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
    debugChapterTitle = normalizeChapterTitleLabel(nextChapterTitle.ifBlank { activeChapter?.title.orEmpty() }, activeChapter?.sort ?: 0)
    debugEndDialog = data.get("endDialog")?.takeIf { !it.isJsonNull }?.asString
    debugEndDialogDetail = data.get("endDialogDetail")?.takeIf { !it.isJsonNull }?.asString.orEmpty()
    loadDebugRevisitSnapshots()
    updateDebugStatePreview()
  }

  private fun debugCanPlayerSpeakFromState(state: JsonElement? = debugRuntimeState): Boolean {
    return try {
      val turnState = state?.takeIf { it.isJsonObject }?.asJsonObject?.getAsJsonObject("turnState")
      turnState?.get("canPlayerSpeak")?.asBoolean ?: true
    } catch (_: Throwable) {
      true
    }
  }

  private fun shouldYieldToUserFromDebugPlan(plan: DebugNarrativePlan?): Boolean {
    if (plan == null) return false
    return plan.awaitUser
  }

  private fun shouldStreamDebugPlan(plan: DebugNarrativePlan?): Boolean {
    if (plan == null) return false
    return plan.role.isNotBlank() && !plan.roleType.trim().equals("player", ignoreCase = true)
  }

  /**
   * 判断调试计划是否属于固定 opening 文案。
   *
   * 用途：
   * - opening 是作者写死的，不该再走普通 streamlines 的 speaker 改写链；
   * - 调试首开这里也要切到 `/game/streamlines/introduction`；
   * - 后续正文台词仍然沿用原来的 debug streamlines。
   */
  private fun shouldUseDebugIntroductionStream(plan: DebugNarrativePlan?): Boolean {
    if (plan == null) return false
    val eventType = plan.eventType.orEmpty().trim()
    val presetContent = plan.presetContent.orEmpty().trim()
    return eventType.equals("on_opening", ignoreCase = true) && presetContent.isNotEmpty()
  }

  // 调试首次进入如果仍未轮到用户，继续自动补到真正可交互的节点，避免 opening 后半路被页面 watcher 抢跑。
  private fun shouldAutoContinueDebugAfterStart(result: DebugOrchestrationResult): Boolean {
    val plan = result.plan ?: return false
    if (result.endDialog != null) return false
    if (debugCanPlayerSpeakFromState(debugRuntimeState)) return false
    if (shouldYieldToUserFromDebugPlan(plan)) return false
    val eventType = plan.eventType.trim()
    return eventType == "on_opening" || !plan.presetContent.isNullOrBlank()
  }

  private suspend fun streamDebugPlanOrFallback(
    plan: DebugNarrativePlan,
    historyMessages: List<MessageItem>,
    playerContent: String?,
  ) {
    runCatching {
      streamDebugPlan(plan, historyMessages, playerContent)
    }.getOrElse { streamError ->
      throwIfCancellation(streamError)
      val fallbackResult = repository.debugStep(
        worldId = worldId,
        chapterId = debugChapterId,
        state = debugRuntimeState,
        messages = historyMessages,
        playerContent = playerContent,
      )
      clearRuntimeRetryState()
      applyDebugStepResult(fallbackResult, historyMessages, playCurrentChapter())
      refreshDebugStoryInfo(playCurrentChapter())
      val advanced = conversationMessages().size > historyMessages.size || debugEndDialog != null || debugCanPlayerSpeakFromState(fallbackResult.state)
      if (!advanced) {
        throw streamError
      }
    }
  }

  private suspend fun streamDebugPlan(plan: DebugNarrativePlan, historyMessages: List<MessageItem>, playerContent: String?) {
    val useIntroductionStream = shouldUseDebugIntroductionStream(plan)
    val placeholder = createStreamingMessage(plan, historyMessages.size + 1)
    messages.clear()
    messages.addAll(historyMessages)
    messages.add(placeholder)
    var done = false
    var accumulated = ""
    val streamHandler: suspend ((JsonObject) -> Unit) -> Unit = { onEvent ->
      if (useIntroductionStream) {
        repository.streamDebugIntroductionLines(
          worldId = worldId,
          chapterId = debugChapterId,
          state = debugRuntimeState,
          messages = historyMessages,
          plan = plan,
          onEvent = onEvent,
        )
      } else {
        repository.streamDebugLines(
          worldId = worldId,
          chapterId = debugChapterId,
          state = debugRuntimeState,
          messages = historyMessages,
          playerContent = playerContent,
          plan = plan,
          onEvent = onEvent,
        )
      }
    }
    streamHandler event@ { event ->
      when (event.get("type")?.asString.orEmpty()) {
        "delta" -> {
          val text = event.getAsJsonObject("data")?.get("text")?.asString.orEmpty()
          if (text.isBlank()) return@event
          accumulated += text
          updateMessageById(placeholder.id) { current ->
            // 调试链同样直接覆盖累计正文，避免占位文案残留到最终内容前面。
            current.copy(content = accumulated)
          }
        }

        "sentence" -> {
          val text = event.getAsJsonObject("data")?.get("text")?.asString.orEmpty().trim()
          if (text.isBlank()) return@event
          updateMessageById(placeholder.id) { current ->
            val meta = buildRuntimeStreamMeta(current.meta, status = "streaming", streaming = true)
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
          applyDebugStreamState(data, playCurrentChapter())
          recordDebugRevisitSnapshot(
            MessageItem(
              id = placeholder.id,
              role = message?.get("role")?.asString ?: placeholder.role,
              roleType = message?.get("roleType")?.asString ?: placeholder.roleType,
              eventType = message?.get("eventType")?.asString ?: placeholder.eventType,
              content = finalContent,
              createTime = placeholder.createTime,
              meta = JsonObject().apply {
                addProperty("kind", "runtime_stream")
                addProperty("streaming", false)
                addProperty("status", if (debugCanPlayerSpeakFromState()) "waiting_player" else "waiting_next")
              },
            ),
          )
          updateMessageById(placeholder.id) { current ->
            current.copy(
              role = message?.get("role")?.asString ?: current.role,
              roleType = message?.get("roleType")?.asString ?: current.roleType,
              eventType = message?.get("eventType")?.asString ?: current.eventType,
              content = finalContent,
              meta = buildRuntimeStreamMeta(
                current.meta,
                status = if (debugCanPlayerSpeakFromState()) "waiting_player" else "waiting_next",
                streaming = false,
                nextRole = if (debugCanPlayerSpeakFromState()) "用户" else playExpectedSpeaker(),
                nextRoleType = if (debugCanPlayerSpeakFromState()) "player" else scalarRuntimeText(runtimeTurnStateRoot()?.get("expectedRoleType")).ifBlank { "npc" },
              ),
            )
          }
          syncRuntimeChatTraceLog()
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
    refreshDebugStoryInfo(playCurrentChapter())
    if (useIntroductionStream) {
      val finalContent = messages.lastOrNull { it.id == placeholder.id }?.content.orEmpty().ifBlank { accumulated }
      delay(estimateOpeningPresentationMs(finalContent))
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

  private fun normalizeDebugPlayerProfileGender(input: String): String {
    return when (normalizeScalarEditorText(input).trim()) {
      "男", "男性", "男生" -> "男"
      "女", "女性", "女生" -> "女"
      else -> ""
    }
  }

  private fun normalizeDebugPlayerProfileAge(input: String): Int? {
    val matched = Regex("(\\d{1,3})").find(normalizeScalarEditorText(input).trim()) ?: return null
    val value = matched.groupValues.getOrNull(1)?.toIntOrNull() ?: return null
    return value.takeIf { it in 1..150 }
  }

  private fun parseDebugPlayerProfileDraft(message: String, currentName: String): ParsedPlayerProfile {
    val text = normalizeScalarEditorText(message)
      .replace(Regex("[。！？!?\\r\\n]+"), " ")
      .replace(Regex("\\s+"), " ")
      .trim()
    if (text.isBlank()) return ParsedPlayerProfile()

    Regex("^([A-Za-z\\u4e00-\\u9fa5·•]{1,12}?)(男|女)(?:性|生)?(?:[，,、/\\s]+(\\d{1,3})(?:岁)?)?$")
      .matchEntire(text)
      ?.let { matched ->
        return ParsedPlayerProfile(
          name = normalizeScalarEditorText(matched.groupValues.getOrNull(1)),
          gender = normalizeDebugPlayerProfileGender(matched.groupValues.getOrNull(2).orEmpty()),
          age = normalizeDebugPlayerProfileAge(matched.groupValues.getOrNull(3).orEmpty()),
        )
      }

    var name = ""
    var gender = ""
    var age: Int? = null

    Regex("(?:我叫|我是|姓名(?:是|[:：])?|名字(?:是|[:：])?)\\s*([A-Za-z\\u4e00-\\u9fa5·•]{1,16})")
      .find(text)
      ?.groupValues
      ?.getOrNull(1)
      ?.let { name = normalizeScalarEditorText(it) }

    Regex("(?:性别(?:是|[:：])?\\s*)?(男|女|男性|女性|男生|女生)")
      .find(text)
      ?.groupValues
      ?.getOrNull(1)
      ?.let { normalizeDebugPlayerProfileGender(it) }
      ?.takeIf { it.isNotBlank() }
      ?.let { gender = it }

    Regex("(?:年龄(?:是|[:：])?\\s*|我今年|今年)\\s*(\\d{1,3})\\s*岁?")
      .find(text)
      ?.groupValues
      ?.getOrNull(1)
      ?.let { normalizeDebugPlayerProfileAge(it) }
      ?.let { age = it }

    if (name.isBlank() && text.length <= 24) {
      val segments = text
        .split(Regex("[，,、/|｜]"))
        .map { it.trim() }
        .filter { it.isNotBlank() }
      val hasProfileSegment = segments.any { normalizeDebugPlayerProfileGender(it).isNotBlank() || normalizeDebugPlayerProfileAge(it) != null }
      if (segments.size in 2..4 && hasProfileSegment) {
        segments.firstOrNull {
          Regex("^[A-Za-z\\u4e00-\\u9fa5·•]{1,16}$").matches(it)
            && normalizeDebugPlayerProfileGender(it).isBlank()
            && normalizeDebugPlayerProfileAge(it) == null
        }?.let { name = normalizeScalarEditorText(it) }
        if (gender.isBlank()) {
          segments
            .map { normalizeDebugPlayerProfileGender(it) }
            .firstOrNull { it.isNotBlank() }
            ?.let { gender = it }
        }
        if (age == null) {
          segments
            .mapNotNull { normalizeDebugPlayerProfileAge(it) }
            .firstOrNull()
            ?.let { age = it }
        }
      }
    }

    if (name == currentName) {
      name = ""
    }
    return ParsedPlayerProfile(name = name, gender = gender, age = age)
  }

  private fun applyLocalDebugPlayerProfile(text: String) {
    if (!debugMode) return
    val displayName = playerName.ifBlank { "用户" }
    val runtimePlayer = runtimeRoleSnapshot("player", "player") ?: buildRole(
      id = "player",
      roleType = "player",
      name = displayName,
      avatarPath = userAvatarPath,
      avatarBgPath = userAvatarBgPath,
      description = playerDesc.ifBlank { "用户在故事中的主视角角色" },
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
    val currentCard = runtimePlayer.parameterCardJson ?: RoleParameterCard()
    val parsed = parseDebugPlayerProfileDraft(text, currentCard.name.ifBlank { displayName })
    if (parsed.name.isBlank() && parsed.gender.isBlank() && parsed.age == null) return
    val nextCard = currentCard.copy(
      name = parsed.name.ifBlank { currentCard.name.ifBlank { displayName } },
      rawSetting = currentCard.rawSetting,
      gender = parsed.gender.ifBlank { currentCard.gender },
      age = parsed.age ?: currentCard.age,
      voice = currentCard.voice.ifBlank { runtimePlayer.voice },
    )
    val updatedRole = runtimePlayer.copy(
      name = displayName,
      parameterCardJson = nextCard.takeIf {
        it.name.isNotBlank()
          || it.rawSetting.isNotBlank()
          || it.gender.isNotBlank()
          || it.age != null
          || it.voice.isNotBlank()
          || it.skills.isNotEmpty()
          || it.items.isNotEmpty()
          || it.equipment.isNotEmpty()
          || it.other.isNotEmpty()
      },
    )
    val root = if (debugRuntimeState?.isJsonObject == true) {
      debugRuntimeState!!.asJsonObject.deepCopy()
    } else {
      JsonObject()
    }
    root.add("player", repository.toJson(updatedRole))
    val turnState = if (root.get("turnState")?.isJsonObject == true) {
      root.getAsJsonObject("turnState").deepCopy()
    } else {
      JsonObject()
    }
    val expectedRoleType = scalarRuntimeText(turnState.get("expectedRoleType")).ifBlank { "player" }.lowercase(Locale.ROOT)
    if (expectedRoleType == "player") {
      turnState.addProperty("expectedRole", displayName)
    }
    root.add("turnState", turnState)
    debugRuntimeState = root
    updateDebugStatePreview()
  }

  private fun parseChapterCondition(raw: String): JsonElement? {
    val text = normalizeScalarEditorText(raw).trim()
    if (text.isEmpty()) return null
    return runCatching { JsonParser.parseString(text) }.getOrElse { JsonPrimitive(text) }
  }

  private fun normalizeRuntimeOutlineEditorText(raw: Any?): String {
    if (raw == null) return ""
    val text = normalizeScalarEditorText(raw).trim()
    if (text.isEmpty()) return ""
    return if (raw is JsonElement) {
      raw.toString()
    } else {
      runCatching { JsonParser.parseString(text).toString() }.getOrElse { text }
    }
  }

  private fun parseRuntimeOutlineEditorText(raw: String): JsonElement? {
    val text = normalizeScalarEditorText(raw).trim()
    if (text.isEmpty()) return null
    val parsed = runCatching { JsonParser.parseString(text) }.getOrNull()
      ?: error("章节 Phase Graph 配置不是有效 JSON")
    if (!parsed.isJsonObject) {
      error("章节 Phase Graph 配置必须是 JSON 对象")
    }
    return parsed
  }

  fun formatChapterRuntimeOutlineDraft() {
    val parsed = parseRuntimeOutlineEditorText(chapterRuntimeOutlineText)
    if (parsed == null || parsed.isJsonNull) {
      chapterRuntimeOutlineText = ""
      notice = "当前没有可格式化的 Phase Graph"
      return
    }
    chapterRuntimeOutlineText = "${prettyGson.toJson(parsed)}\n"
    notice = "已格式化 Phase Graph"
  }

  suspend fun generateChapterRuntimeOutlineDraft() {
    val payload = JsonObject().apply {
      addProperty("openingRole", chapterOpeningRole)
      addProperty("openingText", chapterOpeningLine)
      addProperty("content", chapterContent)
      parseChapterCondition(chapterEntryCondition)?.let { add("entryCondition", it) }
      parseChapterCondition(chapterCondition)?.let { add("completionCondition", it) }
      parseRuntimeOutlineEditorText(chapterRuntimeOutlineText)?.let { add("runtimeOutline", it) }
    }
    val outline = repository.previewRuntimeOutline(payload)
    chapterRuntimeOutlineText = "${prettyGson.toJson(outline)}\n"
    notice = "已生成章节 Phase Graph 草稿"
  }

  fun chapterRuntimePhasePreview(): List<ChapterPhasePreview> {
    val parsed = runCatching { parseRuntimeOutlineEditorText(chapterRuntimeOutlineText) }.getOrNull()
      ?: return emptyList()
    if (!parsed.isJsonObject) return emptyList()
    val phases = parsed.asJsonObject.getAsJsonArray("phases") ?: return emptyList()
    val phaseBasics = phases.mapIndexedNotNull { index, element ->
      if (!element.isJsonObject) return@mapIndexedNotNull null
      val obj = element.asJsonObject
      val id = normalizeScalarEditorText(scalarRuntimeText(obj.get("id"))).ifBlank { "phase_${index + 1}" }
      val label = normalizeScalarEditorText(scalarRuntimeText(obj.get("label"))).ifBlank { "阶段 ${index + 1}" }
      id to label
    }
    val phaseLabelMap = phaseBasics.toMap()
    return phases.mapIndexedNotNull { index, element ->
      if (!element.isJsonObject) return@mapIndexedNotNull null
      val obj = element.asJsonObject
      val id = normalizeScalarEditorText(scalarRuntimeText(obj.get("id"))).ifBlank { "phase_${index + 1}" }
      val label = normalizeScalarEditorText(scalarRuntimeText(obj.get("label"))).ifBlank { "阶段 ${index + 1}" }
      val kind = normalizeScalarEditorText(scalarRuntimeText(obj.get("kind"))).ifBlank { "scene" }
      val allowedSpeakers = obj.getAsJsonArray("allowedSpeakers")
        ?.mapNotNull { normalizeScalarEditorText(scalarRuntimeText(it)).trim().takeIf { item -> item.isNotEmpty() } }
        ?.joinToString(" / ")
        .orEmpty()
      val nextPhaseList = obj.getAsJsonArray("nextPhaseIds")
        ?.mapNotNull { normalizeScalarEditorText(scalarRuntimeText(it)).trim().takeIf { item -> item.isNotEmpty() } }
        .orEmpty()
      val nextPhaseIds = nextPhaseList
        .map { phaseLabelMap[it] ?: it }
        .joinToString(" -> ")
      val defaultNextPhaseId = normalizeScalarEditorText(scalarRuntimeText(obj.get("defaultNextPhaseId"))).trim()
        .let { value -> if (value.isNotBlank()) phaseLabelMap[value] ?: value else "" }
      val requiredEventIds = obj.getAsJsonArray("requiredEventIds")
        ?.mapNotNull { normalizeScalarEditorText(scalarRuntimeText(it)).trim().takeIf { item -> item.isNotEmpty() } }
        ?.joinToString(" / ")
        .orEmpty()
      val completionEventIds = obj.getAsJsonArray("completionEventIds")
        ?.mapNotNull { normalizeScalarEditorText(scalarRuntimeText(it)).trim().takeIf { item -> item.isNotEmpty() } }
        ?.joinToString(" / ")
        .orEmpty()
      val advanceSignals = obj.getAsJsonArray("advanceSignals")
        ?.mapNotNull { normalizeScalarEditorText(scalarRuntimeText(it)).trim().takeIf { item -> item.isNotEmpty() } }
        ?.joinToString(" / ")
        .orEmpty()
      val relatedFixedEventIds = obj.getAsJsonArray("relatedFixedEventIds")
        ?.mapNotNull { normalizeScalarEditorText(scalarRuntimeText(it)).trim().takeIf { item -> item.isNotEmpty() } }
        ?.joinToString(" / ")
        .orEmpty()
      val flowSummary = if (nextPhaseList.isNotEmpty()) {
        "${label} -> ${nextPhaseList.map { phaseLabelMap[it] ?: it }.joinToString(" / ")}"
      } else {
        "${label} -> 顺序回退"
      }
      ChapterPhasePreview(
        id = id,
        label = label,
        kind = kind,
        allowedSpeakers = allowedSpeakers,
        nextPhaseIds = nextPhaseIds,
        defaultNextPhaseId = defaultNextPhaseId,
        requiredEventIds = requiredEventIds,
        completionEventIds = completionEventIds,
        advanceSignals = advanceSignals,
        relatedFixedEventIds = relatedFixedEventIds,
        flowSummary = flowSummary,
      )
    }
  }

  fun chapterRuntimeOutlinePreview(): ChapterRuntimeOutlinePreview? {
    val parsed = runCatching { parseRuntimeOutlineEditorText(chapterRuntimeOutlineText) }.getOrNull()
      ?: return null
    if (!parsed.isJsonObject) return null
    val root = parsed.asJsonObject
    val phases = chapterRuntimePhasePreview()
    val userNodes = root.getAsJsonArray("userNodes")
      ?.mapIndexedNotNull { index, element ->
        if (!element.isJsonObject) return@mapIndexedNotNull null
        val obj = element.asJsonObject
        ChapterUserNodePreview(
          id = normalizeScalarEditorText(scalarRuntimeText(obj.get("id"))).ifBlank { "user_node_${index + 1}" },
          goal = normalizeScalarEditorText(scalarRuntimeText(obj.get("goal"))).ifBlank { normalizeScalarEditorText(scalarRuntimeText(obj.get("label"))).ifBlank { "用户节点 ${index + 1}" } },
          promptRole = normalizeScalarEditorText(scalarRuntimeText(obj.get("promptRole"))).ifBlank { "系统" },
        )
      }
      .orEmpty()
    val fixedEvents = root.getAsJsonArray("fixedEvents")
      ?.mapIndexedNotNull { index, element ->
        if (!element.isJsonObject) return@mapIndexedNotNull null
        val obj = element.asJsonObject
        ChapterFixedEventPreview(
          id = normalizeScalarEditorText(scalarRuntimeText(obj.get("id"))).ifBlank { "fixed_event_${index + 1}" },
          label = normalizeScalarEditorText(scalarRuntimeText(obj.get("label"))).ifBlank { "固定事件 ${index + 1}" },
        )
      }
      .orEmpty()
    val endingRules = root.getAsJsonObject("endingRules")?.let { rules ->
      ChapterEndingRulesPreview(
        success = rules.getAsJsonArray("success")
          ?.mapNotNull { normalizeScalarEditorText(scalarRuntimeText(it)).trim().takeIf { item -> item.isNotEmpty() } }
          ?.joinToString(" / ")
          .orEmpty(),
        failure = rules.getAsJsonArray("failure")
          ?.mapNotNull { normalizeScalarEditorText(scalarRuntimeText(it)).trim().takeIf { item -> item.isNotEmpty() } }
          ?.joinToString(" / ")
          .orEmpty(),
        nextChapterId = normalizeScalarEditorText(scalarRuntimeText(rules.get("nextChapterId"))).trim(),
      )
    }
    return ChapterRuntimeOutlinePreview(
      phases = phases,
      userNodes = userNodes,
      fixedEvents = fixedEvents,
      endingRules = endingRules,
    )
  }

  fun playChapterProgressDebug(): RuntimeChapterProgressDebugItem? {
    val root = runtimeStateRoot() ?: return null
    val progress = root.getAsJsonObject("chapterProgress") ?: return null
    val phaseId = scalarRuntimeText(progress.get("phaseId"))
    val pendingGoal = scalarRuntimeText(progress.get("pendingGoal"))
    val userNodeId = scalarRuntimeText(progress.get("userNodeId"))
    val completedEvents = progress.getAsJsonArray("completedEvents")
      ?.mapNotNull { normalizeScalarEditorText(scalarRuntimeText(it)).trim().takeIf { item -> item.isNotEmpty() } }
      ?.joinToString(" / ")
      .orEmpty()
    val chapter = playCurrentChapter()
    val runtimeOutline = chapter?.runtimeOutline?.takeIf { it.isJsonObject }?.asJsonObject
    val phaseLabel = runtimeOutline?.getAsJsonArray("phases")
      ?.firstOrNull { element -> element.isJsonObject && scalarRuntimeText(element.asJsonObject.get("id")) == phaseId }
      ?.asJsonObject
      ?.let { scalarRuntimeText(it.get("label")) }
      .orEmpty()
    val userNodeLabel = runtimeOutline?.getAsJsonArray("userNodes")
      ?.firstOrNull { element -> element.isJsonObject && scalarRuntimeText(element.asJsonObject.get("id")) == userNodeId }
      ?.asJsonObject
      ?.let { scalarRuntimeText(it.get("goal")).ifBlank { scalarRuntimeText(it.get("label")) } }
      .orEmpty()
    return RuntimeChapterProgressDebugItem(
      phaseLabel = phaseLabel,
      phaseId = phaseId,
      pendingGoal = pendingGoal,
      userNodeLabel = userNodeLabel,
      completedEvents = completedEvents,
    )
  }

  private fun runtimeEventDigestItemFromModel(item: RuntimeEventDigestItem?): RuntimeChapterEventItem? {
    if (item == null) return null
    return RuntimeChapterEventItem(
      eventIndex = item.eventIndex,
      eventKind = item.eventKind,
      eventFlowType = item.eventFlowType,
      eventSummary = item.eventSummary,
      eventStatus = item.eventStatus,
      eventFacts = runtimeStringify(item.eventFacts),
      memorySummary = item.memorySummary,
      memoryFacts = runtimeStringify(item.memoryFacts),
    )
  }

  private fun runtimeEventDigestItemFromJson(input: JsonElement?): RuntimeChapterEventItem? {
    if (input == null || input.isJsonNull || !input.isJsonObject) return null
    val obj = input.asJsonObject
    val summary = scalarRuntimeText(obj.get("eventSummary"))
    val facts = scalarRuntimeText(obj.get("eventFacts"))
    val memorySummary = scalarRuntimeText(obj.get("memorySummary"))
    val memoryFacts = scalarRuntimeText(obj.get("memoryFacts"))
    val eventKind = scalarRuntimeText(obj.get("eventKind"))
    val eventFlowType = scalarRuntimeText(obj.get("eventFlowType"))
    val eventStatus = scalarRuntimeText(obj.get("eventStatus"))
    val eventIndex = runtimeIntValue(obj.get("eventIndex")) ?: 0
    if (
      summary.isBlank()
      && facts.isBlank()
      && memorySummary.isBlank()
      && memoryFacts.isBlank()
      && eventKind.isBlank()
      && eventFlowType.isBlank()
      && eventStatus.isBlank()
      && eventIndex <= 0
    ) {
      return null
    }
    return RuntimeChapterEventItem(
      eventIndex = eventIndex,
      eventKind = eventKind,
      eventFlowType = eventFlowType,
      eventSummary = summary,
      eventStatus = eventStatus,
      eventFacts = facts,
      memorySummary = memorySummary,
      memoryFacts = memoryFacts,
    )
  }

  private fun runtimeEventDigestWindowFromState(): List<RuntimeChapterEventItem> {
    val fromDetail = sessionDetail?.eventDigestWindow
      ?.mapNotNull(::runtimeEventDigestItemFromModel)
      .orEmpty()
    if (fromDetail.isNotEmpty()) {
      return fromDetail.sortedBy { item -> if (item.eventIndex > 0) item.eventIndex else Int.MAX_VALUE }
    }
    val root = runtimeStateRoot() ?: return emptyList()
    val merged = linkedMapOf<Int, RuntimeChapterEventItem>()
    runtimeEventDigestItemFromJson(root.get("currentEvent"))?.let { item ->
      val key = item.eventIndex.takeIf { it > 0 } ?: 1
      merged[key] = item
    }
    root.getAsJsonArray("dynamicEvents")
      ?.mapNotNull(::runtimeEventDigestItemFromJson)
      ?.forEachIndexed { index, item ->
        val key = item.eventIndex.takeIf { it > 0 } ?: (index + 1)
        val current = merged[key]
        merged[key] = if (
          current == null
          || (current.eventSummary.isBlank() && item.eventSummary.isNotBlank())
          || (current.eventFacts.isBlank() && item.eventFacts.isNotBlank())
        ) {
          item
        } else {
          current
        }
      }
    return merged.values.sortedBy { item -> if (item.eventIndex > 0) item.eventIndex else Int.MAX_VALUE }
  }

  fun playCurrentRuntimeEventDigest(): RuntimeChapterEventItem? {
    runtimeEventDigestItemFromModel(sessionDetail?.currentEventDigest)?.let { return it }
    val runtimeItems = runtimeEventDigestWindowFromState()
    return runtimeItems.firstOrNull()
  }

  private fun hasReadyRuntimeEventWindow(items: List<RuntimeChapterEventItem>): Boolean {
    // 这里和 Web 保持一致：
    // 1. 运行时窗口条目数已经超过 1，说明后端给出的事件链比当前章节摘要更完整；
    // 2. 或者任意条目已经带上了真正的摘要/事实，也可以优先信任运行时窗口。
    return items.size > 1
      || items.any { item ->
        item.eventSummary.isNotBlank()
          && item.eventSummary != "当前事件摘要待生成"
      }
      || items.any { item -> item.eventFacts.isNotBlank() }
  }

  private fun isIntroductionEventItem(item: RuntimeChapterEventItem?): Boolean {
    if (item == null) return false
    return item.eventFlowType.trim().equals("introduction", ignoreCase = true)
      || item.eventKind.trim().equals("opening", ignoreCase = true)
  }

  /**
   * 判断事件是否属于章节结尾那条“结束条件检查/固定条件”。
   *
   * 安卓端运行时窗口有时只返回场景事件，不会把 ending/fixed 条目一起带回。
   * 这里单独识别出来，方便后续把章节大纲中的结束事件补回列表，和 Web 保持一致。
   */
  private fun isEndingEventItem(item: RuntimeChapterEventItem?): Boolean {
    if (item == null) return false
    val flowType = item.eventFlowType.trim()
    val kind = item.eventKind.trim()
    return flowType.equals("chapter_ending_check", ignoreCase = true)
      || kind.equals("fixed", ignoreCase = true)
      || kind.equals("ending", ignoreCase = true)
  }

  /**
   * 把章节大纲里缺失的结束事件补到运行时事件窗口里。
   *
   * 用途：
   * - 运行时窗口优先保留后端实时状态；
   * - 但如果后端这轮没把 ending/fixed 事件带回来，安卓仍然要展示 Web 同款的结束事件卡。
   */
  private fun mergeMissingEndingEvents(
    runtimeItems: List<RuntimeChapterEventItem>,
    outlineItems: List<RuntimeChapterEventItem>,
  ): List<RuntimeChapterEventItem> {
    if (runtimeItems.isEmpty()) return outlineItems
    val runtimeHasEnding = runtimeItems.any(::isEndingEventItem)
    if (runtimeHasEnding) return runtimeItems
    val outlineEndingItems = outlineItems.filter(::isEndingEventItem)
    if (outlineEndingItems.isEmpty()) return runtimeItems
    return (runtimeItems + outlineEndingItems)
      .distinctBy { item ->
        val indexKey = item.eventIndex.takeIf { it > 0 }?.toString().orEmpty()
        listOf(indexKey, item.eventFlowType.trim(), item.eventKind.trim(), item.eventSummary.trim()).joinToString("|")
      }
      .sortedBy { item -> if (item.eventIndex > 0) item.eventIndex else Int.MAX_VALUE }
  }

  private fun splitCompletionConditionText(raw: String): Pair<String, String> {
    val text = raw.trim()
    if (text.isBlank()) return "" to ""
    val matched = Regex("""^(.*?)[（(]\s*([^()（）]+?)\s*[)）]\s*$""").find(text)
      ?: return text to ""
    val successText = matched.groupValues.getOrNull(1)?.trim().orEmpty()
    val failureText = matched.groupValues.getOrNull(2)?.trim().orEmpty()
    if (successText.isBlank() || failureText.isBlank() || !Regex("失败|fail|failed|failure", RegexOption.IGNORE_CASE).containsMatchIn(failureText)) {
      return text to ""
    }
    return successText to failureText
  }

  private fun buildEndingOutlineSummary(
    completionCondition: String,
    fixedEvents: List<ChapterFixedEventPreview>,
  ): String {
    val normalized = normalizeConditionEditorText(completionCondition)
    if (normalized.isNotBlank()) {
      return "结束条件：$normalized"
    }
    val labels = fixedEvents.map { it.label.trim() }.filter { it.isNotBlank() }
    return if (labels.isNotEmpty()) "结束条件：${labels.joinToString("；")}" else "结束条件检查"
  }

  private fun buildEndingOutlineFacts(fixedEvents: List<ChapterFixedEventPreview>): String {
    val labels = fixedEvents.map { it.label.trim() }.filter { it.isNotBlank() }
    if (labels.isEmpty()) return ""
    return labels.mapIndexed { index, label ->
      if (index == 0) "成功条件：$label" else "失败条件：$label"
    }.joinToString(" / ")
  }

  private fun runtimeOutlineEventItems(): List<RuntimeChapterEventItem> {
    val chapter = playCurrentChapter() ?: return emptyList()
    val runtimeOutline = chapter.runtimeOutline?.takeIf { it.isJsonObject }?.asJsonObject ?: return emptyList()
    val phases = runtimeOutline.getAsJsonArray("phases")
    val fixedEvents = runtimeOutline.getAsJsonArray("fixedEvents")
    val completionConditionText = normalizeConditionEditorText(chapter.completionCondition)
    val completionBranches = splitCompletionConditionText(completionConditionText)
    val syntheticFixedEvents = if (fixedEvents == null || fixedEvents.size() == 0) {
      listOf(completionBranches.first, completionBranches.second)
        .filter { it.isNotBlank() }
        .mapIndexed { index, label -> ChapterFixedEventPreview("synthetic_fixed_event_${index + 1}", label) }
    } else {
      emptyList()
    }
    if (phases == null && fixedEvents == null && syntheticFixedEvents.isEmpty()) return emptyList()
    val progress = runtimeStateRoot()?.getAsJsonObject("chapterProgress")
    val phaseId = scalarRuntimeText(runtimeStateRoot()?.getAsJsonObject("chapterProgress")?.get("phaseId"))
    val currentEventKind = scalarRuntimeText(progress?.get("eventKind"))
    val currentEventFlowType = playCurrentRuntimeEventDigest()?.eventFlowType.orEmpty()
    // 运行时内部继续保留原始状态枚举，展示层再统一翻译成“未开始/进行中/已完成/等待用户”。
    // 否则安卓端会把部分条目写成中文、部分条目保留英文，和 Web 的事件卡表现不一致。
    val currentEventStatus = scalarRuntimeText(progress?.get("eventStatus")).ifBlank { "idle" }
    val currentEventSummary = playCurrentRuntimeEventDigest()?.eventSummary.orEmpty()
    val completedEvents = progress?.getAsJsonArray("completedEvents")
      ?.mapNotNull { normalizeScalarEditorText(scalarRuntimeText(it)).trim().takeIf { value -> value.isNotEmpty() } }
      ?.toSet()
      .orEmpty()
    val phasePreview = chapterRuntimePhasePreview()
    val activeIndex = phasePreview.indexOfFirst { item -> item.id == phaseId }
    val items = mutableListOf<RuntimeChapterEventItem>()

    phasePreview.forEachIndexed { _, item ->
      val status = when {
        phaseId.isNotBlank() && item.id == phaseId -> currentEventStatus
        activeIndex >= 0 && phasePreview.indexOf(item) < activeIndex -> "completed"
        phaseId.isBlank()
          && currentEventKind.equals(item.kind, ignoreCase = true)
          && currentEventSummary.isNotBlank()
          && currentEventSummary == item.label -> currentEventStatus
        else -> "idle"
      }
      items += RuntimeChapterEventItem(
        eventIndex = items.size + 1,
        eventKind = item.kind,
        eventFlowType = "chapter_content",
        eventSummary = item.label.ifBlank { "事件 ${items.size + 1}" },
        eventStatus = status,
        eventFacts = item.flowSummary,
        memorySummary = "",
        memoryFacts = "",
      )
    }

    val allFixedEvents = buildList {
      fixedEvents?.forEach { element ->
        val event = element.takeIf { it.isJsonObject }?.asJsonObject ?: return@forEach
        add(
          ChapterFixedEventPreview(
            scalarRuntimeText(event.get("id")),
            scalarRuntimeText(event.get("label")),
          ),
        )
      }
      addAll(syntheticFixedEvents)
    }.filter { it.label.isNotBlank() || it.id.isNotBlank() }

    if (allFixedEvents.isNotEmpty()) {
      val anyCompleted = allFixedEvents.any { it.id.isNotBlank() && completedEvents.contains(it.id) }
      val status = when {
        currentEventFlowType.equals("chapter_ending_check", ignoreCase = true)
          || currentEventKind.equals("fixed", ignoreCase = true)
          || currentEventKind.equals("ending", ignoreCase = true) ->
          currentEventStatus
        anyCompleted -> "completed"
        else -> "idle"
      }
      items += RuntimeChapterEventItem(
        eventIndex = items.size + 1,
        eventKind = "fixed",
        eventFlowType = "chapter_ending_check",
        eventSummary = buildEndingOutlineSummary(normalizeConditionEditorText(chapter.completionCondition), allFixedEvents),
        eventStatus = status,
        eventFacts = buildEndingOutlineFacts(allFixedEvents),
        memorySummary = "",
        memoryFacts = "",
      )
    }

    return items
  }

  fun playVisibleChapterEvents(): List<RuntimeChapterEventItem> {
    val runtimeItems = runtimeEventDigestWindowFromState().filterNot { isIntroductionEventItem(it) }
    val outlineItems = runtimeOutlineEventItems()
    if (outlineItems.isEmpty()) return runtimeItems
    val mergedRuntimeItems = mergeMissingEndingEvents(runtimeItems, outlineItems)
    if (outlineItems.size > mergedRuntimeItems.size) return outlineItems
    return if (hasReadyRuntimeEventWindow(mergedRuntimeItems)) mergedRuntimeItems else outlineItems
  }

  fun playEventFlowLabel(item: RuntimeChapterEventItem): String {
    return when (item.eventFlowType.trim().lowercase()) {
      "introduction" -> "开场白"
      "chapter_ending_check" -> "结束条件检查"
      "free_runtime" -> "自由剧情"
      "chapter_content" -> "章节内容"
      else -> when (item.eventKind.trim().lowercase()) {
      "opening" -> "开场白"
      "ending" -> "结束条件检查"
      "fixed" -> "固定条件"
      "scene", "user" -> "章节内容"
      else -> "章节事件"
      }
    }
  }

  fun playCurrentEventProgressText(): String {
    val currentEvent = playCurrentRuntimeEventDigest()
    if (currentEvent != null && !isIntroductionEventItem(currentEvent)) {
      val parts = mutableListOf<String>()
      if (currentEvent.eventIndex > 0) {
        parts += "事件 ${currentEvent.eventIndex}"
      }
      val kind = currentEvent.eventKind.trim()
      if (kind.isNotBlank()) {
        parts += kind
      }
      val summary = currentEvent.eventSummary.trim()
      if (summary.isNotBlank()) {
        parts += summary
      }
      val status = currentEvent.eventStatus.trim()
      if (status.isNotBlank()) {
        parts += status
      }
      if (parts.isNotEmpty()) {
        return parts.joinToString(" · ")
      }
    }
    val progress = playChapterProgressDebug()
    return listOfNotNull(
      progress?.phaseLabel?.takeIf { it.isNotBlank() }?.let { "阶段 $it" },
      progress?.pendingGoal?.takeIf { it.isNotBlank() }?.let { "目标 $it" },
      progress?.userNodeLabel?.takeIf { it.isNotBlank() }?.let { "用户节点 $it" },
      progress?.completedEvents?.takeIf { it.isNotBlank() }?.let { "已完成 $it" },
    ).joinToString(" · ").ifBlank { "当前章节事件待生成" }
  }

  fun playDebugOrchestratorRuntimeText(): String {
    val runtime = debugLatestPlan?.orchestratorRuntime
    val planSourceLabel = when (debugLatestPlan?.planSource?.trim()?.lowercase()) {
      "opening_preset" -> "开场白预设"
      "ai_orchestrator" -> "正式编排"
      "rule_orchestrator" -> "规则编排"
      "fallback_orchestrator" -> "兜底编排"
      "preset" -> "预设流程"
      else -> ""
    }
    if (runtime == null && planSourceLabel.isBlank()) return ""
    val modeLabel = if (runtime?.payloadMode?.trim()?.equals("advanced", ignoreCase = true) == true) "高级版" else "精简版"
    val sourceLabel = if (runtime?.payloadModeSource?.trim()?.equals("explicit", ignoreCase = true) == true) "显式" else "推断"
    val reasoningLabel = runtime?.reasoningEffort?.trim().orEmpty().ifBlank { "未指定" }
    val modelLabel = if (runtime != null) {
      listOf(runtime.manufacturer.trim(), runtime.model.trim()).filter { it.isNotBlank() }.joinToString(" / ")
    } else {
      ""
    }
    return listOf(
      planSourceLabel.takeIf { it.isNotBlank() }?.let { "流程：$it" }.orEmpty(),
      if (runtime != null) "编排运行：$modeLabel（$sourceLabel）" else "",
      if (runtime != null) "推理强度：$reasoningLabel" else "",
      modelLabel,
    )
      .filter { it.isNotBlank() }
      .joinToString(" · ")
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
    parameterCardJson: RoleParameterCard? = null,
  ): StoryRole {
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
      parameterCardJson = parameterCardJson,
    )
  }

  private fun resetRuntimeData() {
    loading = false
    userName = ""
    userNickname = ""
    userIntro = ""
    userId = 0L
    settingsPageMode = "settings"
    profileNicknameDraft = ""
    profileIntroDraft = ""
    profileSaving = false
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
    chapterMusicAutoPlay = true
    chapterConditionVisible = true
    selectedChapterId = null
    homeRecommendWorldId = null
    quickInput = ""
    currentSessionId = ""
    sessionDetail = null
    clearPlayChapterCache()
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

  private fun clearPlayChapterCache() {
    playChapters.clear()
    playChapterWorldId = 0L
  }

  private suspend fun refreshPlayChapterCache(detail: SessionDetail) {
    val worldId = detail.world?.id?.takeIf { it > 0L } ?: run {
      clearPlayChapterCache()
      return
    }
    if (playChapterWorldId != worldId) {
      playChapters.clear()
      playChapterWorldId = worldId
    }
    val activeChapterId = playRuntimeChapterId()
    val cacheReady = playChapterWorldId == worldId
      && playChapters.isNotEmpty()
      && (activeChapterId == null || playChapters.any { it.id == activeChapterId })
    if (cacheReady) return
    runCatching {
      repository.getChapter(worldId)
    }.onSuccess { rows ->
      playChapters.clear()
      playChapters.addAll(rows)
      playChapterWorldId = worldId
    }
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
    chapterMusicAutoPlay = true
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
    playerVoiceMixVoices = normalizedMixVoices(safeRoleMixVoices(world.playerRole))
    narratorName = world.narratorRole?.name ?: "旁白"
    narratorVoice = world.settings?.narratorVoice ?: world.narratorRole?.voice ?: "默认旁白"
    narratorVoiceMode = world.settings?.narratorVoiceMode ?: world.narratorRole?.voiceMode ?: "text"
    narratorVoicePresetId = world.settings?.narratorVoicePresetId ?: world.narratorRole?.voicePresetId.orEmpty()
    narratorVoiceReferenceAudioPath = world.settings?.narratorVoiceReferenceAudioPath ?: world.narratorRole?.voiceReferenceAudioPath.orEmpty()
    narratorVoiceReferenceAudioName = world.settings?.narratorVoiceReferenceAudioName ?: world.narratorRole?.voiceReferenceAudioName.orEmpty()
    narratorVoiceReferenceText = world.settings?.narratorVoiceReferenceText ?: world.narratorRole?.voiceReferenceText.orEmpty()
    narratorVoicePromptText = world.settings?.narratorVoicePromptText ?: world.narratorRole?.voicePromptText.orEmpty()
    narratorVoiceMixVoices = normalizedMixVoices(world.settings?.narratorVoiceMixVoices ?: safeRoleMixVoices(world.narratorRole))
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
