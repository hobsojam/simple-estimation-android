package com.hobsojam.simpleestimation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.hobsojam.simpleestimation.data.room.HttpActiveRoomRepository
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryScreen
import com.hobsojam.simpleestimation.feature.roomdiscovery.RoomDiscoveryStateHolder

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
        Surface(color = MaterialTheme.colorScheme.background) {
            val stateHolder = remember {
                RoomDiscoveryStateHolder(repository = HttpActiveRoomRepository())
            }
            RoomDiscoveryScreen(
                stateHolder = stateHolder,
                modifier = Modifier,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SimpleEstimationAppPreview() {
    SimpleEstimationApp()
}
