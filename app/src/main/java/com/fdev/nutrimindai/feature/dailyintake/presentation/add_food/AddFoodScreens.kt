package com.fdev.nutrimindai.feature.dailyintake.presentation.add_food

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.*
import com.fdev.nutrimindai.core.designsystem.component.NutriMindConfirmationDialog
import com.fdev.nutrimindai.core.designsystem.component.NutriMindInfoDialog
import com.fdev.nutrimindai.core.ai.FoodRecognitionResult
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Food
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodBottomSheet(
    onDismiss: () -> Unit,
    onNavigateToCamera: () -> Unit = {},
    onNavigateToBarcode: () -> Unit = {},
    onNavigateToGallery: () -> Unit = {},
    onNavigateToSearch: () -> Unit,
    onNavigateToManual: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .screenPadding()
                .padding(vertical = AppSpacing.m)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.food_add_food_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(AppSpacing.l))

            AddFoodOption(
                icon = Icons.Default.CameraAlt,
                title = stringResource(R.string.food_take_photo_title),
                subtitle = stringResource(R.string.food_take_photo_subtitle),
                iconTint = FoodInputCamera,
                onClick = {
                    onDismiss()
                    onNavigateToCamera()
                }
            )
            Spacer(modifier = Modifier.height(AppSpacing.s))
            AddFoodOption(
                icon = Icons.Default.QrCodeScanner,
                title = stringResource(R.string.food_scan_barcode_title),
                subtitle = stringResource(R.string.food_scan_barcode_subtitle),
                iconTint = FoodInputBarcode,
                onClick = {
                    onDismiss()
                    onNavigateToBarcode()
                }
            )
            Spacer(modifier = Modifier.height(AppSpacing.s))
            AddFoodOption(
                icon = Icons.Default.PhotoLibrary,
                title = stringResource(R.string.food_choose_gallery_title),
                subtitle = stringResource(R.string.food_choose_gallery_subtitle),
                iconTint = FoodInputGallery,
                onClick = {
                    onDismiss()
                    onNavigateToGallery()
                }
            )
            Spacer(modifier = Modifier.height(AppSpacing.s))
            AddFoodOption(
                icon = Icons.Default.Search,
                title = stringResource(R.string.food_search_food_title),
                subtitle = stringResource(R.string.food_search_food_subtitle),
                iconTint = FoodInputSearch,
                onClick = {
                    onDismiss()
                    onNavigateToSearch()
                }
            )
            Spacer(modifier = Modifier.height(AppSpacing.s))
            AddFoodOption(
                icon = Icons.Default.Edit,
                title = stringResource(R.string.food_manual_entry_title),
                subtitle = stringResource(R.string.food_manual_entry_subtitle),
                iconTint = FoodInputManual,
                onClick = {
                    onDismiss()
                    onNavigateToManual()
                }
            )


            Spacer(modifier = Modifier.height(AppSpacing.xl))
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(AppSpacing.m))
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
                .cardPadding(),
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
    NutriMindInfoDialog(
        title = title,
        description = description,
        onDismiss = onDismiss,
        icon = Icons.Default.Info
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFoodScreen(
    mealType: String,
    viewModel: AddFoodViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit = onNavigateBack,
    onNavigateToBarcode: () -> Unit = {}
) {
    BackHandler {
        onNavigateBack()
    }

    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val foods by viewModel.foods.collectAsStateWithLifecycle()
    var selectedFoodForConfirmation by remember { mutableStateOf<Food?>(null) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEstimating by viewModel.isEstimating.collectAsStateWithLifecycle()
    val estimatedFood by viewModel.estimatedFood.collectAsStateWithLifecycle()

    // 1. Confirmation Dialog for Catalog Food
    if (selectedFoodForConfirmation != null) {
        val food = selectedFoodForConfirmation!!
        NutriMindConfirmationDialog(
            title = food.name,
            icon = Icons.Default.Restaurant,
            iconTint = FoodInputSearch,
            confirmButtonText = stringResource(R.string.food_add_food_title),
            dismissButtonText = stringResource(R.string.dialog_cancel),
            onConfirm = {
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
                    onSuccess = onSaveSuccess
                )
            },
            onDismissRequest = { selectedFoodForConfirmation = null },
            text = {
                Column {
                    Text(text = stringResource(R.string.food_serving_label, food.servingSize, food.servingUnit))
                    Spacer(modifier = Modifier.height(AppSpacing.s))
                    Text(text = stringResource(R.string.food_calories_label, food.calories))
                    Text(text = stringResource(R.string.food_macros_label, food.protein, food.carbs, food.fat))
                }
            }
        )
    }

    // 2. Confirmation Dialog for Meta AI Estimated Food
    if (estimatedFood != null) {
        val result = estimatedFood!!
        NutriMindConfirmationDialog(
            title = result.name,
            icon = Icons.Rounded.AutoAwesome,
            iconTint = MaterialTheme.colorScheme.primary,
            confirmButtonText = stringResource(R.string.food_add_food_title),
            dismissButtonText = stringResource(R.string.dialog_cancel),
            onConfirm = {
                val foodToSave = result
                viewModel.clearEstimatedFood()
                viewModel.saveFood(
                    mealTypeString = mealType,
                    foodName = foodToSave.name,
                    servingSizeText = foodToSave.servingSize.toString(),
                    servingUnit = foodToSave.servingUnit,
                    caloriesText = foodToSave.calories.toString(),
                    proteinText = foodToSave.protein.toString(),
                    carbsText = foodToSave.carbs.toString(),
                    fatText = foodToSave.fat.toString(),
                    fiberText = foodToSave.fiber.toString(),
                    foodSource = FoodSource.SEARCH,
                    onSuccess = onSaveSuccess
                )
            },
            onDismissRequest = { viewModel.clearEstimatedFood() },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.s)) {
                    Surface(
                        shape = RoundedCornerShape(AppSpacing.s),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = stringResource(R.string.food_estimated_by_meta_ai),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.badgePadding()
                        )
                    }
                    if (result.description.isNotBlank()) {
                        Text(
                            text = result.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(text = stringResource(R.string.food_serving_label, result.servingSize, result.servingUnit))
                    Text(
                        text = stringResource(R.string.food_calories_label, result.calories),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            R.string.food_macros_label,
                            result.protein.toInt(),
                            result.carbs.toInt(),
                            result.fat.toInt()
                        )
                    )
                }
            }
        )
    }

    // 3. Error Alert Dialog
    if (uiState is AddFoodUiState.Error) {
        val errorMsg = (uiState as AddFoodUiState.Error).message
        NutriMindInfoDialog(
            title = "Estimation Notice",
            description = errorMsg,
            onDismiss = { viewModel.resetState() },
            icon = Icons.Default.Info,
            iconTint = MaterialTheme.colorScheme.error
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.food_search_food_title), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                }
            }
        )

        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.onSearchQueryChange(it) },
            modifier = Modifier
                .fillMaxWidth()
                .screenPadding()
                .padding(vertical = AppSpacing.s),
            placeholder = { Text(stringResource(R.string.food_search_placeholder)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onNavigateToBarcode) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.scan_barcode_btn_tooltip),
                        tint = FoodInputBarcode
                    )
                }
            },
            shape = RoundedCornerShape(AppSpacing.m),
            singleLine = true
        )

            // Optional shortcut to estimate via Meta AI even if local items exist
            if (searchQuery.isNotBlank() && foods.isNotEmpty()) {
                Surface(
                    onClick = { viewModel.estimateNutrition(searchQuery) },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .screenPadding()
                        .padding(vertical = AppSpacing.xs)
                ) {
                    Row(
                        modifier = Modifier.compactCardPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.food_ai_cannot_find_hint, searchQuery),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (isEstimating) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.food_estimating_ai),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Empty state card with Meta AI action
                if (foods.isEmpty() && searchQuery.isNotBlank() && !isEstimating) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                        ) {
                            Column(
                                modifier = Modifier.cardPadding(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = stringResource(R.string.food_ai_no_match_title, searchQuery),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = stringResource(R.string.food_ai_no_match_subtitle),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = { viewModel.estimateNutrition(searchQuery) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = stringResource(R.string.food_estimate_with_ai),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

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
                                .cardPadding(),
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntryScreen(
    mealType: String,
    viewModel: AddFoodViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit = onNavigateBack
) {
    BackHandler {
        onNavigateBack()
    }

    var foodName by remember { mutableStateOf("") }
    var servingSize by remember { mutableStateOf("1") }
    var servingUnit by remember { mutableStateOf("serving") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    var carbs by remember { mutableStateOf("") }
    var fat by remember { mutableStateOf("") }
    var fiber by remember { mutableStateOf("") }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isEstimating by viewModel.isEstimating.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.resetState()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.food_manual_title), fontWeight = FontWeight.Bold) },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.btn_back))
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .screenPadding(),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.m)
        ) {
            if (uiState is AddFoodUiState.ValidationError) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (uiState as AddFoodUiState.ValidationError).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.cardPadding(),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (uiState is AddFoodUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (uiState as AddFoodUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.cardPadding(),
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
                shape = RoundedCornerShape(AppSpacing.sm)
            )

            // One-tap Auto-Fill Nutrition with Meta AI
            Button(
                onClick = {
                    viewModel.estimateNutrition(foodName) { recognized ->
                        foodName = recognized.name
                        servingSize = recognized.servingSize.toString()
                        servingUnit = recognized.servingUnit
                        calories = recognized.calories.toString()
                        protein = recognized.protein.toString()
                        carbs = recognized.carbs.toString()
                        fat = recognized.fat.toString()
                        fiber = recognized.fiber.toString()
                    }
                },
                enabled = foodName.isNotBlank() && !isEstimating,
                shape = RoundedCornerShape(AppSpacing.sm),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                if (isEstimating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.s))
                    Text(stringResource(R.string.food_estimating_ai))
                } else {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.s))
                    Text(
                        text = stringResource(R.string.food_auto_fill_ai),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

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
                    label = { Text(stringResource(R.string.food_label_serving_unit)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(AppSpacing.sm)
                )
            }

            OutlinedTextField(
                value = calories,
                onValueChange = { calories = it },
                label = { Text(stringResource(R.string.food_label_calories)) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                shape = RoundedCornerShape(AppSpacing.sm)
            )

            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                OutlinedTextField(
                    value = protein,
                    onValueChange = { protein = it },
                    label = { Text(stringResource(R.string.food_label_protein)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(AppSpacing.sm)
                )
                OutlinedTextField(
                    value = carbs,
                    onValueChange = { carbs = it },
                    label = { Text(stringResource(R.string.food_label_carbs)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(AppSpacing.sm)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
                OutlinedTextField(
                    value = fat,
                    onValueChange = { fat = it },
                    label = { Text(stringResource(R.string.food_label_fat)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(AppSpacing.sm)
                )
                OutlinedTextField(
                    value = fiber,
                    onValueChange = { fiber = it },
                    label = { Text(stringResource(R.string.food_label_fiber)) },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(AppSpacing.sm)
                )
            }
        }

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
                        onSuccess = onSaveSuccess
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .screenPadding()
                    .padding(vertical = AppSpacing.m)
                    .height(52.dp),
                shape = RoundedCornerShape(AppSpacing.m),
                enabled = foodName.isNotBlank() && calories.isNotBlank() && uiState !is AddFoodUiState.Loading && !isEstimating
            ) {
                if (uiState is AddFoodUiState.Loading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text(stringResource(R.string.food_btn_save), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
