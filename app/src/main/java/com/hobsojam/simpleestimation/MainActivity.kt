package com.hobsojam.simpleestimation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.hobsojam.simpleestimation.feature.planningpoker.PlanningPokerParticipantRoute

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SimpleEstimationApp()
        }
    }
}

@Composable
fun SimpleEstimationApp() {
    MaterialTheme {
        PlanningPokerParticipantRoute()
    }
}

@Preview(showBackground = true)
@Composable
private fun SimpleEstimationAppPreview() {
    SimpleEstimationApp()
}
