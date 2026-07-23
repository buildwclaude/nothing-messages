package com.buildwclaude.messages.sms

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.buildwclaude.messages.core.util.PhoneNumbers
import com.buildwclaude.messages.data.db.BlockedNumberDao
import com.buildwclaude.messages.notifications.MessageNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Fires only while this app is the default SMS app. The system does NOT write
 * incoming SMS to the Telephony provider in that case — that is our job here.
 */
@AndroidEntryPoint
class SmsDeliverReceiver : BroadcastReceiver() {

    @Inject lateinit var notifier: MessageNotifier
    @Inject lateinit var blockedDao: BlockedNumberDao

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_DELIVER_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent) ?: return
        if (messages.isEmpty()) return
        val address = messages[0].displayOriginatingAddress ?: return
        val body = messages.joinToString("") { it.displayMessageBody ?: "" }
        val subId = intent.getIntExtra("subscription", -1)
        val timestamp = messages[0].timestampMillis

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val blocked = blockedDao.count(PhoneNumbers.normalize(address)) > 0
                if (blocked) return@launch

                val values = ContentValues().apply {
                    put(Telephony.Sms.ADDRESS, address)
                    put(Telephony.Sms.BODY, body)
                    put(Telephony.Sms.DATE, System.currentTimeMillis())
                    put(Telephony.Sms.DATE_SENT, timestamp)
                    put(Telephony.Sms.READ, 0)
                    put(Telephony.Sms.SEEN, 0)
                    put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX)
                    if (subId >= 0) put(Telephony.Sms.SUBSCRIPTION_ID, subId)
                }
                val uri = runCatching {
                    context.contentResolver.insert(Telephony.Sms.CONTENT_URI, values)
                }.getOrNull()

                val threadId = uri?.let {
                    runCatching {
                        context.contentResolver.query(
                            it, arrayOf(Telephony.Sms.THREAD_ID), null, null, null,
                        )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
                    }.getOrNull()
                }

                notifier.notifyIncoming(
                    threadId = threadId ?: -1L,
                    address = address,
                    body = body,
                    subId = subId,
                    isMms = false,
                )
            } finally {
                pending.finish()
            }
        }
    }
}
