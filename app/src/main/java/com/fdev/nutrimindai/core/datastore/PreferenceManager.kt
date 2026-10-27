package com.fdev.nutrimindai.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fdev.nutrimindai.core.util.Constants.PREFERENCES_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCES_NAME)

enum class AppThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

data class ReminderSettings(
    val breakfastEnabled: Boolean = true,
    val breakfastTime: String = "07:30",
    val lunchEnabled: Boolean = true,
    val lunchTime: String = "12:30",
    val dinnerEnabled: Boolean = true,
    val dinnerTime: String = "19:00",
    val waterEnabled: Boolean = true,
    val waterIntervalHours: Float = 2.0f,
    val waterStartTime: String = "08:00",
    val waterEndTime: String = "21:00",
    val dailyReviewEnabled: Boolean = true,
    val dailyReviewTime: String = "21:30",
    val smartSkipIfLogged: Boolean = true,
    val quickLogWaterAmountMl: Int = 250
)

@Singleton
class PreferenceManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    private object PreferencesKeys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val DEMO_SEEDED = booleanPreferencesKey("demo_seeded")
        val THEME_MODE = stringPreferencesKey("theme_mode")

        // Reminder Preferences
        val BREAKFAST_ENABLED = booleanPreferencesKey("reminder_breakfast_enabled")
        val BREAKFAST_TIME = stringPreferencesKey("reminder_breakfast_time")
        val LUNCH_ENABLED = booleanPreferencesKey("reminder_lunch_enabled")
        val LUNCH_TIME = stringPreferencesKey("reminder_lunch_time")
        val DINNER_ENABLED = booleanPreferencesKey("reminder_dinner_enabled")
        val DINNER_TIME = stringPreferencesKey("reminder_dinner_time")

        val WATER_ENABLED = booleanPreferencesKey("reminder_water_enabled")
        val WATER_INTERVAL_MINUTES = intPreferencesKey("reminder_water_interval_minutes")
        val WATER_START_TIME = stringPreferencesKey("reminder_water_start_time")
        val WATER_END_TIME = stringPreferencesKey("reminder_water_end_time")

        val DAILY_REVIEW_ENABLED = booleanPreferencesKey("reminder_daily_review_enabled")
        val DAILY_REVIEW_TIME = stringPreferencesKey("reminder_daily_review_time")

        val SMART_SKIP_IF_LOGGED = booleanPreferencesKey("reminder_smart_skip_if_logged")
        val QUICK_LOG_WATER_AMOUNT = intPreferencesKey("reminder_quick_log_water_amount")

        val ACTIVE_AI_PROVIDER = stringPreferencesKey("active_ai_provider")
    }

    val isOnboardingCompleted: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    val isDemoSeeded: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.DEMO_SEEDED] ?: false
        }

    suspend fun setDemoSeeded(seeded: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEMO_SEEDED] = seeded
        }
    }

    // --- Reminder Flows & Setters ---

    val reminderSettings: Flow<ReminderSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val intervalMinutes = preferences[PreferencesKeys.WATER_INTERVAL_MINUTES] ?: 120
            ReminderSettings(
                breakfastEnabled = preferences[PreferencesKeys.BREAKFAST_ENABLED] ?: true,
                breakfastTime = preferences[PreferencesKeys.BREAKFAST_TIME] ?: "07:30",
                lunchEnabled = preferences[PreferencesKeys.LUNCH_ENABLED] ?: true,
                lunchTime = preferences[PreferencesKeys.LUNCH_TIME] ?: "12:30",
                dinnerEnabled = preferences[PreferencesKeys.DINNER_ENABLED] ?: true,
                dinnerTime = preferences[PreferencesKeys.DINNER_TIME] ?: "19:00",
                waterEnabled = preferences[PreferencesKeys.WATER_ENABLED] ?: true,
                waterIntervalHours = intervalMinutes / 60.0f,
                waterStartTime = preferences[PreferencesKeys.WATER_START_TIME] ?: "08:00",
                waterEndTime = preferences[PreferencesKeys.WATER_END_TIME] ?: "21:00",
                dailyReviewEnabled = preferences[PreferencesKeys.DAILY_REVIEW_ENABLED] ?: true,
                dailyReviewTime = preferences[PreferencesKeys.DAILY_REVIEW_TIME] ?: "21:30",
                smartSkipIfLogged = preferences[PreferencesKeys.SMART_SKIP_IF_LOGGED] ?: true,
                quickLogWaterAmountMl = preferences[PreferencesKeys.QUICK_LOG_WATER_AMOUNT] ?: 250
            )
        }

    suspend fun updateBreakfastReminder(enabled: Boolean, time: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BREAKFAST_ENABLED] = enabled
            preferences[PreferencesKeys.BREAKFAST_TIME] = time
        }
    }

    suspend fun updateLunchReminder(enabled: Boolean, time: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LUNCH_ENABLED] = enabled
            preferences[PreferencesKeys.LUNCH_TIME] = time
        }
    }

    suspend fun updateDinnerReminder(enabled: Boolean, time: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DINNER_ENABLED] = enabled
            preferences[PreferencesKeys.DINNER_TIME] = time
        }
    }

    suspend fun updateWaterReminder(enabled: Boolean, intervalMinutes: Int, startTime: String, endTime: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.WATER_ENABLED] = enabled
            preferences[PreferencesKeys.WATER_INTERVAL_MINUTES] = intervalMinutes
            preferences[PreferencesKeys.WATER_START_TIME] = startTime
            preferences[PreferencesKeys.WATER_END_TIME] = endTime
        }
    }

    suspend fun updateDailyReviewReminder(enabled: Boolean, time: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REVIEW_ENABLED] = enabled
            preferences[PreferencesKeys.DAILY_REVIEW_TIME] = time
        }
    }

    suspend fun updateSmartSkip(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.SMART_SKIP_IF_LOGGED] = enabled
        }
    }

    suspend fun updateQuickLogWaterAmount(amountMl: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.QUICK_LOG_WATER_AMOUNT] = amountMl
        }
    }

    suspend fun clearAllPreferences() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    val activeAiProvider: Flow<String?> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[PreferencesKeys.ACTIVE_AI_PROVIDER]
        }

    suspend fun setActiveAiProvider(providerName: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_AI_PROVIDER] = providerName
        }
    }

    val themeMode: Flow<AppThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val modeStr = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
            try {
                AppThemeMode.valueOf(modeStr)
            } catch (e: Exception) {
                AppThemeMode.SYSTEM
            }
        }

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }
}
