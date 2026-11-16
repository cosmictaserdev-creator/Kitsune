// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)
package helium314.keyboard.latin.whispry

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.*
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.Theme
import zone.ien.hig.*

private val Languages = listOf("English", "Spanish", "French", "German", "Hindi", "Portuguese", "Italian",
    "Japanese", "Korean", "Chinese", "Arabic", "Russian", "Turkish", "Dutch")

private data class Feature(val label: String, val host: String)
private val Features = listOf(
    Feature("Meetings", "meetings"), Feature("Dictionary", "dictionary"), Feature("Snippets", "text-expander"),
    Feature("Memory", "memory"), Feature("My Info", "my-info"), Feature("App Tones", "app-tones"),
)

/** Double-tap-mic panel: feature shortcuts on top, tone tiles in two scrollable rows underneath. */
@Composable
fun ToolsPanel(controller: WhispryKeyboardController) {
    val open by controller.panelOpen.collectAsState()
    val tools by controller.tools.collectAsState()
    val colors = Settings.getValues().mColors
    val surface = Color(colors.get(ColorType.MAIN_BACKGROUND))
    val tile = Color(colors.get(ColorType.KEY_BACKGROUND))
    val text = Color(colors.get(ColorType.KEY_TEXT))
    val accent = Color(colors.get(ColorType.SPECIAL_KEY_BACKGROUND))
    var showLanguages by remember { mutableStateOf(false) }
    Theme {
        AnimatedVisibility(open, enter = fadeIn(spring()) + slideInVertically(spring(dampingRatio = 0.85f, stiffness = 400f)) { it / 6 },
            exit = fadeOut() + slideOutVertically { it / 6 }) {
            Column(Modifier.fillMaxSize().background(surface)
                // Swallow every touch so the dimmed keys underneath cannot be typed.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(top = 8.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LazyRow(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item("translate") {
                            val on = tools?.translate == true
                            Chip("Translate" + if (on) " · ${tools?.translateLanguage}" else "", on, tile, accent, text) {
                                if (on) controller.setTranslation(false, tools?.translateLanguage.orEmpty()) else showLanguages = !showLanguages
                            }
                        }
                        items(Features, key = { it.host }) { f -> Chip(f.label, false, tile, accent, text) { controller.openFeature(f.host) } }
                    }
                    Box(Modifier.size(48.dp).clickable { controller.closePanel() }
                        .semantics { contentDescription = "Close tools" }, contentAlignment = Alignment.Center) {
                        CupertinoIcon(SfSymbols.Monochrome.SFXmark, null, tint = text, modifier = Modifier.size(18.dp))
                    }
                }
                AnimatedVisibility(showLanguages) {
                    LazyRow(contentPadding = PaddingValues(horizontal = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(Languages, key = { it }) { lang ->
                            Chip(lang, tools?.translateLanguage == lang && tools?.translate == true, tile, accent, text) {
                                controller.setTranslation(true, lang); showLanguages = false
                            }
                        }
                    }
                }
                val t = tools
                if (t == null) {
                    Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                        CupertinoText(controller.toolsMessage.collectAsState().value, fontSize = 13.sp, color = text.copy(alpha = 0.8f))
                    }
                } else LazyHorizontalGrid(GridCells.Fixed(2), Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(horizontal = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(t.ids.indices.toList(), key = { t.ids[it] }) { i ->
                        val selected = t.selected == t.ids[i] && !t.translate
                        Column(Modifier.aspectRatio(1.05f).clip(RoundedCornerShape(22.dp)).background(tile)
                            .then(if (selected) Modifier.background(accent.copy(alpha = 0.35f)) else Modifier)
                            .clickable { controller.selectTone(t.ids[i]) }
                            .semantics { contentDescription = t.names[i] + if (selected) ", selected" else "" },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            CupertinoText(t.emojis[i], fontSize = 24.sp)
                            Spacer(Modifier.height(4.dp))
                            CupertinoText(t.names[i], fontSize = 11.sp, color = text, maxLines = 1,
                                modifier = Modifier.padding(horizontal = 4.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Chip(label: String, selected: Boolean, tile: Color, accent: Color, text: Color, onClick: () -> Unit) {
    Box(Modifier.height(36.dp).clip(RoundedCornerShape(18.dp))
        .background(if (selected) accent else tile).clickable(onClick = onClick).padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center) {
        CupertinoText(label, fontSize = 13.sp, color = text, maxLines = 1)
    }
}
