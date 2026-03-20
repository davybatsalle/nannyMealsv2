package com.nannymeals.app.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            // Reschedule reminders after device reboot
            CoroutineScope(Dispatchers.IO).launch {
                rescheduleReminders(context)
            }
        }
    }

    private suspend fun rescheduleReminders(context: Context) {
        val dataStore = context.settingsDataStore
        val preferences = dataStore.data.first()

        val lunchEnabled = preferences[booleanPreferencesKey("lunch_reminder_enabled")] ?: false
        val snackEnabled = preferences[booleanPreferencesKey("snack_reminder_enabled")] ?: false

        if (lunchEnabled) {
            val hour = preferences[intPreferencesKey("lunch_reminder_hour")] ?: 12
            val minute = preferences[intPreferencesKey("lunch_reminder_minute")] ?: 0
            MealReminderWorker.scheduleReminder(context, "LUNCH", hour, minute)
        }

        if (snackEnabled) {
            val hour = preferences[intPreferencesKey("snack_reminder_hour")] ?: 16
            val minute = preferences[intPreferencesKey("snack_reminder_minute")] ?: 0
            MealReminderWorker.scheduleReminder(context, "SNACK", hour, minute)
        }
    }
}
