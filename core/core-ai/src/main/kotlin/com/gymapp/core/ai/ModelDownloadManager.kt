package com.gymapp.core.ai

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// Max number of redirects to follow manually (OkHttp follows up to 20 automatically,
// but we do it ourselves to ensure cross-host redirects strip auth headers cleanly).
private const val MAX_REDIRECTS = 10

/**
 * Download and lifecycle states for the local Gemma model file.
 */
sealed class ModelState {
    /** Model file is not on device yet. */
    object NotDownloaded : ModelState()

    /** Currently downloading — [progress] is 0..100 or -1 if content-length unknown. */
    data class Downloading(val progress: Int) : ModelState()

    /** File exists on device and is ready to load. */
    data class Ready(val path: String) : ModelState()

    /** A download or file-system error occurred. */
    data class Error(val message: String) : ModelState()
}

private const val MODEL_FILENAME = "qwen3-0.6b.litertlm"

// Qwen3-0.6B — 614 MB, ungated, ~3× fewer params than Gemma 4 E2B, same LiteRT-LM format.
private const val MODEL_URL =
    "https://huggingface.co/litert-community/Qwen3-0.6B/resolve/main/Qwen3-0.6B.litertlm?download=true"

// Legacy Gemma file left over from earlier builds — cleaned up on next launch.
private const val LEGACY_MODEL_FILENAME = "gemma-4-e2b-it.litertlm"

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow<ModelState>(ModelState.NotDownloaded)
    val state: Flow<ModelState> = _state.asStateFlow()

    private val modelFile: File
        get() = File(context.filesDir, MODEL_FILENAME)

    init {
        // Clean up the old Gemma file to reclaim ~2.6 GB.
        File(context.filesDir, LEGACY_MODEL_FILENAME).takeIf { it.exists() }?.delete()

        if (modelFile.exists() && modelFile.length() > 0) {
            _state.value = ModelState.Ready(modelFile.absolutePath)
        }
    }

    fun modelPath(): String? = modelFile.takeIf { it.exists() && it.length() > 0 }?.absolutePath

    suspend fun download() {
        if (_state.value is ModelState.Downloading) return

        withContext(Dispatchers.IO) {
            try {
                _state.value = ModelState.Downloading(0)

                // Disable automatic redirects so we can handle cross-host redirects manually
                // (avoids forwarding auth headers to CDN hosts, which causes 403/404).
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(0, TimeUnit.SECONDS) // 0 = no timeout — needed for 2.6 GB file
                    .followRedirects(false)
                    .followSslRedirects(false)
                    .build()

                // Follow redirects manually, stripping headers on cross-host hops.
                var url = MODEL_URL
                var redirectsLeft = MAX_REDIRECTS
                var response = client.newCall(Request.Builder().url(url).build()).execute()

                while (response.code in 300..399 && redirectsLeft-- > 0) {
                    val location = response.header("Location") ?: break
                    response.close()
                    // Build a clean request to the redirect target (no extra headers).
                    url = location
                    response = client.newCall(Request.Builder().url(url).build()).execute()
                }

                if (!response.isSuccessful) {
                    _state.value = ModelState.Error("Download failed: HTTP ${response.code} (url=$url)")
                    response.close()
                    return@withContext
                }

                val body = response.body ?: run {
                    _state.value = ModelState.Error("Empty response body")
                    response.close()
                    return@withContext
                }

                val contentLength = body.contentLength()
                val tmpFile = File(context.filesDir, "$MODEL_FILENAME.tmp")

                body.byteStream().use { input ->
                    tmpFile.outputStream().use { output ->
                        val buffer = ByteArray(8 * 1024)
                        var downloaded = 0L
                        var bytes: Int
                        while (input.read(buffer).also { bytes = it } != -1) {
                            output.write(buffer, 0, bytes)
                            downloaded += bytes
                            if (contentLength > 0) {
                                val progress = (downloaded * 100 / contentLength).toInt()
                                _state.value = ModelState.Downloading(progress)
                            }
                        }
                    }
                }

                tmpFile.renameTo(modelFile)
                _state.value = ModelState.Ready(modelFile.absolutePath)
            } catch (e: Exception) {
                _state.value = ModelState.Error(e.message ?: "Unknown download error")
            }
        }
    }

    fun deleteModel() {
        modelFile.delete()
        _state.value = ModelState.NotDownloaded
    }
}
