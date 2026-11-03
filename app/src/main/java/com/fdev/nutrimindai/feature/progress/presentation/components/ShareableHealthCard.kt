package com.fdev.nutrimindai.feature.progress.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.*
import com.fdev.nutrimindai.feature.progress.presentation.screen.DailyReviewDetailData
import java.util.Locale

/**
 * Premium Shareable Daily Health Snapshot Card.
 * Designed for social sharing (Instagram Stories, WhatsApp, X/Twitter).
 * Features rich dark emerald gradients, glassmorphism cards, and Meta AI branding.
 */
@Composable
fun ShareableHealthCard(
    detail: DailyReviewDetailData,
    modifier: Modifier = Modifier
) {
    // Rich luxury dark emerald & obsidian gradient background
    val cardBackground = Brush.verticalGradient(
        colors = listOf(
            Color(0xFF0F261B),
            Color(0xFF091710),
            Color(0xFF050D09)
        )
    )

    val goldAccent = Color(0xFFFFD54F)
    val cardBorder = BorderStroke(1.dp, Color(0x334CAF50))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .border(cardBorder, RoundedCornerShape(28.dp)),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(cardBackground)
                .padding(AppSpacing.xl)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Header: App Branding + Snapshot Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF1E4B33)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Spa,
                                contentDescription = null,
                                tint = Color(0xFF66BB6A),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Text(
                                text = if (detail.userName.isNotBlank()) detail.userName else stringResource(R.string.share_badge_daily_snapshot),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFA5D6A7)
                            )
                        }
                    }

                    // Date Pill
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0x22FFFFFF),
                        border = BorderStroke(0.5.dp, Color(0x33FFFFFF))
                    ) {
                        Text(
                            text = detail.displayDate.ifBlank { detail.dateStr },
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFE0E0E0),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                // Hero Health Score Card
                ScoreHeroSection(detail = detail)

                // 2x2 Nutrition & Activity Metrics Grid
                MetricsGridSection(detail = detail)

                // Meta AI Coach Insight Box
                if (detail.aiSummary.isNotBlank()) {
                    AiInsightSection(summary = detail.aiSummary)
                }

                // Footer: Meta AI Watermark & Hashtag
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF81C784),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.share_powered_by),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9EBAA9),
                            fontSize = 11.sp
                        )
                    }

                    Surface(
                        shape = CircleShape,
                        color = Color(0x1F4CAF50),
                        border = BorderStroke(0.5.dp, Color(0x444CAF50))
                    ) {
                        Text(
                            text = "#NutriMindAI",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF81C784),
                            modifier = Modifier.badgePadding(),
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScoreHeroSection(detail: DailyReviewDetailData) {
    val score = detail.score
    val scoreColor = when {
        score >= 85 -> Color(0xFF66BB6A)
        score >= 70 -> Color(0xFFFFD54F)
        else -> Color(0xFFFF8A65)
    }

    val statusIcon = when {
        score >= 85 -> Icons.Default.EmojiEvents
        score >= 70 -> Icons.Default.Verified
        else -> Icons.Default.AutoAwesome
    }

    val statusLabel = when (detail.status) {
        "Excellent" -> stringResource(R.string.status_excellent)
        "Good" -> stringResource(R.string.status_good)
        "Fair" -> stringResource(R.string.status_fair)
        "Needs Attention" -> stringResource(R.string.status_needs_attention)
        else -> detail.status
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color(0x1AFFFFFF),
        border = BorderStroke(1.dp, Color(0x26FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .cardPadding(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.label_nutrition_score),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFFB0BEC5)
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = scoreColor.copy(alpha = 0.2f),
                    border = BorderStroke(0.5.dp, scoreColor.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.badgePadding(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = null,
                            tint = scoreColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = scoreColor
                        )
                    }
                }
            }

            // Big Health Score Display
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "$score",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = scoreColor,
                    fontSize = 44.sp
                )
                Text(
                    text = stringResource(R.string.score_out_of_100),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0x88FFFFFF),
                    modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricsGridSection(detail: DailyReviewDetailData) {
    val calProg = if (detail.targetCalories > 0) {
        (detail.calories.toFloat() / detail.targetCalories).coerceIn(0f, 1f)
    } else 0f

    val proteinProg = if (detail.targetProtein > 0) {
        (detail.protein.toFloat() / detail.targetProtein.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val waterProg = if (detail.targetWater > 0) {
        (detail.water.toFloat() / detail.targetWater.toFloat()).coerceIn(0f, 1f)
    } else 0f

    val exProg = if (detail.targetExerciseMinutes > 0) {
        (detail.exerciseMinutes.toFloat() / detail.targetExerciseMinutes.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.LocalFireDepartment,
                iconColor = NutritionCalories,
                label = stringResource(R.string.home_label_calories),
                value = String.format(Locale.US, "%,d", detail.calories),
                unit = "kcal",
                target = "${detail.targetCalories} kcal",
                progress = calProg
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.Egg,
                iconColor = NutritionProtein,
                label = stringResource(R.string.label_protein),
                value = "${detail.protein.toInt()}",
                unit = "g",
                target = "${detail.targetProtein.toInt()} g",
                progress = proteinProg
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.WaterDrop,
                iconColor = NutritionWater,
                label = stringResource(R.string.label_water),
                value = String.format(Locale.US, "%.1f", detail.water / 1000f),
                unit = "L",
                target = String.format(Locale.US, "%.1f L", detail.targetWater / 1000f),
                progress = waterProg
            )
            MetricCard(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.DirectionsRun,
                iconColor = NutritionExercise,
                label = stringResource(R.string.label_activity),
                value = "${detail.exerciseMinutes}",
                unit = "min",
                target = "${detail.targetExerciseMinutes} min",
                progress = exProg
            )
        }
    }
}

@Composable
private fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconColor: Color,
    label: String,
    value: String,
    unit: String,
    target: String,
    progress: Float
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = Color(0x14FFFFFF),
        border = BorderStroke(0.5.dp, Color(0x22FFFFFF))
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (progress >= 1f) Color(0xFF81C784) else Color(0xAAFFFFFF)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = unit,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0x88FFFFFF),
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
                Text(
                    text = "/ $target",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0x66FFFFFF),
                    fontSize = 10.sp
                )
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(CircleShape),
                color = if (progress >= 1f) Color(0xFF66BB6A) else iconColor,
                trackColor = Color(0x1FFFFFFF),
                strokeCap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun AiInsightSection(summary: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color(0x1F2E7D32),
        border = BorderStroke(0.8.dp, Color(0x444CAF50)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.cardPadding(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF388E3C)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = stringResource(R.string.share_ai_evaluation_title),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFA5D6A7)
                )
                Text(
                    text = "\"$summary\"",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFFE8F5E9),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
