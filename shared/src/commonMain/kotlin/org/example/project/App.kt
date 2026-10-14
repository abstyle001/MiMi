package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.example.project.ui.ChatScreen
import org.example.project.ui.PetHomeScreen
import org.example.project.ui.StatsScreen

private data class TabItem(val emoji: String, val label: String)

private val tabs = listOf(
    TabItem("🐱", "宠物"),
    TabItem("💬", "聊天"),
    TabItem("📊", "我的"),
)

@Composable
fun App() {
    MaterialTheme {
        val viewModel = remember { AppServices.viewModel }
        val state by viewModel.ui.collectAsState()
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }

        LaunchedEffect(state.notice) {
            if (state.notice != null) {
                delay(3500)
                viewModel.clearNotice()
            }
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Text(tab.emoji) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                    0 -> PetHomeScreen(
                        state = state,
                        onFeed = viewModel::feed,
                        onRefreshSteps = viewModel::refreshSteps,
                        onRefreshWeather = viewModel::refreshWeather,
                        onPetTap = viewModel::petThePet,
                    )
                    1 -> ChatScreen(state = state, onSend = viewModel::sendChat)
                    else -> StatsScreen(
                        state = state,
                        onCheckInSleep = viewModel::checkInSleep,
                        onSaveSettings = viewModel::saveSettings,
                    )
                }

                state.notice?.let { notice ->
                    Card(Modifier.align(Alignment.TopCenter).padding(16.dp)) {
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }
        }
    }
}
