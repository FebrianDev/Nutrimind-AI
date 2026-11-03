package com.fdev.nutrimindai.feature.dailyintake.presentation.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.fdev.nutrimindai.feature.dailyintake.presentation.common.RecognizedFoodScreen
import com.fdev.nutrimindai.feature.dailyintake.presentation.common.capitalized
import timber.log.Timber
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraScanScreen(
    mealType: String,
    launchGalleryDirectly: Boolean = false,
    initialMode: String? = null,
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

    LaunchedEffect(Unit) {
        viewModel.setScanMode(ScanMode.FOOD_AI)
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
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(cameraExecutor) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(torchEnabled) {
        camera?.cameraControl?.enableTorch(torchEnabled)
    }

    // If food image has been recognized by AI, navigate to Recognized Food Screen
    if (uiState is CameraScanUiState.Recognized) {
        val recognizedState = uiState as CameraScanUiState.Recognized
        RecognizedFoodScreen(
            result = recognizedState.result,
            bitmap = recognizedState.imageBitmap,
            mealType = mealType,
            badgeText = stringResource(R.string.scan_ai_vision_verified),
            badgeIcon = Icons.Default.AutoAwesome,
            badgeColor = FoodInputCamera,
            retakeButtonText = stringResource(R.string.camera_btn_retake),
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
            CameraPermissionRationale(
                onRequestPermission = { permissionLauncher.launch(Manifest.permission.CAMERA) }
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
                            Timber.e(exc, "Camera binding failed")
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Food Plate Framing Reticle Overlay
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .align(Alignment.Center)
                    .border(2.dp, FoodInputCamera.copy(alpha = 0.85f), RoundedCornerShape(AppSpacing.xxl))
            ) {
                // Reticle Corner guides
                Box(
                    modifier = Modifier
                        .size(AppSpacing.xl)
                        .align(Alignment.TopStart)
                        .border(width = 3.5.dp, color = Color.White, shape = RoundedCornerShape(topStart = AppSpacing.xxl))
                )
                Box(
                    modifier = Modifier
                        .size(AppSpacing.xl)
                        .align(Alignment.TopEnd)
                        .border(width = 3.5.dp, color = Color.White, shape = RoundedCornerShape(topEnd = AppSpacing.xxl))
                )
                Box(
                    modifier = Modifier
                        .size(AppSpacing.xl)
                        .align(Alignment.BottomStart)
                        .border(width = 3.5.dp, color = Color.White, shape = RoundedCornerShape(bottomStart = AppSpacing.xxl))
                )
                Box(
                    modifier = Modifier
                        .size(AppSpacing.xl)
                        .align(Alignment.BottomEnd)
                        .border(width = 3.5.dp, color = Color.White, shape = RoundedCornerShape(bottomEnd = AppSpacing.xxl))
                )

                // Top Frame Instruction
                Text(
                    text = stringResource(R.string.camera_frame_hint),
                    color = Color.White.copy(alpha = 0.9f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = AppSpacing.sm)
                        .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(AppSpacing.sm))
                        .padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs)
                )
            }

            // AI Detection Hint Banner below plate guide
            Surface(
                color = Color.Black.copy(alpha = 0.65f),
                shape = RoundedCornerShape(AppSpacing.m),
                border = androidx.compose.foundation.BorderStroke(1.dp, FoodInputCamera.copy(alpha = 0.3f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .offset(y = 175.dp)
                    .padding(horizontal = AppSpacing.xl)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.s),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = FoodInputCamera,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(AppSpacing.s))
                    Text(
                        text = stringResource(R.string.camera_ai_hint),
                        color = Color.White,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Camera Bottom Control Bar with Shutter Button
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = AppSpacing.xxl),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .padding(AppSpacing.xs)
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
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(FoodInputCamera),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = stringResource(R.string.scan_capture_desc),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }

        // Overlay Top App Bar
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringResource(R.string.camera_photo_title),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Color.White
                    )
                    Text(
                        text = stringResource(R.string.camera_photo_subtitle, mealType.lowercase().capitalized()),
                        style = MaterialTheme.typography.bodySmall,
                        color = FoodInputCamera.copy(alpha = 0.9f)
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
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.6f)
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
                        color = FoodInputCamera,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.xl))
                    Text(
                        text = stringResource(R.string.camera_analyzing_title),
                        color = Color.White,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = AppSpacing.xl)
                    )
                    Spacer(modifier = Modifier.height(AppSpacing.s))
                    Text(
                        text = stringResource(R.string.camera_analyzing_subtitle),
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
                confirmButtonText = stringResource(R.string.scan_btn_try_again),
                dismissButtonText = stringResource(R.string.dialog_cancel),
                onConfirm = { viewModel.resetState() },
                onDismissRequest = { viewModel.resetState() }
            )
        }
    }
}

@Composable
fun CameraPermissionRationale(
    onRequestPermission: () -> Unit
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
                .background(FoodInputCamera.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = FoodInputCamera,
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
            colors = ButtonDefaults.buttonColors(containerColor = FoodInputCamera)
        ) {
            Text(stringResource(R.string.scan_btn_grant_camera))
        }
    }
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
