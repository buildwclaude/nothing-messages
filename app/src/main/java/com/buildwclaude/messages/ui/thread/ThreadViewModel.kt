package com.buildwclaude.messages.ui.thread

import android.content.Context
import android.provider.Telephony
import android.telephony.SmsMessage
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buildwclaude.messages.data.db.ScheduledMessageDao
import com.buildwclaude.messages.data.db.ThreadSettingDao
import com.buildwclaude.messages.data.db.ThreadSettingEntity
import com.buildwclaude.messages.data.telephony.ContactsRepository
import com.buildwclaude.messages.data.telephony.DefaultSmsRole
import com.buildwclaude.messages.data.telephony.SimRepository
import com.buildwclaude.messages.data.telephony.TelephonyRepository
import com.buildwclaude.messages.domain.model.Message
import com.buildwclaude.messages.domain.model.MessageBox
import com.buildwclaude.messages.domain.model.PendingAttachment
import com.buildwclaude.messages.domain.model.Recipient
import com.buildwclaude.messages.domain.model.SimInfo
import com.buildwclaude.messages.notifications.MessageNotifier
import com.buildwclaude.messages.scheduled.ScheduledSendManager
import com.buildwclaude.messages.sms.MmsSender
import com.buildwclaude.messages.sms.SmsSender
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class ThreadUiState(
    val threadId: Long = -1,
    val recipients: List<Recipient> = emptyList(),
    val messages: List<Message> = emptyList(),
    val sims: List<SimInfo> = emptyList(),
    val selectedSubId: Int = -1,
    val isDefaultSmsApp: Boolean = true,
    val muted: Boolean = false,
    val canScheduleExact: Boolean = true,
) {
    val title: String get() = when {
        recipients.isEmpty() -> "Conversation"
        recipients.size == 1 -> recipients[0].displayName
        else -> recipients.joinToString(", ") { it.displayName.substringBefore(' ') }
    }
    val isGroup: Boolean get() = recipients.size > 1
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ThreadViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    savedState: SavedStateHandle,
    private val telephony: TelephonyRepository,
    private val contacts: ContactsRepository,
    private val sims: SimRepository,
    private val smsSender: SmsSender,
    private val mmsSender: MmsSender,
    private val scheduled: ScheduledSendManager,
    private val scheduledDao: ScheduledMessageDao,
    private val threadSettings: ThreadSettingDao,
    private val notifier: MessageNotifier,
    private val defaultRole: DefaultSmsRole,
    prefs: com.buildwclaude.messages.data.prefs.AppPrefs,
) : ViewModel() {

    val haptics = prefs.haptics
    private val addressArg: String? = savedState.get<String>("address")?.takeIf { it.isNotBlank() }
    private val threadIdFlow = MutableStateFlow(savedState.get<Long>("threadId") ?: -1L)
    private val recipientsFlow = MutableStateFlow<List<Recipient>>(emptyList())
    private val selectedSubId = MutableStateFlow(sims.defaultSmsSubId())

    // Composer state lives here so notification-reply/scheduling can reuse it.
    val draftText = MutableStateFlow("")
    val attachments = MutableStateFlow<List<PendingAttachment>>(emptyList())
    val editingScheduledId = MutableStateFlow<Long?>(null)

    init {
        viewModelScope.launch {
            var tid = threadIdFlow.value
            val argAddresses = addressArg?.split(";")?.filter { it.isNotBlank() } ?: emptyList()
            if (tid <= 0 && argAddresses.isNotEmpty()) {
                tid = withContext(Dispatchers.IO) {
                    runCatching {
                        Telephony.Threads.getOrCreateThreadId(context, argAddresses.toSet())
                    }.getOrDefault(-1L)
                }
                threadIdFlow.value = tid
            }
            val addresses = if (tid > 0) telephony.recipientsOf(tid).ifEmpty { argAddresses } else argAddresses
            recipientsFlow.value = addresses.map { contacts.resolve(it) }
            if (tid > 0) {
                telephony.markThreadRead(tid)
                notifier.cancelForThread(tid)
            }
        }
    }

    val state = combine(
        threadIdFlow,
        recipientsFlow,
        telephony.changes(),
        selectedSubId,
    ) { tid, recipients, _, subId ->
        Triple(tid, recipients, subId)
    }.mapLatest { (tid, recipients, subId) ->
        val real = if (tid > 0) telephony.loadMessages(tid) else emptyList()
        val pending = if (tid > 0) {
            scheduledDao.observePendingForThread(tid)
        } else null
        ThreadUiState(
            threadId = tid,
            recipients = recipients,
            messages = real,
            sims = sims.activeSims(),
            selectedSubId = subId,
            isDefaultSmsApp = defaultRole.isDefault,
            muted = if (tid > 0) threadSettings.byThread(tid)?.muted == true else false,
            canScheduleExact = scheduled.canScheduleExact(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThreadUiState())

    /** Pending scheduled messages for this thread, shown as distinct bubbles. */
    val pendingScheduled = threadIdFlow
        .flatMapLatest { tid ->
            if (tid > 0) scheduledDao.observePendingForThread(tid)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectSim(subId: Int) { selectedSubId.value = subId }

    fun addAttachment(att: PendingAttachment) {
        attachments.value = attachments.value + att
    }

    fun removeAttachment(att: PendingAttachment) {
        attachments.value = attachments.value - att
    }

    fun segmentInfo(text: String): Pair<Int, Int>? {
        if (text.isEmpty()) return null
        return runCatching {
            val data = SmsMessage.calculateLength(text, false)
            data[0] to data[2] // segments, chars remaining in current segment
        }.getOrNull()
    }

    fun send() {
        val text = draftText.value.trim()
        val atts = attachments.value
        val recipients = recipientsFlow.value.map { it.address }
        if (recipients.isEmpty() || (text.isBlank() && atts.isEmpty())) return
        val subId = selectedSubId.value
        val editId = editingScheduledId.value
        viewModelScope.launch {
            if (editId != null) {
                // Editing a scheduled message and pressing send = send it now.
                scheduled.cancel(editId)
                editingScheduledId.value = null
            }
            if (atts.isNotEmpty() || recipients.size > 1) {
                mmsSender.send(recipients, text.takeIf { it.isNotBlank() }, atts, subId)
            } else {
                smsSender.sendText(recipients[0], text, subId)
            }
            draftText.value = ""
            attachments.value = emptyList()
        }
    }

    fun scheduleSend(sendAt: Long) {
        val text = draftText.value.trim()
        val recipients = recipientsFlow.value.map { it.address }
        if (recipients.isEmpty() || (text.isBlank() && attachments.value.isEmpty())) return
        val editId = editingScheduledId.value
        viewModelScope.launch {
            if (editId != null) {
                scheduled.updateSchedule(editId, text, sendAt)
                editingScheduledId.value = null
            } else {
                scheduled.schedule(
                    threadId = threadIdFlow.value.takeIf { it > 0 },
                    recipients = recipients,
                    body = text,
                    sendAt = sendAt,
                    subId = selectedSubId.value,
                    attachments = attachments.value,
                )
            }
            draftText.value = ""
            attachments.value = emptyList()
        }
    }

    fun editScheduled(id: Long, body: String) {
        editingScheduledId.value = id
        draftText.value = body
    }

    fun cancelScheduled(id: Long) = viewModelScope.launch {
        scheduled.cancel(id)
        if (editingScheduledId.value == id) {
            editingScheduledId.value = null
            draftText.value = ""
        }
    }

    fun retry(message: Message) = viewModelScope.launch {
        if (!message.isMms && message.box == MessageBox.FAILED) {
            smsSender.retry(message.id)
        }
    }

    fun delete(message: Message) = viewModelScope.launch { telephony.deleteMessage(message) }

    fun toggleMute() = viewModelScope.launch {
        val tid = threadIdFlow.value
        if (tid <= 0) return@launch
        val s = threadSettings.byThread(tid) ?: ThreadSettingEntity(tid)
        threadSettings.upsert(s.copy(muted = !s.muted))
    }

    fun markReadNow() = viewModelScope.launch {
        val tid = threadIdFlow.value
        if (tid > 0) telephony.markThreadRead(tid)
    }
}
