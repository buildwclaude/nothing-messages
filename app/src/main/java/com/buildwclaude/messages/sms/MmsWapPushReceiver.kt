package com.buildwclaude.messages.sms

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsManager
import androidx.core.content.FileProvider
import com.android.messaging.mmslib.pdu.NotificationInd
import com.android.messaging.mmslib.pdu.PduParser
import com.android.messaging.mmslib.pdu.PduPersister
import com.buildwclaude.messages.notifications.MessageNotifier
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * Receives the MMS "notification" WAP push (a pointer, not the content), stores it,
 * then asks the platform MmsService to download the actual message over the
 * carrier APN. Fires only while this app is the default SMS app.
 */
@AndroidEntryPoint
class MmsWapPushReceiver : BroadcastReceiver() {

    @Inject lateinit var mmsSender: MmsSender
    @Inject lateinit var notifier: MessageNotifier

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.WAP_PUSH_DELIVER_ACTION) return
        if (intent.type != "application/vnd.wap.mms-message") return
        val data = intent.getByteArrayExtra("data") ?: return
        val subId = intent.getIntExtra("subscription", -1)

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val pdu = runCatching { PduParser(data, true).parse() }.getOrNull()
                val ind = pdu as? NotificationInd ?: return@launch
                val location = ind.contentLocation?.let { String(it) } ?: return@launch

                val msgUri = runCatching {
                    PduPersister.getPduPersister(context)
                        .persist(ind, Telephony.Mms.Inbox.CONTENT_URI, subId, null, null)
                }.getOrNull()

                val dir = File(context.cacheDir, "mms").apply { mkdirs() }
                val file = File(dir, "dl_${System.currentTimeMillis()}.dat")
                file.createNewFile()
                val contentUri = FileProvider.getUriForFile(
                    context, "${context.packageName}.mmsfileprovider", file,
                )
                mmsSender.grantToTelephony(contentUri)

                val downloadedIntent = PendingIntent.getBroadcast(
                    context,
                    file.name.hashCode(),
                    Intent(context, MmsStatusReceiver::class.java).apply {
                        action = MmsStatusReceiver.ACTION_MMS_DOWNLOADED
                        putExtra(MmsStatusReceiver.EXTRA_URI, msgUri?.toString())
                        putExtra(MmsStatusReceiver.EXTRA_FILE, file.absolutePath)
                        putExtra(MmsStatusReceiver.EXTRA_SUB_ID, subId)
                        putExtra(MmsStatusReceiver.EXTRA_LOCATION, location)
                    },
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                val sm = context.getSystemService(SmsManager::class.java).let {
                    if (subId >= 0) it.createForSubscriptionId(subId) else it
                }
                runCatching {
                    sm.downloadMultimediaMessage(context, location, contentUri, null, downloadedIntent)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
