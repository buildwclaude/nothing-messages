package com.buildwclaude.messages.scheduled

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class ScheduledSendReceiver : BroadcastReceiver() {

    @Inject lateinit var manager: ScheduledSendManager

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_FIRE) return
        val id = intent.getLongExtra(EXTRA_ID, -1L)
        if (id <= 0) return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                manager.fire(id)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        const val ACTION_FIRE = "com.buildwclaude.messages.SCHEDULED_FIRE"
        const val EXTRA_ID = "scheduled_id"
    }
}

/**
 * Alarms do not survive reboots; this re-arms all pending scheduled sends after
 * boot, app update, or clock/timezone changes (sendAt is stored as an absolute
 * epoch instant, so timezone/DST changes never shift the actual send moment).
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var manager: ScheduledSendManager

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_TIME_CHANGED,
            -> {
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        manager.rearmAll()
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }
}
