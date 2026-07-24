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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.buildwclaude.messages.core.ui.theme.palette
import com.buildwclaude.messages.core.ui.theme.Inter
import com.buildwclaude.messages.domain.model.Recipient
import kotlin.math.absoluteValue

// iMessage-style gradient avatars: each contact gets a two-stop diagonal gradient
// with white initials on top.
private val AvatarGradients = listOf(
    Color(0xFF56CCF2) to Color(0xFF2F80ED), // blue
    Color(0xFFB06AB3) to Color(0xFF4568DC), // purple
    Color(0xFFF093FB) to Color(0xFFF5576C), // pink
    Color(0xFF43E97B) to Color(0xFF38B2A3), // green
    Color(0xFFFDC830) to Color(0xFFF37335), // orange
    Color(0xFF4FACFE) to Color(0xFF00C2FE), // cyan
    Color(0xFFFF758C) to Color(0xFFFF7EB3), // rose
    Color(0xFFA18CD1) to Color(0xFF6A5ACD), // violet
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
        val (start, end) = AvatarGradients[name.hashCode().absoluteValue % AvatarGradients.size]
        val brush = Brush.linearGradient(
            colors = listOf(start, end),
            start = Offset.Zero,
            end = Offset.Infinite,
        )
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier.size(size).clip(CircleShape).background(brush),
        ) {
            Text(
                text = initials(name),
                color = Color.White,
                fontFamily = Inter,
                fontWeight = FontWeight.SemiBold,
                fontSize = (size.value * 0.38f).sp,
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
            fontFamily = Inter,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
        )
    }
}
