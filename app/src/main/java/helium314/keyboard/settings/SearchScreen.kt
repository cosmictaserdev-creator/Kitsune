// SPDX-License-Identifier: GPL-3.0-only
@file:OptIn(zone.ien.hig.ExperimentalCupertinoApi::class, androidx.compose.ui.text.ExperimentalTextApi::class)
package helium314.keyboard.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import helium314.keyboard.latin.R
import helium314.keyboard.settings.hig.SettingsIcons
import helium314.keyboard.settings.preferences.PreferenceCategory
import helium314.keyboard.settings.preferences.PreferenceGroup
import zone.ien.hig.*
import zone.ien.hig.theme.CupertinoTheme

val LocalHazeState = staticCompositionLocalOf<HazeState> { error("No HazeState provided") }
/** Space reserved for the floating tab bar on root tabs. */
val LocalFloatingBarInset = staticCompositionLocalOf { 0.dp }
val LocalSearchInnerPadding = staticCompositionLocalOf { PaddingValues(0.dp) }
data class SearchState(
    val showSearch: Boolean,
    val searchText: TextFieldValue,
    val onSearchChange: (TextFieldValue) -> Unit,
    val setShowSearch: (Boolean) -> Unit,
    val searchField: @Composable () -> Unit
)
val LocalSearchState = staticCompositionLocalOf<SearchState?> { null }
val materialSymbols = FontFamily(Font(R.font.material_symbols_rounded, variationSettings = FontVariation.Settings(FontVariation.Setting("FILL", 1f))))
val googleSansFlex = FontFamily.Default

@Composable
fun SearchSettingsScreen(
    onClickBack: () -> Unit,
    title: String,
    settings: List<Any?>,
    hideTopSearchBar: Boolean = false,
    showBackButton: Boolean = true,
    filteredItems: ((String) -> List<Setting>)? = null,
    content: @Composable (ColumnScope.() -> Unit)? = null
) {
    SearchScreen(onClickBack, { CupertinoText(title) },
        filteredItems ?: { SettingsActivity.settingsContainer.filter(it) },
        itemContent = { it.Preference() }, hideTopSearchBar = hideTopSearchBar, showBackButton = showBackButton,
        content = content ?: {
            // Split into sections at each header (Int) and render every section as one grouped card.
            val sections = mutableListOf<Pair<Int?, MutableList<Any>>>()
            settings.forEach { setting ->
                when (setting) {
                    is Int -> sections += setting to mutableListOf()
                    null -> Unit
                    else -> {
                        if (sections.isEmpty()) sections += null to mutableListOf()
                        sections.last().second += setting
                    }
                }
            }
            sections.forEach { (header, items) ->
                if (header != null) PreferenceCategory(stringResource(header))
                if (items.isNotEmpty()) PreferenceGroup { items.forEach { SettingsActivity.settingsContainer[it]?.Preference() } }
            }
        })
}

@Composable
fun <T: Any?> SearchScreen(
    onClickBack: () -> Unit,
    title: @Composable () -> Unit,
    filteredItems: (String) -> List<T>,
    itemContent: @Composable (T) -> Unit,
    itemKey: ((T) -> Any)? = null,
    icon: @Composable (() -> Unit)? = null,
    menu: List<Pair<String, () -> Unit>>? = null,
    hideTopSearchBar: Boolean = false,
    showBackButton: Boolean = true,
    content: @Composable (ColumnScope.() -> Unit)? = null,
) {
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue()) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val haze = remember { HazeState() }
    fun closeSearch(value: Boolean) { showSearch = value; if (!value) query = TextFieldValue() }
    BackHandler(enabled = showBackButton || showSearch || query.text.isNotEmpty()) {
        if (showSearch || query.text.isNotEmpty()) closeSearch(false) else onClickBack()
    }
    val field: @Composable () -> Unit = {
        StaticSearchField(query, { query = it; showSearch = true },
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp), onClearClick = { closeSearch(false) })
    }
    CompositionLocalProvider(LocalHazeState provides haze,
        LocalSearchState provides SearchState(showSearch, query, { query = it }, ::closeSearch, field)) {
        Box(Modifier.fillMaxSize()) {
        CupertinoScaffold(containerColor = CupertinoTheme.colorScheme.systemGroupedBackground,
            topBar = {
                if (!SettingsActivity.isTopBarHidden && SettingsActivity.activeOverlay == null) {
                    CupertinoTopAppBar(title = title, isCenterAligned = false,
                        navigationIcon = {
                            if (showBackButton) CupertinoIconButton(onClick = onClickBack) {
                                CupertinoIcon(SettingsIcons.Back, stringResource(helium314.keyboard.latin.R.string.whispry_back))
                            }
                        },
                        actions = {
                            icon?.invoke()
                            if (!hideTopSearchBar) CupertinoIconButton(onClick = { showSearch = !showSearch }) {
                                CupertinoIcon(SettingsIcons.Search, stringResource(R.string.whispry_search_settings))
                            }
                            if (menu != null) CupertinoIconButton(onClick = { showMenu = true }) {
                                CupertinoIcon(SettingsIcons.More, stringResource(R.string.whispry_more))
                            }
                        })
                }
            }) { padding ->
            CompositionLocalProvider(LocalSearchInnerPadding provides padding) {
                LazyColumn(Modifier.fillMaxSize().then(
                    if (android.os.Build.VERSION.SDK_INT >= 31 && zone.ien.hig.compat.LocalGlassEnabled.current)
                        Modifier.haze(haze) else Modifier
                ), contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding() + 20.dp + LocalFloatingBarInset.current)) {
                    if (!hideTopSearchBar || showSearch) item("search") { field() }
                    if (query.text.isNotBlank() || content == null) {
                        val results = filteredItems(query.text)
                        if (results.isEmpty()) item { CupertinoText(stringResource(R.string.whispry_no_results),
                            modifier = Modifier.padding(24.dp), color = CupertinoTheme.colorScheme.secondaryLabel) }
                        if (itemKey == null) items(results) { itemContent(it) }
                        else items(results, key = itemKey) { itemContent(it) }
                    } else item("content") { Column(Modifier.fillMaxWidth(), content = content) }
                }
            }
        }
        SettingsActivity.activeOverlay?.invoke(haze)
        }
    }
    if (showMenu && menu != null) CupertinoActionSheet(visible = true, onDismissRequest = { showMenu = false }, buttons = {
        menu.forEach { (label, action) -> default(onClick = { showMenu = false; action() }) { CupertinoText(label) } }
        cancel(onClick = { showMenu = false }) { CupertinoText(stringResource(android.R.string.cancel)) }
    })
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun StaticSearchField(
    search: TextFieldValue,
    onSearchChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    colors: TextFieldColors = TextFieldDefaults.colors(),
    onClearClick: (() -> Unit)? = null
) {
    CupertinoTextField(search, onSearchChange, modifier = modifier,
        leadingIcon = { CupertinoIcon(SettingsIcons.Search, null, modifier = Modifier.size(18.dp)) },
        trailingIcon = {
            if (search.text.isNotEmpty()) CupertinoIconButton(onClick = { onSearchChange(TextFieldValue()); onClearClick?.invoke() }) {
                CupertinoIcon(SettingsIcons.Close, stringResource(R.string.whispry_clear_search), modifier = Modifier.size(14.dp))
            }
        }, placeholder = { CupertinoText(stringResource(R.string.whispry_search_settings)) },
        singleLine = true, keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search))
}
