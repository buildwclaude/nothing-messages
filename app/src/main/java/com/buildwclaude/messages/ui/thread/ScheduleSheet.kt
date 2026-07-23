package com.buildwclaude.messages.ui.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.buildwclaude.messages.R
import com.buildwclaude.messages.core.ui.components.DateTimeWheel
import com.buildwclaude.messages.core.ui.theme.DesignType
import com.buildwclaude.messages.core.ui.theme.palette
import com.buildwclaude.messages.core.util.Formatters
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSheet(
    canScheduleExact: Boolean,
    hapticsEnabled: Boolean,
    onDismiss: () -> Unit,
    onSchedule: (Long) -> Unit,
) {
    val context = LocalContext.current
    var picked by remember {
        mutableLongStateOf(System.currentTimeMillis() + 60L * 60 * 1000)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.Surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Send later", style = DesignType.screenTitle, color = palette.TextPrimary)
            Spacer(Modifier.height(4.dp))
            if (!canScheduleExact) {
                Text(
                    "Exact alarms are off for this app — scheduled sends may be delayed a little. You can enable them in Settings.",
                    style = DesignType.body,
                    color = palette.Error,
                )
                Spacer(Modifier.height(8.dp))
            }

            PresetRow("In 1 hour", R.drawable.ic_clock) {
                onSchedule(System.currentTimeMillis() + 60L * 60 * 1000)
            }
            PresetRow("Tonight at 8:00 PM", R.drawable.ic_clock) { onSchedule(nextTime(20, 0)) }
            PresetRow("Tomorrow at 9:00 AM", R.drawable.ic_calendar) {
                onSchedule(
                    Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis,
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = palette.Divider)
            Spacer(Modifier.height(12.dp))

            Text("Pick a date & time", style = DesignType.itemTitle, color = palette.TextPrimary)
            Spacer(Modifier.height(8.dp))
            DateTimeWheel(
                hapticsEnabled = hapticsEnabled,
                onChange = { picked = it },
            )
            Spacer(Modifier.height(12.dp))

            val valid = picked > System.currentTimeMillis()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (valid) palette.Blue else palette.Placeholder)
                    .clickable(enabled = valid) { onSchedule(picked) }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_clock), null,
                    tint = Color.White, modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (valid) "Schedule for ${Formatters.fullDateTime(context, picked)}"
                    else "Pick a future time",
                    style = DesignType.itemTitle,
                    color = Color.White,
                )
            }
        }
    }
}

private fun nextTime(hour: Int, minute: Int): Long {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
    }
    if (cal.timeInMillis <= System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR, 1)
    return cal.timeInMillis
}

@Composable
private fun PresetRow(label: String, icon: Int, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
    ) {
        Icon(
            painterResource(icon), null,
            tint = palette.Blue,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(label, style = DesignType.bodyLarge, color = palette.TextPrimary)
    }
}
