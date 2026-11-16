/*
 * Copyright (C) 2021 The Android Open Source Project
 * parts taken from Material3 AlertDialog.kt
 * SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
 */
package helium314.keyboard.settings.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import zone.ien.hig.default
import zone.ien.hig.cancel
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import helium314.keyboard.settings.hig.HigSurface as Surface
import zone.ien.hig.CupertinoText as Text
import helium314.keyboard.settings.hig.HigTextButton as TextButton
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.latin.utils.previewDark

@OptIn(zone.ien.hig.ExperimentalCupertinoApi::class)
@Composable
fun ThreeButtonAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    title: @Composable (() -> Unit)? = null,
    content: @Composable (() -> Unit)? = null,
    scrollContent: Boolean = false,
    onNeutral: () -> Unit = { },
    checkOk: () -> Boolean = { true },
    confirmButtonText: String? = stringResource(android.R.string.ok),
    cancelButtonText: String = stringResource(android.R.string.cancel),
    neutralButtonText: String? = null,
    reducePadding: Boolean = false,
    properties: DialogProperties = DialogProperties()
) {
    zone.ien.hig.CupertinoAlertDialog(
        onDismissRequest = onDismissRequest,
        title = { title?.invoke() },
        message = {
            Box(modifier.heightIn(max = (androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp * .65f).dp)) {
                if (scrollContent) Column(Modifier.verticalScroll(rememberScrollState())) { content?.invoke() }
                else content?.invoke()
            }
        },
        properties = properties,
        buttons = {
            if (neutralButtonText != null) default(onClick = onNeutral) { Text(neutralButtonText) }
            cancel(onClick = onDismissRequest) { Text(cancelButtonText) }
            if (confirmButtonText != null) default(onClick = { onConfirmed(); onDismissRequest() }, enabled = checkOk()) { Text(confirmButtonText) }
        }
    )
}

@Preview
@Composable
private fun Preview() {
    Theme(previewDark) {
        ThreeButtonAlertDialog(
            onDismissRequest = {},
            onConfirmed = { },
            content = { Text("hello") },
            title = { Text("title") },
            neutralButtonText = "Default"
        )
    }
}
