package com.fdev.nutrimindai.feature.dailyintake.presentation.water

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.res.stringResource
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.component.NutriMindConfirmationDialog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Water

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterScreen(
    viewModel: WaterViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showCustomWaterDialog by remember { mutableStateOf(false) }
    var waterToDelete by remember { mutableStateOf<Water?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkDateAndReloadIfNeeded()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (waterToDelete != null) {
        val water = waterToDelete!!
        NutriMindConfirmationDialog(
            title = stringResource(R.string.water_delete_confirm_title),
            description = stringResource(R.string.water_delete_confirm_msg),
            icon = Icons.Default.Delete,
            isDanger = true,
            confirmButtonText = stringResource(R.string.meal_detail_btn_delete_item),
            dismissButtonText = stringResource(R.string.dialog_cancel),
            onConfirm = {
                viewModel.deleteWater(water)
                waterToDelete = null
            },
            onDismissRequest = {
                waterToDelete = null
            }
        )
    }

    if (showCustomWaterDialog) {
        CustomWaterDialog(
            onDismiss = { showCustomWaterDialog = false },
            onConfirm = { amount ->
                showCustomWaterDialog = false
                viewModel.addWater(amount)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.title_water_intake), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when (val state = uiState) {
                is WaterUiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                is WaterUiState.Error -> {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is WaterUiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 32.dp)
                    ) {
                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            WaterProgressCard(
                                totalMl = state.totalMl,
                                goalMl = state.goalMl,
                                exerciseMinutes = state.exerciseMinutes
                            )
                        }

                        if (state.status == DailyLogStatus.DRAFT) {
                            item {
                                Spacer(modifier = Modifier.height(8.dp))
                                QuickAddSection(
                                    onAddQuick = { viewModel.addWater(it) },
                                    onCustomClick = { showCustomWaterDialog = true }
                                )
                            }
                        }

                        item {
                            Text(
                                text = stringResource(R.string.water_history_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }

                        if (state.waterRecords.isEmpty()) {
                            item {
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(R.string.water_empty_hint),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        } else {
                            items(state.waterRecords, key = { it.id }) { record ->
                                WaterHistoryCard(
                                    water = record,
                                    isDraft = state.status == DailyLogStatus.DRAFT,
                                    onDelete = { waterToDelete = record }
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
fun WaterProgressCard(
    totalMl: Int,
    goalMl: Int = 2500,
    exerciseMinutes: Int = 0
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.water_todays_intake_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = String.format(java.util.Locale.US, "%.1f", totalMl / 1000f),
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0288D1)
                )
                Text(
                    text = " " + stringResource(R.string.water_progress_format, goalMl / 1000f),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { if (goalMl > 0) (totalMl.toFloat() / goalMl).coerceIn(0f, 1f) else 0f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp)),
                color = Color(0xFF0288D1),
                trackColor = MaterialTheme.colorScheme.surface
            )

            if (exerciseMinutes > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                val extraWater = ((exerciseMinutes / 30f) * 350f).toInt()
                Text(
                    text = stringResource(R.string.water_dynamic_goal_note, extraWater),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF0288D1),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun QuickAddSection(
    onAddQuick: (Int) -> Unit,
    onCustomClick: () -> Unit
) {
    Column {
        Text(
            text = stringResource(R.string.water_quick_add_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickAddButton("+250 ml", Modifier.weight(1f)) { onAddQuick(250) }
            QuickAddButton("+500 ml", Modifier.weight(1f)) { onAddQuick(500) }
            QuickAddButton("+750 ml", Modifier.weight(1f)) { onAddQuick(750) }
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(
            onClick = onCustomClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.label_custom), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun QuickAddButton(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color(0xFF0288D1).copy(alpha = 0.15f),
            contentColor = Color(0xFF0288D1)
        ),
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
fun CustomWaterDialog(
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(R.string.dialog_custom_water_title), fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(stringResource(R.string.dialog_custom_water_desc))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        isError = false
                    },
                    label = { Text(stringResource(R.string.label_water_amount)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isError) {
                    Text(
                        text = stringResource(R.string.error_invalid_number),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toIntOrNull()
                    if (amount != null && amount > 0) {
                        onConfirm(amount)
                    } else {
                        isError = true
                    }
                }
            ) {
                Text(stringResource(R.string.btn_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
fun WaterHistoryCard(
    water: Water,
    isDraft: Boolean,
    onDelete: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF0288D1).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocalDrink,
                    contentDescription = null,
                    tint = Color(0xFF0288D1),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.water_amount_ml_format, water.amountMl),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (water.time.isNotBlank()) {
                    Text(
                        text = water.time,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isDraft) {
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.water_delete_record_desc),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
