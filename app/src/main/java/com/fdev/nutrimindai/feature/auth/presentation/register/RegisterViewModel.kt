package com.fdev.nutrimindai.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.database.DB_VERSION
import com.fdev.nutrimindai.core.datastore.PreferenceManager
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

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferenceManager: PreferenceManager,
    private val profileRepository: ProfileRepository,
    private val firestoreSyncRepository: FirestoreSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun onNameChange(newName: String) {
        _name.value = newName
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun register(onNavigateToOnboarding: () -> Unit) {
        val currentName = _name.value.trim()
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value

        if (currentName.isBlank()) {
            _uiState.value = RegisterUiState.Error("Please enter your full name.")
            return
        }
        if (currentEmail.isBlank()) {
            _uiState.value = RegisterUiState.Error("Please enter your email.")
            return
        }
        if (currentPassword.length < 6) {
            _uiState.value = RegisterUiState.Error("Password must be at least 6 characters.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            val result = authRepository.registerWithEmail(currentName, currentEmail, currentPassword)
            result.fold(
                onSuccess = {
                    _uiState.value = RegisterUiState.Success
                    onNavigateToOnboarding()
                },
                onFailure = { error ->
                    _uiState.value = RegisterUiState.Error(error.message ?: "Registration failed. Please try again.")
                }
            )
        }
    }

    fun loginWithGoogle(
        idToken: String,
        onNavigateToHome: () -> Unit,
        onNavigateToOnboarding: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            val result = authRepository.loginWithGoogle(idToken)
            result.fold(
                onSuccess = {
                    handleGoogleAuthSuccess(onNavigateToHome, onNavigateToOnboarding)
                },
                onFailure = { error ->
                    _uiState.value = RegisterUiState.Error(error.message ?: "Google registration failed. Please try again.")
                }
            )
        }
    }

    fun handleGoogleAuthSuccess(
        onNavigateToHome: () -> Unit,
        onNavigateToOnboarding: () -> Unit
    ) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                firestoreSyncRepository.downloadAndRestoreUserData()
                preferenceManager.setSavedDbVersion(DB_VERSION)
            } catch (e: Exception) {
                Timber.e(e, "Failed to pull user data from Firestore on Google registration")
            }

            val profile = profileRepository.getProfileDirect()
            val hasProfile = profile != null

            if (hasProfile) {
                preferenceManager.setOnboardingCompleted(true)
                _uiState.value = RegisterUiState.Success
                onNavigateToHome()
            } else {
                _uiState.value = RegisterUiState.Success
                onNavigateToOnboarding()
            }
        }
    }


    fun loginAnonymously(onNavigateToOnboarding: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            val result = authRepository.loginAnonymously()
            result.fold(
                onSuccess = {
                    _uiState.value = RegisterUiState.Success
                    onNavigateToOnboarding()
                },
                onFailure = { error ->
                    _uiState.value = RegisterUiState.Error(error.message ?: "Guest login failed.")
                }
            )
        }
    }

    fun clearError() {
        if (_uiState.value is RegisterUiState.Error) {
            _uiState.value = RegisterUiState.Idle
        }
    }
}
