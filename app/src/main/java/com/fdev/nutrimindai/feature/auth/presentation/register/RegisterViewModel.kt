package com.fdev.nutrimindai.feature.auth.presentation.register

import android.util.Patterns
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

    fun register() {
        if (_uiState.value is RegisterUiState.Loading) return

        val currentName = _name.value.trim()
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value

        if (currentName.isBlank()) {
            _uiState.value = RegisterUiState.Error("Please enter your full name.")
            return
        }
        if (currentEmail.isBlank()) {
            _uiState.value = RegisterUiState.Error("Please enter your email address.")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(currentEmail).matches()) {
            _uiState.value = RegisterUiState.Error("Please enter a valid email address.")
            return
        }
        if (currentPassword.length < 6) {
            _uiState.value = RegisterUiState.Error("Password must be at least 6 characters.")
            return
        }

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val result = authRepository.registerWithEmail(currentName, currentEmail, currentPassword)
                result.fold(
                    onSuccess = {
                        _uiState.value = RegisterUiState.Success(isNewUser = true)
                    },
                    onFailure = { error ->
                        _uiState.value = RegisterUiState.Error(error.message ?: "Registration failed. Please try again.")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during registration")
                _uiState.value = RegisterUiState.Error("An unexpected error occurred. Please try again.")
            }
        }
    }

    fun setLoading() {
        _uiState.value = RegisterUiState.Loading
    }

    fun setIdle() {
        if (_uiState.value is RegisterUiState.Loading) {
            _uiState.value = RegisterUiState.Idle
        }
    }

    fun setError(message: String) {
        _uiState.value = RegisterUiState.Error(message)
    }

    fun loginWithGoogle(idToken: String) {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val result = authRepository.loginWithGoogle(idToken)
                result.fold(
                    onSuccess = {
                        handleGoogleAuthSuccess()
                    },
                    onFailure = { error ->
                        _uiState.value = RegisterUiState.Error(error.message ?: "Google registration failed. Please try again.")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during Google login")
                _uiState.value = RegisterUiState.Error("Google registration failed. Please try again.")
            }
        }
    }

    fun handleGoogleAuthSuccess() {
        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
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
                    _uiState.value = RegisterUiState.Success(isNewUser = false)
                } else {
                    _uiState.value = RegisterUiState.Success(isNewUser = true)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing profile state after Google sign in")
                _uiState.value = RegisterUiState.Error("Error checking user profile. Please try logging in again.")
            }
        }
    }

    fun loginAnonymously() {
        if (_uiState.value is RegisterUiState.Loading) return

        viewModelScope.launch {
            _uiState.value = RegisterUiState.Loading
            try {
                val result = authRepository.loginAnonymously()
                result.fold(
                    onSuccess = {
                        _uiState.value = RegisterUiState.Success(isNewUser = true)
                    },
                    onFailure = { error ->
                        _uiState.value = RegisterUiState.Error(error.message ?: "Guest login failed.")
                    }
                )
            } catch (e: Exception) {
                Timber.e(e, "Unexpected error during guest login")
                _uiState.value = RegisterUiState.Error("Guest login failed. Please try again.")
            }
        }
    }

    fun clearError() {
        if (_uiState.value is RegisterUiState.Error) {
            _uiState.value = RegisterUiState.Idle
        }
    }

    fun resetUiState() {
        _uiState.value = RegisterUiState.Idle
    }
}
