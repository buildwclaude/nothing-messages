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
 * Inter (SIL OFL, bundled) — the open typeface closest to iOS's San Francisco,
 * for the clean iMessage-like look the owner asked for.
 */
val Inter = FontFamily(
    Font(
        R.font.inter,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.inter,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.inter,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.inter,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

object DesignType {
    val pageTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 26.sp)
    val screenTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
    val itemTitle = TextStyle(fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
    val body = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val bodyLarge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp)
    val label = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val caption = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Normal, fontSize = 11.sp)
    val badge = TextStyle(fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
