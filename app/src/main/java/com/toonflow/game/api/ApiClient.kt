package com.toonflow.game.api

import com.toonflow.game.data.SettingsStore
import com.toonflow.game.util.VueTagLogger
import okhttp3.OkHttpClient
import okio.Buffer
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {
  fun create(settingsStore: SettingsStore): GameApi {
    val client = OkHttpClient.Builder()
      .addInterceptor { chain ->
        val builder = chain.request().newBuilder()
        val token = settingsStore.token.trim()
        if (token.isNotEmpty()) {
          builder.addHeader("Authorization", token)
        }
        val request = builder.build()
        val requestBody = runCatching {
          request.body?.let { body ->
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
          }.orEmpty()
        }.getOrDefault("")
        val requestPath = request.url.encodedPath
        val startedAt = System.nanoTime()
        VueTagLogger.info(
          "network",
          "request ${request.method} $requestPath token=${if (token.isNotEmpty()) "attached" else "none"} body=${VueTagLogger.sanitize(requestBody)}",
        )
        try {
          val response = chain.proceed(request)
          val costMs = (System.nanoTime() - startedAt) / 1_000_000
          val responseBody = runCatching { response.peekBody(16L * 1024L).string() }.getOrDefault("")
          VueTagLogger.info(
            "network",
            "response ${response.code} ${request.method} $requestPath ${costMs}ms body=${VueTagLogger.sanitize(responseBody)}",
          )
          response
        } catch (error: Throwable) {
          val costMs = (System.nanoTime() - startedAt) / 1_000_000
          VueTagLogger.error(
            "network",
            "failed ${request.method} $requestPath ${costMs}ms error=${VueTagLogger.throwableMessage(error)} body=${VueTagLogger.sanitize(requestBody)}",
            error,
          )
          throw error
        }
      }
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
