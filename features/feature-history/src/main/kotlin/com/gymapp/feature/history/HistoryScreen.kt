package com.gymapp.feature.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * History feature entry-point screen.
 * Scaffold — full implementation is a future milestone.
 */
@Composable
fun HistoryScreen(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            text = "History — coming soon",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
