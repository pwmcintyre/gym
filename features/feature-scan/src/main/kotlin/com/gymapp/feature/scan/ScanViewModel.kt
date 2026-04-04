package com.gymapp.feature.scan

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.ai.AiSettings
import com.gymapp.core.ai.ModelDownloadManager
import com.gymapp.core.ai.ModelState
import com.gymapp.core.ai.WorkoutCardParser
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.ExerciseEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class ScanUiState(
    val isCapturing: Boolean = false,
    val isParsing: Boolean = false,
    val statusText: String? = null,
    val error: String? = null,
    /** Non-null once the AI has returned results and user is reviewing. */
    val reviewItems: List<ExerciseEntry>? = null,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val parser: WorkoutCardParser,
    private val workoutRepository: WorkoutRepository,
    private val aiSettings: AiSettings,
    private val modelDownloadManager: ModelDownloadManager,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    val knownExerciseNames: StateFlow<List<String>> =
        workoutRepository.observeDistinctExerciseNames()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** True when OpenAI key is set (cloud scan available). */
    val hasApiKey: StateFlow<Boolean> = aiSettings.apiKey
        .map { it.isNotBlank() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    /** True when local Gemma model is downloaded and ready. */
    val isLocalModelReady: StateFlow<Boolean> = modelDownloadManager.state
        .map { it is ModelState.Ready }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private var imageCapture: ImageCapture? = null

    private var cameraProvider: ProcessCameraProvider? = null

    fun bindCamera(context: Context, lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val provider = cameraProviderFuture.get()
            cameraProvider = provider

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture

            provider.unbindAll()
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture,
            )
        }, ContextCompat.getMainExecutor(context))
    }

    /** Release the camera sensor (e.g. while the photo picker is open). */
    fun unbindCamera() {
        cameraProvider?.unbindAll()
    }

    fun captureAndParse(executor: Executor) {
        val capture = imageCapture ?: return
        _uiState.update { it.copy(isCapturing = true, error = null) }

        viewModelScope.launch {
            val bytes = runCatching { captureJpeg(capture, executor) }
            bytes.onFailure { e ->
                _uiState.update { it.copy(isCapturing = false, error = e.message) }
                return@launch
            }
            _uiState.update { it.copy(isCapturing = false, isParsing = true, statusText = "Processing image…") }

            val resized = withContext(Dispatchers.Default) { bytes.getOrThrow().resizeToMax(1280) }
            val result = parser.parse(resized) { msg -> _uiState.update { it.copy(statusText = msg) } }
            result.onSuccess { entries ->
                _uiState.update { it.copy(isParsing = false, statusText = null, reviewItems = entries) }
            }
            result.onFailure { e ->
                _uiState.update { it.copy(isParsing = false, statusText = null, error = e.message) }
            }
        }
    }

    fun updateReviewItem(index: Int, item: ExerciseEntry) {
        _uiState.update { state ->
            val updated = state.reviewItems?.toMutableList() ?: return@update state
            updated[index] = item
            state.copy(reviewItems = updated)
        }
    }

    fun removeReviewItem(index: Int) {
        _uiState.update { state ->
            val updated = state.reviewItems?.toMutableList() ?: return@update state
            updated.removeAt(index)
            state.copy(reviewItems = updated)
        }
    }

    fun addReviewItem() {
        _uiState.update { state ->
            val list = state.reviewItems?.toMutableList() ?: mutableListOf()
            val nextLabel = nextLabel(list)
            list.add(
                ExerciseEntry(
                    id = java.util.UUID.randomUUID().toString(),
                    workoutSessionId = "",
                    label = nextLabel,
                    exerciseName = "",
                    targetSets = null,
                    targetReps = null,
                    targetModifier = com.gymapp.core.model.RepModifier.NONE,
                    targetRawText = null,
                    modifierTags = emptyList(),
                    notes = null,
                )
            )
            state.copy(reviewItems = list)
        }
    }

    fun startWorkout(onCreated: (sessionId: String) -> Unit) {
        val items = _uiState.value.reviewItems ?: return
        viewModelScope.launch {
            val session = workoutRepository.create(date = System.currentTimeMillis())
            items.filter { it.exerciseName.isNotBlank() }.forEach { entry ->
                workoutRepository.addExercise(
                    sessionId = session.id,
                    label = entry.label,
                    exerciseName = entry.exerciseName,
                    targetSets = entry.targetSets,
                    targetReps = entry.targetReps,
                    targetModifier = entry.targetModifier,
                    targetRawText = entry.targetRawText,
                    modifierTags = entry.modifierTags,
                    notes = entry.notes,
                )
            }
            onCreated(session.id)
        }
    }

    /** Parse an already-obtained byte array (e.g. from image picker). */
    fun parseBytes(bytes: ByteArray) {
        _uiState.update { it.copy(isParsing = true, error = null) }
        viewModelScope.launch {
            parser.parse(bytes)
                .onSuccess { entries -> _uiState.update { it.copy(isParsing = false, reviewItems = entries) } }
                .onFailure { e -> _uiState.update { it.copy(isParsing = false, error = e.message) } }
        }
    }

    /** Read a content URI off the main thread, then parse. */
    fun parseUri(uri: Uri, contentResolver: android.content.ContentResolver) {
        _uiState.update { it.copy(isParsing = true, statusText = "Reading image…", error = null) }
        viewModelScope.launch {
            val bytes = runCatching {
                withContext(Dispatchers.IO) {
                    contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: error("Could not open image")
                }
            }
            bytes.onFailure { e ->
                _uiState.update { it.copy(isParsing = false, statusText = null, error = e.message) }
                return@launch
            }
            _uiState.update { it.copy(statusText = "Processing image…") }
            val resized = withContext(Dispatchers.Default) { bytes.getOrThrow().resizeToMax(1280) }
            parser.parse(resized) { msg -> _uiState.update { it.copy(statusText = msg) } }
                .onSuccess { entries -> _uiState.update { it.copy(isParsing = false, statusText = null, reviewItems = entries) } }
                .onFailure { e -> _uiState.update { it.copy(isParsing = false, statusText = null, error = e.message) } }
        }
    }

    fun dismissError() = _uiState.update { it.copy(error = null) }
    fun resetReview() = _uiState.update { it.copy(reviewItems = null) }
}

private suspend fun captureJpeg(capture: ImageCapture, executor: Executor): ByteArray =
    suspendCoroutine { cont ->
        val out = ByteArrayOutputStream()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(out).build()
        capture.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                    cont.resume(out.toByteArray())
                }

                override fun onError(exc: ImageCaptureException) {
                    cont.resumeWithException(exc)
                }
            },
        )
    }

/**
 * Downsample image bytes so the longest side is at most [maxPx].
 * 4K whiteboard photos (4000×3000) are reduced to 1280×960 — plenty for OCR,
 * and ~90% fewer pixels to process / encode.
 */
private fun ByteArray.resizeToMax(maxPx: Int, quality: Int = 85): ByteArray {
    val original = BitmapFactory.decodeByteArray(this, 0, size) ?: return this
    val w = original.width
    val h = original.height
    if (w <= maxPx && h <= maxPx) {
        original.recycle()
        return this
    }
    val scale = maxPx.toFloat() / maxOf(w, h)
    val scaled = Bitmap.createScaledBitmap(original, (w * scale).toInt(), (h * scale).toInt(), true)
    original.recycle()
    val out = ByteArrayOutputStream()
    scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
    scaled.recycle()
    return out.toByteArray()
}

private fun nextLabel(items: List<ExerciseEntry>): String {
    val last = items.lastOrNull()?.label ?: return "A1"
    val letter = last.firstOrNull { it.isLetter() } ?: 'A'
    val num = last.filter { it.isDigit() }.toIntOrNull() ?: 1
    return if (num < 3) "$letter${num + 1}" else "${letter + 1}1"
}
