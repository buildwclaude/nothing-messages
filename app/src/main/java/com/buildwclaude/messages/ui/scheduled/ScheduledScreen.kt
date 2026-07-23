package com.buildwclaude.messages.ui.scheduled

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.buildwclaude.messages.R
import com.buildwclaude.messages.core.ui.theme.palette
import com.buildwclaude.messages.core.ui.theme.DesignType
import com.buildwclaude.messages.core.util.Formatters
import com.buildwclaude.messages.data.db.ScheduledMessageDao
import com.buildwclaude.messages.data.db.ScheduledStatus
import com.buildwclaude.messages.scheduled.ScheduledSendManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduledViewModel @Inject constructor(
    dao: ScheduledMessageDao,
    private val manager: ScheduledSendManager,
) : ViewModel() {
    val items = dao.observeActive()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun cancel(id: Long) = viewModelScope.launch { manager.cancel(id) }
    fun sendNow(id: Long) = viewModelScope.launch { manager.fire(id) }
}

@Composable
fun ScheduledScreen(
    onBack: () -> Unit,
    onOpenThread: (Long) -> Unit,
    viewModel: ScheduledViewModel = hiltViewModel(),
) {
    val items by viewModel.items.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.Surface)
            .statusBarsPadding(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    painterResource(R.drawable.ic_chevron_left), "Back",
                    tint = palette.TextSecondary,
                )
            }
            Text("Scheduled", style = DesignType.screenTitle, color = palette.TextPrimary)
        }

        if (items.isEmpty()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_clock), null,
                    tint = palette.Placeholder, modifier = Modifier.size(48.dp),
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Nothing scheduled",
                    style = DesignType.bodyLarge, color = palette.TextSecondary,
                )
                Text(
                    "Long-press the send button in any chat to schedule a message.",
                    style = DesignType.body, color = palette.MutedText,
                )
            }
        }

        LazyColumn {
            items(items, key = { it.id }) { entity ->
                val failed = entity.status == ScheduledStatus.FAILED
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(palette.IncomingBubble)
                        .clickable { entity.threadId?.let(onOpenThread) }
                        .padding(12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(if (failed) R.drawable.ic_alert_circle else R.drawable.ic_clock),
                            null,
                            tint = if (failed) palette.Error else palette.Blue,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (failed) "Failed — ${entity.lastError ?: "could not send"}"
                            else Formatters.fullDateTime(context, entity.sendAt),
                            style = DesignType.label,
                            color = if (failed) palette.Error else palette.Blue,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "To: " + entity.recipients.replace("|", ", "),
                        style = DesignType.body,
                        color = palette.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        entity.body,
                        style = DesignType.bodyLarge,
                        color = palette.TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row {
                        Text(
                            "Send now",
                            style = DesignType.label,
                            color = palette.Blue,
                            modifier = Modifier
                                .clickable { viewModel.sendNow(entity.id) }
                                .padding(end = 20.dp),
                        )
                        Text(
                            "Cancel",
                            style = DesignType.label,
                            color = palette.Error,
                            modifier = Modifier.clickable { viewModel.cancel(entity.id) },
                        )
                    }
                }
            }
        }
    }
}
