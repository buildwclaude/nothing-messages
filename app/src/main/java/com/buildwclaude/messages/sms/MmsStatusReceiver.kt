package com.buildwclaude.messages.sms

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Telephony
import com.android.messaging.mmslib.pdu.PduHeaders
import com.android.messaging.mmslib.pdu.PduParser
import com.android.messaging.mmslib.pdu.PduPersister
import com.android.messaging.mmslib.pdu.RetrieveConf
import com.buildwclaude.messages.notifications.MessageNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MmsStatusReceiver : BroadcastReceiver() {

    @Inject lateinit var mmsSender: MmsSender
    @Inject lateinit var notifier: MessageNotifier

    override fun onReceive(context: Context, intent: Intent) {
        val filePath = intent.getStringExtra(EXTRA_FILE)
        val messageUri = intent.getStringExtra(EXTRA_URI)?.let(Uri::parse)
        val success = resultCode == Activity.RESULT_OK
        val action = intent.action
        val subId = intent.getIntExtra(EXTRA_SUB_ID, -1)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (action) {
                    ACTION_MMS_SENT -> {
                        if (messageUri != null) {
                            if (success) {
                                runCatching {
                                    PduPersister.getPduPersister(context)
                                        .move(messageUri, Telephony.Mms.Sent.CONTENT_URI)
                                }
                            } else {
                                mmsSender.markFailed(messageUri)
                                notifier.notifySendFailed(messageUri)
                            }
                        }
                        filePath?.let { File(it).delete() }
                    }
                    ACTION_MMS_DOWNLOADED -> {
                        if (success && filePath != null) {
                            val bytes = runCatching { File(filePath).readBytes() }.getOrNull()
                            if (bytes != null && bytes.isNotEmpty()) {
                                val pdu = runCatching { PduParser(bytes, true).parse() }.getOrNull()
                                if (pdu is RetrieveConf) {
                                    val newUri = runCatching {
                                        PduPersister.getPduPersister(context)
                                            .persist(pdu, Telephony.Mms.Inbox.CONTENT_URI, subId, null, null)
                                    }.getOrNull()
                                    // Remove the placeholder notification row.
                                    if (messageUri != null && newUri != null) {
                                        runCatching {
                                            context.contentResolver.delete(messageUri, null, null)
                                        }
                                    }
                                    val sender = pdu.from?.string
                                    val text = extractText(pdu)
                                    val threadId = newUri?.let { threadIdOf(context, it) } ?: -1L
                                    notifier.notifyIncoming(
                                        threadId = threadId,
                                        address = sender ?: "MMS",
                                        body = text ?: "Multimedia message",
                                        subId = subId,
                                        isMms = true,
                                    )
                                }
                            }
                        }
                        filePath?.let { File(it).delete() }
                    }
                }
            } finally {
                pending.finish()
            }
        }
    }

    private fun extractText(pdu: RetrieveConf): String? {
        val body = pdu.body ?: return null
        for (i in 0 until body.partsNum) {
            val part = body.getPart(i)
            if (String(part.contentType ?: continue).startsWith("text/plain")) {
                return part.data?.toString(Charsets.UTF_8)
            }
        }
        return null
    }

    private fun threadIdOf(context: Context, uri: Uri): Long? = runCatching {
        context.contentResolver.query(
            uri, arrayOf(Telephony.Mms.THREAD_ID), null, null, null,
        )?.use { c -> if (c.moveToFirst()) c.getLong(0) else null }
    }.getOrNull()

    companion object {
        const val ACTION_MMS_SENT = "com.buildwclaude.messages.MMS_SENT"
        const val ACTION_MMS_DOWNLOADED = "com.buildwclaude.messages.MMS_DOWNLOADED"
        const val EXTRA_URI = "message_uri"
        const val EXTRA_FILE = "pdu_file"
        const val EXTRA_SUB_ID = "sub_id"
        const val EXTRA_LOCATION = "location"
    }
}
