package com.gymapp.core.model

/**
 * All-time personal record for a given rep count and exercise name.
 *
 * Returned by [com.gymapp.core.database.dao.SetEntryDao.observePrsByRepCount].
 *
 * @param reps       Number of reps performed (1, 2, 3, 5, …)
 * @param weight     Best (heaviest) weight logged for that rep count, in kg
 */
data class ExercisePr(
    val reps: Int,
    val weight: Float,
)
