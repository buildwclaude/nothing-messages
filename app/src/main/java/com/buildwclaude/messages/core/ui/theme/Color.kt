package com.buildwclaude.messages.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Palette derived from the "Messaging App UI" Figma community file
 * (881015895655545375), with a dark variant added on request.
 */
data class Palette(
    val Blue: Color,
    val TextPrimary: Color,
    val TextSecondary: Color,
    val Navy: Color,
    val TimeText: Color,
    val MutedText: Color,
    val Placeholder: Color,
    val IncomingBubble: Color,
    val Surface: Color,
    val SurfaceSubtle: Color,
    val Divider: Color,
    val Online: Color,
    val Away: Color,
    val Error: Color,
    val Success: Color,
    val SenderAccents: List<Color>,
)

val LightPalette = Palette(
    Blue = Color(0xFF2F80ED),
    TextPrimary = Color(0xFF1B1A57),
    TextSecondary = Color(0xFF4F5E7B),
    Navy = Color(0xFF21205A),
    TimeText = Color(0xFF333333),
    MutedText = Color(0xFFA1A1BC),
    Placeholder = Color(0xFFC4C4C4),
    IncomingBubble = Color(0xFFF7F7F7),
    Surface = Color(0xFFFFFFFF),
    SurfaceSubtle = Color(0xFFF8F9FC),
    Divider = Color(0xFFEDEDED),
    Online = Color(0xFF4CE417),
    Away = Color(0xFFF2C94C),
    Error = Color(0xFFEB5757),
    Success = Color(0xFF27AE60),
    SenderAccents = listOf(
        Color(0xFFF2994A), Color(0xFF2D9CDB), Color(0xFF9B51E0),
        Color(0xFF27AE60), Color(0xFFEB5757), Color(0xFF2F80ED),
    ),
)

val DarkPalette = Palette(
    Blue = Color(0xFF3D8BF2),
    TextPrimary = Color(0xFFF2F2F8),
    TextSecondary = Color(0xFF9C9CB2),
    Navy = Color(0xFFE4E4F4),
    TimeText = Color(0xFFB0B0C0),
    MutedText = Color(0xFF8A8AA0),
    Placeholder = Color(0xFF3A3A44),
    IncomingBubble = Color(0xFF1E1E26),
    Surface = Color(0xFF101014),
    SurfaceSubtle = Color(0xFF17171D),
    Divider = Color(0xFF26262E),
    Online = Color(0xFF4CE417),
    Away = Color(0xFFF2C94C),
    Error = Color(0xFFFF6B6B),
    Success = Color(0xFF34C77B),
    SenderAccents = listOf(
        Color(0xFFF2994A), Color(0xFF56B4F5), Color(0xFFB47CF0),
        Color(0xFF43CD87), Color(0xFFFF7B7B), Color(0xFF5EA0F5),
    ),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** Theme-aware colors: `palette.Blue`, `palette.Surface`, … inside composables. */
val palette: Palette
    @Composable
    @ReadOnlyComposable
    get() = LocalPalette.current
