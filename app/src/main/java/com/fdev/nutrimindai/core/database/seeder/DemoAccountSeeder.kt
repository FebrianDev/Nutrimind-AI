package com.fdev.nutrimindai.core.database.seeder

import com.fdev.nutrimindai.core.database.NutriMindDatabase
import com.fdev.nutrimindai.core.datastore.PreferenceManager
import com.fdev.nutrimindai.core.model.ActivityLevel
import com.fdev.nutrimindai.core.model.Gender
import com.fdev.nutrimindai.core.model.Goal
import com.fdev.nutrimindai.feature.coach.data.local.entity.ChatMessageEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.DailyLogEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.ExerciseEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.IntakeEntity
import com.fdev.nutrimindai.feature.dailyintake.data.local.entity.WaterEntity
import com.fdev.nutrimindai.feature.dailyintake.data.seeder.DefaultFoodCatalog
import com.fdev.nutrimindai.feature.dailyintake.domain.model.DailyLogStatus
import com.fdev.nutrimindai.feature.dailyintake.domain.model.FoodSource
import com.fdev.nutrimindai.feature.dailyintake.domain.model.MealType
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity
import com.fdev.nutrimindai.feature.progress.data.local.entity.WeightLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Seeder responsible for initializing rich, pre-configured demo user accounts for hackathon demonstration.
 *
 * Supported Accounts:
 * - Alex Morgan: demo.gain@nutrimind.ai / alex@nutrimind.ai (Target: Weight & Muscle Gain, 14 days history)
 * - Sarah Jenkins: demo.loss@nutrimind.ai / sarah@nutrimind.ai (Target: Weight Loss, 14 days history)
 * Password for both: "password123"
 */
@Singleton
class DemoAccountSeeder @Inject constructor(
    private val database: NutriMindDatabase,
    private val preferenceManager: PreferenceManager
) {

    companion object {
        const val DEMO_PASSWORD = "password123"

        const val EMAIL_ALEX = "demo.gain@nutrimind.ai"
        const val ALIAS_ALEX = "alex@nutrimind.ai"

        const val EMAIL_SARAH = "demo.loss@nutrimind.ai"
        const val ALIAS_SARAH = "sarah@nutrimind.ai"

        fun isDemoAccount(email: String): Boolean {
            val normalized = email.trim().lowercase(Locale.US)
            return normalized == EMAIL_ALEX ||
                    normalized == ALIAS_ALEX ||
                    normalized == EMAIL_SARAH ||
                    normalized == ALIAS_SARAH
        }

        fun isAlexAccount(email: String): Boolean {
            val normalized = email.trim().lowercase(Locale.US)
            return normalized == EMAIL_ALEX || normalized == ALIAS_ALEX
        }
    }

    suspend fun seedDemoAccount(email: String) = withContext(Dispatchers.IO) {
        val isAlex = isAlexAccount(email)
        Timber.d("DemoAccountSeeder: Starting 14-day data generation for ${if (isAlex) "Alex (Gain)" else "Sarah (Loss)"}")

        // 1. Wipe existing local database completely for a clean, deterministic state
        database.clearAllTables()

        // 2. Populate full food catalog
        database.foodDao().insertFoods(DefaultFoodCatalog.foods)

        // 3. Populate Profile
        val profile = if (isAlex) {
            ProfileEntity(
                id = ProfileEntity.DEFAULT_PROFILE_ID,
                name = "Alex Morgan",
                age = 25,
                gender = Gender.Male,
                height = 178,
                currentWeight = 69.2f,
                activityLevel = ActivityLevel.VeryActive,
                goal = Goal.GainWeight,
                targetWeight = 75.0f,
                dailyCalories = 2650,
                dailyProtein = 165,
                dailyCarbs = 320,
                dailyFat = 75,
                dailyExerciseMinutes = 45,
                createdAt = System.currentTimeMillis() - (14L * 86_400_000L),
                updatedAt = System.currentTimeMillis(),
                isSync = true
            )
        } else {
            ProfileEntity(
                id = ProfileEntity.DEFAULT_PROFILE_ID,
                name = "Sarah Jenkins",
                age = 27,
                gender = Gender.Female,
                height = 165,
                currentWeight = 64.9f,
                activityLevel = ActivityLevel.ModeratelyActive,
                goal = Goal.LoseWeight,
                targetWeight = 60.0f,
                dailyCalories = 1650,
                dailyProtein = 125,
                dailyCarbs = 160,
                dailyFat = 45,
                dailyExerciseMinutes = 30,
                createdAt = System.currentTimeMillis() - (14L * 86_400_000L),
                updatedAt = System.currentTimeMillis(),
                isSync = true
            )
        }
        database.profileDao().insertProfile(profile)

        // 4. 14 Days Historical Progression Data
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        // Weight progression curves over 14 days
        // Alex: Gaining progressively from 67.5 kg -> 69.2 kg (+1.7 kg)
        val alexWeights = listOf(
            67.5f, 67.6f, 67.8f, 67.9f, 68.1f, 68.2f, 68.4f,
            68.5f, 68.7f, 68.8f, 68.9f, 69.0f, 69.1f, 69.2f
        )
        // Sarah: Losing progressively from 66.8 kg -> 64.9 kg (-1.9 kg)
        val sarahWeights = listOf(
            66.8f, 66.6f, 66.5f, 66.3f, 66.1f, 66.0f, 65.8f,
            65.6f, 65.5f, 65.3f, 65.2f, 65.1f, 65.0f, 64.9f
        )

        for (dayOffset in 13 downTo 0) {
            val cal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -dayOffset)
            }
            val dateStr = dateFormat.format(cal.time)
            val isToday = dayOffset == 0
            val dayIndex = 13 - dayOffset

            // A. Log Weight
            val weightVal = if (isAlex) alexWeights[dayIndex] else sarahWeights[dayIndex]
            database.weightLogDao().insertWeightLog(
                WeightLogEntity(
                    id = UUID.randomUUID().toString(),
                    date = dateStr,
                    weightKg = weightVal,
                    note = if (isToday) "Morning check-in (Today)" else "Daily morning weigh-in",
                    timestamp = cal.timeInMillis,
                    isSync = true
                )
            )

            // B. Create Daily Log Entity
            val dailyLogId = UUID.randomUUID().toString()
            val dailyLog = DailyLogEntity(
                id = dailyLogId,
                date = dateStr,
                status = if (isToday) DailyLogStatus.DRAFT else DailyLogStatus.COMPLETED,
                completedAt = if (isToday) null else (cal.timeInMillis + 72_000_000L),
                aiSummary = if (isToday) null else {
                    if (isAlex) "Solid hyper-trophy surplus hit! Surpassed 170g protein with progressive overload strength training."
                    else "Flawless calorie deficit adherence! High dietary fiber and clean lean protein sustained steady fat loss."
                },
                aiScore = if (isToday) null else (88 + (dayIndex % 11)),
                isSync = true
            )
            database.dailyLogDao().insertDailyLog(dailyLog)

            if (isToday) {
                // TODAY: Seed partial breakfast (~15-20% budget) and hydration so dashboard looks alive
                // and leaving ample room for user to test Food Scanner and NutriCoach live on video!
                if (isAlex) {
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.BREAKFAST,
                            foodName = "Scrambled Eggs with Olive Oil",
                            servingSize = 2.0,
                            servingUnit = "eggs",
                            calories = 180,
                            protein = 13.0,
                            carbs = 1.2,
                            fat = 14.0,
                            fiber = 0.0,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 28_800_000L,
                            isSync = true
                        )
                    )
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.BREAKFAST,
                            foodName = "Avocado Toast on Sourdough",
                            servingSize = 1.0,
                            servingUnit = "slice",
                            calories = 260,
                            protein = 6.0,
                            carbs = 26.0,
                            fat = 15.0,
                            fiber = 4.0,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 28_900_000L,
                            isSync = true
                        )
                    )
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.BREAKFAST,
                            foodName = "Whey Protein Isolate Shake",
                            servingSize = 1.0,
                            servingUnit = "scoop",
                            calories = 120,
                            protein = 25.0,
                            carbs = 2.0,
                            fat = 1.0,
                            fiber = 0.0,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 29_000_000L,
                            isSync = true
                        )
                    )
                    database.waterDao().insertWater(
                        WaterEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            amountMl = 800,
                            createdAt = cal.timeInMillis + 30_000_000L,
                            isSync = true
                        )
                    )
                } else {
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.BREAKFAST,
                            foodName = "Plain Greek Yogurt (0% Fat)",
                            servingSize = 200.0,
                            servingUnit = "g",
                            calories = 120,
                            protein = 20.0,
                            carbs = 7.0,
                            fat = 0.5,
                            fiber = 0.0,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 28_800_000L,
                            isSync = true
                        )
                    )
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.BREAKFAST,
                            foodName = "Oatmeal with Honey & Berries",
                            servingSize = 1.0,
                            servingUnit = "bowl",
                            calories = 190,
                            protein = 6.0,
                            carbs = 35.0,
                            fat = 2.5,
                            fiber = 4.0,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 28_900_000L,
                            isSync = true
                        )
                    )
                    database.waterDao().insertWater(
                        WaterEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            amountMl = 750,
                            createdAt = cal.timeInMillis + 30_000_000L,
                            isSync = true
                        )
                    )
                }
            } else {
                // PAST DAYS: Complete nutrition records for rich analytics
                if (isAlex) {
                    // Alex Daily Intake ~2,600 kcal
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.BREAKFAST,
                            foodName = "Oatmeal with Honey & Berries + Whey",
                            servingSize = 1.0,
                            servingUnit = "bowl",
                            calories = 420,
                            protein = 33.0,
                            carbs = 62.0,
                            fat = 6.0,
                            fiber = 6.0,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 28_800_000L,
                            isSync = true
                        )
                    )
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.LUNCH,
                            foodName = "Beef Sirloin Steak with Jasmine Rice",
                            servingSize = 1.0,
                            servingUnit = "plate",
                            calories = 880,
                            protein = 58.0,
                            carbs = 92.0,
                            fat = 26.0,
                            fiber = 3.0,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 46_800_000L,
                            isSync = true
                        )
                    )
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.DINNER,
                            foodName = "Baked Atlantic Salmon & Sweet Potato",
                            servingSize = 1.0,
                            servingUnit = "plate",
                            calories = 790,
                            protein = 52.0,
                            carbs = 78.0,
                            fat = 28.0,
                            fiber = 7.0,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 68_400_000L,
                            isSync = true
                        )
                    )
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.OTHERS,
                            foodName = "Greek Yogurt with Almonds & Banana",
                            servingSize = 1.0,
                            servingUnit = "bowl",
                            calories = 510,
                            protein = 28.0,
                            carbs = 56.0,
                            fat = 16.0,
                            fiber = 5.0,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 57_600_000L,
                            isSync = true
                        )
                    )
                    database.waterDao().insertWater(
                        WaterEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            amountMl = 2750,
                            createdAt = cal.timeInMillis + 72_000_000L,
                            isSync = true
                        )
                    )
                    database.exerciseDao().insertExercise(
                        ExerciseEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            exerciseType = "Strength Training",
                            durationMinute = 50,
                            caloriesBurned = 420,
                            note = "Heavy compound lifts (Chest & Triceps)",
                            createdAt = cal.timeInMillis + 61_200_000L,
                            isSync = true
                        )
                    )
                } else {
                    // Sarah Daily Intake ~1,600 kcal
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.BREAKFAST,
                            foodName = "Greek Yogurt Parfait with Mixed Berries",
                            servingSize = 1.0,
                            servingUnit = "bowl",
                            calories = 290,
                            protein = 22.0,
                            carbs = 38.0,
                            fat = 4.0,
                            fiber = 5.0,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 28_800_000L,
                            isSync = true
                        )
                    )
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.LUNCH,
                            foodName = "Grilled Chicken Breast & Quinoa Salad",
                            servingSize = 1.0,
                            servingUnit = "bowl",
                            calories = 520,
                            protein = 46.0,
                            carbs = 48.0,
                            fat = 14.0,
                            fiber = 8.0,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 46_800_000L,
                            isSync = true
                        )
                    )
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.DINNER,
                            foodName = "White Cod Fillet with Steamed Broccoli",
                            servingSize = 1.0,
                            servingUnit = "plate",
                            calories = 430,
                            protein = 42.0,
                            carbs = 32.0,
                            fat = 11.0,
                            fiber = 6.0,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 68_400_000L,
                            isSync = true
                        )
                    )
                    database.intakeDao().insertIntake(
                        IntakeEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            mealType = MealType.OTHERS,
                            foodName = "Sliced Apple & Handful of Almonds",
                            servingSize = 1.0,
                            servingUnit = "portion",
                            calories = 240,
                            protein = 6.0,
                            carbs = 26.0,
                            fat = 12.0,
                            fiber = 4.5,
                            foodSource = FoodSource.MANUAL,
                            createdAt = cal.timeInMillis + 57_600_000L,
                            isSync = true
                        )
                    )
                    database.waterDao().insertWater(
                        WaterEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            amountMl = 2250,
                            createdAt = cal.timeInMillis + 72_000_000L,
                            isSync = true
                        )
                    )
                    database.exerciseDao().insertExercise(
                        ExerciseEntity(
                            id = UUID.randomUUID().toString(),
                            dailyLogId = dailyLogId,
                            exerciseType = if (dayIndex % 2 == 0) "Outdoor Running" else "Vinyasa Yoga",
                            durationMinute = 35,
                            caloriesBurned = if (dayIndex % 2 == 0) 290 else 180,
                            note = "Cardio endurance & recovery flow",
                            createdAt = cal.timeInMillis + 61_200_000L,
                            isSync = true
                        )
                    )
                }
            }
        }

        // 5. Contextual Welcome Message in NutriCoach Chat
        val chatMessage = if (isAlex) {
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                role = "assistant",
                content = "Hey Alex! Welcome back. You're making tremendous progress on your muscle gain trajectory — up 1.7 kg over the last 14 days with stellar protein consistency! What are we fueling your training with today?",
                timestamp = System.currentTimeMillis() - 3_600_000L
            )
        } else {
            ChatMessageEntity(
                id = UUID.randomUUID().toString(),
                role = "assistant",
                content = "Good morning Sarah! You've lost 1.9 kg over the past two weeks with exceptional calorie deficit adherence. Let's keep your hydration high and crush your nutrition targets today!",
                timestamp = System.currentTimeMillis() - 3_600_000L
            )
        }
        database.chatMessageDao().insertMessage(chatMessage)

        Timber.d("DemoAccountSeeder: Successfully seeded 14-day history for ${if (isAlex) "Alex" else "Sarah"}")
    }
}

