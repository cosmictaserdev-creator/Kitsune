// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import zone.ien.hig.CupertinoSwitch
@Composable
fun SettingsSwitch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    CupertinoSwitch(checked, { onCheckedChange?.invoke(it) }, modifier, enabled = enabled && onCheckedChange != null)
}
