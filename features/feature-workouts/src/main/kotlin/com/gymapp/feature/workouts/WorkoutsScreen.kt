package com.gymapp.feature.workouts

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Workouts feature entry-point screen.
 * Scaffold — full implementation is a future milestone.
 */
@Composable
fun WorkoutsScreen(modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.fillMaxSize(),
    ) {
        Text(
            text = "Workouts — coming soon",
            style = MaterialTheme.typography.titleMedium,
        )
    }
}
