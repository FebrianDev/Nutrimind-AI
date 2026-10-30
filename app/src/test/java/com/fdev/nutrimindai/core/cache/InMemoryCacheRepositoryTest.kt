package com.fdev.nutrimindai.core.cache

import com.fdev.nutrimindai.core.model.ActivityLevel
import com.fdev.nutrimindai.core.model.Gender
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.DailyLogDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.ExerciseDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.IntakeDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.WaterDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalDailyLogDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalExerciseDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalFoodDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalIntakeDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalWaterDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.DailyLogEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.ExerciseEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.FoodEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.IntakeEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.WaterEntity
import com.fdev.nutrimindai.feature.dailyintake.data.repository.DailyLogRepositoryImpl
import com.fdev.nutrimindai.feature.dailyintake.data.repository.FoodRepositoryImpl
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.Food
import com.fdev.nutrimindai.feature.profile.data.local.dao.ProfileDao
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.feature.profile.data.repository.ProfileRepositoryImpl
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
class InMemoryCacheRepositoryTest {

    @Test
    fun `ProfileRepositoryImpl serves subsequent reads from memory and supports write-through and reactive invalidation`() = runTest {
        var directReadCount = 0
        val dbFlow = MutableStateFlow<ProfileEntity?>(null)

        val profileDao = object : ProfileDao {
            override suspend fun insertProfile(profile: ProfileEntity) {
                dbFlow.value = profile
            }
            override suspend fun updateProfile(profile: ProfileEntity) {
                dbFlow.value = profile
            }
            override fun getProfile(id: String): Flow<ProfileEntity?> = dbFlow
            override suspend fun getProfileDirect(id: String): ProfileEntity? {
                directReadCount++
                return dbFlow.value
            }
            override suspend fun getUnsyncedProfiles(): List<ProfileEntity> = emptyList()
            override suspend fun markProfilesSynced(ids: List<String>) {}
            override suspend fun deleteProfile() {
                dbFlow.value = null
            }
        }

        val initialProfile = ProfileEntity(
            id = "user_profile",
            name = "Budi",
            age = 28,
            gender = Gender.Male,
            height = 172,
            currentWeight = 68f,
            targetWeight = 65f,
            activityLevel = ActivityLevel.ModeratelyActive,
            goal = Goal.LoseWeight,
            dailyCalories = 1900,
            dailyProtein = 130,
            dailyCarbs = 210,
            dailyFat = 55,
            dailyExerciseMinutes = 30
        )
        dbFlow.value = initialProfile

        val repo = ProfileRepositoryImpl(profileDao, CoroutineScope(UnconfinedTestDispatcher(testScheduler)))

        // Repeated direct reads hit the in-memory cache without calling getProfileDirect on DAO
        assertEquals("Budi", repo.getProfileDirect()?.name)
        assertEquals("Budi", repo.getProfileDirect()?.name)
        assertEquals(0, directReadCount)

        // Write-through updates cache immediately
        val updated = initialProfile.copy(name = "Budi Updated")
        repo.updateProfile(updated)
        assertEquals("Budi Updated", repo.getProfileDirect()?.name)
        assertEquals("Budi Updated", repo.getProfile().first()?.name)

        // Simulated external write (e.g. Firestore sync or clearAllTables on logout)
        dbFlow.value = null
        assertNull(repo.getProfileDirect())
    }

    @Test
    fun `DailyLogRepositoryImpl caches date to id and runs autoCompletePastLogs only once per day`() = runTest {
        var pastDraftQueryCount = 0
        var getByDateDirectCount = 0
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val logsState = MutableStateFlow<List<DailyLogEntity>>(emptyList())

        val dailyLogDao = object : DailyLogDao {
            override fun getDailyLogByDate(date: String): Flow<DailyLogEntity?> = MutableStateFlow(null)
            override suspend fun getDailyLogByDateDirect(date: String): DailyLogEntity? {
                getByDateDirectCount++
                return logsState.value.firstOrNull { it.date == date }
            }
            override fun getAllDailyLogs(): Flow<List<DailyLogEntity>> = logsState
            override suspend fun getAllDailyLogsDirect(): List<DailyLogEntity> = logsState.value
            override fun getDailyLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyLogEntity>> = logsState
            override suspend fun getDailyLogsBetweenDatesDirect(startDate: String, endDate: String): List<DailyLogEntity> = logsState.value
            override suspend fun insertDailyLog(dailyLog: DailyLogEntity) {
                logsState.value = logsState.value + dailyLog
            }
            override suspend fun updateDailyLog(dailyLog: DailyLogEntity) {
                logsState.value = logsState.value.map { if (it.id == dailyLog.id) dailyLog else it }
            }
            override suspend fun updateAiSummary(dailyLogId: String, aiSummary: String) {}
            override suspend fun getDailyLogCount(): Int = logsState.value.size
            override suspend fun getPastDraftLogs(currentDate: String): List<DailyLogEntity> {
                pastDraftQueryCount++
                return emptyList()
            }
            override suspend fun getUnsyncedDailyLogs(): List<DailyLogEntity> = emptyList()
            override suspend fun markDailyLogsSynced(ids: List<String>) {}
        }

        val dummyIntakeDao = object : IntakeDao {
            override fun getAllIntakes(): Flow<List<IntakeEntity>> = MutableStateFlow(emptyList())
            override fun getIntakesForDailyLogs(dailyLogIds: List<String>): Flow<List<IntakeEntity>> = MutableStateFlow(emptyList())
            override suspend fun getIntakesForDailyLogsDirect(dailyLogIds: List<String>): List<IntakeEntity> = emptyList()
            override fun getIntakesForDailyLog(dailyLogId: String): Flow<List<IntakeEntity>> = MutableStateFlow(emptyList())
            override suspend fun getIntakesForDailyLogDirect(dailyLogId: String): List<IntakeEntity> = emptyList()
            override fun getIntakesByMealType(dailyLogId: String, mealType: String): Flow<List<IntakeEntity>> = MutableStateFlow(emptyList())
            override suspend fun insertIntake(intake: IntakeEntity) {}
            override suspend fun insertIntakes(intakes: List<IntakeEntity>) {}
            override suspend fun updateIntake(intake: IntakeEntity) {}
            override suspend fun deleteIntake(intake: IntakeEntity) {}
            override suspend fun getIntakeCount(dailyLogId: String): Int = 0
            override suspend fun getUnsyncedIntakes(): List<IntakeEntity> = emptyList()
            override suspend fun markIntakesSynced(ids: List<String>) {}
        }

        val dummyWaterDao = object : WaterDao {
            override fun getAllWaterRecords(): Flow<List<WaterEntity>> = MutableStateFlow(emptyList())
            override fun getWaterRecordsForDailyLogs(dailyLogIds: List<String>): Flow<List<WaterEntity>> = MutableStateFlow(emptyList())
            override suspend fun getWaterRecordsForDailyLogsDirect(dailyLogIds: List<String>): List<WaterEntity> = emptyList()
            override fun getWaterRecordsForDailyLog(dailyLogId: String): Flow<List<WaterEntity>> = MutableStateFlow(emptyList())
            override suspend fun getWaterRecordsForDailyLogDirect(dailyLogId: String): List<WaterEntity> = emptyList()
            override fun getTotalWater(dailyLogId: String): Flow<Int> = MutableStateFlow(0)
            override suspend fun insertWater(water: WaterEntity) {}
            override suspend fun insertWaterRecords(waters: List<WaterEntity>) {}
            override suspend fun deleteWater(water: WaterEntity) {}
            override suspend fun getWaterCount(dailyLogId: String): Int = 0
            override suspend fun getUnsyncedWater(): List<WaterEntity> = emptyList()
            override suspend fun markWaterSynced(ids: List<String>) {}
        }

        val dummyExerciseDao = object : ExerciseDao {
            override fun getAllExercises(): Flow<List<ExerciseEntity>> = MutableStateFlow(emptyList())
            override fun getExercisesForDailyLogs(dailyLogIds: List<String>): Flow<List<ExerciseEntity>> = MutableStateFlow(emptyList())
            override suspend fun getExercisesForDailyLogsDirect(dailyLogIds: List<String>): List<ExerciseEntity> = emptyList()
            override fun getExercisesForDailyLog(dailyLogId: String): Flow<List<ExerciseEntity>> = MutableStateFlow(emptyList())
            override suspend fun getExercisesForDailyLogDirect(dailyLogId: String): List<ExerciseEntity> = emptyList()
            override fun getTotalCaloriesBurned(dailyLogId: String): Flow<Int> = MutableStateFlow(0)
            override suspend fun insertExercise(exercise: ExerciseEntity) {}
            override suspend fun insertExercises(exercises: List<ExerciseEntity>) {}
            override suspend fun deleteExercise(exercise: ExerciseEntity) {}
            override suspend fun getExerciseCount(dailyLogId: String): Int = 0
            override suspend fun getUnsyncedExercises(): List<ExerciseEntity> = emptyList()
            override suspend fun markExercisesSynced(ids: List<String>) {}
        }

        val repo = DailyLogRepositoryImpl(
            localDataSource = LocalDailyLogDataSource(dailyLogDao),
            intakeDataSource = LocalIntakeDataSource(dummyIntakeDao),
            waterDataSource = LocalWaterDataSource(dummyWaterDao),
            exerciseDataSource = LocalExerciseDataSource(dummyExerciseDao),
            appScope = backgroundScope
        )

        val id1 = repo.getOrCreateDailyLog(today)
        val queriesAfterFirst = getByDateDirectCount
        assertEquals(1, pastDraftQueryCount)

        // Subsequent calls hit the in-memory cache with 0 DB queries
        val id2 = repo.getOrCreateDailyLog(today)
        val id3 = repo.getOrCreateDailyLog(today)
        assertEquals(id1, id2)
        assertEquals(id1, id3)
        assertEquals(1, pastDraftQueryCount)
        assertEquals(queriesAfterFirst, getByDateDirectCount)
    }

    @Test
    fun `FoodRepositoryImpl filters and deduplicates in memory without repeated DB queries`() = runTest {
        var searchDbCallCount = 0
        var findByNameDbCallCount = 0
        val foodsFlow = MutableStateFlow(
            listOf(
                FoodEntity(id = "1", name = "Nasi Goreng", calories = 400, protein = 12.0, carbs = 55.0, fat = 14.0, servingSize = 1.0, servingUnit = "plate"),
                FoodEntity(id = "2", name = "Ayam Bakar", calories = 260, protein = 28.0, carbs = 6.0, fat = 12.0, servingSize = 1.0, servingUnit = "piece")
            )
        )

        val localFoodDataSource = object : LocalFoodDataSource {
            override fun getAllFoods(): Flow<List<FoodEntity>> = foodsFlow
            override suspend fun getAllFoodsDirect(): List<FoodEntity> = foodsFlow.value
            override fun searchFoods(query: String): Flow<List<FoodEntity>> {
                searchDbCallCount++
                return foodsFlow
            }
            override suspend fun searchFoodsDirect(query: String): List<FoodEntity> {
                searchDbCallCount++
                return emptyList()
            }
            override suspend fun findFoodByName(name: String): FoodEntity? {
                findByNameDbCallCount++
                return null
            }
            override suspend fun insertFoods(foods: List<FoodEntity>) {
                foodsFlow.value = foods
            }
            override suspend fun insertFood(food: FoodEntity) {
                foodsFlow.value = foodsFlow.value.filterNot { it.id == food.id } + food
            }
            override suspend fun getFoodCount(): Int = foodsFlow.value.size
        }

        val repo = FoodRepositoryImpl(localFoodDataSource, backgroundScope)

        val results = repo.searchFoodsDirect("nasi")
        assertEquals(1, results.size)
        assertEquals("Nasi Goreng", results.first().name)
        assertEquals(0, searchDbCallCount)

        // Deduplicates by name using in-memory cache (0 findFoodByName DB calls)
        val existingId = repo.insertFood(
            Food(name = "  nasi goreng ", calories = 420, protein = 13.0, carbs = 56.0, fat = 15.0)
        )
        assertEquals("1", existingId)
        assertEquals(0, findByNameDbCallCount)
    }
}

