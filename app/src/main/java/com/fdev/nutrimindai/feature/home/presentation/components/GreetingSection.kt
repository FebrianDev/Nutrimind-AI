package com.fdev.nutrimindai.feature.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WavingHand
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.component.UserAvatar

@Composable
fun GreetingSection(
    userName: String,
    modifier: Modifier = Modifier,
    userPhotoUrl: String? = null
) {
    val morningGreeting = stringResource(R.string.home_greeting_morning)
    val afternoonGreeting = stringResource(R.string.home_greeting_afternoon)
    val eveningGreeting = stringResource(R.string.home_greeting_evening)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            val greeting = remember(morningGreeting, afternoonGreeting, eveningGreeting) {
                val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
                when (hour) {
                    in 4..11 -> morningGreeting
                    in 12..16 -> afternoonGreeting
                    else -> eveningGreeting
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = greeting,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Rounded.WavingHand,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
            Text(
                text = userName,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
        
        // Profile Image / Initial Avatar
        UserAvatar(
            photoUrl = userPhotoUrl,
            userName = userName,
            size = 48.dp
        )
    }
}
