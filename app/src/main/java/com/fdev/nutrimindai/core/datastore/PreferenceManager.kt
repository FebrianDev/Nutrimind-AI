package com.fdev.nutrimindai.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fdev.nutrimindai.core.util.Constants.PREFERENCES_NAME
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
        val DB_VERSION = intPreferencesKey("saved_db_version")
        val DEMO_USER_EMAIL = stringPreferencesKey("demo_user_email")
        val PENDING_CLOUD_DELETIONS = stringSetPreferencesKey("pending_cloud_deletions")
        val PENDING_CLOUD_RESET = booleanPreferencesKey("pending_cloud_reset")
        val HEALTH_CONNECT_CONNECTED = booleanPreferencesKey("health_connect_connected")
    }

    private val safePreferencesFlow: Flow<Preferences> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    // In-memory cache keeping the latest Preferences snapshot in RAM
    private val cachedPreferences: StateFlow<Preferences?> = safePreferencesFlow
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

    private suspend fun getSnapshot(): Preferences {
        return cachedPreferences.value ?: safePreferencesFlow.firstOrNull() ?: emptyPreferences()
    }

    private fun Preferences.toReminderSettings(): ReminderSettings {
        val intervalMinutes = this[PreferencesKeys.WATER_INTERVAL_MINUTES] ?: 120
        return ReminderSettings(
            breakfastEnabled = this[PreferencesKeys.BREAKFAST_ENABLED] ?: true,
            breakfastTime = this[PreferencesKeys.BREAKFAST_TIME] ?: "07:30",
            lunchEnabled = this[PreferencesKeys.LUNCH_ENABLED] ?: true,
            lunchTime = this[PreferencesKeys.LUNCH_TIME] ?: "12:30",
            dinnerEnabled = this[PreferencesKeys.DINNER_ENABLED] ?: true,
            dinnerTime = this[PreferencesKeys.DINNER_TIME] ?: "19:00",
            waterEnabled = this[PreferencesKeys.WATER_ENABLED] ?: true,
            waterIntervalHours = intervalMinutes / 60.0f,
            waterStartTime = this[PreferencesKeys.WATER_START_TIME] ?: "08:00",
            waterEndTime = this[PreferencesKeys.WATER_END_TIME] ?: "21:00",
            dailyReviewEnabled = this[PreferencesKeys.DAILY_REVIEW_ENABLED] ?: true,
            dailyReviewTime = this[PreferencesKeys.DAILY_REVIEW_TIME] ?: "21:30",
            smartSkipIfLogged = this[PreferencesKeys.SMART_SKIP_IF_LOGGED] ?: true,
            quickLogWaterAmountMl = this[PreferencesKeys.QUICK_LOG_WATER_AMOUNT] ?: 250
        )
    }

    val isOnboardingCompleted: Flow<Boolean> = safePreferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] ?: false
        }
        .distinctUntilChanged()

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
        }
    }

    val isDemoSeeded: Flow<Boolean> = safePreferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DEMO_SEEDED] ?: false
        }
        .distinctUntilChanged()

    suspend fun setDemoSeeded(seeded: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DEMO_SEEDED] = seeded
        }
    }

    val savedDbVersion: Flow<Int?> = safePreferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.DB_VERSION]
        }
        .distinctUntilChanged()

    suspend fun getSavedDbVersion(): Int? {
        return getSnapshot()[PreferencesKeys.DB_VERSION]
    }

    suspend fun setSavedDbVersion(version: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.DB_VERSION] = version
        }
    }

    suspend fun setOnboardingAndDbVersion(completed: Boolean, version: Int) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ONBOARDING_COMPLETED] = completed
            preferences[PreferencesKeys.DB_VERSION] = version
        }
    }

    val demoUserEmailFlow: Flow<String?> = safePreferencesFlow
        .map { it[PreferencesKeys.DEMO_USER_EMAIL] }
        .distinctUntilChanged()

    fun getDemoUserEmailBlocking(): String? {
        return cachedPreferences.value?.get(PreferencesKeys.DEMO_USER_EMAIL)
    }

    suspend fun getDemoUserEmail(): String? {
        return getSnapshot()[PreferencesKeys.DEMO_USER_EMAIL]
    }

    suspend fun setDemoUserEmail(email: String?) {
        dataStore.edit { preferences ->
            if (email != null) {
                preferences[PreferencesKeys.DEMO_USER_EMAIL] = email
            } else {
                preferences.remove(PreferencesKeys.DEMO_USER_EMAIL)
            }
        }
    }

    suspend fun getPendingCloudDeletions(): Set<String> {
        return getSnapshot()[PreferencesKeys.PENDING_CLOUD_DELETIONS] ?: emptySet()
    }

    suspend fun queueCloudDeletion(collection: String, documentIds: List<String>) {
        val validEntries = documentIds
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .map { "$collection:$it" }
        if (validEntries.isEmpty()) return

        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.PENDING_CLOUD_DELETIONS] ?: emptySet()
            preferences[PreferencesKeys.PENDING_CLOUD_DELETIONS] = current + validEntries
        }
    }

    suspend fun removePendingCloudDeletions(entries: Set<String>) {
        if (entries.isEmpty()) return
        dataStore.edit { preferences ->
            val current = preferences[PreferencesKeys.PENDING_CLOUD_DELETIONS] ?: emptySet()
            preferences[PreferencesKeys.PENDING_CLOUD_DELETIONS] = current - entries
        }
    }

    suspend fun isPendingCloudReset(): Boolean {
        return getSnapshot()[PreferencesKeys.PENDING_CLOUD_RESET] ?: false
    }

    suspend fun setPendingCloudReset(pending: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.PENDING_CLOUD_RESET] = pending
            if (pending) {
                preferences.remove(PreferencesKeys.PENDING_CLOUD_DELETIONS)
            }
        }
    }

    val isHealthConnectConnected: Flow<Boolean> = safePreferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.HEALTH_CONNECT_CONNECTED] ?: false
        }
        .distinctUntilChanged()

    suspend fun isHealthConnectConnected(): Boolean {
        return getSnapshot()[PreferencesKeys.HEALTH_CONNECT_CONNECTED] ?: false
    }

    suspend fun setHealthConnectConnected(connected: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.HEALTH_CONNECT_CONNECTED] = connected
        }
    }

    // --- Reminder Flows & Setters ---

    val reminderSettings: Flow<ReminderSettings> = safePreferencesFlow
        .map { preferences -> preferences.toReminderSettings() }
        .distinctUntilChanged()

    suspend fun getReminderSettingsDirect(): ReminderSettings {
        return getSnapshot().toReminderSettings()
    }

    suspend fun updateAllReminderSettings(settings: ReminderSettings) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.BREAKFAST_ENABLED] = settings.breakfastEnabled
            preferences[PreferencesKeys.BREAKFAST_TIME] = settings.breakfastTime
            preferences[PreferencesKeys.LUNCH_ENABLED] = settings.lunchEnabled
            preferences[PreferencesKeys.LUNCH_TIME] = settings.lunchTime
            preferences[PreferencesKeys.DINNER_ENABLED] = settings.dinnerEnabled
            preferences[PreferencesKeys.DINNER_TIME] = settings.dinnerTime
            preferences[PreferencesKeys.WATER_ENABLED] = settings.waterEnabled
            preferences[PreferencesKeys.WATER_INTERVAL_MINUTES] =
                (settings.waterIntervalHours * 60).toInt().coerceAtLeast(30)
            preferences[PreferencesKeys.WATER_START_TIME] = settings.waterStartTime
            preferences[PreferencesKeys.WATER_END_TIME] = settings.waterEndTime
            preferences[PreferencesKeys.DAILY_REVIEW_ENABLED] = settings.dailyReviewEnabled
            preferences[PreferencesKeys.DAILY_REVIEW_TIME] = settings.dailyReviewTime
            preferences[PreferencesKeys.SMART_SKIP_IF_LOGGED] = settings.smartSkipIfLogged
            preferences[PreferencesKeys.QUICK_LOG_WATER_AMOUNT] = settings.quickLogWaterAmountMl
        }
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

    val activeAiProvider: Flow<String?> = safePreferencesFlow
        .map { preferences ->
            preferences[PreferencesKeys.ACTIVE_AI_PROVIDER]
        }
        .distinctUntilChanged()

    suspend fun setActiveAiProvider(providerName: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.ACTIVE_AI_PROVIDER] = providerName
        }
    }

    val themeMode: Flow<AppThemeMode> = safePreferencesFlow
        .map { preferences ->
            val modeStr = preferences[PreferencesKeys.THEME_MODE] ?: AppThemeMode.SYSTEM.name
            try {
                AppThemeMode.valueOf(modeStr)
            } catch (e: Exception) {
                AppThemeMode.SYSTEM
            }
        }
        .distinctUntilChanged()

    suspend fun setThemeMode(mode: AppThemeMode) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }
}
