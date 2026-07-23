package com.buildwclaude.messages.core.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Exact tokens from the "Messaging App UI" Figma community file (881015895655545375).
 */
object DesignColors {
    val Blue = Color(0xFF2F80ED)          // brand / outgoing bubbles / selection
    val TextPrimary = Color(0xFF1B1A57)   // titles, primary text
    val TextSecondary = Color(0xFF4F5E7B) // previews, secondary text, inactive icons
    val Navy = Color(0xFF21205A)          // status bar glyphs, dark accents
    val TimeText = Color(0xFF333333)      // timestamps in list rows
    val MutedText = Color(0xFFA1A1BC)     // in-bubble timestamps, "admin" labels
    val Placeholder = Color(0xFFC4C4C4)   // disabled send button, image placeholders
    val IncomingBubble = Color(0xFFF7F7F7)
    val Surface = Color(0xFFFFFFFF)
    val SurfaceSubtle = Color(0xFFF8F9FC)
    val Divider = Color(0xFFEDEDED)
    val Online = Color(0xFF4CE417)
    val Away = Color(0xFFF2C94C)
    val Error = Color(0xFFEB5757)
    val Success = Color(0xFF27AE60)
    val ShadowTint = Color(0x1A466087)    // #466087 at 10%

    // Per-sender name accents in group conversations, as used in the design.
    val SenderAccents = listOf(
        Color(0xFFF2994A), // orange
        Color(0xFF2D9CDB), // light blue
        Color(0xFF9B51E0), // purple
        Color(0xFF27AE60), // green
        Color(0xFFEB5757), // red
        Color(0xFF2F80ED), // blue
    )
}
