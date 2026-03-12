package com.gymapp.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class WeightMode {
    /** Standard weighted exercise — weight field is the load in kg. */
    BARBELL,

    /** Pure bodyweight — weight is pulled from the user's configured body weight. */
    BODYWEIGHT,

    /** Bodyweight with band assistance/resistance — weight field is the band label/value. */
    BANDED,
}
