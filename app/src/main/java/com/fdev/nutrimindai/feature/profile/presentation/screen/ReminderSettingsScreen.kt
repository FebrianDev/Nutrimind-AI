package com.fdev.nutrimindai.feature.profile.presentation.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.BuildConfig
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSettingsScreen(
    viewModel: ReminderSettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.reminderSettings.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val permissionGrantedSnackbar = stringResource(R.string.reminder_permission_granted)
    val testNotificationSentSnackbar = stringResource(R.string.reminder_test_sent_snackbar)

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(permissionGrantedSnackbar)
            }
        }
    }

    var showTimePickerDialog by remember { mutableStateOf<TimePickerTarget?>(null) }
    var showWaterWindowDialog by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.reminder_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Permission Notice Card (if permission not yet granted on Android 13+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.reminder_permission_needed_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                        Text(
                            text = stringResource(R.string.reminder_permission_needed_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.reminder_enable_notifications), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // --- MEAL REMINDERS SECTION ---
            SectionHeader(
                title = stringResource(R.string.reminder_section_meals_title),
                subtitle = stringResource(R.string.reminder_section_meals_subtitle)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column {
                    ReminderRow(
                        icon = Icons.Default.WbSunny,
                        iconTint = Color(0xFFFFB300),
                        title = stringResource(R.string.reminder_breakfast_title),
                        subtitle = stringResource(R.string.reminder_breakfast_subtitle),
                        timeString = settings.breakfastTime,
                        isEnabled = settings.breakfastEnabled,
                        onToggle = { enabled -> viewModel.updateBreakfast(enabled, settings.breakfastTime) },
                        onTimeClick = {
                            showTimePickerDialog = TimePickerTarget.Breakfast(settings.breakfastTime)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                    ReminderRow(
                        icon = Icons.Default.Restaurant,
                        iconTint = NutritionCalories,
                        title = stringResource(R.string.reminder_lunch_title),
                        subtitle = stringResource(R.string.reminder_lunch_subtitle),
                        timeString = settings.lunchTime,
                        isEnabled = settings.lunchEnabled,
                        onToggle = { enabled -> viewModel.updateLunch(enabled, settings.lunchTime) },
                        onTimeClick = {
                            showTimePickerDialog = TimePickerTarget.Lunch(settings.lunchTime)
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                    ReminderRow(
                        icon = Icons.Default.NightsStay,
                        iconTint = Color(0xFF5C6BC0),
                        title = stringResource(R.string.reminder_dinner_title),
                        subtitle = stringResource(R.string.reminder_dinner_subtitle),
                        timeString = settings.dinnerTime,
                        isEnabled = settings.dinnerEnabled,
                        onToggle = { enabled -> viewModel.updateDinner(enabled, settings.dinnerTime) },
                        onTimeClick = {
                            showTimePickerDialog = TimePickerTarget.Dinner(settings.dinnerTime)
                        }
                    )
                }
            }

            // --- HYDRATION REMINDERS SECTION ---
            SectionHeader(
                title = stringResource(R.string.reminder_section_hydration_title),
                subtitle = stringResource(R.string.reminder_section_hydration_subtitle)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(NutritionWater.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.WaterDrop,
                                contentDescription = null,
                                tint = NutritionWater,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.reminder_water_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.reminder_water_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.waterEnabled,
                            onCheckedChange = { enabled ->
                                viewModel.updateWater(
                                    enabled = enabled,
                                    intervalHours = settings.waterIntervalHours,
                                    startTime = settings.waterStartTime,
                                    endTime = settings.waterEndTime
                                )
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    AnimatedVisibility(visible = settings.waterEnabled) {
                        Column(modifier = Modifier.padding(top = 16.dp)) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = stringResource(R.string.reminder_water_interval_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // Interval selector chips
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val intervals = listOf(
                                    Pair(stringResource(R.string.reminder_water_interval_1h), 1.0f),
                                    Pair(stringResource(R.string.reminder_water_interval_1_5h), 1.5f),
                                    Pair(stringResource(R.string.reminder_water_interval_2h), 2.0f),
                                    Pair(stringResource(R.string.reminder_water_interval_3h), 3.0f)
                                )
                                intervals.forEach { (label, value) ->
                                    val isSelected = settings.waterIntervalHours == value
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = {
                                            viewModel.updateWater(
                                                enabled = true,
                                                intervalHours = value,
                                                startTime = settings.waterStartTime,
                                                endTime = settings.waterEndTime
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = label,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                            selectedLabelColor = MaterialTheme.colorScheme.primary
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Active window
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showWaterWindowDialog = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            text = stringResource(R.string.reminder_water_window_title),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${settings.waterStartTime} – ${settings.waterEndTime}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = stringResource(R.string.reminder_water_window_edit_desc),
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            Text(
                                text = stringResource(R.string.reminder_water_quick_log_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val quickAmounts = listOf(200, 250, 330, 500)
                                quickAmounts.forEach { amount ->
                                    val isSelected = settings.quickLogWaterAmountMl == amount
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { viewModel.updateQuickLogAmount(amount) },
                                        label = {
                                            Text(
                                                text = "+$amount ml",
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 12.sp
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = NutritionWater.copy(alpha = 0.2f),
                                            selectedLabelColor = NutritionWater
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- DAILY REVIEW SECTION ---
            SectionHeader(
                title = stringResource(R.string.reminder_section_daily_review_title),
                subtitle = stringResource(R.string.reminder_section_daily_review_subtitle)
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                ReminderRow(
                    icon = Icons.Default.AutoAwesome,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.reminder_daily_review_title),
                    subtitle = stringResource(R.string.reminder_daily_review_subtitle),
                    timeString = settings.dailyReviewTime,
                    isEnabled = settings.dailyReviewEnabled,
                    onToggle = { enabled -> viewModel.updateDailyReview(enabled, settings.dailyReviewTime) },
                    onTimeClick = {
                        showTimePickerDialog = TimePickerTarget.DailyReview(settings.dailyReviewTime)
                    }
                )
            }

            // --- SMART CONTEXT SECTION ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.reminder_smart_skip_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.reminder_smart_skip_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Switch(
                        checked = settings.smartSkipIfLogged,
                        onCheckedChange = { viewModel.updateSmartSkip(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }

            // --- DEMO & TEST SECTION ---
            if (BuildConfig.DEBUG) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.NotificationsActive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.reminder_test_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(R.string.reminder_test_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.sendTestWaterNotification()
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(testNotificationSentSnackbar)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.reminder_btn_test_water), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    viewModel.sendTestMealNotification("Lunch")
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar(testNotificationSentSnackbar)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(stringResource(R.string.reminder_btn_test_meal), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Time Picker Dialog
    showTimePickerDialog?.let { target ->
        val (initialHour, initialMin) = parseTimeString(target.timeStr)
        val timePickerState = rememberTimePickerState(
            initialHour = initialHour,
            initialMinute = initialMin,
            is24Hour = true
        )

        val (dialogTitle, dialogIcon, accentColor) = when (target) {
            is TimePickerTarget.Breakfast -> Triple(stringResource(R.string.reminder_dialog_set_breakfast_time), Icons.Default.WbSunny, Color(0xFFFFB300))
            is TimePickerTarget.Lunch -> Triple(stringResource(R.string.reminder_dialog_set_lunch_time), Icons.Default.Restaurant, NutritionCalories)
            is TimePickerTarget.Dinner -> Triple(stringResource(R.string.reminder_dialog_set_dinner_time), Icons.Default.NightsStay, Color(0xFF5C6BC0))
            is TimePickerTarget.DailyReview -> Triple(stringResource(R.string.reminder_dialog_set_daily_review_time), Icons.Default.AutoAwesome, MaterialTheme.colorScheme.primary)
        }

        AlertDialog(
            onDismissRequest = { showTimePickerDialog = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = dialogIcon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = dialogTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            clockDialSelectedContentColor = Color.White,
                            clockDialUnselectedContentColor = MaterialTheme.colorScheme.onSurface,
                            selectorColor = accentColor,
                            containerColor = MaterialTheme.colorScheme.surface,
                            periodSelectorBorderColor = accentColor,
                            periodSelectorSelectedContainerColor = accentColor.copy(alpha = 0.2f),
                            periodSelectorUnselectedContainerColor = Color.Transparent,
                            periodSelectorSelectedContentColor = accentColor,
                            periodSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            timeSelectorSelectedContainerColor = accentColor.copy(alpha = 0.18f),
                            timeSelectorUnselectedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            timeSelectorSelectedContentColor = accentColor,
                            timeSelectorUnselectedContentColor = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val formattedTime = String.format("%02d:%02d", timePickerState.hour, timePickerState.minute)
                        when (target) {
                            is TimePickerTarget.Breakfast -> viewModel.updateBreakfast(true, formattedTime)
                            is TimePickerTarget.Lunch -> viewModel.updateLunch(true, formattedTime)
                            is TimePickerTarget.Dinner -> viewModel.updateDinner(true, formattedTime)
                            is TimePickerTarget.DailyReview -> viewModel.updateDailyReview(true, formattedTime)
                        }
                        showTimePickerDialog = null
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = if (accentColor == Color(0xFFFFB300)) Color(0xFF332000) else Color.White
                    )
                ) {
                    Text(stringResource(R.string.reminder_dialog_save_time), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePickerDialog = null },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }

    // Active Water Window Configuration Dialog
    if (showWaterWindowDialog) {
        var startHourText by remember { mutableStateOf(settings.waterStartTime) }
        var endHourText by remember { mutableStateOf(settings.waterEndTime) }

        AlertDialog(
            onDismissRequest = { showWaterWindowDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(24.dp),
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(NutritionWater.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WaterDrop,
                            contentDescription = null,
                            tint = NutritionWater,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = stringResource(R.string.reminder_dialog_water_window_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        text = stringResource(R.string.reminder_dialog_water_window_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = startHourText,
                        onValueChange = { startHourText = it },
                        label = { Text(stringResource(R.string.reminder_dialog_water_start_time)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = NutritionWater,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NutritionWater,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = NutritionWater,
                            cursorColor = NutritionWater,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = endHourText,
                        onValueChange = { endHourText = it },
                        label = { Text(stringResource(R.string.reminder_dialog_water_end_time)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.NightsStay,
                                contentDescription = null,
                                tint = NutritionWater,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NutritionWater,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedLabelColor = NutritionWater,
                            cursorColor = NutritionWater,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWater(
                            enabled = true,
                            intervalHours = settings.waterIntervalHours,
                            startTime = startHourText.trim(),
                            endTime = endHourText.trim()
                        )
                        showWaterWindowDialog = false
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NutritionWater,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.reminder_dialog_save_window), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showWaterWindowDialog = false },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
private fun ReminderRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    timeString: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    onTimeClick: () -> Unit
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
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (isEnabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    onClick = onTimeClick,
                    shape = RoundedCornerShape(8.dp),
                    color = iconTint.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = timeString,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = iconTint
                        )
                    }
                }
            }
        }

        Switch(
            checked = isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

private sealed interface TimePickerTarget {
    val timeStr: String
    data class Breakfast(override val timeStr: String) : TimePickerTarget
    data class Lunch(override val timeStr: String) : TimePickerTarget
    data class Dinner(override val timeStr: String) : TimePickerTarget
    data class DailyReview(override val timeStr: String) : TimePickerTarget
}

private fun parseTimeString(timeStr: String): Pair<Int, Int> {
    return try {
        val parts = timeStr.split(":")
        Pair(parts[0].toInt(), parts[1].toInt())
    } catch (e: Exception) {
        Pair(8, 0)
    }
}
