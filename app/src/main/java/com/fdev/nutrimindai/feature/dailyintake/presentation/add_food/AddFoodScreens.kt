package com.fdev.nutrimindai.feature.dailyintake.presentation.add_food

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Food
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodBottomSheet(
    onDismiss: () -> Unit,
    onNavigateToCamera: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToSearch: () -> Unit,
    onNavigateToManual: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.food_add_food_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(20.dp))

            AddFoodOption(
                icon = Icons.Default.CameraAlt,
                title = stringResource(R.string.food_take_photo_title),
                subtitle = stringResource(R.string.food_take_photo_subtitle),
                iconTint = Color(0xFF2196F3),
                onClick = {
                    onDismiss()
                    onNavigateToCamera()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            AddFoodOption(
                icon = Icons.Default.PhotoLibrary,
                title = stringResource(R.string.food_choose_gallery_title),
                subtitle = stringResource(R.string.food_choose_gallery_subtitle),
                iconTint = Color(0xFF9C27B0),
                onClick = {
                    onDismiss()
                    onNavigateToGallery()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            AddFoodOption(
                icon = Icons.Default.Search,
                title = stringResource(R.string.food_search_food_title),
                subtitle = stringResource(R.string.food_search_food_subtitle),
                iconTint = Color(0xFF4CAF50),
                onClick = {
                    onDismiss()
                    onNavigateToSearch()
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            AddFoodOption(
                icon = Icons.Default.Edit,
                title = stringResource(R.string.food_manual_entry_title),
                subtitle = stringResource(R.string.food_manual_entry_subtitle),
                iconTint = Color(0xFFFF9800),
                onClick = {
                    onDismiss()
                    onNavigateToManual()
                }
            )


            Spacer(modifier = Modifier.height(24.dp))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun AddFoodOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconTint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
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
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = title, tint = iconTint)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun PlaceholderDialog(
    title: String,
    description: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        icon = { Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(text = title, fontWeight = FontWeight.Bold) },
        text = { Text(text = description) },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(stringResource(R.string.food_dialog_ok))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFoodScreen(
    mealType: String,
    viewModel: AddFoodViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    var selectedFoodForConfirmation by remember { mutableStateOf<Food?>(null) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    if (selectedFoodForConfirmation != null) {
        val food = selectedFoodForConfirmation!!
        AlertDialog(
            onDismissRequest = { selectedFoodForConfirmation = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(text = food.name, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text(text = stringResource(R.string.food_serving_label, food.servingSize, food.servingUnit))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = stringResource(R.string.food_calories_label, food.calories))
                    Text(text = stringResource(R.string.food_macros_label, food.protein, food.carbs, food.fat))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val foodToSave = selectedFoodForConfirmation!!
                        selectedFoodForConfirmation = null
                        viewModel.saveFood(
                            mealTypeString = mealType,
                            foodName = foodToSave.name,
                            servingSizeText = foodToSave.servingSize.toString(),
                            servingUnit = foodToSave.servingUnit,
                            caloriesText = foodToSave.calories.toString(),
                            proteinText = foodToSave.protein.toString(),
                            carbsText = foodToSave.carbs.toString(),
                            fatText = foodToSave.fat.toString(),
                            foodSource = FoodSource.SEARCH,
                            onSuccess = onNavigateBack
                        )
                    }
                ) {
                    Text(stringResource(R.string.food_add_food_title))
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedFoodForConfirmation = null }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.food_search_food_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                placeholder = { Text(stringResource(R.string.food_search_placeholder)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(16.dp),
                singleLine = true
            )

            if (uiState is AddFoodUiState.Loading) {
                Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(foods, key = { it.id }) { food ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedFoodForConfirmation = food },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = food.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(
                                        R.string.food_search_item_summary_format,
                                        food.calories,
                                        food.protein.toInt(),
                                        food.carbs.toInt(),
                                        food.fat.toInt()
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = { selectedFoodForConfirmation = food },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = stringResource(R.string.food_select_action_desc),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    mealType: String,
    viewModel: AddFoodViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("1") }
    var servingUnit by remember { mutableStateOf("serving") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.food_manual_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        viewModel.saveFood(
                            mealTypeString = mealType,
                            foodName = foodName,
                            servingSizeText = servingSize,
                            servingUnit = servingUnit,
                            caloriesText = calories,
                            proteinText = protein,
                            carbsText = carbs,
                            fatText = fat,
                            fiberText = fiber,
                            foodSource = FoodSource.MANUAL,
                            onSuccess = onNavigateBack
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    enabled = foodName.isNotBlank() && calories.isNotBlank() && uiState !is AddFoodUiState.Loading
                ) {
                    if (uiState is AddFoodUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text(stringResource(R.string.food_btn_save), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState is AddFoodUiState.ValidationError) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (uiState as AddFoodUiState.ValidationError).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            OutlinedTextField(
                value = foodName,
                onValueChange = { foodName = it },
                label = { Text(stringResource(R.string.food_label_name)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = servingSize,
                    onValueChange = { servingSize = it },
                    label = { Text(stringResource(R.string.food_label_serving_size)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = servingUnit,
                    onValueChange = { servingUnit = it },
                    label = { Text(stringResource(R.string.food_label_serving_unit)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text(stringResource(R.string.food_label_calories)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text(stringResource(R.string.food_label_protein)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text(stringResource(R.string.food_label_carbs)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text(stringResource(R.string.food_label_fat)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = fiber,
                    onValueChange = { fiber = it },
                    label = { Text(stringResource(R.string.food_label_fiber)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}
