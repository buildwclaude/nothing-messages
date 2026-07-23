package com.buildwclaude.messages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.buildwclaude.messages.core.ui.theme.DesignType
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

/**
 * iOS-style scroll dial with Day / Month / Year columns. The row that lands in
 * the centre band is the selection. Picking a date filters the conversation list
 * to messages on or after that date ("since"). Until the user moves the dial it
 * stays inert and everything is shown.
 */
@Composable
fun DateWheel(
    onCutoffChange: (Long) -> Unit,
    modifier: Modifier = Modifier,
    rowHeight: Dp = 26.dp,
    visibleRows: Int = 5,
) {
    val now = remember { Calendar.getInstance() }
    val currentYear = now.get(Calendar.YEAR)
    val minYear = currentYear - 10
    val years = remember { (minYear..currentYear).map { it.toString() } }
    val days = remember { (1..31).map { it.toString() } }

    var dayIdx by remember { mutableIntStateOf(now.get(Calendar.DAY_OF_MONTH) - 1) }
    var monthIdx by remember { mutableIntStateOf(now.get(Calendar.MONTH)) }
    var yearIdx by remember { mutableIntStateOf(currentYear - minYear) }

    // Stay inert until the user actually scrolls the dial (so all chats show at first).
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

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 4.dp),
    ) {
        Text(
            "Since",
            style = DesignType.label,
            color = palette.MutedText,
            modifier = Modifier.width(48.dp),
        )
        Box(Modifier.weight(1f).height(rowHeight * visibleRows)) {
            // Centre highlight band.
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(rowHeight)
                    .clip(RoundedCornerShape(10.dp))
                    .background(palette.IncomingBubble),
            )
            Row(Modifier.fillMaxWidth()) {
                WheelColumn(
                    items = days,
                    initialIndex = dayIdx,
                    onCentered = { dayIdx = it; touched = true; emit() },
                    rowHeight = rowHeight,
                    visibleRows = visibleRows,
                    modifier = Modifier.weight(1f),
                )
                WheelColumn(
                    items = MONTHS,
                    initialIndex = monthIdx,
                    onCentered = { monthIdx = it; touched = true; emit() },
                    rowHeight = rowHeight,
                    visibleRows = visibleRows,
                    modifier = Modifier.weight(1.2f),
                )
                WheelColumn(
                    items = years,
                    initialIndex = yearIdx,
                    onCentered = { yearIdx = it; touched = true; emit() },
                    rowHeight = rowHeight,
                    visibleRows = visibleRows,
                    modifier = Modifier.weight(1.3f),
                )
            }
        }
    }
}

@Composable
private fun WheelColumn(
    items: List<String>,
    initialIndex: Int,
    onCentered: (Int) -> Unit,
    rowHeight: Dp,
    visibleRows: Int,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val fling = rememberSnapFlingBehavior(lazyListState = listState)
    val density = LocalDensity.current
    val rowPx = with(density) { rowHeight.toPx() }
    val sidePad = rowHeight * ((visibleRows - 1) / 2)

    // Report the centred item once the dial settles.
    androidx.compose.runtime.LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .drop(1) // ignore the initial "not scrolling" state so the dial stays inert
            .collect { scrolling ->
                if (!scrolling) {
                    val info = listState.layoutInfo
                    val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                    val idx = info.visibleItemsInfo.minByOrNull {
                        abs(it.offset + it.size / 2f - center)
                    }?.index
                    if (idx != null) onCentered(idx)
                }
            }
    }

    LazyColumn(
        state = listState,
        flingBehavior = fling,
        contentPadding = PaddingValues(vertical = sidePad),
        modifier = modifier.height(rowHeight * visibleRows),
    ) {
        itemsIndexed(items) { i, label ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(rowHeight)
                    .graphicsLayer {
                        val info = listState.layoutInfo
                        val item = info.visibleItemsInfo.firstOrNull { it.index == i }
                        val center = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                        val dist = item?.let { abs(it.offset + it.size / 2f - center) }
                            ?: (rowPx * visibleRows)
                        val norm = (dist / (rowPx * (visibleRows / 2f))).coerceIn(0f, 1f)
                        alpha = 1f - norm * 0.75f
                        val s = 1f - norm * 0.25f
                        scaleX = s
                        scaleY = s
                    },
            ) {
                Text(
                    text = label,
                    color = palette.TextPrimary,
                    textAlign = TextAlign.Center,
                    fontFamily = Inter,
                    fontSize = 18.sp,
                )
            }
        }
    }
}
