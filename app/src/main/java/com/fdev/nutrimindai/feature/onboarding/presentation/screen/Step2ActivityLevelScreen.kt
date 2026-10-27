package com.fdev.nutrimindai.feature.onboarding.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Weekend
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.feature.onboarding.presentation.component.SelectionCard
import com.fdev.nutrimindai.feature.onboarding.presentation.state.OnboardingUiState
import com.fdev.nutrimindai.core.model.ActivityLevel

/**
 * Step 2 Onboarding Screen: Activity Level selection.
 *
 * @param state Current UI state.
 * @param onActivitySelected Callback when an activity level card is picked.
 * @param modifier Custom modifier.
 */
@Composable
fun Step2ActivityLevelScreen(
    state: OnboardingUiState,
    onActivitySelected: (ActivityLevel) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val options = listOf(
        Quadruple(
            ActivityLevel.Sedentary,
            stringResource(id = R.string.activity_sedentary_title),
            stringResource(id = R.string.activity_sedentary_desc),
            Icons.Default.Weekend
        ),
        Quadruple(
            ActivityLevel.LightlyActive,
            stringResource(id = R.string.activity_lightly_title),
            stringResource(id = R.string.activity_lightly_desc),
            Icons.AutoMirrored.Filled.DirectionsWalk
        ),
        Quadruple(
            ActivityLevel.ModeratelyActive,
            stringResource(id = R.string.activity_moderately_title),
            stringResource(id = R.string.activity_moderately_desc),
            Icons.Default.FitnessCenter
        ),
        Quadruple(
            ActivityLevel.VeryActive,
            stringResource(id = R.string.activity_very_title),
            stringResource(id = R.string.activity_very_desc),
            Icons.AutoMirrored.Filled.DirectionsRun
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.step2_title),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.step2_subtitle),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        options.forEach { (level, title, desc, icon) ->
            SelectionCard(
                title = title,
                description = desc,
                isSelected = state.activityLevel == level,
                onClick = { onActivitySelected(level) },
                icon = icon,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
