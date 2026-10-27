package com.fdev.nutrimindai.core.sync

import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.DailyLogDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.ExerciseDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.FoodDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.IntakeDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.WaterDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.DailyLogEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.ExerciseEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.FoodEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.IntakeEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.WaterEntity
import com.fdev.nutrimindai.feature.profile.data.local.dao.ProfileDao
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.feature.progress.data.local.dao.WeightLogDao
import com.fdev.nutrimindai.feature.progress.data.local.entity.WeightLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.kotlin.mock

class SyncRepositoryTest {

    private val fakeProfileDao = object : ProfileDao {
        override suspend fun insertProfile(profile: ProfileEntity) {}
        override suspend fun updateProfile(profile: ProfileEntity) {}
        override fun getProfile(id: String): Flow<ProfileEntity?> = flowOf(null)
        override suspend fun getProfileDirect(id: String): ProfileEntity? = null
        override suspend fun getUnsyncedProfiles(): List<ProfileEntity> = emptyList()
        override suspend fun markProfilesSynced(ids: List<String>) {}
        override suspend fun deleteProfile() {}
    }

    private val fakeDailyLogDao = object : DailyLogDao {
        override fun getDailyLogByDate(date: String): Flow<DailyLogEntity?> = flowOf(null)
        override suspend fun getDailyLogByDateDirect(date: String): DailyLogEntity? = null
        override fun getAllDailyLogs(): Flow<List<DailyLogEntity>> = flowOf(emptyList())
        override suspend fun getAllDailyLogsDirect(): List<DailyLogEntity> = emptyList()
        override fun getDailyLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyLogEntity>> = flowOf(emptyList())
        override suspend fun getDailyLogsBetweenDatesDirect(startDate: String, endDate: String): List<DailyLogEntity> = emptyList()
        override suspend fun insertDailyLog(dailyLog: DailyLogEntity): Long = 1L
        override suspend fun updateDailyLog(dailyLog: DailyLogEntity) {}
        override suspend fun updateAiSummary(dailyLogId: Long, aiSummary: String) {}
        override suspend fun getDailyLogCount(): Int = 0
        override suspend fun getPastDraftLogs(currentDate: String): List<DailyLogEntity> = emptyList()
        override suspend fun getUnsyncedDailyLogs(): List<DailyLogEntity> = emptyList()
        override suspend fun markDailyLogsSynced(ids: List<Long>) {}
    }

    private val fakeIntakeDao = object : IntakeDao {
        override fun getIntakesForDailyLog(dailyLogId: Long): Flow<List<IntakeEntity>> = flowOf(emptyList())
        override suspend fun getIntakesForDailyLogDirect(dailyLogId: Long): List<IntakeEntity> = emptyList()
        override fun getIntakesByMealType(dailyLogId: Long, mealType: String): Flow<List<IntakeEntity>> = flowOf(emptyList())
        override suspend fun insertIntake(intake: IntakeEntity) {}
        override suspend fun updateIntake(intake: IntakeEntity) {}
        override suspend fun deleteIntake(intake: IntakeEntity) {}
        override suspend fun getIntakeCount(dailyLogId: Long): Int = 0
        override suspend fun getUnsyncedIntakes(): List<IntakeEntity> = emptyList()
        override suspend fun markIntakesSynced(ids: List<Long>) {}
    }

    private val fakeWaterDao = object : WaterDao {
        override fun getWaterRecordsForDailyLog(dailyLogId: Long): Flow<List<WaterEntity>> = flowOf(emptyList())
        override suspend fun getWaterRecordsForDailyLogDirect(dailyLogId: Long): List<WaterEntity> = emptyList()
        override fun getTotalWater(dailyLogId: Long): Flow<Int> = flowOf(0)
        override suspend fun insertWater(water: WaterEntity) {}
        override suspend fun deleteWater(water: WaterEntity) {}
        override suspend fun getWaterCount(dailyLogId: Long): Int = 0
        override suspend fun getUnsyncedWater(): List<WaterEntity> = emptyList()
        override suspend fun markWaterSynced(ids: List<Long>) {}
    }

    private val fakeExerciseDao = object : ExerciseDao {
        override fun getExercisesForDailyLog(dailyLogId: Long): Flow<List<ExerciseEntity>> = flowOf(emptyList())
        override suspend fun getExercisesForDailyLogDirect(dailyLogId: Long): List<ExerciseEntity> = emptyList()
        override fun getTotalCaloriesBurned(dailyLogId: Long): Flow<Int> = flowOf(0)
        override suspend fun insertExercise(exercise: ExerciseEntity) {}
        override suspend fun deleteExercise(exercise: ExerciseEntity) {}
        override suspend fun getExerciseCount(dailyLogId: Long): Int = 0
        override suspend fun getUnsyncedExercises(): List<ExerciseEntity> = emptyList()
        override suspend fun markExercisesSynced(ids: List<Long>) {}
    }

    private val fakeFoodDao = object : FoodDao {
        override fun getAllFoods(): Flow<List<FoodEntity>> = flowOf(emptyList())
        override suspend fun getAllFoodsDirect(): List<FoodEntity> = emptyList()
        override fun searchFoods(query: String): Flow<List<FoodEntity>> = flowOf(emptyList())
        override suspend fun searchFoodsDirect(query: String): List<FoodEntity> = emptyList()
        override suspend fun insertFoods(foods: List<FoodEntity>) {}
        override suspend fun insertFood(food: FoodEntity): Long = 1L
        override suspend fun getFoodCount(): Int = 0
        override suspend fun getUnsyncedFoods(): List<FoodEntity> = emptyList()
        override suspend fun markFoodsSynced(ids: List<Long>) {}
    }

    private val fakeWeightLogDao = object : WeightLogDao {
        override fun getAllWeightLogs(): Flow<List<WeightLogEntity>> = flowOf(emptyList())
        override suspend fun getAllWeightLogsDirect(): List<WeightLogEntity> = emptyList()
        override fun getWeightLogByDate(date: String): Flow<WeightLogEntity?> = flowOf(null)
        override suspend fun getWeightLogByDateDirect(date: String): WeightLogEntity? = null
        override fun getLatestWeightLog(): Flow<WeightLogEntity?> = flowOf(null)
        override suspend fun getLatestWeightLogDirect(): WeightLogEntity? = null
        override suspend fun insertWeightLog(log: WeightLogEntity): Long = 1L
        override suspend fun deleteWeightLog(id: Long) {}
        override suspend fun getUnsyncedWeightLogs(): List<WeightLogEntity> = emptyList()
        override suspend fun markWeightLogsSynced(ids: List<Long>) {}
    }

    private val fakeAuthManager = FirebaseAuthManager()
    private val fakePreferenceManager: PreferenceManager = mock()

    @Test
    fun `getUnsyncedCount calculates sum of unsynced items across all DAOs`() = runBlocking {
        val repository = FirestoreSyncRepository(
            authManager = fakeAuthManager,
            profileDao = fakeProfileDao,
            dailyLogDao = fakeDailyLogDao,
            intakeDao = fakeIntakeDao,
            waterDao = fakeWaterDao,
            exerciseDao = fakeExerciseDao,
            foodDao = fakeFoodDao,
            weightLogDao = fakeWeightLogDao,
            preferenceManager = fakePreferenceManager
        )

        val count = repository.getUnsyncedCount()
        assertEquals(0, count)
    }

    @Test
    fun `syncUnsyncedData handles uninitialized Firestore gracefully`() = runBlocking {
        val repository = FirestoreSyncRepository(
            authManager = fakeAuthManager,
            profileDao = fakeProfileDao,
            dailyLogDao = fakeDailyLogDao,
            intakeDao = fakeIntakeDao,
            waterDao = fakeWaterDao,
            exerciseDao = fakeExerciseDao,
            foodDao = fakeFoodDao,
            weightLogDao = fakeWeightLogDao,
            preferenceManager = fakePreferenceManager
        )

        val result = repository.syncUnsyncedData()
        assertFalse(result.success)
        assertEquals(0, result.syncedItemsCount)
    }
}
