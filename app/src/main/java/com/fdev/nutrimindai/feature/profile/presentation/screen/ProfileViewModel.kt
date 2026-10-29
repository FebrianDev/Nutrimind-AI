package com.fdev.nutrimindai.feature.profile.presentation.screen

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.R
import dagger.hilt.android.qualifiers.ApplicationContext
import com.fdev.nutrimindai.core.database.NutriMindDatabase
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.core.datastore.AppThemeMode
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.feature.profile.domain.repository.ProfileRepository
import com.fdev.nutrimindai.core.sync.FirestoreSyncRepository
import com.fdev.nutrimindai.core.sync.SyncManager
import com.fdev.nutrimindai.feature.progress.domain.repository.WeightLogRepository
import com.fdev.nutrimindai.feature.auth.domain.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class SyncUiState(
    val isSyncing: Boolean = false,
    val unsyncedCount: Int = 0,
    val lastSyncMessage: String? = null
)

data class AccountInfo(
    val email: String? = null,
    val isAnonymous: Boolean = false,
    val isLoggedIn: Boolean = false,
    val displayName: String? = null
)

data class LinkAccountUiState(
    val isLinking: Boolean = false,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null
)

/**
 * UI State representations for the Profile screen.
 */
sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Success(val profile: ProfileEntity?) : ProfileUiState
    data class Error(val message: String) : ProfileUiState
}

/**
 * ViewModel managing data loading, sync, and logout/reset interactions for [ProfileScreen].
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val profileRepository: ProfileRepository,
    private val preferenceManager: PreferenceManager,
    private val database: NutriMindDatabase,
    private val syncManager: SyncManager,
    private val firestoreSyncRepository: FirestoreSyncRepository,
    private val weightLogRepository: WeightLogRepository,
    private val authRepository: AuthRepository,
    private val reminderScheduler: com.fdev.nutrimindai.core.notification.ReminderScheduler
) : ViewModel() {

    val themeMode: StateFlow<AppThemeMode> = preferenceManager.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = AppThemeMode.SYSTEM
        )

    fun setThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferenceManager.setThemeMode(mode)
        }
    }

    private val _syncState = MutableStateFlow(SyncUiState())
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()

    private val _isLoggingOut = MutableStateFlow(false)
    val isLoggingOut: StateFlow<Boolean> = _isLoggingOut.asStateFlow()


    private val _accountInfo = MutableStateFlow(
        AccountInfo(
            email = authRepository.getCurrentUser()?.email,
            isAnonymous = authRepository.isAnonymousUser(),
            isLoggedIn = authRepository.isUserLoggedIn(),
            displayName = authRepository.getCurrentUser()?.displayName
        )
    )
    val accountInfo: StateFlow<AccountInfo> = _accountInfo.asStateFlow()

    private val _linkAccountState = MutableStateFlow(LinkAccountUiState())
    val linkAccountState: StateFlow<LinkAccountUiState> = _linkAccountState.asStateFlow()

    val uiState: StateFlow<ProfileUiState> = profileRepository.getProfile()
        .map<ProfileEntity?, ProfileUiState> { profile ->
            refreshUnsyncedCount()
            refreshAccountInfo()
            ProfileUiState.Success(profile)
        }
        .catch { e ->
            emit(ProfileUiState.Error(e.message ?: "Failed to load profile"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ProfileUiState.Loading
        )

    fun refreshAccountInfo() {
        val user = authRepository.getCurrentUser()
        _accountInfo.value = AccountInfo(
            email = user?.email,
            isAnonymous = user?.isAnonymous == true,
            isLoggedIn = user != null,
            displayName = user?.displayName
        )
    }

    fun linkAccount(email: String, password: String, displayName: String?) {
        viewModelScope.launch {
            _linkAccountState.value = LinkAccountUiState(isLinking = true)
            val result = authRepository.linkAnonymousWithEmail(email, password, displayName)
            result.fold(
                onSuccess = {
                    _linkAccountState.value = LinkAccountUiState(isSuccess = true)
                    refreshAccountInfo()
                    syncDataNow()
                },
                onFailure = { err ->
                    _linkAccountState.value = LinkAccountUiState(
                        isLinking = false,
                        errorMessage = err.message ?: "Failed to link account"
                    )
                }
            )
        }
    }

    fun linkAccountWithGoogle(idToken: String, displayName: String?) {
        viewModelScope.launch {
            _linkAccountState.value = LinkAccountUiState(isLinking = true)
            val result = authRepository.linkAnonymousWithGoogle(idToken, displayName)
            result.fold(
                onSuccess = {
                    _linkAccountState.value = LinkAccountUiState(isSuccess = true)
                    refreshAccountInfo()
                    syncDataNow()
                },
                onFailure = { err ->
                    _linkAccountState.value = LinkAccountUiState(
                        isLinking = false,
                        errorMessage = err.message ?: "Failed to link Google account"
                    )
                }
            )
        }
    }

    fun resetLinkAccountState() {
        _linkAccountState.value = LinkAccountUiState()
    }

    fun refreshUnsyncedCount() {
        viewModelScope.launch {
            try {
                val count = withContext(Dispatchers.IO) {
                    firestoreSyncRepository.getUnsyncedCount()
                }
                _syncState.value = _syncState.value.copy(unsyncedCount = count)
            } catch (e: Exception) {
                Timber.e(e, "Error refreshing unsynced count")
            }
        }
    }

    fun syncDataNow() {
        viewModelScope.launch {
            _syncState.value = _syncState.value.copy(isSyncing = true, lastSyncMessage = null)
            val result = syncManager.runSyncNowDirectly()
            _syncState.value = SyncUiState(
                isSyncing = false,
                unsyncedCount = result.remainingUnsyncedCount,
                lastSyncMessage = result.message
            )
        }
    }

    fun clearSyncMessage() {
        _syncState.value = _syncState.value.copy(lastSyncMessage = null)
    }

    fun updatePersonalInfo(name: String, age: Int, height: Int, weight: Float) {
        viewModelScope.launch {
            try {
                val currentProfile = (uiState.value as? ProfileUiState.Success)?.profile ?: return@launch
                val plan = com.fdev.nutrimindai.core.util.NutritionCalculator.calculatePlan(
                    age = age,
                    gender = currentProfile.gender,
                    height = height,
                    currentWeight = weight,
                    targetWeight = currentProfile.targetWeight,
                    activityLevel = currentProfile.activityLevel,
                    goal = currentProfile.goal
                )

                val updated = currentProfile.copy(
                    name = name.trim().ifEmpty { currentProfile.name },
                    age = age,
                    height = height,
                    currentWeight = weight,
                    dailyCalories = plan.dailyCalories,
                    dailyProtein = plan.dailyProtein,
                    dailyCarbs = plan.dailyCarbs,
                    dailyFat = plan.dailyFat,
                    dailyExerciseMinutes = plan.dailyExerciseMinutes,
                    updatedAt = System.currentTimeMillis()
                )
                profileRepository.updateProfile(updated)

                // Seamlessly log weight into WeightLog table for today's date
                val todayDateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                weightLogRepository.logWeight(
                    date = todayDateStr,
                    weightKg = weight,
                    note = "Updated from Profile"
                )

                syncManager.triggerManualSync()
            } catch (e: Exception) {
                Timber.e(e, "Error updating personal info")
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {
        viewModelScope.launch {
            _isLoggingOut.value = true
            try {
                // 1. Sign out Firebase Auth & RevenueCat if logged in
                try {
                    FirebaseAuth.getInstance().signOut()
                } catch (e: Exception) {
                    Timber.e(e, "Sign out error")
                }

                // 2. Cancel all scheduled reminder alarms in Android OS AlarmManager
                reminderScheduler.cancelAllReminders()

                // 3. Clear all Room database tables in IO thread
                withContext(Dispatchers.IO) {
                    database.clearAllTables()
                }

                // 4. Clear DataStore Preferences (onboarding completed = false)
                preferenceManager.clearAllPreferences()

                // Graceful delay so user sees progress bar complete
                kotlinx.coroutines.delay(1500)

                // 5. Dismiss loading dialog FIRST so it disappears before navigating
                _isLoggingOut.value = false

                // Short delay to let the dialog cleanly close from the screen
                kotlinx.coroutines.delay(150)

                // 6. Trigger navigation after loading dialog is completely dismissed
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during logout and data clearing")
                _isLoggingOut.value = false
            }
        }
    }

    /**
     * Clears only daily activity logging data (food intake, water, exercise, AI chats)
     * while keeping user profile, weight logs, and auth session intact.
     */
    fun resetProgressData() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    database.runInTransaction {
                        val db = database.openHelper.writableDatabase
                        db.execSQL("DELETE FROM intake")
                        db.execSQL("DELETE FROM water")
                        db.execSQL("DELETE FROM exercise")
                        db.execSQL("DELETE FROM daily_log")
                        db.execSQL("DELETE FROM food_table")
                        db.execSQL("DELETE FROM chat_message_table")
                    }
                }
                refreshUnsyncedCount()
                _syncState.value = _syncState.value.copy(
                    lastSyncMessage = context.getString(R.string.profile_reset_data_success)
                )
            } catch (e: Exception) {
                Timber.e(e, "Error clearing logging data")
                _syncState.value = _syncState.value.copy(
                    lastSyncMessage = context.getString(R.string.profile_reset_data_error)
                )
            }
        }
    }

    fun uploadProfileImage(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            try {
                // 1. Process and save local copy to app files directory
                val localFile = withContext(Dispatchers.IO) {
                    saveProfilePhotoLocally(context, uri)
                } ?: throw IllegalStateException("Unable to process or save profile photo locally")

                val localPhotoUrl = "file://${localFile.absolutePath}"

                // 2. Update local Room DB immediately so UI responds instantly
                val currentProfile = profileRepository.getProfileDirect()
                if (currentProfile != null) {
                    profileRepository.updateProfile(
                        currentProfile.copy(
                            photoUrl = localPhotoUrl,
                            isSync = false,
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                }

                _syncState.value = _syncState.value.copy(
                    lastSyncMessage = context.getString(R.string.profile_photo_updated_success)
                )

                // 3. Try uploading to Firebase Storage & Firestore in background
                val user = authRepository.getCurrentUser()
                val userId = authRepository.getCurrentUserId() ?: "local_user"
                if (localFile.exists()) {
                    try {
                        val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference.child("profile_images/$userId.jpg")
                        storageRef.putFile(android.net.Uri.fromFile(localFile)).await()
                        val downloadUrl = storageRef.downloadUrl.await().toString()

                        // Update Firebase Auth user profile photo
                        try {
                            val request = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                .setPhotoUri(android.net.Uri.parse(downloadUrl))
                                .build()
                            user?.updateProfile(request)?.await()
                        } catch (e: Exception) {
                            Timber.w(e, "Error updating Firebase Auth profile photo")
                        }

                        // Update Firestore user document
                        try {
                            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                            db.collection("users").document(userId)
                                .set(mapOf("photoUrl" to downloadUrl), com.google.firebase.firestore.SetOptions.merge()).await()
                            db.collection("users").document(userId).collection("profiles").document("user_profile")
                                .set(mapOf("photoUrl" to downloadUrl), com.google.firebase.firestore.SetOptions.merge()).await()
                        } catch (e: Exception) {
                            Timber.w(e, "Error updating Firestore profile photo")
                        }

                        // Update Room DB with remote download URL
                        val latestProfile = profileRepository.getProfileDirect()
                        if (latestProfile != null) {
                            profileRepository.updateProfile(
                                latestProfile.copy(
                                    photoUrl = downloadUrl,
                                    isSync = true,
                                    updatedAt = System.currentTimeMillis()
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error uploading profile image to Firebase Storage")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing profile image")
                _syncState.value = _syncState.value.copy(
                    lastSyncMessage = context.getString(R.string.profile_photo_updated_error)
                )
            } finally {
                _isUploadingPhoto.value = false
            }
        }
    }

    private fun saveProfilePhotoLocally(context: android.content.Context, uri: android.net.Uri): File? {
        return try {
            val timestamp = System.currentTimeMillis()
            val targetFile = File(context.filesDir, "profile_photo_$timestamp.jpg")

            val bitmap = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val source = ImageDecoder.createSource(context.contentResolver, uri)
                    ImageDecoder.decodeBitmap(source) { decoder, _, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        decoder.isMutableRequired = true
                    }
                } else {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
            } catch (e: Exception) {
                Timber.w(e, "ImageDecoder/BitmapFactory decode failed, falling back to direct stream copy")
                null
            }

            if (bitmap != null) {
                val maxDim = 800
                val width = bitmap.width
                val height = bitmap.height
                val scaledBitmap = if (width > maxDim || height > maxDim) {
                    val scale = maxDim.toFloat() / Math.max(width, height)
                    val matrix = Matrix()
                    matrix.postScale(scale, scale)
                    Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true)
                } else {
                    bitmap
                }

                FileOutputStream(targetFile).use { out ->
                    scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                }
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(targetFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return null
            }

            // Cleanup older profile photo files
            context.filesDir.listFiles { file ->
                file.name.startsWith("profile_photo") && file.name != targetFile.name
            }?.forEach { oldFile ->
                try {
                    oldFile.delete()
                } catch (e: Exception) {
                    Timber.w(e, "Could not delete old profile photo: ${oldFile.name}")
                }
            }

            targetFile
        } catch (e: Exception) {
            Timber.e(e, "Failed to save profile photo locally")
            null
        }
    }
}



