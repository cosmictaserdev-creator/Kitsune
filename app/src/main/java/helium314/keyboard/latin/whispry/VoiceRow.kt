// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)
package helium314.keyboard.latin.whispry

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.*
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.Theme
import zone.ien.hig.*

@Composable
fun VoiceRow(controller: WhispryKeyboardController) {
    val state by controller.state.collectAsState()
    val colors = Settings.getValues().mColors
    val background = Color(colors.get(ColorType.STRIP_BACKGROUND))
    val foreground = Color(colors.get(ColorType.KEY_TEXT))
    val accent = Color(colors.get(ColorType.SPECIAL_KEY_BACKGROUND))
    val level by animateFloatAsState(if (state.phase == "LISTENING") state.level else 0f,
        spring(stiffness = 500f), label = "Whispry mic amplitude")
    Theme {
        Box(Modifier.fillMaxSize().background(background)) {
            if (state.phase == "LISTENING" || state.phase in listOf("TRANSCRIBING", "FORMATTING")) {
                Canvas(Modifier.fillMaxSize()) {
                    val alpha = if (state.phase == "LISTENING") .12f + level * .45f else .15f
                    drawRoundRect(Brush.linearGradient(listOf(accent.copy(alpha = alpha), Color(0xFF8B73FF).copy(alpha = alpha),
                        Color(0xFF5AC8FA).copy(alpha = alpha)), Offset.Zero, Offset(size.width, size.height)),
                        cornerRadius = CornerRadius(12.dp.toPx()))
                    drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF8B73FF), Color(0xFF5AC8FA), Color(0xFFF5A9CF))),
                        topLeft = Offset(0f, size.height - (2.dp.toPx() + level * 5.dp.toPx())),
                        size = androidx.compose.ui.geometry.Size(size.width, 2.dp.toPx() + level * 5.dp.toPx()),
                        cornerRadius = CornerRadius(4.dp.toPx()))
                }
            }
            Row(Modifier.fillMaxSize().padding(horizontal = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                VoiceAction(SfSymbols.Monochrome.SFXmark, "Cancel dictation", foreground, controller::cancel)
                Column(Modifier.weight(1f).clickable(enabled = state.phase == "ERROR") { controller.openSettings() },
                    verticalArrangement = Arrangement.Center) {
                    CupertinoText(when (state.phase) {
                        "LISTENING" -> "Listening · ${state.elapsed / 1000}s"
                        "PAUSED" -> "Paused · ${state.elapsed / 1000}s"
                        else -> state.message
                    }, fontSize = 12.sp, maxLines = 2, color = foreground)
                }
                if (state.listening) {
                    VoiceAction(if (state.phase == "PAUSED") SfSymbols.Monochrome.SFPlayFill else SfSymbols.Monochrome.SFPauseFill,
                        if (state.phase == "PAUSED") "Resume dictation" else "Pause dictation", foreground, controller::pause)
                    VoiceAction(SfSymbols.Monochrome.SFCheckmark, "Finish and insert", foreground, controller::finish)
                } else if (state.phase == "READY") {
                    CupertinoButton(onClick = controller::finish, colors = CupertinoButtonDefaults.plainButtonColors(contentColor = foreground)) { CupertinoText("Insert", fontSize = 12.sp) }
                } else if (state.phase == "ERROR") {
                    CupertinoButton(onClick = if (state.retry) controller::retry else controller::openSettings,
                        colors = CupertinoButtonDefaults.plainButtonColors(contentColor = foreground)) {
                        CupertinoText(if (state.retry) "Retry" else "Open", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun VoiceAction(icon: ImageVector, label: String, color: Color, action: () -> Unit) {
    CupertinoIconButton(onClick = action, modifier = Modifier.width(42.dp).fillMaxHeight()) {
        CupertinoIcon(icon, label, tint = color, modifier = Modifier.size(18.dp))
    }
}
