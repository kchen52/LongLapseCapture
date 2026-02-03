package dev.ktown.longlapsecapture.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object ReminderScheduler {
    fun scheduleDailyReminder(
        context: Context,
        projectId: String,
        projectName: String,
        hour: Int,
        minute: Int
    ) {
        val now = LocalDateTime.now()
        var next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        if (next.isBefore(now)) {
            next = next.plusDays(1)
        }
        val delayMs = Duration.between(now, next).toMillis()
        val request = PeriodicWorkRequestBuilder<DailyReminderWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    DailyReminderWorker.KEY_PROJECT_ID to projectId,
                    DailyReminderWorker.KEY_PROJECT_NAME to projectName
                )
            )
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "daily_reminder_$projectId",
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }
}
