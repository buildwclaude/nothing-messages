package com.buildwclaude.messages.domain.model

import android.net.Uri

data class Recipient(
    val address: String,
    val contactName: String? = null,
    val photoUri: String? = null,
) {
    val displayName: String get() = contactName ?: address
}

data class Conversation(
    val threadId: Long,
    val recipients: List<Recipient>,
    val snippet: String?,
    val date: Long,
    val messageCount: Int,
    val unreadCount: Int,
    val pinned: Boolean = false,
    val archived: Boolean = false,
    val muted: Boolean = false,
) {
    val isGroup: Boolean get() = recipients.size > 1
    val title: String get() = when {
        recipients.isEmpty() -> "Unknown"
        recipients.size == 1 -> recipients[0].displayName
        else -> recipients.joinToString(", ") { it.displayName.substringBefore(' ') }
    }
}

enum class MessageBox { INBOX, SENT, OUTBOX, FAILED, QUEUED, DRAFT }
enum class DeliveryState { NONE, SENDING, SENT, DELIVERED, FAILED }

data class MmsPartData(
    val id: Long,
    val mimeType: String,
    val text: String? = null,
    val uri: Uri? = null,
    val fileName: String? = null,
)

data class Message(
    val id: Long,
    val threadId: Long,
    val isMms: Boolean,
    val address: String?,        // sender for incoming; null/first recipient for outgoing
    val body: String?,
    val date: Long,
    val box: MessageBox,
    val read: Boolean,
    val subId: Int = -1,
    val delivery: DeliveryState = DeliveryState.NONE,
    val parts: List<MmsPartData> = emptyList(),
    // A scheduled-but-not-yet-sent message rendered as a pending bubble:
    val scheduledId: Long? = null,
    val scheduledFor: Long? = null,
) {
    val isIncoming: Boolean get() = box == MessageBox.INBOX
    val mediaParts: List<MmsPartData> get() = parts.filter {
        !it.mimeType.startsWith("text/") && it.mimeType != "application/smil"
    }
}

data class SimInfo(
    val subId: Int,
    val slotIndex: Int,
    val displayName: String,
)

/** Draft attachment chosen in the composer. */
data class PendingAttachment(
    val uri: Uri,
    val mimeType: String,
    val fileName: String? = null,
)
