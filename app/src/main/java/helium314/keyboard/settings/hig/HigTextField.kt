// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.hig

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import zone.ien.hig.CupertinoTextField

@Suppress("UNUSED_PARAMETER")
@Composable
fun HigTextField(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, readOnly: Boolean = false, textStyle: TextStyle = TextStyle.Default,
    label: @Composable (() -> Unit)? = null, placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null, trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null, isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false, maxLines: Int = Int.MAX_VALUE, minLines: Int = 1,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    colors: TextFieldColors = TextFieldDefaults.colors()) {
    Column(modifier) {
        label?.invoke()
        CupertinoTextField(value, onValueChange, enabled = enabled, readOnly = readOnly, textStyle = textStyle,
            placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon, isError = isError,
            visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            singleLine = singleLine, maxLines = if (singleLine) 1 else maxLines, minLines = minLines)
        supportingText?.invoke()
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun HigTextField(value: TextFieldValue, onValueChange: (TextFieldValue) -> Unit, modifier: Modifier = Modifier,
    enabled: Boolean = true, readOnly: Boolean = false, textStyle: TextStyle = TextStyle.Default,
    label: @Composable (() -> Unit)? = null, placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null, trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null, isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default, keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false, maxLines: Int = Int.MAX_VALUE, minLines: Int = 1,
    shape: Shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    colors: TextFieldColors = TextFieldDefaults.colors()) {
    Column(modifier) {
        label?.invoke()
        CupertinoTextField(value, onValueChange, enabled = enabled, readOnly = readOnly, textStyle = textStyle,
            placeholder = placeholder, leadingIcon = leadingIcon, trailingIcon = trailingIcon, isError = isError,
            visualTransformation = visualTransformation, keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
            singleLine = singleLine, maxLines = if (singleLine) 1 else maxLines, minLines = minLines)
        supportingText?.invoke()
    }
}
