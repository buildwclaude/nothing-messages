package com.buildwclaude.messages.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

val MessagesShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(27.dp),
)

private fun schemeFrom(p: Palette, dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = p.Blue,
        onPrimary = androidx.compose.ui.graphics.Color.White,
        primaryContainer = p.Blue,
        onPrimaryContainer = androidx.compose.ui.graphics.Color.White,
        secondary = p.TextSecondary,
        tertiary = p.Navy,
        background = p.Surface,
        onBackground = p.TextPrimary,
        surface = p.Surface,
        onSurface = p.TextPrimary,
        surfaceVariant = p.IncomingBubble,
        onSurfaceVariant = p.TextSecondary,
        surfaceContainer = p.SurfaceSubtle,
        surfaceContainerLow = p.SurfaceSubtle,
        surfaceContainerHigh = p.IncomingBubble,
        error = p.Error,
        outline = p.Divider,
        outlineVariant = p.Divider,
    )
}

/** Follows the system light/dark setting. */
@Composable
fun MessagesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val p = if (darkTheme) DarkPalette else LightPalette
    CompositionLocalProvider(LocalPalette provides p) {
        MaterialTheme(
            colorScheme = schemeFrom(p, darkTheme),
            typography = MessagesTypography,
            shapes = MessagesShapes,
            content = content,
        )
    }
}
