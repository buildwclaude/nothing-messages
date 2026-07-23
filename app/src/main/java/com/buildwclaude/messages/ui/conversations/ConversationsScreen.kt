package com.buildwclaude.messages.ui.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buildwclaude.messages.R
import com.buildwclaude.messages.core.ui.components.Avatar
import com.buildwclaude.messages.core.ui.components.UnreadBadge
import com.buildwclaude.messages.core.ui.theme.palette
import com.buildwclaude.messages.core.ui.theme.DesignType
import com.buildwclaude.messages.core.util.Formatters
import com.buildwclaude.messages.domain.model.Conversation

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationsScreen(
    onOpenThread: (Long, String?) -> Unit,
    onNewMessage: () -> Unit,
    onOpenScheduled: () -> Unit,
    onOpenSettings: () -> Unit,
    onRequestDefaultRole: () -> Unit,
    viewModel: ConversationsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var searchOpen by remember { mutableStateOf(false) }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        containerColor = palette.Surface,
        bottomBar = {
            BottomBar(
                onHome = {},
                onScheduled = onOpenScheduled,
                onSettings = onOpenSettings,
            )
        },
        floatingActionButton = {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(palette.Blue)
                    .clickable(onClick = onNewMessage),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_plus),
                    contentDescription = "New message",
                    tint = Color.White,
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding(),
        ) {
            if (!state.isDefaultSmsApp) {
                DefaultAppBanner(onRequestDefaultRole)
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    text = if (searchOpen) "Search" else "Recent Chats",
                    style = DesignType.screenTitle,
                    color = palette.TextPrimary,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = {
                    searchOpen = !searchOpen
                    if (!searchOpen) viewModel.setSearch("")
                }) {
                    Icon(
                        painterResource(if (searchOpen) R.drawable.ic_x else R.drawable.ic_search),
                        contentDescription = "Search",
                        tint = palette.TextSecondary,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            if (searchOpen) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearch,
                    placeholder = { Text("Search messages and contacts", style = DesignType.body) },
                    textStyle = DesignType.bodyLarge,
                    singleLine = true,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                )
            } else {
                FilterChips(current = state.filter, onSelect = viewModel::setFilter)
            }

            LazyColumn(modifier = Modifier.fillMaxSize()) {
                if (state.pinned.isNotEmpty()) {
                    item {
                        Text(
                            "Pinned Chats",
                            style = DesignType.screenTitle,
                            color = palette.TextPrimary,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    item {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                        ) {
                            items(state.pinned, key = { it.threadId }) { c ->
                                PinnedCard(c) { onOpenThread(c.threadId, null) }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(12.dp)) }
                }
                items(state.conversations, key = { it.threadId }) { c ->
                    ConversationRow(
                        conversation = c,
                        onClick = { onOpenThread(c.threadId, null) },
                        onArchive = { viewModel.toggleArchive(c) },
                        viewModel = viewModel,
                    )
                }
                if (!state.loading && state.conversations.isEmpty() && state.pinned.isEmpty()) {
                    item {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(top = 96.dp),
                        ) {
                            Icon(
                                painterResource(R.drawable.ic_message_circle),
                                contentDescription = null,
                                tint = palette.Placeholder,
                                modifier = Modifier.size(48.dp),
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                when {
                                    state.searchQuery.isNotBlank() -> "No results"
                                    state.filter == ChatFilter.ARCHIVED -> "No archived chats"
                                    else -> "No conversations yet"
                                },
                                style = DesignType.bodyLarge,
                                color = palette.TextSecondary,
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    // Re-check role + reload conversations every time the screen returns to the
    // foreground (e.g. right after the user grants the default-SMS role in Settings).
    androidx.lifecycle.compose.LifecycleResumeEffect(Unit) {
        viewModel.refreshRole()
        onPauseOrDispose { }
    }
}

@Composable
private fun DefaultAppBanner(onRequest: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(palette.Blue)
            .clickable(onClick = onRequest)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Icon(
            painterResource(R.drawable.ic_alert_circle),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text("Not your default SMS app", style = DesignType.itemTitle, color = Color.White)
            Text(
                "Reading only. Tap to make Messages your default app.",
                style = DesignType.body,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

@Composable
private fun FilterChips(current: ChatFilter, onSelect: (ChatFilter) -> Unit) {
    val labels = listOf(
        ChatFilter.ALL to "All chats",
        ChatFilter.UNREAD to "Unread",
        ChatFilter.GROUPS to "Groups",
        ChatFilter.ARCHIVED to "Archived",
    )
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
        modifier = Modifier.padding(bottom = 8.dp),
    ) {
        items(labels) { (filter, label) ->
            val selected = filter == current
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (selected) palette.Blue else palette.Surface)
                    .clickable { onSelect(filter) }
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    label,
                    style = DesignType.body,
                    color = if (selected) Color.White else palette.TextPrimary,
                )
            }
        }
    }
}

@Composable
private fun PinnedCard(c: Conversation, onClick: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(
        modifier = Modifier
            .width(167.dp)
            .height(102.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(palette.IncomingBubble)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Avatar(c.recipients.firstOrNull(), size = 36.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                c.title,
                style = DesignType.itemTitle,
                color = palette.TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (c.unreadCount > 0) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(palette.Blue))
            }
        }
        Spacer(Modifier.weight(1f))
        Text(
            c.snippet ?: "",
            style = DesignType.body,
            color = palette.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ConversationRow(
    conversation: Conversation,
    onClick: () -> Unit,
    onArchive: () -> Unit,
    viewModel: ConversationsViewModel,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var menuOpen by remember { mutableStateOf(false) }
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            when (value) {
                // Swipe right→left: archive.
                SwipeToDismissBoxValue.EndToStart -> onArchive()
                // Swipe left→right: toggle read/unread.
                SwipeToDismissBoxValue.StartToEnd -> {
                    if (conversation.unreadCount > 0) viewModel.markRead(conversation)
                    else viewModel.markUnread(conversation)
                }
                else -> {}
            }
            false // snap back; the row updates/disappears via state instead
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val towardEnd = dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = if (towardEnd) Arrangement.Start else Arrangement.End,
                modifier = Modifier
                    .fillMaxSize()
                    .background(if (towardEnd) palette.Success else palette.Blue)
                    .padding(horizontal = 24.dp),
            ) {
                Icon(
                    painterResource(if (towardEnd) R.drawable.ic_check else R.drawable.ic_archive),
                    contentDescription = if (towardEnd) "Mark read/unread" else "Archive",
                    tint = Color.White,
                )
            }
        },
    ) {
        Box(Modifier.background(palette.Surface)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .combinedClickable(onClick = onClick, onLongClick = { menuOpen = true })
                    .padding(horizontal = 16.dp),
            ) {
                Avatar(conversation.recipients.firstOrNull(), size = 48.dp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            conversation.title,
                            style = DesignType.itemTitle,
                            color = palette.TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        if (conversation.muted) {
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                painterResource(R.drawable.ic_bell_off),
                                contentDescription = "Muted",
                                tint = palette.MutedText,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        conversation.snippet ?: "",
                        style = DesignType.body,
                        color = palette.TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        Formatters.conversationTime(context, conversation.date),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        color = palette.TimeText,
                    )
                    Spacer(Modifier.height(6.dp))
                    if (conversation.unreadCount > 0) {
                        UnreadBadge(conversation.unreadCount)
                    } else {
                        Spacer(Modifier.size(24.dp))
                    }
                }
            }

            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(if (conversation.pinned) "Unpin" else "Pin") },
                    onClick = { viewModel.togglePin(conversation); menuOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(if (conversation.archived) "Unarchive" else "Archive") },
                    onClick = { viewModel.toggleArchive(conversation); menuOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(if (conversation.muted) "Unmute" else "Mute") },
                    onClick = { viewModel.toggleMute(conversation); menuOpen = false },
                )
                DropdownMenuItem(
                    text = { Text(if (conversation.unreadCount > 0) "Mark as read" else "Mark as unread") },
                    onClick = {
                        if (conversation.unreadCount > 0) viewModel.markRead(conversation)
                        else viewModel.markUnread(conversation)
                        menuOpen = false
                    },
                )
                DropdownMenuItem(
                    text = { Text("Block") },
                    onClick = { viewModel.block(conversation); menuOpen = false },
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = palette.Error) },
                    onClick = { viewModel.delete(conversation); menuOpen = false },
                )
            }
        }
    }
}

@Composable
private fun BottomBar(
    onHome: () -> Unit,
    onScheduled: () -> Unit,
    onSettings: () -> Unit,
) {
    Column(Modifier.background(palette.Surface)) {
        androidx.compose.material3.HorizontalDivider(color = palette.Divider)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 20.dp),
        ) {
            IconButton(onClick = onHome) {
                Icon(
                    painterResource(R.drawable.ic_message_circle),
                    contentDescription = "Chats",
                    tint = palette.Blue,
                )
            }
            IconButton(onClick = onScheduled) {
                Icon(
                    painterResource(R.drawable.ic_clock),
                    contentDescription = "Scheduled",
                    tint = palette.TextSecondary,
                )
            }
            IconButton(onClick = onSettings) {
                Icon(
                    painterResource(R.drawable.ic_settings),
                    contentDescription = "Settings",
                    tint = palette.TextSecondary,
                )
            }
        }
    }
}
