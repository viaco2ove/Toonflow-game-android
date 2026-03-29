package com.toonflow.game.api

import com.toonflow.game.data.SettingsStore
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
  fun create(settingsStore: SettingsStore): GameApi {
    val logger = HttpLoggingInterceptor().apply {
      level = HttpLoggingInterceptor.Level.BASIC
    }

    val client = OkHttpClient.Builder()
      .addInterceptor { chain ->
        val builder = chain.request().newBuilder()
        val token = settingsStore.token.trim()
        if (token.isNotEmpty()) {
          builder.addHeader("Authorization", token)
        }
        chain.proceed(builder.build())
      }
      .addInterceptor(logger)
      .connectTimeout(20, TimeUnit.SECONDS)
      .readTimeout(120, TimeUnit.SECONDS)
      .writeTimeout(120, TimeUnit.SECONDS)
      .callTimeout(150, TimeUnit.SECONDS)
      .build()

    val baseUrl = settingsStore.baseUrl.trim().let {
      if (it.endsWith("/")) it else "$it/"
    }

    return Retrofit.Builder()
      .baseUrl(baseUrl)
      .client(client)
      .addConverterFactory(GsonConverterFactory.create())
      .build()
      .create(GameApi::class.java)
  }
}
