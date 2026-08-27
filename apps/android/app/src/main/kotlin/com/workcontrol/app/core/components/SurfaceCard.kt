package com.workcontrol.app.core.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.WcColor

/**
 * Card base reutilizado por quase toda tela — equivalente ao `bg-[#0f0f18] border rounded-2xl`
 * repetido no protótipo. `accentBorder` cobre as variações de borda tingida (violeta/âmbar).
 */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    accentBorder: Color = WcColor.Border,
    backgroundColor: Color = WcColor.Surface,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(14.dp)
    var cardModifier = modifier
        .clip(shape)
        .background(backgroundColor)
        .border(BorderStroke(1.dp, accentBorder), shape)
    if (onClick != null) {
        cardModifier = cardModifier.clickable(onClick = onClick)
    }

    Column(
        modifier = cardModifier.padding(contentPadding),
        content = { content() },
    )
}
