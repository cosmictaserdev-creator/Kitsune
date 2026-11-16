// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)
package helium314.keyboard.settings.hig

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.*
import zone.ien.hig.CupertinoIcon
import zone.ien.hig.CupertinoSurface
import zone.ien.hig.CupertinoText
import zone.ien.hig.theme.CupertinoTheme

/** Role-based icon boundary: only referenced vectors are retained in release builds. */
object SettingsIcons {
    val Keyboard get() = SfSymbols.Monochrome.SFKeyboard
    val Themes get() = SfSymbols.Monochrome.SFPaintpaletteFill
    val Voice get() = SfSymbols.Monochrome.SFMicrophoneFill
    val Settings get() = SfSymbols.Monochrome.SFGearshape
    val Language get() = SfSymbols.Monochrome.SFGlobe
    val Typing get() = SfSymbols.Monochrome.SFTextAlignleft
    val Layout get() = SfSymbols.Monochrome.SFSquareGrid2x2
    val Gesture get() = SfSymbols.Monochrome.SFHandTapFill
    val Dictionary get() = SfSymbols.Monochrome.SFBook
    val Search get() = SfSymbols.Monochrome.SFMagnifyingglass
    val Back get() = SfSymbols.Monochrome.SFChevronLeft
    val Next get() = SfSymbols.Monochrome.SFChevronRight
    val Close get() = SfSymbols.Monochrome.SFXmark
    val Check get() = SfSymbols.Monochrome.SFCheckmark
    val More get() = SfSymbols.Monochrome.SFEllipsis
    val Privacy get() = SfSymbols.Monochrome.SFShieldFill
    val About get() = SfSymbols.Monochrome.SFInfoCircleFill
    val Cloud get() = SfSymbols.Monochrome.SFCloud
    val Sparkles get() = SfSymbols.Monochrome.SFSparkles
}

@Composable
fun SettingsSection(title: String, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp)) {
        CupertinoText(title.uppercase(), color = CupertinoTheme.colorScheme.secondaryLabel,
            style = CupertinoTheme.typography.footnote, modifier = Modifier.padding(start = 16.dp, bottom = 8.dp))
        CupertinoSurface(shape = RoundedCornerShape(20.dp), color = CupertinoTheme.colorScheme.secondarySystemGroupedBackground) {
            Column(Modifier.fillMaxWidth(), content = content)
        }
        if (footer != null) CupertinoText(footer, color = CupertinoTheme.colorScheme.secondaryLabel,
            style = CupertinoTheme.typography.footnote, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
    }
}

@Composable
fun SettingsRow(title: String, subtitle: String? = null, icon: ImageVector? = null, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).heightIn(min = 60.dp).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        if (icon != null) Box(Modifier.size(36.dp).background(CupertinoTheme.colorScheme.accent.copy(alpha = .12f), RoundedCornerShape(11.dp)),
            contentAlignment = Alignment.Center) {
            CupertinoIcon(icon, null, tint = CupertinoTheme.colorScheme.accent, modifier = Modifier.size(21.dp))
        }
        Column(Modifier.weight(1f)) {
            CupertinoText(title, style = CupertinoTheme.typography.body)
            if (!subtitle.isNullOrBlank()) CupertinoText(subtitle, color = CupertinoTheme.colorScheme.secondaryLabel,
                style = CupertinoTheme.typography.footnote)
        }
        CupertinoIcon(SettingsIcons.Next, null, tint = CupertinoTheme.colorScheme.tertiaryLabel, modifier = Modifier.size(12.dp))
    }
}
