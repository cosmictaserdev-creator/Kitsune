// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)
package helium314.keyboard.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings as AndroidSettings
import android.view.inputmethod.InputMethodManager
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.composables.sfsymbols.SfSymbols
import com.composables.sfsymbols.monochrome.*
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.UncachedInputMethodManagerUtils
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.whispry.WhispryKeyboardController
import helium314.keyboard.settings.hig.ThemePreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import zone.ien.hig.*

private val Ink = Color(0xFF0B0B0D)
private val Card = Color(0xFF1C1C1F)
private val Glow = listOf(Color(0xFF7B2FF7), Color(0xFFFF4D8D), Color(0xFFFF9F43), Color(0xFF3AA0FF), Color(0xFF7B2FF7))

private enum class Stage { Splash, Intro, Setup }

private data class Slide(val title: String, val body: String)
private val Slides = listOf(
    Slide("A keyboard that feels like yours", "Clean, fast typing with themes you will actually want to look at."),
    Slide("Speak. It writes.", "Dictate with a live, breathing glow, then finish with one tap. Works with the Whispry app."),
    Slide("Every tone, one double-tap", "Professional, casual, translated. Pick how your words should sound before you say them."),
    Slide("Make it unmistakably yours", "Start with the included themes. Unlock collections and the Pro editor whenever you like."),
)

/** First-run flow: splash, four animated slides, then live keyboard setup. Dark, calm and quick to skip. */
@Composable
fun KitsuneOnboarding(close: () -> Unit, finish: () -> Unit) {
    var stage by rememberSaveable { mutableStateOf(Stage.Splash) }
    LaunchedEffect(Unit) { if (stage == Stage.Splash) { delay(1100); stage = Stage.Intro } }
    BackHandler(stage == Stage.Setup) { stage = Stage.Intro }
    Box(Modifier.fillMaxSize().background(Ink)) {
        AnimatedContent(stage, transitionSpec = { fadeIn(tween(450)) togetherWith fadeOut(tween(300)) }, label = "stage") { s ->
            when (s) {
                Stage.Splash -> Splash()
                Stage.Intro -> Intro(onDone = { stage = Stage.Setup }, onSkip = { stage = Stage.Setup })
                Stage.Setup -> Setup(close)
            }
        }
    }
}

@Composable
private fun Splash() {
    val scale by rememberInfiniteTransition(label = "splash").animateFloat(0.94f, 1.06f,
        infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "scale")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(120.dp).graphicsLayer { scaleX = scale; scaleY = scale }) {
            drawCircle(Brush.sweepGradient(Glow, center), style = Stroke(10.dp.toPx()))
            drawCircle(Color.White, radius = size.minDimension * 0.18f)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Intro(onDone: () -> Unit, onSkip: () -> Unit) {
    val pager = rememberPagerState { Slides.size }
    var page by remember { mutableIntStateOf(0) }
    LaunchedEffect(pager) { snapshotFlow { pager.currentPage }.collect { page = it } }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
        Row(Modifier.fillMaxWidth().height(48.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.clickable(onClick = onSkip).padding(8.dp)) { CupertinoText("Skip  ›", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp) }
        }
        HorizontalPager(pager, Modifier.weight(1f)) { index ->
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
                Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Hero(index) }
                CupertinoText(Slides[index].title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                CupertinoText(Slides[index].body, color = Color.White.copy(alpha = 0.65f), fontSize = 16.sp)
                Spacer(Modifier.height(20.dp))
            }
        }
        Row(Modifier.padding(bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(Slides.size) { i ->
                val w by animateDpAsState(if (i == page) 22.dp else 6.dp, spring(), label = "dot")
                Box(Modifier.height(6.dp).width(w).clip(CircleShape).background(Color.White.copy(alpha = if (i == page) 0.95f else 0.3f)))
            }
        }
        WhitePill(if (page == Slides.lastIndex) "Get started" else "Continue") {
            if (page == Slides.lastIndex) onDone() else scope.launch { pager.animateScrollToPage(page + 1) }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun WhitePill(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(54.dp).clip(RoundedCornerShape(27.dp))
        .background(if (enabled) Color.White else Color.White.copy(alpha = 0.3f)).clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center) { CupertinoText(label, color = Color.Black, fontSize = 17.sp, fontWeight = FontWeight.SemiBold) }
}

@Composable
private fun Hero(index: Int) {
    val t = rememberInfiniteTransition(label = "hero")
    val phase by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "phase")
    when (index) {
        0 -> MockKeyboard(phase)
        1 -> BreathingCapsule(phase)
        2 -> ToneStrip(phase)
        else -> ThemeFan(phase)
    }
}

@Composable
private fun MockKeyboard(phase: Float) {
    val pressed = (phase * 24).toInt() % 24
    Column(Modifier.graphicsLayer { translationY = kotlin.math.sin(phase * 6.283f) * 8f }
        .clip(RoundedCornerShape(26.dp)).background(Card).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        var n = 0
        listOf(7, 7, 6).forEach { count ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.align(Alignment.CenterHorizontally)) {
                repeat(count) {
                    val on = n++ == pressed
                    val s by animateFloatAsState(if (on) 0.88f else 1f, spring(), label = "key")
                    Box(Modifier.size(36.dp, 44.dp).graphicsLayer { scaleX = s; scaleY = s }.clip(RoundedCornerShape(10.dp))
                        .background(if (on) Color(0xFF7B6BFF) else Color(0xFF3A3A40)))
                }
            }
        }
    }
}

@Composable
private fun BreathingCapsule(phase: Float) {
    val breath = (kotlin.math.sin(phase * 6.283f * 2) * 0.5f + 0.5f)
    Canvas(Modifier.fillMaxWidth().height(110.dp)) {
        val brush = Brush.sweepGradient(Glow.map { it }, center)
        val corner = CornerRadius(34.dp.toPx())
        drawRoundRect(Color(0xFF050507), cornerRadius = corner)
        for (i in 0..6) drawRoundRect(brush, cornerRadius = corner, style = Stroke((3f + i * 6f).dp.toPx() * (0.7f + breath * 0.8f)),
            alpha = (0.55f - i * 0.07f).coerceAtLeast(0.02f))
    }
}

@Composable
private fun ToneStrip(phase: Float) {
    val tones = listOf("📝 Raw", "💼 Professional", "😊 Casual", "✉️ Email", "🌐 Translate", "✅ Checklist", "📣 Social")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(2) { row ->
            Row(Modifier.graphicsLayer { translationX = (if (row == 0) -1 else 1) * phase * 160f }, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                (tones + tones).forEachIndexed { i, label ->
                    Box(Modifier.clip(RoundedCornerShape(20.dp)).background(if ((i + row) % 4 == 0) Color(0xFF3A2F7A) else Card)
                        .padding(horizontal = 18.dp, vertical = 22.dp)) { CupertinoText(label, color = Color.White, fontSize = 15.sp) }
                }
            }
        }
    }
}

@Composable
private fun ThemeFan(phase: Float) {
    val swatches = listOf(Color(0xFF2C2C2E) to Color(0xFF4A4A4F), Color(0xFF1F3A2B) to Color(0xFF3D7A56), Color(0xFF3A1F3F) to Color(0xFF8B4FA0))
    Box(contentAlignment = Alignment.Center) {
        swatches.forEachIndexed { i, (bg, key) ->
            val angle = (i - 1) * (14f + kotlin.math.sin(phase * 6.283f) * 3f)
            Column(Modifier.rotate(angle).size(210.dp, 150.dp).clip(RoundedCornerShape(24.dp)).background(bg).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(3) { Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(7) { Box(Modifier.weight(1f).height(26.dp).clip(RoundedCornerShape(6.dp)).background(key)) } } }
            }
        }
    }
}

@Composable
private fun Setup(done: () -> Unit) {
    val ctx = LocalContext.current
    val imm = ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    var enabled by remember { mutableStateOf(UncachedInputMethodManagerUtils.isThisImeEnabled(ctx, imm)) }
    var current by remember { mutableStateOf(UncachedInputMethodManagerUtils.isThisImeCurrent(ctx, imm)) }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) {
            enabled = UncachedInputMethodManagerUtils.isThisImeEnabled(ctx, imm)
            current = UncachedInputMethodManagerUtils.isThisImeCurrent(ctx, imm)
        } }
        owner.lifecycle.addObserver(obs); onDispose { owner.lifecycle.removeObserver(obs) }
    }
    // The picker closes without a lifecycle event on some devices, so poll briefly after opening it.
    var polling by remember { mutableStateOf(false) }
    LaunchedEffect(polling) { if (polling) { repeat(120) { if (!current) { delay(500)
        enabled = UncachedInputMethodManagerUtils.isThisImeEnabled(ctx, imm); current = UncachedInputMethodManagerUtils.isThisImeCurrent(ctx, imm) } }; polling = false } }

    val prefs = ctx.prefs()
    var whisprySkipped by rememberSaveable { mutableStateOf(false) }
    val whispryInstalled = remember(enabled, current, whisprySkipped) {
        ctx.packageManager.getLaunchIntentForPackage(WhispryKeyboardController.APP) != null }
    var themeName by remember { mutableStateOf(prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS)!!) }
    val themes = remember { KeyboardTheme.getAvailableDefaultColors(prefs, false).take(6) }

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        CupertinoText("Set up Kitsune", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.SemiBold)
        CupertinoText("Two quick system steps, then make it yours.", color = Color.White.copy(alpha = 0.65f), fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        StepRow("1", "Enable Kitsune", "Turn it on in your keyboard list", enabled)
        StepRow("2", "Select Kitsune", "Choose it as the active keyboard", current)
        StepRow("3", "Connect Whispry", if (whispryInstalled) "Whispry found. Voice typing is ready." else "Optional. Adds voice typing and tones.", whispryInstalled || whisprySkipped)
        if (current) {
            CupertinoText("Pick a look", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(themes, key = { it }) { name ->
                    Box(Modifier.width(120.dp).clip(RoundedCornerShape(14.dp))
                        .border(if (name == themeName) 2.dp else 0.dp, if (name == themeName) Color.White else Color.Transparent, RoundedCornerShape(14.dp))
                        .clickable { themeName = name; prefs.edit { putString(Settings.PREF_THEME_COLORS, name) }; KeyboardSwitcher.getInstance().setThemeNeedsReload() }) {
                        ThemePreview(name, Modifier.fillMaxWidth().padding(6.dp))
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        when {
            !enabled -> WhitePill("Enable Kitsune") {
                ctx.startActivity(Intent(AndroidSettings.ACTION_INPUT_METHOD_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); polling = true }
            !current -> WhitePill("Select Kitsune") { imm.showInputMethodPicker(); polling = true }
            !whispryInstalled && !whisprySkipped -> {
                WhitePill("Get Whispry") {
                    val play = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${WhispryKeyboardController.APP}"))
                    runCatching { ctx.startActivity(play.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } }
                Box(Modifier.fillMaxWidth().clickable { whisprySkipped = true }.padding(12.dp), contentAlignment = Alignment.Center) {
                    CupertinoText("Not now", color = Color.White.copy(alpha = 0.7f)) }
            }
            else -> WhitePill("Start typing", onClick = done)
        }
    }
}

@Composable
private fun StepRow(number: String, title: String, subtitle: String, done: Boolean) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Card).padding(14.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(32.dp).clip(CircleShape).background(if (done) Color(0xFF30D158) else Color.White.copy(alpha = 0.12f)), contentAlignment = Alignment.Center) {
            if (done) CupertinoIcon(SfSymbols.Monochrome.SFCheckmark, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            else CupertinoText(number, color = Color.White, fontSize = 14.sp)
        }
        Column {
            CupertinoText(title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            CupertinoText(subtitle, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
    }
}
