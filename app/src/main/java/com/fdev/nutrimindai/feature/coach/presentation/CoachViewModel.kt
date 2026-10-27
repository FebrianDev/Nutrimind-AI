package com.fdev.nutrimindai.feature.coach.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.ai.AiProvider
import com.fdev.nutrimindai.core.audio.AudioRecorder
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
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CoachViewModel @Inject constructor(
    private val coachRepository: CoachRepository,
    private val promptGenerator: ContextualQuickPromptGenerator,
    private val audioRecorder: AudioRecorder
) : ViewModel() {

    private val _isGenerating = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _inputText = MutableStateFlow("")
    private val _contextualPrompts = MutableStateFlow<List<ContextualPrompt>>(emptyList())
    private val _isTranscribingVoice = MutableStateFlow(false)

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
        _inputText,
        _isTranscribingVoice
    ) { isGenerating, errorMessage, inputText, isTranscribing ->
        ChatInternalState(isGenerating, errorMessage, inputText, isTranscribing)
    }

    private val _voiceState = combine(
        audioRecorder.isRecording,
        audioRecorder.durationSeconds,
        audioRecorder.amplitude
    ) { isRecording, duration, amplitude ->
        VoiceInternalState(isRecording, duration, amplitude)
    }

    val uiState: StateFlow<CoachUiState> = combine(
        coachRepository.getAllMessages(),
        _chatState,
        coachRepository.activeProviderFlow,
        _contextualPrompts,
        _voiceState
    ) { messages, chat, activeProvider, contextualPrompts, voice ->
        CoachUiState(
            messages = messages,
            isGenerating = chat.isGenerating,
            errorMessage = chat.errorMessage,
            inputText = chat.inputText,
            activeProvider = activeProvider,
            contextualPrompts = contextualPrompts,
            isRecordingVoice = voice.isRecording,
            isTranscribingVoice = chat.isTranscribing,
            voiceDurationSeconds = voice.duration,
            voiceAmplitude = voice.amplitude
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

    fun startVoiceRecording(cacheDir: File) {
        if (_isGenerating.value || _isTranscribingVoice.value) return
        val targetFile = File(cacheDir, "voice_input_${System.currentTimeMillis()}.wav")
        val result = audioRecorder.startRecording(targetFile)
        result.onFailure { error ->
            _errorMessage.value = error.localizedMessage ?: "Failed to start microphone recording"
        }
    }

    fun stopVoiceRecordingAndTranscribe() {
        val audioFile = audioRecorder.stopRecording()
        if (audioFile == null || !audioFile.exists() || audioFile.length() == 0L) {
            return
        }

        _isTranscribingVoice.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            val result = coachRepository.transcribeVoice(audioFile)
            _isTranscribingVoice.value = false
            audioFile.delete()

            result.onSuccess { transcribed ->
                val clean = transcribed.trim()
                if (clean.isNotBlank()) {
                    val current = _inputText.value.trim()
                    _inputText.value = if (current.isEmpty()) clean else "$current $clean"
                }
            }.onFailure { error ->
                _errorMessage.value = error.localizedMessage ?: "Failed to transcribe voice with Meta AI"
            }
        }
    }

    fun cancelVoiceRecording() {
        audioRecorder.cancelRecording()
    }

    fun dismissError() {
        _errorMessage.value = null
    }

    fun clearHistory() {
        viewModelScope.launch {
            coachRepository.clearHistory()
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioRecorder.cancelRecording()
    }
}

private data class ChatInternalState(
    val isGenerating: Boolean,
    val errorMessage: String?,
    val inputText: String,
    val isTranscribing: Boolean
)

private data class VoiceInternalState(
    val isRecording: Boolean,
    val duration: Int,
    val amplitude: Float
)
