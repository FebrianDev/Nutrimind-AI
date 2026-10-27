package com.fdev.nutrimindai.feature.profile.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.core.datastore.ReminderSettings
import com.fdev.nutrimindai.core.notification.ReminderScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReminderSettingsViewModel @Inject constructor(
    private val preferenceManager: PreferenceManager,
    private val reminderScheduler: ReminderScheduler
) : ViewModel() {

    val reminderSettings: StateFlow<ReminderSettings> = preferenceManager.reminderSettings
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReminderSettings()
        )

    fun updateBreakfast(enabled: Boolean, time: String) {
        viewModelScope.launch {
            preferenceManager.updateBreakfastReminder(enabled, time)
            val current = reminderSettings.value.copy(breakfastEnabled = enabled, breakfastTime = time)
            reminderScheduler.applyReminderSettings(current)
        }
    }

    fun updateLunch(enabled: Boolean, time: String) {
        viewModelScope.launch {
            preferenceManager.updateLunchReminder(enabled, time)
            val current = reminderSettings.value.copy(lunchEnabled = enabled, lunchTime = time)
            reminderScheduler.applyReminderSettings(current)
        }
    }

    fun updateDinner(enabled: Boolean, time: String) {
        viewModelScope.launch {
            preferenceManager.updateDinnerReminder(enabled, time)
            val current = reminderSettings.value.copy(dinnerEnabled = enabled, dinnerTime = time)
            reminderScheduler.applyReminderSettings(current)
        }
    }

    fun updateWater(enabled: Boolean, intervalHours: Float, startTime: String, endTime: String) {
        viewModelScope.launch {
            val intervalMinutes = (intervalHours * 60).toInt().coerceAtLeast(30)
            preferenceManager.updateWaterReminder(enabled, intervalMinutes, startTime, endTime)
            val current = reminderSettings.value.copy(
                waterEnabled = enabled,
                waterIntervalHours = intervalHours,
                waterStartTime = startTime,
                waterEndTime = endTime
            )
            reminderScheduler.applyReminderSettings(current)
        }
    }

    fun updateDailyReview(enabled: Boolean, time: String) {
        viewModelScope.launch {
            preferenceManager.updateDailyReviewReminder(enabled, time)
            val current = reminderSettings.value.copy(dailyReviewEnabled = enabled, dailyReviewTime = time)
            reminderScheduler.applyReminderSettings(current)
        }
    }

    fun updateSmartSkip(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.updateSmartSkip(enabled)
            val current = reminderSettings.value.copy(smartSkipIfLogged = enabled)
            reminderScheduler.applyReminderSettings(current)
        }
    }

    fun updateQuickLogAmount(amountMl: Int) {
        viewModelScope.launch {
            preferenceManager.updateQuickLogWaterAmount(amountMl)
        }
    }

    fun sendTestNotification() {
        reminderScheduler.sendTestNotification()
    }

    fun sendTestWaterNotification() {
        val amount = reminderSettings.value.quickLogWaterAmountMl
        reminderScheduler.sendTestWaterNotification(amount)
    }

    fun sendTestMealNotification(mealType: String = "lunch") {
        reminderScheduler.sendTestMealNotification(mealType)
    }
}
