package com.fitlife.app.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.fitlife.app.data.repository.FitnessRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDate
import java.util.concurrent.TimeUnit

// ── Daily step sync ───────────────────────────────────────────────────────────
@HiltWorker
class DailySyncWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: FitnessRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result = try {
        repo.syncStepsFromHealthConnect()
        Result.success()
    } catch (e: Exception) { Result.retry() }
}

// ── Step reminder at 7 PM ─────────────────────────────────────────────────────
@HiltWorker
class StepReminderWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: FitnessRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val profile = repo.getProfile() ?: return Result.success()
        val today = repo.getTodaySteps()
        val steps = today?.steps ?: 0
        if (steps < profile.dailyStepGoal * 0.8f) {
            sendNotification(
                title = "Keep moving! 🚶",
                body = "%,d steps done — %,d to go!".format(steps, profile.dailyStepGoal - steps)
            )
        }
        return Result.success()
    }

    private fun sendNotification(title: String, body: String) {
        val nm = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel("fitlife_steps", "Step Reminders", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        nm.notify(1001, NotificationCompat.Builder(applicationContext, "fitlife_steps")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle(title).setContentText(body)
            .setAutoCancel(true).build())
    }
}

// ── Water reminder every 2 hours ──────────────────────────────────────────────
@HiltWorker
class WaterReminderWorker @AssistedInject constructor(
    @Assisted ctx: Context,
    @Assisted params: WorkerParameters,
    private val repo: FitnessRepository
) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val today = repo.getWellbeingByDate(LocalDate.now().toString())
        if ((today?.waterGlasses ?: 0) < 4) {
            val nm = applicationContext.getSystemService(NotificationManager::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                nm.createNotificationChannel(
                    NotificationChannel("fitlife_reminders", "Reminders", NotificationManager.IMPORTANCE_DEFAULT)
                )
            nm.notify(1002, NotificationCompat.Builder(applicationContext, "fitlife_reminders")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Stay hydrated! 💧")
                .setContentText("Don't forget to drink water today.")
                .setAutoCancel(true).build())
        }
        return Result.success()
    }
}

// ── Scheduler — called from FitLifeApp.onCreate ───────────────────────────────
object WorkerScheduler {
    fun schedule(context: Context) {
        val wm = WorkManager.getInstance(context)

        wm.enqueueUniquePeriodicWork(
            "daily_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<DailySyncWorker>(1, TimeUnit.HOURS)
                .setConstraints(Constraints(requiredNetworkType = NetworkType.CONNECTED))
                .build()
        )

        wm.enqueueUniquePeriodicWork(
            "step_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<StepReminderWorker>(1, TimeUnit.DAYS)
                .setInitialDelay(delayUntilHour(19), TimeUnit.MILLISECONDS)
                .build()
        )

        wm.enqueueUniquePeriodicWork(
            "water_reminder",
            ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<WaterReminderWorker>(2, TimeUnit.HOURS).build()
        )
    }

    private fun delayUntilHour(hour: Int): Long {
        val now = java.util.Calendar.getInstance()
        val target = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            if (before(now)) add(java.util.Calendar.DAY_OF_MONTH, 1)
        }
        return target.timeInMillis - now.timeInMillis
    }
}
