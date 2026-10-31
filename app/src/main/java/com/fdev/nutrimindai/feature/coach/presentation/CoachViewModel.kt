package com.fdev.nutrimindai.feature.coach.presentation

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.ai.AiProvider
import com.fdev.nutrimindai.core.audio.AudioRecorder
import com.fdev.nutrimindai.feature.coach.domain.repository.CoachRepository
import com.fdev.nutrimindai.feature.coach.domain.ContextualQuickPromptGenerator
import com.fdev.nutrimindai.feature.coach.domain.model.ContextualPrompt
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import javax.inject.Inject

@HiltViewModel
class CoachViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val coachRepository: CoachRepository,
    private val promptGenerator: ContextualQuickPromptGenerator,
    private val audioRecorder: AudioRecorder
) : ViewModel() {

    private val _isGenerating = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _inputText = MutableStateFlow("")
    private val _contextualPrompts = MutableStateFlow<List<ContextualPrompt>>(emptyList())
    private val _isTranscribingVoice = MutableStateFlow(false)
    private val _attachment = MutableStateFlow(ImageAttachment())
    private var generateJob: Job? = null

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
        _isTranscribingVoice,
        _attachment
    ) { isGenerating, errorMessage, inputText, isTranscribing, attachment ->
        ChatInternalState(isGenerating, errorMessage, inputText, isTranscribing, attachment.uri, attachment.bitmap)
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
            voiceAmplitude = voice.amplitude,
            selectedImageUri = chat.imageUri,
            selectedImageBitmap = chat.imageBitmap
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

    fun onImageSelected(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val maxDim = 1024
                        val size = info.size
                        if (size.width > maxDim || size.height > maxDim) {
                            val ratio = size.width.toFloat() / size.height.toFloat()
                            if (size.width > size.height) {
                                decoder.setTargetSize(maxDim, (maxDim / ratio).toInt().coerceAtLeast(1))
                            } else {
                                decoder.setTargetSize((maxDim * ratio).toInt().coerceAtLeast(1), maxDim)
                            }
                        }
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val raw = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    scaleBitmap(raw, 1024)
                }

                // Cache image locally for preview & chat persistence
                val cachedFile = File(context.cacheDir, "coach_attach_${System.currentTimeMillis()}.jpg")
                cachedFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }

                _attachment.value = ImageAttachment(uri = cachedFile.absolutePath, bitmap = bitmap)
            } catch (e: Exception) {
                Timber.e(e, "Failed to decode selected image")
                _errorMessage.value = "Failed to load image from gallery."
            }
        }
    }

    fun onImageBitmapCaptured(rawBitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val scaled = scaleBitmap(rawBitmap, 1024)
                val cachedFile = File(context.cacheDir, "coach_attach_${System.currentTimeMillis()}.jpg")
                cachedFile.outputStream().use { out ->
                    scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
                _attachment.value = ImageAttachment(uri = cachedFile.absolutePath, bitmap = scaled)
            } catch (e: Exception) {
                Timber.e(e, "Failed to process captured image")
                _errorMessage.value = "Failed to capture photo."
            }
        }
    }

    fun clearSelectedImage() {
        _attachment.value = ImageAttachment()
    }

    private fun scaleBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = maxDimension.toFloat() / maxOf(width, height)
        val matrix = Matrix().apply { postScale(ratio, ratio) }
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }

    fun sendMessage(explicitText: String? = null) {
        val textToSend = (explicitText ?: _inputText.value).trim()
        val currentAttachment = _attachment.value
        val imageUri = currentAttachment.uri
        val imageBitmap = currentAttachment.bitmap

        if ((textToSend.isBlank() && imageBitmap == null) || _isGenerating.value) return

        val effectiveText = if (textToSend.isBlank() && imageBitmap != null) {
            "Please analyze this meal or menu image for nutritional breakdown and advice."
        } else {
            textToSend
        }

        if (explicitText == null) {
            _inputText.value = ""
        }
        _attachment.value = ImageAttachment()

        _isGenerating.value = true
        _errorMessage.value = null

        generateJob?.cancel()
        generateJob = viewModelScope.launch {
            try {
                val result = coachRepository.sendMessage(
                    userText = effectiveText,
                    imageUri = imageUri,
                    imageBitmap = imageBitmap
                )
                result.onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Failed to connect to AI. Please try again."
                }
            } finally {
                _isGenerating.value = false
                generateJob = null
            }
        }
    }

    fun stopGenerating() {
        generateJob?.cancel()
        generateJob = null
        _isGenerating.value = false
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
            try {
                val result = coachRepository.transcribeVoice(audioFile)
                result.onSuccess { transcribed ->
                    val clean = transcribed.trim()
                    if (clean.isNotBlank()) {
                        val current = _inputText.value.trim()
                        _inputText.value = if (current.isEmpty()) clean else "$current $clean"
                    }
                }.onFailure { error ->
                    _errorMessage.value = error.localizedMessage ?: "Failed to transcribe voice with Meta AI"
                }
            } finally {
                _isTranscribingVoice.value = false
                try {
                    if (audioFile.exists()) {
                        audioFile.delete()
                    }
                } catch (e: Exception) {
                    timber.log.Timber.w(e, "Failed to delete temporary audio file")
                }
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
    val isTranscribing: Boolean,
    val imageUri: String?,
    val imageBitmap: Bitmap?
)

private data class VoiceInternalState(
    val isRecording: Boolean,
    val duration: Int,
    val amplitude: Float
)

private data class ImageAttachment(
    val uri: String? = null,
    val bitmap: Bitmap? = null
)
