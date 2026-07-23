package com.buildwclaude.messages.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.app.RemoteInput
import com.buildwclaude.messages.R
import com.buildwclaude.messages.data.db.ThreadSettingDao
import com.buildwclaude.messages.data.telephony.ContactsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MessageNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
    private val contacts: ContactsRepository,
    private val threadSettings: ThreadSettingDao,
    private val prefs: com.buildwclaude.messages.data.prefs.AppPrefs,
) {
    private val nm get() = NotificationManagerCompat.from(context)

    fun createBaseChannels() {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MESSAGES,
                context.getString(R.string.notification_channel_messages),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply { enableVibration(true) },
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_FAILURES,
                context.getString(R.string.notification_channel_failures),
                NotificationManager.IMPORTANCE_DEFAULT,
            ),
        )
    }

    /** Per-conversation channel so each thread can be tuned/muted in system settings. */
    private fun conversationChannel(threadId: Long, title: String): String {
        val id = "conv_$threadId"
        val manager = context.getSystemService(NotificationManager::class.java)
        if (manager.getNotificationChannel(id) == null) {
            manager.createNotificationChannel(
                NotificationChannel(id, title, NotificationManager.IMPORTANCE_HIGH).apply {
                    enableVibration(true)
                },
            )
        }
        return id
    }

    private fun canPost() =
        context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    suspend fun notifyIncoming(threadId: Long, address: String, body: String, subId: Int, isMms: Boolean) {
        if (!canPost()) return
        if (threadId > 0 && threadSettings.byThread(threadId)?.muted == true) return

        // Privacy toggle: keep sensitive content (OTPs, personal texts) out of
        // the notification shade and lock screen entirely.
        val hide = prefs.hideContent.value
        val recipient = contacts.resolve(address)
        val title = if (hide) "Messages" else recipient.displayName
        val shownBody = if (hide) "New message" else body
        val channel = if (threadId > 0) conversationChannel(threadId, if (hide) "Conversation" else title) else CHANNEL_MESSAGES

        val person = Person.Builder().setName(title).setKey(address).build()
        val style = NotificationCompat.MessagingStyle(Person.Builder().setName("You").build())
            .addMessage(shownBody, System.currentTimeMillis(), person)

        val contentIntent = PendingIntent.getActivity(
            context,
            threadId.toInt(),
            Intent(Intent.ACTION_VIEW).apply {
                setClassName(context, "com.buildwclaude.messages.MainActivity")
                putExtra("thread_id", threadId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val replyInput = RemoteInput.Builder(KEY_REPLY)
            .setLabel(context.getString(R.string.reply_label)).build()
        val replyIntent = PendingIntent.getBroadcast(
            context,
            threadId.toInt(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_REPLY
                putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
                putExtra(NotificationActionReceiver.EXTRA_ADDRESS, address)
                putExtra(NotificationActionReceiver.EXTRA_SUB_ID, subId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        val replyAction = NotificationCompat.Action.Builder(
            R.drawable.ic_corner_up_right, context.getString(R.string.reply_label), replyIntent,
        ).addRemoteInput(replyInput).setAllowGeneratedReplies(false).build()

        val markReadIntent = PendingIntent.getBroadcast(
            context,
            (threadId + 500_000).toInt(),
            Intent(context, NotificationActionReceiver::class.java).apply {
                action = NotificationActionReceiver.ACTION_MARK_READ
                putExtra(NotificationActionReceiver.EXTRA_THREAD_ID, threadId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val markReadAction = NotificationCompat.Action.Builder(
            R.drawable.ic_check, context.getString(R.string.mark_read_label), markReadIntent,
        ).build()

        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF2F80ED.toInt())
            .setStyle(style)
            .setContentTitle(title)
            .setContentText(shownBody)
            .setVisibility(
                if (hide) NotificationCompat.VISIBILITY_SECRET
                else NotificationCompat.VISIBILITY_PRIVATE,
            )
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setGroup(GROUP_KEY)
            .addAction(replyAction)
            .addAction(markReadAction)
            .build()

        nm.notify(threadId.toInt(), notification)

        val summary = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF2F80ED.toInt())
            .setGroup(GROUP_KEY)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .build()
        nm.notify(SUMMARY_ID, summary)
    }

    fun notifySendFailed(messageUri: Uri?) {
        if (!canPost()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_FAILURES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFEB5757.toInt())
            .setContentTitle("Message not sent")
            .setContentText("Tap to open the conversation and retry.")
            .setContentIntent(
                PendingIntent.getActivity(
                    context, 9001,
                    Intent().setClassName(context, "com.buildwclaude.messages.MainActivity"),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
            .setAutoCancel(true)
            .build()
        nm.notify((messageUri?.hashCode() ?: 9001), notification)
    }

    fun notifyScheduledFailed(recipients: String, reason: String) {
        if (!canPost()) return
        val notification = NotificationCompat.Builder(context, CHANNEL_FAILURES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFFEB5757.toInt())
            .setContentTitle("Scheduled message failed")
            .setContentText("To $recipients — $reason")
            .setAutoCancel(true)
            .build()
        nm.notify(("sched$recipients").hashCode(), notification)
    }

    fun cancelForThread(threadId: Long) {
        nm.cancel(threadId.toInt())
    }

    companion object {
        const val CHANNEL_MESSAGES = "messages"
        const val CHANNEL_FAILURES = "failures"
        const val GROUP_KEY = "com.buildwclaude.messages.MESSAGES"
        const val SUMMARY_ID = 0x5150
        const val KEY_REPLY = "key_reply"
    }
}
