package com.fdev.nutrimindai.feature.auth.presentation.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.database.DB_VERSION
import com.fdev.nutrimindai.core.database.seeder.DemoAccountSeeder
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.core.health.HealthConnectManager
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
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferenceManager: PreferenceManager,
    private val profileRepository: ProfileRepository,
    private val firestoreSyncRepository: FirestoreSyncRepository,
    private val demoAccountSeeder: DemoAccountSeeder,
    private val healthConnectManager: HealthConnectManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    fun loginWithEmail() {
        if (_uiState.value is LoginUiState.Loading) return

        val currentEmail = _email.value.trim()
        val currentPassword = _password.value

        if (currentEmail.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your email address.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            _uiState.value = LoginUiState.Error("Please enter a valid email address.")
            return
        }
        if (currentPassword.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading

            // Intercept pre-configured Demo Accounts for judges and video recording
            if (DemoAccountSeeder.isDemoAccount(currentEmail)) {
                if (currentPassword == DemoAccountSeeder.DEMO_PASSWORD) {
                    try {
                        healthConnectManager.disconnect()
                        demoAccountSeeder.seedDemoAccount(currentEmail)
                        preferenceManager.setDemoUserEmail(currentEmail)
                        preferenceManager.setOnboardingCompleted(true)
                        preferenceManager.setSavedDbVersion(DB_VERSION)
                        _uiState.value = LoginUiState.Success(isNewUser = false)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to seed demo account")
                        _uiState.value = LoginUiState.Error("Failed to initialize demo data: ${e.message}")
                    }
                    return@launch
                } else {
                    _uiState.value = LoginUiState.Error("Incorrect password. Please try again.")
                    return@launch
                }
            }

            try {
                val result = authRepository.loginWithEmail(currentEmail, currentPassword)
                result.fold(
                    onSuccess = {
                        try {
                            firestoreSyncRepository.downloadAndRestoreUserData()
                            preferenceManager.setSavedDbVersion(DB_VERSION)
                        } catch (e: Exception) {
                            Timber.e(e, "Failed to pull user data from Firestore on login")
                        }

                        val profile = profileRepository.getProfileDirect()
                        val hasProfile = profile != null

                        if (hasProfile) {
                            preferenceManager.setOnboardingCompleted(true)
                            _uiState.value = LoginUiState.Success(isNewUser = false)
                        } else {
                            _uiState.value = LoginUiState.Success(isNewUser = true)
                        }
                    },
                    onFailure = { error ->
                        _uiState.value = LoginUiState.Error(error.message ?: "Login failed. Please try again.")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during email login")
                _uiState.value = LoginUiState.Error("An unexpected error occurred. Please try again.")
            }
        }
    }

    fun setLoading() {
        _uiState.value = LoginUiState.Loading
    }

    fun setIdle() {
        if (_uiState.value is LoginUiState.Loading) {
            _uiState.value = LoginUiState.Idle
        }
    }

    fun setError(message: String) {
        _uiState.value = LoginUiState.Error(message)
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val result = authRepository.loginWithGoogle(idToken)
                result.fold(
                    onSuccess = {
                        handleGoogleAuthSuccess()
                    },
                    onFailure = { error ->
                        _uiState.value = LoginUiState.Error(error.message ?: "Google login failed. Please try again.")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during Google login")
                _uiState.value = LoginUiState.Error("Google login failed. Please try again.")
            }
        }
    }

    fun handleGoogleAuthSuccess() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                try {
                    firestoreSyncRepository.downloadAndRestoreUserData()
                    preferenceManager.setSavedDbVersion(DB_VERSION)
                } catch (e: Exception) {
                    Timber.e(e, "Failed to pull user data from Firestore on Google login")
                }

                val profile = profileRepository.getProfileDirect()
                val hasProfile = profile != null

                if (hasProfile) {
                    preferenceManager.setOnboardingCompleted(true)
                    _uiState.value = LoginUiState.Success(isNewUser = false)
                } else {
                    _uiState.value = LoginUiState.Success(isNewUser = true)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing profile state after Google login")
                _uiState.value = LoginUiState.Error("Error checking user profile. Please try logging in again.")
            }
        }
    }

    fun loginAnonymously() {
        if (_uiState.value is LoginUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            try {
                val result = authRepository.loginAnonymously()
                result.fold(
                    onSuccess = {
                        _uiState.value = LoginUiState.Success(isNewUser = true)
                    },
                    onFailure = { error ->
                        _uiState.value = LoginUiState.Error(error.message ?: "Guest login failed. Please check your internet connection.")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during guest login")
                _uiState.value = LoginUiState.Error("Guest login failed. Please try again.")
            }
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        val targetEmail = email.trim()
        if (targetEmail.isBlank()) {
            onResult(false, "Please enter your email to reset password.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(targetEmail).matches()) {
            onResult(false, "Please enter a valid email address.")
            return
        }
        viewModelScope.launch {
            try {
                val result = authRepository.sendPasswordReset(targetEmail)
                result.fold(
                    onSuccess = {
                        onResult(true, "Password reset link has been sent to $targetEmail.")
                    },
                    onFailure = { err ->
                        onResult(false, err.message ?: "Failed to send password reset email.")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during password reset")
                onResult(false, "Failed to send password reset email. Please try again.")
            }
        }
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }

    fun resetUiState() {
        _uiState.value = LoginUiState.Idle
    }
}
