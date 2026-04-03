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

private const val MODEL_FILENAME = "gemma-4-e2b-it.litertlm"

// Hugging Face direct download URL for Gemma 4 E2B
// Requires HF token only for gated repos; this community re-export is public.
private const val MODEL_URL =
    "https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm/resolve/main/gemma-4-E2B-it-cpu.litertlm"

@Singleton
class ModelDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val _state = MutableStateFlow<ModelState>(ModelState.NotDownloaded)
    val state: Flow<ModelState> = _state.asStateFlow()

    private val modelFile: File
        get() = File(context.filesDir, MODEL_FILENAME)

    init {
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

                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .followRedirects(true)
                    .build()

                val request = Request.Builder().url(MODEL_URL).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    _state.value = ModelState.Error("Download failed: HTTP ${response.code}")
                    return@withContext
                }

                val body = response.body ?: run {
                    _state.value = ModelState.Error("Empty response body")
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
