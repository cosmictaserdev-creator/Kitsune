// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.getValue
import androidx.navigation.compose.currentBackStackEntryAsState
import helium314.keyboard.settings.hig.*
import zone.ien.hig.*
import zone.ien.hig.utils.rememberDefaultBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import zone.ien.hig.theme.CupertinoTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import helium314.keyboard.latin.common.LocaleUtils.constructLocale
import helium314.keyboard.latin.settings.SettingsSubtype.Companion.toSettingsSubtype
import helium314.keyboard.latin.settings.getTransitionAnimationScale
import helium314.keyboard.settings.screens.AboutScreen
import helium314.keyboard.settings.screens.AdvancedSettingsScreen
import helium314.keyboard.settings.screens.AppearanceScreen
import helium314.keyboard.settings.screens.CloudScreen
import helium314.keyboard.settings.screens.ColorsScreen
import helium314.keyboard.settings.screens.DebugScreen
import helium314.keyboard.settings.screens.DictionaryScreen
import helium314.keyboard.settings.screens.GestureTypingScreen
import helium314.keyboard.settings.screens.LanguageScreen
import helium314.keyboard.settings.screens.MainSettingsScreen
import helium314.keyboard.settings.screens.PersonalDictionariesScreen
import helium314.keyboard.settings.screens.PersonalDictionaryScreen
import helium314.keyboard.settings.screens.PreferencesScreen
import helium314.keyboard.settings.screens.SecondaryLayoutScreen
import helium314.keyboard.settings.screens.SubtypeScreen
import helium314.keyboard.settings.screens.TextCorrectionScreen
import helium314.keyboard.settings.screens.ToolbarScreen
import helium314.keyboard.settings.screens.gesturedata.GestureDataScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)
@Composable
fun SettingsNavHost(
    showWelcomeWizard: Boolean,
    onShowWelcomeWizardChanged: (Boolean) -> Unit,
    onClickBack: () -> Unit,
    startDestination: String? = null,
) {
    val navController = rememberNavController()
    val dir = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
    val target = SettingsDestination.navTarget.collectAsState()

    // duration does not change when system setting changes, but that's rare enough to not care
    val duration = (250 * getTransitionAnimationScale(LocalContext.current)).toInt()
    val animation = tween<IntOffset>(durationMillis = duration)

    fun goBack() {
        if (!navController.popBackStack()) onClickBack()
    }

    val entry by navController.currentBackStackEntryAsState()
    val roots = listOf(SettingsDestination.Settings, SettingsDestination.Themes, SettingsDestination.Whispry, SettingsDestination.AppSettings)
    val labels = listOf("Keyboard", "Themes", "Whispry", "Settings")
    val icons = listOf(SettingsIcons.Keyboard, SettingsIcons.Themes, SettingsIcons.Voice, SettingsIcons.Settings)
    val backdrop = rememberDefaultBackdrop()
    val selected = roots.indexOf(entry?.destination?.route)
    fun selectTab(index: Int) = navController.navigate(roots[index]) {
        popUpTo(SettingsDestination.Settings) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
    // Floating pill: content fills the screen and scrolls behind the bar, which samples it for the glass effect.
    Box(Modifier.fillMaxSize()) {
    CompositionLocalProvider(LocalFloatingBarInset provides if (selected >= 0) 104.dp else 0.dp) {
    Box(Modifier.fillMaxSize().layerBackdrop(backdrop)) {
    NavHost(
        navController = navController,
        startDestination = startDestination ?: if (showWelcomeWizard) SettingsDestination.WelcomeWizard else SettingsDestination.Settings,
        enterTransition = { slideInHorizontally(initialOffsetX = { +it * dir }, animationSpec = animation) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it * dir }, animationSpec = animation) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it * dir }, animationSpec = animation) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { +it * dir }, animationSpec = animation) }
    ) {
        composable(SettingsDestination.Settings) {
            KeyboardDashboard { navController.navigate(it) }

        }
        composable(SettingsDestination.Themes) { ThemesTab { navController.navigate(it) } }
        composable(SettingsDestination.Whispry) { WhisprySettingsTab() }
        composable(SettingsDestination.AppSettings) { AppSettingsTab { navController.navigate(it) } }
        composable(SettingsDestination.About) {
            AboutScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Cloud) {
            CloudScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.TextCorrection) {
            TextCorrectionScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Preferences) {
            PreferencesScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Toolbar) {
            ToolbarScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.GestureTyping) {
            GestureTypingScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.DataGathering) {
            GestureDataScreen(onClickBack = ::goBack)
        }
/*      will be added as part of passive data gathering
        composable(SettingsDestination.DataReview) {
            ReviewScreen(onClickBack = ::goBack)
        }*/
        composable(SettingsDestination.Advanced) {
            AdvancedSettingsScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Debug) {
            DebugScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Appearance) {
            AppearanceScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.PersonalDictionary + "{locale}") {
            val locale = it.arguments?.getString("locale")?.takeIf { loc -> loc.isNotBlank() }?.constructLocale()
            PersonalDictionaryScreen(
                onClickBack = ::goBack,
                locale = locale
            )
        }
        composable(SettingsDestination.PersonalDictionaries) {
            PersonalDictionariesScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Languages) {
            LanguageScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Dictionaries) {
            DictionaryScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Layouts) {
            SecondaryLayoutScreen(onClickBack = ::goBack)
        }
        composable(SettingsDestination.Colors + "{theme}") {
            ColorsScreen(isNight = false, theme = it.arguments?.getString("theme"), onClickBack = ::goBack)
        }
        composable(SettingsDestination.ColorsNight + "{theme}") {
            ColorsScreen(isNight = true, theme = it.arguments?.getString("theme"), onClickBack = ::goBack)
        }
        composable(SettingsDestination.Subtype + "{subtype}") {
            SubtypeScreen(initialSubtype = it.arguments?.getString("subtype")!!.toSettingsSubtype(), onClickBack = ::goBack)
        }
        composable(SettingsDestination.WelcomeWizard) {
            WelcomeWizard(
                close = {
                    onShowWelcomeWizardChanged(false)
                    navController.navigate(SettingsDestination.Settings) {
                        popUpTo(SettingsDestination.WelcomeWizard) { inclusive = true }
                    }
                },
                finish = onClickBack
            )
        }
    }
    }
    }
    if (selected >= 0) {
        CupertinoNavigationBar(
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding(),
            backdrop = backdrop,
            selectedTabIndex = { selected },
            onTabSelected = ::selectTab,
            tabsCount = roots.size,
        ) {
            roots.forEachIndexed { index, route ->
                CupertinoNavigationBarItem(
                    onClick = { selectTab(index) },
                    icon = { CupertinoIcon(icons[index], null,
                        tint = if (selected == index) CupertinoTheme.colorScheme.accent else CupertinoTheme.colorScheme.secondaryLabel) },
                    label = { CupertinoText(labels[index]) },
                )
            }
        }
    }
    }
    LaunchedEffect(target.value) {
        if (target.value != SettingsDestination.Settings
            && target.value != navController.currentBackStackEntry?.destination?.route)
            navController.navigate(route = target.value)
    }
}

object SettingsDestination {
    const val Settings = "settings"
    const val Themes = "themes"
    const val Whispry = "whispry"
    const val AppSettings = "app_settings"
    const val WelcomeWizard = "welcome_wizard"
    const val About = "about"
    const val Cloud = "cloud"
    const val TextCorrection = "text_correction"
    const val Preferences = "preferences"
    const val Toolbar = "toolbar"
    const val GestureTyping = "gesture_typing"
    const val DataGathering = "data_gathering" // remove when data gathering phase is done (end of 2026 latest)
    const val DataReview = "data_review" // remove when data gathering phase is done (end of 2026 latest)
    const val Advanced = "advanced"
    const val Debug = "debug"
    const val Appearance = "appearance"
    const val Colors = "colors/"
    const val ColorsNight = "colors_night/"
    const val PersonalDictionaries = "personal_dictionaries"
    const val PersonalDictionary = "personal_dictionary/"
    const val Languages = "languages"
    const val Subtype = "subtype/"
    const val Layouts = "layouts"
    const val Dictionaries = "dictionaries"
    val navTarget = MutableStateFlow(Settings)

    private val navScope = CoroutineScope(Dispatchers.Default)
    fun navigateTo(target: String) {
        if (navTarget.value == target) {
            // triggers recompose twice, but that's ok as it's a rare event
            navTarget.value = Settings
            navScope.launch { delay(10); navTarget.value = target }
        } else
            navTarget.value = target
        navScope.launch { delay(50); navTarget.value = Settings }
    }
}
