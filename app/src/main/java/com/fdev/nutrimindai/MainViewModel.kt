package com.fdev.nutrimindai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.core.navigation.NavigationDestination
import com.fdev.nutrimindai.core.sync.FirestoreSyncRepository
import com.fdev.nutrimindai.feature.auth.domain.repository.AuthRepository
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

import com.fdev.nutrimindai.core.datastore.AppThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MainViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferenceManager: PreferenceManager,
    private val profileRepository: ProfileRepository,
    private val firestoreSyncRepository: FirestoreSyncRepository
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = preferenceManager.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppThemeMode.SYSTEM
        )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _startDestination = MutableStateFlow<NavigationDestination>(NavigationDestination.Login)
    val startDestination: StateFlow<NavigationDestination> = _startDestination.asStateFlow()

    private val _pendingDeepLink = MutableStateFlow<String?>(null)
    val pendingDeepLink: StateFlow<String?> = _pendingDeepLink.asStateFlow()

    init {
        determineStartDestination()
    }

    fun handleIntent(intent: android.content.Intent?) {
        val navExtra = intent?.getStringExtra(com.fdev.nutrimindai.core.notification.NotificationHelper.EXTRA_NAVIGATE_TO)
        if (!navExtra.isNullOrBlank()) {
            _pendingDeepLink.value = navExtra
        }
    }

    fun clearPendingDeepLink() {
        _pendingDeepLink.value = null
    }

    fun determineStartDestination() {
        viewModelScope.launch {
            val isLoggedIn = authRepository.isUserLoggedIn()
            if (!isLoggedIn) {
                _startDestination.value = NavigationDestination.Login
            } else {
                var profile = profileRepository.getProfileDirect()
                if (profile == null) {
                    try {
                        firestoreSyncRepository.downloadAndRestoreUserData()
                        profile = profileRepository.getProfileDirect()
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to download user data on start")
                    }
                }

                val hasProfile = profile != null
                if (hasProfile) {
                    preferenceManager.setOnboardingCompleted(true)
                    _startDestination.value = NavigationDestination.Home
                } else {
                    _startDestination.value = NavigationDestination.Onboarding
                }
            }
            _isLoading.value = false
        }
    }
}
