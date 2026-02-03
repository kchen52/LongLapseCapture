package dev.ktown.longlapsecapture.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dev.ktown.longlapsecapture.MainActivity
import dev.ktown.longlapsecapture.R
import dev.ktown.longlapsecapture.di.ServiceLocator

class DailyReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val projectId = inputData.getString(KEY_PROJECT_ID) ?: return Result.failure()
        val projectName = inputData.getString(KEY_PROJECT_NAME) ?: "your project"

        val repository = ServiceLocator.repository()
        if (repository.hasCaptureForToday(projectId)) {
            return Result.success()
        }

        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = CHANNEL_ID
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Daily Longlapse Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_PROJECT_ID, projectId)
            putExtra(MainActivity.EXTRA_OPEN_CAMERA, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            projectId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Time for your daily photo")
            .setContentText("Capture today's photo for $projectName.")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(projectId.hashCode(), notification)
        return Result.success()
    }

    companion object {
        const val KEY_PROJECT_ID = "project_id"
        const val KEY_PROJECT_NAME = "project_name"
        const val CHANNEL_ID = "longlapse_daily_reminders"
    }
}
