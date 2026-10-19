package org.example.project

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import org.example.project.ui.AppTheme
import org.example.project.ui.ChatScreen
import org.example.project.ui.PetHomeScreen
import org.example.project.ui.StatsScreen

private data class TabItem(val icon: ImageVector, val label: String)

private val tabs = listOf(
    TabItem(Icons.Filled.Pets, "宠物"),
    TabItem(Icons.AutoMirrored.Filled.Chat, "聊天"),
    TabItem(Icons.Filled.Person, "我的"),
)

/** 顶栏标题跟随宠物名动态变化（重命名后立即生效） */
private fun tabTitle(selectedTab: Int, petName: String): String = when (selectedTab) {
    0 -> petName
    1 -> "和${petName}聊天"
    else -> "数据与设置"
}

@Composable
fun App() {
    AppTheme {
        val viewModel = remember { AppServices.viewModel }
        val state by viewModel.ui.collectAsState()
        var selectedTab by rememberSaveable { mutableIntStateOf(0) }
        val snackbarHostState = remember { SnackbarHostState() }

        LaunchedEffect(state.notice) {
            state.notice?.let {
                snackbarHostState.showSnackbar(it)
                viewModel.clearNotice()
            }
        }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(title = { Text(tabTitle(selectedTab, state.pet.name)) })
            },
            bottomBar = {
                NavigationBar {
                    tabs.forEachIndexed { index, tab ->
                        NavigationBarItem(
                            selected = selectedTab == index,
                            onClick = { selectedTab = index },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (selectedTab) {
                    0 -> PetHomeScreen(
                        state = state,
                        onFeed = viewModel::feed,
                        onRefreshSteps = viewModel::refreshSteps,
                        onRefreshWeather = viewModel::refreshWeather,
                        onPetTap = viewModel::petThePet,
                        onRename = viewModel::renamePet,
                    )
                    1 -> ChatScreen(state = state, onSend = viewModel::sendChat)
                    else -> StatsScreen(
                        state = state,
                        onCheckInSleep = viewModel::checkInSleep,
                        onSaveSettings = viewModel::saveSettings,
                    )
                }
            }
        }
    }
}
