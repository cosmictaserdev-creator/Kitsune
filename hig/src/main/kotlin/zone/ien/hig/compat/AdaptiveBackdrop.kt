// SPDX-License-Identifier: Apache-2.0
package zone.ien.hig.compat

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.GraphicsLayerScope
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import zone.ien.hig.theme.CupertinoTheme

/** Set false for reduced transparency. Unsupported devices always use opaque surfaces. */
val LocalGlassEnabled = staticCompositionLocalOf { true }

internal fun supportsGlass(sdk: Int, accelerated: Boolean, enabled: Boolean) =
    sdk >= 31 && accelerated && enabled

/** Never evaluates shader/effect callbacks on the solid-surface path. */
@Composable
fun Modifier.adaptiveBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    highlight: (() -> Highlight?)? = { Highlight.Default },
    shadow: (() -> Shadow?)? = { Shadow.Default },
    innerShadow: (() -> InnerShadow?)? = null,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() },
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null,
): Modifier {
    if (supportsGlass(Build.VERSION.SDK_INT, LocalView.current.isHardwareAccelerated, LocalGlassEnabled.current)) {
        return drawBackdrop(backdrop, shape, effects, highlight, shadow, innerShadow, layerBlock,
            exportedBackdrop, onDrawBehind, onDrawBackdrop, onDrawSurface, onDrawFront)
    }
    val surface = CupertinoTheme.colorScheme.secondarySystemGroupedBackground.copy(alpha = 1f)
    return graphicsLayer {
        layerBlock?.invoke(this)
        this.shape = shape()
        clip = true
    }.drawWithContent {
        onDrawBehind?.invoke(this)
        drawRect(surface)
        onDrawSurface?.invoke(this)
        drawContent()
        onDrawFront?.invoke(this)
    }
}

@Composable
fun Modifier.adaptivePlainBackdrop(
    backdrop: Backdrop,
    shape: () -> Shape,
    effects: BackdropEffectScope.() -> Unit,
    layerBlock: (GraphicsLayerScope.() -> Unit)? = null,
    exportedBackdrop: LayerBackdrop? = null,
    onDrawBehind: (DrawScope.() -> Unit)? = null,
    onDrawBackdrop: DrawScope.(DrawScope.() -> Unit) -> Unit = { it() },
    onDrawSurface: (DrawScope.() -> Unit)? = null,
    onDrawFront: (DrawScope.() -> Unit)? = null,
): Modifier = adaptiveBackdrop(backdrop, shape, effects, highlight = null, shadow = null,
    layerBlock = layerBlock, exportedBackdrop = exportedBackdrop, onDrawBehind = onDrawBehind,
    onDrawBackdrop = onDrawBackdrop, onDrawSurface = onDrawSurface, onDrawFront = onDrawFront)
