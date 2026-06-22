package com.SE114.food_tracker.core.designsystem.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.SE114.food_tracker.core.designsystem.theme.MainBackground

/**
 * App-wide [Scaffold] pre-wired with [WindowInsets.safeDrawing] so content never sits
 * under the status bar, navigation/gesture bar, display cutout, or the IME.
 *
 * - Wrap each screen and apply the given [PaddingValues] to the root content.
 * - safeDrawing already covers status bar + nav bar + cutout — never hardcode a top pad.
 * - For an input pinned to the bottom, add `Modifier.imePadding()` so the keyboard lifts it.
 * - For a full-bleed element (e.g. an image header), opt out with `Modifier.consumeWindowInsets(...)`.
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    containerColor: Color = MainBackground,
    content: @Composable (PaddingValues) -> Unit
) {
    Scaffold(
        modifier = modifier,
        topBar = topBar,
        bottomBar = bottomBar,
        containerColor = containerColor,
        contentWindowInsets = WindowInsets.safeDrawing,
        content = content
    )
}
