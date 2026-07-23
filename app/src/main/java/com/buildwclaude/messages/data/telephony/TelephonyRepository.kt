package com.buildwclaude.messages.data.telephony

import android.Manifest
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import com.buildwclaude.messages.domain.model.Conversation
import com.buildwclaude.messages.domain.model.DeliveryState
import com.buildwclaude.messages.domain.model.Message
import com.buildwclaude.messages.domain.model.MessageBox
import com.buildwclaude.messages.domain.model.MmsPartData
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TelephonyRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contacts: ContactsRepository,
    private val defaultRole: DefaultSmsRole,
) {
    private val resolver get() = context.contentResolver

    private fun canRead() =
        context.checkSelfPermission(Manifest.permission.READ_SMS) == android.content.pm.PackageManager.PERMISSION_GRANTED

    /** Emits on every change to the SMS/MMS store (and once immediately). */
    fun changes(): Flow<Unit> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        resolver.registerContentObserver(Uri.parse("content://mms-sms/"), true, observer)
        trySend(Unit)
        awaitClose { resolver.unregisterContentObserver(observer) }
    }.conflate().flowOn(Dispatchers.IO)

    suspend fun loadConversations(): List<Conversation> = withContext(Dispatchers.IO) {
        if (!canRead()) return@withContext emptyList()
        val unreadByThread = loadUnreadCounts()
        val conversations = ArrayList<Conversation>()
        runCatching {
            resolver.query(
                Uri.parse("content://mms-sms/conversations?simple=true"),
                arrayOf("_id", "date", "message_count", "recipient_ids", "snippet"),
                null, null, "date DESC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val threadId = c.getLong(0)
                    val recipientIds = c.getString(3) ?: ""
                    val addresses = recipientIds.split(" ").filter { it.isNotBlank() }
                        .mapNotNull { canonicalAddress(it) }
                    if (addresses.isEmpty()) continue
                    conversations += Conversation(
                        threadId = threadId,
                        recipients = addresses.map { addr -> contacts.resolve(addr) },
                        snippet = c.getString(4),
                        date = c.getLong(1),
                        messageCount = c.getInt(2),
                        unreadCount = unreadByThread[threadId] ?: 0,
                    )
                }
            }
        }
        conversations
    }

    private val canonicalCache = HashMap<String, String?>()

    private fun canonicalAddress(id: String): String? {
        canonicalCache[id]?.let { return it }
        val addr = runCatching {
            resolver.query(
                Uri.parse("content://mms-sms/canonical-address/$id"),
                null, null, null, null,
            )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
        }.getOrNull()
        canonicalCache[id] = addr
        return addr
    }

    private fun loadUnreadCounts(): Map<Long, Int> {
        val map = HashMap<Long, Int>()
        runCatching {
            resolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.THREAD_ID),
                "${Telephony.Sms.READ} = 0 AND ${Telephony.Sms.TYPE} = ${Telephony.Sms.MESSAGE_TYPE_INBOX}",
                null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val t = c.getLong(0)
                    map[t] = (map[t] ?: 0) + 1
                }
            }
        }
        runCatching {
            resolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf(Telephony.Mms.THREAD_ID),
                "${Telephony.Mms.READ} = 0 AND ${Telephony.Mms.MESSAGE_BOX} = ${Telephony.Mms.MESSAGE_BOX_INBOX}",
                null, null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val t = c.getLong(0)
                    map[t] = (map[t] ?: 0) + 1
                }
            }
        }
        return map
    }

    suspend fun loadMessages(threadId: Long): List<Message> = withContext(Dispatchers.IO) {
        if (!canRead()) return@withContext emptyList()
        val out = ArrayList<Message>()
        out += loadSms(threadId)
        out += loadMms(threadId)
        out.sortedBy { it.date }
    }

    private fun loadSms(threadId: Long): List<Message> {
        val list = ArrayList<Message>()
        runCatching {
            resolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(
                    Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY,
                    Telephony.Sms.DATE, Telephony.Sms.TYPE, Telephony.Sms.READ,
                    Telephony.Sms.STATUS, Telephony.Sms.SUBSCRIPTION_ID,
                ),
                "${Telephony.Sms.THREAD_ID} = ?", arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val type = c.getInt(4)
                    val status = c.getInt(6)
                    val box = when (type) {
                        Telephony.Sms.MESSAGE_TYPE_INBOX -> MessageBox.INBOX
                        Telephony.Sms.MESSAGE_TYPE_SENT -> MessageBox.SENT
                        Telephony.Sms.MESSAGE_TYPE_OUTBOX -> MessageBox.OUTBOX
                        Telephony.Sms.MESSAGE_TYPE_FAILED -> MessageBox.FAILED
                        Telephony.Sms.MESSAGE_TYPE_QUEUED -> MessageBox.QUEUED
                        Telephony.Sms.MESSAGE_TYPE_DRAFT -> MessageBox.DRAFT
                        else -> MessageBox.INBOX
                    }
                    val delivery = when {
                        box == MessageBox.FAILED -> DeliveryState.FAILED
                        box == MessageBox.OUTBOX || box == MessageBox.QUEUED -> DeliveryState.SENDING
                        box == MessageBox.SENT && status == Telephony.Sms.STATUS_COMPLETE -> DeliveryState.DELIVERED
                        box == MessageBox.SENT -> DeliveryState.SENT
                        else -> DeliveryState.NONE
                    }
                    list += Message(
                        id = c.getLong(0),
                        threadId = threadId,
                        isMms = false,
                        address = c.getString(1),
                        body = c.getString(2),
                        date = c.getLong(3),
                        box = box,
                        read = c.getInt(5) == 1,
                        subId = c.getInt(7),
                        delivery = delivery,
                    )
                }
            }
        }
        return list
    }

    private fun loadMms(threadId: Long): List<Message> {
        val list = ArrayList<Message>()
        runCatching {
            resolver.query(
                Telephony.Mms.CONTENT_URI,
                arrayOf(
                    Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.MESSAGE_BOX,
                    Telephony.Mms.READ, Telephony.Mms.SUBSCRIPTION_ID, Telephony.Mms.MESSAGE_TYPE,
                ),
                "${Telephony.Mms.THREAD_ID} = ?", arrayOf(threadId.toString()),
                "${Telephony.Mms.DATE} ASC",
            )?.use { c ->
                while (c.moveToNext()) {
                    val id = c.getLong(0)
                    val box = when (c.getInt(2)) {
                        Telephony.Mms.MESSAGE_BOX_INBOX -> MessageBox.INBOX
                        Telephony.Mms.MESSAGE_BOX_SENT -> MessageBox.SENT
                        Telephony.Mms.MESSAGE_BOX_OUTBOX -> MessageBox.OUTBOX
                        Telephony.Mms.MESSAGE_BOX_FAILED -> MessageBox.FAILED
                        else -> MessageBox.INBOX
                    }
                    val parts = loadParts(id)
                    val text = parts.filter { it.mimeType == "text/plain" }
                        .mapNotNull { it.text }.joinToString("\n").ifBlank { null }
                    list += Message(
                        id = id,
                        threadId = threadId,
                        isMms = true,
                        address = mmsSenderAddress(id),
                        body = text,
                        date = c.getLong(1) * 1000L, // MMS dates are stored in seconds
                        box = box,
                        read = c.getInt(3) == 1,
                        subId = c.getInt(4),
                        delivery = when (box) {
                            MessageBox.FAILED -> DeliveryState.FAILED
                            MessageBox.OUTBOX -> DeliveryState.SENDING
                            MessageBox.SENT -> DeliveryState.SENT
                            else -> DeliveryState.NONE
                        },
                        parts = parts,
                    )
                }
            }
        }
        return list
    }

    private fun loadParts(mmsId: Long): List<MmsPartData> {
        val parts = ArrayList<MmsPartData>()
        runCatching {
            resolver.query(
                Uri.parse("content://mms/part"),
                arrayOf("_id", "ct", "text", "name", "cl"),
                "mid = ?", arrayOf(mmsId.toString()), null,
            )?.use { c ->
                while (c.moveToNext()) {
                    val partId = c.getLong(0)
                    val mime = c.getString(1) ?: "application/octet-stream"
                    if (mime == "application/smil") continue
                    parts += MmsPartData(
                        id = partId,
                        mimeType = mime,
                        text = if (mime.startsWith("text/")) readTextPart(partId, c.getString(2)) else null,
                        uri = Uri.parse("content://mms/part/$partId"),
                        fileName = c.getString(3) ?: c.getString(4),
                    )
                }
            }
        }
        return parts
    }

    private fun readTextPart(partId: Long, inline: String?): String? {
        if (inline != null) return inline
        return runCatching {
            resolver.openInputStream(Uri.parse("content://mms/part/$partId"))?.use {
                it.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrNull()
    }

    private fun mmsSenderAddress(mmsId: Long): String? = runCatching {
        resolver.query(
            Uri.parse("content://mms/$mmsId/addr"),
            arrayOf("address", "type"),
            "type = 137", null, null, // 137 = PduHeaders.FROM
        )?.use { c -> if (c.moveToFirst()) c.getString(0) else null }
    }.getOrNull()

    suspend fun recipientsOf(threadId: Long): List<String> = withContext(Dispatchers.IO) {
        var result = emptyList<String>()
        runCatching {
            resolver.query(
                Uri.parse("content://mms-sms/conversations?simple=true"),
                arrayOf("_id", "recipient_ids"),
                "_id = ?", arrayOf(threadId.toString()), null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    result = (c.getString(1) ?: "").split(" ")
                        .filter { it.isNotBlank() }
                        .mapNotNull { canonicalAddress(it) }
                }
            }
        }
        result
    }

    // ----- Writes: allowed only for the default SMS app -----

    suspend fun markThreadRead(threadId: Long) = withContext(Dispatchers.IO) {
        if (!defaultRole.isDefault) return@withContext
        runCatching {
            val values = android.content.ContentValues().apply {
                put(Telephony.Sms.READ, 1)
                put(Telephony.Sms.SEEN, 1)
            }
            resolver.update(
                Telephony.Sms.CONTENT_URI, values,
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.READ} = 0",
                arrayOf(threadId.toString()),
            )
            resolver.update(
                Telephony.Mms.CONTENT_URI, values,
                "${Telephony.Mms.THREAD_ID} = ? AND ${Telephony.Mms.READ} = 0",
                arrayOf(threadId.toString()),
            )
        }
    }

    suspend fun markThreadUnread(threadId: Long) = withContext(Dispatchers.IO) {
        if (!defaultRole.isDefault) return@withContext
        runCatching {
            // Mark the latest inbox message unread so the badge reappears.
            resolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms._ID),
                "${Telephony.Sms.THREAD_ID} = ? AND ${Telephony.Sms.TYPE} = ${Telephony.Sms.MESSAGE_TYPE_INBOX}",
                arrayOf(threadId.toString()),
                "${Telephony.Sms.DATE} DESC LIMIT 1",
            )?.use { c ->
                if (c.moveToFirst()) {
                    val values = android.content.ContentValues().apply { put(Telephony.Sms.READ, 0) }
                    resolver.update(
                        Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, c.getLong(0).toString()),
                        values, null, null,
                    )
                }
            }
        }
    }

    suspend fun deleteMessage(message: Message): Boolean = withContext(Dispatchers.IO) {
        if (!defaultRole.isDefault) return@withContext false
        runCatching {
            val uri = if (message.isMms) {
                Uri.withAppendedPath(Telephony.Mms.CONTENT_URI, message.id.toString())
            } else {
                Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, message.id.toString())
            }
            resolver.delete(uri, null, null) > 0
        }.getOrDefault(false)
    }

    suspend fun deleteThread(threadId: Long): Boolean = withContext(Dispatchers.IO) {
        if (!defaultRole.isDefault) return@withContext false
        runCatching {
            resolver.delete(
                Uri.parse("content://mms-sms/conversations/$threadId"),
                null, null,
            ) > 0
        }.getOrDefault(false)
    }

    /** Simple body search across SMS; returns matching thread ids ordered by recency. */
    suspend fun searchThreads(query: String): List<Long> = withContext(Dispatchers.IO) {
        if (!canRead() || query.isBlank()) return@withContext emptyList()
        val ids = LinkedHashSet<Long>()
        runCatching {
            resolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.THREAD_ID),
                "${Telephony.Sms.BODY} LIKE ?", arrayOf("%$query%"),
                "${Telephony.Sms.DATE} DESC",
            )?.use { c ->
                while (c.moveToNext()) ids += c.getLong(0)
            }
        }
        ids.toList()
    }
}
