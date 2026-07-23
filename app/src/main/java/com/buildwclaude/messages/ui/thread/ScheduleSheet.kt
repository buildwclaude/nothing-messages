package com.buildwclaude.messages.ui.thread

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.buildwclaude.messages.R
import com.buildwclaude.messages.core.ui.theme.DesignColors
import com.buildwclaude.messages.core.ui.theme.DesignType
import com.buildwclaude.messages.core.util.Formatters
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleSheet(
    canScheduleExact: Boolean,
    onDismiss: () -> Unit,
    onSchedule: (Long) -> Unit,
) {
    val context = LocalContext.current
    var pickDateOpen by remember { mutableStateOf(false) }
    var pickTimeOpen by remember { mutableStateOf(false) }
    var pickedDateMillis by remember { mutableStateOf<Long?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DesignColors.Surface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
    ) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Text("Send later", style = DesignType.screenTitle, color = DesignColors.TextPrimary)
            Spacer(Modifier.height(4.dp))
            if (!canScheduleExact) {
                Text(
                    "Exact alarms are off for this app — scheduled sends may be delayed a little. You can enable them in Settings.",
                    style = DesignType.body,
                    color = DesignColors.Error,
                )
                Spacer(Modifier.height(8.dp))
            }
            PresetRow("In 1 hour", R.drawable.ic_clock) {
                onSchedule(System.currentTimeMillis() + 60L * 60 * 1000)
            }
            PresetRow("Tonight at 8:00 PM", R.drawable.ic_clock) {
                onSchedule(nextTime(20, 0))
            }
            PresetRow("Tomorrow at 9:00 AM", R.drawable.ic_calendar) {
                val cal = Calendar.getInstance().apply {
                    add(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }
                onSchedule(cal.timeInMillis)
            }
            PresetRow("Pick date & time", R.drawable.ic_calendar) { pickDateOpen = true }
        }
    }

    if (pickDateOpen) {
        val dateState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDateOpen = false },
            confirmButton = {
                TextButton(onClick = {
                    pickedDateMillis = dateState.selectedDateMillis
                    pickDateOpen = false
                    pickTimeOpen = true
                }) { Text("Next") }
            },
            dismissButton = {
                TextButton(onClick = { pickDateOpen = false }) { Text("Cancel") }
            },
        ) {
            DatePicker(state = dateState)
        }
    }

    if (pickTimeOpen) {
        val timeState = rememberTimePickerState()
        Dialog(onDismissRequest = { pickTimeOpen = false }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .background(DesignColors.Surface, RoundedCornerShape(24.dp))
                    .padding(24.dp),
            ) {
                TimePicker(state = timeState)
                Row {
                    TextButton(onClick = { pickTimeOpen = false }) { Text("Cancel") }
                    TextButton(onClick = {
                        pickTimeOpen = false
                        // Interpret the picked calendar date + time in the local timezone.
                        val dateBase = pickedDateMillis ?: System.currentTimeMillis()
                        val utc = Calendar.getInstance(java.util.TimeZone.getTimeZone("UTC"))
                            .apply { timeInMillis = dateBase }
                        val cal = Calendar.getInstance().apply {
                            set(
                                utc.get(Calendar.YEAR), utc.get(Calendar.MONTH),
                                utc.get(Calendar.DAY_OF_MONTH),
                                timeState.hour, timeState.minute, 0,
                            )
                            set(Calendar.MILLISECOND, 0)
                        }
                        val time = cal.timeInMillis
                        if (time > System.currentTimeMillis()) onSchedule(time)
                    }) { Text("Schedule") }
                }
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
            .padding(vertical = 14.dp),
    ) {
        Icon(
            painterResource(icon), null,
            tint = DesignColors.Blue,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(16.dp))
        Text(label, style = DesignType.bodyLarge, color = DesignColors.TextPrimary)
    }
}
