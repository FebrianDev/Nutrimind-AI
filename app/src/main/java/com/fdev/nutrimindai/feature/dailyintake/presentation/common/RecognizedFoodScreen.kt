package com.fdev.nutrimindai.feature.dailyintake.presentation.common

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.ai.FoodRecognitionResult
import com.fdev.nutrimindai.core.designsystem.*
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognizedFoodScreen(
    result: FoodRecognitionResult,
    bitmap: Bitmap?,
    mealType: String,
    badgeText: String = stringResource(R.string.scan_ai_vision_verified),
    badgeIcon: ImageVector = Icons.Default.AutoAwesome,
    badgeColor: Color = FoodInputCamera,
    retakeButtonText: String = stringResource(R.string.scan_btn_scan_again),
    onSave: (name: String, servingSize: String, servingUnit: String, calories: String, protein: String, carbs: String, fat: String, fiber: String) -> Unit,
    onRetake: () -> Unit
) {
    var foodName by remember { mutableStateOf(result.name) }
    var servingSize by remember { mutableStateOf(result.servingSize.toString()) }
    var servingUnit by remember { mutableStateOf(result.servingUnit) }
    var calories by remember { mutableStateOf(result.calories.toString()) }
    var protein by remember { mutableStateOf(result.protein.toString()) }
    var carbs by remember { mutableStateOf(result.carbs.toString()) }
    var fat by remember { mutableStateOf(result.fat.toString()) }
    var fiber by remember { mutableStateOf(result.fiber.toString()) }

    BackHandler {
        onRetake()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.scan_analysis_title), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onRetake) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = retakeButtonText)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .screenPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Photo with Verification Badge if bitmap is present
            if (bitmap != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(AppSpacing.l),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = stringResource(R.string.scan_scanned_food_desc),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Floating Verification badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.75f),
                        shape = RoundedCornerShape(AppSpacing.sm),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(AppSpacing.sm)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                        ) {
                            Icon(
                                imageVector = badgeIcon,
                                contentDescription = null,
                                tint = badgeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.xs))
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.m))
            } else {
                // Non-image result header (e.g., Barcode scanned item)
                Surface(
                    color = badgeColor.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(AppSpacing.m),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = AppSpacing.m, vertical = AppSpacing.sm)
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.sm))
                        Column {
                            Text(
                                text = badgeText,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = badgeColor
                            )
                            Text(
                                text = stringResource(R.string.food_dialog_ok),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(AppSpacing.m))
            }

            // Food Name Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(AppSpacing.m),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.cardPadding()) {
                    Text(
                        text = stringResource(R.string.scan_details_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text(stringResource(R.string.scan_detected_food_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(AppSpacing.sm)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                        OutlinedTextField(
                            value = servingSize,
                            onValueChange = { servingSize = it },
                            label = { Text(stringResource(R.string.food_label_serving_size)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(AppSpacing.sm)
                        )
                        OutlinedTextField(
                            value = servingUnit,
                            onValueChange = { servingUnit = it },
                            label = { Text(stringResource(R.string.scan_unit_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(AppSpacing.sm)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.m))

            // Nutrition Breakdown Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(AppSpacing.m),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.cardPadding()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.scan_nutrition_card_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        val p = protein.toDoubleOrNull() ?: 0.0
                        val c = carbs.toDoubleOrNull() ?: 0.0
                        val f = fat.toDoubleOrNull() ?: 0.0
                        val total = p + c + f
                        if (total > 0.0) {
                            Text(
                                text = stringResource(
                                    R.string.scan_macro_distribution_format,
                                    (p / total * 100).toInt(),
                                    (c / total * 100).toInt(),
                                    (f / total * 100).toInt()
                                ),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text(stringResource(R.string.scan_total_calories_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(AppSpacing.sm)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.sm))
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                        MacroField(
                            label = stringResource(R.string.food_label_protein),
                            value = protein,
                            onValueChange = { protein = it },
                            modifier = Modifier.weight(1f)
                        )
                        MacroField(
                            label = stringResource(R.string.food_label_carbs),
                            value = carbs,
                            onValueChange = { carbs = it },
                            modifier = Modifier.weight(1f)
                        )
                        MacroField(
                            label = stringResource(R.string.food_label_fat),
                            value = fat,
                            onValueChange = { fat = it },
                            modifier = Modifier.weight(1f)
                        )
                        MacroField(
                            label = stringResource(R.string.food_label_fiber),
                            value = fiber,
                            onValueChange = { fiber = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (result.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(AppSpacing.m))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(AppSpacing.m),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.cardPadding()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.s))
                            Text(
                                text = stringResource(R.string.scan_ai_nutrition_insights),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.s))
                        Text(
                            text = result.description,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppSpacing.xl))
        }

        Surface(
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .screenPadding()
                    .padding(vertical = AppSpacing.m),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)
            ) {
                OutlinedButton(
                    onClick = onRetake,
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(AppSpacing.sm)
                ) {
                    Text(retakeButtonText)
                }

                Button(
                    onClick = {
                        onSave(foodName, servingSize, servingUnit, calories, protein, carbs, fat, fiber)
                    },
                    modifier = Modifier
                        .weight(1.5f)
                        .height(52.dp),
                    shape = RoundedCornerShape(AppSpacing.sm),
                    enabled = foodName.isNotBlank() && calories.isNotBlank()
                ) {
                    Text(
                        text = stringResource(R.string.scan_btn_add_to_meal, mealType.lowercase().capitalized()),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MacroField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 10.sp) },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        singleLine = true,
        shape = RoundedCornerShape(AppSpacing.s)
    )
}

fun String.capitalized(): String = replaceFirstChar {
    if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
}
