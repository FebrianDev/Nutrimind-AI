package com.fdev.nutrimindai.feature.coach

import com.fdev.nutrimindai.core.model.ActivityLevel
import com.fdev.nutrimindai.core.model.Gender
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.feature.coach.domain.ContextualQuickPromptGenerator
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.DailyLogDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.ExerciseDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.IntakeDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.dao.WaterDao
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.DailyLogEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.ExerciseEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.IntakeEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.WaterEntity
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.profile.data.local.dao.ProfileDao
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContextualQuickPromptGeneratorTest {

    private var profileResult: ProfileEntity? = null
    private var dailyLogResult: DailyLogEntity? = null
    private var intakesResult: List<IntakeEntity> = emptyList()
    private var watersResult: List<WaterEntity> = emptyList()
    private var exercisesResult: List<ExerciseEntity> = emptyList()

    private val fakeProfileDao = object : ProfileDao {
        override suspend fun insertProfile(profile: ProfileEntity) {}
        override suspend fun updateProfile(profile: ProfileEntity) {}
        override fun getProfile(id: String): Flow<ProfileEntity?> = flow { emit(profileResult) }
        override suspend fun getProfileDirect(id: String): ProfileEntity? = profileResult
        override suspend fun getUnsyncedProfiles(): List<ProfileEntity> = emptyList()
        override suspend fun markProfilesSynced(ids: List<String>) {}
        override suspend fun deleteProfile() {}
    }

    private val fakeDailyLogDao = object : DailyLogDao {
        override fun getDailyLogByDate(date: String): Flow<DailyLogEntity?> = flow { emit(dailyLogResult) }
        override suspend fun getDailyLogByDateDirect(date: String): DailyLogEntity? = dailyLogResult
        override fun getAllDailyLogs(): Flow<List<DailyLogEntity>> = flow { emit(listOfNotNull(dailyLogResult)) }
        override suspend fun getAllDailyLogsDirect(): List<DailyLogEntity> = listOfNotNull(dailyLogResult)
        override fun getDailyLogsBetweenDates(startDate: String, endDate: String): Flow<List<DailyLogEntity>> = flow { emit(listOfNotNull(dailyLogResult)) }
        override suspend fun getDailyLogsBetweenDatesDirect(startDate: String, endDate: String): List<DailyLogEntity> = listOfNotNull(dailyLogResult)
        override suspend fun insertDailyLog(dailyLog: DailyLogEntity) {}
        override suspend fun updateDailyLog(dailyLog: DailyLogEntity) {}
        override suspend fun updateAiSummary(dailyLogId: String, aiSummary: String) {}
        override suspend fun getDailyLogCount(): Int = if (dailyLogResult != null) 1 else 0
        override suspend fun getPastDraftLogs(currentDate: String): List<DailyLogEntity> = emptyList()
        override suspend fun getUnsyncedDailyLogs(): List<DailyLogEntity> = emptyList()
        override suspend fun markDailyLogsSynced(ids: List<String>) {}
    }

    private val fakeIntakeDao = object : IntakeDao {
        override fun getAllIntakes(): Flow<List<IntakeEntity>> = flowOf(intakesResult)
        override fun getIntakesForDailyLogs(dailyLogIds: List<String>): Flow<List<IntakeEntity>> = flowOf(intakesResult)
        override suspend fun getIntakesForDailyLogsDirect(dailyLogIds: List<String>): List<IntakeEntity> = intakesResult
        override fun getIntakesForDailyLog(dailyLogId: String): Flow<List<IntakeEntity>> = flowOf(intakesResult)
        override suspend fun getIntakesForDailyLogDirect(dailyLogId: String): List<IntakeEntity> = intakesResult
        override fun getIntakesByMealType(dailyLogId: String, mealType: String): Flow<List<IntakeEntity>> = flowOf(emptyList())
        override suspend fun insertIntake(intake: IntakeEntity) {}
        override suspend fun insertIntakes(intakes: List<IntakeEntity>) {}
        override suspend fun updateIntake(intake: IntakeEntity) {}
        override suspend fun deleteIntake(intake: IntakeEntity) {}
        override suspend fun getIntakeCount(dailyLogId: String): Int = 0
        override suspend fun getUnsyncedIntakes(): List<IntakeEntity> = emptyList()
        override suspend fun markIntakesSynced(ids: List<String>) {}
    }

    private val fakeWaterDao = object : WaterDao {
        override fun getAllWaterRecords(): Flow<List<WaterEntity>> = flowOf(watersResult)
        override fun getWaterRecordsForDailyLogs(dailyLogIds: List<String>): Flow<List<WaterEntity>> = flowOf(watersResult)
        override suspend fun getWaterRecordsForDailyLogsDirect(dailyLogIds: List<String>): List<WaterEntity> = watersResult
        override fun getWaterRecordsForDailyLog(dailyLogId: String): Flow<List<WaterEntity>> = flowOf(watersResult)
        override suspend fun getWaterRecordsForDailyLogDirect(dailyLogId: String): List<WaterEntity> = watersResult
        override fun getTotalWater(dailyLogId: String): Flow<Int> = flowOf(0)
        override suspend fun insertWater(water: WaterEntity) {}
        override suspend fun insertWaterRecords(waters: List<WaterEntity>) {}
        override suspend fun deleteWater(water: WaterEntity) {}
        override suspend fun getWaterCount(dailyLogId: String): Int = 0
        override suspend fun getUnsyncedWater(): List<WaterEntity> = emptyList()
        override suspend fun markWaterSynced(ids: List<String>) {}
    }

    private val fakeExerciseDao = object : ExerciseDao {
        override fun getAllExercises(): Flow<List<ExerciseEntity>> = flowOf(exercisesResult)
        override fun getExercisesForDailyLogs(dailyLogIds: List<String>): Flow<List<ExerciseEntity>> = flowOf(exercisesResult)
        override suspend fun getExercisesForDailyLogsDirect(dailyLogIds: List<String>): List<ExerciseEntity> = exercisesResult
        override fun getExercisesForDailyLog(dailyLogId: String): Flow<List<ExerciseEntity>> = flowOf(exercisesResult)
        override suspend fun getExercisesForDailyLogDirect(dailyLogId: String): List<ExerciseEntity> = exercisesResult
        override fun getTotalCaloriesBurned(dailyLogId: String): Flow<Int> = flowOf(0)
        override suspend fun insertExercise(exercise: ExerciseEntity) {}
        override suspend fun insertExercises(exercises: List<ExerciseEntity>) {}
        override suspend fun deleteExercise(exercise: ExerciseEntity) {}
        override suspend fun getExerciseCount(dailyLogId: String): Int = 0
        override suspend fun getUnsyncedExercises(): List<ExerciseEntity> = emptyList()
        override suspend fun markExercisesSynced(ids: List<String>) {}
    }

    private lateinit var generator: ContextualQuickPromptGenerator

    private val sampleProfile = ProfileEntity(
        id = "user_profile",
        name = "Alex",
        age = 25,
        gender = Gender.Male,
        height = 175,
        currentWeight = 70f,
        targetWeight = 68f,
        activityLevel = ActivityLevel.ModeratelyActive,
        goal = Goal.LoseWeight,
        dailyCalories = 1800,
        dailyProtein = 135,
        dailyCarbs = 200,
        dailyFat = 50,
        dailyExerciseMinutes = 30
    )

    private val sampleLog = DailyLogEntity(
        id = "log-101",
        date = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date()),
        status = DailyLogStatus.DRAFT
    )

    @Before
    fun setup() {
        profileResult = sampleProfile
        dailyLogResult = sampleLog
        intakesResult = emptyList()
        watersResult = emptyList()
        exercisesResult = emptyList()

        generator = ContextualQuickPromptGenerator(
            profileDao = fakeProfileDao,
            dailyLogDao = fakeDailyLogDao,
            intakeDao = fakeIntakeDao,
            waterDao = fakeWaterDao,
            exerciseDao = fakeExerciseDao
        )
    }

    @Test
    fun `morning hour generates breakfast and morning hydration prompts`() = runBlocking {
        val prompts = generator.getContextualPrompts(overrideHour = 8)

        assertTrue(prompts.isNotEmpty())
        assertTrue(prompts.any { it.id == "m_brk" })
        assertTrue(prompts.any { it.id == "m_hyd" })
        assertEquals("🌅", prompts.first { it.id == "m_brk" }.emoji)
    }

    @Test
    fun `afternoon hour generates lunch and energy prompts`() = runBlocking {
        val prompts = generator.getContextualPrompts(overrideHour = 13)

        assertTrue(prompts.isNotEmpty())
        assertTrue(prompts.any { it.id == "a_lun" })
        assertEquals("🥗", prompts.first { it.id == "a_lun" }.emoji)
    }

    @Test
    fun `evening hour generates dinner and recovery prompts`() = runBlocking {
        val prompts = generator.getContextualPrompts(overrideHour = 19)

        assertTrue(prompts.isNotEmpty())
        assertTrue(prompts.any { it.id == "e_din" })
        assertEquals("🍲", prompts.first { it.id == "e_din" }.emoji)
    }

    @Test
    fun `night hour generates late-night snack and sleep prompts`() = runBlocking {
        val prompts = generator.getContextualPrompts(overrideHour = 22)

        assertTrue(prompts.isNotEmpty())
        assertTrue(prompts.any { it.id == "n_snk" })
        assertEquals("🌙", prompts.first { it.id == "n_snk" }.emoji)
    }

    @Test
    fun `low remaining calories triggers contextual light snack prompt`() = runBlocking {
        intakesResult = listOf(
            IntakeEntity(
                id = "intake-1",
                dailyLogId = "log-101",
                mealType = MealType.LUNCH,
                foodName = "Chicken Rice",
                servingSize = 1.0,
                servingUnit = "plate",
                calories = 1600,
                protein = 60.0,
                carbs = 180.0,
                fat = 40.0,
                fiber = 5.0,
                foodSource = FoodSource.CAMERA
            )
        )

        val prompts = generator.getContextualPrompts(overrideHour = 18)

        assertTrue(prompts.any { it.id == "rem_cal" })
        val remCalPrompt = prompts.first { it.id == "rem_cal" }
        assertEquals("🍎", remCalPrompt.emoji)
        assertTrue(remCalPrompt.title.contains("200 kcal"))
    }

    @Test
    fun `low water in afternoon triggers hydration boost prompt`() = runBlocking {
        watersResult = listOf(
            WaterEntity(id = "water-1", dailyLogId = "log-101", amountMl = 500)
        )

        val prompts = generator.getContextualPrompts(overrideHour = 15)

        assertTrue(prompts.any { it.id == "hyd_boost" })
        val hydPrompt = prompts.first { it.id == "hyd_boost" }
        assertEquals("💧", hydPrompt.emoji)
        assertTrue(hydPrompt.promptText.contains("500ml"))
    }
}
