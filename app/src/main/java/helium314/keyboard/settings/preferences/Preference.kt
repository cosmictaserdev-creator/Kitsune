// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import zone.ien.hig.CupertinoIcon as Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import helium314.keyboard.settings.hig.HigSurface as Surface
import zone.ien.hig.CupertinoText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.R
import helium314.keyboard.settings.IconOrImage
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.latin.utils.previewDark
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.foundation.shape.RoundedCornerShape

// partially taken from StreetComplete / SCEE

/** True while rendering inside a [PreferenceGroup]: rows are flat and separated by hairlines instead of being blocks. */
val LocalPreferenceGrouped = compositionLocalOf { false }

@Composable
fun PreferenceCategory(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column {
        Text(
            text = title.uppercase(),
            modifier = modifier.padding(top = 18.dp, start = 36.dp, end = 28.dp, bottom = 6.dp),
            color = zone.ien.hig.theme.CupertinoTheme.colorScheme.secondaryLabel,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/** iOS-style inset grouped card: one rounded surface holding all rows of a section, hairlines between them. */
@Composable
fun PreferenceGroup(content: @Composable ColumnScope.() -> Unit) {
    val surface = zone.ien.hig.theme.CupertinoTheme.colorScheme.secondarySystemGroupedBackground
    CompositionLocalProvider(LocalPreferenceGrouped provides true) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 4.dp)
                .clip(RoundedCornerShape(20.dp)).background(surface)
                .drawWithContent {
                    drawContent()
                    // every row draws a bottom hairline; hide the last one so separators sit only between rows
                    drawRect(surface, Offset(0f, size.height - 1.dp.toPx()), Size(size.width, 1.dp.toPx()))
                },
            content = content
        )
    }
}

@Composable
fun Preference(
    name: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    @DrawableRes icon: Int? = null,
    value: @Composable (RowScope.() -> Unit)? = null,
) {
    val grouped = LocalPreferenceGrouped.current
    val separator = zone.ien.hig.theme.CupertinoTheme.colorScheme.separator
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (grouped) Modifier.drawBehind {
                val y = size.height - 0.5.dp.toPx()
                drawLine(separator, Offset(16.dp.toPx(), y), Offset(size.width, y), 0.5.dp.toPx())
            } else Modifier
                .padding(horizontal = 20.dp, vertical = 1.dp)
                .background(zone.ien.hig.theme.CupertinoTheme.colorScheme.secondarySystemGroupedBackground, RoundedCornerShape(12.dp)))
            .clickable { onClick() }
            .heightIn(min = 48.dp)
            .padding(vertical = 10.dp, horizontal = if (grouped) 16.dp else 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null)
            IconOrImage(icon, name, 32)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                CompositionLocalProvider(
                    LocalTextStyle provides MaterialTheme.typography.bodyMedium,
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
                ) {
                    Text(
                        text = description,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
        if (value != null) {
            CompositionLocalProvider(
                LocalTextStyle provides LocalTextStyle.current.copy(
                    textAlign = TextAlign.End,
                    hyphens = Hyphens.Auto
                ),
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(
                        space = 8.dp,
                        alignment = Alignment.End
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) { value() }
            }
        }
    }
}

@Preview
@Composable
private fun PreferencePreview() {
    Theme(previewDark) {
        Surface {
            Column {
                PreferenceCategory("Preference Category")
                Preference(
                    name = "Preference",
                    onClick = {},
                )
                Preference(
                    name = "Preference with icon",
                    onClick = {},
                    icon = R.drawable.ic_settings_about
                )
                SliderPreference(
                    name = "SliderPreference",
                    key = "",
                    default = 1,
                    description = { it.toString() },
                    range = -5f..5f
                )
                Preference(
                    name = "Preference with icon and description",
                    description = "some text",
                    onClick = {},
                    icon = R.drawable.ic_settings_about
                )
                Preference(
                    name = "Preference with switch",
                    onClick = {}
                ) {
                    SettingsSwitch(checked = true, onCheckedChange = {})
                }
                SwitchPreference(
                    name = "SwitchPreference",
                    key = "none",
                    default = true
                )
                Preference(
                    name = "Preference",
                    onClick = {},
                    description = "A long description which may actually be several lines long, so it should wrap."
                ) {
                    Icon(painterResource(R.drawable.ic_arrow_left), null)
                }
                Preference(
                    name = "Long preference name that wraps",
                    onClick = {},
                ) {
                    Text("Long preference value")
                }
                Preference(
                    name = "Long preference name 2",
                    onClick = {},
                    description = "hello I am description"
                ) {
                    Text("Long preference value")
                }
            }
        }
    }
}
