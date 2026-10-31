package com.fdev.nutrimindai.feature.coach.presentation

import android.net.Uri
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.core.content.FileProvider
import java.io.File
import com.fdev.nutrimindai.core.designsystem.component.NutriMindConfirmationDialog
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.R
import com.fdev.nutrimindai.core.ai.AiProvider
import com.fdev.nutrimindai.feature.coach.domain.model.ContextualPrompt
import com.fdev.nutrimindai.feature.coach.presentation.components.CoachChatBubble
import com.fdev.nutrimindai.feature.coach.presentation.components.CoachQuickChips
import com.fdev.nutrimindai.feature.coach.presentation.components.CoachTypingIndicator
import timber.log.Timber
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoachScreen(
    initialPrompt: String? = null,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: CoachViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showClearDialog by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.startVoiceRecording(context.cacheDir)
        }
    }

    fun handleMicClick() {
        if (uiState.isRecordingVoice) {
            viewModel.stopVoiceRecordingAndTranscribe()
            return
        }
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            viewModel.startVoiceRecording(context.cacheDir)
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    var showAttachmentSheet by remember { mutableStateOf(false) }
    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onImageSelected(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            tempCameraUri?.let { viewModel.onImageSelected(it) }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = File(context.cacheDir, "coach_cam_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        }
    }

    fun launchCamera() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            val file = File(context.cacheDir, "coach_cam_${System.currentTimeMillis()}.jpg")
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            tempCameraUri = uri
            cameraLauncher.launch(uri)
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Handle initial prompt from deep navigation if provided (guarded against screen rotations)
    var hasHandledInitialPrompt by rememberSaveable(initialPrompt) { mutableStateOf(false) }
    LaunchedEffect(initialPrompt) {
        if (!initialPrompt.isNullOrBlank() && !hasHandledInitialPrompt) {
            hasHandledInitialPrompt = true
            viewModel.sendMessage(initialPrompt)
        }
    }

    // Auto-scroll to bottom on new message or when generating starts
    LaunchedEffect(uiState.messages.size, uiState.isGenerating) {
        val totalCount = uiState.messages.size + if (uiState.isGenerating) 1 else 0
        if (totalCount > 0) {
            listState.animateScrollToItem(totalCount - 1)
        }
    }

    // Cancel and clean up microphone hardware when screen is navigated away or disposed
    DisposableEffect(Unit) {
        onDispose {
            viewModel.cancelVoiceRecording()
        }
    }

    if (showClearDialog) {
        NutriMindConfirmationDialog(
            title = stringResource(R.string.coach_dialog_clear_title),
            description = stringResource(R.string.coach_dialog_clear_desc),
            confirmButtonText = stringResource(R.string.coach_btn_clear),
            dismissButtonText = stringResource(R.string.dialog_cancel),
            isDanger = true,
            onDismissRequest = { showClearDialog = false },
            onConfirm = {
                showClearDialog = false
                viewModel.clearHistory()
            }
        )
    }


    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
    ) {
        TopAppBar(
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary,
                                        MaterialTheme.colorScheme.tertiary
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.coach_title),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        ) {
                            Text(
                                text = stringResource(R.string.coach_engine_format, uiState.activeProvider.displayName),
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            },
            navigationIcon = {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.btn_back)
                        )
                    }
                }
            },
            actions = {
                if (uiState.messages.isNotEmpty()) {
                    IconButton(onClick = { showClearDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteOutline,
                            contentDescription = stringResource(R.string.coach_cd_clear_history),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        // Chat messages list
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (uiState.messages.isEmpty() && !uiState.isGenerating) {
                CoachWelcomeHero(
                    activeProvider = uiState.activeProvider,
                    quickPrompts = uiState.contextualPrompts,
                    onPromptClick = { prompt -> viewModel.sendMessage(prompt) }
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 6.dp)
                ) {
                    items(uiState.messages, key = { it.id }) { message ->
                        CoachChatBubble(
                            message = message,
                            providerName = uiState.activeProvider.displayName
                        )
                    }
                    if (uiState.isGenerating) {
                        item(key = "typing_indicator") {
                            CoachTypingIndicator()
                        }
                    }
                }
            }
        }

        // Error banner if any
        uiState.errorMessage?.let { error ->
            Surface(
                color = MaterialTheme.colorScheme.errorContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { viewModel.dismissError() }) {
                        Text(text = stringResource(R.string.coach_btn_dismiss), color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }
        }

        // Quick suggestion chips above input if input text is empty AND there are messages
        if (uiState.messages.isNotEmpty() && uiState.inputText.isBlank() && !uiState.isGenerating) {
            CoachQuickChips(
                prompts = uiState.contextualPrompts,
                onPromptSelected = { prompt -> viewModel.sendMessage(prompt) }
            )
        }

        // Bottom Input Bar with Medical Disclaimer
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Transcription status banner
                AnimatedVisibility(
                    visible = uiState.isTranscribingVoice,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.coach_voice_transcribing),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (uiState.isRecordingVoice) {
                    // In-App Meta MMS / Muse Voice Recording Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel button
                        IconButton(
                            onClick = { viewModel.cancelVoiceRecording() },
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = stringResource(R.string.coach_voice_cancel),
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Waveform and Duration Pill
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val alphaPulse by infiniteTransition.animateFloat(
                                initialValue = 0.3f,
                                targetValue = 1f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(500),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "alphaPulse"
                            )

                            // Pulsing red recording dot
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = alphaPulse))
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            val minutes = uiState.voiceDurationSeconds / 60
                            val seconds = uiState.voiceDurationSeconds % 60
                            Text(
                                text = String.format(Locale.US, "%02d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Dynamic sound wave bars based on amplitude
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val amp = uiState.voiceAmplitude.coerceIn(0f, 1f)
                                val barFactors = listOf(0.4f, 0.7f, 1.0f, 0.85f, 0.6f, 0.95f, 0.5f, 0.75f)
                                barFactors.forEach { factor ->
                                    val barHeight = (6 + (amp * 26 * factor)).dp
                                    Box(
                                        modifier = Modifier
                                            .width(3.dp)
                                            .height(barHeight)
                                            .clip(RoundedCornerShape(2.dp))
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Done / Stop button
                        IconButton(
                            onClick = { viewModel.stopVoiceRecordingAndTranscribe() },
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = stringResource(R.string.coach_voice_done),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                } else {
                    // Google Gemini & ChatGPT Style Unified Input Capsule
                    val hasInput = uiState.inputText.isNotBlank() || uiState.selectedImageBitmap != null
                    val isActionEnabled = !uiState.isGenerating && !uiState.isTranscribingVoice

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(32.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
                        )
                    ) {
                        // Custom adaptive Layout that preserves a single persistent BasicTextField instance,
                        // preventing focus loss and keyboard dismissal when transitioning between 1 and 2+ lines.
                        Layout(
                            content = {
                                // 1. Attached Photo Preview (if present)
                                if (uiState.selectedImageBitmap != null) {
                                    Box(
                                        modifier = Modifier
                                            .layoutId("image")
                                            .padding(bottom = 6.dp)
                                    ) {
                                        Image(
                                            bitmap = uiState.selectedImageBitmap!!.asImageBitmap(),
                                            contentDescription = stringResource(R.string.coach_photo_attached_title),
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(68.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .align(Alignment.TopEnd)
                                                .offset(x = 6.dp, y = (-6).dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.surface)
                                                .clickable { viewModel.clearSelectedImage() },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = stringResource(R.string.coach_cd_remove_image),
                                                tint = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }

                                // 2. Single Persistent BasicTextField Box
                                Box(
                                    modifier = Modifier
                                        .layoutId("text")
                                        .padding(horizontal = 6.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (uiState.inputText.isEmpty()) {
                                        Text(
                                            text = if (uiState.selectedImageBitmap != null) "Ask about this photo..." else stringResource(R.string.coach_input_placeholder),
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                            maxLines = 1
                                        )
                                    }
                                    BasicTextField(
                                        value = uiState.inputText,
                                        onValueChange = { viewModel.onInputTextChanged(it) },
                                        maxLines = 6,
                                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                                            color = MaterialTheme.colorScheme.onSurface,
                                            lineHeight = 22.sp
                                        ),
                                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                // 3. Plus Attachment Button
                                IconButton(
                                    onClick = { showAttachmentSheet = true },
                                    enabled = isActionEnabled,
                                    modifier = Modifier
                                        .layoutId("plus")
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Add,
                                        contentDescription = stringResource(R.string.coach_cd_attach_image),
                                        tint = if (uiState.selectedImageBitmap != null) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(26.dp)
                                    )
                                }

                                // 4. Mic Button
                                IconButton(
                                    onClick = { handleMicClick() },
                                    enabled = isActionEnabled,
                                    modifier = Modifier
                                        .layoutId("mic")
                                        .size(40.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = stringResource(R.string.coach_voice_input_desc),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // 5. Dynamic Send / Stop Action Button
                                FilledIconButton(
                                    onClick = {
                                        if (uiState.isGenerating) {
                                            viewModel.stopGenerating()
                                        } else {
                                            viewModel.sendMessage()
                                        }
                                    },
                                    enabled = (uiState.isGenerating || hasInput) && !uiState.isTranscribingVoice,
                                    shape = CircleShape,
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = if (uiState.isGenerating) MaterialTheme.colorScheme.primary
                                        else MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = if (uiState.isGenerating) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onPrimaryContainer,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                                    ),
                                    modifier = Modifier
                                        .layoutId("send")
                                        .size(42.dp)
                                ) {
                                    if (uiState.isGenerating) {
                                        Icon(
                                            imageVector = Icons.Rounded.Stop,
                                            contentDescription = stringResource(R.string.coach_cd_stop),
                                            modifier = Modifier.size(24.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Rounded.ArrowUpward,
                                            contentDescription = stringResource(R.string.coach_cd_send),
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { measurables, constraints ->
                            val horizontalPad = 10.dp.roundToPx()
                            val verticalPad = 6.dp.roundToPx()
                            val gap = 4.dp.roundToPx()

                            val plusMeasurable = measurables.first { it.layoutId == "plus" }
                            val micMeasurable = measurables.first { it.layoutId == "mic" }
                            val sendMeasurable = measurables.first { it.layoutId == "send" }
                            val textMeasurable = measurables.first { it.layoutId == "text" }
                            val imageMeasurable = measurables.firstOrNull { it.layoutId == "image" }

                            val plusPlaceable = plusMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                            val micPlaceable = micMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                            val sendPlaceable = sendMeasurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                            val imagePlaceable = imageMeasurable?.measure(constraints.copy(minWidth = 0, minHeight = 0))

                            val singleLineAvailableWidth = (constraints.maxWidth - (horizontalPad * 2) - plusPlaceable.width - micPlaceable.width - sendPlaceable.width - (gap * 3)).coerceAtLeast(0)

                            // Check intrinsic height without measuring twice
                            val singleLineMaxHeight = 32.dp.roundToPx()
                            val intrinsicHeight = textMeasurable.maxIntrinsicHeight(singleLineAvailableWidth)
                            val isMulti = imagePlaceable != null || uiState.inputText.contains('\n') || intrinsicHeight > singleLineMaxHeight

                            val textPlaceable = if (isMulti) {
                                val multiLineAvailableWidth = (constraints.maxWidth - (horizontalPad * 2)).coerceAtLeast(0)
                                textMeasurable.measure(
                                    constraints.copy(minWidth = 0, maxWidth = multiLineAvailableWidth, minHeight = 0)
                                )
                            } else {
                                textMeasurable.measure(
                                    constraints.copy(minWidth = 0, maxWidth = singleLineAvailableWidth, minHeight = 0)
                                )
                            }

                            if (isMulti) {
                                val actionRowHeight = maxOf(plusPlaceable.height, micPlaceable.height, sendPlaceable.height)
                                val topPad = 12.dp.roundToPx()
                                val bottomPad = 10.dp.roundToPx()
                                val spacing = 8.dp.roundToPx()
                                val imageH = if (imagePlaceable != null) imagePlaceable.height + spacing else 0

                                val totalHeight = topPad + imageH + textPlaceable.height + spacing + actionRowHeight + bottomPad

                                layout(constraints.maxWidth, totalHeight) {
                                    var currentY = topPad
                                    if (imagePlaceable != null) {
                                        imagePlaceable.placeRelative(horizontalPad, currentY)
                                        currentY += imagePlaceable.height + spacing
                                    }

                                    textPlaceable.placeRelative(horizontalPad, currentY)

                                    val bottomRowY = totalHeight - bottomPad - actionRowHeight
                                    plusPlaceable.placeRelative(
                                        horizontalPad,
                                        bottomRowY + (actionRowHeight - plusPlaceable.height) / 2
                                    )
                                    val sendX = constraints.maxWidth - horizontalPad - sendPlaceable.width
                                    val micX = sendX - gap - micPlaceable.width
                                    micPlaceable.placeRelative(
                                        micX,
                                        bottomRowY + (actionRowHeight - micPlaceable.height) / 2
                                    )
                                    sendPlaceable.placeRelative(
                                        sendX,
                                        bottomRowY + (actionRowHeight - sendPlaceable.height) / 2
                                    )
                                }
                            } else {
                                val contentHeight = maxOf(plusPlaceable.height, micPlaceable.height, sendPlaceable.height, textPlaceable.height)
                                val totalHeight = contentHeight + (verticalPad * 2)

                                layout(constraints.maxWidth, totalHeight) {
                                    plusPlaceable.placeRelative(
                                        horizontalPad,
                                        (totalHeight - plusPlaceable.height) / 2
                                    )
                                    textPlaceable.placeRelative(
                                        horizontalPad + plusPlaceable.width + gap,
                                        (totalHeight - textPlaceable.height) / 2
                                    )
                                    val sendX = constraints.maxWidth - horizontalPad - sendPlaceable.width
                                    val micX = sendX - gap - micPlaceable.width
                                    micPlaceable.placeRelative(
                                        micX,
                                        (totalHeight - micPlaceable.height) / 2
                                    )
                                    sendPlaceable.placeRelative(
                                        sendX,
                                        (totalHeight - sendPlaceable.height) / 2
                                    )
                                }
                            }
                        }
                    }
                }

                Text(
                    text = stringResource(R.string.medical_disclaimer_short),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(bottom = 6.dp, top = 0.dp)
                )
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = stringResource(R.string.coach_attach_photo_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.coach_attach_photo_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAttachmentSheet = false
                            launchCamera()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.food_take_photo_title),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.food_take_photo_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showAttachmentSheet = false
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.food_choose_gallery_title),
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.food_choose_gallery_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun CoachWelcomeHero(
    activeProvider: AiProvider,
    quickPrompts: List<ContextualPrompt>,
    onPromptClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App brand gradient circle for Coach AI
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.coach_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        // AI Provider badge styled consistently with app's primary theme
        Surface(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text(
                text = stringResource(R.string.coach_model_format, activeProvider.displayName, activeProvider.shortModelName),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.coach_welcome_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        if (quickPrompts.isNotEmpty()) {
            Text(
                text = stringResource(R.string.coach_quick_questions_header),
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(10.dp))

            quickPrompts.take(3).forEach { prompt ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.background,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    onClick = { onPromptClick(prompt.promptText) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = prompt.emoji, fontSize = 16.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = prompt.title,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = prompt.promptText,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
