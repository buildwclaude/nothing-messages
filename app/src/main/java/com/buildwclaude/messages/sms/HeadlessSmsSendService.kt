package com.buildwclaude.messages.sms

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.telephony.TelephonyManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles "reply with message" from the incoming-call screen
 * (android.intent.action.RESPOND_VIA_MESSAGE). Required for the SMS role.
 */
@AndroidEntryPoint
class HeadlessSmsSendService : Service() {

    @Inject lateinit var smsSender: SmsSender

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == TelephonyManager.ACTION_RESPOND_VIA_MESSAGE) {
            val recipient = intent.data?.schemeSpecificPart
            val message = intent.getStringExtra(Intent.EXTRA_TEXT)
            if (!recipient.isNullOrBlank() && !message.isNullOrBlank()) {
                scope.launch {
                    smsSender.sendText(recipient, message, subId = -1)
                    stopSelf(startId)
                }
                return START_NOT_STICKY
            }
        }
        stopSelf(startId)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
