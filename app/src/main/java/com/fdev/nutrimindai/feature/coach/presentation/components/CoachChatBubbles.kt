package com.fdev.nutrimindai.feature.coach.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.*
import com.fdev.nutrimindai.feature.coach.domain.model.ChatMessage
import com.fdev.nutrimindai.feature.coach.domain.model.ChatRole
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun CoachChatBubble(
    message: ChatMessage,
    providerName: String = "AI",
    modifier: Modifier = Modifier
) {
    val isUser = message.role == ChatRole.USER
    val timeFormatted = rememberTime(message.timestamp)

    // Smooth typewriter animation for freshly generated assistant responses (< 3.5s)
    val isBrandNew = !isUser && (System.currentTimeMillis() - message.timestamp < 3500L)
    var displayedLength by remember(message.id) {
        mutableIntStateOf(if (isBrandNew) 0 else message.content.length)
    }

    LaunchedEffect(message.id, message.content) {
        val total = message.content.length
        if (displayedLength < total) {
            while (displayedLength < total) {
                val step = when {
                    total - displayedLength > 300 -> 8
                    total - displayedLength > 100 -> 4
                    else -> 2
                }
                displayedLength = minOf(total, displayedLength + step)
                delay(12L)
            }
        }
    }

    val renderedContent = remember(displayedLength, message.content) {
        if (displayedLength >= message.content.length) message.content
        else message.content.substring(0, displayedLength)
    }
    val isAnimationComplete = displayedLength >= message.content.length

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top
    ) {
        if (!isUser) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = stringResource(R.string.coach_cd_avatar),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        Column(
            modifier = Modifier.weight(1f, fill = false),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
        ) {
            if (!isUser) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 4.dp, start = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.coach_name),
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = providerName,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.badgePadding()
                        )
                    }
                }
            }

            Surface(
                shape = if (isUser) {
                    RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 4.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                } else {
                    RoundedCornerShape(
                        topStart = 4.dp,
                        topEnd = 18.dp,
                        bottomStart = 18.dp,
                        bottomEnd = 18.dp
                    )
                },
                color = if (isUser) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                tonalElevation = 1.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    if (isUser) {
                        if (!message.imageUri.isNullOrBlank()) {
                            AsyncImage(
                                model = File(message.imageUri),
                                contentDescription = "Attached meal or menu photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .padding(bottom = 8.dp)
                            )
                        }
                        Text(
                            text = message.content,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        MarkdownChatContent(
                            content = renderedContent,
                            textColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (isAnimationComplete && !message.actionSummary.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = message.actionSummary,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timeFormatted,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = if (isUser) {
                            MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        },
                        modifier = Modifier.align(Alignment.End)
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberTime(timestamp: Long): String {
    return SimpleDateFormat("HH:mm", Locale.US).format(Date(timestamp))
}
