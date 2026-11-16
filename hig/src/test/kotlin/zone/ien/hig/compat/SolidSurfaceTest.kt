// SPDX-License-Identifier: Apache-2.0
package zone.ien.hig.compat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import zone.ien.hig.*
import zone.ien.hig.theme.CupertinoTheme
import zone.ien.hig.utils.rememberDefaultBackdrop

/** Exercise real component composition and interaction without evaluating glass shaders. */
@OptIn(ExperimentalCupertinoApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 30, 34])
class SolidSurfaceTest {
    @get:Rule val rule = createComposeRule()

    @Test fun `solid rendering keeps controls interactive and skips effect callbacks`() {
        var changed = false
        rule.setContent {
            CompositionLocalProvider(LocalGlassEnabled provides false) {
                CupertinoTheme {
                    val backdrop = rememberDefaultBackdrop()
                    Column(Modifier.size(300.dp).adaptiveBackdrop(backdrop, { RoundedCornerShape(20.dp) },
                        effects = { error("Glass callbacks must never execute in solid mode") })) {
                        CupertinoButton(onClick = { changed = true }) { CupertinoText("Apply") }
                        CupertinoSwitch(false, { changed = true })
                    }
                }
            }
        }
        rule.onNodeWithText("Apply").performClick()
        rule.waitForIdle()
        assertTrue(changed)
    }
}
