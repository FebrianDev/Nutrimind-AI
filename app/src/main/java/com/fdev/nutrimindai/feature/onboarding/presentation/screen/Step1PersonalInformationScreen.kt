package com.fdev.nutrimindai.feature.onboarding.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdev.nutrimindai.core.designsystem.authScreenPadding
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.feature.onboarding.presentation.component.AppTextField
import com.fdev.nutrimindai.feature.onboarding.presentation.component.GenderSelector
import com.fdev.nutrimindai.feature.onboarding.presentation.state.OnboardingUiState
import com.fdev.nutrimindai.core.model.Gender

/**
 * Step 1 Onboarding Screen: Collects name, age, gender, height, and current weight.
 *
 * @param state Immutable onboarding UI state.
 * @param onNameChange Callback when name changes.
 * @param onAgeChange Callback when age changes.
 * @param onGenderChange Callback when gender selection changes.
 * @param onHeightChange Callback when height changes.
 * @param onWeightChange Callback when current weight changes.
 * @param modifier Custom modifier.
 */
@Composable
fun Step1PersonalInformationScreen(
    state: OnboardingUiState,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onGenderChange: (Gender) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .authScreenPadding()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.step1_title),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.step1_subtitle),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Name Field
        AppTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = stringResource(id = R.string.label_name),
            placeholder = stringResource(id = R.string.placeholder_name),
            errorMessage = state.nameError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Age Field
        AppTextField(
            value = state.age,
            onValueChange = onAgeChange,
            label = stringResource(id = R.string.label_age),
            placeholder = stringResource(id = R.string.placeholder_age),
            errorMessage = state.ageError,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Gender Selector
        Text(
            text = stringResource(id = R.string.label_gender),
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        GenderSelector(
            selectedGender = state.gender,
            onGenderSelected = onGenderChange
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Height & Weight Side by Side
        Row(modifier = Modifier.fillMaxWidth()) {
            AppTextField(
                value = state.height,
                onValueChange = onHeightChange,
                label = stringResource(id = R.string.label_height),
                placeholder = stringResource(id = R.string.placeholder_height),
                errorMessage = state.heightError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(16.dp))

            AppTextField(
                value = state.currentWeight,
                onValueChange = onWeightChange,
                label = stringResource(id = R.string.label_weight),
                placeholder = stringResource(id = R.string.placeholder_weight),
                errorMessage = state.weightError,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Done
                ),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
