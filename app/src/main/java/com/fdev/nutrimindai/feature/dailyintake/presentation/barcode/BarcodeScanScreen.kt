package com.fdev.nutrimindai.feature.dailyintake.presentation.barcode

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.designsystem.*
import com.fdev.nutrimindai.core.designsystem.component.NutriMindConfirmationDialog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.presentation.camera.BarcodeAnalyzer
import com.fdev.nutrimindai.feature.dailyintake.presentation.camera.CameraScanUiState
import com.fdev.nutrimindai.feature.dailyintake.presentation.camera.CameraScanViewModel
import com.fdev.nutrimindai.feature.dailyintake.presentation.camera.ScanMode
import com.fdev.nutrimindai.feature.dailyintake.presentation.common.RecognizedFoodScreen
import com.fdev.nutrimindai.feature.dailyintake.presentation.common.capitalized
import timber.log.Timber
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeScanScreen(
    mealType: String,
    viewModel: CameraScanViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit = onNavigateBack
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val torchEnabled by viewModel.torchEnabled.collectAsStateWithLifecycle()

    BackHandler {
        if (uiState is CameraScanUiState.Recognized) {
            viewModel.resetState()
        } else {
            onNavigateBack()
        }
    }

    var showManualInputDialog by remember { mutableStateOf(false) }
    var manualBarcodeText by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.setScanMode(ScanMode.BARCODE)
    }

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    var camera by remember { mutableStateOf<Camera?>(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeAnalyzer = remember {
        BarcodeAnalyzer { barcode ->
            viewModel.onBarcodeScanned(barcode)
        }
    }

    DisposableEffect(barcodeAnalyzer, cameraExecutor) {
        onDispose {
            barcodeAnalyzer.close()
            cameraExecutor.shutdown()
        }
    }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
    }

    LaunchedEffect(uiState) {
        barcodeAnalyzer.isScanningEnabled = (uiState !is CameraScanUiState.Analyzing)
    }

    LaunchedEffect(torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    // Result screen when barcode food is recognized
    if (uiState is CameraScanUiState.Recognized) {
        val recognizedState = uiState as CameraScanUiState.Recognized
        RecognizedFoodScreen(
            result = recognizedState.result,
            bitmap = recognizedState.imageBitmap,
            mealType = mealType,
            badgeText = stringResource(R.string.barcode_verified_badge),
            badgeIcon = Icons.Default.QrCodeScanner,
            badgeColor = FoodInputBarcode,
            retakeButtonText = stringResource(R.string.barcode_btn_scan_again),
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
                    source = FoodSource.BARCODE,
                    onSuccess = onSaveSuccess
                )
            },
            onRetake = { viewModel.resetState() }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (!hasCameraPermission) {
            BarcodeCameraPermissionRationale(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                onManualEntry = { showManualInputDialog = true }
            )
        } else {
            val infiniteTransition = rememberInfiniteTransition(label = "barcodeLaserTransition")
            val laserFraction by infiniteTransition.animateFloat(
                initialValue = 0.05f,
                targetValue = 0.95f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1400, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "barcodeLaser"
            )

            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis.apply {
                                    setAnalyzer(cameraExecutor, barcodeAnalyzer)
                                }
                            )
                        } catch (exc: Exception) {
                            Timber.e(exc, "Barcode Camera binding failed")
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Dedicated Barcode Reticle Slot (Horizontal)
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(180.dp)
                    .align(Alignment.Center)
                    .border(2.5.dp, FoodInputBarcode, RoundedCornerShape(AppSpacing.m))
            ) {
                // Scanning Laser Line
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(2.5.dp)
                        .align(Alignment.TopCenter)
                        .offset(y = 180.dp * laserFraction)
                        .background(FoodInputBarcode)
                )

                // Corner accents
                Box(
                    modifier = Modifier
                        .size(AppSpacing.m)
                        .align(Alignment.TopStart)
                        .border(width = 3.dp, color = Color.White, shape = RoundedCornerShape(topStart = AppSpacing.m))
                )
                Box(
                    modifier = Modifier
                        .size(AppSpacing.m)
                        .align(Alignment.TopEnd)
                        .border(width = 3.dp, color = Color.White, shape = RoundedCornerShape(topEnd = AppSpacing.m))
                )
                Box(
                    modifier = Modifier
                        .size(AppSpacing.m)
                        .align(Alignment.BottomStart)
                        .border(width = 3.dp, color = Color.White, shape = RoundedCornerShape(bottomStart = AppSpacing.m))
                )
                Box(
                    modifier = Modifier
                        .size(AppSpacing.m)
                        .align(Alignment.BottomEnd)
                        .border(width = 3.dp, color = Color.White, shape = RoundedCornerShape(bottomEnd = AppSpacing.m))
                )
            }

            // Instruction Banner below Viewfinder
            Surface(
                color = Color.Black.copy(alpha = 0.7f),
                shape = RoundedCornerShape(AppSpacing.l),
                border = androidx.compose.foundation.BorderStroke(1.dp, FoodInputBarcode.copy(alpha = 0.4f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 125.dp)
                    .padding(horizontal = AppSpacing.xl)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.m, vertical = AppSpacing.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = FoodInputBarcode,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.s))
                    Text(
                        text = stringResource(R.string.scan_barcode_hint),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Bottom Action: Manual Barcode Input Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppSpacing.xxl, start = AppSpacing.xl, end = AppSpacing.xl)
            ) {
                OutlinedButton(
                    onClick = { showManualInputDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(AppSpacing.m),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, FoodInputBarcode)
                ) {
                    Icon(
                        imageVector = Icons.Default.Keyboard,
                        contentDescription = null,
                        tint = FoodInputBarcode,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.s))
                    Text(
                        text = stringResource(R.string.barcode_manual_input_btn),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Overlay Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.barcode_scan_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.barcode_scan_subtitle, mealType.lowercase().capitalized()),
                        style = MaterialTheme.typography.bodySmall,
                        color = FoodInputBarcode.copy(alpha = 0.9f)
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.scan_btn_back),
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(onClick = { viewModel.toggleTorch() }) {
                    Icon(
                        imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = stringResource(R.string.scan_flash_toggle_desc),
                        tint = if (torchEnabled) Color.Yellow else Color.White
                    )
                }
                IconButton(onClick = { showManualInputDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.barcode_manual_input_btn),
                        tint = FoodInputBarcode
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.7f)
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Analyzing Overlay State
        AnimatedVisibility(
            visible = uiState is CameraScanUiState.Analyzing,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = FoodInputBarcode,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xl))
                    Text(
                        text = stringResource(R.string.scan_barcode_searching),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = AppSpacing.xl)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.s))
                    Text(
                        text = stringResource(R.string.barcode_searching_subtitle),
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = AppSpacing.xxl)
                    )
                }
            }
        }

        // Error / Not Found Dialog
        if (uiState is CameraScanUiState.Error) {
            val errorMessage = (uiState as CameraScanUiState.Error).message
            NutriMindConfirmationDialog(
                title = stringResource(R.string.barcode_not_found_title),
                description = errorMessage,
                icon = Icons.Default.Error,
                iconTint = MaterialTheme.colorScheme.error,
                confirmButtonText = stringResource(R.string.scan_btn_try_again),
                dismissButtonText = stringResource(R.string.barcode_manual_entry_btn),
                onConfirm = { viewModel.resetState() },
                onDismissClick = {
                    viewModel.resetState()
                    showManualInputDialog = true
                },
                onDismissRequest = { viewModel.resetState() }
            )
        }

        // Manual Barcode Input Dialog
        if (showManualInputDialog) {
            NutriMindConfirmationDialog(
                title = stringResource(R.string.barcode_manual_dialog_title),
                icon = Icons.Default.QrCodeScanner,
                iconTint = FoodInputBarcode,
                confirmButtonText = stringResource(R.string.barcode_manual_search_btn),
                confirmButtonEnabled = manualBarcodeText.isNotBlank(),
                dismissButtonText = stringResource(R.string.dialog_cancel),
                onConfirm = {
                    val code = manualBarcodeText.trim()
                    if (code.isNotBlank()) {
                        showManualInputDialog = false
                        viewModel.onBarcodeScanned(code)
                    }
                },
                onDismissRequest = { showManualInputDialog = false },
                text = {
                    Column {
                        Text(
                            text = stringResource(R.string.barcode_manual_dialog_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(AppSpacing.sm))
                        OutlinedTextField(
                            value = manualBarcodeText,
                            onValueChange = { manualBarcodeText = it.filter { ch -> ch.isDigit() } },
                            placeholder = { Text(stringResource(R.string.barcode_manual_dialog_hint)) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(AppSpacing.sm)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun BarcodeCameraPermissionRationale(
    onRequestPermission: () -> Unit,
    onManualEntry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(FoodInputBarcode.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.QrCodeScanner,
                contentDescription = null,
                tint = FoodInputBarcode,
                modifier = Modifier.size(44.dp)
            )
        }
        Spacer(modifier = Modifier.height(AppSpacing.l))
        Text(
            text = stringResource(R.string.scan_perm_camera_title),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.s))
        Text(
            text = stringResource(R.string.scan_perm_camera_desc),
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(AppSpacing.xl))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(AppSpacing.sm),
            colors = ButtonDefaults.buttonColors(containerColor = FoodInputBarcode)
        ) {
            Text(stringResource(R.string.scan_btn_grant_camera), color = Color.Black, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(AppSpacing.sm))
        OutlinedButton(
            onClick = onManualEntry,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(AppSpacing.sm),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(stringResource(R.string.barcode_manual_input_btn))
        }
    }
}
