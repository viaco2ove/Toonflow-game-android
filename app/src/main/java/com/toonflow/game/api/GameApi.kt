package com.toonflow.game.api

import com.google.gson.JsonObject
import com.toonflow.game.data.ApiEnvelope
import com.toonflow.game.data.ChapterItem
import com.toonflow.game.data.GeneratedImageResult
import com.toonflow.game.data.MessageItem
import com.toonflow.game.data.ProjectItem
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

  @POST("project/getProject")
  suspend fun getProjects(@Body payload: JsonObject = JsonObject()): ApiEnvelope<List<ProjectItem>>

  @GET("user/getUser")
  suspend fun getUser(): ApiEnvelope<JsonObject>

  @POST("game/getWorld")
  suspend fun getWorld(@Body payload: JsonObject): ApiEnvelope<WorldItem>

  @POST("game/listWorlds")
  suspend fun listWorlds(@Body payload: JsonObject = JsonObject()): ApiEnvelope<List<WorldItem>>

  @POST("game/saveWorld")
  suspend fun saveWorld(@Body payload: JsonObject): ApiEnvelope<WorldItem>

  @POST("game/generateImage")
  suspend fun generateImage(@Body payload: JsonObject): ApiEnvelope<GeneratedImageResult>

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

  @POST("setting/getVoiceModelList")
  suspend fun getVoiceModelList(@Body payload: JsonObject = JsonObject()): ApiEnvelope<List<VoiceModelConfig>>

  @POST("voice/getVoices")
  suspend fun getVoices(@Body payload: JsonObject): ApiEnvelope<JsonElement>

  @POST("voice/uploadAudio")
  suspend fun uploadVoiceAudio(@Body payload: JsonObject): ApiEnvelope<UploadedVoiceAudioResult>

  @POST("voice/preview")
  suspend fun previewVoice(@Body payload: JsonObject): ApiEnvelope<JsonObject>
}
