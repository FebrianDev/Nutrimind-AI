package com.fdev.nutrimindai.feature.dailyintake.presentation.meal_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.fdev.nutrimindai.R
import coil.compose.AsyncImage
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Intake
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetailScreen(
    viewModel: MealDetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToAddFood: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedIntakeForDetail by remember { mutableStateOf<Intake?>(null) }

    if (selectedIntakeForDetail != null) {
        val intake = selectedIntakeForDetail!!
        FoodItemDetailDialog(
            intake = intake,
            isDraft = (uiState as? MealDetailUiState.Success)?.status == DailyLogStatus.DRAFT,
            onDismiss = { selectedIntakeForDetail = null },
            onDelete = {
                viewModel.deleteIntake(intake)
                selectedIntakeForDetail = null
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (uiState is MealDetailUiState.Success) (uiState as MealDetailUiState.Success).mealType else "",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.scan_btn_back))
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState is MealDetailUiState.Success) {
                val state = uiState as MealDetailUiState.Success
                if (state.status == DailyLogStatus.DRAFT && state.intakes.isNotEmpty()) {
                    ExtendedFloatingActionButton(
                        onClick = { onNavigateToAddFood(viewModel.mealType) },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text(stringResource(R.string.label_add_food_btn), fontWeight = FontWeight.Bold) },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is MealDetailUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is MealDetailUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                is MealDetailUiState.Success -> {
                    if (state.intakes.isEmpty()) {
                        MealDetailEmptyState(
                            isDraft = state.status == DailyLogStatus.DRAFT,
                            onAddFoodClick = { onNavigateToAddFood(viewModel.mealType) }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(top = 8.dp, bottom = 88.dp)
                        ) {
                            item {
                                MealSummaryCard(
                                    calories = state.totalCalories,
                                    protein = state.totalProtein,
                                    carbs = state.totalCarbs,
                                    fat = state.totalFat
                                )
                            }
                            item {
                                Text(
                                    text = stringResource(R.string.meal_detail_logged_foods),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            items(state.intakes, key = { it.id }) { intake ->
                                FoodItemCard(
                                    intake = intake,
                                    isDraft = state.status == DailyLogStatus.DRAFT,
                                    onClick = { selectedIntakeForDetail = intake },
                                    onDelete = { viewModel.deleteIntake(intake) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FoodItemDetailDialog(
    intake: Intake,
    isDraft: Boolean,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = intake.foodName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = intake.foodSource.name.lowercase().replaceFirstChar { it.titlecase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (!intake.imagePath.isNullOrBlank() && File(intake.imagePath).exists()) {
                    AsyncImage(
                        model = File(intake.imagePath),
                        contentDescription = intake.foodName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }

                // Calorie hero box
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(stringResource(R.string.meal_detail_total_energy), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(stringResource(R.string.home_intake_kcal_val, intake.calories), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        }
                        Text(stringResource(R.string.meal_serving_format, intake.servingSize, intake.servingUnit), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                }

                Text(stringResource(R.string.meal_detail_nutritional_breakdown), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroBadge(label = stringResource(R.string.label_protein), value = stringResource(R.string.macro_val_g, intake.protein), color = Color(0xFF4CAF50), modifier = Modifier.weight(1f))
                    MacroBadge(label = stringResource(R.string.label_carbs), value = stringResource(R.string.macro_val_g, intake.carbs), color = Color(0xFFFF9800), modifier = Modifier.weight(1f))
                    MacroBadge(label = stringResource(R.string.label_fats), value = stringResource(R.string.macro_val_g, intake.fat), color = Color(0xFFF44336), modifier = Modifier.weight(1f))
                    MacroBadge(label = stringResource(R.string.label_fiber), value = stringResource(R.string.macro_val_g, intake.fiber), color = Color(0xFF9C27B0), modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.meal_detail_btn_close))
            }
        },
        dismissButton = {
            if (isDraft) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.meal_detail_btn_delete_item))
                }
            }
        }
    )
}

@Composable
fun MacroBadge(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = color, fontSize = 10.sp)
            Spacer(modifier = Modifier.height(2.dp))
            Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MealDetailEmptyState(
    isDraft: Boolean,
    onAddFoodClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.meal_detail_empty_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.meal_detail_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (isDraft) {
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = onAddFoodClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = stringResource(R.string.label_add_food_btn), fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun MealSummaryCard(
    calories: Int,
    protein: Double,
    carbs: Double,
    fat: Double
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = stringResource(R.string.meal_detail_total_nutrition),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$calories",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.unit_kcal),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacroStatItem(label = stringResource(R.string.label_protein), value = "${protein.toInt()}g")
                MacroStatItem(label = stringResource(R.string.label_carbs), value = "${carbs.toInt()}g")
                MacroStatItem(label = stringResource(R.string.label_fats), value = "${fat.toInt()}g")
            }
        }
    }
}

@Composable
fun MacroStatItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FoodItemCard(
    intake: Intake,
    isDraft: Boolean,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!intake.imagePath.isNullOrBlank() && File(intake.imagePath).exists()) {
                AsyncImage(
                    model = File(intake.imagePath),
                    contentDescription = intake.foodName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Restaurant,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = intake.foodName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.meal_food_item_desc, intake.servingSize, intake.servingUnit, intake.calories),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            if (isDraft) {
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.meal_detail_delete_food_desc),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
