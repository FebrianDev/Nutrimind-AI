package com.fdev.nutrimindai.feature.auth.presentation.login

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Success(val isNewUser: Boolean) : LoginUiState
    data class Error(val message: String) : LoginUiState
}
