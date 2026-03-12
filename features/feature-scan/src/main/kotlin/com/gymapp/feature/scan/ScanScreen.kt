package com.gymapp.feature.scan

import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    onWorkoutReady: (sessionId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show error in snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.dismissError()
        }
    }

    // Once review items are ready navigate to review screen — done inline here
    // by showing ScanReviewScreen as an overlay when reviewItems != null
    if (uiState.reviewItems != null) {
        ScanReviewScreen(
            items = uiState.reviewItems!!,
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
        topBar = { TopAppBar(title = { Text("Scan Workout") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // CameraX preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also { previewView ->
                        viewModel.bindCamera(ctx, lifecycleOwner, previewView.surfaceProvider)
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            // Loading overlay
            if (uiState.isCapturing || uiState.isParsing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = Color.White)
                    Text(
                        text = if (uiState.isParsing) "Analysing…" else "Capturing…",
                        color = Color.White,
                        modifier = Modifier.padding(top = 64.dp),
                    )
                }
            }

            // Shutter button
            if (!uiState.isCapturing && !uiState.isParsing) {
                IconButton(
                    onClick = {
                        viewModel.captureAndParse(ContextCompat.getMainExecutor(context))
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                        .size(72.dp)
                        .background(Color.White, CircleShape),
                ) {
                    Icon(
                        imageVector = Icons.Default.Camera,
                        contentDescription = "Capture",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}
