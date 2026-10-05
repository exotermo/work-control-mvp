package com.workcontrol.app.core.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.Prelo
import com.workcontrol.app.core.designsystem.PreloType

private val ButtonShape = RoundedCornerShape(10.dp)

/**
 * "button.primary": signal orange, 2dp ink border and a hard 4dp ink shadow. Pressing pushes it
 * into the paper (the shadow collapses), like a letterpress key.
 */
@Composable
fun InkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    leading: (@Composable RowScope.() -> Unit)? = null) {
    val palette = Prelo.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val lift: Dp = if (pressed || !enabled) 0.dp else 4.dp
    Row(
        modifier
            .alpha(if (enabled) 1f else 0.5f)
            .offset(x = 4.dp - lift, y = 4.dp - lift)
            .drawBehind {
                if (lift > 0.dp) drawRoundRect(palette.rule, Offset(lift.toPx(), lift.toPx()), size,
                    CornerRadius(10.dp.toPx()))
            }
            .background(palette.accent, ButtonShape)
            .border(2.dp, palette.rule, ButtonShape)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading?.invoke(this)
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = palette.onAccent)
    }
}

/** Plain paper button: 2dp ink border, no fill. */
@Composable
fun GhostButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    danger: Boolean = false) {
    val palette = Prelo.colors
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val ink = if (danger) palette.bad else palette.ink
    Box(
        modifier
            .alpha(if (enabled) 1f else 0.5f)
            .background(if (pressed) palette.accentWash else palette.card.copy(alpha = 0f), ButtonShape)
            .border(2.dp, ink, ButtonShape)
            .clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 48.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = ink)
    }
}

/** ".back-link": bold rust text link ("← Voltar", "Ver tudo"). */
@Composable
fun InkLink(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 44.dp)
            .padding(vertical = 12.dp, horizontal = 2.dp),
        style = androidx.compose.material3.MaterialTheme.typography.labelLarge, color = Prelo.colors.accentInk,
    )
}

/** Text field on fresh paper with an ink border; the label stays inside (tests look it up by label). */
@Composable
fun InkField(value: String, onValueChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
    singleLine: Boolean = true, visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, mono: Boolean = false, minLines: Int = 1) {
    val palette = Prelo.colors
    OutlinedTextField(
        value, onValueChange, modifier, singleLine = singleLine, minLines = minLines,
        label = { Text(label) },
        textStyle = if (mono) PreloType.Teletype.copy(color = palette.ink) else
            androidx.compose.material3.MaterialTheme.typography.bodyLarge.copy(color = palette.ink),
        visualTransformation = visualTransformation, keyboardOptions = keyboardOptions,
        shape = ButtonShape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = palette.card, unfocusedContainerColor = palette.card,
            focusedBorderColor = palette.accent, unfocusedBorderColor = palette.rule,
            focusedLabelColor = palette.accentInk, unfocusedLabelColor = palette.text,
            cursorColor = palette.accent, focusedTextColor = palette.ink, unfocusedTextColor = palette.ink,
        ),
    )
}

/** Filter/selection chip (".chip"): pill, ink fill when active. */
@Composable
fun InkChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = Prelo.colors
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .background(if (selected) palette.ink else palette.card.copy(alpha = 0f), shape)
            .border(2.dp, palette.ink, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .defaultMinSize(minHeight = 40.dp)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            color = if (selected) palette.desk else palette.ink)
    }
}
