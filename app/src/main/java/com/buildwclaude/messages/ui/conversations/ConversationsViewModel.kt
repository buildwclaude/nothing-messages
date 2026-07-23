package com.buildwclaude.messages.ui.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buildwclaude.messages.core.util.PhoneNumbers
import com.buildwclaude.messages.data.db.BlockedNumberDao
import com.buildwclaude.messages.data.db.BlockedNumberEntity
import com.buildwclaude.messages.data.db.ThreadSettingDao
import com.buildwclaude.messages.data.db.ThreadSettingEntity
import com.buildwclaude.messages.data.telephony.ContactsRepository
import com.buildwclaude.messages.data.telephony.DefaultSmsRole
import com.buildwclaude.messages.data.telephony.TelephonyRepository
import com.buildwclaude.messages.domain.model.Conversation
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ChatFilter { ALL, UNREAD, GROUPS, ARCHIVED }

data class ConversationsUiState(
    val loading: Boolean = true,
    val pinned: List<Conversation> = emptyList(),
    val conversations: List<Conversation> = emptyList(),
    val filter: ChatFilter = ChatFilter.ALL,
    val searchQuery: String = "",
    val isDefaultSmsApp: Boolean = true,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val telephony: TelephonyRepository,
    private val threadSettings: ThreadSettingDao,
    private val blockedDao: BlockedNumberDao,
    private val contacts: ContactsRepository,
    val defaultRole: DefaultSmsRole,
) : ViewModel() {

    private val filter = MutableStateFlow(ChatFilter.ALL)
    private val searchQuery = MutableStateFlow("")
    private val roleRefresh = MutableStateFlow(0)

    private val rawConversations = telephony.changes().mapLatest { telephony.loadConversations() }

    val state = combine(
        rawConversations,
        threadSettings.observeAll(),
        filter,
        searchQuery,
        roleRefresh,
    ) { convs, settings, filter, query, _ ->
        val settingsMap = settings.associateBy { it.threadId }
        val decorated = convs.map { c ->
            val s = settingsMap[c.threadId]
            c.copy(
                pinned = s?.pinned == true,
                archived = s?.archived == true,
                muted = s?.muted == true,
            )
        }
        val searched = if (query.isBlank()) decorated else {
            val matchingThreads = telephony.searchThreads(query).toSet()
            decorated.filter { c ->
                c.threadId in matchingThreads ||
                    c.title.contains(query, ignoreCase = true) ||
                    c.recipients.any { it.address.contains(query) }
            }
        }
        val visible = when (filter) {
            ChatFilter.ALL -> searched.filter { !it.archived }
            ChatFilter.UNREAD -> searched.filter { !it.archived && it.unreadCount > 0 }
            ChatFilter.GROUPS -> searched.filter { !it.archived && it.isGroup }
            ChatFilter.ARCHIVED -> searched.filter { it.archived }
        }
        ConversationsUiState(
            loading = false,
            pinned = if (filter == ChatFilter.ALL && query.isBlank()) visible.filter { it.pinned } else emptyList(),
            conversations = if (filter == ChatFilter.ALL && query.isBlank()) visible.filter { !it.pinned } else visible,
            filter = filter,
            searchQuery = query,
            isDefaultSmsApp = defaultRole.isDefault,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConversationsUiState())

    fun setFilter(f: ChatFilter) { filter.value = f }
    fun setSearch(q: String) { searchQuery.value = q }
    fun refreshRole() { roleRefresh.value++ ; contacts.invalidate() }

    private suspend fun setting(threadId: Long) =
        threadSettings.byThread(threadId) ?: ThreadSettingEntity(threadId)

    fun togglePin(c: Conversation) = viewModelScope.launch {
        threadSettings.upsert(setting(c.threadId).copy(pinned = !c.pinned))
    }

    fun toggleArchive(c: Conversation) = viewModelScope.launch {
        threadSettings.upsert(setting(c.threadId).copy(archived = !c.archived))
    }

    fun toggleMute(c: Conversation) = viewModelScope.launch {
        threadSettings.upsert(setting(c.threadId).copy(muted = !c.muted))
    }

    fun markRead(c: Conversation) = viewModelScope.launch { telephony.markThreadRead(c.threadId) }
    fun markUnread(c: Conversation) = viewModelScope.launch { telephony.markThreadUnread(c.threadId) }

    fun delete(c: Conversation) = viewModelScope.launch {
        telephony.deleteThread(c.threadId)
        threadSettings.delete(c.threadId)
    }

    fun block(c: Conversation) = viewModelScope.launch {
        for (r in c.recipients) {
            blockedDao.insert(BlockedNumberEntity(PhoneNumbers.normalize(r.address), r.contactName))
        }
    }
}
