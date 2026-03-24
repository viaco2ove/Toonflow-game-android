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
import com.toonflow.game.data.ChapterItem
import com.toonflow.game.data.ChapterExtra
import com.toonflow.game.data.GameRepository
import com.toonflow.game.data.MessageItem
import com.toonflow.game.data.MiniGameState
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

  val baseTabs = listOf("主页", "创建", "聊过", "我的")
  val miniGameTags = listOf("#狼人杀", "#钓鱼", "#修炼", "#研发技能", "#炼药", "#挖矿", "#升级装备")
  val settingsModelSlots = listOf(
    SettingsModelSlot("storyOrchestratorModel", "编排师", "text"),
    SettingsModelSlot("storyMemoryModel", "记忆管理", "text"),
    SettingsModelSlot("storyImageModel", "AI生图", "image"),
    SettingsModelSlot("storyVoiceModel", "语音生成", "voice"),
    SettingsModelSlot("storyAsrModel", "语音识别", "voice"),
  )
  val storyPromptCodes = listOf(
    "story-main",
    "story-orchestrator",
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
  var playerVoiceConfigId by mutableStateOf<Long?>(null)
  var playerVoicePresetId by mutableStateOf("")
  var playerVoiceMode by mutableStateOf("text")
  var playerVoiceReferenceAudioPath by mutableStateOf("")
  var playerVoiceReferenceAudioName by mutableStateOf("")
  var playerVoiceReferenceText by mutableStateOf("")
  var playerVoicePromptText by mutableStateOf("")
  var playerVoiceMixVoices by mutableStateOf(defaultMixVoices())
  var narratorName by mutableStateOf("旁白")
  var narratorVoice by mutableStateOf("默认旁白")
  var narratorVoiceConfigId by mutableStateOf<Long?>(null)
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

  var miniGameType by mutableStateOf("")
  var miniGameStatus by mutableStateOf("idle")
  var miniGameRound by mutableStateOf(0)
  var miniGameStage by mutableStateOf("")
  var miniGameWinner by mutableStateOf("")
  var miniGameRewards by mutableStateOf("")
  var miniGameNotes by mutableStateOf("")

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
  var debugStatePreview by mutableStateOf("{}")
  var debugEndDialog by mutableStateOf<String?>(null)
  private var debugChapterSequence: List<ChapterItem> = emptyList()
  private var debugMessageSeed: Long = 1L
  private data class StoryEditorPersistSnapshot(
    val worldId: Long,
    val worldName: String,
    val worldIntro: String,
    val worldCoverPath: String,
    val worldCoverBgPath: String,
    val playerName: String,
    val playerDesc: String,
    val playerVoice: String,
    val playerVoiceConfigId: Long?,
    val playerVoicePresetId: String,
    val playerVoiceMode: String,
    val playerVoiceReferenceAudioPath: String,
    val playerVoiceReferenceAudioName: String,
    val playerVoiceReferenceText: String,
    val playerVoicePromptText: String,
    val playerVoiceMixVoices: List<VoiceMixItem>,
    val narratorName: String,
    val narratorVoice: String,
    val narratorVoiceConfigId: Long?,
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
      "voice" -> settingsVoiceConfigs.map {
        ModelConfigItem(
          id = it.id,
          type = "voice",
          model = it.model,
          manufacturer = it.manufacturer,
          baseUrl = it.baseUrl,
        )
      }
      else -> emptyList()
    }
  }

  fun settingsModelBinding(key: String): AiModelMapItem? {
    return settingsAiModelMap.firstOrNull { it.key == key }
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
      playerVoiceConfigId = playerVoiceConfigId,
      playerVoicePresetId = playerVoicePresetId,
      playerVoiceMode = playerVoiceMode,
      playerVoiceReferenceAudioPath = playerVoiceReferenceAudioPath,
      playerVoiceReferenceAudioName = playerVoiceReferenceAudioName,
      playerVoiceReferenceText = playerVoiceReferenceText,
      playerVoicePromptText = playerVoicePromptText,
      playerVoiceMixVoices = playerVoiceMixVoices.map { it.copy() },
      narratorName = narratorName,
      narratorVoice = narratorVoice,
      narratorVoiceConfigId = narratorVoiceConfigId,
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
      npcRoles = npcRoles.map { it.copy(voiceMixVoices = it.voiceMixVoices.map { voice -> voice.copy() }) },
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
    worldId = snapshot.worldId
    worldName = snapshot.worldName
    worldIntro = snapshot.worldIntro
    worldCoverPath = snapshot.worldCoverPath
    worldCoverBgPath = snapshot.worldCoverBgPath
    playerName = snapshot.playerName
    playerDesc = snapshot.playerDesc
    playerVoice = snapshot.playerVoice
    playerVoiceConfigId = snapshot.playerVoiceConfigId
    playerVoicePresetId = snapshot.playerVoicePresetId
    playerVoiceMode = snapshot.playerVoiceMode
    playerVoiceReferenceAudioPath = snapshot.playerVoiceReferenceAudioPath
    playerVoiceReferenceAudioName = snapshot.playerVoiceReferenceAudioName
    playerVoiceReferenceText = snapshot.playerVoiceReferenceText
    playerVoicePromptText = snapshot.playerVoicePromptText
    playerVoiceMixVoices = snapshot.playerVoiceMixVoices.map { it.copy() }
    narratorName = snapshot.narratorName
    narratorVoice = snapshot.narratorVoice
    narratorVoiceConfigId = snapshot.narratorVoiceConfigId
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
    npcRoles.addAll(snapshot.npcRoles.map { it.copy(voiceMixVoices = it.voiceMixVoices.map { voice -> voice.copy() }) })
    chapters.clear()
    chapters.addAll(snapshot.chapters.map { it.copy() })
    chapterExtras.clear()
    chapterExtras.addAll(snapshot.chapterExtras.map { it.copy() })
    selectedChapterId = snapshot.selectedChapterId
    chapterTitle = snapshot.chapterTitle
    chapterContent = snapshot.chapterContent
    chapterEntryCondition = snapshot.chapterEntryCondition
    chapterCondition = snapshot.chapterCondition
    chapterOpeningRole = snapshot.chapterOpeningRole
    chapterOpeningLine = snapshot.chapterOpeningLine
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
        val voices = repository.getVoiceModels()
        val bindings = repository.getAiModelMap()
        val prompts = repository.getPrompts()
        settingsTextConfigs.clear()
        settingsTextConfigs.addAll(configs.filter { it.type == "text" }.sortedBy { it.id })
        settingsImageConfigs.clear()
        settingsImageConfigs.addAll(configs.filter { it.type == "image" }.sortedBy { it.id })
        settingsVoiceConfigs.clear()
        settingsVoiceConfigs.addAll(voices.sortedBy { it.id })
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

  suspend fun deleteManagedModelConfig(id: Long) {
    repository.deleteModelConfig(id)
    ensureSettingsPanelData(true)
  }

  suspend fun testManagedModelConfig(config: ModelConfigItem): SettingsModelTestResult {
    return when (config.type.ifBlank { "text" }) {
      "text" -> SettingsModelTestResult(
        kind = "text",
        content = repository.testTextModel(config.model, config.apiKey, config.baseUrl, config.manufacturer),
      )
      "image" -> SettingsModelTestResult(
        kind = "image",
        content = resolveMediaPath(repository.testImageModel(config.model, config.apiKey, config.baseUrl, config.manufacturer)),
      )
      else -> {
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
          voiceModels.clear()
          voiceModels.addAll(rows.sortedBy { it.id })
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
    )
  }

  suspend fun polishVoicePrompt(text: String, style: String = ""): String {
    return repository.polishVoicePrompt(text, style)
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
    if (debugMode) return debugChapterTitle.ifBlank { "未进入章节" }
    return sessionDetail?.chapter?.title?.ifBlank { "未进入章节" } ?: "未进入章节"
  }

  fun playStatePreview(): String {
    if (debugMode) return debugStatePreview.ifBlank { "{}" }
    return sessionDetail?.state?.toString().orEmpty().ifBlank { "{}" }.take(320)
  }

  fun playChapterConditionText(): String {
    return playCurrentChapter()?.completionCondition?.toString()?.ifBlank { "无" } ?: "无"
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
        voiceConfigId = playerVoiceConfigId,
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
        voiceConfigId = narratorVoiceConfigId,
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
    debugMode = false
    debugSessionTitle = ""
    debugWorldName = ""
    debugWorldIntro = ""
    debugChapterId = null
    debugChapterTitle = ""
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
    }
  }

  fun updateStoryPlayerAvatarFromUri(rawUri: String) {
    val uri = runCatching { Uri.parse(rawUri) }.getOrNull()
    if (uri == null) {
      notice = "头像选择失败"
      return
    }

    viewModelScope.launch {
      runCatching {
        val key = if (worldId > 0L) "world_${worldId}_player" else "project_${selectedProjectId}_player"
        val saved = saveAvatarFiles(uri, key, projectScoped = true)
        userAvatarPath = saved.foregroundPath
        userAvatarBgPath = saved.backgroundPath
        notice = "头像已更新"
      }.onFailure {
        notice = "头像更新失败: ${it.message ?: "未知错误"}"
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
      runCatching {
        val saved = saveAvatarFiles(uri, storageKey, projectScoped = true)
        onSaved(saved.foregroundPath, saved.backgroundPath)
        notice = "头像已更新"
      }.onFailure {
        notice = "头像更新失败: ${it.message ?: "未知错误"}"
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
        val source = decodeBitmapFromUrl(resolveMediaPath(remotePath))
        val saved = if (wide) {
          uploadBitmapPair(source, storageKey, coverStdWidth, coverStdHeight, coverBgWidth, coverBgHeight, "scene", true)
        } else {
          uploadBitmapPair(source, storageKey, avatarStdSize, avatarStdSize, avatarBgSize, avatarBgSize, "role", true)
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
    val source = decodeBitmapFromUri(uri)
    return uploadBitmapPair(source, storageKey, avatarStdSize, avatarStdSize, avatarBgSize, avatarBgSize, "role", projectScoped)
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
    return "data:$mime;base64,${Base64.encodeToString(file.readBytes(), Base64.NO_WRAP)}"
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

  fun displayNameForMessage(message: MessageItem): String {
    if (message.role.isNotBlank()) return message.role
    return when (message.roleType) {
      "player" -> playerName.ifBlank { "用户" }
      "narrator" -> narratorName.ifBlank { "旁白" }
      else -> "角色"
    }
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
    return raw.toString()
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

  private fun stripOpeningPrefix(content: String, openingRole: String, openingLine: String): String {
    val text = normalizeScalarEditorText(content)
    if (text.isBlank()) return ""
    val extracted = extractOpeningContentParts(text) ?: return text
    val role = normalizeScalarEditorText(openingRole).trim()
    val line = normalizeScalarEditorText(openingLine).trim()
    val roleMatches = role.isBlank() || extracted.role.isBlank() || extracted.role == role
    val lineMatches = line.isBlank() || extracted.line.isBlank() || extracted.line == line
    return if (roleMatches && lineMatches) extracted.body else text
  }

  private fun buildPersistedChapterContent(): String {
    val role = chapterOpeningRole.ifBlank { narratorName.ifBlank { "旁白" } }.trim()
    val opening = chapterOpeningLine.trim()
    val body = stripOpeningPrefix(chapterContent.trim(), role, opening).trim()
    if (opening.isBlank()) return body
    return if (body.isBlank()) {
      "开场白[$role]：$opening"
    } else {
      "开场白[$role]：$opening\n$body"
    }
  }

  private fun currentChapterDebugOpening(chapter: ChapterItem): Pair<String, String> {
    val useEditorState = (selectedChapterId != null && chapter.id == selectedChapterId) || (selectedChapterId == null && chapter.id < 0L)
    if (useEditorState) {
      return chapterOpeningRole.ifBlank { narratorName.ifBlank { "旁白" } } to chapterOpeningLine
    }
    val extra = chapterExtraFor(chapter.id.takeIf { it > 0L }, chapter.sort)
    val role = normalizeScalarEditorText(extra?.openingRole).trim().ifBlank {
      normalizeScalarEditorText(chapter.openingRole).trim().ifBlank { narratorName.ifBlank { "旁白" } }
    }
    val line = normalizeScalarEditorText(extra?.openingLine).trim().ifBlank {
      normalizeScalarEditorText(chapter.openingText).trim()
    }
    return role to line
  }

  private fun buildEditorChapterSnapshot(): ChapterItem? {
    if (!hasCurrentChapterDraft()) return null
    val sort = currentChapterSort()
    val title = chapterTitle.ifBlank { "第 $sort 章" }
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
    chapterTitle = normalizeScalarEditorText(chapter.title)
    chapterContent = stripOpeningPrefix(chapter.content, openingRole, openingLine)
    chapterEntryCondition = normalizeConditionEditorText(chapter.entryCondition)
    chapterCondition = normalizeConditionEditorText(chapter.completionCondition)
    chapterOpeningRole = openingRole
    chapterOpeningLine = openingLine
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
    playerVoiceConfigId = binding.configId
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
    narratorVoiceConfigId = binding.configId
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
        voiceConfigId = binding.configId,
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
      voiceConfigId = playerVoiceConfigId,
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
      voiceConfigId = narratorVoiceConfigId,
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
          voiceConfigId = role.voiceConfigId,
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
    val mini = MiniGameState(
      gameType = miniGameType,
      status = miniGameStatus,
      round = miniGameRound,
      stage = miniGameStage,
      winner = miniGameWinner,
      rewards = miniGameRewards.split(",").map { it.trim() }.filter { it.isNotBlank() },
      notes = miniGameNotes,
    )
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
      "narratorVoiceConfigId" to narratorVoiceConfigId,
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
      "miniGameState" to mini,
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
    val editorChapter = buildEditorChapterSnapshot()
    val currentWorld = worlds.firstOrNull { it.id == worldId } ?: WorldItem(
      id = worldId,
      projectId = selectedProjectId,
      name = worldName.ifBlank { "未命名故事" },
      intro = worldIntro,
      chapterCount = chapters.size,
    )
    val seq = chapters.toMutableList()
    if (editorChapter != null) {
      val idx = seq.indexOfFirst { it.id == editorChapter.id }
      if (idx >= 0) {
        seq[idx] = editorChapter
      } else {
        seq.add(editorChapter)
      }
    }
    val sorted = seq.sortedWith(compareBy<ChapterItem> { it.sort }.thenBy { it.id })
    val startChapter = when {
      selectedChapterId != null -> sorted.firstOrNull { it.id == selectedChapterId } ?: editorChapter
      else -> editorChapter
    }
    if (startChapter == null) {
      notice = "请先填写当前章节"
      return
    }

    debugMode = true
    debugChapterSequence = sorted
    debugChapterId = startChapter.id
    debugChapterTitle = startChapter.title
    debugWorldName = currentWorld.name
    debugWorldIntro = currentWorld.intro
    debugSessionTitle = "调试：${currentWorld.name.ifBlank { "未命名故事" }}"
    debugEndDialog = null
    debugMessageSeed = 1L
    currentSessionId = "debug_${System.currentTimeMillis()}"
    sessionDetail = null
    messages.clear()
    sendText = ""
    appendDebugNarrator("进入章节《${startChapter.title.ifBlank { "未命名章节" }}》")
    val (openingRole, openingLine) = currentChapterDebugOpening(startChapter)
    if (openingLine.isNotBlank()) {
      appendDebugNarrator("开场白[$openingRole]：$openingLine")
    }
    val chapterBody = stripOpeningPrefix(startChapter.content, openingRole, openingLine)
    if (chapterBody.isNotBlank()) {
      appendDebugNarrator("章节内容：${chapterBody.take(120)}")
    }
    updateDebugStatePreview()
    activeTab = "游玩"
    notice = "已进入章节调试模式（仅本地缓存，不会持久化）"
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

    if (debugMode) {
      sendDebugMessage(text)
      return
    }

    viewModelScope.launch {
      runCatching {
        repository.addPlayerMessage(sid, playerName.ifBlank { "用户" }, text)
        sendText = ""
        refreshCurrentSession()
        loadSessions()
      }.onFailure {
        notice = "发送失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun syncMiniGame() {
    if (debugMode) {
      notice = "调试模式下小游戏状态仅保存在本地"
      return
    }
    if (currentSessionId.isBlank()) {
      notice = "请先进入会话"
      return
    }
    viewModelScope.launch {
      runCatching {
        repository.syncMiniGame(currentSessionId, buildMiniGameJson())
        refreshCurrentSession()
        notice = "小游戏状态已同步"
      }.onFailure {
        notice = "小游戏同步失败: ${it.message ?: "未知错误"}"
      }
    }
  }

  fun triggerMiniGame(command: String) {
    if (currentSessionId.isBlank()) {
      notice = "请先进入会话"
      return
    }
    miniGameType = command.replace("#", "")
    miniGameStatus = "running"
    miniGameRound += 1
    miniGameStage = "触发"
    miniGameNotes = "由指令 $command 触发"

    if (debugMode) {
      appendDebugPlayer(command)
      appendDebugNarrator("调试小游戏已触发：${command.removePrefix("#")}")
      updateDebugStatePreview()
      return
    }

    viewModelScope.launch {
      runCatching {
        repository.addPlayerMessage(currentSessionId, playerName.ifBlank { "用户" }, command)
        repository.syncMiniGame(currentSessionId, buildMiniGameJson())
        refreshCurrentSession()
        loadSessions()
      }.onFailure {
        notice = "小游戏触发失败: ${it.message ?: "未知错误"}"
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
    sessions.clear()
    sessions.addAll(rows)
  }

  private suspend fun refreshCurrentSession() {
    if (currentSessionId.isBlank()) return
    val detail = repository.getSession(currentSessionId)
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

  private fun sendDebugMessage(text: String) {
    appendDebugPlayer(text)
    sendText = ""
    val chapter = playCurrentChapter()
    if (chapter == null) {
      appendDebugNarrator("调试章节不存在，请返回重新选择。")
      updateDebugStatePreview()
      return
    }
    val conditionResult = evaluateDebugChapterResult(chapter, text)
    when (conditionResult.first) {
      DebugChapterResult.Continue -> {
        val hint = chapter.content.take(72).ifBlank { "继续推进剧情。" }
        appendDebugNarrator("调试中：《${chapter.title.ifBlank { "当前章节" }}》继续。\n$hint")
      }

      DebugChapterResult.Success -> {
        val nextChapter = resolveNextDebugChapter(chapter.id, conditionResult.second)
        if (nextChapter != null) {
          appendDebugNarrator("章节《${chapter.title.ifBlank { "当前章节" }}》完成，进入《${nextChapter.title.ifBlank { "下一章节" }}》。")
          debugChapterId = nextChapter.id
          debugChapterTitle = nextChapter.title
          val (nextRole, nextLine) = currentChapterDebugOpening(nextChapter)
          if (nextLine.isNotBlank()) {
            appendDebugNarrator("开场白[$nextRole]：$nextLine")
          }
          val nextBody = stripOpeningPrefix(nextChapter.content, nextRole, nextLine)
          if (nextBody.isNotBlank()) {
            appendDebugNarrator("下一章提示：${nextBody.take(100)}")
          }
        } else {
          appendDebugNarrator("章节《${chapter.title.ifBlank { "当前章节" }}》完成，故事已完结。")
          debugEndDialog = "已完结"
        }
      }

      DebugChapterResult.Failed -> {
        appendDebugNarrator("章节《${chapter.title.ifBlank { "当前章节" }}》判定失败，调试结束。")
        debugEndDialog = "已失败"
      }
    }
    updateDebugStatePreview()
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
    val chapter = playCurrentChapter()
    debugStatePreview = JsonObject().apply {
      addProperty("mode", "debug")
      addProperty("worldName", debugWorldName)
      addProperty("chapterId", chapter?.id ?: 0L)
      addProperty("chapterTitle", chapter?.title ?: "")
      addProperty("messageCount", messages.size)
      addProperty("miniGameType", miniGameType)
      addProperty("miniGameStatus", miniGameStatus)
      addProperty("miniGameRound", miniGameRound)
    }.toString()
  }

  private fun buildMiniGameJson(): JsonObject {
    val mini = linkedMapOf<String, Any>(
      "gameType" to miniGameType,
      "status" to miniGameStatus,
      "round" to miniGameRound,
      "stage" to miniGameStage,
      "winner" to miniGameWinner,
      "rewards" to miniGameRewards.split(",").map { it.trim() }.filter { it.isNotBlank() },
      "notes" to miniGameNotes,
    )
    return repository.toJson(mini)
  }

  private fun parseChapterCondition(raw: String): JsonElement? {
    val text = normalizeScalarEditorText(raw).trim()
    if (text.isEmpty()) return null
    return runCatching { JsonParser.parseString(text) }.getOrElse {
      JsonObject().apply {
        addProperty("type", "contains")
        addProperty("field", "message")
        addProperty("value", text)
      }
    }
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
    voiceConfigId: Long?,
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
      voiceConfigId = voiceConfigId,
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
    playerVoiceConfigId = null
    playerVoicePresetId = ""
    playerVoiceMode = "text"
    playerVoiceReferenceAudioPath = ""
    playerVoiceReferenceAudioName = ""
    playerVoiceReferenceText = ""
    playerVoicePromptText = ""
    playerVoiceMixVoices = defaultMixVoices()
    narratorName = "旁白"
    narratorVoice = "默认旁白"
    narratorVoiceConfigId = null
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
    miniGameType = ""
    miniGameStatus = "idle"
    miniGameRound = 0
    miniGameStage = ""
    miniGameWinner = ""
    miniGameRewards = ""
    miniGameNotes = ""
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
    debugStatePreview = "{}"
    debugEndDialog = null
    debugChapterSequence = emptyList()
    debugMessageSeed = 1L
    voiceModels.clear()
    settingsTextConfigs.clear()
    settingsImageConfigs.clear()
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
    playerVoiceConfigId = null
    playerVoicePresetId = ""
    playerVoiceMode = "text"
    playerVoiceReferenceAudioPath = ""
    playerVoiceReferenceAudioName = ""
    playerVoiceReferenceText = ""
    playerVoicePromptText = ""
    playerVoiceMixVoices = defaultMixVoices()
    narratorName = "旁白"
    narratorVoice = "默认旁白"
    narratorVoiceConfigId = null
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
    miniGameType = ""
    miniGameStatus = "idle"
    miniGameRound = 0
    miniGameStage = ""
    miniGameWinner = ""
    miniGameRewards = ""
    miniGameNotes = ""
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
    playerVoiceConfigId = world.playerRole?.voiceConfigId
    playerVoicePresetId = world.playerRole?.voicePresetId ?: ""
    playerVoiceReferenceAudioPath = world.playerRole?.voiceReferenceAudioPath.orEmpty()
    playerVoiceReferenceAudioName = world.playerRole?.voiceReferenceAudioName.orEmpty()
    playerVoiceReferenceText = world.playerRole?.voiceReferenceText.orEmpty()
    playerVoicePromptText = world.playerRole?.voicePromptText.orEmpty()
    playerVoiceMixVoices = normalizedMixVoices(world.playerRole?.voiceMixVoices ?: emptyList())
    narratorName = world.narratorRole?.name ?: "旁白"
    narratorVoice = world.settings?.narratorVoice ?: world.narratorRole?.voice ?: "默认旁白"
    narratorVoiceMode = world.settings?.narratorVoiceMode ?: world.narratorRole?.voiceMode ?: "text"
    narratorVoiceConfigId = world.settings?.narratorVoiceConfigId ?: world.narratorRole?.voiceConfigId
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
    npcRoles.addAll((world.settings?.roles ?: emptyList()).filter { it.roleType == "npc" }.map(::resolveRoleMedia))

    val mini = world.settings?.miniGameState ?: MiniGameState()
    miniGameType = mini.gameType
    miniGameStatus = mini.status
    miniGameRound = mini.round
    miniGameStage = mini.stage
    miniGameWinner = mini.winner
    miniGameRewards = mini.rewards.joinToString(",")
    miniGameNotes = mini.notes
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
