package com.nannymeals.app.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.nannymeals.app.MainActivity
import com.nannymeals.app.NannyMealsApplication
import com.nannymeals.app.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class MealReminderWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val mealType = inputData.getString(KEY_MEAL_TYPE) ?: "meal"
        
        showNotification(mealType)
        
        return Result.success()
    }

    private fun showNotification(mealType: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra(EXTRA_NAVIGATE_TO_MEALS, true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, NannyMealsApplication.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(context.getString(R.string.reminder_title))
            .setContentText("C'est l'heure d'enregistrer le ${mealType.lowercase()} pour les enfants !")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                NOTIFICATION_ID + mealType.hashCode(),
                notification
            )
        } catch (e: SecurityException) {
            // Notification permission not granted
        }
    }

    companion object {
        const val KEY_MEAL_TYPE = "meal_type"
        const val EXTRA_NAVIGATE_TO_MEALS = "navigate_to_meals"
        private const val NOTIFICATION_ID = 1000

        fun scheduleReminder(
            context: Context,
            mealType: String,
            hour: Int,
            minute: Int
        ) {
            val workManager = WorkManager.getInstance(context)
            
            // Calculate delay until the specified time
            val currentTime = java.util.Calendar.getInstance()
            val targetTime = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, hour)
                set(java.util.Calendar.MINUTE, minute)
                set(java.util.Calendar.SECOND, 0)
            }
            
            // If the target time has passed today, schedule for tomorrow
            if (targetTime.before(currentTime)) {
                targetTime.add(java.util.Calendar.DAY_OF_MONTH, 1)
            }
            
            val delayMillis = targetTime.timeInMillis - currentTime.timeInMillis

            val inputData = Data.Builder()
                .putString(KEY_MEAL_TYPE, mealType)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<MealReminderWorker>(
                1, TimeUnit.DAYS
            )
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag("${mealType}_reminder")
                .build()

            workManager.enqueueUniquePeriodicWork(
                "${mealType}_reminder",
                ExistingPeriodicWorkPolicy.UPDATE,
                workRequest
            )
        }

        fun cancelReminder(context: Context, mealType: String) {
            WorkManager.getInstance(context)
                .cancelUniqueWork("${mealType}_reminder")
        }

        fun cancelAllReminders(context: Context) {
            WorkManager.getInstance(context).cancelAllWorkByTag("meal_reminder")
        }
    }
}
