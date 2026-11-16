// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.hig

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import zone.ien.hig.CupertinoSurface
import zone.ien.hig.theme.CupertinoTheme

@Suppress("UNUSED_PARAMETER")
@Composable
fun HigSurface(modifier: Modifier = Modifier, shape: Shape = RectangleShape,
    color: Color = CupertinoTheme.colorScheme.systemGroupedBackground,
    contentColor: Color = CupertinoTheme.colorScheme.label,
    tonalElevation: Dp = 0.dp, shadowElevation: Dp = 0.dp, border: BorderStroke? = null,
    content: @Composable () -> Unit) {
    CupertinoSurface(if (border != null) modifier.border(border, shape) else modifier,
        shape, color, shadowElevation, contentColor, content)
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun HigSurface(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = RectangleShape, color: Color = CupertinoTheme.colorScheme.secondarySystemGroupedBackground,
    contentColor: Color = CupertinoTheme.colorScheme.label,
    tonalElevation: Dp = 0.dp, shadowElevation: Dp = 0.dp, border: BorderStroke? = null,
    content: @Composable () -> Unit) {
    CupertinoSurface(onClick = onClick, modifier = modifier, enabled = enabled, shape = shape,
        color = color, contentColor = contentColor, border = border, content = content)
}
