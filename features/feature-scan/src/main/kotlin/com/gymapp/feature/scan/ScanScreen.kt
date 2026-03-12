package com.gymapp.feature.scan

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Scan feature entry-point screen (camera capture + AI parsing).
 * Scaffold — full implementation is a future milestone.
 */
@Composable
fun ScanScreen(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            text = "Scan — coming soon",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
