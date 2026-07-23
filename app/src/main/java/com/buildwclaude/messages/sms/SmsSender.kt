package com.buildwclaude.messages.sms

import android.app.PendingIntent
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsManager
import com.buildwclaude.messages.data.telephony.DefaultSmsRole
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsSender @Inject constructor(
    @ApplicationContext private val context: Context,
    private val defaultRole: DefaultSmsRole,
) {
    private fun smsManagerFor(subId: Int): SmsManager {
        val base = context.getSystemService(SmsManager::class.java)
        return if (subId >= 0) base.createForSubscriptionId(subId) else base
    }

    /**
     * Sends a text SMS (multipart if needed). Writes it to the Telephony provider
     * first (as the default app) so it shows up instantly, then updates the row
     * from the sent/delivered broadcasts.
     */
    suspend fun sendText(destination: String, body: String, subId: Int): Uri? =
        withContext(Dispatchers.IO) {
            if (body.isBlank()) return@withContext null
            val uri = if (defaultRole.isDefault) {
                runCatching {
                    val values = ContentValues().apply {
                        put(Telephony.Sms.ADDRESS, destination)
                        put(Telephony.Sms.BODY, body)
                        put(Telephony.Sms.DATE, System.currentTimeMillis())
                        put(Telephony.Sms.READ, 1)
                        put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_OUTBOX)
                        if (subId >= 0) put(Telephony.Sms.SUBSCRIPTION_ID, subId)
                    }
                    context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
                }.getOrNull()
            } else null

            val sm = smsManagerFor(subId)
            val parts = sm.divideMessage(body)
            val sentIntents = ArrayList<PendingIntent>(parts.size)
            val deliveredIntents = ArrayList<PendingIntent>(parts.size)
            for (i in parts.indices) {
                val last = i == parts.size - 1
                sentIntents += statusPendingIntent(
                    SmsStatusReceiver.ACTION_SENT, uri, requestCode = (uri.hashCode() * 31 + i), last,
                )
                deliveredIntents += statusPendingIntent(
                    SmsStatusReceiver.ACTION_DELIVERED, uri, requestCode = (uri.hashCode() * 31 + i + 10_000), last,
                )
            }
            runCatching {
                sm.sendMultipartTextMessage(destination, null, parts, sentIntents, deliveredIntents)
            }.onFailure {
                uri?.let { u -> updateType(u, Telephony.Sms.MESSAGE_TYPE_FAILED) }
            }
            uri
        }

    private fun statusPendingIntent(action: String, messageUri: Uri?, requestCode: Int, isLastPart: Boolean): PendingIntent {
        val intent = Intent(context, SmsStatusReceiver::class.java).apply {
            this.action = action
            putExtra(SmsStatusReceiver.EXTRA_URI, messageUri?.toString())
            putExtra(SmsStatusReceiver.EXTRA_LAST_PART, isLastPart)
        }
        return PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    fun updateType(uri: Uri, type: Int) {
        if (!defaultRole.isDefault) return
        runCatching {
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(Telephony.Sms.TYPE, type) },
                null, null,
            )
        }
    }

    fun updateStatus(uri: Uri, status: Int) {
        if (!defaultRole.isDefault) return
        runCatching {
            context.contentResolver.update(
                uri,
                ContentValues().apply { put(Telephony.Sms.STATUS, status) },
                null, null,
            )
        }
    }

    /** Re-send a previously failed SMS row. */
    suspend fun retry(messageId: Long): Boolean = withContext(Dispatchers.IO) {
        var address: String? = null
        var body: String? = null
        var subId = -1
        runCatching {
            context.contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.SUBSCRIPTION_ID),
                "${Telephony.Sms._ID} = ?", arrayOf(messageId.toString()), null,
            )?.use { c ->
                if (c.moveToFirst()) {
                    address = c.getString(0); body = c.getString(1); subId = c.getInt(2)
                }
            }
        }
        val a = address ?: return@withContext false
        val b = body ?: return@withContext false
        // Delete the failed row; sendText writes a fresh outbox row.
        runCatching {
            context.contentResolver.delete(
                Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageId.toString()), null, null,
            )
        }
        sendText(a, b, subId) != null
    }
}
