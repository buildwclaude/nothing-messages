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

/** Date dial selection; null component = ∞ (any). */
data class DateFilter(val day: Int? = null, val month: Int? = null, val year: Int? = null) {
    val isEmpty: Boolean get() = day == null && month == null && year == null
}

data class ConversationsUiState(
    val loading: Boolean = true,
    val pinned: List<Conversation> = emptyList(),
    val conversations: List<Conversation> = emptyList(), // decorated, searched, time-filtered
    val searchQuery: String = "",
    val isDefaultSmsApp: Boolean = true,
) {
    /** Rows for one pager page. Pinned rows live in the grid on the ALL page only. */
    fun pageList(filter: ChatFilter): List<Conversation> = when (filter) {
        ChatFilter.ALL -> conversations.filter { !it.archived && (searchQuery.isNotBlank() || !it.pinned) }
        ChatFilter.UNREAD -> conversations.filter { !it.archived && it.unreadCount > 0 }
        ChatFilter.GROUPS -> conversations.filter { !it.archived && it.isGroup }
        ChatFilter.ARCHIVED -> conversations.filter { it.archived }
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ConversationsViewModel @Inject constructor(
    private val telephony: TelephonyRepository,
    private val threadSettings: ThreadSettingDao,
    private val blockedDao: BlockedNumberDao,
    private val contacts: ContactsRepository,
    val defaultRole: DefaultSmsRole,
    prefs: com.buildwclaude.messages.data.prefs.AppPrefs,
) : ViewModel() {

    val haptics = prefs.haptics
    private val searchQuery = MutableStateFlow("")
    private val roleRefresh = MutableStateFlow(0)
    // null components mean ∞ (any). All null = no date filter, show everything.
    val dateFilter = MutableStateFlow(DateFilter())

    // Reload on provider changes AND on explicit refreshes (permission/role grants
    // don't fire the ContentObserver, so screen-resume bumps roleRefresh).
    private val rawConversations = combine(telephony.changes(), roleRefresh) { _, _ -> }
        .mapLatest { telephony.loadConversations() }

    val state = combine(
        rawConversations,
        threadSettings.observeAll(),
        searchQuery,
        dateFilter,
        roleRefresh,
    ) { convs, settings, query, filter, _ ->
        val settingsMap = settings.associateBy { it.threadId }
        val decorated = convs.map { c ->
            val s = settingsMap[c.threadId]
            c.copy(
                pinned = s?.pinned == true,
                archived = s?.archived == true,
                muted = s?.muted == true,
            )
        }
        val timed = if (filter.isEmpty) decorated else {
            val cal = java.util.Calendar.getInstance()
            decorated.filter { conv ->
                cal.timeInMillis = conv.date
                (filter.day == null || cal.get(java.util.Calendar.DAY_OF_MONTH) == filter.day) &&
                    (filter.month == null || cal.get(java.util.Calendar.MONTH) == filter.month) &&
                    (filter.year == null || cal.get(java.util.Calendar.YEAR) == filter.year)
            }
        }
        val searched = if (query.isBlank()) timed else {
            val matchingThreads = telephony.searchThreads(query).toSet()
            timed.filter { c ->
                c.threadId in matchingThreads ||
                    c.title.contains(query, ignoreCase = true) ||
                    c.recipients.any { it.address.contains(query) }
            }
        }
        ConversationsUiState(
            loading = false,
            pinned = if (query.isBlank()) searched.filter { it.pinned && !it.archived } else emptyList(),
            conversations = searched,
            searchQuery = query,
            isDefaultSmsApp = defaultRole.isDefault,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ConversationsUiState())

    fun setSearch(q: String) { searchQuery.value = q }
    fun setDateFilter(day: Int?, month: Int?, year: Int?) {
        dateFilter.value = DateFilter(day, month, year)
    }
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
