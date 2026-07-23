package com.buildwclaude.messages.scheduled

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.buildwclaude.messages.data.db.ScheduledMessageDao
import com.buildwclaude.messages.data.db.ScheduledMessageEntity
import com.buildwclaude.messages.data.db.ScheduledStatus
import com.buildwclaude.messages.domain.model.PendingAttachment
import com.buildwclaude.messages.notifications.MessageNotifier
import com.buildwclaude.messages.sms.MmsSender
import com.buildwclaude.messages.sms.SmsSender
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns the lifecycle of scheduled messages: storing them in Room, arming exact
 * alarms, firing sends with retry/backoff, and re-arming after reboots.
 * Scheduled messages are NOT written to the Telephony provider until they send.
 */
@Singleton
class ScheduledSendManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: ScheduledMessageDao,
    private val smsSender: SmsSender,
    private val mmsSender: MmsSender,
    private val notifier: MessageNotifier,
) {
    private val alarmManager get() = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExact(): Boolean = alarmManager.canScheduleExactAlarms()

    suspend fun schedule(
        threadId: Long?,
        recipients: List<String>,
        body: String,
        sendAt: Long,
        subId: Int,
        attachments: List<PendingAttachment> = emptyList(),
    ): Long {
        // Copy attachment content into app-private storage so the send can
        // happen later even if the original Uri permission has lapsed.
        val storedPaths = ArrayList<String>()
        val storedTypes = ArrayList<String>()
        for ((i, att) in attachments.withIndex()) {
            runCatching {
                val dir = File(context.filesDir, "scheduled").apply { mkdirs() }
                val f = File(dir, "${System.currentTimeMillis()}_$i")
                context.contentResolver.openInputStream(att.uri)?.use { input ->
                    f.outputStream().use { input.copyTo(it) }
                }
                storedPaths += f.absolutePath
                storedTypes += att.mimeType
            }
        }
        val id = dao.insert(
            ScheduledMessageEntity(
                threadId = threadId,
                recipients = recipients.joinToString("|"),
                body = body,
                sendAt = sendAt,
                subId = subId,
                attachmentPaths = storedPaths.takeIf { it.isNotEmpty() }?.joinToString("|"),
                attachmentTypes = storedTypes.takeIf { it.isNotEmpty() }?.joinToString("|"),
            ),
        )
        armAlarm(id, sendAt)
        return id
    }

    suspend fun updateSchedule(id: Long, body: String, sendAt: Long) {
        val entity = dao.byId(id) ?: return
        if (entity.status != ScheduledStatus.PENDING) return
        dao.update(entity.copy(body = body, sendAt = sendAt))
        cancelAlarm(id)
        armAlarm(id, sendAt)
    }

    suspend fun cancel(id: Long) {
        val entity = dao.byId(id) ?: return
        if (entity.status == ScheduledStatus.PENDING) {
            dao.setStatus(id, ScheduledStatus.CANCELLED)
            cancelAlarm(id)
            cleanupAttachments(entity)
        }
    }

    fun armAlarm(id: Long, sendAt: Long) {
        val pi = alarmPendingIntent(id)
        runCatching {
            if (canScheduleExact()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, sendAt, pi)
            } else {
                // Inexact fallback; the WorkManager backstop also sweeps overdue sends.
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, sendAt, pi)
            }
        }
    }

    private fun cancelAlarm(id: Long) {
        alarmManager.cancel(alarmPendingIntent(id))
    }

    private fun alarmPendingIntent(id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            id.toInt(),
            Intent(context, ScheduledSendReceiver::class.java).apply {
                action = ScheduledSendReceiver.ACTION_FIRE
                putExtra(ScheduledSendReceiver.EXTRA_ID, id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** Fire one scheduled message now. Handles retry with backoff and failure notice. */
    suspend fun fire(id: Long) = withContext(Dispatchers.IO) {
        val entity = dao.byId(id) ?: return@withContext
        if (entity.status != ScheduledStatus.PENDING) return@withContext

        val recipients = entity.recipients.split("|").filter { it.isNotBlank() }
        if (recipients.isEmpty()) {
            dao.setStatus(id, ScheduledStatus.FAILED)
            return@withContext
        }

        val attachments = buildList {
            val paths = entity.attachmentPaths?.split("|") ?: emptyList()
            val types = entity.attachmentTypes?.split("|") ?: emptyList()
            paths.forEachIndexed { i, p ->
                val f = File(p)
                if (f.exists()) add(
                    PendingAttachment(
                        uri = Uri.fromFile(f),
                        mimeType = types.getOrElse(i) { "application/octet-stream" },
                    ),
                )
            }
        }

        val success = runCatching {
            if (recipients.size > 1 || attachments.isNotEmpty()) {
                mmsSender.send(recipients, entity.body, attachments, entity.subId) != null
            } else {
                smsSender.sendText(recipients[0], entity.body, entity.subId) != null
            }
        }.getOrDefault(false)

        if (success) {
            dao.setStatus(id, ScheduledStatus.SENT)
            cleanupAttachments(entity)
        } else {
            val attempts = entity.attempts + 1
            if (attempts < MAX_ATTEMPTS) {
                val delayMs = BACKOFF_BASE_MS shl (attempts - 1) // 2min, 4min
                dao.update(entity.copy(attempts = attempts, lastError = "send failed"))
                armAlarm(id, System.currentTimeMillis() + delayMs)
            } else {
                dao.update(
                    entity.copy(
                        attempts = attempts,
                        status = ScheduledStatus.FAILED,
                        lastError = "send failed after $attempts attempts",
                    ),
                )
                notifier.notifyScheduledFailed(recipients.joinToString(", "), "could not send")
            }
        }
    }

    /** Re-arms every pending alarm (used after boot/update/time changes). */
    suspend fun rearmAll() {
        val now = System.currentTimeMillis()
        for (entity in dao.pending()) {
            if (entity.sendAt <= now) {
                fire(entity.id)
            } else {
                armAlarm(entity.id, entity.sendAt)
            }
        }
    }

    suspend fun fireOverdue() {
        for (entity in dao.overdue(System.currentTimeMillis())) {
            fire(entity.id)
        }
    }

    private fun cleanupAttachments(entity: ScheduledMessageEntity) {
        entity.attachmentPaths?.split("|")?.forEach { runCatching { File(it).delete() } }
    }

    companion object {
        const val MAX_ATTEMPTS = 3
        const val BACKOFF_BASE_MS = 2L * 60 * 1000
    }
}
