package com.workcontrol.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.WcColor
import com.workcontrol.app.core.designsystem.WcType

enum class TopLevelTab(val label: String, val icon: ImageVector) {
    HOME("Início", Icons.Outlined.Home),
    TASKS("Tarefas", Icons.Outlined.Checklist),
    MACHINES("Máquinas", Icons.Outlined.Dns),
    FILES("Arquivos", Icons.Outlined.Folder),
}

/** Nav inferior de 5 slots (4 destinos + FAB central) — igual ao protótipo. */
@Composable
fun BottomNavBar(
    activeTab: TopLevelTab?,
    onTabSelected: (TopLevelTab) -> Unit,
    onFabClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .background(WcColor.Background)
            .navigationBarsPadding()
            .padding(horizontal = 8.dp, vertical = 6.dp),
    ) {
        NavSlot(TopLevelTab.HOME, activeTab, onTabSelected, Modifier.weight(1f))
        NavSlot(TopLevelTab.TASKS, activeTab, onTabSelected, Modifier.weight(1f))
        FabSlot(onFabClick, Modifier.weight(1f))
        NavSlot(TopLevelTab.MACHINES, activeTab, onTabSelected, Modifier.weight(1f))
        NavSlot(TopLevelTab.FILES, activeTab, onTabSelected, Modifier.weight(1f))
    }
}

@Composable
private fun NavSlot(
    tab: TopLevelTab,
    activeTab: TopLevelTab?,
    onTabSelected: (TopLevelTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isActive = tab == activeTab
    val tint = if (isActive) WcColor.Accent else WcColor.InkFaint
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clickable { onTabSelected(tab) }
            .padding(top = 6.dp, bottom = 2.dp),
    ) {
        Icon(imageVector = tab.icon, contentDescription = tab.label, tint = tint, modifier = Modifier.size(20.dp))
        Text(text = tab.label, style = WcType.Eyebrow, color = tint, maxLines = 1)
    }
}

@Composable
private fun FabSlot(onFabClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(48.dp)
                .background(WcColor.Primary, CircleShape)
                .clickable(onClick = onFabClick),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Ações rápidas", tint = WcColor.Ink)
        }
    }
}
