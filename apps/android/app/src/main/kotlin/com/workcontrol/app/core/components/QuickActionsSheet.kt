package com.workcontrol.app.core.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Monitor
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.outlined.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.WcColor

data class QuickAction(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** Bottom sheet de ações rápidas aberto pelo FAB central — não é um destino de navegação. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun QuickActionsSheet(onDismiss: () -> Unit, actions: List<QuickAction>) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = WcColor.SurfaceRaised) {
        Column(modifier = Modifier.padding(bottom = 24.dp)) {
            Text(
                text = "Ações rápidas",
                color = WcColor.InkFaint,
                modifier = Modifier.padding(start = 20.dp, bottom = 8.dp),
            )
            actions.forEach { action ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { action.onClick(); onDismiss() }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Icon(imageVector = action.icon, contentDescription = null, tint = WcColor.Accent)
                    Text(text = action.label, color = WcColor.Ink, modifier = Modifier.padding(start = 16.dp))
                }
            }
        }
    }
}

object QuickActionIcons {
    val NewTask: ImageVector = Icons.Outlined.Bolt
    val AskAi: ImageVector = Icons.Outlined.SmartToy
    val Terminal: ImageVector = Icons.Outlined.Terminal
    val UploadFile: ImageVector = Icons.Outlined.CloudUpload
    val Machine: ImageVector = Icons.Outlined.Monitor
    val Send: ImageVector = Icons.AutoMirrored.Outlined.Send
}
