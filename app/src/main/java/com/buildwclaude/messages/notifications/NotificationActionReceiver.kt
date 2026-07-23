package com.buildwclaude.messages.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.RemoteInput
import com.buildwclaude.messages.data.telephony.TelephonyRepository
import com.buildwclaude.messages.sms.SmsSender
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class NotificationActionReceiver : BroadcastReceiver() {

    @Inject lateinit var smsSender: SmsSender
    @Inject lateinit var telephony: TelephonyRepository
    @Inject lateinit var notifier: MessageNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val threadId = intent.getLongExtra(EXTRA_THREAD_ID, -1L)
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_REPLY -> {
                        val text = RemoteInput.getResultsFromIntent(intent)
                            ?.getCharSequence(MessageNotifier.KEY_REPLY)?.toString()
                        val address = intent.getStringExtra(EXTRA_ADDRESS)
                        val subId = intent.getIntExtra(EXTRA_SUB_ID, -1)
                        if (!text.isNullOrBlank() && !address.isNullOrBlank()) {
                            smsSender.sendText(address, text, subId)
                            if (threadId > 0) telephony.markThreadRead(threadId)
                        }
                        if (threadId > 0) notifier.cancelForThread(threadId)
                    }
                    ACTION_MARK_READ -> {
                        if (threadId > 0) {
                            telephony.markThreadRead(threadId)
                            notifier.cancelForThread(threadId)
                        }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_REPLY = "com.buildwclaude.messages.NOTIF_REPLY"
        const val ACTION_MARK_READ = "com.buildwclaude.messages.NOTIF_MARK_READ"
        const val EXTRA_THREAD_ID = "thread_id"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_SUB_ID = "sub_id"
    }
}
