package com.fdev.nutrimindai.feature.progress.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdev.nutrimindai.R

/**
 * Modern Jetpack Compose Canvas smooth line chart for progress trends.
 * Supports smooth Bezier curves, gradient fill, grid lines, day labels, and touch points.
 */
@Composable
fun ProgressTrendChart(
    dataPoints: List<Float>,
    labels: List<String>,
    unit: String,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    targetValue: Float? = null
) {
    if (dataPoints.isEmpty() || labels.size != dataPoints.size) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.chart_no_trend_data),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedIndex by remember(dataPoints) { mutableIntStateOf(-1) }
    val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant

    Column(modifier = modifier) {
        if (selectedIndex in dataPoints.indices) {
            val selectedVal = dataPoints[selectedIndex]
            val selectedLabel = labels[selectedIndex]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$selectedLabel: ${selectedVal.toInt()} $unit",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = lineColor
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(dataPoints) {
                    detectTapGestures { tapOffset ->
                        val width = size.width
                        val stepX = width / (dataPoints.size - 1).coerceAtLeast(1)
                        val index = (tapOffset.x / stepX).toInt().coerceIn(0, dataPoints.lastIndex)
                        selectedIndex = if (selectedIndex == index) -1 else index
                    }
                }
        ) {
            val width = size.width
            val height = size.height
            val bottomPadding = 16.dp.toPx()
            val chartHeight = height - bottomPadding
            val stepX = if (dataPoints.size > 1) width / (dataPoints.size - 1) else width

            val maxVal = (dataPoints.maxOrNull() ?: 1f).coerceAtLeast(1f) * 1.15f
            val minVal = 0f
            val valRange = (maxVal - minVal).coerceAtLeast(1f)

            // Draw horizontal grid lines (3 lines)
            val gridLineCount = 3
            for (i in 0..gridLineCount) {
                val y = chartHeight * (1f - i.toFloat() / gridLineCount)
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Draw target line if present
            targetValue?.let { target ->
                val targetY = chartHeight * (1f - (target - minVal) / valRange)
                if (targetY in 0f..chartHeight) {
                    drawLine(
                        color = Color.Red.copy(alpha = 0.5f),
                        start = Offset(0f, targetY),
                        end = Offset(width, targetY),
                        strokeWidth = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 10f), 0f)
                    )
                }
            }

            // Calculate points coordinates
            val points = dataPoints.mapIndexed { index, value ->
                val x = index * stepX
                val y = chartHeight * (1f - (value - minVal) / valRange)
                Offset(x, y.coerceIn(0f, chartHeight))
            }

            // Build smooth Bezier curve path
            val strokePath = Path().apply {
                if (points.isNotEmpty()) {
                    moveTo(points.first().x, points.first().y)
                    for (i in 0 until points.size - 1) {
                        val p1 = points[i]
                        val p2 = points[i + 1]
                        val controlP1 = Offset(p1.x + (p2.x - p1.x) / 2f, p1.y)
                        val controlP2 = Offset(p1.x + (p2.x - p1.x) / 2f, p2.y)
                        cubicTo(controlP1.x, controlP1.y, controlP2.x, controlP2.y, p2.x, p2.y)
                    }
                }
            }

            // Gradient Fill under path
            val fillPath = Path().apply {
                addPath(strokePath)
                if (points.isNotEmpty()) {
                    lineTo(points.last().x, chartHeight)
                    lineTo(points.first().x, chartHeight)
                    close()
                }
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.35f),
                        lineColor.copy(alpha = 0.02f)
                    ),
                    startY = 0f,
                    endY = chartHeight
                )
            )

            // Draw line stroke
            drawPath(
                path = strokePath,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw data points
            points.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex

                drawCircle(
                    color = if (isSelected) lineColor else surfaceVariantColor,
                    radius = if (isSelected) 7.dp.toPx() else 4.dp.toPx(),
                    center = point
                )
                drawCircle(
                    color = lineColor,
                    radius = if (isSelected) 5.dp.toPx() else 3.dp.toPx(),
                    center = point,
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }

        // Labels row below canvas
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val step = when {
                labels.size > 20 -> 5
                labels.size > 10 -> 2
                else -> 1
            }

            labels.forEachIndexed { index, label ->
                if (index == 0 || index == labels.lastIndex || index % step == 0) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (index == selectedIndex) lineColor else labelTextColor,
                        fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

