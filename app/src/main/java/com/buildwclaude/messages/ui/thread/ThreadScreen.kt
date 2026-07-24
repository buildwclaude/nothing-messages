package com.buildwclaude.messages.ui.thread

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.buildwclaude.messages.R
import com.buildwclaude.messages.core.ui.components.Avatar
import com.buildwclaude.messages.core.ui.theme.palette
import com.buildwclaude.messages.core.ui.theme.DesignType
import com.buildwclaude.messages.core.util.Formatters
import com.buildwclaude.messages.data.db.ScheduledMessageEntity
import com.buildwclaude.messages.domain.model.DeliveryState
import com.buildwclaude.messages.domain.model.Message
import com.buildwclaude.messages.domain.model.MmsPartData
import com.buildwclaude.messages.domain.model.PendingAttachment
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ThreadScreen(
    onBack: () -> Unit,
    onOpenScheduled: () -> Unit,
    viewModel: ThreadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pendingScheduled by viewModel.pendingScheduled.collectAsStateWithLifecycle()
    val draft by viewModel.draftText.collectAsState()
    val attachments by viewModel.attachments.collectAsState()
    val editingId by viewModel.editingScheduledId.collectAsState()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    var scheduleSheetOpen by remember { mutableStateOf(false) }
    var viewerPart by remember { mutableStateOf<MmsPartData?>(null) }

    LaunchedEffect(state.messages.size) {
        if (state.messages.isNotEmpty()) {
            listState.scrollToItem((state.messages.size + pendingScheduled.size).coerceAtLeast(1) - 1)
        }
        viewModel.markReadNow()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(palette.Surface)
            .statusBarsPadding()
            .imePadding(),
    ) {
        ThreadTopBar(state, onBack, viewModel, onOpenScheduled)
        HorizontalDivider(color = palette.Divider)

        Box(Modifier.weight(1f)) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
            ) {
                var lastDay = -1L
                val items = state.messages
                items.forEachIndexed { index, message ->
                    val day = message.date / 86_400_000L
                    if (day != lastDay) {
                        lastDay = day
                        item(key = "sep$day") { DateSeparator(message.date) }
                    }
                    item(key = "${if (message.isMms) "m" else "s"}${message.id}") {
                        MessageBubble(
                            message = message,
                            isGroup = state.isGroup,
                            onRetry = { viewModel.retry(message) },
                            onDelete = { viewModel.delete(message) },
                            onOpenPart = { viewerPart = it },
                            simLabel = if (state.sims.size > 1) {
                                state.sims.firstOrNull { it.subId == message.subId }?.displayName
                            } else null,
                        )
                    }
                }
                pendingScheduled.forEach { sched ->
                    item(key = "sched${sched.id}") {
                        ScheduledBubble(
                            entity = sched,
                            onEdit = { viewModel.editScheduled(sched.id, sched.body) },
                            onCancel = { viewModel.cancelScheduled(sched.id) },
                        )
                    }
                }
            }
        }

        if (editingId != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(palette.SurfaceSubtle)
                    .padding(horizontal = 16.dp, vertical = 6.dp),
            ) {
                Icon(
                    painterResource(R.drawable.ic_clock), null,
                    tint = palette.Blue, modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Editing scheduled message — send now or pick a new time",
                    style = DesignType.body, color = palette.TextSecondary,
                )
            }
        }

        Composer(
            draft = draft,
            onDraftChange = { viewModel.draftText.value = it },
            attachments = attachments,
            onRemoveAttachment = viewModel::removeAttachment,
            onAddAttachment = viewModel::addAttachment,
            segmentInfo = viewModel.segmentInfo(draft),
            sims = state.sims,
            selectedSubId = state.selectedSubId,
            onSelectSim = viewModel::selectSim,
            canSend = draft.isNotBlank() || attachments.isNotEmpty(),
            onSend = viewModel::send,
            onOpenSchedule = { scheduleSheetOpen = true },
        )
    }

    if (scheduleSheetOpen) {
        val haptics by viewModel.haptics.collectAsStateWithLifecycle()
        ScheduleSheet(
            canScheduleExact = state.canScheduleExact,
            hapticsEnabled = haptics,
            onDismiss = { scheduleSheetOpen = false },
            onSchedule = { time ->
                scheduleSheetOpen = false
                viewModel.scheduleSend(time)
            },
        )
    }

    viewerPart?.let { part ->
        Dialog(onDismissRequest = { viewerPart = null }) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black)
                    .padding(8.dp),
            ) {
                AsyncImage(
                    model = part.uri,
                    contentDescription = part.fileName,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row {
                    IconButton(onClick = {
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = part.mimeType
                            putExtra(Intent.EXTRA_STREAM, part.uri)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(share, null))
                    }) {
                        Icon(
                            painterResource(R.drawable.ic_corner_up_right),
                            "Share", tint = Color.White,
                        )
                    }
                    IconButton(onClick = { viewerPart = null }) {
                        Icon(painterResource(R.drawable.ic_x), "Close", tint = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadTopBar(
    state: ThreadUiState,
    onBack: () -> Unit,
    viewModel: ThreadViewModel,
    onOpenScheduled: () -> Unit,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.Surface)
            .padding(horizontal = 8.dp, vertical = 10.dp),
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painterResource(R.drawable.ic_chevron_left), "Back",
                tint = palette.TextSecondary,
            )
        }
        Avatar(state.recipients.firstOrNull(), size = 44.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                state.title,
                style = DesignType.screenTitle,
                color = palette.TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (state.isGroup) "${state.recipients.size} people"
                else state.recipients.firstOrNull()?.address ?: "",
                style = DesignType.body,
                color = palette.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!state.isGroup && state.recipients.isNotEmpty()) {
            IconButton(onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Intent.ACTION_DIAL, Uri.parse("tel:${state.recipients[0].address}")),
                    )
                }
            }) {
                Icon(
                    painterResource(R.drawable.ic_phone), "Call",
                    tint = palette.TextSecondary, modifier = Modifier.size(20.dp),
                )
            }
        }
        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    painterResource(R.drawable.ic_more_vertical), "More",
                    tint = palette.TextSecondary,
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (state.muted) "Unmute" else "Mute") },
                    onClick = { viewModel.toggleMute(); menuOpen = false },
                )
                DropdownMenuItem(
                    text = { Text("Scheduled messages") },
                    onClick = { onOpenScheduled(); menuOpen = false },
                )
            }
        }
    }
}

@Composable
private fun DateSeparator(millis: Long) {
    Row(
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        Text(
            Formatters.dateSeparator(millis),
            fontSize = 10.sp,
            color = palette.MutedText,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: Message,
    isGroup: Boolean,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onOpenPart: (MmsPartData) -> Unit,
    simLabel: String?,
) {
    val context = LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val incoming = message.isIncoming
    val bubbleColor = if (incoming) palette.IncomingBubble else palette.Blue
    val textColor = if (incoming) palette.TextPrimary else Color.White
    val timeColor = if (incoming) palette.MutedText else Color.White.copy(alpha = 0.7f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = if (incoming) Arrangement.Start else Arrangement.End,
    ) {
        Box {
            Column(
                horizontalAlignment = if (incoming) Alignment.Start else Alignment.End,
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(bubbleColor)
                        .combinedClickable(onClick = {}, onLongClick = { menuOpen = true })
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                ) {
                    if (incoming && isGroup && message.address != null) {
                        Text(
                            message.address,
                            style = DesignType.label,
                            color = palette.SenderAccents[
                                message.address.hashCode().absoluteValue % palette.SenderAccents.size,
                            ],
                        )
                        Spacer(Modifier.height(4.dp))
                    }
                    message.mediaParts.forEach { part ->
                        if (part.mimeType.startsWith("image/")) {
                            AsyncImage(
                                model = part.uri,
                                contentDescription = part.fileName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { onOpenPart(part) },
                            )
                            Spacer(Modifier.height(6.dp))
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color.Black.copy(alpha = 0.08f))
                                    .clickable {
                                        runCatching {
                                            context.startActivity(
                                                Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(part.uri, part.mimeType)
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                },
                                            )
                                        }
                                    }
                                    .padding(8.dp),
                            ) {
                                Icon(
                                    painterResource(
                                        when {
                                            part.mimeType.startsWith("video/") -> R.drawable.ic_play
                                            part.mimeType.startsWith("audio/") -> R.drawable.ic_mic
                                            else -> R.drawable.ic_file_text
                                        },
                                    ),
                                    null, tint = textColor, modifier = Modifier.size(18.dp),
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    part.fileName ?: part.mimeType,
                                    style = DesignType.body, color = textColor, maxLines = 1,
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                    if (!message.body.isNullOrBlank()) {
                        Text(message.body, style = DesignType.body, color = textColor)
                    }
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            Formatters.timeOfDay(context, message.date) +
                                (simLabel?.let { " · $it" } ?: ""),
                            fontSize = 9.sp,
                            color = timeColor,
                        )
                        if (!incoming) {
                            val (statusIcon, statusWord) = when (message.delivery) {
                                DeliveryState.SENDING -> R.drawable.ic_clock to "Sending…"
                                DeliveryState.SENT -> R.drawable.ic_check to "Sent"
                                DeliveryState.DELIVERED -> R.drawable.ic_check_circle to "Delivered"
                                DeliveryState.FAILED -> R.drawable.ic_alert_circle to "Not sent"
                                else -> null to null
                            }
                            if (statusIcon != null) {
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    painterResource(statusIcon), statusWord,
                                    tint = timeColor, modifier = Modifier.size(10.dp),
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(statusWord ?: "", fontSize = 9.sp, color = timeColor)
                            }
                        }
                    }
                }
                if (message.delivery == DeliveryState.FAILED) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable(onClick = onRetry),
                    ) {
                        Icon(
                            painterResource(R.drawable.ic_refresh_cw), null,
                            tint = palette.Error, modifier = Modifier.size(12.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Not sent. Tap to retry",
                            fontSize = 10.sp,
                            color = palette.Error,
                        )
                    }
                }
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(text = { Text("Copy") }, onClick = {
                    val cm = context.getSystemService(android.content.ClipboardManager::class.java)
                    cm.setPrimaryClip(
                        android.content.ClipData.newPlainText("message", message.body ?: ""),
                    )
                    menuOpen = false
                })
                DropdownMenuItem(text = { Text("Forward") }, onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message.body ?: "")
                    }
                    runCatching { context.startActivity(Intent.createChooser(send, "Forward")) }
                    menuOpen = false
                })
                DropdownMenuItem(text = { Text("Share") }, onClick = {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, message.body ?: "")
                    }
                    runCatching { context.startActivity(Intent.createChooser(send, null)) }
                    menuOpen = false
                })
                DropdownMenuItem(
                    text = { Text("Delete", color = palette.Error) },
                    onClick = { onDelete(); menuOpen = false },
                )
            }
        }
    }
}

@Composable
private fun ScheduledBubble(
    entity: ScheduledMessageEntity,
    onEdit: () -> Unit,
    onCancel: () -> Unit,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Column(
                modifier = Modifier
                    .widthIn(max = 280.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .border(1.dp, palette.Blue, RoundedCornerShape(18.dp))
                    .background(palette.Surface)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painterResource(R.drawable.ic_clock), null,
                        tint = palette.Blue, modifier = Modifier.size(12.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Scheduled · " + Formatters.fullDateTime(context, entity.sendAt),
                        fontSize = 10.sp,
                        color = palette.Blue,
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(entity.body, style = DesignType.body, color = palette.TextPrimary)
            }
            Row {
                Text(
                    "Edit",
                    style = DesignType.label,
                    color = palette.Blue,
                    modifier = Modifier.clickable(onClick = onEdit).padding(6.dp),
                )
                Text(
                    "Cancel",
                    style = DesignType.label,
                    color = palette.Error,
                    modifier = Modifier.clickable(onClick = onCancel).padding(6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    attachments: List<PendingAttachment>,
    onRemoveAttachment: (PendingAttachment) -> Unit,
    onAddAttachment: (PendingAttachment) -> Unit,
    segmentInfo: Pair<Int, Int>?,
    sims: List<com.buildwclaude.messages.domain.model.SimInfo>,
    selectedSubId: Int,
    onSelectSim: (Int) -> Unit,
    canSend: Boolean,
    onSend: () -> Unit,
    onOpenSchedule: () -> Unit,
) {
    val context = LocalContext.current
    val pickMedia = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
            onAddAttachment(PendingAttachment(uri, mime))
        }
    }

    Column(Modifier.background(palette.Surface).navigationBarsPadding()) {
        HorizontalDivider(color = palette.Divider)

        if (attachments.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                attachments.forEach { att ->
                    Box {
                        if (att.mimeType.startsWith("image/")) {
                            AsyncImage(
                                model = att.uri,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                            )
                        } else {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(palette.IncomingBubble),
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_file_text), null,
                                    tint = palette.TextSecondary,
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(palette.Error)
                                .clickable { onRemoveAttachment(att) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_x), "Remove",
                                tint = Color.White, modifier = Modifier.size(10.dp),
                            )
                        }
                    }
                }
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
        ) {
            IconButton(onClick = { pickMedia.launch(arrayOf("image/*", "video/*", "audio/*", "text/x-vcard")) }) {
                Icon(
                    painterResource(R.drawable.ic_paperclip), "Attach",
                    tint = palette.TextPrimary, modifier = Modifier.size(22.dp),
                )
            }
            TextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = {
                    Text("Write a message...", style = DesignType.body, color = palette.TextSecondary)
                },
                textStyle = DesignType.bodyLarge.copy(color = palette.TextPrimary),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                ),
                maxLines = 5,
                modifier = Modifier.weight(1f),
            )
            if (sims.size > 1) {
                val current = sims.firstOrNull { it.subId == selectedSubId } ?: sims[0]
                Text(
                    "SIM ${current.slotIndex + 1}",
                    style = DesignType.label,
                    color = palette.Blue,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            val idx = sims.indexOfFirst { it.subId == selectedSubId }
                            onSelectSim(sims[(idx + 1).mod(sims.size)].subId)
                        }
                        .padding(6.dp),
                )
            }
            IconButton(onClick = onOpenSchedule, modifier = Modifier.size(36.dp)) {
                Icon(
                    painterResource(R.drawable.ic_clock), "Schedule",
                    tint = palette.TextSecondary, modifier = Modifier.size(20.dp),
                )
            }
            Spacer(Modifier.width(4.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (canSend) palette.Blue else palette.Placeholder)
                    .combinedClickable(
                        onClick = { if (canSend) onSend() },
                        onLongClick = { if (canSend) onOpenSchedule() },
                    ),
            ) {
                Icon(
                    painterResource(R.drawable.ic_send), "Send",
                    tint = Color.White, modifier = Modifier.size(18.dp),
                )
            }
        }

        if (segmentInfo != null) {
            Text(
                "${segmentInfo.second} · ${segmentInfo.first} SMS",
                fontSize = 10.sp,
                color = palette.MutedText,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = 20.dp, bottom = 4.dp),
            )
        }
    }
}
