package com.buildwclaude.messages.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.buildwclaude.messages.core.ui.theme.DesignType
import com.buildwclaude.messages.core.ui.theme.palette
import kotlinx.coroutines.flow.distinctUntilChanged
import java.util.Calendar

enum class TimeWindow(val label: String) {
    ALL("All time"),
    TODAY("Today"),
    WEEK("7 days"),
    MONTH("30 days"),
    THREE_MONTHS("3 months"),
    SIX_MONTHS("6 months"),
    YEAR("This year"),
    ;

    /** Epoch-millis cutoff; conversations older than this are hidden. */
    fun cutoff(): Long = when (this) {
        ALL -> 0L
        TODAY -> Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        WEEK -> System.currentTimeMillis() - 7L * 86_400_000
        MONTH -> System.currentTimeMillis() - 30L * 86_400_000
        THREE_MONTHS -> System.currentTimeMillis() - 90L * 86_400_000
        SIX_MONTHS -> System.currentTimeMillis() - 180L * 86_400_000
        YEAR -> Calendar.getInstance().apply {
            set(Calendar.DAY_OF_YEAR, 1); set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}

/**
 * A snapping horizontal wheel of time windows (photoswheel-style): the item that
 * lands in the center becomes the active filter.
 */
@Composable
fun TimeWheel(
    selected: TimeWindow,
    onSelect: (TimeWindow) -> Unit,
    modifier: Modifier = Modifier,
) {
    val windows = remember { TimeWindow.entries }
    val listState = rememberLazyListState()
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val sidePadding = (screenWidth - 96.dp) / 2

    // When the wheel settles, select the centered item.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .distinctUntilChanged()
            .collect { scrolling ->
                if (!scrolling) {
                    val layout = listState.layoutInfo
                    val center = (layout.viewportStartOffset + layout.viewportEndOffset) / 2
                    val centered = layout.visibleItemsInfo.minByOrNull {
                        kotlin.math.abs((it.offset + it.size / 2) - center)
                    }
                    centered?.let {
                        val w = windows[it.index]
                        if (w != selected) onSelect(w)
                    }
                }
            }
    }

    Box(modifier.fillMaxWidth()) {
        LazyRow(
            state = listState,
            flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
            contentPadding = PaddingValues(horizontal = sidePadding, vertical = 6.dp),
        ) {
            itemsIndexed(windows) { index, window ->
                val active = window == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(100.dp))
                        .background(if (active) palette.Blue else palette.IncomingBubble)
                        .clickable { onSelect(window) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                ) {
                    Text(
                        window.label,
                        style = DesignType.label,
                        color = if (active) Color.White else palette.TextSecondary,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}
