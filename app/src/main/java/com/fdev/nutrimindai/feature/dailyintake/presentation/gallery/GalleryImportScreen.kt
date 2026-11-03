package com.fdev.nutrimindai.feature.dailyintake.presentation.gallery

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.*
import com.fdev.nutrimindai.core.designsystem.component.NutriMindConfirmationDialog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.presentation.camera.CameraScanUiState
import com.fdev.nutrimindai.feature.dailyintake.presentation.camera.CameraScanViewModel
import com.fdev.nutrimindai.feature.dailyintake.presentation.common.RecognizedFoodScreen
import com.fdev.nutrimindai.feature.dailyintake.presentation.common.capitalized

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryImportScreen(
    mealType: String,
    viewModel: CameraScanViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit = onNavigateBack
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var hasLaunchedPickerInitially by remember { mutableStateOf(false) }

    BackHandler {
        if (uiState is CameraScanUiState.Recognized) {
            viewModel.resetState()
            selectedUri = null
        } else {
            onNavigateBack()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri
            viewModel.analyzeGalleryUri(uri)
        }
    }

    LaunchedEffect(Unit) {
        if (!hasLaunchedPickerInitially) {
            hasLaunchedPickerInitially = true
            galleryLauncher.launch("image/*")
        }
    }

    // Result screen when food in gallery photo is recognized
    if (uiState is CameraScanUiState.Recognized) {
        val recognizedState = uiState as CameraScanUiState.Recognized
        RecognizedFoodScreen(
            result = recognizedState.result,
            bitmap = recognizedState.imageBitmap,
            mealType = mealType,
            badgeText = stringResource(R.string.gallery_verified_badge),
            badgeIcon = Icons.Default.PhotoLibrary,
            badgeColor = FoodInputGallery,
            retakeButtonText = stringResource(R.string.gallery_btn_pick_again),
            onSave = { name, serving, unit, cal, prot, carb, fat, fib ->
                viewModel.saveRecognizedFood(
                    mealTypeString = mealType,
                    foodName = name,
                    servingSizeText = serving,
                    servingUnit = unit,
                    caloriesText = cal,
                    proteinText = prot,
                    carbsText = carb,
                    fatText = fat,
                    fiberText = fib,
                    source = FoodSource.GALLERY,
                    onSuccess = onSaveSuccess
                )
            },
            onRetake = {
                viewModel.resetState()
                selectedUri = null
                galleryLauncher.launch("image/*")
            }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.gallery_import_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = stringResource(R.string.gallery_import_subtitle, mealType.lowercase().capitalized()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.scan_btn_back)
                        )
                    }
                },
                actions = {
                    if (selectedUri != null) {
                        IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = stringResource(R.string.gallery_btn_change_photo),
                                tint = FoodInputGallery
                            )
                        }
                    }
                }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .screenPadding()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.m)
            ) {
                // If an image is selected, display photo preview card
                if (selectedUri != null) {
                    Card(
                        shape = RoundedCornerShape(AppSpacing.l),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            AsyncImage(
                                model = selectedUri,
                                contentDescription = stringResource(R.string.scan_scanned_food_desc),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // Floating badge
                            Surface(
                                color = Color.Black.copy(alpha = 0.7f),
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
                                        imageVector = Icons.Default.PhotoLibrary,
                                        contentDescription = null,
                                        tint = FoodInputGallery.copy(alpha = 0.85f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(AppSpacing.xs))
                                    Text(
                                        text = stringResource(R.string.gallery_import_title),
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons when image is chosen
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)
                    ) {
                        Button(
                            onClick = { selectedUri?.let { viewModel.analyzeGalleryUri(it) } },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(AppSpacing.sm),
                            colors = ButtonDefaults.buttonColors(containerColor = FoodInputGallery)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(AppSpacing.s))
                            Text(stringResource(R.string.gallery_btn_analyze), fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = { galleryLauncher.launch("image/*") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(AppSpacing.sm),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = FoodInputGallery)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(AppSpacing.s))
                            Text(stringResource(R.string.gallery_btn_change_photo))
                        }
                    }
                } else {
                    // Empty State: Hero Upload Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { galleryLauncher.launch("image/*") },
                        shape = RoundedCornerShape(AppSpacing.xl),
                        colors = CardDefaults.cardColors(
                            containerColor = FoodInputGallery.copy(alpha = 0.08f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            FoodInputGallery.copy(alpha = 0.35f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = AppSpacing.xxl, horizontal = AppSpacing.xl),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .clip(CircleShape)
                                    .background(FoodInputGallery.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = FoodInputGallery,
                                    modifier = Modifier.size(40.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(AppSpacing.m))
                            Text(
                                text = stringResource(R.string.gallery_hero_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.s))
                            Text(
                                text = stringResource(R.string.gallery_hero_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = AppSpacing.s)
                            )
                            Spacer(modifier = Modifier.height(AppSpacing.l))
                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                shape = RoundedCornerShape(AppSpacing.sm),
                                colors = ButtonDefaults.buttonColors(containerColor = FoodInputGallery)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(AppSpacing.s))
                                Text(
                                    text = stringResource(R.string.gallery_btn_open_gallery),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Best Practices Tips Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(AppSpacing.m),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .cardPadding()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.AutoAwesome,
                                contentDescription = null,
                                tint = FoodInputGallery,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(AppSpacing.s))
                            Text(
                                text = stringResource(R.string.gallery_tips_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        GalleryTipItem(text = stringResource(R.string.gallery_tip_1))
                        Spacer(modifier = Modifier.height(AppSpacing.s))
                        GalleryTipItem(text = stringResource(R.string.gallery_tip_2))
                        Spacer(modifier = Modifier.height(AppSpacing.s))
                        GalleryTipItem(text = stringResource(R.string.gallery_tip_3))
                    }
                }

                Spacer(modifier = Modifier.height(AppSpacing.m))
            }
        }

        // Analyzing Overlay State
        AnimatedVisibility(
                visible = uiState is CameraScanUiState.Analyzing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.8f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = FoodInputGallery,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.xl))
                        Text(
                            text = stringResource(R.string.gallery_analyzing_title),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.s))
                        Text(
                            text = stringResource(R.string.gallery_analyzing_subtitle),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = AppSpacing.xxl)
                        )
                    }
                }
            }

            // Error Dialog
            if (uiState is CameraScanUiState.Error) {
                val errorMessage = (uiState as CameraScanUiState.Error).message
                NutriMindConfirmationDialog(
                    title = stringResource(R.string.scan_recognition_failed_title),
                    description = errorMessage,
                    icon = Icons.Default.Error,
                    iconTint = MaterialTheme.colorScheme.error,
                    confirmButtonText = stringResource(R.string.gallery_btn_pick_again),
                    dismissButtonText = stringResource(R.string.dialog_cancel),
                    onConfirm = {
                        viewModel.resetState()
                        galleryLauncher.launch("image/*")
                    },
                    onDismissRequest = { viewModel.resetState() }
                )
            }
        }
    }

@Composable
private fun GalleryTipItem(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(FoodInputGallery)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
