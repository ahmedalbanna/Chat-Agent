package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.AppDatabase
import com.example.data.repository.AgentRepository
import com.example.viewmodel.AgentViewModel
import com.example.viewmodel.ChatViewModel
import com.example.viewmodel.ChatViewModelFactory
import com.example.viewmodel.HistoryViewModel
import com.example.viewmodel.ViewModelFactory

@Composable
fun AppNavigation() {
    val context = LocalContext.current
    val database = remember { AppDatabase.getDatabase(context) }
    val repository = remember { AgentRepository(database.agentDao(), database.instructionDao(), database.messageDao()) }
    val agentViewModel: AgentViewModel = viewModel(factory = ViewModelFactory(repository))

    val navController = rememberNavController()

    Box(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = "agent_list") {
            composable("agent_list") {
                AgentListScreen(
                    viewModel = agentViewModel,
                    onAgentClick = { agentId ->
                        navController.navigate("chat/$agentId")
                    },
                    onSettingsClick = { agentId ->
                        navController.navigate("agent_editor/$agentId")
                    },
                    onHistoryClick = {
                        navController.navigate("history")
                    }
                )
            }
            composable("history") {
                val historyViewModel: HistoryViewModel = viewModel(factory = ViewModelFactory(repository))
                HistoryScreen(
                    viewModel = historyViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                "agent_editor/{agentId}",
                arguments = listOf(navArgument("agentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val agentId = backStackEntry.arguments?.getLong("agentId") ?: 0L
                AgentEditorScreen(
                    viewModel = agentViewModel,
                    agentId = agentId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                "chat/{agentId}",
                arguments = listOf(navArgument("agentId") { type = NavType.LongType })
            ) { backStackEntry ->
                val agentId = backStackEntry.arguments?.getLong("agentId") ?: 0L
                
                // We need a default instruction if none is selected yet
                // For simplicity, we assume the ViewModel handles finding the first instruction
                // Or we pass a placeholder and let it decide.
                
                val chatViewModel: ChatViewModel = viewModel(
                    key = "chat_$agentId",
                    factory = ChatViewModelFactory(repository, agentId, 0L) // 0L will trigger finding first instruction in real implementation or we adapt ChatViewModel
                )

                ChatScreen(
                    chatViewModel = chatViewModel,
                    agentViewModel = agentViewModel,
                    agentId = agentId,
                    onBack = { navController.popBackStack() }
                )
            }
        }

        // Overlay for active live notifications floating at the top
        InAppNotificationBannerOverlay(modifier = Modifier.align(Alignment.TopCenter))
    }
}
