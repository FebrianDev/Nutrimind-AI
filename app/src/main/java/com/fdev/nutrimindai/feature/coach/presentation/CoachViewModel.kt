package com.fdev.nutrimindai.feature.coach.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.ai.AiProvider
import com.fdev.nutrimindai.feature.coach.domain.repository.CoachRepository
import com.fdev.nutrimindai.feature.coach.domain.ContextualQuickPromptGenerator
import com.fdev.nutrimindai.feature.coach.domain.model.ContextualPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val coachRepository: CoachRepository,
    private val promptGenerator: ContextualQuickPromptGenerator
) : ViewModel() {

    private val _isGenerating = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _inputText = MutableStateFlow("")
    private val _contextualPrompts = MutableStateFlow<List<ContextualPrompt>>(emptyList())

    init {
        refreshContextualPrompts()
    }

    fun refreshContextualPrompts() {
        viewModelScope.launch {
            _contextualPrompts.value = promptGenerator.getContextualPrompts()
        }
    }

    private val _chatState = combine(
        _isGenerating,
        _errorMessage,
        _inputText
    ) { isGenerating, errorMessage, inputText ->
        Triple(isGenerating, errorMessage, inputText)
    }

    val uiState: StateFlow<CoachUiState> = combine(
        coachRepository.getAllMessages(),
        _chatState,
        coachRepository.activeProviderFlow,
        _contextualPrompts
    ) { messages, (isGenerating, errorMessage, inputText), activeProvider, contextualPrompts ->
        CoachUiState(
            messages = messages,
            isGenerating = isGenerating,
            errorMessage = errorMessage,
            inputText = inputText,
            activeProvider = activeProvider,
            contextualPrompts = contextualPrompts
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CoachUiState(activeProvider = coachRepository.getActiveProvider())
    )

    fun onInputTextChanged(newText: String) {
        _inputText.value = newText
    }

    fun setProvider(provider: AiProvider) {
        coachRepository.setActiveProvider(provider)
    }

    fun sendMessage(explicitText: String? = null) {
        val textToSend = (explicitText ?: _inputText.value).trim()
        if (textToSend.isBlank() || _isGenerating.value) return

        if (explicitText == null) {
            _inputText.value = ""
        }
        _isGenerating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = coachRepository.sendMessage(textToSend)
            _isGenerating.value = false
            result.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Failed to connect to AI. Please try again."
            }
        }
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun clearHistory() {
        viewModelScope.launch {
            coachRepository.clearHistory()
        }
    }
}
