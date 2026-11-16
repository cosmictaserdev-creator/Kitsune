// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)
package helium314.keyboard.latin.whispry

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import kotlin.math.PI
import kotlin.math.sin

/** Colours of the glow; they drift around the edge over time. Inspired by soft aurora lighting. */
private val GlowPalette = listOf(
    Color(0xFF7B2FF7), Color(0xFFC13CFF), Color(0xFFFF4D8D), Color(0xFFFF9F43),
    Color(0xFFFF4D8D), Color(0xFF5B5BFF), Color(0xFF3AA0FF), Color(0xFF7B2FF7),
)

/** Shift the palette by [phase] (0..size) so the gradient flows smoothly without a seam. */
private fun flowingColors(phase: Float): List<Color> {
    val n = GlowPalette.size - 1 // last entry repeats the first so the sweep closes
    val base = phase.toInt()
    val frac = phase - base
    val shifted = List(n) { i -> lerp(GlowPalette[(i + base) % n], GlowPalette[(i + base + 1) % n], frac) }
    return shifted + shifted.first()
}

@Composable
fun VoiceRow(controller: WhispryKeyboardController) {
    val state by controller.state.collectAsState()
    val colors = Settings.getValues().mColors
    val foreground = Color(colors.get(ColorType.KEY_TEXT))
    val context = LocalContext.current
    val reduceMotion = remember { android.provider.Settings.Global.getFloat(
        context.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val paused = state.phase == "PAUSED"
    val processing = state.phase == "TRANSCRIBING" || state.phase == "FORMATTING"
    // Fast attack, slower release, so the glow follows speech without flicker.
    val rawLevel = if (state.phase == "LISTENING") state.level.coerceIn(0f, 1f) else 0f
    val level by animateFloatAsState(rawLevel,
        if (rawLevel > 0f) spring(dampingRatio = 0.8f, stiffness = 700f) else tween(450, easing = FastOutSlowInEasing),
        label = "Kitsune mic amplitude")
    val dim by animateFloatAsState(if (paused) 0.35f else 1f, tween(300), label = "glow dim")
    val transition = rememberInfiniteTransition(label = "glow")
    val drift by transition.animateFloat(0f, (GlowPalette.size - 1).toFloat(),
        infiniteRepeatable(tween(if (processing) 4000 else 14000, easing = LinearEasing)), label = "drift")
    val breath by transition.animateFloat(0f, (2 * PI).toFloat(),
        infiniteRepeatable(tween(if (processing) 1400 else 4200, easing = LinearEasing)), label = "breath")
    val shape = RoundedCornerShape(18.dp)

    Theme {
        Box(Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 3.dp).clip(shape)
            .background(Color(0xFF050507))
            .semantics { contentDescription = "Dictation" }) {
            val animated = state.visible && !paused && !reduceMotion
            val ambient = if (animated) (sin(breath) * 0.5f + 0.5f) * 0.25f else 0.1f
            val energy = ((if (processing) 0.35f else level) + ambient).coerceIn(0f, 1f) * dim
            Canvas(Modifier.fillMaxSize()) {
                val brush = Brush.sweepGradient(flowingColors(if (animated) drift else 0f), center)
                val corner = CornerRadius(18.dp.toPx())
                // Layered strokes fade inwards so the colour hugs the edge and the centre stays dark.
                for (i in 0..6) {
                    val width = (2.5f + i * 5f).dp.toPx() * (0.65f + energy * 1.1f)
                    val alpha = (0.55f - i * 0.075f).coerceAtLeast(0.02f) * (0.55f + energy * 0.9f).coerceAtMost(1f)
                    drawRoundRect(brush, Offset.Zero, Size(size.width, size.height), corner,
                        style = Stroke(width * 2f), alpha = alpha.coerceIn(0f, 1f))
                }
            }
            Row(Modifier.fillMaxSize().padding(start = 14.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f).clickable(enabled = state.phase == "ERROR") { controller.openSettings() },
                    verticalArrangement = Arrangement.Center) {
                    CupertinoText(when (state.phase) {
                        "LISTENING" -> "Listening · ${state.elapsed / 1000}s"
                        "PAUSED" -> "Paused · ${state.elapsed / 1000}s"
                        else -> state.message
                    }, fontSize = 13.sp, maxLines = 2, color = Color.White.copy(alpha = 0.92f))
                }
                when {
                    state.listening -> {
                        RoundAction(if (paused) SfSymbols.Monochrome.SFPlayFill else SfSymbols.Monochrome.SFPauseFill,
                            if (paused) "Resume dictation" else "Pause dictation", controller::pause)
                        RoundAction(SfSymbols.Monochrome.SFXmark, "Cancel dictation", controller::cancel)
                        SendAction("Finish and insert", controller::finish)
                    }
                    state.phase == "READY" -> {
                        RoundAction(SfSymbols.Monochrome.SFXmark, "Discard transcript", controller::cancel)
                        SendAction("Insert transcript", controller::finish)
                    }
                    state.phase == "ERROR" -> {
                        CupertinoButton(onClick = if (state.retry) controller::retry else controller::openSettings,
                            colors = CupertinoButtonDefaults.plainButtonColors(contentColor = Color.White)) {
                            CupertinoText(if (state.retry) "Retry" else "Open", fontSize = 13.sp)
                        }
                        RoundAction(SfSymbols.Monochrome.SFXmark, "Dismiss", controller::cancel)
                    }
                    else -> RoundAction(SfSymbols.Monochrome.SFXmark, "Cancel dictation", controller::cancel)
                }
            }
        }
    }
}

/** 40dp glass-like circle (48dp touch target) with a white glyph. */
@Composable
private fun RoundAction(icon: ImageVector, label: String, action: () -> Unit) {
    Box(Modifier.size(48.dp).clickable(onClick = action).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(34.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
            CupertinoIcon(icon, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

/** Solid white circle with an up arrow. Finishes dictation; it never sends the message. */
@Composable
private fun SendAction(label: String, action: () -> Unit) {
    Box(Modifier.size(48.dp).clickable(onClick = action).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(34.dp)) {
            drawCircle(Color.White)
            val c = center
            val s = size.minDimension * 0.22f
            val stroke = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
            drawLine(Color.Black, Offset(c.x, c.y + s), Offset(c.x, c.y - s), stroke.width, StrokeCap.Round)
            drawLine(Color.Black, Offset(c.x - s, c.y - s * 0.1f), Offset(c.x, c.y - s), stroke.width, StrokeCap.Round)
            drawLine(Color.Black, Offset(c.x + s, c.y - s * 0.1f), Offset(c.x, c.y - s), stroke.width, StrokeCap.Round)
        }
    }
}
