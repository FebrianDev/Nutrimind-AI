package com.fdev.nutrimindai.feature.auth.presentation.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.feature.auth.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<RegisterUiState>(RegisterUiState.Idle)
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _name = MutableStateFlow("")
    val name: StateFlow<String> = _name.asStateFlow()

    private val _email = MutableStateFlow("")
    val email: StateFlow<String> = _email.asStateFlow()

    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    fun onNameChange(newName: String) {
        _name.value = newName
    }

    fun onEmailChange(newEmail: String) {
        _email.value = newEmail
    }

    fun onPasswordChange(newPassword: String) {
        _password.value = newPassword
    }

    fun onConfirmPasswordChange(newConfirmPassword: String) {
        _confirmPassword.value = newConfirmPassword
    }

    fun register(onNavigateToOnboarding: () -> Unit) {
        val currentName = _name.value.trim()
        val currentEmail = _email.value.trim()
        val currentPassword = _password.value
        val currentConfirm = _confirmPassword.value

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
        if (currentPassword != currentConfirm) {
            _uiState.value = RegisterUiState.Error("Passwords do not match.")
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
