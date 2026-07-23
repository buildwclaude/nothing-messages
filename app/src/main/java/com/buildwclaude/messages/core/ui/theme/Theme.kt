package com.buildwclaude.messages.core.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

// The design's corner radius scale: 4 (thumbnails), 8 (bubbles/cards/chips),
// 16, 24/27 (large cards, pinned tiles), 32 (sheet tops), circle for avatars.
val MessagesShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(27.dp),
)

private val LightColors = lightColorScheme(
    primary = DesignColors.Blue,
    onPrimary = DesignColors.Surface,
    primaryContainer = DesignColors.Blue,
    onPrimaryContainer = DesignColors.Surface,
    secondary = DesignColors.TextSecondary,
    onSecondary = DesignColors.Surface,
    tertiary = DesignColors.Navy,
    background = DesignColors.Surface,
    onBackground = DesignColors.TextPrimary,
    surface = DesignColors.Surface,
    onSurface = DesignColors.TextPrimary,
    surfaceVariant = DesignColors.IncomingBubble,
    onSurfaceVariant = DesignColors.TextSecondary,
    surfaceContainer = DesignColors.SurfaceSubtle,
    surfaceContainerLow = DesignColors.SurfaceSubtle,
    surfaceContainerHigh = DesignColors.IncomingBubble,
    error = DesignColors.Error,
    onError = DesignColors.Surface,
    outline = DesignColors.Divider,
    outlineVariant = DesignColors.Divider,
)

/**
 * The Figma design is light-only; we apply it in both system modes for fidelity.
 */
@Composable
fun MessagesTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = MessagesTypography,
        shapes = MessagesShapes,
        content = content,
    )
}
