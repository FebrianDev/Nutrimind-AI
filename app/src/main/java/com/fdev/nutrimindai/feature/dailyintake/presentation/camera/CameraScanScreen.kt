package com.fdev.nutrimindai.feature.dailyintake.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.ai.FoodRecognitionResult
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    mealType: String,
    launchGalleryDirectly: Boolean = false,
    initialMode: String? = null,
    viewModel: CameraScanViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val torchEnabled by viewModel.torchEnabled.collectAsStateWithLifecycle()
    val scanMode by viewModel.scanMode.collectAsStateWithLifecycle()

    LaunchedEffect(initialMode) {
        if (initialMode == "BARCODE") {
            viewModel.setScanMode(ScanMode.BARCODE)
        }
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

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.analyzeGalleryUri(it) }
    }

    LaunchedEffect(launchGalleryDirectly) {
        if (launchGalleryDirectly) {
            galleryLauncher.launch("image/*")
        }
    }

    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    var camera by remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }
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

    LaunchedEffect(scanMode, uiState) {
        barcodeAnalyzer.isScanningEnabled = (scanMode == ScanMode.BARCODE && uiState !is CameraScanUiState.Analyzing)
    }

    LaunchedEffect(torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    // If food image has been recognized by AI or barcode scanned, navigate to full Recognized Food Result Screen
    if (uiState is CameraScanUiState.Recognized) {
        val recognizedState = uiState as CameraScanUiState.Recognized
        RecognizedFoodScreen(
            result = recognizedState.result,
            bitmap = recognizedState.imageBitmap,
            mealType = mealType,
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
                    source = if (scanMode == ScanMode.BARCODE) FoodSource.BARCODE else FoodSource.CAMERA,
                    onSuccess = onNavigateBack
                )
            },
            onRetake = { viewModel.resetState() }
        )
        return
    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.scan_food_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.scan_meal_subtitle, mealType.lowercase().capitalized()),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.8f)
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
                    IconButton(onClick = { galleryLauncher.launch("image/*") }) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.scan_btn_gallery),
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black.copy(alpha = 0.6f)
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (!hasCameraPermission) {
                CameraPermissionRationale(
                    onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    onOpenGallery = { galleryLauncher.launch("image/*") }
                )
            } else {
                val infiniteTransition = rememberInfiniteTransition(label = "laserTransition")
                val laserFraction by infiniteTransition.animateFloat(
                    initialValue = 0.05f,
                    targetValue = 0.95f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = 1500, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "laserFraction"
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
                                    imageCapture,
                                    imageAnalysis.apply {
                                        setAnalyzer(cameraExecutor, barcodeAnalyzer)
                                    }
                                )
                            } catch (exc: Exception) {
                                Timber.e(exc, "Use case binding failed")
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Top Mode Toggle Pill (Food AI vs Barcode)
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 16.dp)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(24.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Surface(
                        onClick = { viewModel.setScanMode(ScanMode.FOOD_AI) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (scanMode == ScanMode.FOOD_AI) MaterialTheme.colorScheme.primary else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.Restaurant, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(
                                text = stringResource(R.string.scan_mode_food_ai),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (scanMode == ScanMode.FOOD_AI) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                    Surface(
                        onClick = { viewModel.setScanMode(ScanMode.BARCODE) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (scanMode == ScanMode.BARCODE) Color(0xFF00BCD4) else Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Text(
                                text = stringResource(R.string.scan_mode_barcode),
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (scanMode == ScanMode.BARCODE) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }

                // Viewfinder Framing Overlay
                if (scanMode == ScanMode.BARCODE) {
                    Box(
                        modifier = Modifier
                            .width(300.dp)
                            .height(180.dp)
                            .align(Alignment.Center)
                            .border(2.dp, Color(0xFF00BCD4), RoundedCornerShape(16.dp))
                    ) {
                        // Animated Scanning Laser Line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .align(Alignment.TopCenter)
                                .offset(y = 180.dp * laserFraction)
                                .background(Color(0xFF00BCD4))
                        )

                        Text(
                            text = stringResource(R.string.scan_barcode_hint),
                            color = Color.White.copy(alpha = 0.9f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 8.dp)
                                .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .align(Alignment.Center)
                            .border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(24.dp))
                    ) {
                        Text(
                            text = stringResource(R.string.scan_frame_meal_hint),
                            color = Color.White.copy(alpha = 0.8f),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = 12.dp)
                                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                }

                // Camera Bottom Control Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp, start = 24.dp, end = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Flash toggle button
                    IconButton(
                        onClick = { viewModel.toggleTorch() },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = stringResource(R.string.scan_flash_toggle_desc),
                            tint = if (torchEnabled) Color.Yellow else Color.White
                        )
                    }

                    // Main Capture Button (Photo for AI, Manual capture fallback for Barcode)
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(4.dp)
                            .clickable {
                                capturePhoto(
                                    executor = cameraExecutor,
                                    imageCapture = imageCapture,
                                    onImageCaptured = { bitmap ->
                                        viewModel.analyzeImage(bitmap)
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(if (scanMode == ScanMode.BARCODE) Color(0xFF00BCD4) else MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = if (scanMode == ScanMode.BARCODE) Icons.Default.QrCodeScanner else Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.scan_capture_desc),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    // Gallery shortcut button
                    IconButton(
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = stringResource(R.string.scan_btn_gallery),
                            tint = Color.White
                        )
                    }
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
                        .background(Color.Black.copy(alpha = 0.85f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            color = if (scanMode == ScanMode.BARCODE) Color(0xFF00BCD4) else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = if (scanMode == ScanMode.BARCODE) {
                                stringResource(R.string.scan_barcode_searching)
                            } else {
                                stringResource(R.string.scan_analyzing_food)
                            },
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.scan_estimating_nutrition),
                            color = Color.White.copy(alpha = 0.7f),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Error Dialog
            if (uiState is CameraScanUiState.Error) {
                val errorMessage = (uiState as CameraScanUiState.Error).message
                AlertDialog(
                    onDismissRequest = { viewModel.resetState() },
                    icon = { Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    title = { Text(stringResource(R.string.scan_recognition_failed_title)) },
                    text = { Text(errorMessage) },
                    confirmButton = {
                        Button(onClick = { viewModel.resetState() }) {
                            Text(stringResource(R.string.scan_btn_try_again))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { galleryLauncher.launch("image/*") }) {
                            Text(stringResource(R.string.scan_btn_use_gallery))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CameraPermissionRationale(
    onRequestPermission: () -> Unit,
    onOpenGallery: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CameraAlt,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(72.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.scan_perm_camera_title),
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.scan_perm_camera_desc),
            color = Color.White.copy(alpha = 0.8f),
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(stringResource(R.string.scan_btn_grant_camera))
        }
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedButton(
            onClick = onOpenGallery,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
        ) {
            Text(stringResource(R.string.scan_btn_choose_gallery_instead))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecognizedFoodScreen(
    result: FoodRecognitionResult,
    bitmap: Bitmap?,
    mealType: String,
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.scan_analysis_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onRetake) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.scan_btn_scan_again))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetake,
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(stringResource(R.string.scan_btn_scan_again))
                    }

                    Button(
                        onClick = {
                            onSave(foodName, servingSize, servingUnit, calories, protein, carbs, fat, fiber)
                        },
                        modifier = Modifier
                            .weight(1.5f)
                            .height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        enabled = foodName.isNotBlank() && calories.isNotBlank()
                    ) {
                        Text(stringResource(R.string.scan_btn_add_to_meal, mealType.lowercase().capitalized()), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Hero Photo with AI Vision Badge
            bitmap?.let {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                ) {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = stringResource(R.string.scan_scanned_food_desc),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    // Floating AI Vision badge
                    Surface(
                        color = Color.Black.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFF64B5F6),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.scan_ai_vision_verified),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Food Name Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.scan_details_card_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = foodName,
                        onValueChange = { foodName = it },
                        label = { Text(stringResource(R.string.scan_detected_food_name)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
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
                            label = { Text(stringResource(R.string.scan_unit_label)) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nutrition Breakdown Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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

                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { calories = it },
                        label = { Text(stringResource(R.string.scan_total_calories_label)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MacroField(label = stringResource(R.string.food_label_protein), value = protein, onValueChange = { protein = it }, modifier = Modifier.weight(1f))
                        MacroField(label = stringResource(R.string.food_label_carbs), value = carbs, onValueChange = { carbs = it }, modifier = Modifier.weight(1f))
                        MacroField(label = stringResource(R.string.food_label_fat), value = fat, onValueChange = { fat = it }, modifier = Modifier.weight(1f))
                        MacroField(label = stringResource(R.string.food_label_fiber), value = fiber, onValueChange = { fiber = it }, modifier = Modifier.weight(1f))
                    }
                }
            }

            if (result.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.scan_ai_nutrition_insights),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = result.description,
                            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
        shape = RoundedCornerShape(10.dp)
    )
}

private fun capturePhoto(
    executor: Executor,
    imageCapture: ImageCapture,
    onImageCaptured: (Bitmap) -> Unit
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val processedBitmap = try {
                    val bitmap = imageProxy.toBitmap()
                    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                    rotateAndScaleBitmap(bitmap, rotationDegrees, 1024)
                } finally {
                    imageProxy.close()
                }
                onImageCaptured(processedBitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                Timber.e(exception, "Error capturing camera frame")
            }
        }
    )
}

private fun rotateAndScaleBitmap(bitmap: Bitmap, degrees: Int, maxDimension: Int = 1024): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    val rotatedWidth = if (degrees % 180 != 0) height else width
    val rotatedHeight = if (degrees % 180 != 0) width else height

    val maxSide = maxOf(rotatedWidth, rotatedHeight)
    val scale = if (maxSide > maxDimension) maxDimension.toFloat() / maxSide else 1f

    if (degrees == 0 && scale == 1f) return bitmap

    val matrix = Matrix().apply {
        if (scale < 1f) {
            postScale(scale, scale)
        }
        if (degrees != 0) {
            postRotate(degrees.toFloat())
        }
    }
    return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
}

private fun String.capitalized(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.US) else it.toString() }
