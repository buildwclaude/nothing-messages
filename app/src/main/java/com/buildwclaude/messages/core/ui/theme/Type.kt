@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.buildwclaude.messages.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.buildwclaude.messages.R

/**
 * Headings: Doto (SIL OFL) — a dot-matrix face that matches the Nothing OS
 * aesthetic. Body: Space Grotesk (SIL OFL) — geometric, techy, readable.
 * Both are bundled; nothing is fetched at runtime.
 */
val Doto = FontFamily(
    Font(
        R.font.doto,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
    Font(
        R.font.doto,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(900)),
    ),
)

val SpaceGrotesk = FontFamily(
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

object DesignType {
    // Dot-matrix display styles for screen titles / section headers.
    val pageTitle = TextStyle(fontFamily = Doto, fontWeight = FontWeight.Black, fontSize = 28.sp)
    val screenTitle = TextStyle(fontFamily = Doto, fontWeight = FontWeight.Bold, fontSize = 21.sp)

    // Readable UI text.
    val itemTitle = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val body = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 19.sp)
    val bodyLarge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val label = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val caption = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Normal, fontSize = 11.sp)
    val badge = TextStyle(fontFamily = SpaceGrotesk, fontWeight = FontWeight.Bold, fontSize = 10.sp)
}

val MessagesTypography = Typography(
    headlineMedium = DesignType.pageTitle,
    titleLarge = DesignType.screenTitle,
    titleMedium = DesignType.itemTitle,
    bodyLarge = DesignType.bodyLarge,
    bodyMedium = DesignType.body,
    bodySmall = DesignType.caption,
    labelLarge = DesignType.label,
    labelMedium = DesignType.label,
    labelSmall = DesignType.caption,
)
