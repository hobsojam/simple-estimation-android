package com.hobsojam.simpleestimation.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/*
 * Simple Estimation — corner radii, from `tokens/radii.css`.
 *   3px tiny chips/actions -> extraSmall
 *   4px default (buttons, inputs) -> small
 *   6px cards, rows, vote tiles -> medium
 *   8px panels, board columns, form card -> large
 *   10px facilitator pill -> extraLarge
 */
val SimpleEstimationShapes = Shapes(
    extraSmall = RoundedCornerShape(3.dp),
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(6.dp),
    large = RoundedCornerShape(8.dp),
    extraLarge = RoundedCornerShape(10.dp),
)
