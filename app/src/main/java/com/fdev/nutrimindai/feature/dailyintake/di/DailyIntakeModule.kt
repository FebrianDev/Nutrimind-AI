package com.fdev.nutrimindai.feature.dailyintake.di

import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalFoodDataSource
import com.fdev.nutrimindai.feature.dailyintake.data.local.datasource.LocalFoodDataSourceImpl
import com.fdev.nutrimindai.feature.dailyintake.data.repository.DailyLogRepositoryImpl
import com.fdev.nutrimindai.feature.dailyintake.data.repository.ExerciseRepositoryImpl
import com.fdev.nutrimindai.feature.dailyintake.data.repository.FoodRepositoryImpl
import com.fdev.nutrimindai.feature.dailyintake.data.repository.IntakeRepositoryImpl
import com.fdev.nutrimindai.feature.dailyintake.data.repository.WaterRepositoryImpl
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.DailyLogRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.ExerciseRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.FoodRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.IntakeRepository
import com.fdev.nutrimindai.feature.dailyintake.domain.repository.WaterRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DailyIntakeModule {

    @Binds
    @Singleton
    abstract fun bindLocalFoodDataSource(
        impl: LocalFoodDataSourceImpl
    ): LocalFoodDataSource

    @Binds
    @Singleton
    abstract fun bindFoodRepository(
        impl: FoodRepositoryImpl
    ): FoodRepository

    @Binds
    @Singleton
    abstract fun bindDailyLogRepository(
        impl: DailyLogRepositoryImpl
    ): DailyLogRepository

    @Binds
    @Singleton
    abstract fun bindIntakeRepository(
        impl: IntakeRepositoryImpl
    ): IntakeRepository

    @Binds
    @Singleton
    abstract fun bindWaterRepository(
        impl: WaterRepositoryImpl
    ): WaterRepository

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(
        impl: ExerciseRepositoryImpl
    ): ExerciseRepository
}
