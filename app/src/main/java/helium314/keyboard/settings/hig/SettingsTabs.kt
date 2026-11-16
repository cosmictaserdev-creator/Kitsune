// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)
package helium314.keyboard.settings.hig

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.getStringResourceOrName
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.settings.SettingsDestination
import helium314.keyboard.settings.preferences.SettingsSwitch
import zone.ien.hig.*
import zone.ien.hig.theme.CupertinoTheme

@Composable
fun KeyboardDashboard(navigate: (String) -> Unit) {
    SearchSettingsScreen({}, "Whispry Keys", emptyList(), showBackButton = false) {
        SettingsSection("Make it yours", "Your keyboard, your words. Voice tools connect to the separate Whispry app.") {
            SettingsRow("Languages & layouts", "Choose how you type", SettingsIcons.Language) { navigate(SettingsDestination.Languages) }
            SettingsRow("Typing", "Autocorrection, suggestions and spacing", SettingsIcons.Typing) { navigate(SettingsDestination.TextCorrection) }
            SettingsRow("Touch & feedback", "Sound, vibration and long press", SettingsIcons.Gesture) { navigate(SettingsDestination.Preferences) }
        }
        SettingsSection("Keyboard tools") {
            SettingsRow("Toolbar", "Arrange your everyday actions", SettingsIcons.Layout) { navigate(SettingsDestination.Toolbar) }
            SettingsRow("Gesture typing", "Swipe, trails and gesture actions", SettingsIcons.Gesture) { navigate(SettingsDestination.GestureTyping) }
            SettingsRow("Dictionaries", "Suggestions and personal words", SettingsIcons.Dictionary) { navigate(SettingsDestination.Dictionaries) }
            SettingsRow("Number & symbol layouts", "Extra rows and alternative characters", SettingsIcons.Keyboard) { navigate(SettingsDestination.Layouts) }
        }
        SettingsSection("Try your keyboard") {
            var testText by rememberSaveable { mutableStateOf("") }
            CupertinoTextField(testText, { testText = it }, Modifier.fillMaxWidth().padding(16.dp),
                placeholder = { CupertinoText("Tap here and start typing") }, minLines = 2)
        }
    }
}

@Composable
fun AppSettingsTab(navigate: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = context.prefs()
    var solid by rememberSaveable { mutableStateOf(prefs.getBoolean("settings_solid_surfaces", Build.VERSION.SDK_INT < 31)) }
    SearchSettingsScreen({}, "Settings", emptyList(), showBackButton = false) {
        SettingsSection("Interface", "Older Android versions use solid surfaces automatically.") {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    CupertinoText("Reduce transparency")
                    CupertinoText("Use clean, solid surfaces", color = CupertinoTheme.colorScheme.secondaryLabel, style = CupertinoTheme.typography.footnote)
                }
                SettingsSwitch(solid, { solid = it; prefs.edit { putBoolean("settings_solid_surfaces", it) } }, enabled = Build.VERSION.SDK_INT >= 31)
            }
        }
        SettingsSection("Manage") {
            SettingsRow("Keyboard setup", "Enable or select your keyboard", SettingsIcons.Keyboard) { navigate(SettingsDestination.WelcomeWizard) }
            SettingsRow("Advanced", "Clipboard, backups and compatibility", SettingsIcons.Settings) { navigate(SettingsDestination.Advanced) }
            SettingsRow("Connected services", "Existing AI and media integrations", SettingsIcons.Cloud) { navigate(SettingsDestination.Cloud) }
            SettingsRow("About & licenses", "Version, privacy and open-source credits", SettingsIcons.About) { navigate(SettingsDestination.About) }
        }
    }
}

@Composable
fun WhisprySettingsTab() {
    val context = LocalContext.current
    var available by remember { mutableStateOf(false) }
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        available = context.packageManager.getLaunchIntentForPackage(helium314.keyboard.latin.whispry.WhispryKeyboardController.APP) != null
        onPauseOrDispose { }
    }
    fun openApp() {
        val intent = context.packageManager.getLaunchIntentForPackage(helium314.keyboard.latin.whispry.WhispryKeyboardController.APP)
        if (intent != null) context.startActivity(intent)
        else {
            val store = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.cosmictaser.whispry"))
            try { context.startActivity(store) } catch (_: android.content.ActivityNotFoundException) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.cosmictaser.whispry")))
            }
        }
    }
    SearchSettingsScreen({}, "Whispry", emptyList(), hideTopSearchBar = true, showBackButton = false) {
        SettingsSection("Voice & writing", "Whispry manages your providers, recordings and personal writing tools.") {
            SettingsRow(if (available) "Open Whispry" else "Get Whispry",
                if (available) "Manage voice recognition and writing tools" else "Install the companion app to set up voice tools",
                SettingsIcons.Voice, ::openApp)
        }
        SettingsSection("In Whispry") {
            SettingsRow("Tones & translation", "Choose your writing style and output language", SettingsIcons.Sparkles, ::openApp)
            SettingsRow("Meetings & history", "Recordings, transcripts and notes", SettingsIcons.Dictionary, ::openApp)
            SettingsRow("Your words", "Dictionary, snippets and memory", SettingsIcons.Typing, ::openApp)
        }
    }
}

@Composable
fun ThemesTab(navigate: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = context.prefs()
    val changes by ((context.getActivity() as SettingsActivity).prefChanged).collectAsState()
    var collection by rememberSaveable { mutableIntStateOf(0) }
    val selected = remember(changes) { prefs.getString(Settings.PREF_THEME_COLORS, Defaults.PREF_THEME_COLORS)!! }
    val builtIns = remember(changes) { KeyboardTheme.getAvailableDefaultColors(prefs, false) }
    val custom = remember(changes) {
        prefs.all.keys.mapNotNull { key -> listOf(Settings.PREF_USER_COLORS_PREFIX, Settings.PREF_USER_ALL_COLORS_PREFIX, Settings.PREF_USER_MORE_COLORS_PREFIX)
            .firstOrNull { key.startsWith(it) }?.let { key.removePrefix(it) } }.distinct().sorted()
    }
    SearchSettingsScreen({}, "Themes", emptyList(), hideTopSearchBar = true, showBackButton = false) {
        SettingsSection("Your keyboard") {
            ThemePreview(selected, Modifier.fillMaxWidth().padding(16.dp))
            val billing = remember { helium314.keyboard.latin.billing.KitsuneBilling.get(context) }
            val owned by billing.owned.collectAsState()
            val pro = helium314.keyboard.latin.billing.Products.PRO in owned
            // The detailed editor is the Pro "Theme Studio"; locked until a verified purchase exists.
            SettingsRow(if (pro) "Theme Studio" else "Theme Studio (Pro)", "Colors, shapes, borders, fonts and background", SettingsIcons.Themes) {
                if (pro) navigate(SettingsDestination.Appearance)
                else context.getActivity()?.let { billing.buy(it, helium314.keyboard.latin.billing.Products.PRO) }
            }
        }
        SettingsSection("Kitsune Pro", "One-time purchase. Unlocks the advanced theme editor and every collection.") {
            val billing = remember { helium314.keyboard.latin.billing.KitsuneBilling.get(context) }
            val owned by billing.owned.collectAsState()
            val message by billing.message.collectAsState()
            val pro = helium314.keyboard.latin.billing.Products.PRO in owned
            SettingsRow(if (pro) "Kitsune Pro is unlocked" else "Unlock Kitsune Pro",
                message.ifBlank { if (pro) "Thank you for supporting Kitsune" else "Theme editor, collections and more" }, SettingsIcons.Sparkles) {
                if (!pro) context.getActivity()?.let { billing.buy(it, helium314.keyboard.latin.billing.Products.PRO) }
            }
            if (!pro) SettingsRow("Restore purchases", "Already bought on another device?", SettingsIcons.Check) { billing.restore() }
        }
        CupertinoSegmentedControl(collection, Modifier.fillMaxWidth().padding(horizontal = 20.dp), tabs = {
            listOf("Included", "My themes", "Collections").forEachIndexed { index, title ->
                CupertinoSegmentedControlTab({ collection = index }, collection == index) { CupertinoText(title) }
            }
        })
        if (collection == 2) {
            SettingsSection("Collections", "Purchased theme artwork will be offered through Google Play when collections become available.") {
                Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CupertinoIcon(SettingsIcons.Sparkles, null, tint = CupertinoTheme.colorScheme.accent, modifier = Modifier.size(32.dp))
                    CupertinoText("Something beautiful is on the way", style = CupertinoTheme.typography.headline)
                    CupertinoText("Your included themes and custom designs are always here.", color = CupertinoTheme.colorScheme.secondaryLabel)
                }
            }
        } else {
            val themes = if (collection == 0) builtIns else custom
            if (themes.isEmpty()) SettingsSection("Start with a color") {
                SettingsRow("Create your first theme", "Open the visual appearance editor", SettingsIcons.Themes) { navigate(SettingsDestination.Appearance) }
            }
            themes.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { name ->
                        val active = name == selected
                        val shape = RoundedCornerShape(20.dp)
                        Column(Modifier.weight(1f).background(CupertinoTheme.colorScheme.secondarySystemGroupedBackground, shape)
                            .border(if (active) 2.dp else 0.dp, if (active) CupertinoTheme.colorScheme.accent else Color.Transparent, shape)
                            .clickable {
                                prefs.edit { putString(Settings.PREF_THEME_COLORS, name) }
                                KeyboardSwitcher.getInstance().setThemeNeedsReload()
                            }.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            ThemePreview(name, Modifier.fillMaxWidth())
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CupertinoText(name.getStringResourceOrName("theme_name_", context), modifier = Modifier.weight(1f), style = CupertinoTheme.typography.subhead)
                                if (active) CupertinoIcon(SettingsIcons.Check, "Selected", tint = CupertinoTheme.colorScheme.accent, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

/** Preview uses the same palette resolver as the actual keyboard, without changing preferences. */
@Composable
fun ThemePreview(name: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val palette = KeyboardTheme.getPreviewColors(context, name)
    val background = Color(palette.get(helium314.keyboard.latin.common.ColorType.MAIN_BACKGROUND))
    val key = Color(palette.get(helium314.keyboard.latin.common.ColorType.KEY_BACKGROUND))
    val text = Color(palette.get(helium314.keyboard.latin.common.ColorType.KEY_TEXT))
    Column(modifier.background(background, RoundedCornerShape(12.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("qwertyuiop", "asdfghjkl", "zxcvbnm").forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally)) {
                row.forEach { char ->
                    Box(Modifier.weight(1f).height(19.dp).background(key, RoundedCornerShape(4.dp)), contentAlignment = Alignment.Center) {
                        CupertinoText(char.toString(), color = text, style = CupertinoTheme.typography.caption2)
                    }
                }
            }
        }
        Box(Modifier.fillMaxWidth(.55f).align(Alignment.CenterHorizontally).height(13.dp).background(key, RoundedCornerShape(4.dp)))
    }
}
