// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)
package helium314.keyboard.settings.hig

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import zone.ien.hig.*
import zone.ien.hig.theme.CupertinoTheme

/** Compatibility boundary lets existing preference actions retain their behavior with HIG rendering. */
@Suppress("UNUSED_PARAMETER")
@Composable
fun HigButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    shape: Shape = CircleShape, colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = null, border: BorderStroke? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    content: @Composable RowScope.() -> Unit) {
    CupertinoButton(onClick, modifier, enabled, shape = shape, border = border, contentPadding = contentPadding,
        colors = CupertinoButtonDefaults.filledButtonColors(
            containerColor = colors.containerColor, contentColor = colors.contentColor,
            disabledContainerColor = colors.disabledContainerColor, disabledContentColor = colors.disabledContentColor), content = content)
}

@Composable
fun HigTextButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    content: @Composable RowScope.() -> Unit) {
    CupertinoButton(onClick, modifier, enabled, colors = CupertinoButtonDefaults.plainButtonColors(contentColor = colors.contentColor),
        contentPadding = contentPadding, content = content)
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun HigRadioButton(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier,
    enabled: Boolean = true, colors: RadioButtonColors = RadioButtonDefaults.colors()) {
    Box(modifier.size(40.dp).then(if (onClick != null) Modifier.selectable(selected = selected, enabled = enabled, role = Role.RadioButton, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center) {
        if (selected) CupertinoIcon(SettingsIcons.Check, null, tint = CupertinoTheme.colorScheme.accent, modifier = Modifier.size(20.dp))
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun HigCard(modifier: Modifier = Modifier, shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
    colors: CardColors = CardDefaults.cardColors(), elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null, content: @Composable ColumnScope.() -> Unit) {
    CupertinoSurface(modifier, shape = shape, color = CupertinoTheme.colorScheme.secondarySystemGroupedBackground) {
        Column(content = content)
    }
}
