package com.toonflow.game.data

import com.google.gson.JsonElement
import com.google.gson.annotations.SerializedName

data class ApiEnvelope<T>(
  @SerializedName("code") val code: Int,
  @SerializedName("data") val data: T,
  @SerializedName("message") val message: String,
)

data class ProjectItem(
  @SerializedName("id") val id: Long,
  @SerializedName("name") val name: String,
  @SerializedName("intro") val intro: String? = null,
)

data class RoleParameterCard(
  @SerializedName("name") val name: String = "",
  @SerializedName("raw_setting") val rawSetting: String = "",
  @SerializedName("gender") val gender: String = "",
  @SerializedName("age") val age: Int? = null,
  @SerializedName("level") val level: Int = 1,
  @SerializedName("level_desc") val levelDesc: String = "初入此界",
  @SerializedName("personality") val personality: String = "",
  @SerializedName("appearance") val appearance: String = "",
  @SerializedName("voice") val voice: String = "",
  @SerializedName("skills") val skills: List<String> = emptyList(),
  @SerializedName("items") val items: List<String> = emptyList(),
  @SerializedName("equipment") val equipment: List<String> = emptyList(),
  @SerializedName("hp") val hp: Int = 100,
  @SerializedName("mp") val mp: Int = 0,
  @SerializedName("money") val money: Int = 0,
  @SerializedName("other") val other: List<String> = emptyList(),
)

data class StoryRole(
  @SerializedName("id") val id: String,
  @SerializedName("roleType") val roleType: String,
  @SerializedName("name") val name: String,
  @SerializedName("avatarPath") val avatarPath: String = "",
  @SerializedName("avatarBgPath") val avatarBgPath: String = "",
  @SerializedName("description") val description: String = "",
  @SerializedName("voice") val voice: String = "",
  @SerializedName("voiceMode") val voiceMode: String = "text",
  @SerializedName("voiceConfigId") val voiceConfigId: Long? = null,
  @SerializedName("voicePresetId") val voicePresetId: String = "",
  @SerializedName("voiceReferenceAudioPath") val voiceReferenceAudioPath: String = "",
  @SerializedName("voiceReferenceAudioName") val voiceReferenceAudioName: String = "",
  @SerializedName("voiceReferenceText") val voiceReferenceText: String = "",
  @SerializedName("voicePromptText") val voicePromptText: String = "",
  @SerializedName("voiceMixVoices") val voiceMixVoices: List<VoiceMixItem>? = emptyList(),
  @SerializedName("sample") val sample: String = "",
  @SerializedName("parameterCardJson") val parameterCardJson: RoleParameterCard? = null,
)

data class VoiceMixItem(
  @SerializedName("voiceId") val voiceId: String = "",
  @SerializedName("weight") val weight: Double = 0.7,
)

data class VoiceBindingDraft(
  val label: String = "",
  val configId: Long? = null,
  val presetId: String = "",
  val mode: String = "text",
  val referenceAudioPath: String = "",
  val referenceAudioName: String = "",
  val referenceText: String = "",
  val promptText: String = "",
  val mixVoices: List<VoiceMixItem> = emptyList(),
)

data class ChapterExtra(
  @SerializedName("chapterId") val chapterId: Long? = null,
  @SerializedName("sort") val sort: Int = 0,
  @SerializedName("openingRole") val openingRole: String = "旁白",
  @SerializedName("openingLine") val openingLine: String = "",
  @SerializedName("background") val background: String = "",
  @SerializedName("music") val music: String = "",
  @SerializedName("conditionVisible") val conditionVisible: Boolean = true,
)

data class WorldSettings(
  @SerializedName("roles") val roles: List<StoryRole> = emptyList(),
  @SerializedName("narratorVoice") val narratorVoice: String = "默认旁白",
  @SerializedName("narratorVoiceMode") val narratorVoiceMode: String = "text",
  @SerializedName("narratorVoiceConfigId") val narratorVoiceConfigId: Long? = null,
  @SerializedName("narratorVoicePresetId") val narratorVoicePresetId: String = "",
  @SerializedName("narratorVoiceReferenceAudioPath") val narratorVoiceReferenceAudioPath: String = "",
  @SerializedName("narratorVoiceReferenceAudioName") val narratorVoiceReferenceAudioName: String = "",
  @SerializedName("narratorVoiceReferenceText") val narratorVoiceReferenceText: String = "",
  @SerializedName("narratorVoicePromptText") val narratorVoicePromptText: String = "",
  @SerializedName("narratorVoiceMixVoices") val narratorVoiceMixVoices: List<VoiceMixItem> = emptyList(),
  @SerializedName("globalBackground") val globalBackground: String = "",
  @SerializedName("coverPath") val coverPath: String = "",
  @SerializedName("coverBgPath") val coverBgPath: String = "",
  @SerializedName("allowRoleView") val allowRoleView: Boolean = true,
  @SerializedName("allowChatShare") val allowChatShare: Boolean = true,
  @SerializedName("publishStatus") val publishStatus: String = "draft",
  @SerializedName("chapterExtras") val chapterExtras: List<ChapterExtra> = emptyList(),
)

data class WorldItem(
  @SerializedName("id") val id: Long,
  @SerializedName("projectId") val projectId: Long,
  @SerializedName("name") val name: String,
  @SerializedName("intro") val intro: String = "",
  @SerializedName("coverPath") val coverPath: String = "",
  @SerializedName("coverBgPath") val coverBgPath: String = "",
  @SerializedName("publishStatus") val publishStatus: String = "",
  @SerializedName("updateTime") val updateTime: Long = 0L,
  @SerializedName("chapterCount") val chapterCount: Int? = 0,
  @SerializedName("sessionCount") val sessionCount: Int? = 0,
  @SerializedName("settings") val settings: WorldSettings? = null,
  @SerializedName("playerRole") val playerRole: StoryRole? = null,
  @SerializedName("narratorRole") val narratorRole: StoryRole? = null,
)

data class ChapterItem(
  @SerializedName("id") val id: Long,
  @SerializedName("title") val title: String,
  @SerializedName("content") val content: String = "",
  @SerializedName("entryCondition") val entryCondition: JsonElement? = null,
  @SerializedName("sort") val sort: Int = 0,
  @SerializedName("status") val status: String = "draft",
  @SerializedName("completionCondition") val completionCondition: JsonElement? = null,
  @SerializedName("chapterKey") val chapterKey: String = "",
  @SerializedName("backgroundPath") val backgroundPath: String = "",
  @SerializedName("openingRole") val openingRole: String = "",
  @SerializedName("openingText") val openingText: String = "",
  @SerializedName("bgmPath") val bgmPath: String = "",
  @SerializedName("showCompletionCondition") val showCompletionCondition: Boolean = true,
)

data class MessageItem(
  @SerializedName("id") val id: Long,
  @SerializedName("role") val role: String = "",
  @SerializedName("roleType") val roleType: String = "",
  @SerializedName("eventType") val eventType: String = "",
  @SerializedName("content") val content: String = "",
  @SerializedName("createTime") val createTime: Long = 0L,
  @SerializedName("meta") val meta: JsonElement? = null,
)

data class SessionItem(
  @SerializedName("sessionId") val sessionId: String,
  @SerializedName("worldId") val worldId: Long,
  @SerializedName("worldName") val worldName: String = "",
  @SerializedName("worldIntro") val worldIntro: String = "",
  @SerializedName("worldCoverPath") val worldCoverPath: String = "",
  @SerializedName("chapterId") val chapterId: Long? = null,
  @SerializedName("chapterTitle") val chapterTitle: String = "",
  @SerializedName("projectId") val projectId: Long? = null,
  @SerializedName("projectName") val projectName: String = "",
  @SerializedName("title") val title: String = "",
  @SerializedName("status") val status: String = "",
  @SerializedName("updateTime") val updateTime: Long = 0L,
  @SerializedName("latestMessage") val latestMessage: MessageItem? = null,
)

data class SessionSnapshot(
  @SerializedName("state") val state: JsonElement? = null,
)

data class SessionDetail(
  @SerializedName("sessionId") val sessionId: String = "",
  @SerializedName("title") val title: String = "",
  @SerializedName("status") val status: String = "",
  @SerializedName("chapterId") val chapterId: Long? = null,
  @SerializedName("state") val state: JsonElement? = null,
  @SerializedName("latestSnapshot") val latestSnapshot: SessionSnapshot? = null,
  @SerializedName("world") val world: WorldItem? = null,
  @SerializedName("chapter") val chapter: ChapterItem? = null,
  @SerializedName("messages") val messages: List<MessageItem> = emptyList(),
)

data class SessionNarrativeResult(
  @SerializedName("sessionId") val sessionId: String = "",
  @SerializedName("status") val status: String = "",
  @SerializedName("chapterId") val chapterId: Long? = null,
  @SerializedName("chapter") val chapter: ChapterItem? = null,
  @SerializedName("state") val state: JsonElement? = null,
  @SerializedName("message") val message: MessageItem? = null,
  @SerializedName("chapterSwitchMessage") val chapterSwitchMessage: MessageItem? = null,
  @SerializedName("narrativeMessage") val narrativeMessage: MessageItem? = null,
  @SerializedName("generatedMessages") val generatedMessages: List<MessageItem> = emptyList(),
  @SerializedName("narrativePlan") val narrativePlan: DebugNarrativePlan? = null,
  @SerializedName("snapshotSaved") val snapshotSaved: Boolean = false,
  @SerializedName("snapshotReason") val snapshotReason: String = "",
)

data class SessionOrchestrationResult(
  @SerializedName("sessionId") val sessionId: String = "",
  @SerializedName("status") val status: String = "",
  @SerializedName("chapterId") val chapterId: Long? = null,
  @SerializedName("expectedRole") val expectedRole: String = "",
  @SerializedName("expectedRoleType") val expectedRoleType: String = "",
  @SerializedName("plan") val plan: DebugNarrativePlan? = null,
)

data class DebugStepResult(
  @SerializedName("chapterId") val chapterId: Long? = null,
  @SerializedName("chapterTitle") val chapterTitle: String = "",
  @SerializedName("state") val state: JsonElement? = null,
  @SerializedName("endDialog") val endDialog: String? = null,
  @SerializedName("messages") val messages: List<MessageItem> = emptyList(),
)

data class DebugNarrativePlan(
  @SerializedName("role") val role: String = "",
  @SerializedName("roleType") val roleType: String = "",
  @SerializedName("motive") val motive: String = "",
  @SerializedName("awaitUser") val awaitUser: Boolean = false,
  @SerializedName("nextRole") val nextRole: String = "",
  @SerializedName("nextRoleType") val nextRoleType: String = "",
  @SerializedName("chapterOutcome") val chapterOutcome: String = "continue",
  @SerializedName("nextChapterId") val nextChapterId: Long? = null,
  @SerializedName("source") val source: String = "ai",
  @SerializedName("triggerMemoryAgent") val triggerMemoryAgent: Boolean = false,
  @SerializedName("eventType") val eventType: String = "",
  @SerializedName("presetContent") val presetContent: String? = null,
)

data class DebugOrchestrationResult(
  @SerializedName("chapterId") val chapterId: Long? = null,
  @SerializedName("chapterTitle") val chapterTitle: String = "",
  @SerializedName("state") val state: JsonElement? = null,
  @SerializedName("endDialog") val endDialog: String? = null,
  @SerializedName("plan") val plan: DebugNarrativePlan? = null,
)

data class GeneratedImageResult(
  @SerializedName("path") val path: String = "",
  @SerializedName("filePath") val filePath: String = "",
)

data class SeparatedRoleImageResult(
  @SerializedName("foregroundPath") val foregroundPath: String = "",
  @SerializedName("foregroundFilePath") val foregroundFilePath: String = "",
  @SerializedName("backgroundPath") val backgroundPath: String = "",
  @SerializedName("backgroundFilePath") val backgroundFilePath: String = "",
)

data class RoleAvatarTaskResult(
  @SerializedName("taskId") val taskId: Long = 0L,
  @SerializedName("status") val status: String = "",
  @SerializedName("progress") val progress: Int? = null,
  @SerializedName("message") val message: String = "",
  @SerializedName("errorMessage") val errorMessage: String = "",
  @SerializedName("foregroundPath") val foregroundPath: String = "",
  @SerializedName("foregroundFilePath") val foregroundFilePath: String = "",
  @SerializedName("backgroundPath") val backgroundPath: String = "",
  @SerializedName("backgroundFilePath") val backgroundFilePath: String = "",
)

data class VoiceModelConfig(
  @SerializedName("id") val id: Long,
  @SerializedName("type") val type: String = "",
  @SerializedName("model") val model: String = "",
  @SerializedName("modelType") val modelType: String = "",
  @SerializedName("manufacturer") val manufacturer: String = "",
  @SerializedName("baseUrl") val baseUrl: String = "",
  @SerializedName("apiKey") val apiKey: String = "",
  @SerializedName("modes") val modes: List<String> = emptyList(),
  @SerializedName("createTime") val createTime: Long = 0L,
)

data class ModelConfigItem(
  @SerializedName("id") val id: Long,
  @SerializedName("type") val type: String = "",
  @SerializedName("model") val model: String = "",
  @SerializedName("modelType") val modelType: String = "",
  @SerializedName("manufacturer") val manufacturer: String = "",
  @SerializedName("baseUrl") val baseUrl: String = "",
  @SerializedName("apiKey") val apiKey: String = "",
  @SerializedName("createTime") val createTime: Long = 0L,
)

data class LocalAvatarMattingStatus(
  @SerializedName("manufacturer") val manufacturer: String = "",
  @SerializedName("model") val model: String = "",
  @SerializedName("status") val status: String = "not_installed",
  @SerializedName("installed") val installed: Boolean = false,
  @SerializedName("canInstall") val canInstall: Boolean = false,
  @SerializedName("message") val message: String = "",
)

data class AiModelMapItem(
  @SerializedName("id") val id: Long,
  @SerializedName("key") val key: String = "",
  @SerializedName("name") val name: String = "",
  @SerializedName("configId") val configId: Long? = null,
  @SerializedName("model") val model: String? = null,
  @SerializedName("manufacturer") val manufacturer: String? = null,
)

data class AiModelOptionItem(
  @SerializedName("label") val label: String = "",
  @SerializedName("value") val value: String = "",
)

data class PromptItem(
  @SerializedName("id") val id: Long,
  @SerializedName("code") val code: String = "",
  @SerializedName("name") val name: String? = null,
  @SerializedName("type") val type: String? = null,
  @SerializedName("parentCode") val parentCode: String? = null,
  @SerializedName("defaultValue") val defaultValue: String? = null,
  @SerializedName("customValue") val customValue: String? = null,
)

data class VoicePresetItem(
  @SerializedName("voiceId") val voiceId: String,
  @SerializedName("name") val name: String,
  @SerializedName("provider") val provider: String? = null,
  @SerializedName("modes") val modes: List<String> = emptyList(),
  @SerializedName("description") val description: String? = null,
)

data class UploadedVoiceAudioResult(
  @SerializedName("filePath") val filePath: String = "",
  @SerializedName("url") val url: String = "",
)
