package com.fdev.nutrimindai.core.audio

import kotlinx.coroutines.flow.StateFlow
import java.io.File

interface AudioRecorder {
    val isRecording: StateFlow<Boolean>
    val amplitude: StateFlow<Float>
    val durationSeconds: StateFlow<Int>

    fun startRecording(outputFile: File): Result<Unit>
    fun stopRecording(): File?
    fun cancelRecording()
}
