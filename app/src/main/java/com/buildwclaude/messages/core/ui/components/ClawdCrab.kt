package com.buildwclaude.messages.core.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.dp
import kotlin.math.sin

/**
 * Clawd, the little orange crab — scuttles back and forth, waves a claw.
 * Drawn and animated entirely in code; nothing loaded from anywhere.
 */
@Composable
fun ClawdCrab(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "clawd")

    // Walk across and back over ~7 seconds.
    val walk by transition.animateFloat(
        initialValue = 0f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing)),
        label = "walk",
    )
    // Leg scuttle + body bob.
    val scuttle by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(400, easing = LinearEasing)),
        label = "scuttle",
    )
    // Claw wave.
    val wave by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(1600, easing = LinearEasing), RepeatMode.Restart),
        label = "wave",
    )

    val shell = Color(0xFFE8734A)      // Clawd orange
    val shellDark = Color(0xFFD05A32)
    val eyeWhite = Color.White
    val eyeDark = Color(0xFF20201E)

    Canvas(modifier.fillMaxWidth().height(56.dp)) {
        val w = size.width
        val h = size.height
        // Position 0→1→0 across the strip, with margins.
        val t = if (walk <= 1f) walk else 2f - walk
        val facingRight = walk <= 1f
        val cx = 40.dp.toPx() + t * (w - 80.dp.toPx())
        val cy = h * 0.62f + sin(scuttle) * 1.5f

        translate(cx, cy) {
            scale(if (facingRight) 1f else -1f, 1f, pivot = Offset.Zero) {
                val bodyW = 34.dp.toPx()
                val bodyH = 22.dp.toPx()

                // Legs: three per side, scuttling.
                val legStroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                for (i in 0..2) {
                    val phase = sin(scuttle + i * 1.3f) * 3.dp.toPx()
                    val lx = -bodyW / 2 + 4.dp.toPx() + i * 10.dp.toPx()
                    drawLine(
                        shellDark,
                        Offset(lx, bodyH * 0.2f),
                        Offset(lx - 4.dp.toPx() + phase * 0.4f, bodyH * 0.75f + phase.coerceAtLeast(0f)),
                        strokeWidth = legStroke.width,
                        cap = StrokeCap.Round,
                    )
                }

                // Body shell.
                drawOval(
                    shell,
                    topLeft = Offset(-bodyW / 2, -bodyH / 2),
                    size = Size(bodyW, bodyH),
                )
                // Shell highlight band.
                drawOval(
                    shellDark.copy(alpha = 0.35f),
                    topLeft = Offset(-bodyW / 2 + 3.dp.toPx(), -bodyH / 2 + bodyH * 0.55f),
                    size = Size(bodyW - 6.dp.toPx(), bodyH * 0.4f),
                )

                // Eye stalks + googly eyes.
                for (side in listOf(-1f, 1f)) {
                    val ex = side * 6.dp.toPx()
                    drawLine(
                        shellDark,
                        Offset(ex, -bodyH / 2 + 2.dp.toPx()),
                        Offset(ex, -bodyH / 2 - 6.dp.toPx()),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                    drawCircle(eyeWhite, 4.dp.toPx(), Offset(ex, -bodyH / 2 - 8.dp.toPx()))
                    drawCircle(eyeDark, 1.8.dp.toPx(), Offset(ex + 1.dp.toPx(), -bodyH / 2 - 8.dp.toPx()))
                }

                // Fixed claw (front-low).
                drawCircle(shell, 6.dp.toPx(), Offset(bodyW / 2 + 5.dp.toPx(), 2.dp.toPx()))
                // Waving claw (front-high) — bobs up and down.
                val waveLift = (sin(wave).coerceAtLeast(0f)) * 7.dp.toPx()
                drawCircle(
                    shell, 6.5.dp.toPx(),
                    Offset(bodyW / 2 + 7.dp.toPx(), -bodyH / 2 - 2.dp.toPx() - waveLift),
                )
                // Claw notch.
                drawLine(
                    shellDark,
                    Offset(bodyW / 2 + 7.dp.toPx(), -bodyH / 2 - 6.dp.toPx() - waveLift),
                    Offset(bodyW / 2 + 10.dp.toPx(), -bodyH / 2 - 2.dp.toPx() - waveLift),
                    strokeWidth = 1.8.dp.toPx(),
                    cap = StrokeCap.Round,
                )

                // Little smile.
                drawArc(
                    color = eyeDark,
                    startAngle = 20f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(-4.dp.toPx(), -4.dp.toPx()),
                    size = Size(8.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(1.5.dp.toPx(), cap = StrokeCap.Round),
                )
            }
        }
    }
}
