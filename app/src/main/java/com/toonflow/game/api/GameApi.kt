package com.toonflow.game.api

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.toonflow.game.data.ApiEnvelope
import com.toonflow.game.data.AiModelOptionItem
import com.toonflow.game.data.AiModelMapItem
import com.toonflow.game.data.AiTokenUsageLogItem
import com.toonflow.game.data.AiTokenUsageStatsItem
import com.toonflow.game.data.ChapterItem
import com.toonflow.game.data.DebugOrchestrationResult
import com.toonflow.game.data.DebugStepResult
import com.toonflow.game.data.GeneratedImageResult
import com.toonflow.game.data.LocalAvatarMattingStatus
import com.toonflow.game.data.MessageItem
import com.toonflow.game.data.ModelConfigItem
import com.toonflow.game.data.ProjectItem
import com.toonflow.game.data.PromptItem
import com.toonflow.game.data.RoleAvatarTaskResult
import com.toonflow.game.data.SeparatedRoleImageResult
import com.toonflow.game.data.SessionDetail
import com.toonflow.game.data.SessionItem
import com.toonflow.game.data.SessionNarrativeResult
import com.toonflow.game.data.SessionOrchestrationResult
import com.toonflow.game.data.StoryRuntimeConfig
import com.toonflow.game.data.UploadedVoiceAudioResult
import com.toonflow.game.data.VoiceModelConfig
import com.toonflow.game.data.WorldItem
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface GameApi {
  @POST("other/login")
  suspend fun login(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("other/register")
  suspend fun register(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("project/getProject")
  suspend fun getProjects(@Body payload: JsonObject = JsonObject()): ApiEnvelope<List<ProjectItem>>

  @GET("user/getUser")
  suspend fun getUser(): ApiEnvelope<JsonObject>

  @POST("user/saveUser")
  suspend fun saveUser(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("user/changePassword")
  suspend fun changePassword(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("game/getWorld")
  suspend fun getWorld(@Body payload: JsonObject): ApiEnvelope<WorldItem>

  @POST("game/listWorlds")
  suspend fun listWorlds(@Body payload: JsonObject = JsonObject()): ApiEnvelope<List<WorldItem>>

  @POST("game/saveWorld")
  suspend fun saveWorld(@Body payload: JsonObject): ApiEnvelope<WorldItem>

  @POST("game/deleteWorld")
  suspend fun deleteWorld(@Body payload: JsonObject): ApiEnvelope<JsonElement>

  @POST("game/generateImage")
  suspend fun generateImage(@Body payload: JsonObject): ApiEnvelope<GeneratedImageResult>

  @POST("game/uploadImage")
  suspend fun uploadImage(@Body payload: JsonObject): ApiEnvelope<GeneratedImageResult>

  @POST("game/convertAvatarVideoToGif")
  suspend fun convertAvatarVideoToGif(@Body payload: JsonObject): ApiEnvelope<SeparatedRoleImageResult>

  @POST("game/separateRoleAvatar")
  suspend fun separateRoleAvatar(@Body payload: JsonObject): ApiEnvelope<RoleAvatarTaskResult>

  @POST("game/separateRoleAvatar/status")
  suspend fun separateRoleAvatarStatus(@Body payload: JsonObject): ApiEnvelope<RoleAvatarTaskResult>

  @POST("game/getChapter")
  suspend fun getChapter(@Body payload: JsonObject): ApiEnvelope<List<ChapterItem>>

  @POST("game/saveChapter")
  suspend fun saveChapter(@Body payload: JsonObject): ApiEnvelope<ChapterItem>

  @POST("game/previewRuntimeOutline")
  suspend fun previewRuntimeOutline(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("game/startSession")
  suspend fun startSession(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("game/listSession")
  suspend fun listSession(@Body payload: JsonObject): ApiEnvelope<List<SessionItem>>

  @POST("game/getSession")
  suspend fun getSession(@Body payload: JsonObject): ApiEnvelope<SessionDetail>

  @POST("game/deleteSession")
  suspend fun deleteSession(@Body payload: JsonObject): ApiEnvelope<JsonElement>

  @POST("game/deleteMessage")
  suspend fun deleteMessage(@Body payload: JsonObject): ApiEnvelope<JsonElement>

  @POST("game/revisitMessage")
  suspend fun revisitMessage(@Body payload: JsonObject): ApiEnvelope<JsonElement>

  @POST("game/getMessage")
  suspend fun getMessage(@Body payload: JsonObject): ApiEnvelope<List<MessageItem>>

  @POST("game/addMessage")
  suspend fun addMessage(@Body payload: JsonObject): ApiEnvelope<SessionNarrativeResult>

  @POST("game/commitNarrativeTurn")
  suspend fun commitNarrativeTurn(@Body payload: JsonObject): ApiEnvelope<SessionNarrativeResult>

  @POST("game/continueSession")
  suspend fun continueSession(@Body payload: JsonObject): ApiEnvelope<SessionNarrativeResult>

  @POST("game/debugStep")
  suspend fun debugStep(@Body payload: JsonObject): ApiEnvelope<DebugStepResult>

  @POST("game/introduction")
  suspend fun introduceDebug(@Body payload: JsonObject): ApiEnvelope<DebugOrchestrationResult>

  @POST("game/orchestration")
  suspend fun orchestrateDebug(@Body payload: JsonObject): ApiEnvelope<DebugOrchestrationResult>

  @POST("game/orchestration")
  suspend fun orchestrateSession(@Body payload: JsonObject): ApiEnvelope<SessionOrchestrationResult>

  @POST("setting/getVoiceModelList")
  suspend fun getVoiceModelList(@Body payload: JsonObject = JsonObject()): ApiEnvelope<List<VoiceModelConfig>>

  @POST("setting/getSetting")
  suspend fun getModelConfigs(@Body payload: JsonObject = JsonObject()): ApiEnvelope<List<ModelConfigItem>>

  @POST("setting/addModel")
  suspend fun addModelConfig(@Body payload: JsonObject): ApiEnvelope<String>

  @POST("setting/updateModel")
  suspend fun updateModelConfig(@Body payload: JsonObject): ApiEnvelope<String>

  @POST("setting/delModel")
  suspend fun deleteModelConfig(@Body payload: JsonObject): ApiEnvelope<String>

  @POST("setting/getAiModelMap")
  suspend fun getAiModelMap(@Body payload: JsonObject = JsonObject()): ApiEnvelope<List<AiModelMapItem>>

  @POST("setting/getAiModelList")
  suspend fun getAiModelList(@Body payload: JsonObject): ApiEnvelope<Map<String, List<AiModelOptionItem>>>

  @POST("setting/getAiTokenUsageLog")
  suspend fun getAiTokenUsageLog(@Body payload: JsonObject): ApiEnvelope<List<AiTokenUsageLogItem>>

  @POST("setting/getAiTokenUsageStats")
  suspend fun getAiTokenUsageStats(@Body payload: JsonObject): ApiEnvelope<List<AiTokenUsageStatsItem>>

  @POST("setting/configurationModel")
  suspend fun bindModelConfig(@Body payload: JsonObject): ApiEnvelope<String>

  @POST("setting/saveStoryRuntimeConfig")
  suspend fun saveStoryRuntimeConfig(@Body payload: JsonObject): ApiEnvelope<StoryRuntimeConfig>

  @POST("setting/localAvatarMatting/status")
  suspend fun getLocalAvatarMattingStatus(@Body payload: JsonObject): ApiEnvelope<LocalAvatarMattingStatus>

  @POST("setting/localAvatarMatting/install")
  suspend fun installLocalAvatarMatting(@Body payload: JsonObject): ApiEnvelope<LocalAvatarMattingStatus>

  @GET("prompt/getPrompts")
  suspend fun getPrompts(): ApiEnvelope<List<PromptItem>>

  @POST("prompt/updatePrompt")
  suspend fun updatePrompt(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("voice/getVoices")
  suspend fun getVoices(@Body payload: JsonObject): ApiEnvelope<JsonElement>

  @POST("voice/uploadAudio")
  suspend fun uploadVoiceAudio(@Body payload: JsonObject): ApiEnvelope<UploadedVoiceAudioResult>

  @POST("voice/preview")
  suspend fun previewVoice(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("game/streamvoice")
  suspend fun streamVoice(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("voice/transcribe")
  suspend fun transcribeVoice(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("voice/polishPrompt")
  suspend fun polishVoicePrompt(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("other/testAI")
  suspend fun testTextModel(@Body payload: JsonObject): ApiEnvelope<String>

  @POST("other/testImage")
  suspend fun testImageModel(@Body payload: JsonObject): ApiEnvelope<String>

  @POST("other/testVoiceDesign")
  suspend fun testVoiceDesignModel(@Body payload: JsonObject): ApiEnvelope<String>
}
