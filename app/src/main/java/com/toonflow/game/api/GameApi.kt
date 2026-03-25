package com.toonflow.game.api

import com.google.gson.JsonObject
import com.toonflow.game.data.ApiEnvelope
import com.toonflow.game.data.AiModelMapItem
import com.toonflow.game.data.ChapterItem
import com.toonflow.game.data.DebugStepResult
import com.toonflow.game.data.GeneratedImageResult
import com.toonflow.game.data.MessageItem
import com.toonflow.game.data.ModelConfigItem
import com.toonflow.game.data.ProjectItem
import com.toonflow.game.data.PromptItem
import com.toonflow.game.data.SessionDetail
import com.toonflow.game.data.SessionItem
import com.toonflow.game.data.UploadedVoiceAudioResult
import com.toonflow.game.data.VoiceModelConfig
import com.toonflow.game.data.WorldItem
import com.google.gson.JsonElement
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
  suspend fun deleteWorld(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("game/generateImage")
  suspend fun generateImage(@Body payload: JsonObject): ApiEnvelope<GeneratedImageResult>

  @POST("game/uploadImage")
  suspend fun uploadImage(@Body payload: JsonObject): ApiEnvelope<GeneratedImageResult>

  @POST("game/getChapter")
  suspend fun getChapter(@Body payload: JsonObject): ApiEnvelope<List<ChapterItem>>

  @POST("game/saveChapter")
  suspend fun saveChapter(@Body payload: JsonObject): ApiEnvelope<ChapterItem>

  @POST("game/startSession")
  suspend fun startSession(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("game/listSession")
  suspend fun listSession(@Body payload: JsonObject): ApiEnvelope<List<SessionItem>>

  @POST("game/getSession")
  suspend fun getSession(@Body payload: JsonObject): ApiEnvelope<SessionDetail>

  @POST("game/getMessage")
  suspend fun getMessage(@Body payload: JsonObject): ApiEnvelope<List<MessageItem>>

  @POST("game/addMessage")
  suspend fun addMessage(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("game/debugStep")
  suspend fun debugStep(@Body payload: JsonObject): ApiEnvelope<DebugStepResult>

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

  @POST("setting/configurationModel")
  suspend fun bindModelConfig(@Body payload: JsonObject): ApiEnvelope<String>

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

  @POST("voice/polishPrompt")
  suspend fun polishVoicePrompt(@Body payload: JsonObject): ApiEnvelope<JsonObject>

  @POST("other/testAI")
  suspend fun testTextModel(@Body payload: JsonObject): ApiEnvelope<String>

  @POST("other/testImage")
  suspend fun testImageModel(@Body payload: JsonObject): ApiEnvelope<String>
}
