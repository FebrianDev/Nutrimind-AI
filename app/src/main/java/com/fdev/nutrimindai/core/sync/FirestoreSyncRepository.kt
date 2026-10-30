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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import java.io.File
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
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

    private fun com.google.firebase.firestore.DocumentSnapshot.getIdString(
        field: String,
        fallback: String = ""
    ): String {
        val raw = this.get(field)
        return when (raw) {
            is String -> raw.ifBlank { fallback }
            is Number -> raw.toLong().toString()
            else -> fallback
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
        val pendingDeletions = preferenceManager.getPendingCloudDeletions()?.size ?: 0
        val pendingReset = if (preferenceManager.isPendingCloudReset() == true) 1 else 0
        return profiles + dailyLogs + intakes + waters + exercises + foods + weights + pendingDeletions + pendingReset
    }

    private suspend fun commitBatchDeletes(db: FirebaseFirestore, refs: List<DocumentReference>) {
        if (refs.isEmpty()) return
        for (chunk in refs.chunked(450)) {
            val batch = db.batch()
            for (ref in chunk) {
                batch.delete(ref)
            }
            batch.commit().await()
        }
    }

    private suspend fun commitBatchSets(
        db: FirebaseFirestore,
        entries: List<Pair<DocumentReference, Map<String, Any?>>>
    ) {
        if (entries.isEmpty()) return
        for (chunk in entries.chunked(450)) {
            val batch = db.batch()
            for ((ref, data) in chunk) {
                batch.set(ref, data)
            }
            batch.commit().await()
        }
    }

    private suspend fun processPendingCloudDeletions(
        db: FirebaseFirestore,
        userRef: DocumentReference
    ): Int {
        var processedCount = 0

        if (preferenceManager.isPendingCloudReset()) {
            val collectionsToClear = listOf("intakes", "water_records", "exercises", "daily_logs", "foods")
            val snapshots = coroutineScope {
                collectionsToClear.map { collectionName ->
                    async { userRef.collection(collectionName).get().await() }
                }.map { it.await() }
            }
            val refsToDelete = snapshots.flatMap { snapshot ->
                snapshot.documents.map { it.reference }
            }
            commitBatchDeletes(db, refsToDelete)
            processedCount += refsToDelete.size
            preferenceManager.setPendingCloudReset(false)
            processedCount++
        }

        val pendingDeletions = preferenceManager.getPendingCloudDeletions()
        if (pendingDeletions.isNotEmpty()) {
            val completedEntries = mutableSetOf<String>()
            val refsToDelete = mutableListOf<DocumentReference>()
            for (entry in pendingDeletions) {
                val parts = entry.split(":", limit = 2)
                if (parts.size == 2) {
                    val collection = parts[0]
                    val docId = parts[1]
                    if (collection.isNotBlank() && docId.isNotBlank()) {
                        refsToDelete.add(userRef.collection(collection).document(docId))
                    }
                }
                completedEntries.add(entry)
            }
            commitBatchDeletes(db, refsToDelete)
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
            totalSynced += processPendingCloudDeletions(db, userRef)

            // 1. Sync Profiles
            val unsyncedProfiles = profileDao.getUnsyncedProfiles()
            if (unsyncedProfiles.isNotEmpty()) {
                val profileEntries = unsyncedProfiles.map { profile ->
                    userRef.collection("profiles").document(profile.id) to mapOf(
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

                commitBatchSets(db, profileEntries)
                profileDao.markProfilesSynced(unsyncedProfiles.map { it.id })
                totalSynced += unsyncedProfiles.size
            }

            // 2. Sync Daily Logs (using date as deterministic document ID)
            val unsyncedDailyLogs = dailyLogDao.getUnsyncedDailyLogs()
            if (unsyncedDailyLogs.isNotEmpty()) {
                val dailyLogEntries = unsyncedDailyLogs.map { log ->
                    val docId = log.date.ifBlank { log.id }
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
                    userRef.collection("daily_logs").document(docId) to data
                }
                commitBatchSets(db, dailyLogEntries)
                dailyLogDao.markDailyLogsSynced(unsyncedDailyLogs.map { it.id })
                totalSynced += unsyncedDailyLogs.size
            }

            val logIdToDate = dailyLogDao.getAllDailyLogsDirect().associate { it.id to it.date }

            // 3. Sync Intakes (using UUID document ID & uploading food photo)
            val unsyncedIntakes = intakeDao.getUnsyncedIntakes()
            if (unsyncedIntakes.isNotEmpty()) {
                val intakeEntries = unsyncedIntakes.map { intake ->
                    val docId = intake.id
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
                    userRef.collection("intakes").document(docId) to data
                }
                commitBatchSets(db, intakeEntries)
                intakeDao.markIntakesSynced(unsyncedIntakes.map { it.id })
                totalSynced += unsyncedIntakes.size
            }

            // 4. Sync Water Records (using UUID document ID)
            val unsyncedWaters = waterDao.getUnsyncedWater()
            if (unsyncedWaters.isNotEmpty()) {
                val waterEntries = unsyncedWaters.map { water ->
                    val docId = water.id
                    val data = mapOf(
                        "id" to water.id,
                        "dailyLogId" to water.dailyLogId,
                        "logDate" to (logIdToDate[water.dailyLogId] ?: ""),
                        "amountMl" to water.amountMl,
                        "createdAt" to water.createdAt,
                        "isSync" to true
                    )
                    userRef.collection("water_records").document(docId) to data
                }
                commitBatchSets(db, waterEntries)
                waterDao.markWaterSynced(unsyncedWaters.map { it.id })
                totalSynced += unsyncedWaters.size
            }

            // 5. Sync Exercises (using UUID document ID)
            val unsyncedExercises = exerciseDao.getUnsyncedExercises()
            if (unsyncedExercises.isNotEmpty()) {
                val exerciseEntries = unsyncedExercises.map { ex ->
                    val docId = ex.id
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
                    userRef.collection("exercises").document(docId) to data
                }
                commitBatchSets(db, exerciseEntries)
                exerciseDao.markExercisesSynced(unsyncedExercises.map { it.id })
                totalSynced += unsyncedExercises.size
            }

            // 6. Sync Custom Foods
            val unsyncedFoods = foodDao.getUnsyncedFoods()
            if (unsyncedFoods.isNotEmpty()) {
                val foodEntries = unsyncedFoods.map { food ->
                    val docId = food.id
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
                    userRef.collection("foods").document(docId) to data
                }
                commitBatchSets(db, foodEntries)
                foodDao.markFoodsSynced(unsyncedFoods.map { it.id })
                totalSynced += unsyncedFoods.size
            }

            // 7. Sync Weight Logs (using date or UUID as document ID)
            val unsyncedWeightLogs = weightLogDao.getUnsyncedWeightLogs()
            if (unsyncedWeightLogs.isNotEmpty()) {
                val weightEntries = unsyncedWeightLogs.map { log ->
                    val docId = log.date.ifBlank { log.id }
                    val data = mapOf(
                        "id" to log.id,
                        "date" to log.date,
                        "weightKg" to log.weightKg,
                        "note" to log.note,
                        "timestamp" to log.timestamp,
                        "isSync" to true
                    )
                    userRef.collection("weight_logs").document(docId) to data
                }
                commitBatchSets(db, weightEntries)
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
            processPendingCloudDeletions(db, userRef)

            // Fetch all 7 Firestore collections in parallel
            val snapshots = coroutineScope {
                val pDef = async { userRef.collection("profiles").get().await() }
                val dlDef = async { userRef.collection("daily_logs").get().await() }
                val inDef = async { userRef.collection("intakes").get().await() }
                val wDef = async { userRef.collection("water_records").get().await() }
                val exDef = async { userRef.collection("exercises").get().await() }
                val fDef = async { userRef.collection("foods").get().await() }
                val wlDef = async { userRef.collection("weight_logs").get().await() }
                listOf(
                    pDef.await(),
                    dlDef.await(),
                    inDef.await(),
                    wDef.await(),
                    exDef.await(),
                    fDef.await(),
                    wlDef.await()
                )
            }
            val profileSnapshot = snapshots[0]
            val dailyLogsSnapshot = snapshots[1]
            val intakesSnapshot = snapshots[2]
            val waterSnapshot = snapshots[3]
            val exerciseSnapshot = snapshots[4]
            val foodsSnapshot = snapshots[5]
            val weightsSnapshot = snapshots[6]

            // 1. Restore profiles
            val restoredProfiles = if (!profileSnapshot.isEmpty) {
                profileSnapshot.documents.mapNotNull { doc ->
                    try {
                        val genderStr = doc.getString("gender") ?: "Male"
                        val activityLevelStr = doc.getString("activityLevel") ?: "Sedentary"
                        val goalStr = doc.getString("goal") ?: "MaintainWeight"

                        ProfileEntity(
                            id = doc.getIdString("id", ProfileEntity.DEFAULT_PROFILE_ID),
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

            val remoteLogIdToDate = mutableMapOf<String, String>()
            val existingLocalLogsByDate = dailyLogDao.getAllDailyLogsDirect()
                .associateBy { it.date }
                .toMutableMap()
            val dateToLocalLogId = existingLocalLogsByDate
                .mapValues { it.value.id }
                .toMutableMap()

            // 2. Restore Daily Logs (deduplicated by date, preserving local primary keys)
            if (!dailyLogsSnapshot.isEmpty) {
                val parsedDailyLogs = dailyLogsSnapshot.documents.mapNotNull { doc ->
                    try {
                        val statusStr = doc.getString("status") ?: "DRAFT"
                        val remoteId = doc.getIdString("id", doc.id)
                        val dateStr = doc.getString("date") ?: ""
                        if (remoteId.isNotBlank() && dateStr.isNotBlank()) {
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
                    val existingLocal = existingLocalLogsByDate[dateStr]
                    if (existingLocal != null) {
                        val mergedStatus = if (
                            existingLocal.status == DailyLogStatus.COMPLETED ||
                            log.status == DailyLogStatus.COMPLETED
                        ) {
                            DailyLogStatus.COMPLETED
                        } else {
                            log.status
                        }
                        val updatedLog = existingLocal.copy(
                            status = mergedStatus,
                            completedAt = log.completedAt ?: existingLocal.completedAt,
                            aiSummary = log.aiSummary?.takeIf { it.isNotBlank() } ?: existingLocal.aiSummary,
                            aiScore = log.aiScore ?: existingLocal.aiScore,
                            isSync = true
                        )
                        dailyLogDao.updateDailyLog(updatedLog)
                        existingLocalLogsByDate[dateStr] = updatedLog
                        dateToLocalLogId[dateStr] = existingLocal.id
                    } else {
                        val logId = log.id.takeIf { it.isNotBlank() } ?: UUID.randomUUID().toString()
                        val newLog = log.copy(id = logId, isSync = true)
                        dailyLogDao.insertDailyLog(newLog)
                        val finalId = dailyLogDao.getDailyLogByDateDirect(dateStr)?.id ?: logId
                        dateToLocalLogId[dateStr] = finalId
                    }
                }
            }

            suspend fun resolveLocalDailyLogId(remoteDailyLogId: String, logDate: String?): String {
                val resolvedDate = logDate?.takeIf { it.isNotBlank() }
                    ?: remoteLogIdToDate[remoteDailyLogId]
                    ?: LocalDate.now().toString()

                dateToLocalLogId[resolvedDate]?.let { if (it.isNotBlank()) return it }

                val existing = dailyLogDao.getDailyLogByDateDirect(resolvedDate)
                if (existing != null) {
                    dateToLocalLogId[resolvedDate] = existing.id
                    return existing.id
                }

                val newId = UUID.randomUUID().toString()
                val newLog = DailyLogEntity(
                    id = newId,
                    date = resolvedDate,
                    status = DailyLogStatus.DRAFT,
                    isSync = true
                )
                dailyLogDao.insertDailyLog(newLog)
                val finalId = dailyLogDao.getDailyLogByDateDirect(resolvedDate)?.id ?: newId
                dateToLocalLogId[resolvedDate] = finalId
                return finalId
            }

            // 3. Restore Intakes (batch insert)
            if (!intakesSnapshot.isEmpty) {
                val existingIntakeKeysByLog = mutableMapOf<String, MutableSet<String>>()
                val intakesToInsert = mutableListOf<IntakeEntity>()
                for (doc in intakesSnapshot.documents) {
                    try {
                        val mealTypeStr = doc.getString("mealType") ?: "BREAKFAST"
                        val foodSourceStr = doc.getString("foodSource") ?: "MANUAL"
                        val remoteDailyLogId = doc.getIdString("dailyLogId")
                        val logDate = doc.getString("logDate")
                        val localDailyLogId = resolveLocalDailyLogId(remoteDailyLogId, logDate)
                        if (localDailyLogId.isBlank()) continue

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
                            intakesToInsert.add(
                                IntakeEntity(
                                    id = doc.getIdString("id", doc.id),
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
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing intake document")
                    }
                }
                if (intakesToInsert.isNotEmpty()) {
                    intakeDao.insertIntakes(intakesToInsert)
                }
            }

            // 4. Restore Water Records (batch insert)
            if (!waterSnapshot.isEmpty) {
                val existingWaterKeysByLog = mutableMapOf<String, MutableSet<String>>()
                val watersToInsert = mutableListOf<WaterEntity>()
                for (doc in waterSnapshot.documents) {
                    try {
                        val remoteDailyLogId = doc.getIdString("dailyLogId")
                        val logDate = doc.getString("logDate")
                        val localDailyLogId = resolveLocalDailyLogId(remoteDailyLogId, logDate)
                        if (localDailyLogId.isBlank()) continue

                        val existingKeys = existingWaterKeysByLog.getOrPut(localDailyLogId) {
                            waterDao.getWaterRecordsForDailyLogDirect(localDailyLogId)
                                .map { "${it.createdAt}|${it.amountMl}" }
                                .toMutableSet()
                        }

                        val createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis()
                        val amountMl = doc.getLong("amountMl")?.toInt() ?: 0
                        val dedupKey = "$createdAt|$amountMl"

                        if (existingKeys.add(dedupKey)) {
                            watersToInsert.add(
                                WaterEntity(
                                    id = doc.getIdString("id", doc.id),
                                    dailyLogId = localDailyLogId,
                                    amountMl = amountMl,
                                    createdAt = createdAt,
                                    isSync = true
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing water document")
                    }
                }
                if (watersToInsert.isNotEmpty()) {
                    waterDao.insertWaterRecords(watersToInsert)
                }
            }

            // 5. Restore Exercises (batch insert)
            if (!exerciseSnapshot.isEmpty) {
                val existingExerciseKeysByLog = mutableMapOf<String, MutableSet<String>>()
                val exercisesToInsert = mutableListOf<ExerciseEntity>()
                for (doc in exerciseSnapshot.documents) {
                    try {
                        val remoteDailyLogId = doc.getIdString("dailyLogId")
                        val logDate = doc.getString("logDate")
                        val localDailyLogId = resolveLocalDailyLogId(remoteDailyLogId, logDate)
                        if (localDailyLogId.isBlank()) continue

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
                            exercisesToInsert.add(
                                ExerciseEntity(
                                    id = doc.getIdString("id", doc.id),
                                    dailyLogId = localDailyLogId,
                                    exerciseType = exerciseType,
                                    durationMinute = durationMinute,
                                    caloriesBurned = caloriesBurned,
                                    note = doc.getString("note"),
                                    createdAt = createdAt,
                                    isSync = true
                                )
                            )
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing exercise document")
                    }
                }
                if (exercisesToInsert.isNotEmpty()) {
                    exerciseDao.insertExercises(exercisesToInsert)
                }
            }

            // 6. Restore Custom Foods (matched by name in memory & batch insert)
            if (!foodsSnapshot.isEmpty) {
                val existingFoodByName = foodDao.getAllFoodsDirect()
                    .associateBy { it.name.trim().lowercase(Locale.ROOT) }
                val foodsToUpsert = mutableListOf<FoodEntity>()
                for (doc in foodsSnapshot.documents) {
                    try {
                        val name = doc.getString("name")?.trim() ?: ""
                        if (name.isBlank()) continue
                        val existingFood = existingFoodByName[name.lowercase(Locale.ROOT)]
                        foodsToUpsert.add(
                            FoodEntity(
                                id = existingFood?.id ?: doc.getIdString("id", doc.id),
                                name = name,
                                calories = doc.getLong("calories")?.toInt() ?: 0,
                                protein = doc.getDouble("protein") ?: 0.0,
                                carbs = doc.getDouble("carbs") ?: 0.0,
                                fat = doc.getDouble("fat") ?: 0.0,
                                servingSize = doc.getDouble("servingSize") ?: 1.0,
                                servingUnit = doc.getString("servingUnit") ?: "serving",
                                isSync = true
                            )
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing custom food document")
                    }
                }
                if (foodsToUpsert.isNotEmpty()) {
                    foodDao.insertFoods(foodsToUpsert)
                }
            }

            // 7. Restore Weight Logs (deduplicated by date & batch insert)
            if (!weightsSnapshot.isEmpty) {
                val parsedWeights = weightsSnapshot.documents.mapNotNull { doc ->
                    try {
                        val remoteId = doc.getIdString("id", doc.id)
                        WeightLogEntity(
                            id = remoteId,
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

                val existingWeightByDate = weightLogDao.getAllWeightLogsDirect()
                    .associateBy { it.date }

                val weightsToUpsert = deduplicatedWeights.map { (dateStr, weight) ->
                    val existingLocal = existingWeightByDate[dateStr]
                    weight.copy(
                        id = existingLocal?.id ?: weight.id,
                        isSync = true
                    )
                }
                if (weightsToUpsert.isNotEmpty()) {
                    weightLogDao.insertWeightLogs(weightsToUpsert)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "FirestoreSyncRepository: Download and restore failed")
            Result.failure(e)
        }
    }
}
