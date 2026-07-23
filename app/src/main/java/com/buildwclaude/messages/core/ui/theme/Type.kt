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
 * The design uses "Plus Jakarta Display"; we bundle the open (SIL OFL) sibling
 * "Plus Jakarta Sans" as a variable font and pin the three weights the design uses.
 */
val PlusJakarta = FontFamily(
    Font(
        R.font.plus_jakarta,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400)),
    ),
    Font(
        R.font.plus_jakarta,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500)),
    ),
    Font(
        R.font.plus_jakarta,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600)),
    ),
    Font(
        R.font.plus_jakarta,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700)),
    ),
)

// Named styles matching the design's text scale.
object DesignType {
    val screenTitle = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Medium, fontSize = 18.sp)
    val pageTitle = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Medium, fontSize = 24.sp)
    val itemTitle = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    val body = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 18.sp)
    val bodyLarge = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp)
    val label = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Medium, fontSize = 12.sp)
    val caption = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Normal, fontSize = 10.sp)
    val badge = TextStyle(fontFamily = PlusJakarta, fontWeight = FontWeight.Bold, fontSize = 10.sp)
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
