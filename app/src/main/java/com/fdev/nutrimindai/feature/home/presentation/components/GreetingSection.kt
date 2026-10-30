package com.fdev.nutrimindai.feature.home.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material3.*
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun GreetingSection(
    userName: String,
    modifier: Modifier = Modifier,
    date: String? = null,
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

    val formattedDate = remember(date) {
        try {
            if (!date.isNullOrBlank()) {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(date)
                if (parsed != null) {
                    val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                    val dayMonth = SimpleDateFormat("EEE, d MMM", Locale.US).format(parsed)
                    if (date == todayStr) "Today • $dayMonth" else dayMonth
                } else {
                    val dayMonth = SimpleDateFormat("EEE, d MMM", Locale.US).format(Date())
                    "Today • $dayMonth"
                }
            } else {
                val dayMonth = SimpleDateFormat("EEE, d MMM", Locale.US).format(Date())
                "Today • $dayMonth"
            }
        } catch (_: Exception) {
            val dayMonth = SimpleDateFormat("EEE, d MMM", Locale.US).format(Date())
            "Today • $dayMonth"
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
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
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Profile Image / Initial Avatar
        UserAvatar(
            photoUrl = userPhotoUrl,
            userName = userName,
            size = 40.dp
        )
    }
}
