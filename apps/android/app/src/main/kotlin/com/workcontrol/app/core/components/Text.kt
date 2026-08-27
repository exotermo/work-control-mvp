package com.workcontrol.app.core.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.workcontrol.app.core.designsystem.WcColor
import com.workcontrol.app.core.designsystem.WcType

/** Rótulo pequeno, mono, uppercase, tracked — o "eyebrow" usado como cabeçalho de toda seção. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(bottom = 10.dp),
) {
    Text(
        text = text.uppercase(),
        style = WcType.Eyebrow,
        color = WcColor.InkFaint,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.padding(padding),
    )
}
