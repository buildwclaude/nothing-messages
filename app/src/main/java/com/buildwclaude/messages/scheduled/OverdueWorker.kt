package com.buildwclaude.messages.scheduled

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Backstop for the exact alarms: periodically sweeps for scheduled messages
 * whose send time has passed without firing (e.g. exact alarms revoked).
 */
@HiltWorker
class OverdueWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val manager: ScheduledSendManager,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        manager.fireOverdue()
        return Result.success()
    }

    companion object {
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "overdue-scheduled-sweep",
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<OverdueWorker>(6, TimeUnit.HOURS).build(),
            )
        }
    }
}
