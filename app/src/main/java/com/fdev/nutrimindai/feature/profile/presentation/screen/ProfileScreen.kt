package com.fdev.nutrimindai.feature.profile.presentation.screen

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.res.stringResource
import com.fdev.nutrimindai.core.datastore.AppThemeMode
import com.fdev.nutrimindai.R
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fdev.nutrimindai.core.designsystem.component.UserAvatar
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.core.model.Goal
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import timber.log.Timber
import java.util.Locale

import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onNavigateToGoalSettings: () -> Unit,
    onNavigateToOnboarding: () -> Unit = {},
    onNavigateToReminderSettings: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val syncState by viewModel.syncState.collectAsStateWithLifecycle()
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val linkAccountState by viewModel.linkAccountState.collectAsStateWithLifecycle()
    val isUploadingPhoto by viewModel.isUploadingPhoto.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current
    var showPhotoSourceDialog by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempPhotoUri != null) {
            viewModel.uploadProfileImage(context, tempPhotoUri!!)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.uploadProfileImage(context, uri)
        }
    }

    fun launchCamera() {
        try {
            val photoFile = java.io.File.createTempFile("profile_pic_", ".jpg", context.cacheDir)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            tempPhotoUri = uri
            cameraLauncher.launch(uri)
        } catch (e: Exception) {
            Timber.e(e, "Failed to launch camera")
        }
    }

    fun launchGallery() {
        galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val snackbarHostState = remember { SnackbarHostState() }
    var showResetDialog by remember { mutableStateOf(false) }
    var showResetProgressDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showLinkAccountDialog by remember { mutableStateOf(false) }

    val emailFallback = stringResource(R.string.profile_email_fallback)
    val accountLinkedSuccessMsg = stringResource(
        R.string.profile_account_linked_success,
        accountInfo.email ?: emailFallback
    )

    LaunchedEffect(linkAccountState.isSuccess) {
        if (linkAccountState.isSuccess) {
            showLinkAccountDialog = false
            snackbarHostState.showSnackbar(
                message = accountLinkedSuccessMsg,
                duration = SnackbarDuration.Short
            )
            viewModel.resetLinkAccountState()
        }
    }

    LaunchedEffect(syncState.lastSyncMessage) {
        syncState.lastSyncMessage?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                duration = SnackbarDuration.Short
            )
            viewModel.clearSyncMessage()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val state = uiState) {
            is ProfileUiState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            is ProfileUiState.Error -> {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = state.message, color = MaterialTheme.colorScheme.error)
                }
            }
            is ProfileUiState.Success -> {
                val profile = state.profile
                if (profile == null) {
                    EmptyProfileContent(onNavigateToGoalSettings = onNavigateToGoalSettings)
                } else {
                    ProfileMainContent(
                        profile = profile,
                        syncState = syncState,
                        accountInfo = accountInfo,
                        themeMode = themeMode,
                        isUploadingPhoto = isUploadingPhoto,
                        onEditPhotoClick = { showPhotoSourceDialog = true },
                        onSyncNowClick = { viewModel.syncDataNow() },
                        onThemeClick = { showThemeDialog = true },
                        onEditProfileClick = { showEditProfileDialog = true },
                        onHealthGoalClick = onNavigateToGoalSettings,
                        onReminderSettingsClick = onNavigateToReminderSettings,
                        onResetDataClick = { showResetProgressDialog = true },
                        onLinkAccountClick = { showLinkAccountDialog = true },
                        onAboutClick = { showAboutDialog = true },
                        onLogOutClick = { showResetDialog = true }
                    )

                    if (showEditProfileDialog) {
                        EditProfileDialog(
                            profile = profile,
                            onDismiss = { showEditProfileDialog = false },
                            onSave = { name, age, height, weight ->
                                viewModel.updatePersonalInfo(name, age, height, weight)
                                showEditProfileDialog = false
                            }
                        )
                    }

                    if (showPhotoSourceDialog) {
                        PhotoSourceDialog(
                            onDismiss = { showPhotoSourceDialog = false },
                            onCameraSelect = { launchCamera() },
                            onGallerySelect = { launchGallery() }
                        )
                    }



                    if (showLinkAccountDialog) {
                        LinkAccountDialog(
                            linkAccountState = linkAccountState,
                            onDismiss = {
                                showLinkAccountDialog = false
                                viewModel.resetLinkAccountState()
                            },
                            onConfirmEmail = { email, password ->
                                viewModel.linkAccount(email, password, profile.name)
                            }
                        )
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showResetProgressDialog) {
        AlertDialog(
            onDismissRequest = { showResetProgressDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            icon = {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(R.string.profile_reset_data_dialog_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.profile_reset_data_dialog_msg),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetProgressDialog = false
                        viewModel.resetProgressData()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(
                        text = stringResource(R.string.profile_reset_data_confirm_btn),
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetProgressDialog = false }) {
                    Text(
                        text = stringResource(R.string.dialog_cancel),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(
                    stringResource(R.string.btn_log_out),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    stringResource(R.string.profile_logout_confirm_msg),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        viewModel.logout(onSuccess = onNavigateToOnboarding)
                    }
                ) {
                    Text(stringResource(R.string.btn_log_out), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
    }

    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentMode = themeMode,
            onDismiss = { showThemeDialog = false },
            onSelectMode = { selectedMode ->
                viewModel.setThemeMode(selectedMode)
                showThemeDialog = false
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Text(stringResource(R.string.label_about_app), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.profile_about_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        stringResource(R.string.profile_about_version),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        stringResource(R.string.profile_about_storage_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text(stringResource(R.string.btn_close), color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }
}

@Composable
private fun ProfileMainContent(
    profile: ProfileEntity,
    syncState: SyncUiState,
    accountInfo: AccountInfo,
    themeMode: AppThemeMode,
    isUploadingPhoto: Boolean,
    onEditPhotoClick: () -> Unit,
    onSyncNowClick: () -> Unit,
    onThemeClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onHealthGoalClick: () -> Unit,
    onReminderSettingsClick: () -> Unit,
    onResetDataClick: () -> Unit,
    onLinkAccountClick: () -> Unit,
    onAboutClick: () -> Unit,
    onLogOutClick: () -> Unit
) {

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // --- Header Title ---
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.profile_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.profile_subtitle),
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        // --- User Card ---
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Avatar Circle
                    UserAvatar(
                        photoUrl = profile.photoUrl,
                        userName = profile.name,
                        size = 64.dp,
                        showEditBadge = true,
                        isUploading = isUploadingPhoto,
                        onClick = onEditPhotoClick
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Real User Physical Stats
                        Text(
                            text = stringResource(R.string.profile_user_stats, profile.age, profile.height, profile.currentWeight.toString()),
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.TrackChanges,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = getGoalTitle(profile.goal),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Edit Profile Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        modifier = Modifier.clickable { onEditProfileClick() },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.profile_edit_personal_info),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                    }
                }
            }
        }


        // --- Guest / Anonymous Account Linking Banner or Verified Status ---
        if (accountInfo.isLoggedIn) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(
                            R.string.profile_account_label,
                            accountInfo.email ?: stringResource(R.string.profile_secured_account)
                        ),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    )
                }
            }
        }

        // --- QUICK SETTINGS Section ---
        Text(
            text = stringResource(R.string.profile_section_settings_data),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column {
                if (accountInfo.isAnonymous) {
                    QuickMenuItem(
                        icon = Icons.Default.Link,
                        iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        iconTint = MaterialTheme.colorScheme.primary,
                        title = stringResource(R.string.profile_link_account),
                        subtitle = stringResource(R.string.profile_link_account_sub),
                        onClick = onLinkAccountClick
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
                }

                QuickMenuItem(
                    icon = Icons.Default.Sync,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.profile_sync_data_title),
                    subtitle = when {
                        syncState.isSyncing -> stringResource(R.string.profile_sync_status_syncing)
                        syncState.unsyncedCount > 0 -> stringResource(R.string.profile_sync_status_pending, syncState.unsyncedCount)
                        else -> stringResource(R.string.profile_sync_status_synced)
                    },
                    onClick = onSyncNowClick,
                    isLoading = syncState.isSyncing
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                QuickMenuItem(
                    icon = Icons.Default.TrackChanges,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.profile_health_goal_title),
                    subtitle = stringResource(R.string.profile_health_goal_sub),
                    onClick = onHealthGoalClick
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                QuickMenuItem(
                    icon = Icons.Default.Notifications,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.reminder_title),
                    subtitle = stringResource(R.string.reminder_subtitle),
                    onClick = onReminderSettingsClick
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                QuickMenuItem(
                    icon = Icons.Default.Palette,
                    iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    iconTint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.profile_theme_title),
                    subtitle = when (themeMode) {
                        AppThemeMode.SYSTEM -> stringResource(R.string.profile_theme_sub_system)
                        AppThemeMode.LIGHT -> stringResource(R.string.profile_theme_sub_light)
                        AppThemeMode.DARK -> stringResource(R.string.profile_theme_sub_dark)
                    },
                    onClick = onThemeClick
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                QuickMenuItem(
                    icon = Icons.Default.Person,
                    iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    iconTint = MaterialTheme.colorScheme.secondary,
                    title = stringResource(R.string.profile_edit_physical_title),
                    subtitle = stringResource(R.string.profile_edit_physical_sub),
                    onClick = onEditProfileClick
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                QuickMenuItem(
                    icon = Icons.Default.RestartAlt,
                    iconContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    iconTint = MaterialTheme.colorScheme.error,
                    title = stringResource(R.string.profile_reset_data_title),
                    subtitle = stringResource(R.string.profile_reset_data_sub),
                    onClick = onResetDataClick
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))

                QuickMenuItem(
                    icon = Icons.Default.Info,
                    iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    iconTint = MaterialTheme.colorScheme.tertiary,
                    title = stringResource(R.string.label_about_app),
                    subtitle = stringResource(R.string.profile_about_sub),
                    onClick = onAboutClick
                )
            }
        }

        // --- Log Out Button ---
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onLogOutClick() },
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.btn_log_out),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun LinkAccountDialog(
    linkAccountState: LinkAccountUiState,
    onDismiss: () -> Unit,
    onConfirmEmail: (email: String, password: String) -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var clientError by remember { mutableStateOf<String?>(null) }

    val errInvalidEmail = stringResource(R.string.profile_err_invalid_email)
    val errShortPassword = stringResource(R.string.profile_err_short_password)
    val errPasswordsNotMatch = stringResource(R.string.profile_err_passwords_not_match)

    AlertDialog(
        onDismissRequest = {
            if (!linkAccountState.isLinking) onDismiss()
        },
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Link,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.profile_link_account),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.profile_link_dialog_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        clientError = null
                    },
                    label = { Text(stringResource(R.string.label_email_address)) },
                    placeholder = { Text(stringResource(R.string.placeholder_email)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !linkAccountState.isLinking,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        clientError = null
                    },
                    label = { Text(stringResource(R.string.label_password_min)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) stringResource(R.string.profile_hide_password) else stringResource(R.string.profile_show_password)
                            )
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !linkAccountState.isLinking,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = {
                        confirmPassword = it
                        clientError = null
                    },
                    label = { Text(stringResource(R.string.label_confirm_password)) },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                    },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (confirmPasswordVisible) stringResource(R.string.profile_hide_password) else stringResource(R.string.profile_show_password)
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !linkAccountState.isLinking,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                val activeError = clientError ?: linkAccountState.errorMessage
                if (activeError != null) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = activeError,
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Medium
                            ),
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmedEmail = email.trim()
                    if (trimmedEmail.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                        clientError = errInvalidEmail
                    } else if (password.length < 6) {
                        clientError = errShortPassword
                    } else if (password != confirmPassword) {
                        clientError = errPasswordsNotMatch
                    } else {
                        onConfirmEmail(trimmedEmail, password)
                    }
                },
                enabled = !linkAccountState.isLinking,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (linkAccountState.isLinking) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.btn_linking), fontWeight = FontWeight.Bold)
                } else {
                    Text(stringResource(R.string.btn_link_account), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !linkAccountState.isLinking
            ) {
                Text(stringResource(R.string.dialog_cancel))
            }
        }
    )
}

@Composable
private fun EditProfileDialog(
    profile: ProfileEntity,
    onDismiss: () -> Unit,
    onSave: (name: String, age: Int, height: Int, weight: Float) -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var ageText by remember { mutableStateOf(profile.age.toString()) }
    var heightText by remember { mutableStateOf(profile.height.toString()) }
    var weightText by remember { mutableStateOf(profile.currentWeight.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(stringResource(R.string.title_edit_profile), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { ageText = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.label_age)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { heightText = it.filter { char -> char.isDigit() } },
                    label = { Text(stringResource(R.string.label_height)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text(stringResource(R.string.label_weight)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val parsedAge = ageText.toIntOrNull() ?: profile.age
                    val parsedHeight = heightText.toIntOrNull() ?: profile.height
                    val parsedWeight = weightText.toFloatOrNull() ?: profile.currentWeight
                    onSave(name, parsedAge, parsedHeight, parsedWeight)
                }
            ) {
                Text(stringResource(R.string.btn_save_changes), fontWeight = FontWeight.Bold)
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
private fun NutrientColumn(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = color
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
private fun QuickMenuItem(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    isLoading: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isLoading) { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(iconContainerColor),
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
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
            )
        }

        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun MetricColumn(
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        )
    }
}

@Composable
private fun EmptyProfileContent(
    onNavigateToGoalSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.profile_empty_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.profile_empty_desc),
            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onNavigateToGoalSettings,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(text = stringResource(R.string.profile_btn_create_profile), color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun getGoalTitle(goal: Goal): String {
    return when (goal) {
        Goal.LoseWeight -> stringResource(R.string.goal_lose_title)
        Goal.MaintainWeight -> stringResource(R.string.goal_maintain_title)
        Goal.GainWeight -> stringResource(R.string.goal_gain_title)
    }
}

@Composable
private fun PhotoSourceDialog(
    onDismiss: () -> Unit,
    onCameraSelect: () -> Unit,
    onGallerySelect: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.profile_change_photo_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onCameraSelect()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.profile_take_photo),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onDismiss()
                            onGallerySelect()
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.profile_choose_gallery),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.dialog_cancel), color = MaterialTheme.colorScheme.primary)
            }
        }
    )
}

@Composable
private fun ThemeSelectionDialog(
    currentMode: AppThemeMode,
    onDismiss: () -> Unit,
    onSelectMode: (AppThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(R.string.profile_theme_dialog_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeOptionItem(
                    title = stringResource(R.string.profile_theme_sub_system),
                    subtitle = stringResource(R.string.profile_theme_desc_system),
                    icon = Icons.Default.BrightnessAuto,
                    isSelected = currentMode == AppThemeMode.SYSTEM,
                    onClick = { onSelectMode(AppThemeMode.SYSTEM) }
                )
                ThemeOptionItem(
                    title = stringResource(R.string.profile_theme_sub_light),
                    subtitle = stringResource(R.string.profile_theme_desc_light),
                    icon = Icons.Default.LightMode,
                    isSelected = currentMode == AppThemeMode.LIGHT,
                    onClick = { onSelectMode(AppThemeMode.LIGHT) }
                )
                ThemeOptionItem(
                    title = stringResource(R.string.profile_theme_sub_dark),
                    subtitle = stringResource(R.string.profile_theme_desc_dark),
                    icon = Icons.Default.DarkMode,
                    isSelected = currentMode == AppThemeMode.DARK,
                    onClick = { onSelectMode(AppThemeMode.DARK) }
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.dialog_cancel),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}

@Composable
private fun ThemeOptionItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else Color.Transparent,
        border = if (isSelected) BorderStroke(1.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = MaterialTheme.colorScheme.primary)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
