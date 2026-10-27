package com.fdev.nutrimindai.core.database

import android.content.Context
import androidx.room.Room
import com.fdev.nutrimindai.core.util.Constants.DATABASE_NAME
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): NutriMindDatabase {
        return Room.databaseBuilder(
            context,
            NutriMindDatabase::class.java,
            DATABASE_NAME
        ).fallbackToDestructiveMigration(true).build()
    }

    @Provides
    @Singleton
    fun provideDailyLogDao(database: NutriMindDatabase) = database.dailyLogDao()

    @Provides
    @Singleton
    fun provideIntakeDao(database: NutriMindDatabase) = database.intakeDao()

    @Provides
    @Singleton
    fun provideWaterDao(database: NutriMindDatabase) = database.waterDao()

    @Provides
    @Singleton
    fun provideExerciseDao(database: NutriMindDatabase) = database.exerciseDao()

    @Provides
    @Singleton
    fun provideFoodDao(database: NutriMindDatabase) = database.foodDao()

    @Provides
    @Singleton
    fun provideWeightLogDao(database: NutriMindDatabase) = database.weightLogDao()

    @Provides
    @Singleton
    fun provideChatMessageDao(database: NutriMindDatabase): com.fdev.nutrimindai.feature.coach.data.local.dao.ChatMessageDao =
        database.chatMessageDao()
}

