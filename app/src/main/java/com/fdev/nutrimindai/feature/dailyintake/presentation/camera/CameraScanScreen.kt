package com.fdev.nutrimindai.feature.dailyintake.presentation.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
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
    viewModel: CameraScanViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val torchEnabled by viewModel.torchEnabled.collectAsStateWithLifecycle()

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
        uri?.let {
            try {
                val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, it)
                    ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        val maxDim = 1024
                        val size = info.size
                        if (size.width > maxDim || size.height > maxDim) {
                            val ratio = size.width.toFloat() / size.height.toFloat()
                            if (size.width > size.height) {
                                decoder.setTargetSize(maxDim, (maxDim / ratio).toInt().coerceAtLeast(1))
                            } else {
                                decoder.setTargetSize((maxDim * ratio).toInt().coerceAtLeast(1), maxDim)
                            }
                        }
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val raw = MediaStore.Images.Media.getBitmap(context.contentResolver, it)
                    scaleBitmapDown(raw, 1024)
                }
                viewModel.analyzeImage(bitmap)
            } catch (e: Exception) {
                Timber.e(e, "Error loading image from gallery")
            }
        }
    }

    LaunchedEffect(launchGalleryDirectly) {
        if (launchGalleryDirectly) {
            galleryLauncher.launch("image/*")
        }
    }

    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val imageCapture = remember { ImageCapture.Builder().build() }

    LaunchedEffect(torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }



    // If food image has been recognized by AI, navigate to full Recognized Food Result Screen
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
                    source = FoodSource.CAMERA,
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
                                    imageCapture
                                )
                            } catch (exc: Exception) {
                                Timber.e(exc, "Use case binding failed")
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )


                // Viewfinder framing overlay
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

                    // Main Capture Button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .padding(4.dp)
                            .clickable {
                                capturePhoto(
                                    context = context,
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
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = stringResource(R.string.scan_capture_desc),
                                tint = Color.White,
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }

                    // Flip camera button
                    IconButton(
                        onClick = {
                            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                                CameraSelector.DEFAULT_FRONT_CAMERA
                            } else {
                                CameraSelector.DEFAULT_BACK_CAMERA
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cameraswitch,
                            contentDescription = stringResource(R.string.scan_flip_camera_desc),
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
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = stringResource(R.string.scan_analyzing_food),
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
    context: Context,
    imageCapture: ImageCapture,
    onImageCaptured: (Bitmap) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(context)
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(imageProxy: ImageProxy) {
                val bitmap = imageProxy.toBitmap()
                val rotationDegrees = imageProxy.imageInfo.rotationDegrees
                val rotatedBitmap = rotateBitmap(bitmap, rotationDegrees)
                imageProxy.close()
                val scaledBitmap = scaleBitmapDown(rotatedBitmap, 1024)
                onImageCaptured(scaledBitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                Timber.e(exception, "Error capturing camera frame")
            }
        }
    )
}

private fun scaleBitmapDown(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
    val width = bitmap.width
    val height = bitmap.height
    if (width <= maxDimension && height <= maxDimension) return bitmap

    val ratio = width.toFloat() / height.toFloat()
    val targetWidth: Int
    val targetHeight: Int
    if (width > height) {
        targetWidth = maxDimension
        targetHeight = (maxDimension / ratio).toInt().coerceAtLeast(1)
    } else {
        targetHeight = maxDimension
        targetWidth = (maxDimension * ratio).toInt().coerceAtLeast(1)
    }
    return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
}

private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
    if (degrees == 0) return bitmap
    val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
    return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private fun String.capitalized(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(java.util.Locale.getDefault()) else it.toString() }
