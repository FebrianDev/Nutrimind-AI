package com.fdev.nutrimindai.feature.onboarding.presentation.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdev.nutrimindai.core.designsystem.authScreenPadding
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.feature.onboarding.presentation.component.SelectionCard
import com.fdev.nutrimindai.feature.onboarding.presentation.state.OnboardingUiState
import com.fdev.nutrimindai.core.model.Goal

/**
 * Step 3 Onboarding Screen: Primary Goal Selection (Lose Weight, Maintain Weight, Gain Weight).
 *
 * @param state Current UI state.
 * @param onGoalSelected Callback when a goal card is selected.
 * @param modifier Custom modifier.
 */
@Composable
fun Step3GoalScreen(
    state: OnboardingUiState,
    onGoalSelected: (Goal) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val options = listOf(
        Triple(
            Goal.LoseWeight,
            Pair(stringResource(id = R.string.goal_lose_title), stringResource(id = R.string.goal_lose_desc)),
            Icons.AutoMirrored.Filled.TrendingDown
        ),
        Triple(
            Goal.MaintainWeight,
            Pair(stringResource(id = R.string.goal_maintain_title), stringResource(id = R.string.goal_maintain_desc)),
            Icons.Default.MonitorWeight
        ),
        Triple(
            Goal.GainWeight,
            Pair(stringResource(id = R.string.goal_gain_title), stringResource(id = R.string.goal_gain_desc)),
            Icons.AutoMirrored.Filled.TrendingUp
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .authScreenPadding()
            .padding(vertical = 16.dp)
    ) {
        Text(
            text = stringResource(id = R.string.step3_title),
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(id = R.string.step3_subtitle),
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        options.forEach { (goalOption, titles, icon) ->
            SelectionCard(
                title = titles.first,
                description = titles.second,
                isSelected = state.goal == goalOption,
                onClick = { onGoalSelected(goalOption) },
                icon = icon,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
