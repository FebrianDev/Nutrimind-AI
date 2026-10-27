package com.fdev.nutrimindai.feature.onboarding.presentation.state

import com.fdev.nutrimindai.feature.onboarding.util.ActivityLevel
import com.fdev.nutrimindai.feature.onboarding.util.Gender
import com.fdev.nutrimindai.feature.onboarding.util.Goal

/**
 * Immutable UI State representing the complete state of the onboarding wizard flow.
 *
 * @property currentStep Current step in the wizard (1 to 5).
 * @property name User's input name.
 * @property age User's input age string.
 * @property gender User's selected gender.
 * @property height User's input height string (cm).
 * @property currentWeight User's input current weight string (kg).
 * @property activityLevel User's selected activity level.
 * @property goal User's selected primary goal.
 * @property targetWeight User's target weight string (kg).
 * @property nameError Field validation error for name, if any.
 * @property ageError Field validation error for age, if any.
 * @property heightError Field validation error for height, if any.
 * @property weightError Field validation error for weight, if any.
 * @property targetWeightError Field validation error for target weight, if any.
 * @property isStep1Valid Flag indicating if Step 1 inputs are valid.
 * @property isStep2Valid Flag indicating if Step 2 selection is valid.
 * @property isStep3Valid Flag indicating if Step 3 selection is valid.
 * @property isStep4Valid Flag indicating if Step 4 input is valid.
 * @property isLoadingStep5 Flag indicating whether Step 5 calculations/simulations are in progress.
 * @property loadingStep5Text Loading text displayed during Step 5 setup sequence.
 * @property calculatedCalories Output daily target calories.
 * @property calculatedProtein Output daily target protein in grams.
 * @property calculatedCarbs Output daily target carbohydrates in grams.
 * @property calculatedFat Output daily target fats in grams.
 * @property calculatedExerciseMinutes Output daily target exercise minutes.
 * @property estimatedCompletionTime Output estimated duration string to achieve target weight.
 * @property isCompleted Flag triggering navigation to Home upon saving profile.
 */
data class OnboardingUiState(
    val currentStep: Int = 1,

    // Step 1 Inputs
    val name: String = "",
    val age: String = "",
    val gender: Gender = Gender.Female,
    val height: String = "",
    val currentWeight: String = "",

    // Step 2 Inputs
    val activityLevel: ActivityLevel? = null,

    // Step 3 Inputs
    val goal: Goal? = null,

    // Step 4 Inputs
    val targetWeight: String = "",

    // Validation Errors
    val nameError: String? = null,
    val ageError: String? = null,
    val heightError: String? = null,
    val weightError: String? = null,
    val targetWeightError: String? = null,

    // Step Validation Status
    val isStep1Valid: Boolean = false,
    val isStep2Valid: Boolean = false,
    val isStep3Valid: Boolean = false,
    val isStep4Valid: Boolean = false,

    // Step 5 Calculation & Loading State
    val isLoadingStep5: Boolean = false,
    val loadingStep5Text: String = "",
    val calculatedCalories: Int = 0,
    val calculatedProtein: Int = 0,
    val calculatedCarbs: Int = 0,
    val calculatedFat: Int = 0,
    val calculatedExerciseMinutes: Int = 0,
    val estimatedCompletionTime: String = "",

    // Navigation state
    val isCompleted: Boolean = false
)
