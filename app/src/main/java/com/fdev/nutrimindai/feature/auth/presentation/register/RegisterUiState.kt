package com.fdev.nutrimindai.feature.auth.presentation.register

sealed interface RegisterUiState {
    data object Idle : RegisterUiState
    data object Loading : RegisterUiState
    data class Success(val isNewUser: Boolean = true) : RegisterUiState
    data class Error(val message: String) : RegisterUiState
}
