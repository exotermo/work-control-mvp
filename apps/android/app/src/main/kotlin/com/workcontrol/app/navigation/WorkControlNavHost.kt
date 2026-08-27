package com.workcontrol.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.toRoute
import com.workcontrol.app.core.components.BottomNavBar
import com.workcontrol.app.core.components.QuickAction
import com.workcontrol.app.core.components.QuickActionIcons
import com.workcontrol.app.core.components.QuickActionsSheet
import com.workcontrol.app.core.components.TopLevelTab
import com.workcontrol.app.core.designsystem.WcColor
import com.workcontrol.app.feature.agent.AgentDetailScreen
import com.workcontrol.app.feature.approval.ApprovalScreen
import com.workcontrol.app.feature.codediff.CodeDiffScreen
import com.workcontrol.app.feature.files.FilesScreen
import com.workcontrol.app.feature.home.HomeScreen
import com.workcontrol.app.feature.machines.MachineDetailScreen
import com.workcontrol.app.feature.machines.MachinesScreen
import com.workcontrol.app.feature.newtask.NewTaskScreen
import com.workcontrol.app.feature.result.ResultScreen
import com.workcontrol.app.feature.taskdetail.TaskDetailScreen
import com.workcontrol.app.feature.tasks.TaskListScreen
import com.workcontrol.app.feature.terminal.TerminalScreen

const val DEFAULT_MACHINE_ID = "workstation-01"
private const val DEFAULT_APPROVAL_ID = "approval-184-staging"

@Composable
fun WorkControlNavHost(navController: NavHostController, modifier: Modifier = Modifier) {
    var showQuickActions by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val activeTab = currentTopLevelTab(backStackEntry)

    Scaffold(
        modifier = modifier,
        containerColor = WcColor.Background,
        bottomBar = {
            BottomNavBar(
                activeTab = activeTab,
                onTabSelected = { tab -> navController.navigateToTab(tab) },
                onFabClick = { showQuickActions = true },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HomeRoute,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable<HomeRoute> {
                HomeScreen(
                    onAgentClick = { id -> navController.navigate(AgentDetailRoute(id)) },
                    onMachineClick = { id -> navController.navigate(MachineDetailRoute(id)) },
                    onTaskClick = { id -> navController.navigate(TaskDetailRoute(id)) },
                    onSeeAllMachines = { navController.navigate(MachineListRoute) },
                    onSeeAllTasks = { navController.navigate(TaskListRoute) },
                    onApprovalClick = { navController.navigate(ApprovalRoute(DEFAULT_APPROVAL_ID)) },
                )
            }
            composable<TaskListRoute> {
                TaskListScreen(
                    onTaskClick = { id -> navController.navigate(TaskDetailRoute(id)) },
                )
            }
            composable<TaskDetailRoute> { entry ->
                val route = entry.toRoute<TaskDetailRoute>()
                TaskDetailScreen(
                    taskId = route.taskId,
                    onBack = { navController.popBackStack() },
                    onAgentClick = { id -> navController.navigate(AgentDetailRoute(id)) },
                )
            }
            composable<AgentDetailRoute> { entry ->
                val route = entry.toRoute<AgentDetailRoute>()
                AgentDetailScreen(agentId = route.agentId, onBack = { navController.popBackStack() })
            }
            composable<CodeDiffRoute> { entry ->
                val route = entry.toRoute<CodeDiffRoute>()
                CodeDiffScreen(filePath = route.filePath, onBack = { navController.popBackStack() })
            }
            composable<TerminalRoute> { entry ->
                val route = entry.toRoute<TerminalRoute>()
                TerminalScreen(machineId = route.machineId, onBack = { navController.popBackStack() })
            }
            composable<ApprovalRoute> { entry ->
                val route = entry.toRoute<ApprovalRoute>()
                ApprovalScreen(approvalId = route.approvalId, onBack = { navController.popBackStack() })
            }
            composable<MachineListRoute> {
                MachinesScreen(onBack = null)
            }
            composable<MachineDetailRoute> { entry ->
                val route = entry.toRoute<MachineDetailRoute>()
                MachineDetailScreen(machineId = route.machineId, onBack = { navController.popBackStack() })
            }
            composable<NewTaskRoute> {
                NewTaskScreen(onBack = { navController.popBackStack() })
            }
            composable<FilesRoute> {
                FilesScreen(machineId = DEFAULT_MACHINE_ID, onBack = null)
            }
            composable<ResultRoute> { entry ->
                val route = entry.toRoute<ResultRoute>()
                ResultScreen(taskId = route.taskId, onBack = { navController.popBackStack() })
            }
        }
    }

    if (showQuickActions) {
        QuickActionsSheet(
            onDismiss = { showQuickActions = false },
            actions = listOf(
                QuickAction("Nova tarefa", QuickActionIcons.NewTask) { navController.navigate(NewTaskRoute) },
                QuickAction("Perguntar à IA", QuickActionIcons.AskAi) { },
                QuickAction("Terminal / SSH", QuickActionIcons.Terminal) {
                    navController.navigate(TerminalRoute(DEFAULT_MACHINE_ID))
                },
                QuickAction("Enviar arquivo", QuickActionIcons.UploadFile) { },
                QuickAction("Acessar máquina", QuickActionIcons.Machine) {
                    navController.navigate(MachineDetailRoute(DEFAULT_MACHINE_ID))
                },
            ),
        )
    }
}

private fun currentTopLevelTab(entry: NavBackStackEntry?): TopLevelTab? {
    val destination = entry?.destination ?: return null
    return when {
        destination.hasRoute<HomeRoute>() -> TopLevelTab.HOME
        destination.hasRoute<TaskListRoute>() -> TopLevelTab.TASKS
        destination.hasRoute<MachineListRoute>() -> TopLevelTab.MACHINES
        destination.hasRoute<FilesRoute>() -> TopLevelTab.FILES
        else -> null
    }
}

private fun NavHostController.navigateToTab(tab: TopLevelTab) {
    val route = when (tab) {
        TopLevelTab.HOME -> HomeRoute
        TopLevelTab.TASKS -> TaskListRoute
        TopLevelTab.MACHINES -> MachineListRoute
        TopLevelTab.FILES -> FilesRoute(DEFAULT_MACHINE_ID)
    }
    navigate(route) {
        popUpTo(startDestinationRoot().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun NavHostController.startDestinationRoot(): androidx.navigation.NavDestination {
    var destination: androidx.navigation.NavDestination = graph
    while (destination is NavGraph) {
        val next = destination.findNode(destination.startDestinationId) ?: return destination
        destination = next
    }
    return destination
}
