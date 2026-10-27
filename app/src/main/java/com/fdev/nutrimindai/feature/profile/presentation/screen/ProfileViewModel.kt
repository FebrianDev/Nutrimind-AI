package com.fdev.nutrimindai.feature.profile.presentation.screen

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fdev.nutrimindai.core.database.NutriMindDatabase
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.feature.onboarding.data.entity.ProfileEntity
import com.fdev.nutrimindai.feature.onboarding.data.repository.ProfileRepository
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
    private val profileRepository: ProfileRepository,
    private val preferenceManager: PreferenceManager,
    private val database: NutriMindDatabase,
    private val syncManager: SyncManager,
    private val firestoreSyncRepository: FirestoreSyncRepository,
    private val weightLogRepository: WeightLogRepository,
    private val authRepository: AuthRepository,
    private val reminderScheduler: com.fdev.nutrimindai.core.notification.ReminderScheduler
) : ViewModel() {

    private val _syncState = MutableStateFlow(SyncUiState())
    val syncState: StateFlow<SyncUiState> = _syncState.asStateFlow()

    private val _isUploadingPhoto = MutableStateFlow(false)
    val isUploadingPhoto: StateFlow<Boolean> = _isUploadingPhoto.asStateFlow()


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
                val plan = com.fdev.nutrimindai.feature.onboarding.util.NutritionCalculator.calculatePlan(
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
            } catch (e: Exception) {
                Timber.e(e, "Error updating personal info")
            }
        }
    }

    fun logout(onSuccess: () -> Unit) {

        viewModelScope.launch {
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

                // 5. Trigger callback to navigate to Onboarding / Auth screen
                withContext(Dispatchers.Main) {
                    onSuccess()
                }
            } catch (e: Exception) {
                Timber.e(e, "Error during logout and data clearing")
            }
        }
    }

    fun uploadProfileImage(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch {
            _isUploadingPhoto.value = true
            try {
                // 1. Save local copy to app files directory
                val localFile = java.io.File(context.filesDir, "profile_photo.jpg")
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        java.io.FileOutputStream(localFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
                val localPhotoUrl = "file://${localFile.absolutePath}"

                // 2. Update local Room DB immediately so UI responds instantly
                val currentProfile = profileRepository.getProfileDirect()
                if (currentProfile != null) {
                    profileRepository.updateProfile(currentProfile.copy(photoUrl = localPhotoUrl, isSync = false, updatedAt = System.currentTimeMillis()))
                }

                // 3. Try uploading to Firebase Storage & Firestore
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
                            profileRepository.updateProfile(latestProfile.copy(photoUrl = downloadUrl, isSync = true, updatedAt = System.currentTimeMillis()))
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error uploading profile image to Firebase Storage")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error processing profile image")
            } finally {
                _isUploadingPhoto.value = false
            }
        }
    }
}



