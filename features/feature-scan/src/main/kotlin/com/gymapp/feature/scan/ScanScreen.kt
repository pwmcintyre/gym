package com.gymapp.feature.scan

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onWorkoutReady: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val knownNames by viewModel.knownExerciseNames.collectAsStateWithLifecycle()
    val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
    val isLocalModelReady by viewModel.isLocalModelReady.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Incremented to force AndroidView to rebind the camera after the picker returns
    var cameraBindKey by remember { mutableIntStateOf(0) }
    fun rebindCamera() { cameraBindKey++ }

    // Holds the image chosen from the gallery while it is being analysed
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<ImageBitmap?>(null) }

    // Load bitmap off the main thread whenever the selected URI changes
    LaunchedEffect(selectedImageUri) {
        val uri = selectedImageUri
        selectedImageBitmap = if (uri != null) {
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)?.asImageBitmap()
                }
            }
        } else null
    }

    // When parsing finishes (success or error), clear the selected image and return to camera
    LaunchedEffect(uiState.isParsing) {
        if (!uiState.isParsing && selectedImageUri != null) {
            selectedImageUri = null
            rebindCamera()
        }
    }

    // --- Camera permission state ---
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // Request on first composition if not already granted
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // --- Image picker ---
    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            // Show the selected image while it is being analysed; camera stays unbound
            selectedImageUri = uri
            viewModel.parseUri(uri, context.contentResolver)
        } else {
            // Picker cancelled — return to live camera
            rebindCamera()
        }
    }

    // Show errors in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Review screen overlays once AI results arrive
    if (uiState.reviewItems != null) {
        ScanReviewScreen(
            items = uiState.reviewItems!!,
            knownNames = knownNames,
            onItemChanged = { index, item -> viewModel.updateReviewItem(index, item) },
            onItemRemoved = { index -> viewModel.removeReviewItem(index) },
            onItemAdded = { viewModel.addReviewItem() },
            onStartWorkout = { viewModel.startWorkout(onWorkoutReady) },
            onBack = { viewModel.resetReview() },
            modifier = modifier,
        )
        return
    }

    Scaffold(
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            val bitmap = selectedImageBitmap
            if (bitmap != null) {
                // Show the gallery-picked image while it is being analysed
                Image(
                    bitmap = bitmap,
                    contentDescription = "Selected image",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                )
            } else if (hasCameraPermission) {
                // Wrapping in key() forces AndroidView to be recreated (and bindCamera
                // re-called) whenever cameraBindKey changes — i.e. after the picker returns.
                key(cameraBindKey) {
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).also { previewView ->
                                viewModel.bindCamera(ctx, lifecycleOwner, previewView.surfaceProvider)
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            } else {
                // Permission denied — prompt
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        "Camera permission is needed to scan workouts.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 32.dp),
                    )
                    Button(
                        onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                        modifier = Modifier.padding(top = 16.dp),
                    ) { Text("Grant permission") }
                }
            }

            // AI not ready banner — floats at top of camera view
            if (!hasApiKey && !isLocalModelReady && !uiState.isCapturing && !uiState.isParsing) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.92f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "No AI backend ready. Add an OpenAI key or download Gemma in Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
            }

            // Loading overlay
            if (uiState.isCapturing || uiState.isParsing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.72f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text(
                            text = uiState.statusText ?: if (uiState.isCapturing) "Capturing…" else "Processing…",
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }

            // Bottom controls: shutter + gallery picker
            if (!uiState.isCapturing && !uiState.isParsing) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Gallery picker button
                    IconButton(
                        onClick = {
                            viewModel.unbindCamera()
                            imagePicker.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                                CircleShape,
                            ),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = "Pick from gallery",
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }

                    // Shutter button (only usable when camera permission granted)
                    if (hasCameraPermission) {
                        IconButton(
                            onClick = {
                                viewModel.captureAndParse(ContextCompat.getMainExecutor(context))
                            },
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Capture",
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                    }
                }
            }
        }
    }
}
