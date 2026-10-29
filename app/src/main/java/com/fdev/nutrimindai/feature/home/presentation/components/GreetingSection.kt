package com.fdev.nutrimindai.feature.home.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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

    val greeting = remember(morningGreeting, afternoonGreeting, eveningGreeting) {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 4..11 -> morningGreeting
            in 12..16 -> afternoonGreeting
            else -> eveningGreeting
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                    )
                ) {
                    append("$greeting, ")
                }
                withStyle(
                    SpanStyle(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    append(userName)
                }
            },
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Profile Image / Initial Avatar
        UserAvatar(
            photoUrl = userPhotoUrl,
            userName = userName,
            size = 40.dp
        )
    }
}
