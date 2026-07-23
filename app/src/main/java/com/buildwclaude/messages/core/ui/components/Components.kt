package com.buildwclaude.messages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.buildwclaude.messages.core.ui.theme.palette
import com.buildwclaude.messages.core.ui.theme.SpaceGrotesk
import com.buildwclaude.messages.domain.model.Recipient
import kotlin.math.absoluteValue

private val AvatarPalette = listOf(
    Color(0xFF2F80ED), Color(0xFF9B51E0), Color(0xFF27AE60),
    Color(0xFFF2994A), Color(0xFF2D9CDB), Color(0xFFEB5757),
    Color(0xFF21205A),
)

@Composable
fun Avatar(recipient: Recipient?, size: Dp, modifier: Modifier = Modifier) {
    val name = recipient?.displayName ?: "?"
    if (recipient?.photoUri != null) {
        AsyncImage(
            model = recipient.photoUri,
            contentDescription = name,
            contentScale = ContentScale.Crop,
            modifier = modifier.size(size).clip(CircleShape),
        )
    } else {
        val color = AvatarPalette[name.hashCode().absoluteValue % AvatarPalette.size]
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.15f)),
        ) {
            Text(
                text = initials(name),
                color = color,
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = (size.value * 0.36f).sp,
            )
        }
    }
}

private fun initials(name: String): String {
    val parts = name.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "?"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> (parts[0].take(1) + parts[1].take(1)).uppercase()
    }.ifBlank { "#" }
}

@Composable
fun UnreadBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(24.dp).clip(CircleShape).background(palette.Blue),
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = Color.White,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
        )
    }
}
