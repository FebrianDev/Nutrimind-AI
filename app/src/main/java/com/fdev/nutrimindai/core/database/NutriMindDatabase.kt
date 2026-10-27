package com.fdev.nutrimindai.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fdev.nutrimindai.feature.profile.data.local.dao.ProfileDao
import com.fdev.nutrimindai.feature.profile.data.local.entity.ProfileEntity

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

import com.fdev.nutrimindai.feature.coach.data.local.dao.ChatMessageDao
import com.fdev.nutrimindai.feature.coach.data.local.entity.ChatMessageEntity
import com.fdev.nutrimindai.feature.progress.data.local.dao.WeightLogDao
import com.fdev.nutrimindai.feature.progress.data.local.entity.WeightLogEntity

/**
 * Main Room Database configuration for NutriMind AI.
 */
@Database(
    entities = [
        ProfileEntity::class,
        DailyLogEntity::class,
        IntakeEntity::class,
        WaterEntity::class,
        ExerciseEntity::class,
        FoodEntity::class,
        WeightLogEntity::class,
        ChatMessageEntity::class
    ],
    version = 11,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class NutriMindDatabase : RoomDatabase() {

    /**
     * Provides access to [ProfileDao] for user profile operations.
     */
    abstract fun profileDao(): ProfileDao

    abstract fun dailyLogDao(): DailyLogDao
    abstract fun intakeDao(): IntakeDao
    abstract fun waterDao(): WaterDao
    abstract fun exerciseDao(): ExerciseDao
    abstract fun foodDao(): FoodDao
    abstract fun weightLogDao(): WeightLogDao
    abstract fun chatMessageDao(): ChatMessageDao
}

