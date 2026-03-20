package com.nannymeals.app.notifications

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class ReminderSettings(
    val lunchEnabled: Boolean = false,
    val lunchHour: Int = 12,
    val lunchMinute: Int = 0,
    val snackEnabled: Boolean = false,
    val snackHour: Int = 16,
    val snackMinute: Int = 0
)

class SettingsRepository(private val context: Context) {

    companion object {
        val LUNCH_REMINDER_ENABLED = booleanPreferencesKey("lunch_reminder_enabled")
        val LUNCH_REMINDER_HOUR = intPreferencesKey("lunch_reminder_hour")
        val LUNCH_REMINDER_MINUTE = intPreferencesKey("lunch_reminder_minute")
        
        val SNACK_REMINDER_ENABLED = booleanPreferencesKey("snack_reminder_enabled")
        val SNACK_REMINDER_HOUR = intPreferencesKey("snack_reminder_hour")
        val SNACK_REMINDER_MINUTE = intPreferencesKey("snack_reminder_minute")
    }

    val reminderSettings: Flow<ReminderSettings> = context.settingsDataStore.data.map { preferences ->
        ReminderSettings(
            lunchEnabled = preferences[LUNCH_REMINDER_ENABLED] ?: false,
            lunchHour = preferences[LUNCH_REMINDER_HOUR] ?: 12,
            lunchMinute = preferences[LUNCH_REMINDER_MINUTE] ?: 0,
            snackEnabled = preferences[SNACK_REMINDER_ENABLED] ?: false,
            snackHour = preferences[SNACK_REMINDER_HOUR] ?: 16,
            snackMinute = preferences[SNACK_REMINDER_MINUTE] ?: 0
        )
    }

    suspend fun updateLunchReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[LUNCH_REMINDER_ENABLED] = enabled
            preferences[LUNCH_REMINDER_HOUR] = hour
            preferences[LUNCH_REMINDER_MINUTE] = minute
        }
        
        if (enabled) {
            MealReminderWorker.scheduleReminder(context, "LUNCH", hour, minute)
        } else {
            MealReminderWorker.cancelReminder(context, "LUNCH")
        }
    }

    suspend fun updateSnackReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.settingsDataStore.edit { preferences ->
            preferences[SNACK_REMINDER_ENABLED] = enabled
            preferences[SNACK_REMINDER_HOUR] = hour
            preferences[SNACK_REMINDER_MINUTE] = minute
        }
        
        if (enabled) {
            MealReminderWorker.scheduleReminder(context, "SNACK", hour, minute)
        } else {
            MealReminderWorker.cancelReminder(context, "SNACK")
        }
    }
}
