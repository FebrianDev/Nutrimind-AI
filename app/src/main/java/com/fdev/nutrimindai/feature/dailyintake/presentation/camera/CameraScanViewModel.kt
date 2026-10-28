package com.fdev.nutrimindai.feature.dailyintake.presentation.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.ai.AiRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Food
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.BarcodeFoodRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.FoodRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

enum class ScanMode {
    FOOD_AI,
    BARCODE
}

@HiltViewModel
class CameraScanViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val aiRepository: AiRepository,
    private val intakeRepository: IntakeRepository,
    private val dailyLogRepository: DailyLogRepository,
    private val foodRepository: FoodRepository,
    private val barcodeFoodRepository: BarcodeFoodRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<CameraScanUiState>(CameraScanUiState.Idle)
    val uiState: StateFlow<CameraScanUiState> = _uiState.asStateFlow()

    private val _torchEnabled = MutableStateFlow(false)
    val torchEnabled: StateFlow<Boolean> = _torchEnabled.asStateFlow()

    private val _scanMode = MutableStateFlow(ScanMode.FOOD_AI)
    val scanMode: StateFlow<ScanMode> = _scanMode.asStateFlow()

    private var currentBitmap: Bitmap? = null

    private val currentDate: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

    fun toggleTorch() {
        _torchEnabled.value = !_torchEnabled.value
    }

    fun setScanMode(mode: ScanMode) {
        _scanMode.value = mode
        resetState()
    }

    fun resetState() {
        _uiState.value = CameraScanUiState.Idle
        currentBitmap = null
    }

    fun onBarcodeScanned(barcode: String) {
        if (_uiState.value is CameraScanUiState.Analyzing) return
        viewModelScope.launch {
            _uiState.value = CameraScanUiState.Analyzing
            val result = barcodeFoodRepository.getFoodByBarcode(barcode)
            result.onSuccess { recognized ->
                _uiState.value = CameraScanUiState.Recognized(recognized, null)
            }.onFailure { error ->
                Timber.e(error, "Failed to analyze barcode product")
                _uiState.value = CameraScanUiState.Error(
                    error.message ?: "Product barcode was not found in the database. Please try scanning again or enter manually."
                )
            }
        }
    }

    fun analyzeImage(bitmap: Bitmap) {
        viewModelScope.launch {
            _uiState.value = CameraScanUiState.Analyzing
            val scaledBitmap = scaleBitmapForAi(bitmap)
            currentBitmap = scaledBitmap
            val result = aiRepository.analyzeFoodImage(scaledBitmap)
            result.onSuccess { recognized ->
                _uiState.value = CameraScanUiState.Recognized(recognized, scaledBitmap)
            }.onFailure { error ->
                Timber.e(error, "Failed to analyze food image with AI")
                _uiState.value = CameraScanUiState.Error(
                    error.message ?: "Could not recognize food image. Please try again with clear lighting."
                )
            }
        }
    }


    fun saveRecognizedFood(
        mealTypeString: String,
        foodName: String,
        servingSizeText: String,
        servingUnit: String,
        caloriesText: String,
        proteinText: String,
        carbsText: String,
        fatText: String,
        fiberText: String,
        source: FoodSource,
        onSuccess: () -> Unit
    ) {
        val nameTrimmed = foodName.trim()
        if (nameTrimmed.isBlank()) {
            _uiState.value = CameraScanUiState.Error("Food name cannot be empty.")
            return
        }

        val calories = caloriesText.toIntOrNull() ?: 0
        val servingSize = servingSizeText.toDoubleOrNull() ?: 1.0
        val protein = proteinText.toDoubleOrNull() ?: 0.0
        val carbs = carbsText.toDoubleOrNull() ?: 0.0
        val fat = fatText.toDoubleOrNull() ?: 0.0
        val fiber = fiberText.toDoubleOrNull() ?: 0.0

        val mealType = try {
            MealType.valueOf(mealTypeString.uppercase())
        } catch (e: Exception) {
            MealType.BREAKFAST
        }

        viewModelScope.launch {
            try {
                val dailyLog = dailyLogRepository.getDailyLogByDateDirect(currentDate)
                if (dailyLog != null && dailyLog.status != DailyLogStatus.DRAFT) {
                    _uiState.value = CameraScanUiState.Error("Today's journal is completed and read-only.")
                    return@launch
                }

                val savedImagePath = currentBitmap?.let { saveBitmapLocally(it) }

                val dailyLogId = dailyLogRepository.getOrCreateDailyLog(currentDate)
                val intake = Intake(
                    id = 0,
                    dailyLogId = dailyLogId,
                    mealType = mealType,
                    foodName = nameTrimmed,
                    servingSize = servingSize,
                    servingUnit = servingUnit.ifBlank { "portion" },
                    calories = calories,
                    protein = protein,
                    carbs = carbs,
                    fat = fat,
                    fiber = fiber,
                    imagePath = savedImagePath,
                    foodSource = source
                )

                intakeRepository.addIntake(intake)

                // Also save AI-recognized food into the local food catalog for future searchability
                foodRepository.insertFood(
                    Food(
                        id = 0,
                        name = nameTrimmed,
                        calories = calories,
                        protein = protein,
                        carbs = carbs,
                        fat = fat,
                        servingSize = servingSize,
                        servingUnit = servingUnit.ifBlank { "portion" }
                    )
                )

                _uiState.value = CameraScanUiState.SuccessSaved
                onSuccess()
            } catch (e: Exception) {
                Timber.e(e, "Failed to save recognized food intake")
                _uiState.value = CameraScanUiState.Error(e.message ?: "Failed to save food intake.")
            }
        }
    }

    private fun saveBitmapLocally(bitmap: Bitmap): String? {
        return try {
            val dir = File(context.filesDir, "food_images")
            if (!dir.exists()) dir.mkdirs()
            val file = File(dir, "meal_${System.currentTimeMillis()}.jpg")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
            file.absolutePath
        } catch (e: Exception) {
            Timber.e(e, "Failed to save food image locally")
            null
        }
    }

    private fun scaleBitmapForAi(bitmap: Bitmap): Bitmap {
        val maxDimension = 1024
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val scale = maxDimension.toFloat() / Math.max(width, height)
        val matrix = Matrix()
        matrix.postScale(scale, scale)
        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
    }
}

