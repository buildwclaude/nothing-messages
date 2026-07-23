package com.buildwclaude.messages.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import android.telephony.SmsMessage
import com.buildwclaude.messages.notifications.MessageNotifier
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SmsStatusReceiver : BroadcastReceiver() {

    @Inject lateinit var smsSender: SmsSender
    @Inject lateinit var notifier: MessageNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val uriString = intent.getStringExtra(EXTRA_URI) ?: return
        val uri = Uri.parse(uriString)
        val lastPart = intent.getBooleanExtra(EXTRA_LAST_PART, true)
        when (intent.action) {
            ACTION_SENT -> {
                if (resultCode == Activity.RESULT_OK) {
                    if (lastPart) smsSender.updateType(uri, Telephony.Sms.MESSAGE_TYPE_SENT)
                } else {
                    smsSender.updateType(uri, Telephony.Sms.MESSAGE_TYPE_FAILED)
                    notifier.notifySendFailed(uri)
                }
            }
            ACTION_DELIVERED -> {
                if (lastPart) {
                    // The delivery PDU's status could be parsed; treat callback as delivered.
                    smsSender.updateStatus(uri, Telephony.Sms.STATUS_COMPLETE)
                }
            }
        }
    }

    companion object {
        const val ACTION_SENT = "com.buildwclaude.messages.SMS_SENT"
        const val ACTION_DELIVERED = "com.buildwclaude.messages.SMS_DELIVERED"
        const val EXTRA_URI = "message_uri"
        const val EXTRA_LAST_PART = "last_part"
    }
}
