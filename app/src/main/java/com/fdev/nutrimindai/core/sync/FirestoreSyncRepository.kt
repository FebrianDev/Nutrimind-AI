package com.fdev.nutrimindai.core.sync

import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.DailyLogDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.ExerciseDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.FoodDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.IntakeDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.WaterDao
import com.fdev.nutrimindai.feature.onboarding.data.dao.ProfileDao
import com.fdev.nutrimindai.feature.progress.data.local.dao.WeightLogDao
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

import com.fdev.nutrimindai.feature.onboarding.data.entity.ProfileEntity
import com.fdev.nutrimindai.feature.onboarding.util.Gender
import com.fdev.nutrimindai.feature.onboarding.util.ActivityLevel
import com.fdev.nutrimindai.feature.onboarding.util.Goal
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

    private val firestore: FirebaseFirestore? by lazy {
        try {
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            Timber.w("FirestoreSyncRepository: Firestore is not initialized.")
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
        return profiles + dailyLogs + intakes + waters + exercises + foods + weights
    }

    suspend fun syncUnsyncedData(): SyncResult {
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

            // 2. Sync Daily Logs
            val unsyncedDailyLogs = dailyLogDao.getUnsyncedDailyLogs()
            if (unsyncedDailyLogs.isNotEmpty()) {
                for (log in unsyncedDailyLogs) {
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
                    userRef.collection("daily_logs").document(log.id.toString()).set(data).await()
                }
                dailyLogDao.markDailyLogsSynced(unsyncedDailyLogs.map { it.id })
                totalSynced += unsyncedDailyLogs.size
            }

            // 3. Sync Intakes
            val unsyncedIntakes = intakeDao.getUnsyncedIntakes()
            if (unsyncedIntakes.isNotEmpty()) {
                for (intake in unsyncedIntakes) {
                    val data = mapOf(
                        "id" to intake.id,
                        "dailyLogId" to intake.dailyLogId,
                        "mealType" to intake.mealType.name,
                        "foodName" to intake.foodName,
                        "servingSize" to intake.servingSize,
                        "servingUnit" to intake.servingUnit,
                        "calories" to intake.calories,
                        "protein" to intake.protein,
                        "carbs" to intake.carbs,
                        "fat" to intake.fat,
                        "fiber" to intake.fiber,
                        "imagePath" to intake.imagePath,
                        "foodSource" to intake.foodSource.name,
                        "createdAt" to intake.createdAt,
                        "isSync" to true
                    )
                    userRef.collection("intakes").document(intake.id.toString()).set(data).await()
                }
                intakeDao.markIntakesSynced(unsyncedIntakes.map { it.id })
                totalSynced += unsyncedIntakes.size
            }

            // 4. Sync Water Records
            val unsyncedWaters = waterDao.getUnsyncedWater()
            if (unsyncedWaters.isNotEmpty()) {
                for (water in unsyncedWaters) {
                    val data = mapOf(
                        "id" to water.id,
                        "dailyLogId" to water.dailyLogId,
                        "amountMl" to water.amountMl,
                        "createdAt" to water.createdAt,
                        "isSync" to true
                    )
                    userRef.collection("water_records").document(water.id.toString()).set(data).await()
                }
                waterDao.markWaterSynced(unsyncedWaters.map { it.id })
                totalSynced += unsyncedWaters.size
            }

            // 5. Sync Exercises
            val unsyncedExercises = exerciseDao.getUnsyncedExercises()
            if (unsyncedExercises.isNotEmpty()) {
                for (ex in unsyncedExercises) {
                    val data = mapOf(
                        "id" to ex.id,
                        "dailyLogId" to ex.dailyLogId,
                        "exerciseType" to ex.exerciseType,
                        "durationMinute" to ex.durationMinute,
                        "caloriesBurned" to ex.caloriesBurned,
                        "note" to ex.note,
                        "createdAt" to ex.createdAt,
                        "isSync" to true
                    )
                    userRef.collection("exercises").document(ex.id.toString()).set(data).await()
                }
                exerciseDao.markExercisesSynced(unsyncedExercises.map { it.id })
                totalSynced += unsyncedExercises.size
            }

            // 6. Sync Custom Foods
            val unsyncedFoods = foodDao.getUnsyncedFoods()
            if (unsyncedFoods.isNotEmpty()) {
                for (food in unsyncedFoods) {
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
                    userRef.collection("foods").document(food.id.toString()).set(data).await()
                }
                foodDao.markFoodsSynced(unsyncedFoods.map { it.id })
                totalSynced += unsyncedFoods.size
            }

            // 7. Sync Weight Logs
            val unsyncedWeightLogs = weightLogDao.getUnsyncedWeightLogs()
            if (unsyncedWeightLogs.isNotEmpty()) {
                for (log in unsyncedWeightLogs) {
                    val data = mapOf(
                        "id" to log.id,
                        "date" to log.date,
                        "weightKg" to log.weightKg,
                        "note" to log.note,
                        "timestamp" to log.timestamp,
                        "isSync" to true
                    )
                    userRef.collection("weight_logs").document(log.id.toString()).set(data).await()
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

    suspend fun downloadAndRestoreUserData(): Result<Unit> {
        val db = firestore ?: return Result.failure(Exception("Firebase Firestore is not initialized."))
        val userId = authManager.getOrCreateUserId()
        val userRef = db.collection("users").document(userId)

        return try {
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

            // 2. Fetch & Restore Daily Logs
            val dailyLogsSnapshot = userRef.collection("daily_logs").get().await()
            if (!dailyLogsSnapshot.isEmpty) {
                val dailyLogs = dailyLogsSnapshot.documents.mapNotNull { doc ->
                    try {
                        val statusStr = doc.getString("status") ?: "DRAFT"
                        DailyLogEntity(
                            id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L,
                            date = doc.getString("date") ?: "",
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
                for (log in dailyLogs) {
                    dailyLogDao.insertDailyLog(log)
                }
            }

            // 3. Fetch & Restore Intakes
            val intakesSnapshot = userRef.collection("intakes").get().await()
            if (!intakesSnapshot.isEmpty) {
                val intakes = intakesSnapshot.documents.mapNotNull { doc ->
                    try {
                        val mealTypeStr = doc.getString("mealType") ?: "BREAKFAST"
                        val foodSourceStr = doc.getString("foodSource") ?: "MANUAL"
                        IntakeEntity(
                            id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L,
                            dailyLogId = doc.getLong("dailyLogId") ?: 0L,
                            mealType = MealType.valueOf(mealTypeStr),
                            foodName = doc.getString("foodName") ?: "",
                            servingSize = doc.getDouble("servingSize") ?: 1.0,
                            servingUnit = doc.getString("servingUnit") ?: "serving",
                            calories = doc.getLong("calories")?.toInt() ?: 0,
                            protein = doc.getDouble("protein") ?: 0.0,
                            carbs = doc.getDouble("carbs") ?: 0.0,
                            fat = doc.getDouble("fat") ?: 0.0,
                            fiber = doc.getDouble("fiber") ?: 0.0,
                            imagePath = doc.getString("imagePath"),
                            foodSource = FoodSource.valueOf(foodSourceStr),
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            isSync = true
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing intake document")
                        null
                    }
                }
                for (intake in intakes) {
                    intakeDao.insertIntake(intake)
                }
            }

            // 4. Fetch & Restore Water Records
            val waterSnapshot = userRef.collection("water_records").get().await()
            if (!waterSnapshot.isEmpty) {
                val waters = waterSnapshot.documents.mapNotNull { doc ->
                    try {
                        WaterEntity(
                            id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L,
                            dailyLogId = doc.getLong("dailyLogId") ?: 0L,
                            amountMl = doc.getLong("amountMl")?.toInt() ?: 0,
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            isSync = true
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing water document")
                        null
                    }
                }
                for (water in waters) {
                    waterDao.insertWater(water)
                }
            }

            // 5. Fetch & Restore Exercises
            val exerciseSnapshot = userRef.collection("exercises").get().await()
            if (!exerciseSnapshot.isEmpty) {
                val exercises = exerciseSnapshot.documents.mapNotNull { doc ->
                    try {
                        ExerciseEntity(
                            id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L,
                            dailyLogId = doc.getLong("dailyLogId") ?: 0L,
                            exerciseType = doc.getString("exerciseType") ?: "",
                            durationMinute = doc.getLong("durationMinute")?.toInt() ?: 0,
                            caloriesBurned = doc.getLong("caloriesBurned")?.toInt() ?: 0,
                            note = doc.getString("note"),
                            createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                            isSync = true
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing exercise document")
                        null
                    }
                }
                for (ex in exercises) {
                    exerciseDao.insertExercise(ex)
                }
            }

            // 6. Fetch & Restore Custom Foods
            val foodsSnapshot = userRef.collection("foods").get().await()
            if (!foodsSnapshot.isEmpty) {
                val foods = foodsSnapshot.documents.mapNotNull { doc ->
                    try {
                        FoodEntity(
                            id = doc.getLong("id") ?: doc.id.toLongOrNull() ?: 0L,
                            name = doc.getString("name") ?: "",
                            calories = doc.getLong("calories")?.toInt() ?: 0,
                            protein = doc.getDouble("protein") ?: 0.0,
                            carbs = doc.getDouble("carbs") ?: 0.0,
                            fat = doc.getDouble("fat") ?: 0.0,
                            servingSize = doc.getDouble("servingSize") ?: 1.0,
                            servingUnit = doc.getString("servingUnit") ?: "serving",
                            isSync = true
                        )
                    } catch (e: Exception) {
                        Timber.e(e, "Error parsing custom food document")
                        null
                    }
                }
                foodDao.insertFoods(foods)
            }

            // 7. Fetch & Restore Weight Logs
            val weightsSnapshot = userRef.collection("weight_logs").get().await()
            if (!weightsSnapshot.isEmpty) {
                val weights = weightsSnapshot.documents.mapNotNull { doc ->
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
                for (weight in weights) {
                    weightLogDao.insertWeightLog(weight)
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "FirestoreSyncRepository: Download and restore failed")
            Result.failure(e)
        }
    }
}
