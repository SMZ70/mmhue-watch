package com.smz70.mmhue.watch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Thin client over the mmhue panel API.
 *
 * Every call is suspend and hits the network on Dispatchers.IO. Timeouts are
 * short on purpose: on a watch a request that hangs for 30s is worse than one
 * that fails fast and lets the UI roll back an optimistic toggle.
 */
class MmhueClient(
    private val baseUrlProvider: () -> String = { AppConfig.baseUrl },
    private val http: OkHttpClient = defaultHttpClient(),
) {
    /** Fixed-URL constructor, used by tests against a mock server. */
    constructor(baseUrl: String) : this(baseUrlProvider = { baseUrl })

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun state(): HomeState = withContext(Dispatchers.IO) {
        val body = get("/api/state")
        json.decodeFromString(HomeState.serializer(), body)
    }

    suspend fun allOn() = post("/api/all/on")

    suspend fun allOff() = post("/api/all/off")

    suspend fun setRoom(roomId: String, on: Boolean) =
        post("/api/rooms/$roomId/${on.onOff()}")

    suspend fun toggleLight(lightId: String) =
        post("/api/lights/$lightId/toggle")

    suspend fun setBrightness(lightId: String, pct: Int) =
        post("/api/lights/$lightId/brightness/${Brightness.clamp(pct)}")

    /** hue: 0-360 degrees, sat: 0-1. Matches mmhue's /color/{hue}/{sat} endpoint. */
    suspend fun setColor(lightId: String, hue: Float, sat: Float) {
        val h = ((hue % 360f) + 360f) % 360f
        val s = sat.coerceIn(0f, 1f)
        post("/api/lights/$lightId/color/$h/$s")
    }

    /** mirek: 153 (cool) - 500 (warm). Matches mmhue's /ct/{mirek} endpoint. */
    suspend fun setColorTemp(lightId: String, mirek: Int) =
        post("/api/lights/$lightId/ct/${ColorTemp.clamp(mirek)}")

    private fun Boolean.onOff() = if (this) "on" else "off"

    private suspend fun get(path: String): String = withContext(Dispatchers.IO) {
        execute(Request.Builder().url(baseUrlProvider() + path).get().build())
    }

    private suspend fun post(path: String): Unit = withContext(Dispatchers.IO) {
        execute(
            Request.Builder()
                .url(baseUrlProvider() + path)
                .post(ByteArray(0).toRequestBody(null))
                .build()
        )
        Unit
    }

    private fun execute(request: Request): String {
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                throw IOException("${request.method} ${request.url.encodedPath} -> HTTP ${response.code}")
            }
            return body
        }
    }

    companion object {
        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(3, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }
}
