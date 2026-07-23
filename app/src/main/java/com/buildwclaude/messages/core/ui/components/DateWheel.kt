package com.buildwclaude.messages.core.ui.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListLayoutInfo
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buildwclaude.messages.core.ui.theme.Inter
import com.buildwclaude.messages.core.ui.theme.palette
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import java.util.Calendar
import kotlin.math.abs

private val MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)

private const val ROW_HEIGHT_DP = 30
private const val VISIBLE_ROWS = 3

/**
 * iOS-style Day / Month / Year scroll dial. Day and Month loop endlessly
 * (scroll past Jan and Dec comes round again); picking a date filters the
 * conversation list to messages on or after it. Stays inert until first touched.
 */
@Composable
fun DateWheel(
    hapticsEnabled: Boolean,
    onCutoffChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val now = remember { Calendar.getInstance() }
    val currentYear = now.get(Calendar.YEAR)
    val minYear = currentYear - 10
    val years = remember { (minYear..currentYear).map { it.toString() } }
    val days = remember { (1..31).map { it.toString() } }

    var dayIdx by remember { mutableIntStateOf(now.get(Calendar.DAY_OF_MONTH) - 1) }
    var monthIdx by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
    var yearIdx by remember { mutableIntStateOf(years.lastIndex) }
    var touched by remember { mutableStateOf(false) }

    fun emit() {
        if (!touched) return
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, minYear + yearIdx)
            set(Calendar.MONTH, monthIdx)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, (dayIdx + 1).coerceAtMost(maxDay))
        }
        onCutoffChange(cal.timeInMillis)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height((ROW_HEIGHT_DP * VISIBLE_ROWS).dp),
    ) {
        // Centre highlight band.
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(ROW_HEIGHT_DP.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.IncomingBubble),
        )
        Row(Modifier.fillMaxWidth()) {
            WheelColumn(days, dayIdx, true, hapticsEnabled, { dayIdx = it; touched = true; emit() }, Modifier.weight(1f), 15)
            WheelColumn(MONTHS, monthIdx, true, hapticsEnabled, { monthIdx = it; touched = true; emit() }, Modifier.weight(1f), 15)
            WheelColumn(years, yearIdx, false, hapticsEnabled, { yearIdx = it; touched = true; emit() }, Modifier.weight(1f), 15)
        }
    }
}

/**
 * Full Day / Month / Year / Hour / Minute dial for the scheduler, 24-hour time.
 * Emits the selected instant on every change.
 */
@Composable
fun DateTimeWheel(
    hapticsEnabled: Boolean,
    onChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val start = remember { Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) } }
    val minYear = start.get(Calendar.YEAR)
    val years = remember { (minYear..minYear + 5).map { it.toString() } }
    val days = remember { (1..31).map { it.toString() } }
    val hours = remember { (0..23).map { it.toString().padStart(2, '0') } }
    val minutes = remember { (0..59).map { it.toString().padStart(2, '0') } }

    var dayIdx by remember { mutableIntStateOf(start.get(Calendar.DAY_OF_MONTH) - 1) }
    var monthIdx by remember { mutableIntStateOf(start.get(Calendar.MONTH)) }
    var yearIdx by remember { mutableIntStateOf(0) }
    var hourIdx by remember { mutableIntStateOf(start.get(Calendar.HOUR_OF_DAY)) }
    var minuteIdx by remember { mutableIntStateOf(start.get(Calendar.MINUTE)) }

    fun emit() {
        val cal = Calendar.getInstance().apply {
            clear()
            set(Calendar.YEAR, minYear + yearIdx)
            set(Calendar.MONTH, monthIdx)
            val maxDay = getActualMaximum(Calendar.DAY_OF_MONTH)
            set(Calendar.DAY_OF_MONTH, (dayIdx + 1).coerceAtMost(maxDay))
            set(Calendar.HOUR_OF_DAY, hourIdx)
            set(Calendar.MINUTE, minuteIdx)
        }
        onChange(cal.timeInMillis)
    }

    Box(
        modifier
            .fillMaxWidth()
            .height((ROW_HEIGHT_DP * VISIBLE_ROWS).dp),
    ) {
        Box(
            Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(ROW_HEIGHT_DP.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(palette.IncomingBubble),
        )
        Row(Modifier.fillMaxWidth()) {
            WheelColumn(days, dayIdx, true, hapticsEnabled, { dayIdx = it; emit() }, Modifier.weight(1f), 15)
            WheelColumn(MONTHS, monthIdx, true, hapticsEnabled, { monthIdx = it; emit() }, Modifier.weight(1f), 15)
            WheelColumn(years, yearIdx, false, hapticsEnabled, { yearIdx = it; emit() }, Modifier.weight(1f), 15)
            WheelColumn(hours, hourIdx, true, hapticsEnabled, { hourIdx = it; emit() }, Modifier.weight(1f), 15)
            WheelColumn(minutes, minuteIdx, true, hapticsEnabled, { minuteIdx = it; emit() }, Modifier.weight(1f), 15)
        }
    }
}

private fun LazyListLayoutInfo.centeredIndex(size: Int): Int? {
    if (visibleItemsInfo.isEmpty() || size == 0) return null
    val center = (viewportStartOffset + viewportEndOffset) / 2f
    val pos = visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2f - center) }?.index ?: return null
    return ((pos % size) + size) % size
}

@Composable
private fun WheelColumn(
    items: List<String>,
    initialIndex: Int,
    loop: Boolean,
    hapticsEnabled: Boolean,
    onCentered: (Int) -> Unit,
    modifier: Modifier = Modifier,
    fontSize: Int = 18,
) {
    val size = items.size
    // For looping columns we render a huge list and map by modulo, starting in
    // the middle so the user can scroll a long way in either direction.
    val bigCount = if (loop) size * 2000 else size
    val startPos = if (loop) size * 1000 + initialIndex else initialIndex
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startPos)
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val density = LocalDensity.current
    val view = LocalView.current
    val rowPx = with(density) { ROW_HEIGHT_DP.dp.toPx() }
    val sidePad = (ROW_HEIGHT_DP * ((VISIBLE_ROWS - 1) / 2)).dp

    // Haptic tick each time a new value rolls under the centre line.
    LaunchedEffect(listState, hapticsEnabled) {
        snapshotFlow { listState.layoutInfo.centeredIndex(size) }
            .distinctUntilChanged()
            .drop(1)
            .collect {
                if (hapticsEnabled) {
                    val c = if (Build.VERSION.SDK_INT >= 34) {
                        HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
                    } else {
                        HapticFeedbackConstants.CLOCK_TICK
                    }
                    view.performHapticFeedback(c)
                }
            }
    }

    // Report the settled value.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .drop(1)
            .collect { scrolling ->
                if (!scrolling) listState.layoutInfo.centeredIndex(size)?.let(onCentered)
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = sidePad),
        modifier = modifier.height((ROW_HEIGHT_DP * VISIBLE_ROWS).dp),
    ) {
        items(bigCount) { pos ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(ROW_HEIGHT_DP.dp)
                    .graphicsLayer {
                        val info = listState.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == pos }
                        val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                        val dist = item?.let { abs(it.offset + it.size / 2f - center) }
                            ?: (rowPx * VISIBLE_ROWS)
                        val norm = (dist / (rowPx * (VISIBLE_ROWS / 2f))).coerceIn(0f, 1f)
                        alpha = 1f - norm * 0.72f
                        val s = 1f - norm * 0.22f
                        scaleX = s
                        scaleY = s
                    },
            ) {
                Text(
                    text = items[((pos % size) + size) % size],
                    color = palette.TextPrimary,
                    textAlign = TextAlign.Center,
                    fontFamily = Inter,
                    fontSize = fontSize.sp,
                )
            }
        }
    }
}
