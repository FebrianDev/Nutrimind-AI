package com.fdev.nutrimindai.core.sync

import android.net.Uri
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.DailyLogDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.ExerciseDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.FoodDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.IntakeDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.WaterDao
import com.fdev.nutrimindai.feature.profile.data.local.dao.ProfileDao
import com.fdev.nutrimindai.feature.progress.data.local.dao.WeightLogDao
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.core.model.Gender
import com.fdev.nutrimindai.core.model.ActivityLevel
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.DailyLogEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.IntakeEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.WaterEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.ExerciseEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.FoodEntity
import com.fdev.nutrimindai.feature.progress.data.local.entity.WeightLogEntity

import com.fdev.nutrimindai.core.datastore.PreferenceManager

data class SyncResult(
    val success: Boolean,
    val syncedItemsCount: Int,
    val remainingUnsyncedCount: Int,
    val message: String
)

@Singleton
class FirestoreSyncRepository @Inject constructor(
    private val authManager: FirebaseAuthManager,
    private val profileDao: ProfileDao,
    private val dailyLogDao: DailyLogDao,
    private val intakeDao: IntakeDao,
    private val waterDao: WaterDao,
    private val exerciseDao: ExerciseDao,
    private val foodDao: FoodDao,
    private val weightLogDao: WeightLogDao,
    private val preferenceManager: PreferenceManager
) {

    private val syncMutex = Mutex()

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Timber.w("FirestoreSyncRepository: Firestore is not initialized.")
            null
        }
    }

    private val storage: FirebaseStorage? by lazy {
        try {
            FirebaseStorage.getInstance()
        } catch (e: Exception) {
            Timber.w("FirestoreSyncRepository: FirebaseStorage is not initialized.")
            null
        }
    }

    suspend fun getUnsyncedCount(): Int {
        val profiles = profileDao.getUnsyncedProfiles().size
        val dailyLogs = dailyLogDao.getUnsyncedDailyLogs().size
        val intakes = intakeDao.getUnsyncedIntakes().size
        val waters = waterDao.getUnsyncedWater().size
        val exercises = exerciseDao.getUnsyncedExercises().size
        val foods = foodDao.getUnsyncedFoods().size
        val weights = weightLogDao.getUnsyncedWeightLogs().size
        val pendingDeletions = preferenceManager.getPendingCloudDeletions().size
        val pendingReset = if (preferenceManager.isPendingCloudReset()) 1 else 0
        return profiles + dailyLogs + intakes + waters + exercises + foods + weights + pendingDeletions + pendingReset
    }

    private suspend fun processPendingCloudDeletions(userRef: DocumentReference): Int {
        var processedCount = 0

        if (preferenceManager.isPendingCloudReset()) {
            val collectionsToClear = listOf("intakes", "water_records", "exercises", "daily_logs", "foods")
            for (collectionName in collectionsToClear) {
                val snapshot = userRef.collection(collectionName).get().await()
                for (doc in snapshot.documents) {
                    doc.reference.delete().await()
                    processedCount++
                }
            }
            preferenceManager.setPendingCloudReset(false)
            processedCount++
        }

        val pendingDeletions = preferenceManager.getPendingCloudDeletions()
        if (pendingDeletions.isNotEmpty()) {
            val completedEntries = mutableSetOf<String>()
            for (entry in pendingDeletions) {
                val parts = entry.split(":", limit = 2)
                if (parts.size == 2) {
                    val collection = parts[0]
                    val docId = parts[1]
                    if (collection.isNotBlank() && docId.isNotBlank()) {
                        userRef.collection(collection).document(docId).delete().await()
                    }
                }
                completedEntries.add(entry)
            }
            preferenceManager.removePendingCloudDeletions(completedEntries)
            processedCount += completedEntries.size
        }

        return processedCount
    }

    private suspend fun uploadFoodImageIfNeeded(
        userId: String,
        docId: String,
        imagePath: String?
    ): String? {
        if (imagePath.isNullOrBlank()) return null
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) return imagePath

        val localFile = File(imagePath)
        if (!localFile.exists()) return null

        val storageInstance = storage ?: return imagePath
        return try {
            val imageRef = storageInstance.reference.child("users/$userId/food_images/$docId.jpg")
            imageRef.putFile(Uri.fromFile(localFile)).await()
            imageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            Timber.w(e, "FirestoreSyncRepository: Failed to upload food image, keeping local path")
            imagePath
        }
    }

    suspend fun syncUnsyncedData(): SyncResult = syncMutex.withLock {
        val db = firestore
        if (db == null) {
            val remaining = getUnsyncedCount()
            return SyncResult(
                success = false,
                syncedItemsCount = 0,
                remainingUnsyncedCount = remaining,
                message = "Firebase Firestore is not initialized."
            )
        }

        val userId = authManager.getOrCreateUserId()
        val userRef = db.collection("users").document(userId)
        var totalSynced = 0

        try {
            // 0. Process pending cloud deletions / progress resets first
            totalSynced += processPendingCloudDeletions(userRef)

            // 1. Sync Profiles
            val unsyncedProfiles = profileDao.getUnsyncedProfiles()
            if (unsyncedProfiles.isNotEmpty()) {
                val profileMap = unsyncedProfiles.associate { profile ->
                    profile.id to mapOf(
                        "id" to profile.id,
                        "name" to profile.name,
                        "age" to profile.age,
                        "gender" to profile.gender.name,
                        "height" to profile.height,
                        "currentWeight" to profile.currentWeight,
                        "activityLevel" to profile.activityLevel.name,
                        "goal" to profile.goal.name,
                        "targetWeight" to profile.targetWeight,
                        "dailyCalories" to profile.dailyCalories,
                        "dailyProtein" to profile.dailyProtein,
                        "dailyCarbs" to profile.dailyCarbs,
                        "dailyFat" to profile.dailyFat,
                        "dailyExerciseMinutes" to profile.dailyExerciseMinutes,
                        "photoUrl" to profile.photoUrl,
                        "createdAt" to profile.createdAt,
                        "updatedAt" to profile.updatedAt,
                        "isSync" to true
                    )
                }

                for ((id, data) in profileMap) {
                    userRef.collection("profiles").document(id).set(data).await()
                }
                profileDao.markProfilesSynced(unsyncedProfiles.map { it.id })
                totalSynced += unsyncedProfiles.size
            }

            // 2. Sync Daily Logs (using date as deterministic document ID)
            val unsyncedDailyLogs = dailyLogDao.getUnsyncedDailyLogs()
            if (unsyncedDailyLogs.isNotEmpty()) {
                for (log in unsyncedDailyLogs) {
                    val docId = log.date.ifBlank { log.id.toString() }
                    val data = mapOf(
                        "id" to log.id,
                        "date" to log.date,
                        "status" to log.status.name,
                        "createdAt" to log.createdAt,
                        "completedAt" to log.completedAt,
                        "aiSummary" to log.aiSummary,
                        "aiScore" to log.aiScore,
                        "isSync" to true
                    )
                    userRef.collection("daily_logs").document(docId).set(data).await()
                }
                dailyLogDao.markDailyLogsSynced(unsyncedDailyLogs.map { it.id })
                totalSynced += unsyncedDailyLogs.size
            }

            val logIdToDate = dailyLogDao.getAllDailyLogsDirect().associate { it.id to it.date }

            // 3. Sync Intakes (using composite createdAt_id document ID & uploading food photo)
            val unsyncedIntakes = intakeDao.getUnsyncedIntakes()
            if (unsyncedIntakes.isNotEmpty()) {
                for (intake in unsyncedIntakes) {
                    val docId = "${intake.createdAt}_${intake.id}"
                    val syncedImagePath = uploadFoodImageIfNeeded(userId, docId, intake.imagePath)
                    val data = mapOf(
                        "id" to intake.id,
                        "dailyLogId" to intake.dailyLogId,
                        "logDate" to (logIdToDate[intake.dailyLogId] ?: ""),
                        "mealType" to intake.mealType.name,
                        "foodName" to intake.foodName,
                        "servingSize" to intake.servingSize,
                        "servingUnit" to intake.servingUnit,
                        "calories" to intake.calories,
                        "protein" to intake.protein,
                        "carbs" to intake.carbs,
                        "fat" to intake.fat,
                        "fiber" to intake.fiber,
                        "imagePath" to syncedImagePath,
                        "foodSource" to intake.foodSource.name,
                        "createdAt" to intake.createdAt,
                        "isSync" to true
                    )
                    userRef.collection("intakes").document(docId).set(data).await()
                }
                intakeDao.markIntakesSynced(unsyncedIntakes.map { it.id })
                totalSynced += unsyncedIntakes.size
            }

            // 4. Sync Water Records (using composite createdAt_id document ID)
            val unsyncedWaters = waterDao.getUnsyncedWater()
            if (unsyncedWaters.isNotEmpty()) {
                for (water in unsyncedWaters) {
                    val docId = "${water.createdAt}_${water.id}"
                    val data = mapOf(
                        "id" to water.id,
                        "dailyLogId" to water.dailyLogId,
                        "logDate" to (logIdToDate[water.dailyLogId] ?: ""),
                        "amountMl" to water.amountMl,
                        "createdAt" to water.createdAt,
                        "isSync" to true
                    )
                    userRef.collection("water_records").document(docId).set(data).await()
                }
                waterDao.markWaterSynced(unsyncedWaters.map { it.id })
                totalSynced += unsyncedWaters.size
            }

            // 5. Sync Exercises (using composite createdAt_id document ID)
            val unsyncedExercises = exerciseDao.getUnsyncedExercises()
            if (unsyncedExercises.isNotEmpty()) {
                for (ex in unsyncedExercises) {
                    val docId = "${ex.createdAt}_${ex.id}"
                    val data = mapOf(
                        "id" to ex.id,
                        "dailyLogId" to ex.dailyLogId,
                        "logDate" to (logIdToDate[ex.dailyLogId] ?: ""),
                        "exerciseType" to ex.exerciseType,
                        "durationMinute" to ex.durationMinute,
                        "caloriesBurned" to ex.caloriesBurned,
                        "note" to ex.note,
                        "createdAt" to ex.createdAt,
                        "isSync" to true
                    )
                    userRef.collection("exercises").document(docId).set(data).await()
                }
                exerciseDao.markExercisesSynced(unsyncedExercises.map { it.id })
                totalSynced += unsyncedExercises.size
            }

            // 6. Sync Custom Foods (using normalized name document ID to avoid colliding with pre-seeded IDs)
            val unsyncedFoods = foodDao.getUnsyncedFoods()
            if (unsyncedFoods.isNotEmpty()) {
                for (food in unsyncedFoods) {
                    val normalizedName = food.name.trim()
                        .lowercase(Locale.ROOT)
                        .replace(Regex("[^a-z0-9]+"), "_")
                        .trim('_')
                    val docId = if (normalizedName.isNotBlank()) "food_$normalizedName" else "food_${food.id}"
                    val data = mapOf(
                        "id" to food.id,
                        "name" to food.name,
                        "calories" to food.calories,
                        "protein" to food.protein,
                        "carbs" to food.carbs,
                        "fat" to food.fat,
                        "servingSize" to food.servingSize,
                        "servingUnit" to food.servingUnit,
                        "isSync" to true
                    )
                    userRef.collection("foods").document(docId).set(data).await()
                }
                foodDao.markFoodsSynced(unsyncedFoods.map { it.id })
                totalSynced += unsyncedFoods.size
            }

            // 7. Sync Weight Logs (using date as deterministic document ID)
            val unsyncedWeightLogs = weightLogDao.getUnsyncedWeightLogs()
            if (unsyncedWeightLogs.isNotEmpty()) {
                for (log in unsyncedWeightLogs) {
                    val docId = log.date.ifBlank { "${log.timestamp}_${log.id}" }
                    val data = mapOf(
                        "id" to log.id,
                        "date" to log.date,
                        "weightKg" to log.weightKg,
                        "note" to log.note,
                        "timestamp" to log.timestamp,
                        "isSync" to true
                    )
                    userRef.collection("weight_logs").document(docId).set(data).await()
                }
                weightLogDao.markWeightLogsSynced(unsyncedWeightLogs.map { it.id })
                totalSynced += unsyncedWeightLogs.size
            }

            val remaining = getUnsyncedCount()
            Timber.d("FirestoreSyncRepository: Successfully synced $totalSynced items to Firestore.")
            return SyncResult(
                success = true,
                syncedItemsCount = totalSynced,
                remainingUnsyncedCount = remaining,
                message = "Synced $totalSynced items successfully."
            )
        } catch (e: Exception) {
            Timber.e(e, "FirestoreSyncRepository: Sync error")
            val remaining = getUnsyncedCount()
            return SyncResult(
                success = false,
                syncedItemsCount = totalSynced,
                remainingUnsyncedCount = remaining,
                message = e.message ?: "Sync failed due to an error."
            )
        }
    }

    suspend fun downloadAndRestoreUserData(): Result<Unit> = syncMutex.withLock {
        val db = firestore ?: return Result.failure(Exception("Firebase Firestore is not initialized."))
        val userId = authManager.getOrCreateUserId()
        val userRef = db.collection("users").document(userId)

        return try {
            // Apply any pending local deletions or resets before downloading from cloud
            processPendingCloudDeletions(userRef)

            // 1. Fetch & Restore profiles
            val profileSnapshot = userRef.collection("profiles").get().await()
            val restoredProfiles = if (!profileSnapshot.isEmpty) {
                profileSnapshot.documents.mapNotNull { doc ->
                    try {
                        val genderStr = doc.getString("gender") ?: "Male"
                        val activityLevelStr = doc.getString("activityLevel") ?: "Sedentary"
                        val goalStr = doc.getString("goal") ?: "MaintainWeight"

                        ProfileEntity(
                            id = doc.getString("id") ?: ProfileEntity.DEFAULT_PROFILE_ID,
                            name = doc.getString("name") ?: "",
                            age = doc.getLong("age")?.toInt() ?: 0,
                            gender = Gender.valueOf(genderStr),
                            height = doc.getLong("height")?.toInt() ?: 0,
                            currentWeight = doc.getDouble("currentWeight")?.toFloat() ?: 0f,
                            activityLevel = ActivityLevel.valueOf(activityLevelStr),
                            goal = Goal.valueOf(goalStr),
                            targetWeight = doc.getDouble("targetWeight")?.toFloat() ?: 0f,
                            dailyCalories = doc.getLong("dailyCalories")?.toInt() ?: 0,
                            dailyProtein = doc.getLong("dailyProtein")?.toInt() ?: 0,
                            dailyCarbs = doc.getLong("dailyCarbs")?.toInt() ?: 0,
                            dailyFat = doc.getLong("dailyFat")?.toInt() ?: 0,
                            dailyExerciseMinutes = doc.getLong("dailyExerciseMinutes")?.toInt() ?: 0,
                            photoUrl = doc.getString("photoUrl"),
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),
                            isSync = true
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing profile document")
                        null
                    }
                }
            } else {
                // Fallback: Check if profile fields exist directly in root user doc
                val userDoc = userRef.get().await()
                if (userDoc.exists() && (userDoc.contains("dailyCalories") || userDoc.contains("height"))) {
                    try {
                        val genderStr = userDoc.getString("gender") ?: "Male"
                        val activityLevelStr = userDoc.getString("activityLevel") ?: "Sedentary"
                        val goalStr = userDoc.getString("goal") ?: "MaintainWeight"
                        listOf(
                            ProfileEntity(
                                id = ProfileEntity.DEFAULT_PROFILE_ID,
                                name = userDoc.getString("name") ?: "",
                                age = userDoc.getLong("age")?.toInt() ?: 0,
                                gender = Gender.valueOf(genderStr),
                                height = userDoc.getLong("height")?.toInt() ?: 0,
                                currentWeight = userDoc.getDouble("currentWeight")?.toFloat() ?: 0f,
                                activityLevel = ActivityLevel.valueOf(activityLevelStr),
                                goal = Goal.valueOf(goalStr),
                                targetWeight = userDoc.getDouble("targetWeight")?.toFloat() ?: 0f,
                                dailyCalories = userDoc.getLong("dailyCalories")?.toInt() ?: 0,
                                dailyProtein = userDoc.getLong("dailyProtein")?.toInt() ?: 0,
                                dailyCarbs = userDoc.getLong("dailyCarbs")?.toInt() ?: 0,
                                dailyFat = userDoc.getLong("dailyFat")?.toInt() ?: 0,
                                dailyExerciseMinutes = userDoc.getLong("dailyExerciseMinutes")?.toInt() ?: 0,
                                photoUrl = userDoc.getString("photoUrl"),
                                createdAt = userDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                                updatedAt = userDoc.getLong("updatedAt") ?: System.currentTimeMillis(),
                                isSync = true
                            )
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing root profile doc")
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }

            if (restoredProfiles.isNotEmpty()) {
                profileDao.insertProfile(restoredProfiles.first())
                preferenceManager.setOnboardingCompleted(true)
            }

            val remoteLogIdToDate = mutableMapOf<Long, String>()
            val dateToLocalLogId = dailyLogDao.getAllDailyLogsDirect()
                .associate { it.date to it.id }
                .toMutableMap()

            // 2. Fetch & Restore Daily Logs (deduplicated by date, preserving local primary keys)
            val dailyLogsSnapshot = userRef.collection("daily_logs").get().await()
            if (!dailyLogsSnapshot.isEmpty) {
                val parsedDailyLogs = dailyLogsSnapshot.documents.mapNotNull { doc ->
                    try {
                        val statusStr = doc.getString("status") ?: "DRAFT"
                        val remoteId = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L
                        val dateStr = doc.getString("date") ?: ""
                        if (remoteId > 0L && dateStr.isNotBlank()) {
                            remoteLogIdToDate[remoteId] = dateStr
                        }
                        DailyLogEntity(
                            id = remoteId,
                            date = dateStr,
                            status = DailyLogStatus.valueOf(statusStr),
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            completedAt = doc.getLong("completedAt"),
                            aiSummary = doc.getString("aiSummary"),
                            aiScore = doc.getLong("aiScore")?.toInt(),
                            isSync = true
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing daily log document")
                        null
                    }
                }

                val deduplicatedByDate = parsedDailyLogs
                    .filter { it.date.isNotBlank() }
                    .groupBy { it.date }
                    .mapValues { (_, logs) ->
                        logs.maxWithOrNull(
                            compareBy<DailyLogEntity> { it.status == DailyLogStatus.COMPLETED }
                                .thenBy { !it.aiSummary.isNullOrBlank() }
                                .thenBy { it.completedAt ?: 0L }
                                .thenBy { it.createdAt }
                        )!!
                    }

                for ((dateStr, log) in deduplicatedByDate) {
                    val existingLocal = dailyLogDao.getDailyLogByDateDirect(dateStr)
                    if (existingLocal != null) {
                        val mergedStatus = if (
                            existingLocal.status == DailyLogStatus.COMPLETED ||
                            log.status == DailyLogStatus.COMPLETED
                        ) {
                            DailyLogStatus.COMPLETED
                        } else {
                            log.status
                        }
                        dailyLogDao.updateDailyLog(
                            existingLocal.copy(
                                status = mergedStatus,
                                completedAt = log.completedAt ?: existingLocal.completedAt,
                                aiSummary = log.aiSummary?.takeIf { it.isNotBlank() } ?: existingLocal.aiSummary,
                                aiScore = log.aiScore ?: existingLocal.aiScore,
                                isSync = true
                            )
                        )
                        dateToLocalLogId[dateStr] = existingLocal.id
                    } else {
                        val insertedId = dailyLogDao.insertDailyLog(log.copy(id = 0L, isSync = true))
                        val finalId = if (insertedId > 0L) {
                            insertedId
                        } else {
                            dailyLogDao.getDailyLogByDateDirect(dateStr)?.id ?: 0L
                        }
                        if (finalId > 0L) {
                            dateToLocalLogId[dateStr] = finalId
                        }
                    }
                }
            }

            suspend fun resolveLocalDailyLogId(remoteDailyLogId: Long, logDate: String?): Long {
                val resolvedDate = logDate?.takeIf { it.isNotBlank() }
                    ?: remoteLogIdToDate[remoteDailyLogId]
                    ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

                dateToLocalLogId[resolvedDate]?.let { if (it > 0L) return it }

                val existing = dailyLogDao.getDailyLogByDateDirect(resolvedDate)
                if (existing != null) {
                    dateToLocalLogId[resolvedDate] = existing.id
                    return existing.id
                }

                val insertedId = dailyLogDao.insertDailyLog(
                    DailyLogEntity(
                        id = 0L,
                        date = resolvedDate,
                        status = DailyLogStatus.DRAFT,
                        isSync = true
                    )
                )
                val finalId = if (insertedId > 0L) {
                    insertedId
                } else {
                    dailyLogDao.getDailyLogByDateDirect(resolvedDate)?.id ?: 0L
                }
                if (finalId > 0L) {
                    dateToLocalLogId[resolvedDate] = finalId
                }
                return finalId
            }

            // 3. Fetch & Restore Intakes
            val intakesSnapshot = userRef.collection("intakes").get().await()
            if (!intakesSnapshot.isEmpty) {
                val existingIntakeKeysByLog = mutableMapOf<Long, MutableSet<String>>()
                for (doc in intakesSnapshot.documents) {
                    try {
                        val mealTypeStr = doc.getString("mealType") ?: "BREAKFAST"
                        val foodSourceStr = doc.getString("foodSource") ?: "MANUAL"
                        val remoteDailyLogId = doc.getLong("dailyLogId") ?: 0L
                        val logDate = doc.getString("logDate")
                        val localDailyLogId = resolveLocalDailyLogId(remoteDailyLogId, logDate)
                        if (localDailyLogId <= 0L) continue

                        val existingKeys = existingIntakeKeysByLog.getOrPut(localDailyLogId) {
                            intakeDao.getIntakesForDailyLogDirect(localDailyLogId)
                                .map { "${it.createdAt}|${it.foodName}|${it.mealType.name}|${it.calories}" }
                                .toMutableSet()
                        }

                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        val foodName = doc.getString("foodName") ?: ""
                        val calories = doc.getLong("calories")?.toInt() ?: 0
                        val dedupKey = "$createdAt|$foodName|$mealTypeStr|$calories"

                        if (existingKeys.add(dedupKey)) {
                            val intake = IntakeEntity(
                                id = 0L,
                                dailyLogId = localDailyLogId,
                                mealType = MealType.valueOf(mealTypeStr),
                                foodName = foodName,
                                servingSize = doc.getDouble("servingSize") ?: 1.0,
                                servingUnit = doc.getString("servingUnit") ?: "serving",
                                calories = calories,
                                protein = doc.getDouble("protein") ?: 0.0,
                                carbs = doc.getDouble("carbs") ?: 0.0,
                                fat = doc.getDouble("fat") ?: 0.0,
                                fiber = doc.getDouble("fiber") ?: 0.0,
                                imagePath = doc.getString("imagePath"),
                                foodSource = FoodSource.valueOf(foodSourceStr),
                                createdAt = createdAt,
                                isSync = true
                            )
                            intakeDao.insertIntake(intake)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing intake document")
                    }
                }
            }

            // 4. Fetch & Restore Water Records
            val waterSnapshot = userRef.collection("water_records").get().await()
            if (!waterSnapshot.isEmpty) {
                val existingWaterKeysByLog = mutableMapOf<Long, MutableSet<String>>()
                for (doc in waterSnapshot.documents) {
                    try {
                        val remoteDailyLogId = doc.getLong("dailyLogId") ?: 0L
                        val logDate = doc.getString("logDate")
                        val localDailyLogId = resolveLocalDailyLogId(remoteDailyLogId, logDate)
                        if (localDailyLogId <= 0L) continue

                        val existingKeys = existingWaterKeysByLog.getOrPut(localDailyLogId) {
                            waterDao.getWaterRecordsForDailyLogDirect(localDailyLogId)
                                .map { "${it.createdAt}|${it.amountMl}" }
                                .toMutableSet()
                        }

                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        val amountMl = doc.getLong("amountMl")?.toInt() ?: 0
                        val dedupKey = "$createdAt|$amountMl"

                        if (existingKeys.add(dedupKey)) {
                            val water = WaterEntity(
                                id = 0L,
                                dailyLogId = localDailyLogId,
                                amountMl = amountMl,
                                createdAt = createdAt,
                                isSync = true
                            )
                            waterDao.insertWater(water)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing water document")
                    }
                }
            }

            // 5. Fetch & Restore Exercises
            val exerciseSnapshot = userRef.collection("exercises").get().await()
            if (!exerciseSnapshot.isEmpty) {
                val existingExerciseKeysByLog = mutableMapOf<Long, MutableSet<String>>()
                for (doc in exerciseSnapshot.documents) {
                    try {
                        val remoteDailyLogId = doc.getLong("dailyLogId") ?: 0L
                        val logDate = doc.getString("logDate")
                        val localDailyLogId = resolveLocalDailyLogId(remoteDailyLogId, logDate)
                        if (localDailyLogId <= 0L) continue

                        val existingKeys = existingExerciseKeysByLog.getOrPut(localDailyLogId) {
                            exerciseDao.getExercisesForDailyLogDirect(localDailyLogId)
                                .map { "${it.createdAt}|${it.exerciseType}|${it.durationMinute}|${it.caloriesBurned}" }
                                .toMutableSet()
                        }

                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        val exerciseType = doc.getString("exerciseType") ?: ""
                        val durationMinute = doc.getLong("durationMinute")?.toInt() ?: 0
                        val caloriesBurned = doc.getLong("caloriesBurned")?.toInt() ?: 0
                        val dedupKey = "$createdAt|$exerciseType|$durationMinute|$caloriesBurned"

                        if (existingKeys.add(dedupKey)) {
                            val exercise = ExerciseEntity(
                                id = 0L,
                                dailyLogId = localDailyLogId,
                                exerciseType = exerciseType,
                                durationMinute = durationMinute,
                                caloriesBurned = caloriesBurned,
                                note = doc.getString("note"),
                                createdAt = createdAt,
                                isSync = true
                            )
                            exerciseDao.insertExercise(exercise)
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing exercise document")
                    }
                }
            }

            // 6. Fetch & Restore Custom Foods (matched by name so pre-populated foods are not overwritten)
            val foodsSnapshot = userRef.collection("foods").get().await()
            if (!foodsSnapshot.isEmpty) {
                for (doc in foodsSnapshot.documents) {
                    try {
                        val name = doc.getString("name")?.trim() ?: ""
                        if (name.isBlank()) continue
                        val existingFood = foodDao.findFoodByName(name)
                        val foodEntity = FoodEntity(
                            id = existingFood?.id ?: 0L,
                            name = name,
                            calories = doc.getLong("calories")?.toInt() ?: 0,
                            protein = doc.getDouble("protein") ?: 0.0,
                            carbs = doc.getDouble("carbs") ?: 0.0,
                            fat = doc.getDouble("fat") ?: 0.0,
                            servingSize = doc.getDouble("servingSize") ?: 1.0,
                            servingUnit = doc.getString("servingUnit") ?: "serving",
                            isSync = true
                        )
                        foodDao.insertFood(foodEntity)
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing custom food document")
                    }
                }
            }

            // 7. Fetch & Restore Weight Logs (deduplicated by date)
            val weightsSnapshot = userRef.collection("weight_logs").get().await()
            if (!weightsSnapshot.isEmpty) {
                val parsedWeights = weightsSnapshot.documents.mapNotNull { doc ->
                    try {
                        WeightLogEntity(
                            id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L,
                            date = doc.getString("date") ?: "",
                            weightKg = doc.getDouble("weightKg")?.toFloat() ?: 0f,
                            note = doc.getString("note"),
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            isSync = true
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing weight log document")
                        null
                    }
                }

                val deduplicatedWeights = parsedWeights
                    .filter { it.date.isNotBlank() }
                    .groupBy { it.date }
                    .mapValues { (_, logs) -> logs.maxByOrNull { it.timestamp }!! }

                for ((dateStr, weight) in deduplicatedWeights) {
                    val existingLocal = weightLogDao.getWeightLogByDateDirect(dateStr)
                    weightLogDao.insertWeightLog(
                        weight.copy(
                            id = existingLocal?.id ?: 0L,
                            isSync = true
                        )
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "FirestoreSyncRepository: Download and restore failed")
            Result.failure(e)
        }
    }
}
