package com.gymapp.feature.scan

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymapp.core.ai.WorkoutCardParser
import com.gymapp.core.database.repository.WorkoutRepository
import com.gymapp.core.model.ExerciseEntry
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

data class ScanUiState(
    val isCapturing: Boolean = false,
    val isParsing: Boolean = false,
    val error: String? = null,
    /** Non-null once the AI has returned results and user is reviewing. */
    val reviewItems: List<ExerciseEntry>? = null,
)

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val parser: WorkoutCardParser,
    private val workoutRepository: WorkoutRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private var imageCapture: ImageCapture? = null

    fun bindCamera(context: Context, lifecycleOwner: LifecycleOwner, surfaceProvider: Preview.SurfaceProvider) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surfaceProvider)
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            imageCapture = capture

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                capture,
            )
        }, ContextCompat.getMainExecutor(context))
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
            _uiState.update { it.copy(isCapturing = false, isParsing = true) }

            val result = parser.parse(bytes.getOrThrow())
            result.onSuccess { entries ->
                _uiState.update { it.copy(isParsing = false, reviewItems = entries) }
            }
            result.onFailure { e ->
                _uiState.update { it.copy(isParsing = false, error = e.message) }
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
                    notes = entry.notes,
                )
            }
            onCreated(session.id)
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

private fun nextLabel(items: List<ExerciseEntry>): String {
    val last = items.lastOrNull()?.label ?: return "A1"
    val letter = last.firstOrNull { it.isLetter() } ?: 'A'
    val num = last.filter { it.isDigit() }.toIntOrNull() ?: 1
    return if (num < 3) "$letter${num + 1}" else "${letter + 1}1"
}
