package com.fdev.nutrimindai.feature.dailyintake.presentation.camera

import android.graphics.Bitmap
import com.fdev.nutrimindai.core.ai.FoodRecognitionResult

sealed interface CameraScanUiState {
    data object Idle : CameraScanUiState
    data object Analyzing : CameraScanUiState
    data class Recognized(val result: FoodRecognitionResult, val imageBitmap: Bitmap?) : CameraScanUiState
    data class Error(val message: String) : CameraScanUiState
    data object SuccessSaved : CameraScanUiState
}
