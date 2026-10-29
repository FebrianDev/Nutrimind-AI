package com.fdev.nutrimindai.feature.auth.presentation.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.feature.auth.domain.repository.AuthRepository
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.fdev.nutrimindai.core.database.DB_VERSION
import com.fdev.nutrimindai.core.sync.FirestoreSyncRepository
import timber.log.Timber

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val preferenceManager: PreferenceManager,
    private val profileRepository: ProfileRepository,
    private val firestoreSyncRepository: FirestoreSyncRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun loginWithEmail(
        onNavigateToHome: () -> Unit,
        onNavigateToOnboarding: () -> Unit
    ) {
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value

        if (currentEmail.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your email.")
            return
        }
        if (currentPassword.isBlank()) {
            _uiState.value = LoginUiState.Error("Please enter your password.")
            return
        }

        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
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
                        onNavigateToHome()
                    } else {
                        _uiState.value = LoginUiState.Success(isNewUser = true)
                        onNavigateToOnboarding()
                    }
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(error.message ?: "Login failed. Please try again.")
                }
            )
        }
    }

    fun loginAnonymously(onNavigateToOnboarding: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            val result = authRepository.loginAnonymously()
            result.fold(
                onSuccess = {
                    _uiState.value = LoginUiState.Success(isNewUser = true)
                    onNavigateToOnboarding()
                },
                onFailure = { error ->
                    _uiState.value = LoginUiState.Error(error.message ?: "Guest login failed. Please check your internet connection.")
                }
            )
        }
    }

    fun resetPassword(email: String, onResult: (Boolean, String) -> Unit) {
        val targetEmail = email.trim()
        if (targetEmail.isBlank()) {
            onResult(false, "Please enter your email to reset password.")
            return
        }
        viewModelScope.launch {
            val result = authRepository.sendPasswordReset(targetEmail)
            result.fold(
                onSuccess = {
                    onResult(true, "Password reset link has been sent to $targetEmail.")
                },
                onFailure = { err ->
                    onResult(false, err.message ?: "Failed to send password reset email.")
                }
            )
        }
    }

    fun clearError() {
        if (_uiState.value is LoginUiState.Error) {
            _uiState.value = LoginUiState.Idle
        }
    }
}
